package com.example.callcompressor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NameParserTest {

    @Test
    fun testParseStandardCallWithContactAndNumber() {
        val res = parseRecordingName("Call_Mom_+123456789_20261030_191300.m4a")
        assertEquals("Mom", res.contact)
        assertEquals("+123456789", res.number)
    }

    @Test
    fun testParseCallWithOnlyNumber() {
        val res = parseRecordingName("Call_+987654321_20261030_121500.wav")
        assertEquals("+987654321", res.contact)
        assertEquals("+987654321", res.number)
    }

    @Test
    fun testParseFallbackRawName() {
        val res = parseRecordingName("my_custom_call_file.mp3")
        assertEquals("my_custom_call_file", res.contact)
        assertNull(res.number)
    }

    @Test
    fun testMaskPhoneNumber() {
        assertEquals("•••••••789", maskPhoneNumber("+123456789"))
        assertEquals("123", maskPhoneNumber("123"))
        assertEquals("•••••••445", maskPhoneNumber("+112233445"))
    }
}
