// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import android.content.Context
import androidx.room.Room

fun createAndroidContainer(context: Context): AppContainer =
    AppContainer(openAndroidChronicle(context.getDatabasePath(CHRONICLE_FILENAME).absolutePath, context))

internal fun openAndroidChronicle(path: String, context: Context): ChronicleDatabase =
    openChronicle(Room.databaseBuilder<ChronicleDatabase>(context.applicationContext, path))
