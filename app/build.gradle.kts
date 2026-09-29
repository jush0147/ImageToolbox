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

        applicationId = "com.jush0147.huazhongdian"

        versionCode = libs.versions.versionCode.get().toIntOrNull()
        versionName = System.getenv("VERSION_NAME") ?: libs.versions.versionName.get()

    }

    androidResources {
        generateLocaleConfig = true
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
            resValue("string", "file_provider", "com.jush0147.huazhongdian.fileprovider.debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_launcher_name", "畫重點")
            resValue("string", "file_provider", "com.jush0147.huazhongdian.fileprovider")
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
            pickFirsts.add("lib/*/libcoder.so")
            pickFirsts.add("**/libdatstore_shared_counter.so")
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
    archivesName = "image-toolbox-${android.defaultConfig.versionName}"
}

aboutLibraries {
    export.excludeFields.addAll("generated")
}

dependencies {
    baselineProfile(project(":benchmark"))

    implementation(projects.feature.draw)
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
