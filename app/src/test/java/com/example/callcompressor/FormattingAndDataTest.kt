package com.example.callcompressor

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingAndDataTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("500 B", formatBytes(500))
        assertEquals("1 KB", formatBytes(1024))
        assertEquals("1.5 MB", formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.0 GB", formatBytes((2.0 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testFormatDuration() {
        assertEquals("00:00", formatDuration(0))
        assertEquals("00:05", formatDuration(5000))
        assertEquals("01:00", formatDuration(60000))
        assertEquals("03:45", formatDuration((3 * 60 + 45) * 1000L))
        assertEquals("65:10", formatDuration((65 * 60 + 10) * 1000L))
    }

    @Test
    fun testSavingsCalculation() {
        val originalBytes = 10_000_000L
        val compressedBytes = 1_500_000L
        val savedBytes = (originalBytes - compressedBytes).coerceAtLeast(0L)
        val savedPercentage = if (originalBytes > 0) ((savedBytes.toDouble() / originalBytes) * 100).toInt() else 0

        assertEquals(8_500_000L, savedBytes)
        assertEquals(85, savedPercentage)
    }
}
