package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.broadcast.BroadcastOverlayRenderer
import com.example.broadcast.LiveOverlayConfig
import com.example.broadcast.RenderSnapshot
import com.example.broadcast.StreamStatus
import com.example.broadcast.StreamTelemetry
import com.example.camera.CameraHardwareState
import com.example.camera.StudioCameraController
import com.example.data.AppDatabase
import com.example.data.BrandProfileEntity
import com.example.data.BroadcastSessionEntity
import com.example.data.LogoType
import com.example.data.OverlayPresetEntity
import com.example.data.SceneMode
import com.example.data.StreamDestinationEntity
import com.example.data.StudioRepository
import com.example.data.TransitionStyle
import com.example.streaming.RtmpLiveStreamer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudioUiState(
    val brands: List<BrandProfileEntity> = emptyList(),
    val activeBrand: BrandProfileEntity = BrandProfileEntity(
        businessName = "Asian Press",
        tagline = "Independent News & Live Production",
        phone = "+977-1-555-0192",
        email = "asianpress.np@gmail.com",
        website = "asianpress.media",
        socialHandle = "@asianpresslive"
    ),
    val overlayPresets: List<OverlayPresetEntity> = emptyList(),
    val streamDestinations: List<StreamDestinationEntity> = emptyList(),
    val broadcastSessions: List<BroadcastSessionEntity> = emptyList(),
    val liveOverlays: LiveOverlayConfig = LiveOverlayConfig(),
    val sceneMode: SceneMode = SceneMode.LIVE_CAMERA,
    val transitionDurationMs: Int = 450,
    val transitionProgress: Float = 0f,
    val isMuted: Boolean = false,
    val showSafeGuides: Boolean = false,
    val useBackCamera: Boolean = true,
    val cameraState: CameraHardwareState = CameraHardwareState(),
    val streamTelemetry: StreamTelemetry = StreamTelemetry(),
    val needsStreamKeyPrompt: Boolean = false
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudioRepository(
        context = application.applicationContext,
        database = AppDatabase.getInstance(application.applicationContext)
    )
    val renderer = BroadcastOverlayRenderer(application.applicationContext)
    private val cameraController = StudioCameraController(application.applicationContext, renderer)
    private val liveStreamer = RtmpLiveStreamer(application.applicationContext, renderer)

    private val _liveOverlays = MutableStateFlow(LiveOverlayConfig())
    private val _sceneMode = MutableStateFlow(SceneMode.LIVE_CAMERA)
    private val _transitionProgress = MutableStateFlow(0f)
    private val _isMuted = MutableStateFlow(false)
    private val _showSafeGuides = MutableStateFlow(false)
    private val _useBackCamera = MutableStateFlow(true)
    private val _needsStreamKeyPrompt = MutableStateFlow(false)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private var transitionJob: Job? = null
    private var liveStartTimestampMs: Long = 0L

    private val baseDataFlow = combine(
        repository.brands,
        repository.activeBrandId,
        repository.overlayPresets,
        repository.streamDestinations,
        repository.broadcastSessions
    ) { brands, activeId, presets, destinations, sessions ->
        BaseRepoSnapshot(brands, activeId, presets, destinations, sessions)
    }

    private val controlStateFlow = combine(
        _liveOverlays,
        _sceneMode,
        _transitionProgress,
        _isMuted,
        _showSafeGuides,
        _useBackCamera,
        _needsStreamKeyPrompt,
        repository.transitionDurationMs
    ) { values ->
        ControlSnapshot(
            overlays = values[0] as LiveOverlayConfig,
            sceneMode = values[1] as SceneMode,
            transitionProgress = values[2] as Float,
            isMuted = values[3] as Boolean,
            showSafeGuides = values[4] as Boolean,
            useBackCamera = values[5] as Boolean,
            needsStreamKeyPrompt = values[6] as Boolean,
            transitionDurationMs = values[7] as Int
        )
    }

    val uiState: StateFlow<StudioUiState> = combine(
        baseDataFlow,
        controlStateFlow,
        cameraController.hardwareState,
        liveStreamer.telemetry
    ) { repo, ctrl, cam, stream ->
        val active = repo.brands.firstOrNull { it.id == repo.activeBrandId }
            ?: repo.brands.firstOrNull { it.isDefault }
            ?: repo.brands.firstOrNull()
            ?: StudioUiState().activeBrand

        StudioUiState(
            brands = repo.brands,
            activeBrand = active,
            overlayPresets = repo.presets,
            streamDestinations = repo.destinations,
            broadcastSessions = repo.sessions,
            liveOverlays = ctrl.overlays,
            sceneMode = ctrl.sceneMode,
            transitionDurationMs = ctrl.transitionDurationMs,
            transitionProgress = ctrl.transitionProgress,
            isMuted = ctrl.isMuted,
            showSafeGuides = ctrl.showSafeGuides,
            useBackCamera = ctrl.useBackCamera,
            cameraState = cam,
            streamTelemetry = stream,
            needsStreamKeyPrompt = ctrl.needsStreamKeyPrompt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudioUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun currentRenderSnapshot(): RenderSnapshot {
        val state = uiState.value
        val isStreaming = state.streamTelemetry.status == StreamStatus.LIVE
        val elapsed = if (isStreaming) {
            state.streamTelemetry.elapsedSeconds
        } else {
            state.cameraState.recordingDurationSeconds
        }
        return RenderSnapshot(
            brand = state.activeBrand,
            overlays = state.liveOverlays,
            sceneMode = state.sceneMode,
            isRecording = state.cameraState.isRecording,
            isStreaming = isStreaming,
            elapsedSeconds = elapsed,
            transitionProgress = state.transitionProgress,
            transitionStyle = state.liveOverlays.transitionStyle,
            showSafeGuides = state.showSafeGuides
        )
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraController.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            useBackCamera = _useBackCamera.value,
            snapshotProvider = ::currentRenderSnapshot
        )
    }

    fun flipCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val nextBack = !_useBackCamera.value
        _useBackCamera.value = nextBack
        triggerTransitionEffect()
        cameraController.bindCamera(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            useBackCamera = nextBack,
            snapshotProvider = ::currentRenderSnapshot
        )
    }

    fun toggleTorch() = cameraController.toggleTorch()

    fun toggleMute() {
        val next = !_isMuted.value
        _isMuted.value = next
        cameraController.setRecordingMuted(next)
        liveStreamer.setMuted(next)
    }

    fun toggleSafeGuides() {
        _showSafeGuides.value = !_showSafeGuides.value
    }

    fun switchScene(mode: SceneMode) {
        if (_sceneMode.value == mode) return
        val style = _liveOverlays.value.transitionStyle
        if (style == TransitionStyle.CUT) {
            _sceneMode.value = mode
            return
        }
        runTransitionWithMidpointAction {
            _sceneMode.value = mode
        }
    }

    fun triggerTransitionEffect() {
        runTransitionWithMidpointAction {}
    }

    private fun runTransitionWithMidpointAction(onMidpoint: () -> Unit) {
        transitionJob?.cancel()
        val duration = uiState.value.transitionDurationMs.coerceIn(150, 1200)
        transitionJob = viewModelScope.launch {
            val steps = (duration / 16).coerceAtLeast(8)
            var midpointFired = false
            for (i in 1..steps) {
                val p = i.toFloat() / steps.toFloat()
                _transitionProgress.value = p
                if (!midpointFired && p >= 0.5f) {
                    midpointFired = true
                    onMidpoint()
                }
                delay(16L)
            }
            _transitionProgress.value = 0f
        }
    }

    // Recording & Snapshot
    fun toggleRecording(hasAudioPermission: Boolean) {
        if (uiState.value.cameraState.isRecording) {
            cameraController.stopRecording()
        } else {
            val brand = uiState.value.activeBrand
            cameraController.startRecording(
                brandName = brand.businessName,
                hasAudioPermission = hasAudioPermission,
                isMuted = _isMuted.value
            ) { uri, durationSecs, errorMsg ->
                viewModelScope.launch {
                    if (uri != null) {
                        repository.logBroadcastSession(
                            BroadcastSessionEntity(
                                title = "${brand.businessName} Recording",
                                brandName = brand.businessName,
                                mode = "RECORD",
                                platformsSummary = "Local MP4 (With Overlays)",
                                durationSeconds = durationSecs,
                                fileUri = uri.toString()
                            )
                        )
                        _events.tryEmit("Branded MP4 saved to Movies/AsianPressLive")
                    } else if (!errorMsg.isNullOrBlank()) {
                        _events.tryEmit(errorMsg)
                    }
                }
            }
        }
    }

    fun captureSnapshot() {
        viewModelScope.launch {
            val uri = cameraController.saveBrandedSnapshot(currentRenderSnapshot())
            if (uri != null) {
                _events.tryEmit("Branded snapshot saved to Pictures/AsianPressLive")
            } else {
                _events.tryEmit("Could not save snapshot")
            }
        }
    }

    // 1-Tap Social Media Go Live
    fun toggleLiveBroadcast(hasAudioPermission: Boolean) {
        val currentStatus = uiState.value.streamTelemetry.status
        if (currentStatus == StreamStatus.LIVE || currentStatus == StreamStatus.CONNECTING) {
            val elapsed = ((System.currentTimeMillis() - liveStartTimestampMs) / 1000L).coerceAtLeast(1L)
            val platforms = uiState.value.streamTelemetry.activePlatforms.joinToString(", ")
            val brandName = uiState.value.activeBrand.businessName
            liveStreamer.stopStream()
            if (currentStatus == StreamStatus.LIVE) {
                viewModelScope.launch {
                    repository.logBroadcastSession(
                        BroadcastSessionEntity(
                            title = "$brandName Live Broadcast",
                            brandName = brandName,
                            mode = if (uiState.value.streamTelemetry.activePlatforms.size > 1) "SIMULCAST" else "LIVESTREAM",
                            platformsSummary = platforms.ifBlank { "RTMP Live" },
                            durationSeconds = elapsed
                        )
                    )
                }
            }
            return
        }

        val readyDestinations = uiState.value.streamDestinations.filter {
            it.isArmed && it.streamKey.isNotBlank() && it.rtmpUrl.isNotBlank()
        }
        if (readyDestinations.isEmpty()) {
            _needsStreamKeyPrompt.value = true
            return
        }

        liveStartTimestampMs = System.currentTimeMillis()
        liveStreamer.startStream(
            destinations = readyDestinations,
            hasAudioPermission = hasAudioPermission,
            snapshotProvider = ::currentRenderSnapshot,
            cameraFrameProvider = {
                if (_sceneMode.value == SceneMode.LIVE_CAMERA) {
                    cameraController.captureCurrentCameraBitmap()
                } else {
                    null
                }
            }
        )
    }

    fun dismissStreamKeyPrompt() {
        _needsStreamKeyPrompt.value = false
    }

    // Overlay Live Controls
    fun updateOverlayConfig(transform: (LiveOverlayConfig) -> LiveOverlayConfig) {
        _liveOverlays.value = transform(_liveOverlays.value)
    }

    fun loadPreset(preset: OverlayPresetEntity) {
        runTransitionWithMidpointAction {
            _liveOverlays.value = LiveOverlayConfig.fromPreset(preset)
        }
        _events.tryEmit("Loaded preset: ${preset.name}")
    }

    fun saveCurrentAsPreset(name: String) {
        val current = _liveOverlays.value
        viewModelScope.launch {
            repository.saveOverlayPreset(
                OverlayPresetEntity(
                    name = name.ifBlank { "Custom Studio Preset" },
                    showWatermark = current.showWatermark,
                    showLowerThird = current.showLowerThird,
                    lowerThirdTitle = current.lowerThirdTitle,
                    lowerThirdSubtitle = current.lowerThirdSubtitle,
                    showTicker = current.showTicker,
                    tickerText = current.tickerText,
                    showContactPill = current.showContactPill,
                    showLiveBug = current.showLiveBug,
                    showClockBug = current.showClockBug,
                    showPromoCard = current.showPromoCard,
                    promoTitle = current.promoTitle,
                    promoCta = current.promoCta,
                    transitionStyle = current.transitionStyle.name
                )
            )
            _events.tryEmit("Saved overlay preset: $name")
        }
    }

    fun deletePreset(preset: OverlayPresetEntity) {
        viewModelScope.launch {
            repository.deleteOverlayPreset(preset)
        }
    }

    fun setTransitionDuration(durationMs: Int) {
        viewModelScope.launch {
            repository.setTransitionDurationMs(durationMs)
        }
    }

    // Brand Management
    fun selectActiveBrand(brand: BrandProfileEntity) {
        runTransitionWithMidpointAction {
            viewModelScope.launch {
                repository.setActiveBrandId(brand.id)
            }
        }
        _events.tryEmit("Switched On-Air Brand: ${brand.businessName}")
    }

    fun saveBrandProfile(profile: BrandProfileEntity, setAsActive: Boolean = true) {
        viewModelScope.launch {
            val savedId = repository.saveBrand(profile)
            if (setAsActive) {
                repository.setActiveBrandId(savedId)
            }
            _events.tryEmit("Saved brand: ${profile.businessName}")
        }
    }

    fun deleteBrandProfile(profile: BrandProfileEntity) {
        viewModelScope.launch {
            repository.deleteBrand(profile)
            _events.tryEmit("Deleted brand: ${profile.businessName}")
        }
    }

    suspend fun importLogoFromGallery(uri: Uri): String? {
        return repository.copyLogoToAppStorage(uri)
    }

    // Social Destinations & Sessions
    fun toggleDestinationArmed(destination: StreamDestinationEntity) {
        viewModelScope.launch {
            repository.saveStreamDestination(destination.copy(isArmed = !destination.isArmed))
        }
    }

    fun saveStreamDestination(destination: StreamDestinationEntity) {
        viewModelScope.launch {
            repository.saveStreamDestination(destination)
            _events.tryEmit("Updated ${destination.displayName}")
        }
    }

    fun deleteSession(session: BroadcastSessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.release()
        liveStreamer.stopStream()
    }

    private data class BaseRepoSnapshot(
        val brands: List<BrandProfileEntity>,
        val activeBrandId: Long?,
        val presets: List<OverlayPresetEntity>,
        val destinations: List<StreamDestinationEntity>,
        val sessions: List<BroadcastSessionEntity>
    )

    private data class ControlSnapshot(
        val overlays: LiveOverlayConfig,
        val sceneMode: SceneMode,
        val transitionProgress: Float,
        val isMuted: Boolean,
        val showSafeGuides: Boolean,
        val useBackCamera: Boolean,
        val needsStreamKeyPrompt: Boolean,
        val transitionDurationMs: Int
    )
}
