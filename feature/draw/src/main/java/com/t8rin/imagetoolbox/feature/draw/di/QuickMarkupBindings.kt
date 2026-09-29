/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.feature.draw.di

import android.graphics.Bitmap
import com.t8rin.imagetoolbox.core.domain.model.ImageModel
import com.t8rin.imagetoolbox.core.domain.model.IntegerSize
import com.t8rin.imagetoolbox.core.domain.remote.AnalyticsManager
import com.t8rin.imagetoolbox.core.domain.saving.RandomStringGenerator
import com.t8rin.imagetoolbox.core.domain.saving.io.Writeable
import com.t8rin.imagetoolbox.core.domain.transformation.EmptyTransformation
import com.t8rin.imagetoolbox.core.domain.transformation.Transformation
import com.t8rin.imagetoolbox.core.filters.domain.FilterParamsInteractor
import com.t8rin.imagetoolbox.core.filters.domain.FilterProvider
import com.t8rin.imagetoolbox.core.filters.domain.ShaderPresetRepository
import com.t8rin.imagetoolbox.core.filters.domain.model.Filter
import com.t8rin.imagetoolbox.core.filters.domain.model.TemplateFilter
import com.t8rin.imagetoolbox.core.filters.domain.model.shader.ShaderPreset
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Small bindings required by inherited ImageToolbox infrastructure that the quick-markup app
 * does not expose. Keeping them here avoids pulling the full Filters/Cipher/Crash feature stack
 * back into the APK just to satisfy Hilt.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object QuickMarkupBindings {

    @Provides
    @Singleton
    fun analyticsManager(): AnalyticsManager = object : AnalyticsManager {
        override val allowCollectCrashlytics: Boolean = false
        override val allowCollectAnalytics: Boolean = false

        override fun updateAnalyticsCollectionEnabled(value: Boolean) = Unit
        override fun updateAllowCollectCrashlytics(value: Boolean) = Unit
        override fun sendReport(throwable: Throwable) = Unit
        override fun registerScreenOpen(screenName: String) = Unit
        override fun pushMetric(tag: String, metric: String) = Unit
    }

    @Provides
    @Singleton
    fun randomStringGenerator(): RandomStringGenerator = object : RandomStringGenerator {
        private val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        override fun generate(length: Int): String = buildString(length.coerceAtLeast(0)) {
            repeat(length.coerceAtLeast(0)) {
                append(alphabet[Random.nextInt(alphabet.length)])
            }
        }
    }

    @Provides
    @Singleton
    fun filterProvider(): FilterProvider<Bitmap> = object : FilterProvider<Bitmap> {
        override fun filterToTransformation(
            filter: Filter<*>
        ): Transformation<Bitmap> = EmptyTransformation()
    }

    @Provides
    @Singleton
    fun filterParamsInteractor(): FilterParamsInteractor = object : FilterParamsInteractor {
        override fun getRecentFilters(): Flow<List<Filter<*>>> = flowOf(emptyList())
        override suspend fun addRecentFilter(filter: Filter<*>) = Unit

        override fun getFavoriteFilters(): Flow<List<Filter<*>>> = flowOf(emptyList())
        override suspend fun toggleFavorite(filter: Filter<*>) = Unit

        override suspend fun addTemplateFilter(
            templateFilter: TemplateFilter,
            replacing: TemplateFilter?
        ) = Unit

        override fun getTemplateFilters(): Flow<List<TemplateFilter>> = flowOf(emptyList())

        override suspend fun addTemplateFilterFromString(
            string: String,
            onSuccess: suspend (filterName: String, filtersCount: Int) -> Unit,
            onFailure: suspend () -> Unit
        ) = onFailure()

        override suspend fun convertTemplateFilterToString(
            templateFilter: TemplateFilter
        ): String = ""

        override suspend fun removeTemplateFilter(templateFilter: TemplateFilter) = Unit

        override suspend fun addTemplateFilterFromUri(
            uri: String,
            onSuccess: suspend (filterName: String, filtersCount: Int) -> Unit,
            onFailure: suspend () -> Unit
        ) = onFailure()

        override suspend fun addTemplateFiltersFromUris(
            uris: List<String>
        ): List<TemplateFilter> = emptyList()

        override suspend fun exportTemplateFilters(
            templateFilters: List<TemplateFilter>,
            destination: Writeable
        ) = Unit

        override fun isValidTemplateFilter(string: String): Boolean = false

        override suspend fun reorderFavoriteFilters(newOrder: List<Filter<*>>) = Unit

        override fun getFilterPreviewModel(): Flow<ImageModel> = flowOf(ImageModel(""))

        override fun getCanSetDynamicFilterPreview(): Flow<Boolean> = flowOf(false)

        override suspend fun setCanSetDynamicFilterPreview(value: Boolean) = Unit

        override suspend fun setFilterPreviewModel(uri: String) = Unit
    }

    @Provides
    @Singleton
    fun shaderPresetRepository(): ShaderPresetRepository = object : ShaderPresetRepository {
        override fun getPresets(): Flow<List<ShaderPreset>> = flowOf(emptyList())

        override suspend fun savePreset(
            preset: ShaderPreset,
            replacingName: String?
        ): Result<Unit> = Result.success(Unit)

        override suspend fun importPreset(json: String): Result<ShaderPreset> =
            Result.failure(UnsupportedOperationException("Shader presets are not used by 畫重點"))

        override suspend fun deletePreset(preset: ShaderPreset) = Unit

        override fun exportPreset(preset: ShaderPreset): String = ""
    }
}
