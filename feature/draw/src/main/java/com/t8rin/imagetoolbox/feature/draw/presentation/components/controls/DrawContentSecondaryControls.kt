/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.controls

import androidx.compose.runtime.Composable
import com.t8rin.imagetoolbox.core.ui.widget.controls.UndoRedoButtons
import com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic.DrawComponent

@Composable
internal fun DrawContentSecondaryControls(
    component: DrawComponent
) {
    UndoRedoButtons(
        canUndo = component.canUndo,
        canRedo = component.canRedo,
        onUndo = component::undo,
        onRedo = component::redo
    )
}
