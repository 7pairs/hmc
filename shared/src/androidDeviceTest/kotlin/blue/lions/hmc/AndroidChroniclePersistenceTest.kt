// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class AndroidChroniclePersistenceTest {
    @Test
    fun roundTrip() = exercise { roundTrip() }

    @Test
    fun atomicityAndParallelSaves() = exercise { atomicityAndParallelSaves() }

    private fun exercise(test: suspend ChroniclePersistenceChecks.() -> Unit) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "chronicle-test-${kotlin.uuid.Uuid.random()}")
        check(directory.mkdir())
        try {
            ChroniclePersistenceChecks { openAndroidChronicle(File(directory, CHRONICLE_FILENAME).absolutePath, context) }.test()
        } finally {
            directory.deleteRecursively()
        }
    }
}
