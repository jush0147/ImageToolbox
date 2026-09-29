/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.t8rin.imagetoolbox.feature.draw.data

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.t8rin.imagetoolbox.feature.draw.domain.SensitiveDataDetector
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal data class SensitiveRegion(
    val bounds: Rect,
    val kind: SensitiveDataDetector.Kind
)

internal class SmartRedactionEngine @Inject constructor() {

    suspend fun detect(bitmap: Bitmap): List<SensitiveRegion> {
        val recognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )

        return try {
            val result = recognizer
                .process(InputImage.fromBitmap(bitmap, 0))
                .awaitResult()

            buildList {
                result.textBlocks.forEach { block ->
                    block.lines.forEach { line ->
                        addLineCandidates(line)
                    }
                }
            }.distinctBy {
                listOf(it.bounds.left, it.bounds.top, it.bounds.right, it.bounds.bottom, it.kind)
            }
        } finally {
            recognizer.close()
        }
    }

    private fun MutableList<SensitiveRegion>.addLineCandidates(line: Text.Line) {
        var directMatchFound = false

        line.elements.forEach { element ->
            val kind = SensitiveDataDetector.detect(element.text)
            val bounds = element.boundingBox
            if (kind != null && bounds != null) {
                add(SensitiveRegion(bounds = bounds, kind = kind))
                directMatchFound = true
            }
        }

        // OCR sometimes splits e-mail addresses or phone numbers into multiple elements.
        // In that case, redact the whole line rather than silently leaking the value.
        if (!directMatchFound) {
            val kind = SensitiveDataDetector.detect(line.text)
            val bounds = line.boundingBox
            if (kind != null && bounds != null) {
                add(SensitiveRegion(bounds = bounds, kind = kind))
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { continuation ->
    val completed = AtomicBoolean(false)

    addOnSuccessListener { result ->
        if (completed.compareAndSet(false, true)) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { throwable ->
        if (completed.compareAndSet(false, true)) {
            continuation.resumeWithException(throwable)
        }
    }
}
