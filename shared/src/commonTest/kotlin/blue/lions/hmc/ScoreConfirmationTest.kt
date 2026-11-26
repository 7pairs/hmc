// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreConfirmationTest {
    private fun confirmScores(inputs: List<String>, players: List<String?>): ScoreConfirmation =
        confirmScoreFields(inputs, players) ?: confirmValidScores(
            inputs.map(String::toInt),
            players,
            listOf("A", "B", "C", "D").associateWith { "${it}さん" },
            PlacementPoints(50, 10, -10, -30),
        )

    private val scores = listOf("20100", "50000", "-1100", "31000")
    private val players = listOf("A", "B", "C", "D")
    private val noEdits = listOf(false, false, false, false)
    private val firstEdited = listOf(true, false, false, false)

    @Test
    fun `swaps players and leaves the source empty when moving into an empty seat`() {
        val initial = listOf("A", "B", null, "D")
        val swapped = assignPlayer(initial, seat = 0, player = "B")
        assertEquals(listOf("B", "A", null, "D"), swapped)
        assertEquals(listOf(null, "A", "B", "D"), assignPlayer(swapped, seat = 2, player = "B"))
    }

    @Test
    fun `validates all player fields and score fields together`() {
        val result = confirmScores(
            inputs = listOf("", "-", "2147483648", "10000"),
            players = listOf(null, "B", "B", "D"),
        )
        assertEquals(
            listOf(
                "選手を選んでください。",
                "同じ選手が複数の席に割り当てられています。",
                "同じ選手が複数の席に割り当てられています。",
                null,
            ),
            result.playerErrors,
        )
        assertEquals(listOf(true, true, true, false), result.fieldErrors.map { it != null })
        assertTrue(result.rows.isEmpty())
        assertEquals(null, result.calculationError)
    }

    @Test
    fun `does not check the score total when a seat is unassigned`() {
        val result = confirmScores(
            inputs = listOf("50000", "30000", "20000", "-1000"),
            players = listOf(null, "B", "C", "D"),
        )
        assertEquals(listOf(true, false, false, false), result.playerErrors.map { it != null })
        assertEquals(null, result.calculationError)
        assertTrue(result.rows.isEmpty())
    }

    @Test
    fun `shows a newly empty seat error only after confirming again`() {
        val inputs = listOf("20100", "50000", "", "31000")
        val initial = listOf(null, "B", null, "D")
        val confirmed = confirmScores(inputs, initial)
        assertEquals(listOf(true, false, true, false), confirmed.playerErrors.map { it != null })

        val moved = assignPlayer(initial, seat = 0, player = "B")
        val visible = confirmed.afterEdits(noEdits, listOf(true, true, false, false))
        assertEquals(listOf(false, false, true, false), visible.playerErrors.map { it != null })
        assertEquals(listOf(false, false, true, false), visible.fieldErrors.map { it != null })

        val reconfirmed = confirmScores(inputs, moved)
        assertEquals(listOf(false, true, true, false), reconfirmed.playerErrors.map { it != null })
        assertEquals(visible.fieldErrors, reconfirmed.fieldErrors)
    }

    @Test
    fun `hides only errors in the player or score fields actually edited`() {
        val result = confirmScores(listOf("", "", "-1100", "31000"), listOf(null, null, "C", "D"))
        val visible = result.afterEdits(listOf(false, true, false, false), firstEdited)

        assertEquals(listOf(false, true, false, false), visible.playerErrors.map { it != null })
        assertEquals(listOf(true, false, false, false), visible.fieldErrors.map { it != null })
    }

    @Test
    fun `keeps the calculation error when only players change`() {
        val result = confirmScores(listOf("50000", "30000", "20000", "-1000"), players)
        val playersEdited = listOf(true, true, false, false)
        assertEquals(result.calculationError, result.afterEdits(noEdits, playersEdited).calculationError)
        assertEquals(null, result.afterEdits(firstEdited, playersEdited).calculationError)
    }

    @Test
    fun `uses the chosen names with each seat score after confirming a swap`() {
        val confirmed = confirmScores(scores, players)
        assertEquals(players, assignPlayer(players, seat = 0, player = "A"))
        assertEquals(confirmed, confirmed.afterEdits(noEdits, noEdits))

        val swapped = assignPlayer(players, seat = 0, player = "B")
        val playersEdited = listOf(true, true, false, false)
        assertTrue(confirmed.afterEdits(noEdits, playersEdited).rows.isEmpty())
        val reconfirmed = confirmScores(scores, swapped)
        assertEquals(
            listOf(confirmed.rows[0].copy(name = "Bさん"), confirmed.rows[1].copy(name = "Aさん")) +
                confirmed.rows.drop(2),
            reconfirmed.rows,
        )

        val changed = assignPlayer(swapped, seat = 0, player = "A")
        assertEquals(swapped, assignPlayer(changed, seat = 0, player = "B"))
        assertTrue(reconfirmed.afterEdits(noEdits, playersEdited).rows.isEmpty())
    }

    @Test
    fun `accepts partial signed integers and rejects edits with other characters`() {
        for (input in listOf("35000", "-1100", "", "-")) {
            assertTrue(isScoreInput(input), "Expected to accept '$input'")
        }
        for (input in listOf("35,000", "３５０００", "35.0", "+35000", "35-000", "--1100")) {
            assertFalse(isScoreInput(input), "Expected to reject '$input'")
        }
    }

    @Test
    fun `reports every field that is empty or cannot be read as an Int`() {
        assertEquals(
            ScoreConfirmation(
                fieldErrors = listOf(
                    "素点を入力してください。",
                    "素点を点単位の整数で入力してください。",
                    "素点を点単位の整数で入力してください。",
                    null,
                ),
            ),
            confirmScores(listOf("", "-", "2147483648", "10000"), players),
        )
    }

    @Test
    fun `formats scores and points in player order even with a negative score`() {
        assertEquals(
            ScoreConfirmation(
                rows = listOf(
                    ScoreResultRow("Aさん", "20,100", 3, "-19.9 pt"),
                    ScoreResultRow("Bさん", "50,000", 1, "+70.0 pt"),
                    ScoreResultRow("Cさん", "-1,100", 4, "-61.1 pt"),
                    ScoreResultRow("Dさん", "31,000", 2, "+11.0 pt"),
                ),
            ),
            confirmScores(scores, players),
        )
    }

    @Test
    fun `shows matching places and points for players tied for first`() {
        val result = confirmScores(listOf("35000", "20000", "35000", "10000"), players)
        assertEquals(listOf(1, 3, 1, 4), result.rows.map { it.place })
        assertEquals(listOf("+35.0 pt", "-20.0 pt", "+35.0 pt", "-50.0 pt"), result.rows.map { it.points })
    }

    @Test
    fun `shows zero points without a sign`() {
        val result = confirmScores(listOf("60000", "20000", "15000", "5000"), players)
        assertEquals("0.0 pt", result.rows.getOrNull(1)?.points)
    }

    @Test
    fun `shows the increment error for scores that are not multiples of 100`() {
        assertCalculationError(listOf("350", "50000", "19650", "30000"), "素点は100点刻みで入力してください。")
    }

    @Test
    fun `shows the expected total with a thousands separator`() {
        assertCalculationError(listOf("50000", "30000", "20000", "-1000"), "4人の素点の合計を100,000点にしてください。")
    }

    @Test
    fun `tells the user to record three way ties on paper`() {
        assertCalculationError(listOf("30000", "30000", "30000", "10000"), "3人以上が同点の半荘は紙に記録してください。")
    }

    @Test
    fun `hides only the field errors for edited fields`() {
        val result = ScoreConfirmation(fieldErrors = listOf("Aのエラー", "Bのエラー", null, null))
        assertEquals(listOf(null, "Bのエラー", null, null), result.afterEdits(firstEdited, noEdits).fieldErrors)
    }

    @Test
    fun `hides the calculation error after an edit`() {
        val result = ScoreConfirmation(calculationError = "計算エラー")
        assertEquals(ScoreConfirmation(), result.afterEdits(firstEdited, noEdits))
    }

    @Test
    fun `keeps confirmed rows only while all fields have no edit history`() {
        val result = ScoreConfirmation(rows = listOf(ScoreResultRow("Aさん", "20,100", 3, "-19.9 pt")))
        assertFalse(isScoreInput("35,000"))
        assertEquals(result, result.afterEdits(noEdits, noEdits))
        // The history still marks the field as edited when its text returns to the confirmed value.
        assertTrue(result.afterEdits(firstEdited, noEdits).rows.isEmpty())
    }

    @Test
    fun `shows new places and points in the same player order when confirmed again`() {
        val changed = listOf(scores[1], scores[0], scores[2], scores[3])
        assertTrue(confirmScores(scores, players).afterEdits(firstEdited, noEdits).rows.isEmpty())
        val result = confirmScores(changed, players).afterEdits(noEdits, noEdits)
        assertEquals(listOf("Aさん", "Bさん", "Cさん", "Dさん"), result.rows.map { it.name })
        assertEquals(listOf(1, 3, 4, 2), result.rows.map { it.place })
        assertEquals(listOf("+70.0 pt", "-19.9 pt", "-61.1 pt", "+11.0 pt"), result.rows.map { it.points })
    }

    private fun assertCalculationError(inputs: List<String>, message: String) {
        assertEquals(ScoreConfirmation(calculationError = message), confirmScores(inputs, players))
    }
}
