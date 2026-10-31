package com.example.callcompressor

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattingAndPathTest {

    @Test
    fun testFormatFolderPathNullAndEmpty() {
        assertEquals("None selected", formatFolderPath(null))
        assertEquals("None selected", formatFolderPath(""))
        assertEquals("None selected", formatFolderPath("   "))
    }

    @Test
    fun testFormatFolderPathPrimaryInternalStorage() {
        assertEquals("Internal storage", formatFolderPath("primary:"))
        assertEquals("Internal storage › Recordings", formatFolderPath("primary:Recordings"))
        assertEquals(
            "Internal storage › Recordings › Call",
            formatFolderPath("content://com.android.externalstorage.documents/tree/primary%3ARecordings%2FCall")
        )
    }

    @Test
    fun testFormatFolderPathSdCard() {
        assertEquals(
            "SD card › Recordings",
            formatFolderPath("content://com.android.externalstorage.documents/tree/12E4-5678%3ARecordings")
        )
    }

    @Test
    fun testStatusColorsContrastRatiosLightMode() {
        val primary = Color(0xFF6750A4)
        val surfaceLight = Color(0xFFFEF7FF)

        val doneLight = Color(0xFF1B5E20).harmonize(primary)
        val queuedLight = Color(0xFFBF360C).harmonize(primary)
        val failedLight = Color(0xFFB3261E).harmonize(primary)

        val contrastDone = calculateContrastRatio(doneLight, surfaceLight)
        val contrastQueued = calculateContrastRatio(queuedLight, surfaceLight)
        val contrastFailed = calculateContrastRatio(failedLight, surfaceLight)

        assertTrue("Done text contrast ratio in Light mode ($contrastDone) should be >= 4.5", contrastDone >= 4.5)
        assertTrue("Queued text contrast ratio in Light mode ($contrastQueued) should be >= 4.5", contrastQueued >= 4.5)
        assertTrue("Failed text contrast ratio in Light mode ($contrastFailed) should be >= 4.5", contrastFailed >= 4.5)
    }

    @Test
    fun testStatusColorsContrastRatiosDarkMode() {
        val primary = Color(0xFFD0BCFF)
        val surfaceDark = Color(0xFF141218)

        val doneDark = Color(0xFF81C784).harmonize(primary)
        val queuedDark = Color(0xFFFFB74D).harmonize(primary)
        val failedDark = Color(0xFFF2B8B5).harmonize(primary)

        val contrastDone = calculateContrastRatio(doneDark, surfaceDark)
        val contrastQueued = calculateContrastRatio(queuedDark, surfaceDark)
        val contrastFailed = calculateContrastRatio(failedDark, surfaceDark)

        assertTrue("Done text contrast ratio in Dark mode ($contrastDone) should be >= 4.5", contrastDone >= 4.5)
        assertTrue("Queued text contrast ratio in Dark mode ($contrastQueued) should be >= 4.5", contrastQueued >= 4.5)
        assertTrue("Failed text contrast ratio in Dark mode ($contrastFailed) should be >= 4.5", contrastFailed >= 4.5)
    }
}
