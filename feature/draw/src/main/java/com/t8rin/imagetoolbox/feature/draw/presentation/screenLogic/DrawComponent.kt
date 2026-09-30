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

package com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import com.arkivanov.decompose.ComponentContext
import com.t8rin.imagetoolbox.core.domain.coroutines.DispatchersHolder
import com.t8rin.imagetoolbox.core.domain.image.ImageCompressor
import com.t8rin.imagetoolbox.core.domain.image.ImageGetter
import com.t8rin.imagetoolbox.core.domain.image.ImageScaler
import com.t8rin.imagetoolbox.core.domain.image.ImageShareProvider
import com.t8rin.imagetoolbox.core.domain.image.model.ImageFormat
import com.t8rin.imagetoolbox.core.domain.image.model.ImageInfo
import com.t8rin.imagetoolbox.core.domain.model.GradientFill
import com.t8rin.imagetoolbox.core.domain.model.IntegerSize
import com.t8rin.imagetoolbox.core.domain.model.pt
import com.t8rin.imagetoolbox.core.domain.saving.FileController
import com.t8rin.imagetoolbox.core.domain.saving.model.ImageSaveTarget
import com.t8rin.imagetoolbox.core.domain.utils.smartJob
import com.t8rin.imagetoolbox.core.domain.utils.update
import com.t8rin.imagetoolbox.core.settings.domain.SettingsProvider
import com.t8rin.imagetoolbox.core.ui.utils.BaseComponent
import com.t8rin.imagetoolbox.core.ui.utils.helper.AppToastHost
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.t8rin.imagetoolbox.core.ui.utils.state.savable
import com.t8rin.imagetoolbox.core.ui.utils.state.update
import com.t8rin.imagetoolbox.core.ui.widget.modifier.HelperGridParams
import com.t8rin.imagetoolbox.feature.draw.domain.DrawBehavior
import com.t8rin.imagetoolbox.feature.draw.domain.DrawLineStyle
import com.t8rin.imagetoolbox.feature.draw.domain.DrawMode
import com.t8rin.imagetoolbox.feature.draw.domain.DrawOnBackgroundParams
import com.t8rin.imagetoolbox.feature.draw.domain.DrawPathMode
import com.t8rin.imagetoolbox.feature.draw.domain.ImageDrawApplier
import com.t8rin.imagetoolbox.feature.draw.data.SmartRedactionEngine
import com.t8rin.imagetoolbox.feature.draw.presentation.components.UiPathPaint
import com.t8rin.imagetoolbox.feature.draw.presentation.components.utils.DrawRenderCache
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

class DrawComponent @AssistedInject internal constructor(
    @Assisted componentContext: ComponentContext,
    @Assisted val initialUri: Uri?,
    @Assisted val onGoBack: () -> Unit,
    @Assisted val onNavigate: (Screen) -> Unit,
    private val fileController: FileController,
    private val imageCompressor: ImageCompressor<Bitmap>,
    private val imageDrawApplier: ImageDrawApplier<Bitmap, Path, Color>,
    private val imageGetter: ImageGetter<Bitmap>,
    private val imageScaler: ImageScaler<Bitmap>,
    private val shareProvider: ImageShareProvider<Bitmap>,
    private val settingsProvider: SettingsProvider,
    private val smartRedactionEngine: SmartRedactionEngine,
    dispatchersHolder: DispatchersHolder,
) : BaseComponent(dispatchersHolder, componentContext) {

    init {
        debounce {
            initialUri?.let(::setUri)
        }
    }

    private val _drawOnBackgroundParams = fileController.savable(
        scope = componentScope,
        initial = DrawOnBackgroundParams.Default
    )
    val drawOnBackgroundParams: DrawOnBackgroundParams by _drawOnBackgroundParams

    private val _imageBitmap: MutableState<ImageBitmap?> = mutableStateOf(null)
    val imageBitmap: ImageBitmap? by _imageBitmap

    private val _backgroundColor: MutableState<Color> = mutableStateOf(Color.Transparent)
    val backgroundColor by _backgroundColor

    private val _backgroundGradient: MutableState<GradientFill?> = mutableStateOf(null)
    val backgroundGradient by _backgroundGradient

    private val _colorPickerBitmap: MutableState<Bitmap?> = mutableStateOf(null)
    val colorPickerBitmap by _colorPickerBitmap

    private val _drawBehavior: MutableState<DrawBehavior> = mutableStateOf(DrawBehavior.None)
    val drawBehavior: DrawBehavior by _drawBehavior

    private val _drawMode: MutableState<DrawMode> = mutableStateOf(DrawMode.Pen)
    val drawMode: DrawMode by _drawMode

    private val _drawPathMode: MutableState<DrawPathMode> = mutableStateOf(DrawPathMode.Free)
    val drawPathMode: DrawPathMode by _drawPathMode

    private val _drawLineStyle: MutableState<DrawLineStyle> = mutableStateOf(DrawLineStyle.None)
    val drawLineStyle: DrawLineStyle by _drawLineStyle

    private val _uri = mutableStateOf(Uri.EMPTY)
    val uri: Uri by _uri

    private val _paths = mutableStateOf(listOf<UiPathPaint>())
    val paths: List<UiPathPaint> by _paths

    private val _undoHistory = mutableStateOf<List<List<UiPathPaint>>>(emptyList())
    private val _redoHistory = mutableStateOf<List<List<UiPathPaint>>>(emptyList())

    val lastPaths: List<UiPathPaint>
        get() = _undoHistory.value.lastOrNull().orEmpty()

    val undonePaths: List<UiPathPaint>
        get() = _redoHistory.value.lastOrNull().orEmpty()

    val canUndo: Boolean
        get() = _undoHistory.value.isNotEmpty()

    val canRedo: Boolean
        get() = _redoHistory.value.isNotEmpty()

    private var pathTransformStart: List<UiPathPaint>? = null

    val renderCache = DrawRenderCache()

    val havePaths: Boolean
        get() = paths.isNotEmpty()

    private fun pushUndoSnapshot(snapshot: List<UiPathPaint>) {
        _undoHistory.value = (_undoHistory.value + listOf(snapshot)).takeLast(MaxPathHistory)
    }

    private fun pushRedoSnapshot(snapshot: List<UiPathPaint>) {
        _redoHistory.value = (_redoHistory.value + listOf(snapshot)).takeLast(MaxPathHistory)
    }

    private fun clearPathHistory() {
        _undoHistory.value = emptyList()
        _redoHistory.value = emptyList()
        pathTransformStart = null
    }

    private fun commitPaths(newPaths: List<UiPathPaint>) {
        if (newPaths == paths) return

        pushUndoSnapshot(paths)
        _redoHistory.value = emptyList()
        pathTransformStart = null
        _paths.value = newPaths
        registerChanges()
    }

    private val _imageFormat = mutableStateOf(ImageFormat.Default)
    val imageFormat by _imageFormat

    private val _isSaving: MutableState<Boolean> = mutableStateOf(false)
    val isSaving: Boolean by _isSaving

    private val _isSmartRedacting: MutableState<Boolean> = mutableStateOf(false)
    val isSmartRedacting: Boolean by _isSmartRedacting

    private val _saveExif: MutableState<Boolean> = mutableStateOf(false)
    val saveExif: Boolean by _saveExif

    private val _helperGridParams = fileController.savable(
        scope = componentScope,
        initial = HelperGridParams()
    )
    val helperGridParams: HelperGridParams by _helperGridParams

    init {
        componentScope.launch {
            val settingsState = settingsProvider.getSettingsState()
            _drawPathMode.update { DrawPathMode.fromOrdinal(settingsState.defaultDrawPathMode) }
        }
    }

    private var savingJob: Job? by smartJob {
        _isSaving.update { false }
    }

    fun saveBitmap(
        oneTimeSaveLocationUri: String?
    ) {
        savingJob = trackProgress {
            _isSaving.value = true
            getDrawingBitmap()?.let { localBitmap ->
                parseSaveResult(
                    fileController.save(
                        saveTarget = ImageSaveTarget(
                            imageInfo = ImageInfo(
                                originalUri = _uri.value.toString(),
                                imageFormat = imageFormat,
                                width = localBitmap.width,
                                height = localBitmap.height
                            ),
                            originalUri = _uri.value.toString(),
                            sequenceNumber = null,
                            data = imageCompressor.compressAndTransform(
                                image = localBitmap,
                                imageInfo = ImageInfo(
                                    originalUri = _uri.value.toString(),
                                    imageFormat = imageFormat,
                                    width = localBitmap.width,
                                    height = localBitmap.height
                                )
                            )
                        ),
                        keepOriginalMetadata = _saveExif.value,
                        oneTimeSaveLocationUri = oneTimeSaveLocationUri
                    ).onSuccess(::registerSave)
                )
            }
            _isSaving.value = false
        }
    }

    private fun calculateScreenOrientationBasedOnBitmap(bitmap: Bitmap): Int {
        val imageRatio = bitmap.width / bitmap.height.toFloat()
        return if (imageRatio <= 1.05f) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    fun setImageFormat(imageFormat: ImageFormat) {
        _imageFormat.value = imageFormat
        registerChanges()
    }

    fun setSaveExif(bool: Boolean) {
        _saveExif.value = bool
        registerChanges()
    }

    private fun updateBitmap(bitmap: Bitmap?) {
        componentScope.launch {
            _isImageLoading.value = true
            val scaledBitmap = imageScaler.scaleUntilCanShow(bitmap)
            val scaledImageBitmap = scaledBitmap?.let {
                withContext(defaultDispatcher) {
                    it.copy(Bitmap.Config.ARGB_8888, true).asImageBitmap()
                }
            }
            _imageBitmap.value = scaledImageBitmap
            _isImageLoading.value = false
        }
    }

    fun setUri(
        uri: Uri
    ) {
        renderCache.clear()
        componentScope.launch {
            _paths.value = listOf()
            clearPathHistory()
            _imageBitmap.value = null
            _isImageLoading.value = true

            _uri.value = uri
            imageGetter.getImageData(
                uri = uri.toString(),
                size = 2500,
                onFailure = AppToastHost::showFailureToast
            )?.let { data ->
                if (drawBehavior !is DrawBehavior.Background) {
                    _drawBehavior.update {
                        DrawBehavior.Image(calculateScreenOrientationBasedOnBitmap(data.image))
                    }
                }
                updateBitmap(data.image)
                _imageFormat.update {
                    when (val sourceFormat = data.imageInfo.imageFormat) {
                        ImageFormat.Jpg,
                        ImageFormat.Jpeg,
                        is ImageFormat.Png,
                        is ImageFormat.Webp -> sourceFormat

                        else -> ImageFormat.Png.Lossless
                    }
                }
            } ?: run {
                _isImageLoading.value = false
            }
        }
    }

    private suspend fun getDrawingBitmap(): Bitmap? = withContext(defaultDispatcher) {
        imageDrawApplier.applyDrawToImage(
            drawBehavior = drawBehavior.let {
                if (it is DrawBehavior.Background) {
                    it.copy(color = backgroundColor.toArgb(), gradient = backgroundGradient)
                } else it
            },
            pathPaints = paths,
            imageUri = _uri.value.toString()
        )
    }

    fun openColorPicker() {
        componentScope.launch {
            _colorPickerBitmap.value = getDrawingBitmap()
        }
    }

    fun resetDrawBehavior() {
        renderCache.clear()
        _paths.value = listOf()
        clearPathHistory()
        _imageBitmap.value = null
        _drawBehavior.update {
            DrawBehavior.None
        }
        _drawPathMode.update { DrawPathMode.Free }
        _uri.value = Uri.EMPTY
        _backgroundColor.value = Color.Transparent
        _backgroundGradient.value = null
        registerChangesCleared()
    }

    fun startDrawOnBackground(
        reqWidth: Int,
        reqHeight: Int,
        color: Color,
        gradient: GradientFill? = null,
    ) {
        renderCache.clear()
        val width = reqWidth.takeIf { it > 0 } ?: 1
        val height = reqHeight.takeIf { it > 0 } ?: 1
        val imageRatio = width / height.toFloat()
        _drawBehavior.update {
            DrawBehavior.Background(
                orientation = if (imageRatio <= 1f) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                },
                width = width,
                height = height,
                color = color.toArgb(),
                gradient = gradient
            )
        }
        _backgroundColor.value = color
        _backgroundGradient.value = gradient

        componentScope.launch {
            val newValue = DrawOnBackgroundParams(
                width = width,
                height = height,
                color = color.toArgb(),
                gradient = gradient
            )

            _drawOnBackgroundParams.update { newValue }
        }
    }

    fun shareBitmap() {
        savingJob = trackProgress {
            _isSaving.value = true
            getDrawingBitmap()?.let {
                shareProvider.shareImage(
                    image = it,
                    imageInfo = ImageInfo(
                        originalUri = _uri.value.toString(),
                        imageFormat = imageFormat,
                        width = it.width,
                        height = it.height
                    ),
                    onComplete = AppToastHost::showConfetti
                )
            }
            _isSaving.value = false
        }
    }

    fun prepareCrop(
        onReady: (Bitmap, Uri, IntSize) -> Unit
    ) {
        savingJob = trackProgress {
            _isSaving.value = true
            getDrawingBitmap()?.let { image ->
                val imageInfo = ImageInfo(
                    originalUri = _uri.value.toString(),
                    imageFormat = imageFormat,
                    width = image.width,
                    height = image.height
                )
                shareProvider.cacheImage(
                    image = image,
                    imageInfo = imageInfo
                )?.let { cachedUri ->
                    val preview = imageScaler.scaleUntilCanShow(image) ?: image
                    onReady(
                        preview,
                        cachedUri.toUri(),
                        IntSize(image.width, image.height)
                    )
                }
            }
            _isSaving.value = false
        }
    }

    fun smartRedact() {
        val preview = imageBitmap ?: return

        componentScope.launch {
            _isSmartRedacting.value = true
            runCatching {
                val bitmap = preview.asAndroidBitmap()
                val canvasSize = IntegerSize(bitmap.width, bitmap.height)
                val padding = (minOf(bitmap.width, bitmap.height) * 0.006f).coerceAtLeast(2f)

                smartRedactionEngine.detect(bitmap).map { region ->
                    val bounds = region.bounds
                    UiPathPaint(
                        path = Path().apply {
                            addRect(
                                Rect(
                                    left = (bounds.left - padding).coerceAtLeast(0f),
                                    top = (bounds.top - padding).coerceAtLeast(0f),
                                    right = (bounds.right + padding).coerceAtMost(bitmap.width.toFloat()),
                                    bottom = (bounds.bottom + padding).coerceAtMost(bitmap.height.toFloat())
                                )
                            )
                        },
                        strokeWidth = 1.pt,
                        brushSoftness = 0.pt,
                        drawColor = Color.Black,
                        isErasing = false,
                        drawMode = DrawMode.Pen,
                        canvasSize = canvasSize,
                        drawPathMode = DrawPathMode.Rect()
                    )
                }
            }.onSuccess { redactions ->
                if (redactions.isNotEmpty()) {
                    commitPaths(paths + redactions)
                } else {
                    AppToastHost.showToast("沒有找到可自動遮蔽的敏感資訊")
                }
            }.onFailure(AppToastHost::showFailureToast)
            _isSmartRedacting.value = false
        }
    }

    fun updateBackgroundColor(color: Color) {
        _backgroundColor.value = color
        _backgroundGradient.value = null
        registerChanges()
    }

    fun updateBackgroundGradient(gradient: GradientFill) {
        _backgroundGradient.value = gradient
        registerChanges()
    }

    fun clearDrawing() {
        if (paths.isNotEmpty()) {
            commitPaths(emptyList())
        }
    }

    fun undo() {
        val previous = _undoHistory.value.lastOrNull() ?: return
        pathTransformStart = null
        pushRedoSnapshot(paths)
        _undoHistory.value = _undoHistory.value.dropLast(1)
        _paths.value = previous
        registerChanges()
    }

    fun redo() {
        val next = _redoHistory.value.lastOrNull() ?: return
        pathTransformStart = null
        pushUndoSnapshot(paths)
        _redoHistory.value = _redoHistory.value.dropLast(1)
        _paths.value = next
        registerChanges()
    }

    fun addPath(pathPaint: UiPathPaint) {
        commitPaths(paths + pathPaint)
    }

    fun removePath(pathPaint: UiPathPaint) {
        commitPaths(paths - pathPaint)
    }

    fun removePathAt(index: Int) {
        if (index !in paths.indices) return
        commitPaths(paths.filterIndexed { pathIndex, _ -> pathIndex != index })
    }

    fun beginPathTransform() {
        if (pathTransformStart == null) {
            pathTransformStart = paths
        }
    }

    fun previewPathTransform(
        index: Int,
        pathPaint: UiPathPaint
    ) {
        if (index !in paths.indices) return
        if (pathTransformStart == null) beginPathTransform()

        _paths.value = paths.toMutableList().apply {
            this[index] = pathPaint
        }
    }

    fun finishPathTransform() {
        val before = pathTransformStart ?: return
        pathTransformStart = null

        if (before != paths) {
            pushUndoSnapshot(before)
            _redoHistory.value = emptyList()
            registerChanges()
        }
    }

    fun cancelPathTransform() {
        pathTransformStart?.let { _paths.value = it }
        pathTransformStart = null
    }

    fun scalePathAt(
        index: Int,
        factor: Float
    ) {
        val current = paths.getOrNull(index) ?: return
        val nativePath = android.graphics.Path(current.path.asAndroidPath())
        val bounds = RectF()
        nativePath.computeBounds(bounds, true)
        if (bounds.isEmpty) return

        nativePath.transform(
            Matrix().apply {
                setScale(
                    factor,
                    factor,
                    bounds.centerX(),
                    bounds.centerY()
                )
            }
        )

        val scaled = current.copy(
            path = nativePath.asComposePath(),
            strokeWidth = (current.strokeWidth.value * factor)
                .coerceIn(1f, 100f)
                .pt
        )

        commitPaths(
            paths.toMutableList().apply {
                this[index] = scaled
            }
        )
    }

    fun cancelSaving() {
        savingJob?.cancel()
        savingJob = null
        _isSaving.value = false
    }

    fun cacheCurrentImage(onComplete: (Uri) -> Unit) {
        savingJob = trackProgress {
            _isSaving.value = true
            getDrawingBitmap()?.let { image ->
                shareProvider.cacheImage(
                    image = image,
                    imageInfo = ImageInfo(
                        originalUri = _uri.value.toString(),
                        imageFormat = imageFormat,
                        width = image.width,
                        height = image.height
                    )
                )?.let { uri ->
                    onComplete(uri.toUri())
                }
            }
            _isSaving.value = false
        }
    }

    fun updateDrawMode(drawMode: DrawMode) {
        _drawMode.update { drawMode }

        if (drawMode is DrawMode.Warp) {
            _drawPathMode.update { DrawPathMode.Free }
            _drawLineStyle.update { DrawLineStyle.None }
        }
    }

    fun updateDrawPathMode(drawPathMode: DrawPathMode) {
        _drawPathMode.update { drawPathMode }
    }

    fun getFormatForFilenameSelection(): ImageFormat = imageFormat

    fun updateDrawLineStyle(style: DrawLineStyle) {
        _drawLineStyle.update { style }
    }

    fun updateHelperGridParams(params: HelperGridParams) {
        _helperGridParams.update { params }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(
            componentContext: ComponentContext,
            initialUri: Uri?,
            onGoBack: () -> Unit,
            onNavigate: (Screen) -> Unit,
        ): DrawComponent
    }
}

private const val MaxPathHistory = 50
