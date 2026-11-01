package com.voiceshrink.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow

data class StatusColors(
    val done: Color,
    val onDone: Color,
    val doneContainer: Color,
    val onDoneContainer: Color,
    val queued: Color,
    val onQueued: Color,
    val queuedContainer: Color,
    val onQueuedContainer: Color,
    val failed: Color,
    val onFailed: Color,
    val failedContainer: Color,
    val onFailedContainer: Color,
)

/**
 * Harmonizes a color toward a target color (e.g. theme primary) in ARGB space.
 */
fun Color.harmonize(target: Color, fraction: Float = 0.15f): Color {
    val c1 = this.toArgb()
    val c2 = target.toArgb()

    val a1 = (c1 shr 24) and 0xFF
    val r1 = (c1 shr 16) and 0xFF
    val g1 = (c1 shr 8) and 0xFF
    val b1 = c1 and 0xFF

    val a2 = (c2 shr 24) and 0xFF
    val r2 = (c2 shr 16) and 0xFF
    val g2 = (c2 shr 8) and 0xFF
    val b2 = c2 and 0xFF

    val a = (a1 + (a2 - a1) * fraction).toInt().coerceIn(0, 255)
    val r = (r1 + (r2 - r1) * fraction).toInt().coerceIn(0, 255)
    val g = (g1 + (g2 - g1) * fraction).toInt().coerceIn(0, 255)
    val b = (b1 + (b2 - b1) * fraction).toInt().coerceIn(0, 255)

    return Color((a shl 24) or (r shl 16) or (g shl 8) or b)
}

/**
 * Calculates WCAG 2.1 relative luminance for a Compose Color.
 */
fun Color.luminance(): Double {
    val colorInt = this.toArgb()
    val r = ((colorInt shr 16) and 0xFF) / 255.0
    val g = ((colorInt shr 8) and 0xFF) / 255.0
    val b = (colorInt and 0xFF) / 255.0

    val rL = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
    val gL = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
    val bL = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

    return 0.2126 * rL + 0.7152 * gL + 0.0722 * bL
}

/**
 * Calculates WCAG 2.1 contrast ratio between two Compose Colors.
 */
fun calculateContrastRatio(fg: Color, bg: Color): Double {
    val l1 = fg.luminance()
    val l2 = bg.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

@Composable
@ReadOnlyComposable
fun statusColors(): StatusColors {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val isDark = surface.luminance() < 0.5

    return if (isDark) {
        val rawDone = Color(0xFF81C784).harmonize(primary)
        val rawDoneContainer = Color(0xFF1B4D24).harmonize(primary)
        val rawOnDoneContainer = Color(0xFFC8E6C9).harmonize(primary)

        val rawQueued = Color(0xFFFFB74D).harmonize(primary)
        val rawQueuedContainer = Color(0xFF4A2800).harmonize(primary)
        val rawOnQueuedContainer = Color(0xFFFFE0B2).harmonize(primary)

        StatusColors(
            done = rawDone,
            onDone = Color(0xFF00390A),
            doneContainer = rawDoneContainer,
            onDoneContainer = rawOnDoneContainer,
            queued = rawQueued,
            onQueued = Color(0xFF4A2800),
            queuedContainer = rawQueuedContainer,
            onQueuedContainer = rawOnQueuedContainer,
            failed = MaterialTheme.colorScheme.error,
            onFailed = MaterialTheme.colorScheme.onError,
            failedContainer = MaterialTheme.colorScheme.errorContainer,
            onFailedContainer = MaterialTheme.colorScheme.onErrorContainer,
        )
    } else {
        val rawDone = Color(0xFF1B5E20).harmonize(primary)
        val rawDoneContainer = Color(0xFFE8F5E9).harmonize(primary)
        val rawOnDoneContainer = Color(0xFF0D3811).harmonize(primary)

        val rawQueued = Color(0xFFBF360C).harmonize(primary)
        val rawQueuedContainer = Color(0xFFFFF3E0).harmonize(primary)
        val rawOnQueuedContainer = Color(0xFF4E1500).harmonize(primary)

        StatusColors(
            done = rawDone,
            onDone = Color.White,
            doneContainer = rawDoneContainer,
            onDoneContainer = rawOnDoneContainer,
            queued = rawQueued,
            onQueued = Color.White,
            queuedContainer = rawQueuedContainer,
            onQueuedContainer = rawOnQueuedContainer,
            failed = MaterialTheme.colorScheme.error,
            onFailed = MaterialTheme.colorScheme.onError,
            failedContainer = MaterialTheme.colorScheme.errorContainer,
            onFailedContainer = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
