package com.example.broadcast

import com.example.data.BrandProfileEntity
import com.example.data.OverlayPresetEntity
import com.example.data.SceneMode
import com.example.data.TransitionStyle

data class LiveOverlayConfig(
    val showWatermark: Boolean = true,
    val showLowerThird: Boolean = true,
    val lowerThirdTitle: String = "LIVE REPORT: STUDIO DESK",
    val lowerThirdSubtitle: String = "On-location coverage powered by Asian Press",
    val showTicker: Boolean = true,
    val tickerText: String = "ASIAN PRESS LIVE  •  REAL-TIME FIELD REPORTING  •  CONTACT DESK FOR LIVE COVERAGE",
    val showContactPill: Boolean = true,
    val showLiveBug: Boolean = true,
    val showClockBug: Boolean = true,
    val showPromoCard: Boolean = false,
    val promoTitle: String = "BOOK LIVE COVERAGE",
    val promoCta: String = "Call or email our studio desk",
    val transitionStyle: TransitionStyle = TransitionStyle.STINGER_WIPE
) {
    companion object {
        fun fromPreset(preset: OverlayPresetEntity): LiveOverlayConfig {
            return LiveOverlayConfig(
                showWatermark = preset.showWatermark,
                showLowerThird = preset.showLowerThird,
                lowerThirdTitle = preset.lowerThirdTitle,
                lowerThirdSubtitle = preset.lowerThirdSubtitle,
                showTicker = preset.showTicker,
                tickerText = preset.tickerText,
                showContactPill = preset.showContactPill,
                showLiveBug = preset.showLiveBug,
                showClockBug = preset.showClockBug,
                showPromoCard = preset.showPromoCard,
                promoTitle = preset.promoTitle,
                promoCta = preset.promoCta,
                transitionStyle = runCatching {
                    TransitionStyle.valueOf(preset.transitionStyle)
                }.getOrDefault(TransitionStyle.STINGER_WIPE)
            )
        }
    }
}

enum class StreamStatus {
    IDLE,
    CONNECTING,
    LIVE,
    ERROR
}

data class StreamTelemetry(
    val status: StreamStatus = StreamStatus.IDLE,
    val activePlatforms: List<String> = emptyList(),
    val bitrateKbps: Int = 0,
    val fps: Int = 30,
    val bytesSent: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val statusMessage: String = "Ready"
)

data class RenderSnapshot(
    val brand: BrandProfileEntity,
    val overlays: LiveOverlayConfig,
    val sceneMode: SceneMode,
    val isRecording: Boolean,
    val isStreaming: Boolean,
    val elapsedSeconds: Long,
    val transitionProgress: Float, // 0f = inactive, 0f..1f = animating
    val transitionStyle: TransitionStyle,
    val showSafeGuides: Boolean = false
)
