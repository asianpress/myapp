package com.example.streaming

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.net.ssl.SSLSocketFactory

/**
 * Lightweight RTMP/RTMPS client supporting C0/C1/C2 handshake, AMF0 publish sequence,
 * and FLV-wrapped H.264 video + AAC audio packet transmission.
 */
class RtmpClient {
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: BufferedOutputStream? = null
    private var transactionId = 1.0
    private var streamId = 1
    private var chunkSize = 4096

    @Volatile
    var isConnected: Boolean = false
        private set

    fun connectAndPublish(fullRtmpUrl: String, streamKey: String, width: Int, height: Int, fps: Int, videoBitrateKbps: Int) {
        val parsed = parseRtmpUrl(fullRtmpUrl, streamKey)
        val rawSocket = if (parsed.useTls) {
            SSLSocketFactory.getDefault().createSocket()
        } else {
            Socket()
        }
        rawSocket.soTimeout = 10_000
        rawSocket.tcpNoDelay = true
        rawSocket.connect(InetSocketAddress(parsed.host, parsed.port), 8_000)

        socket = rawSocket
        input = DataInputStream(BufferedInputStream(rawSocket.getInputStream()))
        output = BufferedOutputStream(rawSocket.getOutputStream())

        performHandshake()
        sendSetChunkSize(chunkSize)

        sendConnectCommand(parsed.app, parsed.tcUrl)
        drainResponses(expectTransactionId = 1.0)

        sendReleaseStream(parsed.streamName)
        sendFCPublish(parsed.streamName)
        sendCreateStream()
        streamId = readCreateStreamResult()

        sendPublish(parsed.streamName)
        drainResponses(expectTransactionId = null, maxReadLoops = 3)

        sendMetaData(width, height, fps, videoBitrateKbps)
        isConnected = true
    }

    fun sendVideoSequenceHeader(sps: ByteArray, pps: ByteArray) {
        if (!isConnected) return
        val payload = ByteArrayOutputStream().apply {
            write(0x17) // Keyframe + AVC
            write(0x00) // AVC sequence header
            write(0x00); write(0x00); write(0x00) // composition time
            write(0x01) // configurationVersion
            write(if (sps.size > 1) sps[1].toInt() else 0x42)
            write(if (sps.size > 2) sps[2].toInt() else 0xC0)
            write(if (sps.size > 3) sps[3].toInt() else 0x1E)
            write(0xFF) // 4 bytes lengthSizeMinusOne
            write(0xE1) // 1 SPS
            write((sps.size shr 8) and 0xFF)
            write(sps.size and 0xFF)
            write(sps)
            write(0x01) // 1 PPS
            write((pps.size shr 8) and 0xFF)
            write(pps.size and 0xFF)
            write(pps)
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 6, timestampMs = 0, messageType = 0x09, streamId = streamId, payload = payload)
    }

    fun sendVideoFrame(avccData: ByteArray, isKeyFrame: Boolean, timestampMs: Long) {
        if (!isConnected) return
        val payload = ByteArray(5 + avccData.size)
        payload[0] = if (isKeyFrame) 0x17 else 0x27
        payload[1] = 0x01 // AVC NALU
        payload[2] = 0x00
        payload[3] = 0x00
        payload[4] = 0x00
        System.arraycopy(avccData, 0, payload, 5, avccData.size)
        writeRtmpMessage(chunkStreamId = 6, timestampMs = timestampMs, messageType = 0x09, streamId = streamId, payload = payload)
    }

    fun sendAudioSequenceHeader(aacConfig: ByteArray) {
        if (!isConnected) return
        val payload = ByteArray(2 + aacConfig.size)
        payload[0] = 0xAF.toByte() // AAC, 44kHz, 16-bit, stereo
        payload[1] = 0x00 // AAC sequence header
        System.arraycopy(aacConfig, 0, payload, 2, aacConfig.size)
        writeRtmpMessage(chunkStreamId = 4, timestampMs = 0, messageType = 0x08, streamId = streamId, payload = payload)
    }

    fun sendAudioFrame(aacFrame: ByteArray, timestampMs: Long) {
        if (!isConnected) return
        val payload = ByteArray(2 + aacFrame.size)
        payload[0] = 0xAF.toByte()
        payload[1] = 0x01 // Raw AAC frame
        System.arraycopy(aacFrame, 0, payload, 2, aacFrame.size)
        writeRtmpMessage(chunkStreamId = 4, timestampMs = timestampMs, messageType = 0x08, streamId = streamId, payload = payload)
    }

    fun close() {
        isConnected = false
        runCatching { output?.flush() }
        runCatching { socket?.close() }
        socket = null
        input = null
        output = null
    }

    private fun performHandshake() {
        val out = output ?: throw IOException("Output stream closed")
        val inp = input ?: throw IOException("Input stream closed")

        // C0 + C1
        out.write(0x03)
        val c1 = ByteArray(1536)
        SecureRandom().nextBytes(c1)
        // Zero timestamp & version bytes
        for (i in 0 until 8) c1[i] = 0
        out.write(c1)
        out.flush()

        // Read S0 + S1 + S2
        val s0 = inp.readByte().toInt()
        if (s0 != 0x03) throw IOException("Unsupported RTMP version: $s0")
        val s1 = ByteArray(1536)
        inp.readFully(s1)
        val s2 = ByteArray(1536)
        inp.readFully(s2)

        // C2 echoes S1
        out.write(s1)
        out.flush()
    }

    private fun sendSetChunkSize(size: Int) {
        val payload = ByteBuffer.allocate(4).putInt(size).array()
        writeRtmpMessage(chunkStreamId = 2, timestampMs = 0, messageType = 0x01, streamId = 0, payload = payload)
    }

    private fun sendConnectCommand(app: String, tcUrl: String) {
        val amf = ByteArrayOutputStream().apply {
            writeAmfString("connect")
            writeAmfNumber(nextTx())
            writeAmfObjectStart()
            writeAmfProperty("app", app)
            writeAmfProperty("type", "nonprivate")
            writeAmfProperty("flashVer", "FMLE/3.0 (compatible; AsianPressLive)")
            writeAmfProperty("tcUrl", tcUrl)
            writeAmfObjectEnd()
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 3, timestampMs = 0, messageType = 0x14, streamId = 0, payload = amf)
    }

    private fun sendReleaseStream(streamName: String) {
        val amf = ByteArrayOutputStream().apply {
            writeAmfString("releaseStream")
            writeAmfNumber(nextTx())
            write(0x05) // null
            writeAmfString(streamName)
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 3, timestampMs = 0, messageType = 0x14, streamId = 0, payload = amf)
    }

    private fun sendFCPublish(streamName: String) {
        val amf = ByteArrayOutputStream().apply {
            writeAmfString("FCPublish")
            writeAmfNumber(nextTx())
            write(0x05) // null
            writeAmfString(streamName)
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 3, timestampMs = 0, messageType = 0x14, streamId = 0, payload = amf)
    }

    private fun sendCreateStream() {
        val amf = ByteArrayOutputStream().apply {
            writeAmfString("createStream")
            writeAmfNumber(nextTx())
            write(0x05) // null
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 3, timestampMs = 0, messageType = 0x14, streamId = 0, payload = amf)
    }

    private fun readCreateStreamResult(): Int {
        drainResponses(expectTransactionId = transactionId - 1, maxReadLoops = 5)
        return 1
    }

    private fun sendPublish(streamName: String) {
        val amf = ByteArrayOutputStream().apply {
            writeAmfString("publish")
            writeAmfNumber(nextTx())
            write(0x05) // null
            writeAmfString(streamName)
            writeAmfString("live")
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 8, timestampMs = 0, messageType = 0x14, streamId = 1, payload = amf)
    }

    private fun sendMetaData(width: Int, height: Int, fps: Int, videoBitrateKbps: Int) {
        val amf = ByteArrayOutputStream().apply {
            writeAmfString("@setDataFrame")
            writeAmfString("onMetaData")
            write(0x08) // ECMA array
            writeInt32(6)
            writeAmfPropName("width"); writeAmfNumber(width.toDouble())
            writeAmfPropName("height"); writeAmfNumber(height.toDouble())
            writeAmfPropName("videodatarate"); writeAmfNumber(videoBitrateKbps.toDouble())
            writeAmfPropName("framerate"); writeAmfNumber(fps.toDouble())
            writeAmfPropName("videocodecid"); writeAmfNumber(7.0) // AVC
            writeAmfPropName("audiocodecid"); writeAmfNumber(10.0) // AAC
            writeAmfObjectEnd()
        }.toByteArray()
        writeRtmpMessage(chunkStreamId = 4, timestampMs = 0, messageType = 0x12, streamId = streamId, payload = amf)
    }

    private fun drainResponses(expectTransactionId: Double?, maxReadLoops: Int = 8) {
        val inp = input ?: return
        var loops = 0
        while (loops < maxReadLoops) {
            loops++
            val firstByte = runCatching { inp.readUnsignedByte() }.getOrNull() ?: return
            val fmt = (firstByte shr 6) and 0x03
            var csid = firstByte and 0x3F
            if (csid == 0) csid = inp.readUnsignedByte() + 64
            else if (csid == 1) {
                val b1 = inp.readUnsignedByte()
                val b2 = inp.readUnsignedByte()
                csid = b2 * 256 + b1 + 64
            }

            var messageLength = 0
            var messageType = 0
            when (fmt) {
                0 -> {
                    val hdr = ByteArray(11)
                    inp.readFully(hdr)
                    messageLength = ((hdr[3].toInt() and 0xFF) shl 16) or
                        ((hdr[4].toInt() and 0xFF) shl 8) or
                        (hdr[5].toInt() and 0xFF)
                    messageType = hdr[6].toInt() and 0xFF
                }
                1 -> {
                    val hdr = ByteArray(7)
                    inp.readFully(hdr)
                    messageLength = ((hdr[3].toInt() and 0xFF) shl 16) or
                        ((hdr[4].toInt() and 0xFF) shl 8) or
                        (hdr[5].toInt() and 0xFF)
                    messageType = hdr[6].toInt() and 0xFF
                }
                2 -> {
                    val hdr = ByteArray(3)
                    inp.readFully(hdr)
                }
                3 -> Unit
            }

            if (messageLength > 0 && messageLength < 65536) {
                val body = ByteArray(minOf(messageLength, chunkSize))
                inp.readFully(body)
                if (messageType == 0x01 && body.size >= 4) {
                    val newChunk = ByteBuffer.wrap(body).int
                    if (newChunk in 128..65536) chunkSize = newChunk
                }
                val text = body.decodeToString()
                if (text.contains("_error") || text.contains("NetStream.Publish.BadName")) {
                    throw IOException("RTMP server rejected stream key or path")
                }
                if (expectTransactionId != null && text.contains("_result")) {
                    return
                }
                if (text.contains("NetStream.Publish.Start")) {
                    return
                }
            }
            if (csid < 0) break
        }
    }

    @Synchronized
    private fun writeRtmpMessage(chunkStreamId: Int, timestampMs: Long, messageType: Int, streamId: Int, payload: ByteArray) {
        val out = output ?: throw IOException("Socket not open")
        val ts = timestampMs.coerceAtLeast(0L).toInt()
        val len = payload.size
        var offset = 0
        var firstChunk = true

        while (offset < len) {
            val bytesThisChunk = minOf(chunkSize, len - offset)
            if (firstChunk) {
                out.write(chunkStreamId and 0x3F) // fmt 0
                out.write((ts shr 16) and 0xFF)
                out.write((ts shr 8) and 0xFF)
                out.write(ts and 0xFF)
                out.write((len shr 16) and 0xFF)
                out.write((len shr 8) and 0xFF)
                out.write(len and 0xFF)
                out.write(messageType and 0xFF)
                // Stream ID little-endian
                out.write(streamId and 0xFF)
                out.write((streamId shr 8) and 0xFF)
                out.write((streamId shr 16) and 0xFF)
                out.write((streamId shr 24) and 0xFF)
                firstChunk = false
            } else {
                out.write(0xC0 or (chunkStreamId and 0x3F)) // fmt 3 continuation
            }
            out.write(payload, offset, bytesThisChunk)
            offset += bytesThisChunk
        }
        out.flush()
    }

    private fun nextTx(): Double {
        val current = transactionId
        transactionId += 1.0
        return current
    }

    private fun ByteArrayOutputStream.writeAmfString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        write(0x02)
        write((bytes.size shr 8) and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeAmfPropName(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        write((bytes.size shr 8) and 0xFF)
        write(bytes.size and 0xFF)
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeAmfNumber(value: Double) {
        write(0x00)
        write(ByteBuffer.allocate(8).putDouble(value).array())
    }

    private fun ByteArrayOutputStream.writeAmfObjectStart() = write(0x03)

    private fun ByteArrayOutputStream.writeAmfObjectEnd() {
        write(0x00); write(0x00); write(0x09)
    }

    private fun ByteArrayOutputStream.writeAmfProperty(key: String, value: String) {
        writeAmfPropName(key)
        writeAmfString(value)
    }

    private fun ByteArrayOutputStream.writeInt32(value: Int) {
        write((value shr 24) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 8) and 0xFF)
        write(value and 0xFF)
    }

    private data class ParsedRtmp(
        val useTls: Boolean,
        val host: String,
        val port: Int,
        val app: String,
        val tcUrl: String,
        val streamName: String
    )

    private fun parseRtmpUrl(url: String, streamKey: String): ParsedRtmp {
        val cleanUrl = url.trim().removeSuffix("/")
        val uri = URI(cleanUrl)
        val useTls = uri.scheme.equals("rtmps", ignoreCase = true)
        val host = uri.host ?: throw IOException("Invalid RTMP host in URL")
        val port = if (uri.port != -1) uri.port else if (useTls) 443 else 1935
        val pathParts = (uri.path ?: "/live").trim('/').split('/').filter { it.isNotBlank() }
        val app = pathParts.firstOrNull() ?: "live"
        val prefix = if (pathParts.size > 1) pathParts.drop(1).joinToString("/") + "/" else ""
        val key = streamKey.trim()
        if (key.isEmpty()) throw IOException("Stream key cannot be empty")
        val tcUrl = "${uri.scheme}://$host:$port/$app"
        return ParsedRtmp(useTls, host, port, app, tcUrl, prefix + key)
    }
}
