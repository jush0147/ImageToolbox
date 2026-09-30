/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.feature.draw.presentation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t8rin.dynamic.theme.LocalDynamicThemeState
import com.t8rin.imagetoolbox.core.domain.model.pt
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.ArrowBack
import com.t8rin.imagetoolbox.core.resources.icons.AutoFixHigh
import com.t8rin.imagetoolbox.core.resources.icons.CropSmall
import com.t8rin.imagetoolbox.core.resources.icons.ImagesMode
import com.t8rin.imagetoolbox.core.resources.icons.MoreVert
import com.t8rin.imagetoolbox.core.resources.icons.Redo
import com.t8rin.imagetoolbox.core.resources.icons.Save
import com.t8rin.imagetoolbox.core.resources.icons.Share
import com.t8rin.imagetoolbox.core.resources.icons.Undo
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalSettingsState
import com.t8rin.imagetoolbox.core.settings.presentation.provider.rememberAppColorTuple
import com.t8rin.imagetoolbox.core.ui.utils.content_pickers.ImagePickerMode
import com.t8rin.imagetoolbox.core.ui.utils.content_pickers.rememberImagePicker
import com.t8rin.imagetoolbox.core.ui.widget.dialogs.ExitWithoutSavingDialog
import com.t8rin.imagetoolbox.core.ui.widget.other.DrawLockScreenOrientation
import com.t8rin.imagetoolbox.core.ui.widget.utils.AutoContentBasedColors
import com.t8rin.imagetoolbox.feature.draw.domain.DrawBehavior
import com.t8rin.imagetoolbox.feature.draw.presentation.components.BitmapDrawer
import com.t8rin.imagetoolbox.feature.draw.presentation.components.QuickCropData
import com.t8rin.imagetoolbox.feature.draw.presentation.components.QuickCropOverlay
import com.t8rin.imagetoolbox.feature.draw.presentation.components.controls.MarkitEditorControls
import com.t8rin.imagetoolbox.feature.draw.presentation.components.controls.MarkitTool
import com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic.DrawComponent

@Composable
fun DrawContent(
    component: DrawComponent,
) {
    val settingsState = LocalSettingsState.current
    val themeState = LocalDynamicThemeState.current
    val appColorTuple = rememberAppColorTuple()
    val scope = rememberCoroutineScope()

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    val onBack = {
        when (component.drawBehavior) {
            !is DrawBehavior.None if component.haveChanges -> showExitDialog = true

            !is DrawBehavior.None -> {
                component.resetDrawBehavior()
                themeState.updateColorTuple(appColorTuple)
            }

            else -> component.onGoBack()
        }
    }

    AutoContentBasedColors(component.imageBitmap)

    val imagePicker = rememberImagePicker(
        mode = ImagePickerMode.PhotoPickerSingle,
        onSuccess = { uris ->
            uris.firstOrNull()?.let(component::setUri)
        }
    )
    val pickImage = imagePicker::pickImage

    var tool by rememberSaveable(component.drawBehavior) {
        mutableStateOf(MarkitTool.Arrow)
    }
    var textValue by rememberSaveable(component.drawBehavior) {
        mutableStateOf("文字")
    }
    var numberValue by rememberSaveable(component.drawBehavior) {
        mutableIntStateOf(1)
    }
    var drawColor by rememberSaveable(component.drawBehavior) {
        mutableStateOf(settingsState.defaultDrawColor)
    }
    var strokeWidth by rememberSaveable(component.drawBehavior) {
        mutableStateOf(settingsState.defaultDrawLineWidth.pt)
    }
    var alpha by rememberSaveable(component.drawBehavior) {
        mutableFloatStateOf(1f)
    }
    var selectedPathIndex by rememberSaveable(component.drawBehavior) {
        mutableStateOf<Int?>(null)
    }
    var quickCropData by remember(component.drawBehavior) {
        mutableStateOf<QuickCropData?>(null)
    }

    LaunchedEffect(component.paths.size) {
        if (selectedPathIndex?.let { it !in component.paths.indices } == true) {
            selectedPathIndex = null
        }
    }

    val imageBitmap = component.imageBitmap
    val hasImage = imageBitmap != null && component.drawBehavior !is DrawBehavior.None
    val busy = component.isSaving || component.isImageLoading || component.isSmartRedacting

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.quick_markup_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (hasImage) {
                        IconButton(
                            onClick = component::undo,
                            enabled = component.canUndo && !busy
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Undo,
                                contentDescription = stringResource(R.string.markit_undo)
                            )
                        }
                        IconButton(
                            onClick = component::redo,
                            enabled = component.canRedo && !busy
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Redo,
                                contentDescription = stringResource(R.string.markit_redo)
                            )
                        }
                        IconButton(
                            onClick = component::smartRedact,
                            enabled = !busy
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = stringResource(R.string.quick_markup_smart_redact)
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                enabled = !busy
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.markit_more)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.quick_markup_crop)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.CropSmall,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        component.prepareCrop { bitmap, uri, size ->
                                            quickCropData = QuickCropData(
                                                bitmap = bitmap,
                                                uri = uri,
                                                imageSize = size
                                            )
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.markit_replace_image)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.ImagesMode,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        pickImage()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (hasImage) {
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MarkitEditorControls(
                            component = component,
                            tool = tool,
                            onToolChange = { selectedTool ->
                                tool = selectedTool
                                selectedPathIndex = null
                            },
                            textValue = textValue,
                            onTextValueChange = { textValue = it },
                            numberValue = numberValue,
                            drawColor = drawColor,
                            strokeWidth = strokeWidth,
                            onDrawColorChange = { drawColor = it },
                            onStrokeWidthChange = { strokeWidth = it },
                            onAlphaChange = { alpha = it },
                            hasSelectedPath = selectedPathIndex != null,
                            onScaleSelectedDown = {
                                selectedPathIndex?.let { component.scalePathAt(it, 0.8f) }
                            },
                            onScaleSelectedUp = {
                                selectedPathIndex?.let { component.scalePathAt(it, 1.25f) }
                            },
                            onDeleteSelected = {
                                selectedPathIndex?.let(component::removePathAt)
                                selectedPathIndex = null
                            }
                        )

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { component.saveBitmap(null) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null
                                )
                                Text(stringResource(R.string.markit_save))
                            }
                            Button(
                                onClick = component::shareBitmap,
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null
                                )
                                Text(stringResource(R.string.markit_share))
                            }
                        }
                    }
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when {
                component.isImageLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                !hasImage || imageBitmap == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImagesMode,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.markit_empty_hint),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(onClick = pickImage) {
                            Text(stringResource(R.string.pick_image))
                        }
                    }
                }

                else -> {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageRatio = imageBitmap.width / imageBitmap.height.toFloat()
                        val availableRatio = maxWidth.value / maxHeight.value
                        val canvasModifier = if (imageRatio >= availableRatio) {
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(imageRatio)
                        } else {
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(imageRatio)
                        }

                        BitmapDrawer(
                            imageBitmap = imageBitmap,
                            renderCache = component.renderCache,
                            sourceKey = imageBitmap.asAndroidBitmap(),
                            paths = component.paths,
                            strokeWidth = strokeWidth,
                            brushSoftness = 0.pt,
                            drawColor = drawColor.copy(alpha = alpha),
                            gradientPalette = null,
                            gradientGeometry = null,
                            gradientLength = 1f,
                            isGradientMirrored = false,
                            onAddPath = { path ->
                                component.addPath(path)
                                if (tool == MarkitTool.Number) {
                                    numberValue++
                                }
                            },
                            isEraserOn = false,
                            drawMode = component.drawMode,
                            modifier = canvasModifier,
                            panEnabled = false,
                            onRequestFiltering = { bitmap, _ -> bitmap },
                            drawPathMode = component.drawPathMode,
                            backgroundColor = component.backgroundColor,
                            backgroundGradient = component.backgroundGradient,
                            drawLineStyle = component.drawLineStyle,
                            helperGridParams = component.helperGridParams,
                            showLineAngle = false,
                            onRemovePath = component::removePath,
                            pathEditEnabled = true,
                            directPathEditingEnabled = true,
                            selectedPathIndex = selectedPathIndex,
                            onSelectedPathIndexChange = {
                                selectedPathIndex = it
                            },
                            onPathTransformStart = component::beginPathTransform,
                            onPathTransformPreview = component::previewPathTransform,
                            onPathTransformFinish = component::finishPathTransform,
                            onPathTransformCancel = component::cancelPathTransform
                        )
                    }
                }
            }

            if (busy && hasImage) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }

    QuickCropOverlay(
        data = quickCropData,
        onDismiss = {
            quickCropData = null
        },
        onApplied = { uri: Uri ->
            quickCropData = null
            component.setUri(uri)
        }
    )

    ExitWithoutSavingDialog(
        onExit = {
            if (component.drawBehavior !is DrawBehavior.None) {
                component.resetDrawBehavior()
                themeState.updateColorTuple(appColorTuple)
            } else {
                component.onGoBack()
            }
        },
        onDismiss = { showExitDialog = false },
        visible = showExitDialog
    )

    DrawLockScreenOrientation()
}
