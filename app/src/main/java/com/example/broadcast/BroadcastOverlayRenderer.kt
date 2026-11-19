package com.example.broadcast

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.R
import com.example.data.BrandProfileEntity
import com.example.data.LogoType
import com.example.data.SceneMode
import com.example.data.TransitionStyle
import com.example.data.WatermarkPosition
import com.example.data.WatermarkStyle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class BroadcastOverlayRenderer(private val context: Context) {

    private var cachedLogoKey: String? = null
    private var cachedLogoBitmap: Bitmap? = null
    private var studioSlateBitmap: Bitmap? = null

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 240, 244, 250)
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    private val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.MONOSPACE
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    fun drawFrame(
        canvas: Canvas,
        snapshot: RenderSnapshot,
        clearTransparent: Boolean,
        includeGuides: Boolean = false
    ) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        if (w <= 0f || h <= 0f) return

        if (clearTransparent) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else {
            canvas.drawColor(Color.rgb(11, 13, 18))
        }

        val unit = min(w, h) / 100f // responsive scaling unit
        val brandPrimary = snapshot.brand.primaryColorHex.toInt()
        val brandAccent = snapshot.brand.accentColorHex.toInt()
        val logoBitmap = resolveLogoBitmap(snapshot.brand)

        // 1. Non-camera scene backgrounds (Slate, BRB, Contact Card)
        if (snapshot.sceneMode != SceneMode.LIVE_CAMERA) {
            drawSceneCard(canvas, w, h, unit, snapshot, brandPrimary, brandAccent, logoBitmap)
        }

        // 2. Live/Clock Program Bug (placed opposite watermark if top-left is taken)
        val wmPos = runCatching {
            WatermarkPosition.valueOf(snapshot.brand.watermarkPosition)
        }.getOrDefault(WatermarkPosition.TOP_LEFT)

        if (snapshot.overlays.showLiveBug || snapshot.overlays.showClockBug) {
            drawProgramBug(canvas, w, h, unit, snapshot, wmPos, brandPrimary)
        }

        // 3. Brand Watermark Layer (Business name, logo, contact info)
        if (snapshot.overlays.showWatermark) {
            drawWatermark(canvas, w, h, unit, snapshot.brand, wmPos, brandPrimary, brandAccent, logoBitmap)
        }

        // 4. Contact & Social Pill
        if (snapshot.overlays.showContactPill && snapshot.sceneMode != SceneMode.CONTACT_CARD) {
            drawContactPill(canvas, w, h, unit, snapshot.brand, wmPos, brandPrimary)
        }

        // 5. Promo CTA Card
        if (snapshot.overlays.showPromoCard) {
            drawPromoCard(canvas, w, h, unit, snapshot, brandPrimary, brandAccent)
        }

        // 6. Lower Third Broadcast Banner
        if (snapshot.overlays.showLowerThird && snapshot.sceneMode != SceneMode.CONTACT_CARD) {
            drawLowerThird(canvas, w, h, unit, snapshot, brandPrimary, brandAccent)
        }

        // 7. Breaking / Promo Ticker Bar
        if (snapshot.overlays.showTicker) {
            drawTicker(canvas, w, h, unit, snapshot.brand, snapshot.overlays, brandPrimary)
        }

        // 8. Real-time Broadcast Transitions
        if (snapshot.transitionProgress in 0.001f..0.999f) {
            drawTransitionEffect(
                canvas = canvas,
                w = w,
                h = h,
                unit = unit,
                progress = snapshot.transitionProgress,
                style = snapshot.transitionStyle,
                brandPrimary = brandPrimary,
                brandAccent = brandAccent,
                logoBitmap = logoBitmap,
                brandName = snapshot.brand.businessName
            )
        }

        // 9. Optional Viewfinder Safe-Area Guides (preview only)
        if (includeGuides && snapshot.showSafeGuides) {
            drawSafeGuides(canvas, w, h)
        }
    }

    private fun drawSceneCard(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        snapshot: RenderSnapshot,
        brandPrimary: Int,
        brandAccent: Int,
        logoBitmap: Bitmap?
    ) {
        val slate = getStudioSlateBitmap()
        if (slate != null) {
            val src = Rect(0, 0, slate.width, slate.height)
            val dst = RectF(0f, 0f, w, h)
            bgPaint.alpha = 215
            canvas.drawBitmap(slate, src, dst, bgPaint)
            bgPaint.alpha = 255
        }

        // Subtle dark vignette overlay
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            Color.argb(175, 11, 13, 18),
            Color.argb(230, 11, 13, 18),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        bgPaint.shader = null

        val cx = w / 2f
        val cy = h * 0.45f

        val cardWidth = min(w * 0.82f, unit * 86f)
        val cardHeight = min(h * 0.56f, unit * 50f)
        val cardRect = RectF(cx - cardWidth / 2f, cy - cardHeight / 2f, cx + cardWidth / 2f, cy + cardHeight / 2f)

        bgPaint.color = Color.argb(225, 18, 21, 29)
        canvas.drawRoundRect(cardRect, unit * 3f, unit * 3f, bgPaint)
        borderPaint.color = brandPrimary
        borderPaint.strokeWidth = max(2f, unit * 0.45f)
        canvas.drawRoundRect(cardRect, unit * 3f, unit * 3f, borderPaint)

        // Top accent bar
        accentPaint.color = brandPrimary
        canvas.drawRoundRect(
            RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + unit * 1.4f),
            unit * 1.2f,
            unit * 1.2f,
            accentPaint
        )

        val logoSize = unit * 13f
        val logoTop = cardRect.top + unit * 4.2f
        drawBrandAvatar(
            canvas,
            RectF(cx - logoSize / 2f, logoTop, cx + logoSize / 2f, logoTop + logoSize),
            snapshot.brand,
            brandPrimary,
            brandAccent,
            logoBitmap,
            255
        )

        titlePaint.textAlign = Paint.Align.CENTER
        bodyPaint.textAlign = Paint.Align.CENTER

        when (snapshot.sceneMode) {
            SceneMode.STUDIO_SLATE -> {
                titlePaint.textSize = unit * 5.0f
                titlePaint.color = Color.WHITE
                canvas.drawText(snapshot.brand.businessName.uppercase(Locale.US), cx, logoTop + logoSize + unit * 6.5f, titlePaint)

                bodyPaint.textSize = unit * 3.1f
                bodyPaint.color = brandAccent
                canvas.drawText("STUDIO BROADCAST STANDBY", cx, logoTop + logoSize + unit * 11.2f, bodyPaint)

                bodyPaint.textSize = unit * 2.6f
                bodyPaint.color = Color.argb(215, 220, 226, 238)
                canvas.drawText(snapshot.brand.tagline, cx, logoTop + logoSize + unit * 15.8f, bodyPaint)
            }
            SceneMode.BRB_CARD -> {
                titlePaint.textSize = unit * 5.4f
                titlePaint.color = brandAccent
                canvas.drawText("BE RIGHT BACK", cx, logoTop + logoSize + unit * 6.8f, titlePaint)

                bodyPaint.textSize = unit * 3.0f
                bodyPaint.color = Color.WHITE
                canvas.drawText(
                    "${snapshot.brand.businessName} will resume momentarily",
                    cx,
                    logoTop + logoSize + unit * 11.8f,
                    bodyPaint
                )

                bodyPaint.textSize = unit * 2.5f
                bodyPaint.color = Color.argb(200, 200, 210, 225)
                canvas.drawText(
                    "${snapshot.brand.website}  •  ${snapshot.brand.socialHandle}",
                    cx,
                    logoTop + logoSize + unit * 16.2f,
                    bodyPaint
                )
            }
            SceneMode.CONTACT_CARD -> {
                titlePaint.textSize = unit * 4.5f
                titlePaint.color = Color.WHITE
                canvas.drawText(snapshot.brand.businessName, cx, logoTop + logoSize + unit * 6.0f, titlePaint)

                bodyPaint.textSize = unit * 2.7f
                bodyPaint.color = brandAccent
                canvas.drawText(snapshot.brand.tagline, cx, logoTop + logoSize + unit * 10.2f, bodyPaint)

                bodyPaint.textSize = unit * 2.55f
                bodyPaint.color = Color.argb(240, 240, 244, 250)
                val line1 = listOf(snapshot.brand.phone, snapshot.brand.email).filter { it.isNotBlank() }.joinToString("  •  ")
                val line2 = listOf(snapshot.brand.website, snapshot.brand.socialHandle).filter { it.isNotBlank() }.joinToString("  •  ")
                canvas.drawText(line1, cx, logoTop + logoSize + unit * 15.2f, bodyPaint)
                canvas.drawText(line2, cx, logoTop + logoSize + unit * 19.2f, bodyPaint)
            }
            SceneMode.LIVE_CAMERA -> Unit
        }

        titlePaint.textAlign = Paint.Align.LEFT
        bodyPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawWatermark(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        brand: BrandProfileEntity,
        pos: WatermarkPosition,
        brandPrimary: Int,
        brandAccent: Int,
        logoBitmap: Bitmap?
    ) {
        val style = runCatching { WatermarkStyle.valueOf(brand.watermarkStyle) }
            .getOrDefault(WatermarkStyle.CORNER_CONTACT)
        val alphaInt = (brand.watermarkOpacity.coerceIn(0.2f, 1.0f) * 255).toInt()
        val scale = brand.watermarkScale.coerceIn(0.6f, 1.5f)
        val u = unit * scale

        val padX = unit * 3.2f
        val padY = unit * 3.2f

        if (style == WatermarkStyle.FULL_HEADER) {
            val barH = u * 9.0f
            val topY = if (pos == WatermarkPosition.BOTTOM_LEFT || pos == WatermarkPosition.BOTTOM_RIGHT) {
                h - unit * 18f - barH
            } else {
                padY
            }
            val barRect = RectF(padX, topY, w - padX, topY + barH)
            bgPaint.color = Color.argb((alphaInt * 0.85f).toInt(), 14, 17, 24)
            canvas.drawRoundRect(barRect, u * 1.6f, u * 1.6f, bgPaint)

            accentPaint.color = Color.argb(alphaInt, Color.red(brandPrimary), Color.green(brandPrimary), Color.blue(brandPrimary))
            canvas.drawRoundRect(
                RectF(barRect.left, barRect.top, barRect.left + u * 1.1f, barRect.bottom),
                u * 0.6f,
                u * 0.6f,
                accentPaint
            )

            val avSize = u * 6.4f
            val avTop = barRect.centerY() - avSize / 2f
            drawBrandAvatar(
                canvas,
                RectF(barRect.left + u * 2.2f, avTop, barRect.left + u * 2.2f + avSize, avTop + avSize),
                brand,
                brandPrimary,
                brandAccent,
                logoBitmap,
                alphaInt
            )

            titlePaint.textSize = u * 3.1f
            titlePaint.color = Color.argb(alphaInt, 255, 255, 255)
            canvas.drawText(brand.businessName.uppercase(Locale.US), barRect.left + u * 10.0f, barRect.centerY() - u * 0.3f, titlePaint)

            bodyPaint.textSize = u * 2.1f
            bodyPaint.color = Color.argb(alphaInt, 215, 222, 235)
            val contactLine = listOf(brand.phone, brand.website, brand.socialHandle).filter { it.isNotBlank() }.joinToString(" • ")
            canvas.drawText(contactLine, barRect.left + u * 10.0f, barRect.centerY() + u * 2.5f, bodyPaint)
            return
        }

        if (style == WatermarkStyle.DIAGONAL_STAMP) {
            val stampAlpha = (alphaInt * 0.45f).toInt()
            val avSize = u * 10f
            val cx = when (pos) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> padX + avSize / 2f + u * 2f
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> w - padX - avSize / 2f - u * 2f
                WatermarkPosition.CENTER -> w / 2f
            }
            val cy = when (pos) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> padY + avSize / 2f + u * 2f
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> h - unit * 16f - avSize / 2f
                WatermarkPosition.CENTER -> h / 2f
            }
            drawBrandAvatar(
                canvas,
                RectF(cx - avSize / 2f, cy - avSize / 2f, cx + avSize / 2f, cy + avSize / 2f),
                brand,
                brandPrimary,
                brandAccent,
                logoBitmap,
                stampAlpha
            )
            titlePaint.textAlign = Paint.Align.CENTER
            titlePaint.textSize = u * 2.4f
            titlePaint.color = Color.argb(stampAlpha, 255, 255, 255)
            canvas.drawText(brand.businessName.uppercase(Locale.US), cx, cy + avSize / 2f + u * 3.0f, titlePaint)
            titlePaint.textAlign = Paint.Align.LEFT
            return
        }

        // BADGE or CORNER_CONTACT
        val showContact = style == WatermarkStyle.CORNER_CONTACT
        titlePaint.textSize = u * 2.9f
        bodyPaint.textSize = u * 2.0f
        val nameWidth = titlePaint.measureText(brand.businessName.uppercase(Locale.US))
        val contactText = listOf(brand.phone, brand.website).filter { it.isNotBlank() }.joinToString(" • ")
        val contactWidth = if (showContact) bodyPaint.measureText(contactText) else 0f

        val avSize = u * 6.0f
        val boxW = avSize + u * 4.2f + max(nameWidth, contactWidth)
        val boxH = if (showContact) u * 8.2f else u * 7.2f

        val left = when (pos) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> padX
            WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> w - padX - boxW
            WatermarkPosition.CENTER -> (w - boxW) / 2f
        }
        val top = when (pos) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> padY
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> h - unit * 17f - boxH
            WatermarkPosition.CENTER -> (h - boxH) / 2f
        }

        val rect = RectF(left, top, left + boxW, top + boxH)
        bgPaint.color = Color.argb((alphaInt * 0.82f).toInt(), 13, 16, 23)
        canvas.drawRoundRect(rect, u * 1.6f, u * 1.6f, bgPaint)

        borderPaint.color = Color.argb((alphaInt * 0.7f).toInt(), Color.red(brandPrimary), Color.green(brandPrimary), Color.blue(brandPrimary))
        borderPaint.strokeWidth = max(1.5f, u * 0.3f)
        canvas.drawRoundRect(rect, u * 1.6f, u * 1.6f, borderPaint)

        val avTop = rect.centerY() - avSize / 2f
        drawBrandAvatar(
            canvas,
            RectF(rect.left + u * 1.1f, avTop, rect.left + u * 1.1f + avSize, avTop + avSize),
            brand,
            brandPrimary,
            brandAccent,
            logoBitmap,
            alphaInt
        )

        val textX = rect.left + u * 2.2f + avSize
        titlePaint.color = Color.argb(alphaInt, 255, 255, 255)
        if (showContact && contactText.isNotBlank()) {
            canvas.drawText(brand.businessName.uppercase(Locale.US), textX, rect.centerY() - u * 0.35f, titlePaint)
            bodyPaint.color = Color.argb(alphaInt, Color.red(brandAccent), Color.green(brandAccent), Color.blue(brandAccent))
            canvas.drawText(contactText, textX, rect.centerY() + u * 2.25f, bodyPaint)
        } else {
            canvas.drawText(brand.businessName.uppercase(Locale.US), textX, rect.centerY() + u * 1.0f, titlePaint)
        }
    }

    private fun drawProgramBug(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        snapshot: RenderSnapshot,
        wmPos: WatermarkPosition,
        brandPrimary: Int
    ) {
        val placeLeft = wmPos == WatermarkPosition.TOP_RIGHT
        val padX = unit * 3.2f
        val padY = unit * 3.2f

        val label = when {
            snapshot.isStreaming -> "LIVE"
            snapshot.isRecording -> "REC"
            else -> "STUDIO"
        }
        val clockStr = if (snapshot.isStreaming || snapshot.isRecording) {
            formatDuration(snapshot.elapsedSeconds)
        } else {
            timeFormat.format(Date())
        }

        monoPaint.textSize = unit * 2.3f
        val liveW = if (snapshot.overlays.showLiveBug) monoPaint.measureText(label) + unit * 4.5f else 0f
        val clockW = if (snapshot.overlays.showClockBug) monoPaint.measureText(clockStr) + unit * 3.2f else 0f
        val totalW = liveW + clockW + if (liveW > 0f && clockW > 0f) unit * 1.0f else 0f
        if (totalW <= 0f) return

        val bugH = unit * 5.2f
        var curX = if (placeLeft) padX else w - padX - totalW

        if (snapshot.overlays.showLiveBug) {
            val pillColor = when {
                snapshot.isStreaming || snapshot.isRecording -> brandPrimary
                else -> Color.argb(215, 28, 34, 46)
            }
            bgPaint.color = pillColor
            val liveRect = RectF(curX, padY, curX + liveW, padY + bugH)
            canvas.drawRoundRect(liveRect, unit * 1.1f, unit * 1.1f, bgPaint)

            // Recording dot
            accentPaint.color = if (snapshot.isStreaming || snapshot.isRecording) Color.WHITE else brandPrimary
            canvas.drawCircle(liveRect.left + unit * 1.8f, liveRect.centerY(), unit * 0.65f, accentPaint)

            monoPaint.color = Color.WHITE
            canvas.drawText(label, liveRect.left + unit * 3.1f, liveRect.centerY() + unit * 0.85f, monoPaint)
            curX += liveW + unit * 1.0f
        }

        if (snapshot.overlays.showClockBug) {
            bgPaint.color = Color.argb(200, 13, 16, 23)
            val clockRect = RectF(curX, padY, curX + clockW, padY + bugH)
            canvas.drawRoundRect(clockRect, unit * 1.1f, unit * 1.1f, bgPaint)
            monoPaint.color = Color.argb(240, 235, 240, 248)
            canvas.drawText(clockStr, clockRect.left + unit * 1.6f, clockRect.centerY() + unit * 0.85f, monoPaint)
        }
    }

    private fun drawContactPill(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        brand: BrandProfileEntity,
        wmPos: WatermarkPosition,
        brandPrimary: Int
    ) {
        val text = listOf(brand.socialHandle, brand.email, brand.phone).firstOrNull { it.isNotBlank() } ?: return
        val fullText = listOf(brand.socialHandle, brand.phone).filter { it.isNotBlank() }.joinToString("  •  ").ifBlank { text }

        bodyPaint.textSize = unit * 2.15f
        val textW = bodyPaint.measureText(fullText)
        val pillW = textW + unit * 4.5f
        val pillH = unit * 4.6f

        val topY = if (wmPos == WatermarkPosition.TOP_RIGHT) unit * 10.2f else unit * 10.2f
        val leftX = if (wmPos == WatermarkPosition.TOP_RIGHT) unit * 3.2f else w - unit * 3.2f - pillW

        val rect = RectF(leftX, topY, leftX + pillW, topY + pillH)
        bgPaint.color = Color.argb(190, 14, 17, 24)
        canvas.drawRoundRect(rect, unit * 2.3f, unit * 2.3f, bgPaint)

        accentPaint.color = brandPrimary
        canvas.drawCircle(rect.left + unit * 1.7f, rect.centerY(), unit * 0.6f, accentPaint)

        bodyPaint.color = Color.WHITE
        canvas.drawText(fullText, rect.left + unit * 3.0f, rect.centerY() + unit * 0.75f, bodyPaint)
    }

    private fun drawPromoCard(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        snapshot: RenderSnapshot,
        brandPrimary: Int,
        brandAccent: Int
    ) {
        val cardW = min(w * 0.44f, unit * 44f)
        val cardH = unit * 13.5f
        val left = w - unit * 3.2f - cardW
        val top = h - unit * 24f - cardH

        val rect = RectF(left, top, left + cardW, top + cardH)
        bgPaint.color = Color.argb(225, 16, 20, 29)
        canvas.drawRoundRect(rect, unit * 1.8f, unit * 1.8f, bgPaint)
        borderPaint.color = brandAccent
        borderPaint.strokeWidth = max(1.5f, unit * 0.32f)
        canvas.drawRoundRect(rect, unit * 1.8f, unit * 1.8f, borderPaint)

        accentPaint.color = brandPrimary
        canvas.drawRoundRect(
            RectF(rect.left + unit * 1.6f, rect.top + unit * 1.4f, rect.left + unit * 16f, rect.top + unit * 4.4f),
            unit * 0.7f,
            unit * 0.7f,
            accentPaint
        )
        monoPaint.textSize = unit * 1.8f
        monoPaint.color = Color.WHITE
        canvas.drawText("STUDIO OFFER", rect.left + unit * 2.3f, rect.top + unit * 3.55f, monoPaint)

        titlePaint.textSize = unit * 2.7f
        titlePaint.color = Color.WHITE
        canvas.drawText(snapshot.overlays.promoTitle, rect.left + unit * 1.8f, rect.top + unit * 8.0f, titlePaint)

        bodyPaint.textSize = unit * 2.0f
        bodyPaint.color = Color.argb(230, 220, 226, 236)
        canvas.drawText(snapshot.overlays.promoCta, rect.left + unit * 1.8f, rect.top + unit * 11.2f, bodyPaint)
    }

    private fun drawLowerThird(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        snapshot: RenderSnapshot,
        brandPrimary: Int,
        brandAccent: Int
    ) {
        val bottomAnchor = if (snapshot.overlays.showTicker) h - unit * 7.8f else h - unit * 3.5f
        val barH = unit * 11.0f
        val barTop = bottomAnchor - barH
        val barLeft = unit * 3.2f
        val barRight = min(w - unit * 3.2f, barLeft + w * 0.72f)

        val cardRect = RectF(barLeft, barTop, barRight, bottomAnchor)
        bgPaint.color = Color.argb(228, 13, 16, 23)
        canvas.drawRoundRect(cardRect, unit * 1.6f, unit * 1.6f, bgPaint)

        // Left crimson/primary vertical stripe
        accentPaint.color = brandPrimary
        canvas.drawRoundRect(
            RectF(cardRect.left, cardRect.top, cardRect.left + unit * 1.35f, cardRect.bottom),
            unit * 0.8f,
            unit * 0.8f,
            accentPaint
        )

        // Brand tag pill on top edge
        val tagText = snapshot.brand.businessName.uppercase(Locale.US)
        monoPaint.textSize = unit * 1.75f
        val tagW = monoPaint.measureText(tagText) + unit * 2.8f
        val tagRect = RectF(cardRect.left + unit * 2.4f, cardRect.top - unit * 1.8f, cardRect.left + unit * 2.4f + tagW, cardRect.top + unit * 1.8f)
        accentPaint.color = brandAccent
        canvas.drawRoundRect(tagRect, unit * 0.7f, unit * 0.7f, accentPaint)
        monoPaint.color = Color.rgb(15, 18, 25)
        canvas.drawText(tagText, tagRect.left + unit * 1.4f, tagRect.centerY() + unit * 0.65f, monoPaint)

        titlePaint.textSize = unit * 3.25f
        titlePaint.color = Color.WHITE
        canvas.drawText(
            snapshot.overlays.lowerThirdTitle.take(42),
            cardRect.left + unit * 2.6f,
            cardRect.top + unit * 5.6f,
            titlePaint
        )

        bodyPaint.textSize = unit * 2.25f
        bodyPaint.color = Color.argb(225, 205, 214, 228)
        canvas.drawText(
            snapshot.overlays.lowerThirdSubtitle.take(58),
            cardRect.left + unit * 2.6f,
            cardRect.top + unit * 9.0f,
            bodyPaint
        )
    }

    private fun drawTicker(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        brand: BrandProfileEntity,
        overlays: LiveOverlayConfig,
        brandPrimary: Int
    ) {
        val tickerH = unit * 5.6f
        val top = h - tickerH
        bgPaint.color = Color.argb(238, 10, 12, 18)
        canvas.drawRect(0f, top, w, h, bgPaint)

        // Top accent hairline
        accentPaint.color = brandPrimary
        canvas.drawRect(0f, top, w, top + max(2f, unit * 0.35f), accentPaint)

        val badgeLabel = " ${brand.businessName.uppercase(Locale.US).take(14)} "
        monoPaint.textSize = unit * 2.0f
        val badgeW = monoPaint.measureText(badgeLabel) + unit * 2.0f
        accentPaint.color = brandPrimary
        canvas.drawRect(0f, top, badgeW, h, accentPaint)
        monoPaint.color = Color.WHITE
        canvas.drawText(badgeLabel, unit * 1.0f, top + tickerH * 0.66f, monoPaint)

        // Scrolling ticker text driven by current time so both preview & recorded MP4 animate smoothly
        val text = "${overlays.tickerText}   •   "
        bodyPaint.textSize = unit * 2.15f
        bodyPaint.color = Color.WHITE
        val textWidth = max(1f, bodyPaint.measureText(text))
        val speedPxPerSec = unit * 11f
        val shift = ((System.currentTimeMillis() / 1000.0) * speedPxPerSec) % textWidth

        canvas.save()
        canvas.clipRect(badgeW, top, w, h)
        var drawX = badgeW - shift.toFloat()
        while (drawX < w) {
            canvas.drawText(text, drawX, top + tickerH * 0.66f, bodyPaint)
            drawX += textWidth
        }
        canvas.restore()
    }

    private fun drawTransitionEffect(
        canvas: Canvas,
        w: Float,
        h: Float,
        unit: Float,
        progress: Float,
        style: TransitionStyle,
        brandPrimary: Int,
        brandAccent: Int,
        logoBitmap: Bitmap?,
        brandName: String
    ) {
        when (style) {
            TransitionStyle.CUT -> Unit
            TransitionStyle.CROSS_DISSOLVE -> {
                val alpha = (sin(progress * PI).toFloat().coerceIn(0f, 1f) * 235).toInt()
                bgPaint.color = Color.argb(alpha, 11, 13, 18)
                canvas.drawRect(0f, 0f, w, h, bgPaint)
            }
            TransitionStyle.SLIDE_WIPE -> {
                val band = w * 0.25f
                val edge = -band + (w + band * 2f) * progress
                bgPaint.color = Color.rgb(14, 17, 24)
                canvas.drawRect(edge - w * 0.5f, 0f, edge, h, bgPaint)
                accentPaint.color = brandPrimary
                canvas.drawRect(edge, 0f, edge + unit * 3.5f, h, accentPaint)
            }
            TransitionStyle.STINGER_WIPE -> {
                // Signature diagonal broadcast chevron stinger with mid-transition studio badge
                val travel = (w + h * 0.8f) * progress - h * 0.4f
                val path = Path().apply {
                    moveTo(travel - w * 0.35f, 0f)
                    lineTo(travel + w * 0.28f, 0f)
                    lineTo(travel + w * 0.08f, h)
                    lineTo(travel - w * 0.55f, h)
                    close()
                }
                bgPaint.color = brandPrimary
                canvas.drawPath(path, bgPaint)

                val goldStripe = Path().apply {
                    moveTo(travel + w * 0.26f, 0f)
                    lineTo(travel + w * 0.33f, 0f)
                    lineTo(travel + w * 0.13f, h)
                    lineTo(travel + w * 0.06f, h)
                    close()
                }
                accentPaint.color = brandAccent
                canvas.drawPath(goldStripe, accentPaint)

                val peak = sin(progress * PI).toFloat()
                if (peak > 0.35f) {
                    val badgeSize = unit * 14f
                    val alpha = (peak * 255).toInt().coerceIn(0, 255)
                    val cx = w / 2f
                    val cy = h / 2f
                    if (logoBitmap != null) {
                        bgPaint.alpha = alpha
                        canvas.drawBitmap(
                            logoBitmap,
                            Rect(0, 0, logoBitmap.width, logoBitmap.height),
                            RectF(cx - badgeSize / 2f, cy - badgeSize / 2f, cx + badgeSize / 2f, cy + badgeSize / 2f),
                            bgPaint
                        )
                        bgPaint.alpha = 255
                    }
                    titlePaint.textAlign = Paint.Align.CENTER
                    titlePaint.textSize = unit * 3.6f
                    titlePaint.color = Color.argb(alpha, 255, 255, 255)
                    canvas.drawText(brandName.uppercase(Locale.US), cx, cy + badgeSize / 2f + unit * 4.5f, titlePaint)
                    titlePaint.textAlign = Paint.Align.LEFT
                }
            }
            TransitionStyle.ZOOM_PUSH -> {
                val iris = sin(progress * PI).toFloat()
                borderPaint.color = brandPrimary
                borderPaint.strokeWidth = unit * 2.5f * iris
                val inset = (1f - iris) * min(w, h) * 0.35f
                canvas.drawRoundRect(
                    RectF(inset, inset, w - inset, h - inset),
                    unit * 4f,
                    unit * 4f,
                    borderPaint
                )
            }
        }
    }

    private fun drawSafeGuides(canvas: Canvas, w: Float, h: Float) {
        borderPaint.color = Color.argb(115, 255, 255, 255)
        borderPaint.strokeWidth = 1.5f
        // Rule of thirds
        canvas.drawLine(w / 3f, 0f, w / 3f, h, borderPaint)
        canvas.drawLine(w * 2f / 3f, 0f, w * 2f / 3f, h, borderPaint)
        canvas.drawLine(0f, h / 3f, w, h / 3f, borderPaint)
        canvas.drawLine(0f, h * 2f / 3f, w, h * 2f / 3f, borderPaint)
        // Broadcast title-safe box (90%)
        borderPaint.color = Color.argb(135, 245, 166, 35)
        canvas.drawRect(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.95f, borderPaint)
    }

    private fun drawBrandAvatar(
        canvas: Canvas,
        bounds: RectF,
        brand: BrandProfileEntity,
        brandPrimary: Int,
        brandAccent: Int,
        logoBitmap: Bitmap?,
        alpha: Int
    ) {
        if (logoBitmap != null) {
            canvas.save()
            val clip = Path().apply {
                addRoundRect(bounds, bounds.width() * 0.22f, bounds.height() * 0.22f, Path.Direction.CW)
            }
            canvas.clipPath(clip)
            bgPaint.alpha = alpha
            canvas.drawBitmap(
                logoBitmap,
                Rect(0, 0, logoBitmap.width, logoBitmap.height),
                bounds,
                bgPaint
            )
            bgPaint.alpha = 255
            canvas.restore()
        } else {
            accentPaint.color = Color.argb(alpha, Color.red(brandPrimary), Color.green(brandPrimary), Color.blue(brandPrimary))
            canvas.drawRoundRect(bounds, bounds.width() * 0.24f, bounds.height() * 0.24f, accentPaint)

            val initials = brand.businessName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }
                .ifBlank { "AP" }

            titlePaint.textAlign = Paint.Align.CENTER
            titlePaint.textSize = bounds.height() * 0.46f
            titlePaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawText(initials, bounds.centerX(), bounds.centerY() + bounds.height() * 0.16f, titlePaint)
            titlePaint.textAlign = Paint.Align.LEFT

            borderPaint.color = Color.argb(alpha, Color.red(brandAccent), Color.green(brandAccent), Color.blue(brandAccent))
            borderPaint.strokeWidth = max(1.5f, bounds.width() * 0.05f)
            canvas.drawRoundRect(bounds, bounds.width() * 0.24f, bounds.height() * 0.24f, borderPaint)
        }
    }

    private fun resolveLogoBitmap(brand: BrandProfileEntity): Bitmap? {
        val key = "${brand.logoType}:${brand.logoPath.orEmpty()}"
        if (cachedLogoKey == key && cachedLogoBitmap != null) {
            return cachedLogoBitmap
        }
        val bmp = when (brand.logoType) {
            LogoType.CUSTOM_FILE.name -> {
                brand.logoPath?.let { path ->
                    val file = File(path)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
            }
            LogoType.BUILTIN_ASIAN_PRESS.name -> {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_asian_press_logo)
            }
            else -> null
        }
        cachedLogoKey = key
        cachedLogoBitmap = bmp
        return bmp
    }

    private fun getStudioSlateBitmap(): Bitmap? {
        if (studioSlateBitmap == null) {
            studioSlateBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_studio_slate)
        }
        return studioSlateBitmap
    }

    companion object {
        fun formatDuration(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        }
    }
}
