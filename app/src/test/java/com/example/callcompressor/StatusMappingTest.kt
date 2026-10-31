package com.example.callcompressor

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusMappingTest {

    @Test
    fun testStatusMappingStrings() {
        val statusDone = Status.DONE
        val statusPending = Status.PENDING
        val statusFailed = Status.FAILED

        assertEquals("DONE", statusDone)
        assertEquals("PENDING", statusPending)
        assertEquals("FAILED", statusFailed)
    }

    @Test
    fun testQueuedReasonMapping() {
        val pendingRec = Recording(
            id = 10,
            sha256 = "hash10",
            uri = "uri10",
            name = "Call_Doctor_+112233445_20261030_140000.wav",
            sizeBytes = 200_000L,
            lastModified = System.currentTimeMillis(),
            status = Status.PENDING,
        )

        assertEquals(Status.PENDING, pendingRec.status)
    }
}
