package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransitionStyle(val label: String) {
    CUT("Instant Cut"),
    CROSS_DISSOLVE("Dissolve"),
    SLIDE_WIPE("Slide Wipe"),
    STINGER_WIPE("Press Stinger"),
    ZOOM_PUSH("Zoom Push")
}

enum class SceneMode(val label: String) {
    LIVE_CAMERA("Live Cam"),
    STUDIO_SLATE("Studio Slate"),
    BRB_CARD("Be Right Back"),
    CONTACT_CARD("Contact Card")
}

@Entity(tableName = "overlay_presets")
data class OverlayPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val showWatermark: Boolean = true,
    val showLowerThird: Boolean = true,
    val lowerThirdTitle: String = "LIVE COVERAGE",
    val lowerThirdSubtitle: String = "Direct from the newsroom & field studio",
    val showTicker: Boolean = true,
    val tickerText: String = "ASIAN PRESS LIVE  •  BREAKING UPDATES  •  SUBSCRIBE & FOLLOW @ASIANPRESS  •  CONTACT DESK FOR LIVE COVERAGE",
    val showContactPill: Boolean = true,
    val showLiveBug: Boolean = true,
    val showClockBug: Boolean = true,
    val showPromoCard: Boolean = false,
    val promoTitle: String = "BOOK LIVE COVERAGE",
    val promoCta: String = "Call or email our studio desk",
    val transitionStyle: String = TransitionStyle.STINGER_WIPE.name
)
