/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.core.ui.widget.other

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.t8rin.imagetoolbox.core.resources.shapes.CloverShape
import com.t8rin.imagetoolbox.core.ui.widget.modifier.shimmer

@Composable
fun EmojiItem(
    emoji: String?,
    animatedEmoji: String? = null,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    filterQuality: FilterQuality = FilterQuality.High,
    animateEmojiChange: Boolean = true,
    containerColor: Color? = null,
    shape: Shape? = null,
    contentPadding: Dp? = null,
    onNoEmoji: (@Composable () -> Unit)? = null
) {
    // Markit keeps the lightweight static emoji path and does not ship DotLottie.
    key(emoji, animatedEmoji) {
        val content: @Composable (emoji: String?) -> Unit = { currentEmoji ->
            currentEmoji?.let {
                var shimmering by remember(currentEmoji) { mutableStateOf(true) }

                val emojiModifier = remember(shimmering) {
                    Modifier
                        .then(
                            if (fontSize > 0.sp) {
                                Modifier.layout { measurable, constraints ->
                                    val size = fontSize.roundToPx() + 4.dp.roundToPx()
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = size,
                                            minHeight = size,
                                            maxWidth = size,
                                            maxHeight = size
                                        )
                                    )
                                    layout(size, size) { placeable.place(0, 0) }
                                }
                            } else {
                                Modifier.fillMaxSize()
                            }
                        )
                        .clip(shape ?: CloverShape)
                        .then(
                            if (containerColor != null) Modifier.background(containerColor)
                            else Modifier
                        )
                        .shimmer(shimmering)
                        .padding(2.dp + (contentPadding ?: 0.dp))
                }

                AsyncImage(
                    model = currentEmoji,
                    onLoading = { shimmering = true },
                    onSuccess = { shimmering = false },
                    filterQuality = filterQuality,
                    contentDescription = null,
                    modifier = emojiModifier
                )
            } ?: onNoEmoji?.invoke()
        }

        if (animateEmojiChange) {
            AnimatedContent(
                targetState = emoji,
                modifier = modifier,
                transitionSpec = {
                    fadeIn() + scaleIn(initialScale = 0.85f) togetherWith fadeOut() + scaleOut(
                        targetScale = 0.85f
                    )
                }
            ) { currentEmoji -> content(currentEmoji) }
        } else {
            Box(modifier) { content(emoji) }
        }
    }
}
