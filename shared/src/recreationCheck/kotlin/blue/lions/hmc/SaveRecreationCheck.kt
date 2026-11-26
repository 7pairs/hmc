// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.uuid.Uuid

/** Available only with the opt-in test build. Its database is separate from the app database. */
class SaveRecreationCheck internal constructor(private val open: () -> ChronicleDatabase) {
    private var database = open()
    private var repository: ChronicleRepository = RoomChronicleRepository(database)
    private var attempts by mutableStateOf(0)
    private var host by mutableStateOf<String?>(null)
    private var originalHost by mutableStateOf<String?>(null)
    private var message by mutableStateOf("結果を選んでください。")
    private var running by mutableStateOf(false)
    private var finished by mutableStateOf(false)
    private var fail = false
    private var firstRequest: MatchRecord? = null
    private val gate = CompletableDeferred<Unit>()
    private var canRetry by mutableStateOf(false)
    private var saving: Job? = null
    private val gatedRepository = object : ChronicleRepository {
        override suspend fun players() = repository.players()
        override suspend fun writePlayer(id: String?, name: String) = repository.writePlayer(id, name)
        override suspend fun currentRule() = repository.currentRule()
        override suspend fun reconcile(record: MatchRecord) = repository.reconcile(record)

        override suspend fun save(record: MatchRecord): SaveResult {
            attempts++
            if (attempts == 1) {
                firstRequest = record.copy(seats = record.seats.toList())
                gate.await()
            } else {
                check(record == firstRequest) { "再試行で保存内容、ID、または時刻が変わりました。" }
            }
            return repository.save(record)
        }
    }
    private val state = ChronicleState(
        gatedRepository,
        { Uuid.random().toString() },
        { Clock.System.now().toEpochMilliseconds() },
    )

    @Composable
    fun Content(scope: CoroutineScope, hostId: String, onRecreate: () -> Unit) {
        LaunchedEffect(hostId) { host = hostId }
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Text("画面再生成テスト")
            Text(message)
            Text("保存依頼 ${attempts}回")
            if (!running && !finished && state.screen == Screen.PLAYERS) {
                Button(onClick = { begin(scope, false) }) { Text("成功を試す") }
                Button(onClick = { begin(scope, true) }) { Text("失敗と再試行を試す") }
            }
            val pending = state.saveState == SaveState.WORKING && attempts == 1 && !gate.isCompleted
            if (pending && host == originalHost) {
                Button(onClick = onRecreate) { Text("画面を作り直す") }
            }
            if (pending && host != null && host != originalHost) {
                Text("新しい画面で保存中")
                Button(onClick = { finish(scope) }) { Text("保存を進める") }
            }
            if (canRetry) {
                Button(onClick = { retry(scope) }) { Text("保存をやり直す") }
            }
            App(state, scope)
        }
    }

    private fun begin(scope: CoroutineScope, fail: Boolean) {
        if (running || finished) return
        running = true
        this.fail = fail
        originalHost = host
        saving = scope.launch {
            reportFailures {
                for (name in listOf("A", "B", "C", "D")) {
                    state.writePlayer(null, name)
                    check(state.nameResult == NameResult.SAVED)
                }
                state.showScores()
                for ((index, score) in listOf("20100", "50000", "-1100", "31000").withIndex()) {
                    state.select(index, state.players[index].id)
                    state.fields[index].edit { append(score) }
                }
                state.confirm()
                check(state.canSave)
                if (fail) execute(
                    "CREATE TRIGGER reject_north BEFORE INSERT ON match_seats " +
                        "WHEN NEW.seat = 'NORTH' BEGIN SELECT RAISE(ABORT, 'test'); END",
                )
                message = "保存を待機中。画面を作り直せます。"
                state.save()
            }
        }
    }

    private fun finish(scope: CoroutineScope) {
        if (gate.isCompleted || host == originalHost) return
        gate.complete(Unit)
        scope.launch {
            reportFailures {
                saving?.join()
                check(attempts == 1)
                if (fail) {
                    check(state.saveState == SaveState.FAILED)
                    check(state.fields.first().text.toString() == "20100")
                    check(state.visibleConfirmation.rows.size == 4)
                    reopenAndCheck(0)
                    message = "保存失敗を確認済み。DBの記録は0件。"
                    canRetry = true
                } else {
                    confirmSaved()
                }
            }
        }
    }

    private fun retry(scope: CoroutineScope) {
        if (!canRetry) return
        canRetry = false
        scope.launch {
            reportFailures {
                execute("DROP TRIGGER reject_north")
                state.save()
                check(attempts == 2)
                confirmSaved()
            }
        }
    }

    private suspend fun confirmSaved() {
        check(state.saveState == SaveState.SAVED)
        check(state.fields.all { it.text.isEmpty() } && state.seats.all { it == null })
        reopenAndCheck(1)
        message = if (fail) "確認済み。書き込み失敗後は0件、再試行後は1件。" else "確認済み。保存は1回、記録は1件。"
        finished = true
        running = false
    }

    private suspend fun execute(sql: String) {
        database.useWriterConnection { connection -> connection.usePrepared(sql) { it.step() } }
    }

    private suspend fun reopenAndCheck(matches: Int) {
        database.close()
        database = open()
        repository = RoomChronicleRepository(database)
        database.useReaderConnection { connection ->
            connection.usePrepared("SELECT count(*) FROM matches") {
                check(it.step() && it.getInt(0) == matches)
            }
            connection.usePrepared("SELECT count(*) FROM match_seats") {
                check(it.step() && it.getInt(0) == matches * 4)
            }
        }
        if (matches == 1) {
            check(repository.reconcile(checkNotNull(firstRequest)) == SaveResult.SAVED)
        }
    }

    private suspend fun reportFailures(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            message = "確認に失敗しました。 ${error.message}"
            running = false
            finished = true
        }
    }
}
