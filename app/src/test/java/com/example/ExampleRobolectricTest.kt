package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.broadcast.BroadcastOverlayRenderer
import com.example.broadcast.LiveOverlayConfig
import com.example.broadcast.RenderSnapshot
import com.example.data.AppDatabase
import com.example.data.BrandProfileEntity
import com.example.data.SceneMode
import com.example.data.StudioRepository
import com.example.data.TransitionStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Asian Press Live`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Asian Press Live", appName)
    }

    @Test
    fun `repository seeds Asian Press brand and social destinations`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = StudioRepository(context, db)
        repo.ensureSeeded()

        val brands = repo.brands.first()
        val presets = repo.overlayPresets.first()
        val destinations = repo.streamDestinations.first()

        assertTrue(brands.any { it.businessName == "Asian Press" && it.isDefault })
        assertTrue(presets.size >= 4)
        assertTrue(destinations.size >= 5)
        db.close()
    }

    @Test
    fun `overlay renderer draws watermark lower third and stinger without error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val renderer = BroadcastOverlayRenderer(context)
        val bitmap = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val snapshot = RenderSnapshot(
            brand = BrandProfileEntity(
                businessName = "Asian Press",
                tagline = "Live Studio",
                phone = "+977-1-555-0192",
                email = "asianpress.np@gmail.com",
                website = "asianpress.media",
                socialHandle = "@asianpresslive"
            ),
            overlays = LiveOverlayConfig(showPromoCard = true),
            sceneMode = SceneMode.STUDIO_SLATE,
            isRecording = true,
            isStreaming = false,
            elapsedSeconds = 42L,
            transitionProgress = 0.5f,
            transitionStyle = TransitionStyle.STINGER_WIPE,
            showSafeGuides = true
        )

        renderer.drawFrame(canvas, snapshot, clearTransparent = false, includeGuides = true)
        assertEquals("00:00:42", BroadcastOverlayRenderer.formatDuration(42L))
        bitmap.recycle()
    }
}
