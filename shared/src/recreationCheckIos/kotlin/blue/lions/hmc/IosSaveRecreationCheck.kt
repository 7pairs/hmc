// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlin.uuid.Uuid
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIViewController

class IosSaveRecreationCheck {
    private val path = NSTemporaryDirectory() + "chronicle-recreation-" + Uuid.random() + ".db"
    private val check = SaveRecreationCheck { openIosChronicle(path) }
    private val scope = MainScope()

    fun makeViewController(onRecreate: () -> Unit): UIViewController {
        val hostId = Uuid.random().toString()
        return ComposeUIViewController { check.Content(scope, hostId, onRecreate) }
    }

    fun close() { scope.cancel() }
}
