package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.broadcast.LiveOverlayConfig
import com.example.broadcast.RenderSnapshot
import com.example.data.BrandProfileEntity
import com.example.data.LogoType
import com.example.data.SceneMode
import com.example.data.TransitionStyle
import com.example.data.WatermarkPosition
import com.example.data.WatermarkStyle
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.components.BrandAvatarBadge
import com.example.ui.theme.AsianPressCrimson
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.StudioGold
import kotlinx.coroutines.launch

@Composable
fun BrandsScreen(
    uiState: StudioUiState,
    viewModel: StudioViewModel,
    onBackToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingBrand by remember { mutableStateOf<BrandProfileEntity?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.brands_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.brands_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.brands, key = { it.id }) { brand ->
                val isActive = brand.id == uiState.activeBrand.id
                BrandProfileCard(
                    brand = brand,
                    isActive = isActive,
                    onSetActive = {
                        viewModel.selectActiveBrand(brand)
                        onBackToStudio()
                    },
                    onEdit = { editingBrand = brand },
                    onDelete = if (!brand.isDefault && uiState.brands.size > 1) {
                        { viewModel.deleteBrandProfile(brand) }
                    } else null
                )
            }
        }

        FloatingActionButton(
            onClick = {
                editingBrand = BrandProfileEntity(
                    businessName = "",
                    tagline = "Live Broadcast Studio",
                    phone = "",
                    email = "",
                    website = "",
                    socialHandle = "",
                    logoType = LogoType.MONOGRAM.name
                )
            },
            containerColor = AsianPressCrimson,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_brand_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new_brand))
        }
    }

    editingBrand?.let { initial ->
        BrandEditorSheet(
            initial = initial,
            viewModel = viewModel,
            onDismiss = { editingBrand = null },
            onSave = { updated ->
                viewModel.saveBrandProfile(updated, setAsActive = true)
                editingBrand = null
            }
        )
    }
}

@Composable
private fun BrandProfileCard(
    brand: BrandProfileEntity,
    isActive: Boolean,
    onSetActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val primaryColor = Color(brand.primaryColorHex)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) AsianPressCrimson else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                BrandAvatarBadge(brand = brand, size = 48.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = brand.businessName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Surface(
                                color = SignalEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_active_on_air),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SignalEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = brand.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }

            val contactSummary = listOf(brand.phone, brand.email, brand.website, brand.socialHandle)
                .filter { it.isNotBlank() }
                .joinToString("  •  ")
            if (contactSummary.isNotBlank()) {
                Text(
                    text = contactSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val posLabel = runCatching { WatermarkPosition.valueOf(brand.watermarkPosition).label }
                .getOrDefault("Top Left")
            val styleLabel = runCatching { WatermarkStyle.valueOf(brand.watermarkStyle).label }
                .getOrDefault("Badge")

            Text(
                text = "Watermark: $styleLabel • $posLabel • ${(brand.watermarkOpacity * 100).toInt()}% opacity",
                style = MaterialTheme.typography.labelSmall,
                color = StudioGold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSetActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) SignalEmerald else AsianPressCrimson
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("set_active_brand_${brand.id}")
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Check else Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActive) "Active On Air" else stringResource(R.string.action_set_active_brand)
                    )
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_brand_${brand.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete_brand),
                            tint = AsianPressCrimson
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandEditorSheet(
    initial: BrandProfileEntity,
    viewModel: StudioViewModel,
    onDismiss: () -> Unit,
    onSave: (BrandProfileEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var businessName by remember { mutableStateOf(initial.businessName) }
    var tagline by remember { mutableStateOf(initial.tagline) }
    var phone by remember { mutableStateOf(initial.phone) }
    var email by remember { mutableStateOf(initial.email) }
    var website by remember { mutableStateOf(initial.website) }
    var socialHandle by remember { mutableStateOf(initial.socialHandle) }
    var logoType by remember { mutableStateOf(initial.logoType) }
    var logoPath by remember { mutableStateOf(initial.logoPath) }
    var primaryColorHex by remember { mutableLongStateOf(initial.primaryColorHex) }
    var accentColorHex by remember { mutableLongStateOf(initial.accentColorHex) }
    var wmPosition by remember { mutableStateOf(initial.watermarkPosition) }
    var wmStyle by remember { mutableStateOf(initial.watermarkStyle) }
    var wmOpacity by remember { mutableFloatStateOf(initial.watermarkOpacity) }
    var wmScale by remember { mutableFloatStateOf(initial.watermarkScale) }

    val draftBrand = initial.copy(
        businessName = businessName.ifBlank { "Studio Preview" },
        tagline = tagline,
        phone = phone,
        email = email,
        website = website,
        socialHandle = socialHandle,
        logoType = logoType,
        logoPath = logoPath,
        primaryColorHex = primaryColorHex,
        accentColorHex = accentColorHex,
        watermarkPosition = wmPosition,
        watermarkStyle = wmStyle,
        watermarkOpacity = wmOpacity,
        watermarkScale = wmScale
    )

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copied = viewModel.importLogoFromGallery(uri)
                if (copied != null) {
                    logoPath = copied
                    logoType = LogoType.CUSTOM_FILE.name
                }
            }
        }
    }

    val colorSwatches = listOf(
        0xFFE5243B to 0xFFF5A623,
        0xFF0EA5E9 to 0xFFF59E0B,
        0xFF10B981 to 0xFF34D399,
        0xFF8B5CF6 to 0xFFF472B6,
        0xFFF97316 to 0xFFFACC15,
        0xFF1E293B to 0xFFE5243B
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (initial.id == 0L) "Create Studio / Client Brand" else "Customize Brand & Watermark",
                style = MaterialTheme.typography.titleLarge
            )

            // Live Interactive Watermark Preview Canvas
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
                            brand = draftBrand,
                            overlays = LiveOverlayConfig(
                                showWatermark = true,
                                showLowerThird = false,
                                showTicker = false,
                                showContactPill = true,
                                showLiveBug = true,
                                showClockBug = false
                            ),
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

            // Logo Source Buttons
            Text("Studio Logo", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = logoType == LogoType.CUSTOM_FILE.name,
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(stringResource(R.string.action_pick_logo)) },
                    modifier = Modifier.testTag("pick_custom_logo_button")
                )
                FilterChip(
                    selected = logoType == LogoType.BUILTIN_ASIAN_PRESS.name,
                    onClick = { logoType = LogoType.BUILTIN_ASIAN_PRESS.name },
                    label = { Text(stringResource(R.string.action_use_asian_press_logo)) }
                )
                FilterChip(
                    selected = logoType == LogoType.MONOGRAM.name,
                    onClick = { logoType = LogoType.MONOGRAM.name },
                    label = { Text(stringResource(R.string.action_use_monogram)) }
                )
            }

            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business / Studio Name *") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("brand_name_input")
            )
            OutlinedTextField(
                value = tagline,
                onValueChange = { tagline = it },
                label = { Text("Tagline / Show Subtitle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / WhatsApp") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = socialHandle,
                    onValueChange = { socialHandle = it },
                    label = { Text("Social @handle") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Brand Accent Palette", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colorSwatches.forEach { (prim, acc) ->
                    val selected = primaryColorHex == prim
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(prim))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable {
                                primaryColorHex = prim
                                accentColorHex = acc
                            }
                    )
                }
            }

            Text("Watermark Position", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                WatermarkPosition.entries.forEach { pos ->
                    FilterChip(
                        selected = wmPosition == pos.name,
                        onClick = { wmPosition = pos.name },
                        label = { Text(pos.label) }
                    )
                }
            }

            Text("Watermark Style", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                WatermarkStyle.entries.forEach { st ->
                    FilterChip(
                        selected = wmStyle == st.name,
                        onClick = { wmStyle = st.name },
                        label = { Text(st.label) }
                    )
                }
            }

            Column {
                Text(
                    text = "Opacity: ${(wmOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = wmOpacity,
                    onValueChange = { wmOpacity = it },
                    valueRange = 0.25f..1.0f
                )
            }

            Column {
                Text(
                    text = "Scale: ${(wmScale * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = wmScale,
                    onValueChange = { wmScale = it },
                    valueRange = 0.65f..1.4f
                )
            }

            Button(
                onClick = {
                    onSave(
                        draftBrand.copy(
                            businessName = businessName.trim().ifBlank { "Custom Studio" }
                        )
                    )
                },
                enabled = businessName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AsianPressCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_brand_button")
            ) {
                Text(stringResource(R.string.action_save_brand), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
