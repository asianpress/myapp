package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.broadcast.BroadcastOverlayRenderer
import com.example.data.BroadcastSessionEntity
import com.example.data.SocialPlatform
import com.example.data.StreamDestinationEntity
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.theme.AsianPressCrimson
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.StudioGold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StreamsScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBackToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var editingDestination by remember { mutableStateOf<StreamDestinationEntity?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.streams_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.streams_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.section_social_destinations),
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioGold
                )
                TextButton(onClick = onBackToStudio) {
                    Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Studio")
                }
            }
        }

        items(uiState.streamDestinations, key = { it.id }) { dest ->
            StreamDestinationCard(
                destination = dest,
                onToggleArmed = { viewModel.toggleDestinationArmed(dest) },
                onConfigure = { editingDestination = dest }
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.section_recordings),
                style = MaterialTheme.typography.titleMedium,
                color = StudioGold
            )
        }

        if (uiState.broadcastSessions.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_recordings_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.empty_recordings_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.broadcastSessions, key = { it.id }) { session ->
                BroadcastSessionCard(
                    session = session,
                    onPlay = {
                        session.fileUri?.let { uriStr ->
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(uriStr), "video/mp4")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    onShare = {
                        session.fileUri?.let { uriStr ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/mp4"
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                putExtra(Intent.EXTRA_TEXT, "Broadcast produced with ${session.brandName} via Asian Press Live")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Branded Video"))
                            }
                        }
                    },
                    onDelete = { viewModel.deleteSession(session) }
                )
            }
        }
    }

    editingDestination?.let { dest ->
        ConfigureDestinationDialog(
            destination = dest,
            onDismiss = { editingDestination = null },
            onSave = { updated ->
                viewModel.saveStreamDestination(updated)
                editingDestination = null
            }
        )
    }
}

@Composable
private fun StreamDestinationCard(
    destination: StreamDestinationEntity,
    onToggleArmed: () -> Unit,
    onConfigure: () -> Unit
) {
    val platformEnum = runCatching { SocialPlatform.valueOf(destination.platform) }
        .getOrDefault(SocialPlatform.CUSTOM_RTMP)
    val brandColor = Color(platformEnum.brandHex)
    val isReady = destination.streamKey.isNotBlank() && destination.rtmpUrl.isNotBlank()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (destination.isArmed) 1.5.dp else 1.dp,
                color = if (destination.isArmed) SignalEmerald else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.2f))
                    .border(1.dp, brandColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Radio,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isReady) {
                        "${destination.resolutionPreset.replace("_", " @ ")}fps • ${destination.bitrateKbps}kbps • Key Ready"
                    } else {
                        "Tap Configure to add Stream Key"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReady) SignalEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onConfigure,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("configure_dest_${destination.platform.lowercase()}")
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.action_configure))
            }

            Switch(
                checked = destination.isArmed,
                onCheckedChange = { onToggleArmed() },
                modifier = Modifier.testTag("arm_dest_${destination.platform.lowercase()}")
            )
        }
    }
}

@Composable
private fun BroadcastSessionCard(
    session: BroadcastSessionEntity,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${session.platformsSummary} • ${BroadcastOverlayRenderer.formatDuration(session.durationSeconds)} • ${dateFormat.format(Date(session.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!session.fileUri.isNullOrBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_open_clip))
                    }
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.action_share_clip))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigureDestinationDialog(
    destination: StreamDestinationEntity,
    onDismiss: () -> Unit,
    onSave: (StreamDestinationEntity) -> Unit
) {
    var displayName by remember { mutableStateOf(destination.displayName) }
    var rtmpUrl by remember { mutableStateOf(destination.rtmpUrl) }
    var streamKey by remember { mutableStateOf(destination.streamKey) }
    var showKey by remember { mutableStateOf(false) }
    var resolution by remember { mutableStateOf(destination.resolutionPreset) }
    var bitrate by remember { mutableIntStateOf(destination.bitrateKbps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${destination.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rtmpUrl,
                    onValueChange = { rtmpUrl = it },
                    label = { Text("RTMP / RTMPS URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    label = { Text("Stream Key") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle stream key visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_stream_key_input")
                )

                Text("Bitrate", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2500, 4500, 6000).forEach { kbps ->
                        FilterChip(
                            selected = bitrate == kbps,
                            onClick = { bitrate = kbps },
                            label = { Text("${kbps / 1000f}M") }
                        )
                    }
                }

                Text("Resolution & FPS", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("720P_30" to "720p30", "1080P_30" to "1080p30", "720P_60" to "720p60").forEach { (id, lbl) ->
                        FilterChip(
                            selected = resolution == id,
                            onClick = { resolution = id },
                            label = { Text(lbl) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        destination.copy(
                            displayName = displayName.trim().ifBlank { destination.displayName },
                            rtmpUrl = rtmpUrl.trim(),
                            streamKey = streamKey.trim(),
                            resolutionPreset = resolution,
                            bitrateKbps = bitrate,
                            isArmed = if (streamKey.isNotBlank()) true else destination.isArmed
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
