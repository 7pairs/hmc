// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import android.content.Context
import java.io.File
import kotlin.uuid.Uuid

fun createAndroidSaveRecreationCheck(context: Context): SaveRecreationCheck {
    val appContext = context.applicationContext
    val path = File(appContext.cacheDir, "chronicle-recreation-${Uuid.random()}.db").absolutePath
    return SaveRecreationCheck { openAndroidChronicle(path, appContext) }
}
