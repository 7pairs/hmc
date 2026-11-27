// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import android.os.Parcel
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidTaskStateTest {
    @Test
    fun restoresConfirmationAndCursorOnlyWhenATaskDraftExists() = runBlocking {
        val repository = TaskRepository()
        val original = repository.newState()
        original.fillAndConfirm()
        original.fields.first().edit { selection = TextRange(2) }
        val restored = repository.restore(original)
        assertEquals(original.seats, restored.seats)
        assertEquals("20100", restored.fields.first().text.toString())
        assertEquals(TextRange(2), restored.fields.first().selection)
        assertEquals(original.visibleConfirmation, restored.visibleConfirmation)
        assertTrue(restored.canSave)

        // A seat edit followed by a move back still requires reconfirmation.
        original.select(0, "B")
        original.select(0, "A")
        val edited = repository.restore(original)
        assertEquals(original.seats, edited.seats)
        assertTrue(edited.visibleConfirmation.rows.isEmpty())
        assertFalse(edited.canSave)

        val fresh = repository.newState()
        fresh.loadPlayers()
        assertTrue(fresh.seats.all { it == null })
        assertTrue(fresh.fields.all { it.text.isEmpty() })
        assertFalse(fresh.canSave)
    }

    @Test
    fun interruptedSaveIsLookedUpBeforeRetriedWithTheOriginalRequest() = runBlocking {
        val repository = TaskRepository()
        val original = repository.newState()
        original.fillAndConfirm()
        repository.saveGate = CompletableDeferred()
        val saving = launch(start = CoroutineStart.UNDISPATCHED) { original.save() }
        assertEquals(SaveState.WORKING, original.saveState)
        val restored = repository.restore(original)
        saving.cancelAndJoin()
        assertEquals(SaveState.UNKNOWN, restored.saveState)
        assertFalse(restored.canEdit)
        assertEquals(original.visibleConfirmation, restored.visibleConfirmation)
        restored.save()
        val firstRequest = repository.saves.single()

        repository.reconcileResult = SaveResult.MISSING
        restored.reconcile()
        assertEquals(listOf(firstRequest), repository.lookups)
        assertEquals(SaveState.FAILED, restored.saveState)

        repository.saveGate = null
        restored.save()
        assertEquals(listOf(firstRequest, firstRequest), repository.saves)
        assertEquals(SaveState.SAVED, restored.saveState)
    }

    @Test
    fun interruptedLookupOfCommittedSaveDoesNotWriteAgainAfterRestore() = runBlocking {
        val repository = TaskRepository()
        val original = repository.newState()
        original.fillAndConfirm()
        repository.saveResult = SaveResult.UNKNOWN
        original.save()
        repository.reconcileGate = CompletableDeferred()
        val checking = launch(start = CoroutineStart.UNDISPATCHED) { original.reconcile() }
        assertEquals(SaveState.CHECKING, original.saveState)
        val restored = repository.restore(original)
        checking.cancelAndJoin()
        assertEquals(SaveState.UNKNOWN, restored.saveState)
        repository.reconcileGate = null
        repository.reconcileResult = SaveResult.SAVED
        restored.reconcile()
        val firstRequest = repository.saves.single()
        assertEquals(listOf(firstRequest, firstRequest), repository.lookups)
        assertEquals(SaveState.SAVED, restored.saveState)
        assertTrue(restored.fields.all { it.text.isEmpty() })
    }

    private suspend fun ChronicleState.fillAndConfirm() {
        loadPlayers()
        listOf("20100", "50000", "-1100", "31000").forEachIndexed { i, score ->
            select(i, players[i].id)
            fields[i].setTextAndPlaceCursorAtEnd(score)
        }
        confirm()
    }

    private class TaskRepository : ChronicleRepository {
        val saves = mutableListOf<MatchRecord>()
        val lookups = mutableListOf<MatchRecord>()
        var saveGate: CompletableDeferred<SaveResult>? = null
        var reconcileGate: CompletableDeferred<SaveResult>? = null
        var saveResult = SaveResult.SAVED
        var reconcileResult = SaveResult.UNKNOWN

        override suspend fun players() = listOf("A", "B", "C", "D").map { Player(it, it) }
        override suspend fun writePlayer(id: String?, name: String) = error("unexpected write")
        override suspend fun currentRule() =
            MatchRule(INITIAL_RULE_ID, M_LEAGUE_CALCULATION, PlacementPoints(50, 10, -10, -30))

        override suspend fun save(record: MatchRecord): SaveResult {
            saves.add(record)
            return saveGate?.await() ?: saveResult
        }

        override suspend fun reconcile(record: MatchRecord): SaveResult {
            lookups.add(record)
            return reconcileGate?.await() ?: reconcileResult
        }

        fun newState() = ChronicleState(this, { "original" }, { 42L })

        suspend fun restore(original: ChronicleState): ChronicleState {
            val parcel = Parcel.obtain()
            val draft = try {
                parcel.writeBundle(original.saveAndroidTaskState())
                parcel.setDataPosition(0)
                checkNotNull(parcel.readBundle()).toChronicleTaskDraft()
            } finally {
                parcel.recycle()
            }
            val restored = ChronicleState(this, { error("unexpected ID") }, { error("unexpected time") }, draft)
            restored.loadPlayers()
            return restored
        }
    }
}
