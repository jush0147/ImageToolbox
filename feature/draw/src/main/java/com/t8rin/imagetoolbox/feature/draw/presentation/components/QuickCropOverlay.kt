/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.t8rin.cropper.ImageCropper
import com.t8rin.cropper.model.OutlineType
import com.t8rin.cropper.model.RectCropShape
import com.t8rin.cropper.rememberImageCropperState
import com.t8rin.cropper.settings.CropDefaults
import com.t8rin.cropper.settings.CropOutlineProperty
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.Close
import com.t8rin.imagetoolbox.core.resources.icons.Done

internal data class QuickCropData(
    val bitmap: Bitmap,
    val uri: Uri,
    val imageSize: IntSize
)

@Composable
internal fun QuickCropOverlay(
    data: QuickCropData?,
    onDismiss: () -> Unit,
    onApplied: (Uri) -> Unit
) {
    if (data == null) return

    val cropperState = rememberImageCropperState()
    var cropRequested by remember(data.uri) { mutableStateOf(false) }
    var loading by remember(data.uri) { mutableStateOf(false) }

    val cropProperties = remember {
        CropDefaults.properties(
            cropOutlineProperty = CropOutlineProperty(
                outlineType = OutlineType.Rect,
                cropOutline = RectCropShape(
                    id = 0,
                    title = OutlineType.Rect.name
                )
            ),
            fixedAspectRatio = false,
            rotatable = false,
            maxZoom = 12f
        )
    }

    Dialog(
        onDismissRequest = {
            if (!loading) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !loading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }

                    Text(
                        text = stringResource(R.string.quick_markup_crop),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            if (!loading) cropRequested = true
                        },
                        enabled = !loading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Done,
                            contentDescription = stringResource(R.string.quick_markup_apply_crop)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val imageBitmap = remember(data.bitmap) {
                        data.bitmap.asImageBitmap()
                    }

                    ImageCropper(
                        modifier = Modifier.fillMaxSize(),
                        imageBitmap = imageBitmap,
                        sourceImageUri = data.uri,
                        sourceImageSize = data.imageSize,
                        contentDescription = stringResource(R.string.quick_markup_crop),
                        cropProperties = cropProperties,
                        state = cropperState,
                        crop = cropRequested,
                        enableOneFingerZoom = true,
                        isOverlayDraggable = true,
                        onCropStart = {
                            loading = true
                        },
                        onZoomChange = {},
                        onCropSuccess = { uri ->
                            cropRequested = false
                            loading = false
                            uri?.let(onApplied)
                        }
                    )

                    if (loading) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
