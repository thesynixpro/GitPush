package com.aprax.gitpush.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catches fatal crashes and saves the stack trace where the user can reach it:
 * 1) app-private files/crashes/ (shown in-app on next launch with copy button),
 * 2) public Downloads/GitPush/ as GitPush-crash-*.txt (no permission needed on
 *    API 29+), so the trace can be shared even if the app dies on every launch.
 *
 * The report contains device model + stack trace only. No tokens, no file
 * contents, no credentials are ever written here.
 */
object CrashReporter {
    private const val TAG = "GitPushCrash"
    private const val DIR = "crashes"
    private const val MAX_KEPT = 5

    fun install(appContext: Context) {
        val ctx = appContext.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { writeReport(ctx, thread, error) }
            if (prev != null) {
                runCatching { prev.uncaughtException(thread, error) }
            } else {
                runCatching { Log.e(TAG, "Fatal", error) }
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    fun pendingReport(context: Context): File? {
        return try {
            val dir = File(context.filesDir, DIR)
            if (!dir.isDirectory) return null
            dir.listFiles { f -> f.isFile && f.name.startsWith("crash-") && f.name.endsWith(".txt") }
                ?.maxByOrNull { it.lastModified() }
        } catch (_: Exception) {
            null
        }
    }

    fun readReport(file: File): String {
        return runCatching { file.readText() }.getOrDefault("(could not read crash file)")
    }

    fun clearReports(context: Context) {
        runCatching {
            File(context.filesDir, DIR).listFiles()?.forEach { runCatching { it.delete() } }
        }
    }

    private fun writeReport(ctx: Context, thread: Thread, error: Throwable) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val report = buildString {
            appendLine("GitPush crash report — $stamp")
            appendLine("package=com.aprax.gitpush")
            appendLine("app=" + runCatching {
                val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                @Suppress("DEPRECATION")
                "${pi.versionName} (${pi.versionCode})"
            }.getOrDefault("unknown"))
            appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android=${Build.VERSION.RELEASE} (sdk=${Build.VERSION.SDK_INT})")
            appendLine("thread=${thread.name}")
            appendLine("---- stack ----")
            appendLine(Log.getStackTraceString(error))
        }
        // 1) private copy (in-app viewer)
        runCatching {
            val dir = File(ctx.filesDir, DIR)
            dir.mkdirs()
            File(dir, "crash-$stamp.txt").writeText(report)
            dir.listFiles { f -> f.isFile && f.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(MAX_KEPT)
                ?.forEach { runCatching { it.delete() } }
        }
        // 2) public copy in Downloads (no permission needed on API 29+)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "GitPush-crash-$stamp.txt")
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GitPush")
                }
                val uri = ctx.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: return
                ctx.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(report.toByteArray())
                }
            }
        }
    }
}
