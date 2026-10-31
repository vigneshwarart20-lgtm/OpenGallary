package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AspectRatioOption
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem
import com.example.ui.theme.iOSBlue
import com.example.ui.theme.iOSYellow
import com.example.viewmodel.EditorViewModel

enum class AppleEditorTab(val label: String, val icon: ImageVector) {
    ADJUST("Adjust", Icons.Default.Tune),
    FILTERS("Filters", Icons.Default.Filter),
    CROP("Crop", Icons.Default.Crop)
}

enum class AppleAdjustTool(val displayName: String) {
    EXPOSURE("Exposure"),
    BRILLIANCE("Brilliance"),
    HIGHLIGHTS("Highlights"),
    SHADOWS("Shadows"),
    CONTRAST("Contrast"),
    BRIGHTNESS("Brightness"),
    SATURATION("Saturation"),
    VIBRANCE("Vibrance"),
    WARMTH("Warmth"),
    TINT("Tint"),
    SHARPNESS("Sharpness"),
    VIGNETTE("Vignette")
}

@Composable
fun PhotoEditorScreen(
    mediaItem: MediaItem,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    editorViewModel: EditorViewModel = viewModel()
) {
    BackHandler(onBack = onCancel)

    LaunchedEffect(mediaItem) {
        editorViewModel.initializeWithMedia(mediaItem)
    }

    val params by editorViewModel.currentParams.collectAsState()
    val canUndo by editorViewModel.canUndo.collectAsState()
    val canRedo by editorViewModel.canRedo.collectAsState()
    val previewBitmap by editorViewModel.previewBitmap.collectAsState()
    val isSaving by editorViewModel.isSaving.collectAsState()

    var activeTab by remember { mutableStateOf(AppleEditorTab.ADJUST) }
    var activeAdjustTool by remember { mutableStateOf(AppleAdjustTool.EXPOSURE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("photo_editor_screen")
    ) {
        // Apple Photos Top Bar: Cancel (left), Undo/Redo/Revert (center), Done (right in yellow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("editor_cancel_button")
            ) {
                Text(
                    text = "Cancel",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { editorViewModel.undo() },
                enabled = canUndo,
                modifier = Modifier.testTag("editor_undo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.25f)
                )
            }

            IconButton(
                onClick = { editorViewModel.redo() },
                enabled = canRedo,
                modifier = Modifier.testTag("editor_redo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.25f)
                )
            }

            IconButton(
                onClick = { editorViewModel.resetAll() },
                modifier = Modifier.testTag("editor_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Revert",
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Done button in signature iOS Yellow
            TextButton(
                onClick = {
                    editorViewModel.saveImage {
                        onSaved()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.testTag("editor_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = iOSYellow,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Done",
                        color = iOSYellow,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Large Preview Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "Edited Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(color = iOSYellow)
            }

            if (isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = iOSYellow)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Saving...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Apple Photos Editing Controls Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFF141416))
                .padding(vertical = 8.dp)
        ) {
            when (activeTab) {
                AppleEditorTab.ADJUST -> {
                    AppleAdjustPanel(
                        activeTool = activeAdjustTool,
                        onSelectTool = { activeAdjustTool = it },
                        params = params,
                        viewModel = editorViewModel
                    )
                }
                AppleEditorTab.FILTERS -> {
                    AppleFiltersPanel(
                        params = params,
                        viewModel = editorViewModel
                    )
                }
                AppleEditorTab.CROP -> {
                    AppleCropPanel(
                        params = params,
                        viewModel = editorViewModel
                    )
                }
            }
        }

        // Apple Photos Bottom Tab Bar: Adjust, Filters, Crop
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF000000)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppleEditorTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { activeTab = tab }
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .testTag("editor_tab_${tab.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) iOSYellow else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) iOSYellow else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleAdjustPanel(
    activeTool: AppleAdjustTool,
    onSelectTool: (AppleAdjustTool) -> Unit,
    params: com.example.data.model.EditParameters,
    viewModel: EditorViewModel
) {
    val currentValue = when (activeTool) {
        AppleAdjustTool.EXPOSURE -> params.exposure
        AppleAdjustTool.BRILLIANCE -> params.highlights
        AppleAdjustTool.HIGHLIGHTS -> params.highlights
        AppleAdjustTool.SHADOWS -> params.shadows
        AppleAdjustTool.CONTRAST -> params.contrast
        AppleAdjustTool.BRIGHTNESS -> params.brightness
        AppleAdjustTool.SATURATION -> params.saturation
        AppleAdjustTool.VIBRANCE -> params.vibrance
        AppleAdjustTool.WARMTH -> params.temperature
        AppleAdjustTool.TINT -> params.tint
        AppleAdjustTool.SHARPNESS -> params.sharpen
        AppleAdjustTool.VIGNETTE -> params.vignette
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Horizontal Carousel of Tool Dials
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(AppleAdjustTool.values()) { tool ->
                val isSelected = tool == activeTool
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectTool(tool) }
                        .testTag("adjust_tool_${tool.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (isSelected) Color(0xFF28282A) else Color(0xFF1E1E20),
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) iOSYellow else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tool.displayName.take(3),
                            color = if (isSelected) iOSYellow else Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tool.displayName,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Active Tool Value & Dial Slider
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "${currentValue.toInt()}",
                color = if (currentValue.toInt() != 0) iOSYellow else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Slider(
                value = currentValue,
                onValueChange = { newVal ->
                    when (activeTool) {
                        AppleAdjustTool.EXPOSURE -> viewModel.setExposure(newVal)
                        AppleAdjustTool.BRILLIANCE -> viewModel.setHighlights(newVal)
                        AppleAdjustTool.HIGHLIGHTS -> viewModel.setHighlights(newVal)
                        AppleAdjustTool.SHADOWS -> viewModel.setShadows(newVal)
                        AppleAdjustTool.CONTRAST -> viewModel.setContrast(newVal)
                        AppleAdjustTool.BRIGHTNESS -> viewModel.setBrightness(newVal)
                        AppleAdjustTool.SATURATION -> viewModel.setSaturation(newVal)
                        AppleAdjustTool.VIBRANCE -> viewModel.setVibrance(newVal)
                        AppleAdjustTool.WARMTH -> viewModel.setTemperature(newVal)
                        AppleAdjustTool.TINT -> viewModel.setTint(newVal)
                        AppleAdjustTool.SHARPNESS -> viewModel.setSharpen(newVal)
                        AppleAdjustTool.VIGNETTE -> viewModel.setVignette(newVal)
                    }
                },
                valueRange = if (activeTool == AppleAdjustTool.SHARPNESS || activeTool == AppleAdjustTool.VIGNETTE) 0f..100f else -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = iOSYellow,
                    activeTrackColor = iOSYellow,
                    inactiveTrackColor = Color(0xFF2C2C2E)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adjust_slider")
            )
        }
    }
}

@Composable
private fun AppleFiltersPanel(
    params: com.example.data.model.EditParameters,
    viewModel: EditorViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(FilterPreset.values()) { preset ->
                val isSelected = params.filter == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { viewModel.setFilter(preset) }
                        .testTag("filter_preset_${preset.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) iOSYellow else Color(0xFF262628))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) iOSYellow else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.displayName.take(4),
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.displayName,
                        fontSize = 11.sp,
                        color = if (isSelected) iOSYellow else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (params.filter != FilterPreset.ORIGINAL) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "${(params.filterIntensity * 100).toInt()}%",
                    color = iOSYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(50.dp)
                )
                Slider(
                    value = params.filterIntensity,
                    onValueChange = { viewModel.setFilterIntensity(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = iOSYellow,
                        activeTrackColor = iOSYellow,
                        inactiveTrackColor = Color(0xFF2C2C2E)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slider_filter_intensity")
                )
            }
        }
    }
}

@Composable
private fun AppleCropPanel(
    params: com.example.data.model.EditParameters,
    viewModel: EditorViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Transform buttons: 90 Rotate, Flip H, Flip V
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = { viewModel.rotate90() },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("crop_rotate_90")
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("90°", color = Color.White)
            }

            OutlinedButton(
                onClick = { viewModel.toggleFlipHorizontal() },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("crop_flip_h")
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Flip", color = Color.White)
            }
        }

        // Straighten Dial
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${params.fineRotationAngle.toInt()}°",
                color = if (params.fineRotationAngle.toInt() != 0) iOSYellow else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            Slider(
                value = params.fineRotationAngle,
                onValueChange = { viewModel.setFineRotation(it) },
                valueRange = -45f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = iOSYellow,
                    activeTrackColor = iOSYellow,
                    inactiveTrackColor = Color(0xFF2C2C2E)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("slider_fine_rotation")
            )
        }

        // Aspect Ratio chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(AspectRatioOption.values()) { ratio ->
                val isSelected = params.aspectRatio == ratio
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) iOSYellow else Color(0xFF242426))
                        .clickable { viewModel.setAspectRatio(ratio) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = ratio.label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
