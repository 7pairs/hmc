// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import android.app.Application

class HmcApplication : Application() {
    val container: AppContainer by lazy { createAndroidContainer(this) }
}
