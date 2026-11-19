package com.example.streaming

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.view.Surface
import com.example.broadcast.BroadcastOverlayRenderer
import com.example.broadcast.RenderSnapshot
import com.example.broadcast.StreamStatus
import com.example.broadcast.StreamTelemetry
import com.example.data.StreamDestinationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

class RtmpLiveStreamer(
    private val context: Context,
    private val renderer: BroadcastOverlayRenderer
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var streamJob: Job? = null

    private val _telemetry = MutableStateFlow(StreamTelemetry())
    val telemetry: StateFlow<StreamTelemetry> = _telemetry.asStateFlow()

    private val clients = mutableListOf<RtmpClient>()
    private val totalBytesSent = AtomicLong(0L)

    @Volatile
    private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun startStream(
        destinations: List<StreamDestinationEntity>,
        hasAudioPermission: Boolean,
        snapshotProvider: () -> RenderSnapshot,
        cameraFrameProvider: () -> Bitmap?
    ) {
        if (streamJob?.isActive == true) return
        val armed = destinations.filter { it.isArmed && it.streamKey.isNotBlank() && it.rtmpUrl.isNotBlank() }
        if (armed.isEmpty()) {
            _telemetry.value = StreamTelemetry(
                status = StreamStatus.ERROR,
                statusMessage = "Enter a valid Stream Key for at least one armed social platform."
            )
            return
        }

        val primary = armed.first()
        val (width, height, fps) = when (primary.resolutionPreset) {
            "720P_60" -> Triple(1280, 720, 60)
            "720P_30" -> Triple(1280, 720, 30)
            else -> Triple(1280, 720, 30) // Reliable hardware encoder profile across all devices
        }
        val bitrateKbps = primary.bitrateKbps.coerceIn(1200, 8000)

        _telemetry.value = StreamTelemetry(
            status = StreamStatus.CONNECTING,
            activePlatforms = armed.map { it.displayName },
            statusMessage = "Handshaking with ${armed.joinToString(", ") { it.displayName }}..."
        )

        totalBytesSent.set(0L)
        streamJob = scope.launch {
            try {
                val activeClients = mutableListOf<RtmpClient>()
                for (dest in armed) {
                    val client = RtmpClient()
                    client.connectAndPublish(
                        fullRtmpUrl = dest.rtmpUrl,
                        streamKey = dest.streamKey,
                        width = width,
                        height = height,
                        fps = fps,
                        videoBitrateKbps = bitrateKbps
                    )
                    activeClients.add(client)
                }
                synchronized(clients) {
                    clients.clear()
                    clients.addAll(activeClients)
                }

                BroadcastForegroundService.start(
                    context,
                    "Asian Press Live • ON AIR",
                    "Streaming to ${armed.joinToString(", ") { it.displayName }}"
                )

                val startMs = System.currentTimeMillis()
                _telemetry.value = StreamTelemetry(
                    status = StreamStatus.LIVE,
                    activePlatforms = armed.map { it.displayName },
                    bitrateKbps = bitrateKbps,
                    fps = fps,
                    statusMessage = "Live on ${armed.joinToString(", ") { it.displayName }}"
                )

                val audioJob = if (hasAudioPermission) {
                    launch { runAudioEncoder(startMs) }
                } else null

                val telemetryJob = launch {
                    var lastBytes = 0L
                    while (isActive) {
                        delay(1000L)
                        val curBytes = totalBytesSent.get()
                        val deltaBytes = curBytes - lastBytes
                        lastBytes = curBytes
                        val kbps = ((deltaBytes * 8) / 1000).toInt()
                        val elapsed = (System.currentTimeMillis() - startMs) / 1000L
                        _telemetry.value = _telemetry.value.copy(
                            status = StreamStatus.LIVE,
                            bitrateKbps = if (kbps > 0) kbps else bitrateKbps,
                            bytesSent = curBytes,
                            elapsedSeconds = elapsed
                        )
                    }
                }

                runVideoEncoder(width, height, fps, bitrateKbps, startMs, snapshotProvider, cameraFrameProvider)

                audioJob?.cancel()
                telemetryJob.cancel()
            } catch (e: Exception) {
                _telemetry.value = StreamTelemetry(
                    status = StreamStatus.ERROR,
                    activePlatforms = armed.map { it.displayName },
                    statusMessage = e.message ?: "RTMP connection failed"
                )
            } finally {
                cleanupClients()
                BroadcastForegroundService.stop(context)
            }
        }
    }

    fun stopStream() {
        streamJob?.cancel()
        streamJob = null
        cleanupClients()
        BroadcastForegroundService.stop(context)
        _telemetry.value = StreamTelemetry(
            status = StreamStatus.IDLE,
            statusMessage = "Stream ended"
        )
    }

    private fun runVideoEncoder(
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        startMs: Long,
        snapshotProvider: () -> RenderSnapshot,
        cameraFrameProvider: () -> Bitmap?
    ) {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface: Surface = encoder.createInputSurface()
        encoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var spsSent = false
        val frameIntervalMs = (1000L / fps).coerceAtLeast(16L)

        try {
            while (streamJob?.isActive == true) {
                val frameStart = System.currentTimeMillis()
                val canvas = inputSurface.lockCanvas(null)
                try {
                    val camBmp = cameraFrameProvider()
                    if (camBmp != null && !camBmp.isRecycled) {
                        canvas.drawBitmap(
                            camBmp,
                            Rect(0, 0, camBmp.width, camBmp.height),
                            RectF(0f, 0f, width.toFloat(), height.toFloat()),
                            null
                        )
                    }
                    renderer.drawFrame(
                        canvas = canvas,
                        snapshot = snapshotProvider(),
                        clearTransparent = false,
                        includeGuides = false
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                var index = encoder.dequeueOutputBuffer(bufferInfo, 0)
                while (index >= 0) {
                    val encoded = encoder.getOutputBuffer(index)
                    if (encoded != null && bufferInfo.size > 0) {
                        encoded.position(bufferInfo.offset)
                        encoded.limit(bufferInfo.offset + bufferInfo.size)
                        val bytes = ByteArray(bufferInfo.size)
                        encoded.get(bytes)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            val (sps, pps) = extractSpsPps(bytes)
                            if (sps != null && pps != null) {
                                broadcastAll { it.sendVideoSequenceHeader(sps, pps) }
                                spsSent = true
                            }
                        } else if (spsSent) {
                            val avcc = annexBToAvcc(bytes)
                            val isKey = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                            val ptsMs = System.currentTimeMillis() - startMs
                            broadcastAll { it.sendVideoFrame(avcc, isKey, ptsMs) }
                            totalBytesSent.addAndGet(avcc.size.toLong())
                        }
                    }
                    encoder.releaseOutputBuffer(index, false)
                    index = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }

                val spent = System.currentTimeMillis() - frameStart
                if (spent < frameIntervalMs) {
                    Thread.sleep(frameIntervalMs - spent)
                }
            }
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching { inputSurface.release() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun runAudioEncoder(startMs: Long) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_STEREO
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
            .coerceAtLeast(4096)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) return

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBuf * 2)
        }
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        audioRecord.startRecording()

        val pcmBuffer = ByteArray(4096)
        val bufferInfo = MediaCodec.BufferInfo()
        var ascSent = false

        try {
            while (streamJob?.isActive == true) {
                val read = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)
                if (read > 0) {
                    if (isMuted) pcmBuffer.fill(0, 0, read)
                    val inIdx = encoder.dequeueInputBuffer(5000)
                    if (inIdx >= 0) {
                        val inBuf = encoder.getInputBuffer(inIdx)
                        inBuf?.clear()
                        inBuf?.put(pcmBuffer, 0, read)
                        val ptsUs = (System.currentTimeMillis() - startMs) * 1000L
                        encoder.queueInputBuffer(inIdx, 0, read, ptsUs, 0)
                    }
                }

                var outIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                while (outIdx >= 0) {
                    val outBuf = encoder.getOutputBuffer(outIdx)
                    if (outBuf != null && bufferInfo.size > 0) {
                        outBuf.position(bufferInfo.offset)
                        outBuf.limit(bufferInfo.offset + bufferInfo.size)
                        val bytes = ByteArray(bufferInfo.size)
                        outBuf.get(bytes)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            broadcastAll { it.sendAudioSequenceHeader(bytes) }
                            ascSent = true
                        } else if (ascSent) {
                            val ptsMs = System.currentTimeMillis() - startMs
                            broadcastAll { it.sendAudioFrame(bytes, ptsMs) }
                            totalBytesSent.addAndGet(bytes.size.toLong())
                        }
                    }
                    encoder.releaseOutputBuffer(outIdx, false)
                    outIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }
            }
        } finally {
            runCatching { audioRecord.stop() }
            runCatching { audioRecord.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
        }
    }

    private fun broadcastAll(action: (RtmpClient) -> Unit) {
        synchronized(clients) {
            for (client in clients) {
                if (client.isConnected) {
                    action(client)
                }
            }
        }
    }

    private fun cleanupClients() {
        synchronized(clients) {
            clients.forEach { it.close() }
            clients.clear()
        }
    }

    private fun extractSpsPps(configBytes: ByteArray): Pair<ByteArray?, ByteArray?> {
        val nalus = splitAnnexBNalus(configBytes)
        val sps = nalus.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 7 }
        val pps = nalus.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 8 }
        return sps to pps
    }

    private fun annexBToAvcc(annexB: ByteArray): ByteArray {
        val nalus = splitAnnexBNalus(annexB)
        if (nalus.isEmpty()) return annexB
        val total = nalus.sumOf { 4 + it.size }
        val out = ByteBuffer.allocate(total)
        for (nal in nalus) {
            out.putInt(nal.size)
            out.put(nal)
        }
        return out.array()
    }

    private fun splitAnnexBNalus(data: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        var i = 0
        var start = -1
        while (i < data.size - 3) {
            val is4 = data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 0 && data[i + 3].toInt() == 1
            val is3 = !is4 && data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 1
            if (is4 || is3) {
                if (start >= 0 && i > start) {
                    result.add(data.copyOfRange(start, i))
                }
                i += if (is4) 4 else 3
                start = i
            } else {
                i++
            }
        }
        if (start >= 0 && start < data.size) {
            result.add(data.copyOfRange(start, data.size))
        }
        return result
    }
}
