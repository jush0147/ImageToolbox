/*
 * Markit editor controls
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0.
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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
import com.t8rin.imagetoolbox.core.resources.icons.Delete
import com.t8rin.imagetoolbox.core.resources.icons.Highlighter
import com.t8rin.imagetoolbox.core.resources.icons.LineArrow
import com.t8rin.imagetoolbox.core.resources.icons.PhotoSizeSelectLarge
import com.t8rin.imagetoolbox.core.resources.icons.PhotoSizeSelectSmall
import com.t8rin.imagetoolbox.core.resources.icons.Rectangle
import com.t8rin.imagetoolbox.core.resources.icons.TextFormat
import com.t8rin.imagetoolbox.core.resources.icons.VisibilityOff
import com.t8rin.imagetoolbox.feature.draw.domain.DrawLineStyle
import com.t8rin.imagetoolbox.feature.draw.domain.DrawMode
import com.t8rin.imagetoolbox.feature.draw.domain.DrawPathMode
import com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic.DrawComponent

internal enum class MarkitTool {
    Arrow,
    Box,
    Text,
    Highlighter,
    Redact,
    Number
}

@Composable
internal fun MarkitEditorControls(
    component: DrawComponent,
    tool: MarkitTool,
    onToolChange: (MarkitTool) -> Unit,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    numberValue: Int,
    drawColor: Color,
    strokeWidth: Pt,
    onDrawColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Pt) -> Unit,
    onAlphaChange: (Float) -> Unit,
    hasSelectedPath: Boolean,
    onScaleSelectedDown: () -> Unit,
    onScaleSelectedUp: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    fun applyTool(selected: MarkitTool) {
        component.updateDrawLineStyle(DrawLineStyle.None)

        when (selected) {
            MarkitTool.Arrow -> {
                component.updateDrawMode(DrawMode.Pen)
                component.updateDrawPathMode(DrawPathMode.LinePointingArrow())
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(4.pt)
                onAlphaChange(1f)
            }

            MarkitTool.Box -> {
                component.updateDrawMode(DrawMode.Pen)
                component.updateDrawPathMode(DrawPathMode.OutlinedRect())
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(4.pt)
                onAlphaChange(1f)
            }

            MarkitTool.Text -> {
                component.updateDrawMode(DrawMode.Text(text = textValue))
                component.updateDrawPathMode(DrawPathMode.Line)
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(28.pt)
                onAlphaChange(1f)
            }

            MarkitTool.Highlighter -> {
                component.updateDrawMode(DrawMode.Highlighter)
                component.updateDrawPathMode(DrawPathMode.Free)
                onDrawColorChange(Color.Yellow)
                onStrokeWidthChange(18.pt)
                onAlphaChange(0.4f)
            }

            MarkitTool.Redact -> {
                component.updateDrawMode(DrawMode.Pen)
                component.updateDrawPathMode(DrawPathMode.Rect())
                onDrawColorChange(Color.Black)
                onStrokeWidthChange(1.pt)
                onAlphaChange(1f)
            }

            MarkitTool.Number -> {
                component.updateDrawMode(DrawMode.Text(text = circledNumber(numberValue)))
                component.updateDrawPathMode(DrawPathMode.Line)
                onDrawColorChange(Color.Red)
                onStrokeWidthChange(28.pt)
                onAlphaChange(1f)
            }
        }
    }

    LaunchedEffect(tool) {
        applyTool(tool)
    }

    LaunchedEffect(textValue, tool) {
        if (tool == MarkitTool.Text) {
            component.updateDrawMode(DrawMode.Text(text = textValue))
        }
    }

    LaunchedEffect(numberValue, tool) {
        if (tool == MarkitTool.Number) {
            component.updateDrawMode(DrawMode.Text(text = circledNumber(numberValue)))
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hasSelectedPath) {
            SelectedPathControls(
                onScaleDown = onScaleSelectedDown,
                onScaleUp = onScaleSelectedUp,
                onDelete = onDeleteSelected
            )
        } else {
            ToolContextControls(
                tool = tool,
                textValue = textValue,
                onTextValueChange = onTextValueChange,
                drawColor = drawColor,
                strokeWidth = strokeWidth,
                onDrawColorChange = onDrawColorChange,
                onStrokeWidthChange = onStrokeWidthChange
            )
        }

        ToolDock(
            selected = tool,
            onToolChange = onToolChange
        )
    }
}

@Composable
private fun ToolDock(
    selected: MarkitTool,
    onToolChange: (MarkitTool) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToolButton(
            selected = selected == MarkitTool.Arrow,
            label = stringResource(R.string.quick_markup_arrow),
            icon = Icons.Rounded.LineArrow,
            onClick = { onToolChange(MarkitTool.Arrow) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            selected = selected == MarkitTool.Box,
            label = stringResource(R.string.quick_markup_box),
            icon = Icons.Outlined.Rectangle,
            onClick = { onToolChange(MarkitTool.Box) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            selected = selected == MarkitTool.Text,
            label = stringResource(R.string.quick_markup_text),
            icon = Icons.Rounded.TextFormat,
            onClick = { onToolChange(MarkitTool.Text) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            selected = selected == MarkitTool.Highlighter,
            label = stringResource(R.string.quick_markup_highlighter),
            icon = Icons.Outlined.Highlighter,
            onClick = { onToolChange(MarkitTool.Highlighter) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            selected = selected == MarkitTool.Redact,
            label = stringResource(R.string.quick_markup_redact),
            icon = Icons.Rounded.VisibilityOff,
            onClick = { onToolChange(MarkitTool.Redact) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            selected = selected == MarkitTool.Number,
            label = stringResource(R.string.quick_markup_number),
            icon = Icons.Outlined.Counter,
            onClick = { onToolChange(MarkitTool.Number) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToolButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToolContextControls(
    tool: MarkitTool,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    drawColor: Color,
    strokeWidth: Pt,
    onDrawColorChange: (Color) -> Unit,
    onStrokeWidthChange: (Pt) -> Unit
) {
    if (tool == MarkitTool.Redact) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (tool == MarkitTool.Text) {
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(stringResource(R.string.quick_markup_text_content))
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ColorButton(
                color = Color.Red,
                selected = drawColor == Color.Red,
                label = stringResource(R.string.quick_markup_color_red),
                onClick = { onDrawColorChange(Color.Red) }
            )
            ColorButton(
                color = Color.Yellow,
                selected = drawColor == Color.Yellow,
                label = stringResource(R.string.quick_markup_color_yellow),
                onClick = { onDrawColorChange(Color.Yellow) }
            )
            ColorButton(
                color = Color.Black,
                selected = drawColor == Color.Black,
                label = stringResource(R.string.quick_markup_color_black),
                onClick = { onDrawColorChange(Color.Black) }
            )
            ColorButton(
                color = Color.White,
                selected = drawColor == Color.White,
                label = stringResource(R.string.quick_markup_color_white),
                onClick = { onDrawColorChange(Color.White) }
            )

            val widths = widthOptions(tool)
            WidthChip(
                selected = strokeWidth == widths[0],
                label = stringResource(R.string.quick_markup_width_thin),
                onClick = { onStrokeWidthChange(widths[0]) }
            )
            WidthChip(
                selected = strokeWidth == widths[1],
                label = stringResource(R.string.quick_markup_width_medium),
                onClick = { onStrokeWidthChange(widths[1]) }
            )
            WidthChip(
                selected = strokeWidth == widths[2],
                label = stringResource(R.string.quick_markup_width_thick),
                onClick = { onStrokeWidthChange(widths[2]) }
            )
        }
    }
}

@Composable
private fun ColorButton(
    color: Color,
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(color, CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun WidthChip(
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
private fun SelectedPathControls(
    onScaleDown: () -> Unit,
    onScaleUp: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onScaleDown,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoSizeSelectSmall,
                contentDescription = null
            )
            Text(stringResource(R.string.quick_markup_smaller))
        }
        FilledTonalButton(
            onClick = onScaleUp,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoSizeSelectLarge,
                contentDescription = null
            )
            Text(stringResource(R.string.quick_markup_larger))
        }
        FilledTonalButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = null
            )
            Text(stringResource(R.string.delete))
        }
    }
}

private fun widthOptions(tool: MarkitTool): List<Pt> = when (tool) {
    MarkitTool.Text,
    MarkitTool.Number -> listOf(20.pt, 28.pt, 40.pt)

    MarkitTool.Highlighter -> listOf(10.pt, 18.pt, 30.pt)

    MarkitTool.Arrow,
    MarkitTool.Box -> listOf(2.pt, 4.pt, 8.pt)

    MarkitTool.Redact -> listOf(1.pt, 1.pt, 1.pt)
}

private fun circledNumber(value: Int): String = when (value) {
    in 1..20 -> CIRCLED_NUMBERS[value - 1].toString()
    else -> value.toString()
}

private const val CIRCLED_NUMBERS = "①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳"
