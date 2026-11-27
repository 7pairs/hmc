// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/Kotlin/KMP-App-Template/blob/6b6d09e4f3f5e845116422d59b658791ed6149c6/shared/src/iosMain/kotlin/com/jetbrains/kmpapp/MainViewController.kt
// Modified for hmc (https://github.com/7pairs/hmc).

package blue.lions.hmc

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class IosEntryOwner(container: AppContainer) {
    private val state = container.createState()
    private val scope = MainScope()

    fun makeViewController() = ComposeUIViewController { App(state, scope) }

    fun close() { scope.cancel() }
}
