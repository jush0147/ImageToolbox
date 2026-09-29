/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.domain.model.Pt
import com.t8rin.imagetoolbox.core.domain.model.pt
import com.t8rin.imagetoolbox.core.resources.Icons
import com.t8rin.imagetoolbox.core.resources.R
import com.t8rin.imagetoolbox.core.resources.icons.Highlighter
import com.t8rin.imagetoolbox.core.resources.icons.LineArrow
import com.t8rin.imagetoolbox.core.resources.icons.Rectangle
import com.t8rin.imagetoolbox.core.resources.icons.TextFormat
import com.t8rin.imagetoolbox.core.resources.icons.VisibilityOff
import com.t8rin.imagetoolbox.feature.draw.domain.DrawLineStyle
import com.t8rin.imagetoolbox.feature.draw.domain.DrawMode
import com.t8rin.imagetoolbox.feature.draw.domain.DrawPathMode
import com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic.DrawComponent

internal enum class QuickMarkupTool {
    Arrow,
    Box,
    Text,
    Highlighter,
    Redact
}

@Composable
internal fun QuickMarkupControls(
    component: DrawComponent,
    tool: QuickMarkupTool,
    onToolChange: (QuickMarkupTool) -> Unit,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    onDrawColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Pt) -> Unit,
    onAlphaChange: (Float) -> Unit
) {
    fun applyTool(selected: QuickMarkupTool) {
        component.updateDrawLineStyle(DrawLineStyle.None)

        when (selected) {
            QuickMarkupTool.Arrow -> {
                component.updateDrawMode(DrawMode.Pen)
                component.updateDrawPathMode(DrawPathMode.LinePointingArrow())
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(4.pt)
                onAlphaChange(1f)
            }

            QuickMarkupTool.Box -> {
                component.updateDrawMode(DrawMode.Pen)
                component.updateDrawPathMode(DrawPathMode.OutlinedRect())
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(4.pt)
                onAlphaChange(1f)
            }

            QuickMarkupTool.Text -> {
                component.updateDrawMode(DrawMode.Text(text = textValue))
                component.updateDrawPathMode(DrawPathMode.Line)
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(28.pt)
                onAlphaChange(1f)
            }

            QuickMarkupTool.Highlighter -> {
                component.updateDrawMode(DrawMode.Highlighter)
                component.updateDrawPathMode(DrawPathMode.Free)
                onDrawColorChange(Color.Yellow)
                onStrokeWidthChange(18.pt)
                onAlphaChange(0.4f)
            }

            QuickMarkupTool.Redact -> {
                component.updateDrawMode(DrawMode.Pen)
                component.updateDrawPathMode(DrawPathMode.Rect())
                onDrawColorChange(Color.Black)
                onStrokeWidthChange(1.pt)
                onAlphaChange(1f)
            }
        }
    }

    LaunchedEffect(tool) {
        applyTool(tool)
    }

    LaunchedEffect(textValue, tool) {
        if (tool == QuickMarkupTool.Text) {
            component.updateDrawMode(DrawMode.Text(text = textValue))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickToolChip(
                selected = tool == QuickMarkupTool.Arrow,
                label = stringResource(R.string.quick_markup_arrow),
                icon = Icons.Rounded.LineArrow,
                onClick = { onToolChange(QuickMarkupTool.Arrow) }
            )
            QuickToolChip(
                selected = tool == QuickMarkupTool.Box,
                label = stringResource(R.string.quick_markup_box),
                icon = Icons.Outlined.Rectangle,
                onClick = { onToolChange(QuickMarkupTool.Box) }
            )
            QuickToolChip(
                selected = tool == QuickMarkupTool.Text,
                label = stringResource(R.string.quick_markup_text),
                icon = Icons.Rounded.TextFormat,
                onClick = { onToolChange(QuickMarkupTool.Text) }
            )
            QuickToolChip(
                selected = tool == QuickMarkupTool.Highlighter,
                label = stringResource(R.string.quick_markup_highlighter),
                icon = Icons.Outlined.Highlighter,
                onClick = { onToolChange(QuickMarkupTool.Highlighter) }
            )
            QuickToolChip(
                selected = tool == QuickMarkupTool.Redact,
                label = stringResource(R.string.quick_markup_redact),
                icon = Icons.Outlined.VisibilityOff,
                onClick = { onToolChange(QuickMarkupTool.Redact) }
            )
        }

        AnimatedVisibility(visible = tool == QuickMarkupTool.Text) {
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                label = {
                    Text(stringResource(R.string.quick_markup_text_content))
                }
            )
        }
    }
}

@Composable
private fun QuickToolChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    )
}
