// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/build.gradle.kts
// Modified for hmc (https://github.com/7pairs/hmc).

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
