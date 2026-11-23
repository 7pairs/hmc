// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import kotlin.math.abs

internal val scoreSeatNames = listOf("東", "南", "西", "北")
internal val scorePlayerNames = mapOf("A" to "Aさん", "B" to "Bさん", "C" to "Cさん", "D" to "Dさん")

private val scoreInputPattern = Regex("-?[0-9]*")
private val mLeaguePoints = PlacementPoints(50, 10, -10, -30)

internal data class ScoreResultRow(
    val name: String,
    val score: String,
    val place: Int,
    val points: String,
)

internal data class ScoreConfirmation(
    val fieldErrors: List<String?> = List(scoreSeatNames.size) { null },
    val calculationError: String? = null,
    val rows: List<ScoreResultRow> = emptyList(),
    val playerErrors: List<String?> = List(scoreSeatNames.size) { null },
) {
    fun afterEdits(
        editedScores: List<Boolean>,
        editedPlayers: List<Boolean>,
    ): ScoreConfirmation {
        val scoresChanged = editedScores.any { it }
        val playersChanged = editedPlayers.any { it }
        if (!scoresChanged && !playersChanged) return this

        return ScoreConfirmation(
            fieldErrors = fieldErrors.mapIndexed { index, error -> error.takeUnless { editedScores[index] } },
            calculationError = calculationError.takeUnless { scoresChanged },
            playerErrors = playerErrors.mapIndexed { index, error -> error.takeUnless { editedPlayers[index] } },
        )
    }
}

internal fun isScoreInput(text: CharSequence): Boolean = scoreInputPattern.matches(text)

internal fun assignPlayer(players: List<String?>, seat: Int, player: String): List<String?> {
    if (players[seat] == player) return players
    val previousSeat = players.indexOf(player)
    return players.toMutableList().apply {
        if (previousSeat >= 0) this[previousSeat] = players[seat]
        this[seat] = player
    }
}

internal fun confirmScores(inputs: List<String>, players: List<String?>): ScoreConfirmation {
    val playerCounts = players.filterNotNull().groupingBy { it }.eachCount()
    val playerErrors = players.map { player ->
        when {
            player == null -> "選手を選んでください。"
            playerCounts.getValue(player) > 1 -> "同じ選手が複数の席に割り当てられています。"
            else -> null
        }
    }
    val parsedScores = inputs.map(String::toIntOrNull)
    val fieldErrors = inputs.mapIndexed { index, input ->
        when {
            input.isEmpty() -> "素点を入力してください。"
            parsedScores[index] == null -> "素点を点単位の整数で入力してください。"
            else -> null
        }
    }
    if (playerErrors.any { it != null } || fieldErrors.any { it != null }) {
        return ScoreConfirmation(fieldErrors = fieldErrors, playerErrors = playerErrors)
    }

    val scores = parsedScores.filterNotNull()
    val rawScores = RawScores(scores[0], scores[1], scores[2], scores[3])
    return when (val result = calculateMatchResults(rawScores, mLeaguePoints)) {
        is MatchResult.Failure -> ScoreConfirmation(calculationError = result.reason.message())
        is MatchResult.Success -> ScoreConfirmation(rows = result.players.mapIndexed { index, player ->
            ScoreResultRow(
                name = scorePlayerNames.getValue(checkNotNull(players[index])),
                score = formatScore(scores[index].toLong()),
                place = player.place,
                points = formatPoints(player.pointsInTenths),
            )
        })
    }
}

private fun MatchFailureReason.message(): String = when (this) {
    MatchFailureReason.INVALID_SCORE_INCREMENT -> "素点は100点刻みで入力してください。"
    MatchFailureReason.INVALID_SCORE_TOTAL -> "4人の素点の合計を${formatScore(EXPECTED_SCORE_TOTAL)}点にしてください。"
    MatchFailureReason.UNSUPPORTED_TIE -> "3人以上が同点の半荘は紙に記録してください。"
}

private fun formatScore(score: Long): String {
    val sign = if (score < 0) "-" else ""
    val digits = score.toString().removePrefix("-")
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return sign + grouped
}

private fun formatPoints(tenths: Long): String {
    val sign = when {
        tenths > 0 -> "+"
        tenths < 0 -> "-"
        else -> ""
    }
    return "$sign${abs(tenths / 10)}.${abs(tenths % 10)} pt"
}
