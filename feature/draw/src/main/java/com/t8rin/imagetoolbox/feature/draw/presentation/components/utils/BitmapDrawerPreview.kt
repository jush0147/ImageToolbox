/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */

package com.t8rin.imagetoolbox.feature.draw.presentation.components.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.t8rin.imagetoolbox.core.ui.theme.outlineVariant
import com.t8rin.imagetoolbox.core.ui.widget.image.Picture
import com.t8rin.imagetoolbox.core.ui.widget.modifier.HelperGridParams
import com.t8rin.imagetoolbox.core.ui.widget.modifier.ShapeDefaults
import com.t8rin.imagetoolbox.core.ui.widget.modifier.drawHelperGrid
import com.t8rin.imagetoolbox.core.ui.widget.modifier.transparencyChecker

@Composable
fun BoxScope.BitmapDrawerPreview(
    preview: ImageBitmap,
    globalTouchPointersCount: MutableIntState,
    onReceiveMotionEvent: (MotionEvent) -> Unit,
    onInvalidate: () -> Unit,
    onUpdateCurrentDrawPosition: (Offset) -> Unit,
    onUpdateDrawDownPosition: (Offset) -> Unit,
    drawEnabled: Boolean,
    pathEditEnabled: Boolean = false,
    directPathEditEnabled: Boolean = false,
    shouldPathEditAt: (Offset) -> Boolean = { false },
    onPathEditDown: (Offset) -> Unit = {},
    onPathEditMove: (Offset) -> Unit = {},
    onPathEditUp: (Offset) -> Unit = {},
    onPathEditCancel: () -> Unit = {},
    onPathEditMiss: () -> Unit = {},
    helperGridParams: HelperGridParams,
    drawBitmapBorder: Boolean,
    beforeHelperGridModifier: Modifier = Modifier,
) {
    Picture(
        model = preview,
        modifier = Modifier
            .matchParentSize()
            .then(
                if (directPathEditEnabled) {
                    Modifier.pointerDrawOrPathEditHandler(
                        globalTouchPointersCount = globalTouchPointersCount,
                        onReceiveMotionEvent = onReceiveMotionEvent,
                        onInvalidate = onInvalidate,
                        onUpdateCurrentDrawPosition = onUpdateCurrentDrawPosition,
                        onUpdateDrawDownPosition = onUpdateDrawDownPosition,
                        drawEnabled = drawEnabled,
                        pathEditEnabled = pathEditEnabled,
                        shouldPathEditAt = shouldPathEditAt,
                        onPathEditDown = onPathEditDown,
                        onPathEditMove = onPathEditMove,
                        onPathEditUp = onPathEditUp,
                        onPathEditCancel = onPathEditCancel,
                        onPathEditMiss = onPathEditMiss
                    )
                } else {
                    Modifier
                        .pointerDrawHandler(
                            globalTouchPointersCount = globalTouchPointersCount,
                            onReceiveMotionEvent = onReceiveMotionEvent,
                            onInvalidate = onInvalidate,
                            onUpdateCurrentDrawPosition = onUpdateCurrentDrawPosition,
                            onUpdateDrawDownPosition = onUpdateDrawDownPosition,
                            enabled = drawEnabled
                        )
                        .pointerPathEditHandler(
                            globalTouchPointersCount = globalTouchPointersCount,
                            enabled = pathEditEnabled,
                            onDown = onPathEditDown,
                            onMove = onPathEditMove,
                            onUp = onPathEditUp,
                            onCancel = onPathEditCancel
                        )
                }
            )
            .clip(ShapeDefaults.extremeSmall)
            .transparencyChecker()
            .then(beforeHelperGridModifier)
            .drawHelperGrid(helperGridParams)
            .then(
                if (drawBitmapBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant(),
                        shape = ShapeDefaults.extremeSmall
                    )
                } else Modifier
            ),
        contentDescription = null,
        contentScale = ContentScale.FillBounds
    )
}