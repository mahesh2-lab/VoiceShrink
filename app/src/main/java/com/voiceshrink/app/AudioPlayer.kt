package com.voiceshrink.app

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioPlayer(private val context: Context, private val scope: CoroutineScope) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    var currentRecording by mutableStateOf<Recording?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    fun play(recording: Recording) {
        val targetUri = recording.outUri?.let(Uri::parse) ?: Uri.parse(recording.uri)

        // If clicking the same recording while playing, toggle pause
        if (currentRecording?.id == recording.id && mediaPlayer != null) {
            if (isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }

        stop()

        try {
            val mp = MediaPlayer()
            mp.setDataSource(context, targetUri)
            mp.prepare()
            mp.setOnCompletionListener {
                stop()
            }
            mp.start()

            mediaPlayer = mp
            currentRecording = recording
            isPlaying = true
            durationMs = mp.duration.toLong().coerceAtLeast(0L)
            currentPositionMs = 0L

            startProgressTicker()
        } catch (e: Exception) {
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                progressJob?.cancel()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            isPlaying = true
            startProgressTicker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            currentPositionMs = positionMs
        }
    }

    fun seekForward(deltaMs: Long = 10_000L) {
        val newPos = (currentPositionMs + deltaMs).coerceAtMost(durationMs)
        seekTo(newPos)
    }

    fun seekBackward(deltaMs: Long = 10_000L) {
        val newPos = (currentPositionMs - deltaMs).coerceAtLeast(0L)
        seekTo(newPos)
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentRecording = null
        isPlaying = false
        currentPositionMs = 0L
        durationMs = 0L
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive && isPlaying) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        currentPositionMs = it.currentPosition.toLong()
                    }
                }
                delay(200)
            }
        }
    }

    fun release() {
        stop()
    }
}

