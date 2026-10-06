package com.aprax.gitpush.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    fun bytes(n: Long): String {
        if (n < 0) return "unknown"
        if (n < 1024) return "$n B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val exp = (log10(n.toDouble()) / log10(1024.0)).toInt().coerceIn(0, 3)
        return String.format(Locale.US, "%.1f %s", n / 1024.0.pow(exp.toDouble()), units[exp])
    }

    fun date(ms: Long): String =
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(ms))

    fun eta(remainingBytes: Long, bytesPerSec: Double): String {
        if (bytesPerSec <= 0 || remainingBytes <= 0) return "…"
        val s = (remainingBytes / bytesPerSec).toLong()
        return if (s < 60) "${s}s left" else "${s / 60}m ${s % 60}s left"
    }

    fun speed(bytes: Long, elapsedMs: Long): String {
        if (elapsedMs <= 0) return ""
        val bps = bytes * 1000.0 / elapsedMs
        return "${bytes(bps.toLong())}/s"
    }

    /** ASCII preview tree: Project/ ├── a.html └── sub/ └── b.css (max [maxLines]). */
    fun treePreview(paths: List<String>, maxLines: Int = 14, rootName: String = "Project/"): String {
        val sb = StringBuilder(rootName.trimEnd('/') + "/\n")
        val top = paths.groupBy { it.substringBefore('/') }.toSortedMap()
        var lines = 0
        for ((k, v) in top) {
            if (lines >= maxLines) { sb.append("└── …\n"); break }
            if (!k.contains('.') && v.size == 1 && v[0].contains('/')) {
                sb.append("├── $k/\n"); lines++
                val inner = v[0].substringAfter('/').substringBefore('/')
                if (lines < maxLines) { sb.append("│   └── $inner\n"); lines++ }
            } else if (v.size == 1 && !v[0].contains('/')) {
                sb.append("├── $k\n"); lines++
            } else {
                sb.append("├── $k/\n"); lines++
                for (f in v.take(2)) {
                    val tail = f.substringAfter('/', f)
                    if (lines < maxLines && tail != f) { sb.append("│   ├── $tail\n"); lines++ }
                }
            }
        }
        return sb.toString().replace("├──", "├──").trimEnd()
    }
}
