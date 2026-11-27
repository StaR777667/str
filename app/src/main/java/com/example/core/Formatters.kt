package com.example.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

fun formatGigabytes(bytes: Long): String {
    if (bytes <= 0L) return "0.0 GB"
    val gb = 1024.0 * 1024.0 * 1024.0
    return String.format(Locale.US, "%.1f GB", bytes / gb)
}

fun formatTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Belum pernah"
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("id-ID"))
    return sdf.format(Date(timestampMs))
}
