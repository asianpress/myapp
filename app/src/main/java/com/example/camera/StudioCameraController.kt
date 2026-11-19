package com.example.camera

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.broadcast.BroadcastOverlayRenderer
import com.example.broadcast.RenderSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CameraHardwareState(
    val isBound: Boolean = false,
    val isBackCamera: Boolean = true,
    val isTorchOn: Boolean = false,
    val hasTorch: Boolean = false,
    val isRecording: Boolean = false,
    val recordingDurationSeconds: Long = 0L,
    val audioAmplitude: Float = 0f,
    val errorMessage: String? = null
)

class StudioCameraController(
    private val context: Context,
    private val renderer: BroadcastOverlayRenderer
) {
    private val _hardwareState = MutableStateFlow(CameraHardwareState())
    val hardwareState: StateFlow<CameraHardwareState> = _hardwareState.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var overlayEffect: OverlayEffect? = null
    private var currentPreviewView: PreviewView? = null

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        useBackCamera: Boolean,
        snapshotProvider: () -> RenderSnapshot
    ) {
        currentPreviewView = previewView
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            runCatching {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val selector = if (useBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                if (!provider.hasCamera(selector)) {
                    _hardwareState.value = _hardwareState.value.copy(
                        isBound = false,
                        errorMessage = "Camera lens unavailable on this device. Studio Slate & Overlays remain active."
                    )
                    return@addListener
                }

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HD))
                    .build()
                val vidCapture = VideoCapture.withOutput(recorder)
                videoCapture = vidCapture

                overlayEffect?.close()
                val effect = OverlayEffect(
                    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
                    5,
                    Handler(Looper.getMainLooper())
                ) { throwable ->
                    _hardwareState.value = _hardwareState.value.copy(
                        errorMessage = throwable.localizedMessage
                    )
                }
                effect.setOnDrawListener { frame ->
                    renderer.drawFrame(
                        canvas = frame.overlayCanvas,
                        snapshot = snapshotProvider(),
                        clearTransparent = true,
                        includeGuides = true
                    )
                    true
                }
                overlayEffect = effect

                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview)
                    .addUseCase(vidCapture)
                    .addEffect(effect)
                    .build()

                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, useCaseGroup)
                boundCamera = camera

                _hardwareState.value = _hardwareState.value.copy(
                    isBound = true,
                    isBackCamera = useBackCamera,
                    hasTorch = camera.cameraInfo.hasFlashUnit(),
                    isTorchOn = false,
                    errorMessage = null
                )
            }.onFailure { err ->
                _hardwareState.value = _hardwareState.value.copy(
                    isBound = false,
                    errorMessage = err.localizedMessage ?: "Unable to initialize camera"
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleTorch() {
        val cam = boundCamera ?: return
        if (!cam.cameraInfo.hasFlashUnit()) return
        val next = !_hardwareState.value.isTorchOn
        cam.cameraControl.enableTorch(next)
        _hardwareState.value = _hardwareState.value.copy(isTorchOn = next)
    }

    fun captureCurrentCameraBitmap(): Bitmap? {
        return currentPreviewView?.bitmap
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        brandName: String,
        hasAudioPermission: Boolean,
        isMuted: Boolean,
        onFinalized: (Uri?, Long, String?) -> Unit
    ) {
        val capture = videoCapture ?: run {
            onFinalized(null, 0L, "Camera not initialized")
            return
        }
        if (activeRecording != null) return

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeBrand = brandName.replace(Regex("[^A-Za-z0-9]"), "_").take(20)
        val displayName = "${safeBrand}_$stamp"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AsianPressLive")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        var pendingRecording = capture.output.prepareRecording(context, mediaStoreOutput)
        if (hasAudioPermission) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        val recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    activeRecording?.mute(isMuted)
                    _hardwareState.value = _hardwareState.value.copy(
                        isRecording = true,
                        recordingDurationSeconds = 0L
                    )
                }
                is VideoRecordEvent.Status -> {
                    val secs = event.recordingStats.recordedDurationNanos / 1_000_000_000L
                    val amp = event.recordingStats.audioStats.audioAmplitude.toFloat().coerceIn(0f, 1f)
                    _hardwareState.value = _hardwareState.value.copy(
                        isRecording = true,
                        recordingDurationSeconds = secs,
                        audioAmplitude = amp
                    )
                }
                is VideoRecordEvent.Finalize -> {
                    val secs = event.recordingStats.recordedDurationNanos / 1_000_000_000L
                    activeRecording = null
                    _hardwareState.value = _hardwareState.value.copy(
                        isRecording = false,
                        recordingDurationSeconds = 0L,
                        audioAmplitude = 0f
                    )
                    if (!event.hasError()) {
                        onFinalized(event.outputResults.outputUri, secs, null)
                    } else {
                        onFinalized(null, secs, "Recording error code: ${event.error}")
                    }
                }
            }
        }
        activeRecording = recording
    }

    fun setRecordingMuted(muted: Boolean) {
        activeRecording?.mute(muted)
    }

    fun stopRecording() {
        activeRecording?.stop()
    }

    fun saveBrandedSnapshot(snapshot: RenderSnapshot): Uri? {
        val camBmp = currentPreviewView?.bitmap
        val w = camBmp?.width ?: 1280
        val h = camBmp?.height ?: 720
        val outBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBmp)
        if (camBmp != null) {
            canvas.drawBitmap(camBmp, 0f, 0f, null)
        }
        renderer.drawFrame(canvas, snapshot, clearTransparent = false, includeGuides = false)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "AsianPress_Snap_$stamp")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AsianPressLive")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                outBmp.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
        }
        outBmp.recycle()
        return uri
    }

    fun release() {
        activeRecording?.stop()
        overlayEffect?.close()
        cameraProvider?.unbindAll()
    }
}
