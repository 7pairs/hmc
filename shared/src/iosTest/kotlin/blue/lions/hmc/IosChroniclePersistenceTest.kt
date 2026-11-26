// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.uuid.Uuid
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory

class IosChroniclePersistenceTest {
    @Test
    fun roundTrip() = exercise { roundTrip() }

    @Test
    fun atomicityAndParallelSaves() = exercise { atomicityAndParallelSaves() }

    @OptIn(ExperimentalForeignApi::class)
    private fun exercise(test: suspend ChroniclePersistenceChecks.() -> Unit) = runBlocking {
        val directory = NSTemporaryDirectory() + "chronicle-test-" + Uuid.random()
        val files = NSFileManager.defaultManager
        check(files.createDirectoryAtPath(directory, withIntermediateDirectories = false, attributes = null, error = null))
        try {
            ChroniclePersistenceChecks { openIosChronicle("$directory/$CHRONICLE_FILENAME") }.test()
        } finally {
            files.removeItemAtPath(directory, error = null)
        }
    }
}
