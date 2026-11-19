package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogoType {
    BUILTIN_ASIAN_PRESS,
    CUSTOM_FILE,
    MONOGRAM
}

enum class WatermarkPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right"),
    CENTER("Center Stamp")
}

enum class WatermarkStyle(val label: String) {
    BADGE("Logo + Studio Pill"),
    CORNER_CONTACT("Logo + Contact Info"),
    FULL_HEADER("Broadcast Header Bar"),
    DIAGONAL_STAMP("Subtle Stamp")
}

@Entity(tableName = "brand_profiles")
data class BrandProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessName: String,
    val tagline: String,
    val phone: String,
    val email: String,
    val website: String,
    val socialHandle: String,
    val logoType: String = LogoType.BUILTIN_ASIAN_PRESS.name,
    val logoPath: String? = null,
    val primaryColorHex: Long = 0xFFE5243B,
    val accentColorHex: Long = 0xFFF5A623,
    val watermarkPosition: String = WatermarkPosition.TOP_LEFT.name,
    val watermarkStyle: String = WatermarkStyle.CORNER_CONTACT.name,
    val watermarkOpacity: Float = 0.92f,
    val watermarkScale: Float = 1.0f,
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
