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

@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.image.toolbox.application)
    alias(libs.plugins.image.toolbox.hilt)
    id("androidx.baselineprofile")
}

android {
    val requestedAbi = providers.gradleProperty("quickMarkupAbi").orNull
    val supportedAbi = requestedAbi
        ?.let { arrayOf(it) }
        ?: arrayOf("armeabi-v7a", "arm64-v8a", "x86_64")

    namespace = "com.t8rin.imagetoolbox"

    defaultConfig {
        vectorDrawables.useSupportLibrary = true

        applicationId = "com.e04stuff.markit"

        versionCode = libs.versions.versionCode.get().toIntOrNull()
        versionName = System.getenv("VERSION_NAME") ?: libs.versions.versionName.get()

        // Taiwan-first release: keep the default English resources and Traditional Chinese.
        // Upstream ships dozens of translations that Markit does not need in v1.
        resourceConfigurations += listOf("en", "zh-rTW")

    }

    androidResources {
        generateLocaleConfig = true

        // Markit is a lightweight screenshot/markup tool. Do not package models for
        // upstream background-removal and face-detection features that are not exposed.
        ignoreAssetsPatterns += listOf(
            "u2netp.onnx",
            "face_detection_yunet_2026may.onnx",
            "*.lottie"
        )
    }

    flavorDimensions += "app"

    productFlavors {
        create("foss") {
            dimension = "app"
            versionNameSuffix = "-foss"
            extra.set("gmsEnabled", false)
        }
        create("market") {
            dimension = "app"
            extra.set("gmsEnabled", true)
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_launcher_name", "畫重點 DEBUG")
            resValue("string", "file_provider", "com.e04stuff.markit.fileprovider.debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_launcher_name", "畫重點")
            resValue("string", "file_provider", "com.e04stuff.markit.fileprovider")
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    splits {
        abi {
            // Detect app bundle and conditionally disable split abis
            // This is needed due to a "Sequence contains more than one matching element" error
            // present since AGP 8.9.0, for more info see:
            // https://issuetracker.google.com/issues/402800800

            // AppBundle tasks usually contain "bundle" in their name
            //noinspection WrongGradleMethod
            val isBuildingBundle =
                gradle.startParameter.taskNames.any { it.lowercase().contains("bundle") }

            // Disable split abis when building appBundle
            isEnable = !isBuildingBundle
            reset()
            //noinspection ChromeOsAbiSupport
            include(*supportedAbi)
            isUniversalApk = requestedAbi == null
        }
    }

    lint {
        disable += "Instantiatable"
    }

    packaging {
        jniLibs {
            keepDebugSymbols.add("**/*.so")
                pickFirsts.add("**/libdatstore_shared_counter.so")

            // Quick Markup never exposes ImageToolbox's neural, OpenCV, warp or Aire tools.
            // Keep their compile-time contracts for now, but do not ship their native engines.
            excludes.add("**/libonnxruntime.so")
            excludes.add("**/libopencv_java5.so")
            excludes.add("**/libtrickle.so")
            excludes.add("**/libaire.so")
            excludes.add("**/libaire_filters.so")

            // Markit only preserves JPG/JPEG/PNG/WebP output. These native codecs belong
            // to ImageToolbox's advanced format support and are unreachable in Markit.
            excludes.add("**/libcoder.so") // AVIF / HEIC
            excludes.add("**/librjxlcoder.so")
            excludes.add("**/libjxl.so")
            excludes.add("**/libjxlcoder.so")
            excludes.add("**/libjxl_cms.so")
            excludes.add("**/libjxl_threads.so")
            excludes.add("**/libraw_coder.so")
            excludes.add("**/libdjvu-coder.so")
            excludes.add("**/libtiffconverter.so")
            excludes.add("**/libtiff.so")
            excludes.add("**/libtifffactory.so")
            excludes.add("**/libtiffsaver.so")
            excludes.add("**/libopenjpeg.so")
            excludes.add("**/libqoi-coder.so")
            excludes.add("**/libvvc_jni.so")
            excludes.add("**/libdav1d.so")
            excludes.add("**/libjpeglicoder.so")
            excludes.add("**/liboxipng_jni.so")
            excludes.add("**/libimagequant_jni.so")
            excludes.add("**/libgif_encoder.so")

            useLegacyPackaging = true
        }
        resources {
            excludes += "META-INF/"
            excludes += "META-INF/LICENSE.md"
            excludes += "kotlin/"
            excludes += "org/"
            excludes += ".properties"
            excludes += ".bin"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

    buildFeatures {
        resValues = true
    }
}

base {
    archivesName = "markit-${android.defaultConfig.versionName}"
}

aboutLibraries {
    export.excludeFields.addAll("generated")
}

dependencies {
    baselineProfile(project(":benchmark"))

    implementation(projects.feature.draw)
    implementation(projects.feature.settings)
}

baselineProfile {
    automaticGenerationDuringBuild = false
    dexLayoutOptimization = true
    mergeIntoMain = true
}

androidComponents {
    beforeVariants(selector().all()) { variantBuilder ->
        val flavorName = variantBuilder.productFlavors.firstOrNull()?.second.orEmpty()
        val flavorCap = flavorName.replaceFirstChar(Char::uppercase)

        val gmsEnabled = android.productFlavors
            .findByName(flavorName)
            ?.extra
            ?.get("gmsEnabled") == true

        tasks.configureEach {
            val isTargetTask = listOf("GoogleServices", "Crashlytics").any { marker ->
                name.contains(marker)
            } && name.contains(flavorCap)

            if (isTargetTask) {
                enabled = gmsEnabled
            }
        }
    }
}
