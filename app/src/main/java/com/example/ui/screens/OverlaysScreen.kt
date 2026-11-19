package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.broadcast.LiveOverlayConfig
import com.example.broadcast.RenderSnapshot
import com.example.data.OverlayPresetEntity
import com.example.data.SceneMode
import com.example.data.TransitionStyle
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.theme.AsianPressCrimson
import com.example.ui.theme.StudioGold

@Composable
fun OverlaysScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onLoadToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.overlays_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.overlays_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.overlayPresets, key = { it.id }) { preset ->
                OverlayPresetCard(
                    preset = preset,
                    uiState = uiState,
                    viewModel = viewModel,
                    onLoad = {
                        viewModel.loadPreset(preset)
                        onLoadToStudio()
                    },
                    onDelete = if (uiState.overlayPresets.size > 1) {
                        { viewModel.deletePreset(preset) }
                    } else null
                )
            }
        }

        FloatingActionButton(
            onClick = { showSaveDialog = true },
            containerColor = AsianPressCrimson,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("save_current_preset_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new_preset))
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Current Studio Overlays") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Saves your active toggle switches, lower-third headline, ticker copy, promo card, and transition style.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Preset Name") },
                        placeholder = { Text("e.g. Evening News Desk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentAsPreset(name.trim().ifBlank { "Studio Preset" })
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson)
                ) {
                    Text(stringResource(R.string.action_save_preset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun OverlayPresetCard(
    preset: OverlayPresetEntity,
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onLoad: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val transitionLabel = runCatching { TransitionStyle.valueOf(preset.transitionStyle).label }
        .getOrDefault("Press Stinger")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = StudioGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Transition: $transitionLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = StudioGold
                    )
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete preset",
                            tint = AsianPressCrimson
                        )
                    }
                }
            }

            // Live Mini Preview rendered through BroadcastOverlayRenderer
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                drawIntoCanvas { canvas ->
                    viewModel.renderer.drawFrame(
                        canvas = canvas.nativeCanvas,
                        snapshot = RenderSnapshot(
                            brand = uiState.activeBrand,
                            overlays = LiveOverlayConfig.fromPreset(preset),
                            sceneMode = SceneMode.STUDIO_SLATE,
                            isRecording = false,
                            isStreaming = false,
                            elapsedSeconds = 0L,
                            transitionProgress = 0f,
                            transitionStyle = TransitionStyle.CUT
                        ),
                        clearTransparent = false,
                        includeGuides = false
                    )
                }
            }

            val layers = buildList {
                if (preset.showWatermark) add("Watermark")
                if (preset.showLowerThird) add("Lower Third")
                if (preset.showTicker) add("Ticker")
                if (preset.showContactPill) add("Contact")
                if (preset.showPromoCard) add("Promo CTA")
            }.joinToString(" • ")

            Text(
                text = "Active Layers: $layers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onLoad,
                colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("load_preset_${preset.id}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.action_load_preset))
            }
        }
    }
}
