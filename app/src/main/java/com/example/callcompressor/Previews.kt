package com.example.callcompressor

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val sampleDoneRecording = Recording(
    id = 1,
    sha256 = "abc123hash",
    uri = "content://com.android.externalstorage.documents/tree/primary%3ARecordings/document/Call_Mom.m4a",
    name = "Call_Mom_+123456789_20261030.m4a",
    sizeBytes = 125_000L,
    lastModified = System.currentTimeMillis() - 3600_000L,
    status = Status.DONE,
    outSizeBytes = 17_000L,
    outUri = "content://com.android.externalstorage.documents/tree/primary%3ARecordings/document/compressed/Call_Mom.ogg",
    durationMs = 185_000L,
)

val sampleQueuedRecording = Recording(
    id = 2,
    sha256 = "def456hash",
    uri = "content://com.android.externalstorage.documents/tree/primary%3ARecordings/document/Call_Work.wav",
    name = "Call_Work_+987654321_20261030.wav",
    sizeBytes = 500_000L,
    lastModified = System.currentTimeMillis() - 7200_000L,
    status = Status.PENDING,
    durationMs = 240_000L,
)

val sampleFailedRecording = Recording(
    id = 3,
    sha256 = "ghi789hash",
    uri = "content://com.android.externalstorage.documents/tree/primary%3ARecordings/document/Call_Doctor.mp3",
    name = "Call_Doctor_+112233445_20261030.mp3",
    sizeBytes = 250_000L,
    lastModified = System.currentTimeMillis() - 10800_000L,
    status = Status.FAILED,
    error = "Decoder initialization failed",
    durationMs = 95_000L,
)

@Composable
private fun PreviewWrapper(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Preview(name = "RecordingCard - Done - Light", showBackground = true)
@Composable
fun PreviewRecordingCardDoneLight() {
    PreviewWrapper {
        RecordingCard(
            recording = sampleDoneRecording,
            isPlaying = false,
            hideNumbers = false,
            onPlayToggle = {},
            onShowDetails = {},
            onShare = {},
            onDelete = {},
            onRetry = {},
            onCompressNow = {},
        )
    }
}

@Preview(name = "RecordingCard - Queued - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewRecordingCardQueuedDark() {
    PreviewWrapper {
        RecordingCard(
            recording = sampleQueuedRecording,
            isPlaying = false,
            hideNumbers = false,
            onPlayToggle = {},
            onShowDetails = {},
            onShare = {},
            onDelete = {},
            onRetry = {},
            onCompressNow = {},
        )
    }
}

@Preview(name = "RecordingCard - Failed - Font 150%", fontScale = 1.5f, showBackground = true)
@Composable
fun PreviewRecordingCardFailedFont150() {
    PreviewWrapper {
        RecordingCard(
            recording = sampleFailedRecording,
            isPlaying = false,
            hideNumbers = true,
            onPlayToggle = {},
            onShowDetails = {},
            onShare = {},
            onDelete = {},
            onRetry = {},
            onCompressNow = {},
        )
    }
}

@Preview(name = "EmptyStateNoFolder - Light", showBackground = true)
@Composable
fun PreviewEmptyStateNoFolderLight() {
    PreviewWrapper {
        EmptyStateNoFolder(onPickFolder = {})
    }
}

@Preview(name = "EmptyStateNoFolder - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
fun PreviewEmptyStateNoFolderDark() {
    PreviewWrapper {
        EmptyStateNoFolder(onPickFolder = {})
    }
}

@Preview(name = "EmptyStateNoFolder - Font 150%", fontScale = 1.5f, showBackground = true)
@Composable
fun PreviewEmptyStateNoFolderFont150() {
    PreviewWrapper {
        EmptyStateNoFolder(onPickFolder = {})
    }
}
