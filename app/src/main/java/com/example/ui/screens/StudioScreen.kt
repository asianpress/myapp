package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.broadcast.StreamStatus
import com.example.data.SceneMode
import com.example.data.SocialPlatform
import com.example.data.TransitionStyle
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.components.AudioLevelMeter
import com.example.ui.components.BrandAvatarBadge
import com.example.ui.components.BroadcastViewfinder
import com.example.ui.components.StatusTelemetryPill
import com.example.ui.theme.AsianPressCrimson
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.StudioGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onNavigateToStreams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var showBrandSwitcherSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPerm = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasAudioPerm = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasCameraPerm, previewViewRef) {
        val pv = previewViewRef
        if (hasCameraPerm && pv != null) {
            viewModel.bindCamera(lifecycleOwner, pv)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideStudio = maxWidth >= 720.dp

        if (isWideStudio) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    StudioHudBar(
                        uiState = uiState,
                        onOpenBrandSwitcher = { showBrandSwitcherSheet = true },
                        onFlipCamera = {
                            previewViewRef?.let { viewModel.flipCamera(lifecycleOwner, it) }
                        },
                        onToggleTorch = viewModel::toggleTorch,
                        onToggleMute = viewModel::toggleMute,
                        onToggleGuides = viewModel::toggleSafeGuides
                    )
                    BroadcastViewfinder(
                        hasCameraPermission = hasCameraPerm,
                        isCameraBound = uiState.cameraState.isBound,
                        sceneMode = uiState.sceneMode,
                        renderer = viewModel.renderer,
                        snapshotProvider = viewModel::currentRenderSnapshot,
                        onPreviewViewReady = { pv ->
                            previewViewRef = pv
                            if (hasCameraPerm) viewModel.bindCamera(lifecycleOwner, pv)
                        },
                        onRequestPermissions = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    SceneSwitcherStrip(
                        activeScene = uiState.sceneMode,
                        onSelectScene = viewModel::switchScene
                    )
                    BroadcastActionDock(
                        uiState = uiState,
                        onToggleLive = { viewModel.toggleLiveBroadcast(hasAudioPerm) },
                        onToggleRecord = { viewModel.toggleRecording(hasAudioPerm) },
                        onSnapshot = viewModel::captureSnapshot,
                        onOpenStreams = onNavigateToStreams
                    )
                }

                ControlDeckPanel(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                StudioHudBar(
                    uiState = uiState,
                    onOpenBrandSwitcher = { showBrandSwitcherSheet = true },
                    onFlipCamera = {
                        previewViewRef?.let { viewModel.flipCamera(lifecycleOwner, it) }
                    },
                    onToggleTorch = viewModel::toggleTorch,
                    onToggleMute = viewModel::toggleMute,
                    onToggleGuides = viewModel::toggleSafeGuides
                )

                BroadcastViewfinder(
                    hasCameraPermission = hasCameraPerm,
                    isCameraBound = uiState.cameraState.isBound,
                    sceneMode = uiState.sceneMode,
                    renderer = viewModel.renderer,
                    snapshotProvider = viewModel::currentRenderSnapshot,
                    onPreviewViewReady = { pv ->
                        previewViewRef = pv
                        if (hasCameraPerm) viewModel.bindCamera(lifecycleOwner, pv)
                    },
                    onRequestPermissions = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                )

                SceneSwitcherStrip(
                    activeScene = uiState.sceneMode,
                    onSelectScene = viewModel::switchScene
                )

                ControlDeckPanel(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                BroadcastActionDock(
                    uiState = uiState,
                    onToggleLive = { viewModel.toggleLiveBroadcast(hasAudioPerm) },
                    onToggleRecord = { viewModel.toggleRecording(hasAudioPerm) },
                    onSnapshot = viewModel::captureSnapshot,
                    onOpenStreams = onNavigateToStreams
                )
            }
        }
    }

    if (showBrandSwitcherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBrandSwitcherSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Hot-Swap On-Air Brand",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Switch watermark, logo, and contact info instantly during recording or broadcast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.brands, key = { it.id }) { brand ->
                        val isActive = brand.id == uiState.activeBrand.id
                        Surface(
                            color = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectActiveBrand(brand)
                                    showBrandSwitcherSheet = false
                                }
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) AsianPressCrimson else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                BrandAvatarBadge(brand = brand, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = brand.businessName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = listOf(brand.phone, brand.website).filter { it.isNotBlank() }.joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active brand",
                                        tint = SignalEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.needsStreamKeyPrompt) {
        QuickStreamKeyDialog(
            uiState = uiState,
            onDismiss = viewModel::dismissStreamKeyPrompt,
            onSaveAndGoLive = { dest, rtmpUrl, key ->
                viewModel.saveStreamDestination(
                    dest.copy(rtmpUrl = rtmpUrl, streamKey = key, isArmed = true)
                )
                viewModel.dismissStreamKeyPrompt()
                viewModel.toggleLiveBroadcast(hasAudioPerm)
            },
            onGoToSocialSettings = {
                viewModel.dismissStreamKeyPrompt()
                onNavigateToStreams()
            }
        )
    }
}

@Composable
private fun StudioHudBar(
    uiState: StudioUiState,
    onOpenBrandSwitcher: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleGuides: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenBrandSwitcher)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                .testTag("active_brand_switcher_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
            ) {
                BrandAvatarBadge(brand = uiState.activeBrand, size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = uiState.activeBrand.businessName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Switch brand",
                    tint = StudioGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        StatusTelemetryPill(
            isRecording = uiState.cameraState.isRecording,
            recordingSeconds = uiState.cameraState.recordingDurationSeconds,
            streamTelemetry = uiState.streamTelemetry
        )

        Spacer(modifier = Modifier.weight(1f))

        AudioLevelMeter(
            amplitude = uiState.cameraState.audioAmplitude,
            isMuted = uiState.isMuted
        )

        IconButton(
            onClick = onToggleMute,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("toggle_mute_button")
        ) {
            Icon(
                imageVector = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = stringResource(R.string.action_toggle_mute),
                tint = if (uiState.isMuted) AsianPressCrimson else MaterialTheme.colorScheme.onSurface
            )
        }

        if (uiState.cameraState.hasTorch) {
            IconButton(
                onClick = onToggleTorch,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_torch_button")
            ) {
                Icon(
                    imageVector = if (uiState.cameraState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = stringResource(R.string.action_toggle_torch),
                    tint = if (uiState.cameraState.isTorchOn) StudioGold else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        IconButton(
            onClick = onToggleGuides,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("toggle_guides_button")
        ) {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = stringResource(R.string.action_toggle_guides),
                tint = if (uiState.showSafeGuides) StudioGold else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(
            onClick = onFlipCamera,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("flip_camera_button")
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = stringResource(R.string.action_flip_camera),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SceneSwitcherStrip(
    activeScene: SceneMode,
    onSelectScene: (SceneMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = "SCENE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SceneMode.entries.forEach { scene ->
            val selected = scene == activeScene
            FilterChip(
                selected = selected,
                onClick = { onSelectScene(scene) },
                label = {
                    Text(
                        text = scene.label,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AsianPressCrimson,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("scene_chip_${scene.name.lowercase()}")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlDeckPanel(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_overlays),
        stringResource(R.string.tab_transitions),
        stringResource(R.string.tab_live_copy)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("studio_deck_tab_$idx")
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> OverlaysControlTab(uiState = uiState, viewModel = viewModel)
                    1 -> TransitionsControlTab(uiState = uiState, viewModel = viewModel)
                    2 -> LiveCopyEditorTab(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun OverlaysControlTab(uiState: StudioUiState, viewModel: StudioViewModel) {
    val ov = uiState.liveOverlays
    val toggles = listOf(
        Triple("Brand Watermark", ov.showWatermark) {
            viewModel.updateOverlayConfig { it.copy(showWatermark = !it.showWatermark) }
        },
        Triple("Lower Third", ov.showLowerThird) {
            viewModel.updateOverlayConfig { it.copy(showLowerThird = !it.showLowerThird) }
        },
        Triple("News Ticker", ov.showTicker) {
            viewModel.updateOverlayConfig { it.copy(showTicker = !it.showTicker) }
        },
        Triple("Contact Pill", ov.showContactPill) {
            viewModel.updateOverlayConfig { it.copy(showContactPill = !it.showContactPill) }
        },
        Triple("LIVE/REC Bug", ov.showLiveBug) {
            viewModel.updateOverlayConfig { it.copy(showLiveBug = !it.showLiveBug) }
        },
        Triple("Timecode Clock", ov.showClockBug) {
            viewModel.updateOverlayConfig { it.copy(showClockBug = !it.showClockBug) }
        },
        Triple("Promo CTA Card", ov.showPromoCard) {
            viewModel.updateOverlayConfig { it.copy(showPromoCard = !it.showPromoCard) }
        }
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        toggles.chunked(2).forEach { pair ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                pair.forEach { (label, active, onClick) ->
                    FilterChip(
                        selected = active,
                        onClick = onClick,
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = if (active) {
                            {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = SignalEmerald
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("overlay_toggle_${label.lowercase().replace(" ", "_")}")
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TransitionsControlTab(uiState: StudioUiState, viewModel: StudioViewModel) {
    val activeStyle = uiState.liveOverlays.transitionStyle
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Real-Time Scene & Overlay Transition",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            TransitionStyle.entries.forEach { style ->
                val selected = style == activeStyle
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.updateOverlayConfig { it.copy(transitionStyle = style) }
                    },
                    label = { Text(style.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioGold,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("transition_style_${style.name.lowercase()}")
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Duration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            listOf(250 to "Fast", 450 to "Pro", 750 to "Smooth").forEach { (ms, title) ->
                FilterChip(
                    selected = uiState.transitionDurationMs == ms,
                    onClick = { viewModel.setTransitionDuration(ms) },
                    label = { Text("$title (${ms}ms)") }
                )
            }
        }

        Button(
            onClick = viewModel::triggerTransitionEffect,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trigger_transition_button")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.action_trigger_stinger),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveCopyEditorTab(uiState: StudioUiState, viewModel: StudioViewModel) {
    val ov = uiState.liveOverlays
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = ov.lowerThirdTitle,
            onValueChange = { text -> viewModel.updateOverlayConfig { it.copy(lowerThirdTitle = text) } },
            label = { Text("Lower Third Headline") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_lower_third_title_input")
        )
        OutlinedTextField(
            value = ov.lowerThirdSubtitle,
            onValueChange = { text -> viewModel.updateOverlayConfig { it.copy(lowerThirdSubtitle = text) } },
            label = { Text("Lower Third Subtitle") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = ov.tickerText,
            onValueChange = { text -> viewModel.updateOverlayConfig { it.copy(tickerText = text) } },
            label = { Text("Scrolling Ticker Copy") },
            maxLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_ticker_input")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = ov.promoTitle,
                onValueChange = { text -> viewModel.updateOverlayConfig { it.copy(promoTitle = text) } },
                label = { Text("Promo Title") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = ov.promoCta,
                onValueChange = { text -> viewModel.updateOverlayConfig { it.copy(promoCta = text) } },
                label = { Text("Promo CTA") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BroadcastActionDock(
    uiState: StudioUiState,
    onToggleLive: () -> Unit,
    onToggleRecord: () -> Unit,
    onSnapshot: () -> Unit,
    onOpenStreams: () -> Unit
) {
    val isLive = uiState.streamTelemetry.status == StreamStatus.LIVE ||
        uiState.streamTelemetry.status == StreamStatus.CONNECTING
    val isRecording = uiState.cameraState.isRecording
    val armed = uiState.streamDestinations.filter { it.isArmed }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "SOCIAL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (armed.isEmpty()) {
                        Text(
                            text = "Tap to arm YouTube, Facebook, Twitch...",
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(onClick = onOpenStreams)
                        )
                    } else {
                        armed.take(3).forEach { dest ->
                            val hasKey = dest.streamKey.isNotBlank()
                            Surface(
                                color = if (hasKey) SignalEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable(onClick = onOpenStreams)
                            ) {
                                Text(
                                    text = dest.displayName.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasKey) SignalEmerald else StudioGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
                if (uiState.streamTelemetry.status == StreamStatus.ERROR) {
                    Text(
                        text = uiState.streamTelemetry.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = AsianPressCrimson,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onToggleLive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) SignalEmerald else AsianPressCrimson
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .height(48.dp)
                        .testTag("go_live_button")
                ) {
                    Icon(
                        imageVector = if (isLive) Icons.Default.Stop else Icons.Default.Radio,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (isLive) R.string.action_stop_live else R.string.action_go_live),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onToggleRecord,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isRecording) AsianPressCrimson.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (isRecording) AsianPressCrimson else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("record_mp4_button")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = AsianPressCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(if (isRecording) R.string.action_stop_record else R.string.action_record),
                        maxLines = 1
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onSnapshot)
                        .testTag("snapshot_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.action_snapshot),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStreamKeyDialog(
    uiState: StudioUiState,
    onDismiss: () -> Unit,
    onSaveAndGoLive: (com.example.data.StreamDestinationEntity, String, String) -> Unit,
    onGoToSocialSettings: () -> Unit
) {
    val defaultDest = uiState.streamDestinations.firstOrNull { it.isArmed }
        ?: uiState.streamDestinations.firstOrNull()
    if (defaultDest == null) {
        onDismiss()
        return
    }
    var rtmpUrl by remember { mutableStateOf(defaultDest.rtmpUrl) }
    var streamKey by remember { mutableStateOf(defaultDest.streamKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "1-Tap Go Live: ${defaultDest.displayName}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste your ${defaultDest.displayName} Stream Key once to enable instant 1-tap broadcasting with your Asian Press / studio overlays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rtmpUrl,
                    onValueChange = { rtmpUrl = it },
                    label = { Text("RTMP Ingest URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    label = { Text("Stream Key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_stream_key_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndGoLive(defaultDest, rtmpUrl, streamKey) },
                enabled = streamKey.isNotBlank() && rtmpUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save & Go Live")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onGoToSocialSettings) {
                    Text("All Platforms")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
