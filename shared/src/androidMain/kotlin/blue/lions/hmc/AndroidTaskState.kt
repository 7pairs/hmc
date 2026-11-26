// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

@file:Suppress("DEPRECATION") // Use the Bundle getters available on Android API 24.

package blue.lions.hmc

import android.os.Bundle
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.saveable.SaverScope

fun AppContainer.restoreAndroidTaskState(state: Bundle?): ChronicleState = createState(state?.toChronicleTaskDraft())

fun ChronicleState.saveAndroidTaskState(): Bundle {
    val draft = taskDraft()
    return Bundle().apply {
        val scope = SaverScope { true }
        val savedFields = draft.fields.map { field ->
            with(TextFieldState.Saver) { checkNotNull(scope.save(field)) }
        }
        putSerializable("fields", ArrayList(savedFields))
        putStringArrayList("seats", ArrayList(draft.seats))
        putBundle("confirmation", draft.confirmation.toBundle())
        putStringArrayList("confirmedTexts", ArrayList(draft.confirmedTexts))
        putParcelableArrayList("confirmedSeats", ArrayList(draft.confirmedSeats.map(SeatScore::toBundle)))
        putString("confirmedRuleId", draft.confirmedRuleId)
        putBooleanArray("editedPlayers", draft.editedPlayers.toBooleanArray())
        putBundle("pending", draft.pending?.toBundle())
        putString("saveState", draft.saveState.name)
        putBoolean("showRegistration", draft.showRegistration)
    }
}

internal fun Bundle.toChronicleTaskDraft(): ChronicleTaskDraft {
    val savedFields = getSerializable("fields") as List<*>
    return ChronicleTaskDraft(
        fields = savedFields.map { checkNotNull(TextFieldState.Saver.restore(checkNotNull(it))) },
        seats = checkNotNull(getStringArrayList("seats")),
        confirmation = checkNotNull(getBundle("confirmation")).toConfirmation(),
        confirmedTexts = checkNotNull(getStringArrayList("confirmedTexts")),
        confirmedSeats = seatScores("confirmedSeats"),
        confirmedRuleId = getString("confirmedRuleId"),
        editedPlayers = checkNotNull(getBooleanArray("editedPlayers")).toList(),
        pending = getBundle("pending")?.toMatch(),
        saveState = SaveState.valueOf(checkNotNull(getString("saveState"))),
        showRegistration = getBoolean("showRegistration"),
    )
}

private fun ScoreConfirmation.toBundle(): Bundle = Bundle().apply {
    putStringArrayList("fieldErrors", ArrayList(fieldErrors))
    putString("calculationError", calculationError)
    putStringArrayList("playerErrors", ArrayList(playerErrors))
    putParcelableArrayList("rows", ArrayList(rows.map { row ->
        Bundle().apply {
            putString("name", row.name)
            putString("score", row.score)
            putInt("place", row.place)
            putString("points", row.points)
        }
    }))
}

private fun Bundle.toConfirmation() = ScoreConfirmation(
    fieldErrors = checkNotNull(getStringArrayList("fieldErrors")),
    calculationError = getString("calculationError"),
    rows = checkNotNull(getParcelableArrayList<Bundle>("rows")).map {
        ScoreResultRow(
            name = checkNotNull(it.getString("name")),
            score = checkNotNull(it.getString("score")),
            place = it.getInt("place"),
            points = checkNotNull(it.getString("points")),
        )
    },
    playerErrors = checkNotNull(getStringArrayList("playerErrors")),
)

private fun MatchRecord.toBundle(): Bundle = Bundle().apply {
    putString("id", id)
    putLong("savedAtMillis", savedAtMillis)
    putString("leagueId", leagueId)
    putString("ruleId", ruleId)
    putParcelableArrayList("seats", ArrayList(seats.map(SeatScore::toBundle)))
}

private fun Bundle.toMatch() = MatchRecord(
    id = checkNotNull(getString("id")),
    savedAtMillis = getLong("savedAtMillis"),
    leagueId = checkNotNull(getString("leagueId")),
    ruleId = checkNotNull(getString("ruleId")),
    seats = seatScores("seats"),
)

private fun SeatScore.toBundle(): Bundle = Bundle().apply {
    putString("seat", seat.name)
    putString("playerId", playerId)
    putInt("score", score)
}

private fun Bundle.seatScores(key: String): List<SeatScore> =
    checkNotNull(getParcelableArrayList<Bundle>(key)).map {
        SeatScore(
            seat = Seat.valueOf(checkNotNull(it.getString("seat"))),
            playerId = checkNotNull(it.getString("playerId")),
            score = it.getInt("score"),
        )
    }
