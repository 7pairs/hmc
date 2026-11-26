// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/shared/build.gradle.kts
// Modified for hmc (https://github.com/7pairs/hmc).

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "blue.lions.hmc.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {}
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        if (providers.gradleProperty("hmcRecreationCheck").orNull == "true") {
            commonMain {
                kotlin.srcDir("src/recreationCheck/kotlin")
            }
            androidMain {
                kotlin.srcDir("src/recreationCheckAndroid/kotlin")
            }
            matching { it.name == "iosMain" }.configureEach {
                kotlin.srcDir("src/recreationCheckIos/kotlin")
            }
        }
        commonMain.dependencies {
            api(libs.coroutines.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidDeviceTest") {
            kotlin.srcDir("src/persistenceTest/kotlin")
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(kotlin("test-junit"))
            }
        }
        matching { it.name == "iosTest" }.configureEach {
            kotlin.srcDir("src/persistenceTest/kotlin")
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

// Compose 1.12.2 leaves the output directory unset for this AGP device-test task.
// The device tests have no Compose resources of their own.
tasks.matching { it.name == "copyAndroidDeviceTestComposeResourcesToAndroidAssets" }.configureEach {
    enabled = false
}
