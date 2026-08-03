package com.mohnishraj.lunara.data.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import java.util.UUID

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L
    private var paused = false

    val isRecording: Boolean get() = recorder != null
    val isPaused: Boolean get() = isRecording && paused

    @Suppress("DEPRECATION")
    fun start(): Result<File> = runCatching {
        check(recorder == null) { "A voice note is already being recorded" }
        val directory = File(context.cacheDir, "lunara_media/recordings").apply { mkdirs() }
        val file = File(directory, "recording-${UUID.randomUUID()}.m4a")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        outputFile = file
        startedAt = SystemClock.elapsedRealtime()
        pausedAt = 0L
        pausedTotal = 0L
        paused = false
        file
    }

    fun pause(): Result<Unit> = runCatching {
        val active = recorder ?: error("No active recording")
        check(!paused) { "Recording is already paused" }
        active.pause()
        pausedAt = SystemClock.elapsedRealtime()
        paused = true
    }

    fun resume(): Result<Unit> = runCatching {
        val active = recorder ?: error("No active recording")
        check(paused) { "Recording is not paused" }
        active.resume()
        pausedTotal += SystemClock.elapsedRealtime() - pausedAt
        pausedAt = 0L
        paused = false
    }

    fun currentAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun elapsedMs(): Long {
        if (!isRecording) return 0L
        val now = if (paused) pausedAt else SystemClock.elapsedRealtime()
        return (now - startedAt - pausedTotal).coerceAtLeast(0L)
    }

    fun stop(): Result<VoiceRecording> {
        val active = recorder ?: return Result.failure(IllegalStateException("No active recording"))
        val file = outputFile ?: return Result.failure(IllegalStateException("Recording file is missing"))
        val duration = elapsedMs()
        return runCatching {
            active.stop()
            require(file.isFile && file.length() > 0L) { "Voice recording is empty" }
            VoiceRecording(file, duration)
        }.onFailure {
            file.delete()
        }.also {
            runCatching { active.reset() }
            runCatching { active.release() }
            recorder = null
            outputFile = null
            paused = false
            startedAt = 0L
            pausedAt = 0L
            pausedTotal = 0L
        }
    }

    fun cancel() = reset(deleteFile = true)

    private fun reset(deleteFile: Boolean) {
        runCatching { recorder?.stop() }
        runCatching { recorder?.reset() }
        runCatching { recorder?.release() }
        recorder = null
        if (deleteFile) outputFile?.delete()
        outputFile = null
        paused = false
        startedAt = 0L
        pausedAt = 0L
        pausedTotal = 0L
    }

    data class VoiceRecording(val file: File, val durationMs: Long)
}
