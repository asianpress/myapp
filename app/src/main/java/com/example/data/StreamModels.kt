package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SocialPlatform(val label: String, val defaultRtmpUrl: String, val brandHex: Long) {
    YOUTUBE("YouTube Live", "rtmp://a.rtmp.youtube.com/live2", 0xFFFF0000),
    FACEBOOK("Facebook Live", "rtmps://live-api-s.facebook.com:443/rtmp/", 0xFF1877F2),
    INSTAGRAM("Instagram Live", "rtmps://live-upload.instagram.com:443/rtmp/", 0xFFE4405F),
    TWITCH("Twitch", "rtmp://live.twitch.tv/app", 0xFF9146FF),
    CUSTOM_RTMP("Custom RTMP", "rtmp://your-studio-server.com/live", 0xFF10B981)
}

@Entity(tableName = "stream_destinations")
data class StreamDestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val platform: String,
    val displayName: String,
    val rtmpUrl: String,
    val streamKey: String = "",
    val isArmed: Boolean = false,
    val resolutionPreset: String = "1080P_30",
    val bitrateKbps: Int = 4500,
    val lastStreamedAt: Long? = null
)

@Entity(tableName = "broadcast_sessions")
data class BroadcastSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val brandName: String,
    val mode: String, // "RECORD", "LIVESTREAM", "SIMULCAST"
    val platformsSummary: String,
    val durationSeconds: Long,
    val fileUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
