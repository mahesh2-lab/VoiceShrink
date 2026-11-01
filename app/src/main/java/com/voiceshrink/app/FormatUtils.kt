package com.voiceshrink.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import java.net.URLDecoder
import java.util.Locale

fun isDeviceCharging(context: Context): Boolean {
    return try {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } catch (_: Exception) {
        false
    }
}

fun formatFolderPath(uriString: String?): String {
    if (uriString.isNullOrBlank()) return "None selected"
    return try {
        val decoded = try {
            URLDecoder.decode(uriString, "UTF-8")
        } catch (_: Exception) {
            uriString
        }
        when {
            decoded.contains("primary:") -> {
                val subPath = decoded.substringAfter("primary:").trim('/')
                if (subPath.isBlank()) {
                    "Internal storage"
                } else {
                    "Internal storage › " + subPath.replace("/", " › ")
                }
            }
            decoded.contains("tree/") -> {
                val treePart = decoded.substringAfter("tree/")
                val volume = treePart.substringBefore(":").substringBefore("/")
                val subPath = treePart.substringAfter(":", "").trim('/')
                val volLabel = if (volume.equals("primary", ignoreCase = true)) {
                    "Internal storage"
                } else if (volume.isNotBlank()) {
                    "SD card"
                } else {
                    "Storage"
                }
                if (subPath.isBlank()) {
                    volLabel
                } else {
                    "$volLabel › " + subPath.replace("/", " › ")
                }
            }
            else -> {
                val lastSeg = decoded.trim('/').substringAfterLast('/')
                lastSeg.ifBlank { "Selected Folder" }
            }
        }
    } catch (_: Exception) {
        "Selected Folder"
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun formatDate(epochMs: Long): String {
    return DateFormat.format("MMM dd, yyyy • hh:mm a", epochMs).toString()
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
