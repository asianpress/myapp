package com.example.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.R
import com.example.broadcast.BroadcastOverlayRenderer
import com.example.broadcast.RenderSnapshot
import com.example.broadcast.StreamStatus
import com.example.broadcast.StreamTelemetry
import com.example.data.BrandProfileEntity
import com.example.data.LogoType
import com.example.data.SceneMode
import com.example.ui.theme.AsianPressCrimson
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.StudioGold
import com.example.ui.theme.StudioObsidian
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun BroadcastViewfinder(
    hasCameraPermission: Boolean,
    isCameraBound: Boolean,
    sceneMode: SceneMode,
    renderer: BroadcastOverlayRenderer,
    snapshotProvider: () -> RenderSnapshot,
    onPreviewViewReady: (PreviewView) -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Frame ticker drives smooth scrolling ticker & transitions when rendering via Compose Canvas
    var frameTick by remember { mutableLongStateOf(0L) }
    val showComposeCanvas = !hasCameraPermission || !isCameraBound || sceneMode != SceneMode.LIVE_CAMERA

    LaunchedEffect(showComposeCanvas) {
        while (showComposeCanvas) {
            frameTick++
            delay(33L)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StudioObsidian)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        onPreviewViewReady(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showComposeCanvas) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION")
                frameTick
                drawIntoCanvas { canvas ->
                    renderer.drawFrame(
                        canvas = canvas.nativeCanvas,
                        snapshot = snapshotProvider(),
                        clearTransparent = false,
                        includeGuides = true
                    )
                }
            }
        }

        if (!hasCameraPermission) {
            Surface(
                color = StudioObsidian.copy(alpha = 0.86f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideocamOff,
                        contentDescription = null,
                        tint = StudioGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.camera_permission_rationale),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson),
                        modifier = Modifier.testTag("grant_camera_permissions_button")
                    ) {
                        Text(text = stringResource(R.string.grant_camera_permissions))
                    }
                }
            }
        }
    }
}

@Composable
fun BrandAvatarBadge(
    brand: BrandProfileEntity,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(brand.primaryColorHex)
    val accentColor = Color(brand.accentColorHex)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(primaryColor.copy(alpha = 0.2f))
            .border(1.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
    ) {
        when (brand.logoType) {
            LogoType.BUILTIN_ASIAN_PRESS.name -> {
                Image(
                    painter = painterResource(id = R.drawable.img_asian_press_logo),
                    contentDescription = "${brand.businessName} logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            LogoType.CUSTOM_FILE.name if !brand.logoPath.isNullOrBlank() && File(brand.logoPath).exists() -> {
                AsyncImage(
                    model = File(brand.logoPath),
                    contentDescription = "${brand.businessName} custom logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val initials = brand.businessName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifBlank { "AP" }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(primaryColor)
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusTelemetryPill(
    isRecording: Boolean,
    recordingSeconds: Long,
    streamTelemetry: StreamTelemetry,
    modifier: Modifier = Modifier
) {
    val isLive = streamTelemetry.status == StreamStatus.LIVE
    val isConnecting = streamTelemetry.status == StreamStatus.CONNECTING

    val containerColor by animateColorAsState(
        targetValue = when {
            isLive -> AsianPressCrimson
            isConnecting -> StudioGold
            isRecording -> AsianPressCrimson.copy(alpha = 0.9f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "status_pill_color"
    )
    val contentColor = if (isConnecting) StudioObsidian else Color.White

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = if (isLive) Icons.Default.Radio else Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint = if (isLive || isRecording) Color.White else SignalEmerald,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val label = when {
                isLive -> "ON AIR ${BroadcastOverlayRenderer.formatDuration(streamTelemetry.elapsedSeconds)}"
                isConnecting -> stringResource(R.string.status_connecting)
                isRecording -> "REC ${BroadcastOverlayRenderer.formatDuration(recordingSeconds)}"
                else -> stringResource(R.string.status_standby)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isLive && streamTelemetry.bitrateKbps > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${streamTelemetry.bitrateKbps}kbps",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun AudioLevelMeter(
    amplitude: Float,
    isMuted: Boolean,
    modifier: Modifier = Modifier
) {
    val bars = 6
    val activeBars = if (isMuted) 0 else (amplitude * bars * 1.4f).toInt().coerceIn(0, bars)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        repeat(bars) { index ->
            val color = when {
                index >= activeBars -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                index >= 5 -> AsianPressCrimson
                index >= 3 -> StudioGold
                else -> SignalEmerald
            }
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((8 + index * 2).dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
