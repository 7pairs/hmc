// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

private const val SCORE_INCREMENT = 100L
private const val EXPECTED_SCORE_TOTAL = 100_000L
private const val RETURN_SCORE = 30_000L
private const val SCORE_POINTS_PER_TENTH = 100L
private const val TENTHS_PER_POINT = 10L

internal data class RawScores(val first: Int, val second: Int, val third: Int, val fourth: Int)

internal data class PlacementPoints(val first: Int, val second: Int, val third: Int, val fourth: Int)

internal data class PlayerResult(val place: Int, val pointsInTenths: Long)

internal sealed interface MatchResult {
    data class Success(val players: List<PlayerResult>) : MatchResult
    data class Failure(val reason: MatchFailureReason) : MatchResult
}

internal enum class MatchFailureReason {
    INVALID_SCORE_INCREMENT,
    INVALID_SCORE_TOTAL,
    UNSUPPORTED_TIE,
}

internal fun calculateMatchResults(scores: RawScores, placementPoints: PlacementPoints): MatchResult {
    val scoresInInputOrder = listOf(scores.first, scores.second, scores.third, scores.fourth).map(Int::toLong)
    if (scoresInInputOrder.any { it % SCORE_INCREMENT != 0L }) {
        return MatchResult.Failure(MatchFailureReason.INVALID_SCORE_INCREMENT)
    }
    if (scoresInInputOrder.sum() != EXPECTED_SCORE_TOTAL) {
        return MatchResult.Failure(MatchFailureReason.INVALID_SCORE_TOTAL)
    }

    val playersByScore = scoresInInputOrder.withIndex()
        .sortedByDescending { it.value }
        .groupBy { it.value }
    if (playersByScore.values.any { it.size > 2 }) {
        return MatchResult.Failure(MatchFailureReason.UNSUPPORTED_TIE)
    }

    val pointsByPlace = listOf(placementPoints.first, placementPoints.second, placementPoints.third, placementPoints.fourth)
        .map(Int::toLong)
    val rankedResults = mutableListOf<IndexedValue<PlayerResult>>()
    var placeIndex = 0
    for ((score, players) in playersByScore) {
        val placementPointsInTenths = if (players.size == 2) {
            (pointsByPlace[placeIndex] + pointsByPlace[placeIndex + 1]) * (TENTHS_PER_POINT / 2)
        } else {
            pointsByPlace[placeIndex] * TENTHS_PER_POINT
        }
        val pointsInTenths = (score - RETURN_SCORE) / SCORE_POINTS_PER_TENTH + placementPointsInTenths
        val result = PlayerResult(place = placeIndex + 1, pointsInTenths = pointsInTenths)
        for (player in players) {
            rankedResults.add(IndexedValue(player.index, result))
        }
        placeIndex += players.size
    }

    return MatchResult.Success(rankedResults.sortedBy { it.index }.map { it.value })
}
