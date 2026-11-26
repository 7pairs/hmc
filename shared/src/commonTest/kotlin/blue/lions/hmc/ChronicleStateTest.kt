// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChronicleStateTest {
    private val repository = FakeChronicle()
    private var issuedIds = 0
    private val state = ChronicleState(repository, { "match-${++issuedIds}" }, { 42L })

    @Test
    fun registrationAndStartup() = immediate {
        state.loadPlayers()
        assertEquals(Screen.PLAYERS, state.screen)
        state.writePlayer(null, " A ")
        state.writePlayer(null, "A")
        assertEquals(listOf("A"), state.players.map { it.name })
        listOf("B", "C", "D").forEach { state.writePlayer(null, it) }
        state.showScores()
        assertEquals(Screen.SCORES, state.screen)
        val reopened = ChronicleState(repository, { error("unexpected ID") }, { 42L })
        reopened.loadPlayers()
        assertEquals(Screen.SCORES, reopened.screen)
    }

    @Test
    fun retryWithEditedPayloadUsesFirstIdentity() = immediate {
        prepareMatch()
        repository.saveResult = SaveResult.UNKNOWN
        state.save()
        assertEquals(SaveState.UNKNOWN, state.saveState)
        state.select(0, "B")
        state.save()
        assertEquals("A", state.seats[0])
        assertEquals(1, repository.saves.size)
        repository.lookup = SaveResult.MISSING
        state.reconcile()
        assertEquals(repository.saves, repository.lookups)
        state.select(0, "B")
        state.save()
        assertEquals(1, repository.saves.size)
        state.confirm()
        repository.saveResult = SaveResult.SAVED
        state.save()
        val (first, second) = repository.saves
        assertEquals(first.id, second.id)
        assertEquals(first.savedAtMillis, second.savedAtMillis)
        assertEquals("B", second.seats.first().playerId)
        assertEquals(1, issuedIds)
        assertTrue(state.seats.all { it == null })
        assertTrue(state.fields.all { it.text.isEmpty() })
    }

    @Test
    fun directConflictPreservesInput() = immediate {
        prepareMatch()
        repository.saveResult = SaveResult.CONFLICT
        state.save()
        assertEquals(SaveState.CONFLICT, state.saveState)
        state.select(0, "B")
        state.save()
        assertEquals("A", state.seats[0])
        assertEquals("20100", state.fields[0].text.toString())
        assertEquals(1, repository.saves.size)
        assertEquals(4, state.visibleConfirmation.rows.size)
        assertFalse(state.canEdit)
    }

    @Test
    fun unknownResultsRetainPendingRequestUntilReconciled() = immediate {
        prepareMatch()
        repository.saveResult = SaveResult.UNKNOWN
        state.save()
        state.reconcile()
        assertEquals(SaveState.UNKNOWN, state.saveState)
        assertEquals(4, state.visibleConfirmation.rows.size)
        repository.lookup = SaveResult.SAVED
        state.reconcile()
        assertEquals(SaveState.SAVED, state.saveState)
        assertEquals(List(2) { repository.saves.single() }, repository.lookups)
        assertTrue(state.fields.all { it.text.isEmpty() })
    }

    @Test
    fun reconciledConflictUsesSameStateAsDirectConflict() = immediate {
        prepareMatch()
        repository.saveResult = SaveResult.UNKNOWN
        repository.lookup = SaveResult.CONFLICT
        state.save()
        state.reconcile()
        assertEquals(SaveState.CONFLICT, state.saveState)
        state.save()
        assertEquals(1, repository.saves.size)
        assertEquals(4, state.visibleConfirmation.rows.size)
        assertFalse(state.canEdit)
    }

    @Test
    fun renamePreservesConfirmationAndDoesNotChangeTheAssignedPlayerId() = immediate {
        prepareMatch()
        state.showPlayers()
        state.writePlayer("A", "A changed")
        state.writePlayer(null, "E")
        state.showScores()
        assertEquals("A", state.seats.first())
        assertEquals("A changed", state.visibleConfirmation.rows.first().name)
        assertTrue(state.canSave)
        assertEquals(5, state.players.size)
    }

    @Test
    fun parallelSaveIsIgnoredAndConfirmedRuleIsPreserved() = immediate {
        prepareMatch()
        repository.rule = repository.rule.copy(id = "rule-b")
        repository.pause = true
        val first = start { state.save() }
        assertEquals(SaveState.WORKING, state.saveState)
        state.save()
        assertEquals(1, repository.saves.size)
        assertEquals(INITIAL_RULE_ID, repository.saves.single().ruleId)
        repository.pending!!.resume(SaveResult.MISSING)
        first.check()
        assertEquals(SaveState.FAILED, state.saveState)
        repository.pause = false
        repository.saveResult = SaveResult.SAVED
        state.save()
        assertEquals(repository.saves.first(), repository.saves.last())
    }

    private suspend fun prepareMatch() {
        repository.roster.addAll(listOf("A", "B", "C", "D").map { Player(it, it) })
        state.loadPlayers()
        listOf("20100", "50000", "-1100", "31000").forEachIndexed { i, score ->
            state.select(i, repository.roster[i].id)
            state.fields[i].setTextAndPlaceCursorAtEnd(score)
        }
        state.confirm()
    }

    private class FakeChronicle : ChronicleRepository {
        val roster = mutableListOf<Player>()
        val saves = mutableListOf<MatchRecord>()
        val lookups = mutableListOf<MatchRecord>()
        var rule = MatchRule(INITIAL_RULE_ID, M_LEAGUE_CALCULATION, PlacementPoints(50, 10, -10, -30))
        var saveResult = SaveResult.SAVED
        var lookup = SaveResult.UNKNOWN
        var pause = false
        var pending: Continuation<SaveResult>? = null
        override suspend fun players() = roster.toList()
        override suspend fun currentRule() = rule
        override suspend fun writePlayer(id: String?, name: String): NameResult {
            if (name.isEmpty()) return NameResult.EMPTY
            if (roster.any { it.name == name && it.id != id }) return NameResult.DUPLICATE
            if (id == null) {
                roster.add(Player(name, name))
            } else {
                val index = roster.indexOfFirst { it.id == id }
                roster[index] = Player(id, name)
            }
            return NameResult.SAVED
        }
        override suspend fun save(record: MatchRecord): SaveResult {
            saves += record
            return if (pause) suspendCoroutine { pending = it } else saveResult
        }
        override suspend fun reconcile(record: MatchRecord): SaveResult {
            lookups += record
            return lookup
        }
    }

    private fun immediate(block: suspend () -> Unit) = start(block).check()

    private class Completion : Continuation<Unit> {
        override val context = kotlin.coroutines.EmptyCoroutineContext
        private var result: Result<Unit>? = null
        override fun resumeWith(result: Result<Unit>) { this.result = result }
        fun check() { checkNotNull(result).getOrThrow() }
    }

    private fun start(block: suspend () -> Unit): Completion = Completion().also { block.startCoroutine(it) }
}
