// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosContainer(): AppContainer {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val path = checkNotNull(directory?.path) + "/" + CHRONICLE_FILENAME
    return AppContainer(openIosChronicle(path))
}

internal fun openIosChronicle(path: String): ChronicleDatabase =
    openChronicle(Room.databaseBuilder<ChronicleDatabase>(name = path))
