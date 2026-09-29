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
import com.t8rin.imagetoolbox.core.resources.icons.Counter
import com.t8rin.imagetoolbox.core.resources.icons.CropSmall
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
    Number,
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
    numberValue: Int,
    drawColor: Color,
    strokeWidth: Pt,
    onDrawColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Pt) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onCropClick: () -> Unit
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

            QuickMarkupTool.Number -> {
                component.updateDrawMode(DrawMode.Text(text = circledNumber(numberValue)))
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

    LaunchedEffect(numberValue, tool) {
        if (tool == QuickMarkupTool.Number) {
            component.updateDrawMode(DrawMode.Text(text = circledNumber(numberValue)))
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
                selected = false,
                label = stringResource(R.string.quick_markup_crop),
                icon = Icons.Rounded.CropSmall,
                onClick = onCropClick
            )
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
                selected = tool == QuickMarkupTool.Number,
                label = stringResource(R.string.quick_markup_number),
                icon = Icons.Outlined.Counter,
                onClick = { onToolChange(QuickMarkupTool.Number) }
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
                icon = Icons.Rounded.VisibilityOff,
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

        AnimatedVisibility(visible = tool != QuickMarkupTool.Redact) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorChip(
                        selected = drawColor == Color.Red,
                        label = stringResource(R.string.quick_markup_color_red),
                        onClick = { onDrawColorChange(Color.Red) }
                    )
                    ColorChip(
                        selected = drawColor == Color.Yellow,
                        label = stringResource(R.string.quick_markup_color_yellow),
                        onClick = { onDrawColorChange(Color.Yellow) }
                    )
                    ColorChip(
                        selected = drawColor == Color.Black,
                        label = stringResource(R.string.quick_markup_color_black),
                        onClick = { onDrawColorChange(Color.Black) }
                    )
                    ColorChip(
                        selected = drawColor == Color.White,
                        label = stringResource(R.string.quick_markup_color_white),
                        onClick = { onDrawColorChange(Color.White) }
                    )
                }

                val widths = widthOptions(tool)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WidthChip(
                        selected = strokeWidth == widths[0],
                        label = stringResource(R.string.quick_markup_width_thin),
                        onClick = { onStrokeWidthChange(widths[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    WidthChip(
                        selected = strokeWidth == widths[1],
                        label = stringResource(R.string.quick_markup_width_medium),
                        onClick = { onStrokeWidthChange(widths[1]) },
                        modifier = Modifier.weight(1f)
                    )
                    WidthChip(
                        selected = strokeWidth == widths[2],
                        label = stringResource(R.string.quick_markup_width_thick),
                        onClick = { onStrokeWidthChange(widths[2]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun widthOptions(tool: QuickMarkupTool): List<Pt> = when (tool) {
    QuickMarkupTool.Text,
    QuickMarkupTool.Number -> listOf(20.pt, 28.pt, 40.pt)
    QuickMarkupTool.Highlighter -> listOf(10.pt, 18.pt, 30.pt)
    QuickMarkupTool.Arrow,
    QuickMarkupTool.Box -> listOf(2.pt, 4.pt, 8.pt)

    QuickMarkupTool.Redact -> listOf(1.pt, 1.pt, 1.pt)
}

private fun circledNumber(value: Int): String = when (value) {
    in 1..20 -> CIRCLED_NUMBERS[value - 1].toString()
    else -> value.toString()
}

private const val CIRCLED_NUMBERS = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳"

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

@Composable
private fun ColorChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun WidthChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier
    )
}
