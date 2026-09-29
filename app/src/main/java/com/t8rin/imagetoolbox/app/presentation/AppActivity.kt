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
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.retainedComponent
import com.t8rin.imagetoolbox.core.settings.presentation.model.toUiState
import com.t8rin.imagetoolbox.core.ui.utils.ComposeActivity
import com.t8rin.imagetoolbox.core.ui.utils.navigation.Screen
import com.t8rin.imagetoolbox.core.ui.utils.provider.ImageToolboxCompositionLocals
import com.t8rin.imagetoolbox.feature.draw.presentation.DrawContent
import com.t8rin.imagetoolbox.feature.draw.presentation.screenLogic.DrawComponent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : ComposeActivity() {

    @Inject
    lateinit var drawComponentFactory: DrawComponent.Factory

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

    override fun handleIntent(intent: Intent) {
        intent.firstImageUriOrNull()?.let(component::setUri)
    }

    @Composable
    override fun Content() {
        ImageToolboxCompositionLocals(
            settingsState = settingsState.toUiState(),
            currentScreen = Screen.Draw(),
            onNavigate = {}
        ) {
            DrawContent(component = component)
        }
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
