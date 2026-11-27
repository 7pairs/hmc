// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.coroutines.cancellation.CancellationException

internal enum class Screen { LOADING, LOAD_FAILED, PLAYERS, SCORES }
internal enum class SaveState { IDLE, WORKING, SAVED, FAILED, UNKNOWN, CHECKING, CONFLICT }

internal data class ChronicleTaskDraft(
    val fields: List<TextFieldState>,
    val seats: List<String?>,
    val confirmation: ScoreConfirmation,
    val confirmedTexts: List<String>,
    val confirmedSeats: List<SeatScore>,
    val confirmedRuleId: String?,
    val editedPlayers: List<Boolean>,
    val pending: MatchRecord?,
    val saveState: SaveState,
    val showRegistration: Boolean,
)

@OptIn(ExperimentalFoundationApi::class)
class ChronicleState internal constructor(
    private val repository: ChronicleRepository,
    private val newId: () -> String,
    private val now: () -> Long,
    draft: ChronicleTaskDraft? = null,
) {
    internal val fields = draft?.fields ?: Seat.entries.map { TextFieldState() }
    internal var screen by mutableStateOf(Screen.LOADING)
        private set
    internal var players by mutableStateOf(emptyList<Player>())
        private set
    internal var seats by mutableStateOf(draft?.seats ?: List<String?>(Seat.entries.size) { null })
        private set
    internal var saveState by mutableStateOf(when (val saved = draft?.saveState) {
        SaveState.WORKING, SaveState.CHECKING -> SaveState.UNKNOWN
        null -> SaveState.IDLE
        else -> saved
    })
        private set
    internal var nameResult by mutableStateOf<NameResult?>(null)
        private set
    internal var playerSaveVersion by mutableIntStateOf(0)
        private set
    internal var writingPlayer by mutableStateOf(false)
        private set
    private var loadingPlayers = false
    private var resumeRegistration = draft?.showRegistration == true
    private var confirming by mutableStateOf(false)
    private var confirmation by mutableStateOf(draft?.confirmation ?: ScoreConfirmation())
    private var confirmedTexts by mutableStateOf(draft?.confirmedTexts ?: emptyList())
    private var confirmedSeats by mutableStateOf(draft?.confirmedSeats ?: emptyList())
    private var confirmedRuleId by mutableStateOf(draft?.confirmedRuleId)
    private var editedPlayers by mutableStateOf(draft?.editedPlayers ?: List(Seat.entries.size) { false })
    private var pending: MatchRecord? = draft?.pending

    internal fun taskDraft(): ChronicleTaskDraft = ChronicleTaskDraft(
        fields = fields,
        seats = seats,
        confirmation = if (confirming) ScoreConfirmation() else confirmation,
        confirmedTexts = confirmedTexts,
        confirmedSeats = confirmedSeats,
        confirmedRuleId = confirmedRuleId,
        editedPlayers = editedPlayers,
        pending = pending,
        saveState = saveState,
        showRegistration = screen == Screen.PLAYERS || resumeRegistration,
    )

    internal val canEdit: Boolean
        get() = !confirming && when (saveState) {
            SaveState.IDLE, SaveState.SAVED, SaveState.FAILED -> true
            else -> false
        }
    internal val visibleConfirmation: ScoreConfirmation
        get() {
            val visible = confirmation.afterEdits(
                fields.map { it.undoState.canUndo || it.undoState.canRedo },
                editedPlayers,
            )
            return visible.copy(rows = visible.rows.mapIndexed { i, row ->
                row.copy(name = players.firstOrNull { it.id == seats[i] }?.name ?: row.name)
            })
        }
    internal val canSave: Boolean
        get() = screen == Screen.SCORES && canEdit && confirmedRuleId != null &&
            !editedPlayers.any { it } && fields.withIndex().none { (index, field) ->
                field.undoState.canUndo || field.undoState.canRedo || field.text.toString() != confirmedTexts[index]
            }

    internal suspend fun loadPlayers() {
        if (loadingPlayers || screen !in setOf(Screen.LOADING, Screen.LOAD_FAILED)) return
        loadingPlayers = true
        screen = Screen.LOADING
        try {
            players = repository.players()
            screen = if (resumeRegistration || players.size < Seat.entries.size) Screen.PLAYERS else Screen.SCORES
            resumeRegistration = false
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            screen = Screen.LOAD_FAILED
        } finally {
            loadingPlayers = false
        }
    }

    internal fun showPlayers() {
        if (screen == Screen.SCORES && canEdit) {
            nameResult = null
            screen = Screen.PLAYERS
        }
    }

    internal fun showScores() {
        if (screen == Screen.PLAYERS && !writingPlayer && players.size >= Seat.entries.size) {
            screen = Screen.SCORES
        }
    }

    internal suspend fun writePlayer(id: String?, name: String) {
        if (screen != Screen.PLAYERS || writingPlayer) return
        writingPlayer = true
        try {
            val result = repository.writePlayer(id, name.trim())
            if (result == NameResult.SAVED) {
                try {
                    players = repository.players()
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    screen = Screen.LOAD_FAILED
                }
                playerSaveVersion++
            }
            nameResult = result
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            nameResult = NameResult.FAILED
        } finally {
            writingPlayer = false
        }
    }

    internal fun clearNameResult() {
        nameResult = null
    }

    internal fun select(seat: Int, player: String) {
        if (screen != Screen.SCORES || !canEdit || players.none { it.id == player }) return
        val updated = assignPlayer(seats, seat, player)
        editedPlayers = seats.indices.map { editedPlayers[it] || seats[it] != updated[it] }
        seats = updated
    }

    internal suspend fun confirm() {
        if (screen != Screen.SCORES || !canEdit) return
        confirming = true
        try {
            val scores = fields.map { it.text.toString() }
            val names = players.associate { it.id to it.name }
            val input = confirmScoreFields(scores, seats)
            confirmedRuleId = null
            confirmedSeats = emptyList()
            confirmedTexts = scores
            editedPlayers = List(Seat.entries.size) { false }
            fields.forEach { it.undoState.clearHistory() }
            if (input != null) {
                confirmation = input
                return
            }

            val rule = repository.currentRule()
            check(rule.calculation == M_LEAGUE_CALCULATION)
            val parsedScores = scores.map(String::toInt)
            val result = confirmValidScores(parsedScores, seats, names, rule.points)
            confirmation = result
            if (result.rows.isNotEmpty()) {
                confirmedRuleId = rule.id
                confirmedSeats = Seat.entries.mapIndexed { index, seat ->
                    SeatScore(seat, checkNotNull(seats[index]), parsedScores[index])
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            confirmation = ScoreConfirmation(calculationError = "計算ルールを読み込めませんでした。もう一度お試しください。")
        } finally {
            confirming = false
        }
    }

    internal suspend fun save() {
        if (!canSave) return
        val previous = pending
        val record = MatchRecord(
            id = previous?.id ?: newId(),
            savedAtMillis = previous?.savedAtMillis ?: now(),
            leagueId = INITIAL_LEAGUE_ID,
            ruleId = checkNotNull(confirmedRuleId),
            seats = confirmedSeats,
        )
        pending = record
        saveState = SaveState.WORKING
        applySaveResult(repository.save(record))
    }

    internal suspend fun reconcile() {
        if (saveState != SaveState.UNKNOWN) return
        saveState = SaveState.CHECKING
        applySaveResult(repository.reconcile(checkNotNull(pending)))
    }

    private fun applySaveResult(result: SaveResult) {
        saveState = when (result) {
            SaveResult.MISSING -> SaveState.FAILED
            SaveResult.UNKNOWN -> SaveState.UNKNOWN
            SaveResult.CONFLICT -> SaveState.CONFLICT
            SaveResult.SAVED -> {
                seats = List(Seat.entries.size) { null }
                editedPlayers = List(Seat.entries.size) { false }
                fields.forEach { field ->
                    field.edit { replace(0, length, "") }
                    field.undoState.clearHistory()
                }
                confirmation = ScoreConfirmation()
                confirmedTexts = emptyList()
                confirmedRuleId = null
                confirmedSeats = emptyList()
                pending = null
                SaveState.SAVED
            }
        }
    }
}
