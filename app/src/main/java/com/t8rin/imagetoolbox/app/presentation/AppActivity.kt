/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.app.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.retainedComponent
import com.t8rin.imagetoolbox.core.domain.resource.ResourceManager
import com.t8rin.imagetoolbox.core.domain.saving.FileController
import com.t8rin.imagetoolbox.core.domain.saving.FileController.Companion.toMetadataProvider
import com.t8rin.imagetoolbox.core.domain.saving.KeepAliveService
import com.t8rin.imagetoolbox.core.settings.domain.SettingsManager
import com.t8rin.imagetoolbox.core.settings.domain.model.SettingsState
import com.t8rin.imagetoolbox.core.settings.domain.toSimpleSettingsInteractor
import com.t8rin.imagetoolbox.core.settings.presentation.model.toUiState
import com.t8rin.imagetoolbox.core.settings.presentation.provider.LocalSimpleSettingsInteractor
import com.t8rin.imagetoolbox.core.ui.utils.content_pickers.ProvideImagePickerEventEmitter
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.t8rin.imagetoolbox.core.ui.utils.provider.ImageToolboxCompositionLocals
import com.t8rin.imagetoolbox.core.ui.utils.provider.LocalKeepAliveService
import com.t8rin.imagetoolbox.core.ui.utils.provider.LocalMetadataProvider
import com.t8rin.imagetoolbox.core.ui.utils.provider.LocalResourceManager
import com.t8rin.imagetoolbox.core.ui.utils.provider.LocalWindowSizeClass
import com.t8rin.imagetoolbox.feature.draw.presentation.DrawContent
import com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic.DrawComponent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    @Inject
    lateinit var drawComponentFactory: DrawComponent.Factory

    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var fileController: FileController

    @Inject
    lateinit var keepAliveService: KeepAliveService

    @Inject
    lateinit var resourceManager: ResourceManager

    private var settingsState by mutableStateOf(SettingsState.Default)

    private val component: DrawComponent by lazy {
        retainedComponent { componentContext ->
            drawComponentFactory(
                componentContext = componentContext,
                initialUri = null,
                onGoBack = ::finish,
                onNavigate = {}
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            settingsManager.settingsState.collectLatest {
                settingsState = it
            }
        }

        if (savedInstanceState == null) {
            handleIntent(intent)
        }

        setContent {
            ProvideImagePickerEventEmitter {
                CompositionLocalProvider(
                    LocalSimpleSettingsInteractor provides settingsManager.toSimpleSettingsInteractor(),
                    LocalMetadataProvider provides fileController.toMetadataProvider(),
                    LocalKeepAliveService provides keepAliveService,
                    LocalResourceManager provides resourceManager,
                    LocalWindowSizeClass provides calculateWindowSizeClass(this)
                ) {
                    ImageToolboxCompositionLocals(
                        settingsState = settingsState.toUiState(),
                        currentScreen = Screen.Draw(),
                        onNavigate = {}
                    ) {
                        DrawContent(component = component)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.firstImageUriOrNull()?.let(component::setUri)
    }
}

private fun Intent.firstImageUriOrNull(): Uri? = when (action) {
    Intent.ACTION_SEND -> {
        (extras?.get(Intent.EXTRA_STREAM) as? Uri)
            ?: clipData?.getItemAt(0)?.uri
            ?: data
    }

    Intent.ACTION_VIEW,
    Intent.ACTION_EDIT -> data ?: clipData?.getItemAt(0)?.uri

    else -> null
}
