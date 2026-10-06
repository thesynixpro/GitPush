package com.aprax.gitpush.scanner

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aprax.gitpush.model.ScannedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

data class ScanResult(
    val folderName: String,
    val files: List<ScannedFile>,
    val folderCount: Int,
    val warnings: List<String>
)

object FolderScanner {
    // Skip obviously non-pushable / huge system artifacts, but keep everything else.
    private val SKIP_NAMES = setOf(".thumbnails", ".trash")
    const val WARN_LIMIT = 5000

    suspend fun scan(
        context: Context,
        treeUri: Uri,
        onProgress: (files: Int, folders: Int) -> Unit = { _, _ -> }
    ): ScanResult = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("This file could not be read from the selected folder.")
        val folderName = root.name ?: "Selected folder"
        val out = ArrayList<ScannedFile>(512)
        val warnings = mutableListOf<String>()
        var folders = 0

        // Persist permission so re-scan / upload can re-open Uris.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        // Iterative DFS (no recursion → safe for deep trees, low stack).
        val stack = ArrayDeque<Pair<DocumentFile, String>>()
        stack.add(root to "")
        val seen = HashSet<String>(1024)

        while (stack.isNotEmpty() && isActive) {
            val (dir, relDir) = stack.removeLast()
            val children: Array<DocumentFile> = try {
                dir.listFiles()
            } catch (_: Exception) {
                warnings.add("Could not read '${if (relDir.isEmpty()) folderName else relDir}'. Skipped.")
                continue
            }
            for (child in children) {
                if (!isActive) break
                val name = child.name ?: continue
                if (name in SKIP_NAMES) continue
                val rel = if (relDir.isEmpty()) name else "$relDir/$name"
                if (!seen.add(child.uri.toString())) continue
                try {
                    if (child.isDirectory) {
                        folders++
                        stack.add(child to rel)
                    } else if (child.isFile) {
                        val size = try { child.length() } catch (_: Exception) { -1L }
                        out.add(
                            ScannedFile(
                                id = rel,
                                displayName = name,
                                relativePath = rel,
                                uri = child.uri,
                                size = size,
                                mimeType = try { child.type } catch (_: Exception) { null }
                            )
                        )
                    }
                } catch (_: Exception) {
                    warnings.add("Could not read '$rel'. Skipped.")
                }
                if ((out.size + folders) % 200 == 0) onProgress(out.size, folders)
                if (out.size > 20000) {
                    warnings.add("Very large folder: showing first 20,000 files.")
                    break
                }
            }
        }
        if (out.size >= WARN_LIMIT) {
            warnings.add(0, "Large folder (${out.size} files). Review list is virtualized for smooth scrolling.")
        }
        // Git cannot store empty dirs → explain, don't fail.
        if (folders > 0 && out.isEmpty()) {
            warnings.add("Empty folders cannot be stored by GitHub and will be ignored.")
        }
        out.sortBy { it.relativePath.lowercase() }
        onProgress(out.size, folders)
        ScanResult(folderName, out, folders, warnings)
    }
}
