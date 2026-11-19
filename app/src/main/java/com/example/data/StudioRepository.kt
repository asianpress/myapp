package com.example.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.studioDataStore by preferencesDataStore(name = "studio_settings")

class StudioRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val brandDao = database.brandProfileDao()
    private val overlayDao = database.overlayPresetDao()
    private val streamDao = database.streamDestinationDao()
    private val sessionDao = database.broadcastSessionDao()

    private val activeBrandIdKey = longPreferencesKey("active_brand_id")
    private val transitionDurationMsKey = intPreferencesKey("transition_duration_ms")

    val brands: Flow<List<BrandProfileEntity>> = brandDao.observeAll()
    val overlayPresets: Flow<List<OverlayPresetEntity>> = overlayDao.observeAll()
    val streamDestinations: Flow<List<StreamDestinationEntity>> = streamDao.observeAll()
    val broadcastSessions: Flow<List<BroadcastSessionEntity>> = sessionDao.observeAll()

    val activeBrandId: Flow<Long?> = context.studioDataStore.data.map { prefs ->
        prefs[activeBrandIdKey]
    }

    val transitionDurationMs: Flow<Int> = context.studioDataStore.data.map { prefs ->
        prefs[transitionDurationMsKey] ?: 450
    }

    suspend fun ensureSeeded() {
        if (brandDao.count() == 0) {
            brandDao.insertAll(
                listOf(
                    BrandProfileEntity(
                        businessName = "Asian Press",
                        tagline = "Independent News & Live Production",
                        phone = "+977-1-555-0192",
                        email = "asianpress.np@gmail.com",
                        website = "asianpress.media",
                        socialHandle = "@asianpresslive",
                        logoType = LogoType.BUILTIN_ASIAN_PRESS.name,
                        primaryColorHex = 0xFFE5243B,
                        accentColorHex = 0xFFF5A623,
                        watermarkPosition = WatermarkPosition.TOP_LEFT.name,
                        watermarkStyle = WatermarkStyle.CORNER_CONTACT.name,
                        watermarkOpacity = 0.94f,
                        watermarkScale = 1.0f,
                        isDefault = true
                    ),
                    BrandProfileEntity(
                        businessName = "Himalayan Craft Shop",
                        tagline = "Artisan Goods • Live Showroom",
                        phone = "+1 (555) 284-9910",
                        email = "orders@himalayancraft.shop",
                        website = "himalayancraft.shop",
                        socialHandle = "@himalayancraft",
                        logoType = LogoType.MONOGRAM.name,
                        primaryColorHex = 0xFF0EA5E9,
                        accentColorHex = 0xFFF59E0B,
                        watermarkPosition = WatermarkPosition.TOP_RIGHT.name,
                        watermarkStyle = WatermarkStyle.BADGE.name,
                        watermarkOpacity = 0.90f,
                        watermarkScale = 1.0f,
                        isDefault = false
                    ),
                    BrandProfileEntity(
                        businessName = "Vertex Event Studio",
                        tagline = "Weddings, Keynotes & Stage Broadcast",
                        phone = "+44 20 7946 0821",
                        email = "bookings@vertexstudio.tv",
                        website = "vertexstudio.tv",
                        socialHandle = "@vertexlive",
                        logoType = LogoType.MONOGRAM.name,
                        primaryColorHex = 0xFF10B981,
                        accentColorHex = 0xFF34D399,
                        watermarkPosition = WatermarkPosition.BOTTOM_RIGHT.name,
                        watermarkStyle = WatermarkStyle.FULL_HEADER.name,
                        watermarkOpacity = 0.88f,
                        watermarkScale = 0.95f,
                        isDefault = false
                    )
                )
            )
        }

        if (overlayDao.count() == 0) {
            overlayDao.insertAll(
                listOf(
                    OverlayPresetEntity(
                        name = "Asian Press Newsroom",
                        showWatermark = true,
                        showLowerThird = true,
                        lowerThirdTitle = "LIVE REPORT: STUDIO DESK",
                        lowerThirdSubtitle = "On-location coverage powered by Asian Press",
                        showTicker = true,
                        tickerText = "ASIAN PRESS LIVE  •  REAL-TIME FIELD REPORTING  •  WHATSAPP TIPS & BOOKINGS AVAILABLE  •  FOLLOW @ASIANPRESSLIVE",
                        showContactPill = true,
                        showLiveBug = true,
                        showClockBug = true,
                        showPromoCard = false,
                        transitionStyle = TransitionStyle.STINGER_WIPE.name
                    ),
                    OverlayPresetEntity(
                        name = "Live Shop & Product Demo",
                        showWatermark = true,
                        showLowerThird = true,
                        lowerThirdTitle = "FEATURED SHOWCASE",
                        lowerThirdSubtitle = "Message or call now to reserve yours today",
                        showTicker = true,
                        tickerText = "LIVE STUDIO SALE  •  SAME-DAY DISPATCH  •  DM OR CALL OUR STUDIO HOTLINE TO ORDER  •  LIMITED STOCK",
                        showContactPill = true,
                        showLiveBug = true,
                        showClockBug = false,
                        showPromoCard = true,
                        promoTitle = "ORDER LIVE NOW",
                        promoCta = "Tap contact details on screen",
                        transitionStyle = TransitionStyle.SLIDE_WIPE.name
                    ),
                    OverlayPresetEntity(
                        name = "Clean Client Watermark",
                        showWatermark = true,
                        showLowerThird = false,
                        lowerThirdTitle = "CLIENT PRODUCTION",
                        lowerThirdSubtitle = "Recorded in HD",
                        showTicker = false,
                        tickerText = "POWERED BY ASIAN PRESS MOBILE STUDIO",
                        showContactPill = true,
                        showLiveBug = false,
                        showClockBug = false,
                        showPromoCard = false,
                        transitionStyle = TransitionStyle.CROSS_DISSOLVE.name
                    ),
                    OverlayPresetEntity(
                        name = "Interview & Townhall",
                        showWatermark = true,
                        showLowerThird = true,
                        lowerThirdTitle = "SPECIAL GUEST INTERVIEW",
                        lowerThirdSubtitle = "Live Q&A session from the field",
                        showTicker = false,
                        tickerText = "SEND YOUR QUESTIONS IN THE LIVE CHAT",
                        showContactPill = false,
                        showLiveBug = true,
                        showClockBug = true,
                        showPromoCard = false,
                        transitionStyle = TransitionStyle.ZOOM_PUSH.name
                    )
                )
            )
        }

        if (streamDao.count() == 0) {
            streamDao.insertAll(
                listOf(
                    StreamDestinationEntity(
                        platform = SocialPlatform.YOUTUBE.name,
                        displayName = "YouTube Live",
                        rtmpUrl = SocialPlatform.YOUTUBE.defaultRtmpUrl,
                        isArmed = true,
                        resolutionPreset = "1080P_30",
                        bitrateKbps = 4500
                    ),
                    StreamDestinationEntity(
                        platform = SocialPlatform.FACEBOOK.name,
                        displayName = "Facebook Live",
                        rtmpUrl = SocialPlatform.FACEBOOK.defaultRtmpUrl,
                        isArmed = false,
                        resolutionPreset = "720P_30",
                        bitrateKbps = 3500
                    ),
                    StreamDestinationEntity(
                        platform = SocialPlatform.INSTAGRAM.name,
                        displayName = "Instagram Live",
                        rtmpUrl = SocialPlatform.INSTAGRAM.defaultRtmpUrl,
                        isArmed = false,
                        resolutionPreset = "720P_30",
                        bitrateKbps = 3500
                    ),
                    StreamDestinationEntity(
                        platform = SocialPlatform.TWITCH.name,
                        displayName = "Twitch",
                        rtmpUrl = SocialPlatform.TWITCH.defaultRtmpUrl,
                        isArmed = false,
                        resolutionPreset = "1080P_30",
                        bitrateKbps = 6000
                    ),
                    StreamDestinationEntity(
                        platform = SocialPlatform.CUSTOM_RTMP.name,
                        displayName = "Custom RTMP / Studio",
                        rtmpUrl = SocialPlatform.CUSTOM_RTMP.defaultRtmpUrl,
                        isArmed = false,
                        resolutionPreset = "1080P_30",
                        bitrateKbps = 4500
                    )
                )
            )
        }
    }

    suspend fun setActiveBrandId(id: Long) {
        context.studioDataStore.edit { prefs ->
            prefs[activeBrandIdKey] = id
        }
    }

    suspend fun setTransitionDurationMs(durationMs: Int) {
        context.studioDataStore.edit { prefs ->
            prefs[transitionDurationMsKey] = durationMs
        }
    }

    suspend fun saveBrand(profile: BrandProfileEntity): Long {
        val updated = profile.copy(updatedAt = System.currentTimeMillis())
        return if (updated.id == 0L) {
            brandDao.insert(updated)
        } else {
            brandDao.update(updated)
            updated.id
        }
    }

    suspend fun deleteBrand(profile: BrandProfileEntity) {
        if (!profile.logoPath.isNullOrBlank()) {
            runCatching { File(profile.logoPath).delete() }
        }
        brandDao.delete(profile)
    }

    suspend fun saveOverlayPreset(preset: OverlayPresetEntity): Long {
        return if (preset.id == 0L) overlayDao.insert(preset) else {
            overlayDao.update(preset)
            preset.id
        }
    }

    suspend fun deleteOverlayPreset(preset: OverlayPresetEntity) = overlayDao.delete(preset)

    suspend fun saveStreamDestination(destination: StreamDestinationEntity) {
        if (destination.id == 0L) {
            streamDao.insert(destination)
        } else {
            streamDao.update(destination)
        }
    }

    suspend fun logBroadcastSession(session: BroadcastSessionEntity) {
        sessionDao.insert(session)
    }

    suspend fun deleteSession(session: BroadcastSessionEntity) {
        sessionDao.delete(session)
    }

    suspend fun copyLogoToAppStorage(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val logosDir = File(context.filesDir, "logos").apply { mkdirs() }
            val destFile = File(logosDir, "brand_logo_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            destFile.absolutePath
        }.getOrNull()
    }
}
