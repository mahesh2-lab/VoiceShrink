package com.voiceshrink.app

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Decodes any audio file Android can read and re-encodes it as mono Opus in an Ogg container
 * (MediaCodec Opus encoder + Ogg muxer, both need Android 10 / API 29).
 *
 * Output sample rate: 16 kHz (or 12/8 kHz if the source is lower). Stereo is averaged to mono.
 * Returns true when finished, false if [shouldContinue] turned false (e.g. charger unplugged).
 */
class OpusCompressor(private val context: Context) {

    fun compress(input: Uri, output: File, bitrate: Int, shouldContinue: () -> Boolean): Boolean {
        val extractor = MediaExtractor()
        var pfd: ParcelFileDescriptor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var muxerStopped = false

        try {
            // Open via ParcelFileDescriptor for reliable SAF content:// support across all OEMs
            pfd = context.contentResolver.openFileDescriptor(input, "r")
                ?: error("Cannot open input uri: $input")
            extractor.setDataSource(pfd.fileDescriptor)

            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("No audio track found in $input")
            extractor.selectTrack(track)
            val inFormat = extractor.getTrackFormat(track)

            val mime = inFormat.getString(MediaFormat.KEY_MIME) ?: error("Missing audio mime type")
            val dec = MediaCodec.createDecoderByType(mime)
            dec.configure(inFormat, null, null, 0)
            dec.start()
            decoder = dec

            val fifo = ShortFifo()
            val chunk = ShortArray(16384)
            val info = MediaCodec.BufferInfo()

            var resampler: Resampler? = null
            var srcChannels = 1
            var outRate = 16000
            var frameSamples = 320 // 20ms frame at 16kHz
            var muxTrack = -1
            var ptsSamples = 0L
            var extractorDone = false
            var decoderDone = false
            var eosSent = false
            var encoderDone = false

            // The encoder is created once the decoder reports its real output format.
            fun ensureEncoder() {
                if (encoder != null) return
                val of = dec.outputFormat
                val srcRate = if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else {
                    44100
                }
                srcChannels = if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    of.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                } else {
                    1
                }
                outRate = when {
                    srcRate >= 16000 -> 16000
                    srcRate >= 12000 -> 12000
                    else -> 8000
                }
                frameSamples = outRate * 20 / 1000 // Standard 20ms Opus frame duration
                resampler = Resampler(srcRate, outRate, fifo)

                val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, outRate, 1)
                fmt.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                val e = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
                e.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                e.start()
                encoder = e
                muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
            }

            while (!encoderDone) {
                if (!shouldContinue()) return false

                // 1. compressed bytes -> decoder
                if (!extractorDone) {
                    val i = dec.dequeueInputBuffer(0)
                    if (i >= 0) {
                        val buf = dec.getInputBuffer(i)!!
                        val n = extractor.readSampleData(buf, 0)
                        if (n < 0) {
                            dec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            extractorDone = true
                        } else {
                            dec.queueInputBuffer(i, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // 2. decoder -> PCM -> mono -> resample -> fifo (back-pressure when fifo is large)
                if (!decoderDone && fifo.size < 100_000) {
                    val o = dec.dequeueOutputBuffer(info, if (encoder == null) 5_000 else 0)
                    if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        ensureEncoder()
                    } else if (o >= 0) {
                        ensureEncoder()
                        if (info.size > 0) {
                            val ob = dec.getOutputBuffer(o)!!
                            ob.position(info.offset)
                            ob.limit(info.offset + info.size)
                            val sb = ob.order(ByteOrder.nativeOrder()).asShortBuffer()
                            val pcm = ShortArray(sb.remaining())
                            sb.get(pcm)

                            val rs = resampler!!
                            val frames = pcm.size / srcChannels
                            var k = 0
                            for (f in 0 until frames) {
                                var sum = 0
                                for (c in 0 until srcChannels) sum += pcm[k++]
                                rs.push(sum.toFloat() / srcChannels)
                            }
                        }
                        dec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) decoderDone = true
                    }
                }

                val enc = encoder ?: continue

                // 3. fifo -> encoder in standard 20ms frame blocks
                while (!eosSent && (fifo.size >= frameSamples || (decoderDone && fifo.size > 0) || (decoderDone && fifo.size == 0))) {
                    val i = enc.dequeueInputBuffer(0)
                    if (i < 0) break
                    val buf = enc.getInputBuffer(i)!!
                    buf.clear()

                    val available = fifo.size
                    val n = if (available >= frameSamples) {
                        frameSamples
                    } else if (decoderDone && available > 0) {
                        available
                    } else {
                        0
                    }

                    val ptsUs = ptsSamples * 1_000_000L / outRate
                    if (n > 0) {
                        fifo.take(chunk, n)
                        buf.order(ByteOrder.nativeOrder()).asShortBuffer().put(chunk, 0, n)
                        enc.queueInputBuffer(i, 0, n * 2, ptsUs, 0)
                        ptsSamples += n
                    } else if (decoderDone) {
                        enc.queueInputBuffer(i, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        eosSent = true
                    }
                }

                // 4. encoder -> Ogg muxer
                var timeout = 2_000L
                while (true) {
                    val eo = enc.dequeueOutputBuffer(info, timeout)
                    timeout = 0
                    if (eo == MediaCodec.INFO_TRY_AGAIN_LATER) break
                    if (eo == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        muxTrack = muxer!!.addTrack(enc.outputFormat)
                        muxer!!.start()
                        muxerStarted = true
                        continue
                    }
                    if (eo < 0) continue
                    val ob = enc.getOutputBuffer(eo)!!
                    val isConfig = info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!isConfig && info.size > 0 && muxerStarted) {
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        muxer!!.writeSampleData(muxTrack, ob, info)
                    }
                    enc.releaseOutputBuffer(eo, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        encoderDone = true
                        break
                    }
                }
            }

            if (!muxerStarted) error("Encoder produced no output")
            muxer!!.stop()
            muxerStopped = true
            return true
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { if (muxerStarted && !muxerStopped) muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            runCatching { pfd?.close() }
        }
    }
}

/** Growable FIFO of 16-bit samples. */
private class ShortFifo {
    private var a = ShortArray(1 shl 16)
    private var head = 0
    private var tail = 0
    val size: Int get() = tail - head

    fun add(s: Short) {
        if (tail == a.size) {
            val n = size
            if (head > 0) {
                System.arraycopy(a, head, a, 0, n)
                head = 0
                tail = n
            } else {
                a = a.copyOf(a.size * 2)
            }
        }
        a[tail++] = s
    }

    fun take(dst: ShortArray, max: Int): Int {
        val n = minOf(max, size)
        System.arraycopy(a, head, dst, 0, n)
        head += n
        if (head == tail) {
            head = 0
            tail = 0
        }
        return n
    }
}

/**
 * Streaming area-averaging resampler (box filter): averages the input samples that fall inside
 * each output sample, which doubles as a simple anti-alias filter when downsampling.
 */
private class Resampler(inRate: Int, outRate: Int, private val sink: ShortFifo) {
    private val step = inRate.toDouble() / outRate
    private var t = 0.0
    private var boundary = step
    private var sum = 0.0

    fun push(x: Float) {
        val end = t + 1.0
        var s = t
        while (end >= boundary) {
            sum += x * (boundary - s)
            sink.add((sum / step).roundToInt().coerceIn(-32768, 32767).toShort())
            sum = 0.0
            s = boundary
            boundary += step
        }
        sum += x * (end - s)
        t = end
    }
}
