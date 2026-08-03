package com.mohnishraj.lunara.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAttachmentTest {
    @Test
    fun imageAndDocumentValidationEnforcesLimits() {
        val image = MediaAttachment(
            id = "image-1",
            kind = MediaKind.Image,
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 2_000_000,
            localUri = "file:///tmp/photo.jpg",
            width = 1200,
            height = 900,
        )
        assertTrue(image.validateForSend().isSuccess)
        assertTrue(image.copy(sizeBytes = MediaAttachment.MAX_IMAGE_BYTES + 1).validateForSend().isFailure)

        val document = image.copy(
            id = "document-1",
            kind = MediaKind.Document,
            fileName = "brief.pdf",
            mimeType = "application/pdf",
            width = 0,
            height = 0,
            sizeBytes = MediaAttachment.MAX_FILE_BYTES,
        )
        assertTrue(document.validateForSend().isSuccess)
    }

    @Test
    fun voiceValidationChecksDurationAndWaveform() {
        val voice = MediaAttachment(
            id = "voice-1",
            kind = MediaKind.Voice,
            fileName = "voice.m4a",
            mimeType = "audio/mp4",
            sizeBytes = 400_000,
            localUri = "file:///tmp/voice.m4a",
            durationMs = 8_500,
            waveform = listOf(10, 40, 90, 55),
        )
        assertTrue(voice.validateForSend().isSuccess)
        assertTrue(voice.copy(durationMs = 100).validateForSend().isFailure)
        assertTrue(voice.copy(waveform = listOf(101)).validateForSend().isFailure)
    }

    @Test
    fun previewAndFormattingRemainAccessible() {
        val attachment = MediaAttachment(
            id = "doc",
            kind = MediaKind.Document,
            fileName = "Roadmap.pdf",
            mimeType = "application/pdf",
            sizeBytes = 2_500_000,
            caption = "Review before Friday",
            remotePath = "conversation/user/client/Roadmap.pdf",
        )
        assertEquals("Document · Roadmap.pdf · Review before Friday", attachment.previewText())
        assertTrue(formatFileSize(attachment.sizeBytes).endsWith("MB"))
        assertEquals("1:05", formatDuration(65_000))
    }
}
