// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import kotlin.test.Test
import kotlin.test.assertEquals

class MatchResultsTest {
    private val mLeaguePoints = PlacementPoints(50, 10, -10, -30)

    @Test
    fun `when one score is negative, all results stay in input order`() {
        assertResults(
            RawScores(20_100, 50_000, -1_100, 31_000),
            listOf(PlayerResult(3, -199), PlayerResult(1, 700), PlayerResult(4, -611), PlayerResult(2, 110)),
        )
    }

    @Test
    fun `when two players tie for first, they split first and second place points`() {
        assertResults(
            RawScores(35_000, 20_000, 35_000, 10_000),
            listOf(PlayerResult(1, 350), PlayerResult(3, -200), PlayerResult(1, 350), PlayerResult(4, -500)),
        )
    }

    @Test
    fun `when two players tie for second, they split second and third place points`() {
        assertResults(
            RawScores(40_000, 25_000, 25_000, 10_000),
            listOf(PlayerResult(1, 600), PlayerResult(2, -50), PlayerResult(2, -50), PlayerResult(4, -500)),
        )
    }

    @Test
    fun `when two pairs tie, their places are first and third`() {
        assertResults(
            RawScores(35_000, 15_000, 35_000, 15_000),
            listOf(PlayerResult(1, 350), PlayerResult(3, -350), PlayerResult(1, 350), PlayerResult(3, -350)),
        )
    }

    @Test
    fun `when tied players split an odd number of placement points, they keep the half point`() {
        assertResults(
            RawScores(35_000, 20_000, 35_000, 10_000),
            listOf(PlayerResult(1, 355), PlayerResult(3, -210), PlayerResult(1, 355), PlayerResult(4, -500)),
            PlacementPoints(51, 10, -11, -30),
        )
    }

    @Test
    fun `when the sum of tied placement points exceeds the Int range, the split stays exact`() {
        assertResults(
            RawScores(35_000, 20_000, 35_000, 10_000),
            listOf(
                PlayerResult(1, 21_474_836_515),
                PlayerResult(3, -21_474_836_570),
                PlayerResult(1, 21_474_836_515),
                PlayerResult(4, -21_474_836_660),
            ),
            PlacementPoints(Int.MAX_VALUE, Int.MAX_VALUE - 1, -Int.MAX_VALUE, -(Int.MAX_VALUE - 1)),
        )
    }

    @Test
    fun `when a score is not a multiple of 100, a valid total still yields an increment error`() {
        assertFailure(RawScores(50_050, 30_950, 20_000, -1_000), MatchFailureReason.INVALID_SCORE_INCREMENT)
    }

    @Test
    fun `when scores do not add up to 100000, the result is a total error`() {
        assertFailure(RawScores(50_000, 30_000, 20_000, -1_000), MatchFailureReason.INVALID_SCORE_TOTAL)
    }

    @Test
    fun `when three players tie, the result is an unsupported tie error`() {
        assertFailure(RawScores(30_000, 30_000, 30_000, 10_000), MatchFailureReason.UNSUPPORTED_TIE)
    }

    @Test
    fun `when all four players tie, the result is an unsupported tie error`() {
        assertFailure(RawScores(25_000, 25_000, 25_000, 25_000), MatchFailureReason.UNSUPPORTED_TIE)
    }

    @Test
    fun `when both the increment and total are invalid, the increment error takes precedence`() {
        assertFailure(RawScores(50_050, 30_000, 20_000, -1_000), MatchFailureReason.INVALID_SCORE_INCREMENT)
    }

    @Test
    fun `when three players tie and the total is invalid, the total error takes precedence`() {
        assertFailure(RawScores(30_000, 30_000, 30_000, 9_000), MatchFailureReason.INVALID_SCORE_TOTAL)
    }

    private fun assertResults(
        scores: RawScores,
        expected: List<PlayerResult>,
        placementPoints: PlacementPoints = mLeaguePoints,
    ) {
        assertEquals<MatchResult>(MatchResult.Success(expected), calculateMatchResults(scores, placementPoints))
    }

    private fun assertFailure(scores: RawScores, reason: MatchFailureReason) {
        assertEquals<MatchResult>(MatchResult.Failure(reason), calculateMatchResults(scores, mLeaguePoints))
    }
}
