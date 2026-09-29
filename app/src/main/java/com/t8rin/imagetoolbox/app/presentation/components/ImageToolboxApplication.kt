/*
 * ImageToolbox is an image editor for android
 * Copyright (c) 2026 T8RIN (Malik Mukhametzyanov)
 * Modifications Copyright (c) 2026 jush0147
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.t8rin.imagetoolbox.app.presentation.components

import com.t8rin.imagetoolbox.app.presentation.components.functions.injectBaseComponent
import com.t8rin.imagetoolbox.app.presentation.components.functions.setupFlags
import com.t8rin.imagetoolbox.app.presentation.components.utils.isMain
import com.t8rin.imagetoolbox.core.domain.saving.KeepAliveService
import com.t8rin.imagetoolbox.core.ui.utils.ComposeApplication
import com.t8rin.imagetoolbox.core.utils.initAppContext
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ImageToolboxApplication : ComposeApplication() {

    @Inject
    lateinit var keepAliveService: KeepAliveService

    private var isSetupCompleted: Boolean = false

    override fun onCreate() {
        super.onCreate()
        runSetup()
    }

    override fun runSetup() {
        if (isSetupCompleted) return

        if (isMain()) {
            setupFlags()
            initAppContext()
            injectBaseComponent()

            isSetupCompleted = true
        }
    }
}
