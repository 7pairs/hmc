// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreConfirmationTest {
    private val scores = listOf("20100", "50000", "-1100", "31000")
    private val noEdits = listOf(false, false, false, false)
    private val firstEdited = listOf(true, false, false, false)

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
            confirmScores(listOf("", "-", "2147483648", "10000")),
        )
    }

    @Test
    fun `formats scores and points in player order even with a negative score`() {
        assertEquals(
            ScoreConfirmation(
                rows = listOf(
                    ScoreResultRow("選手A", "20,100", 3, "-19.9 pt"),
                    ScoreResultRow("選手B", "50,000", 1, "+70.0 pt"),
                    ScoreResultRow("選手C", "-1,100", 4, "-61.1 pt"),
                    ScoreResultRow("選手D", "31,000", 2, "+11.0 pt"),
                ),
            ),
            confirmScores(scores),
        )
    }

    @Test
    fun `shows matching places and points for players tied for first`() {
        val result = confirmScores(listOf("35000", "20000", "35000", "10000"))
        assertEquals(listOf(1, 3, 1, 4), result.rows.map { it.place })
        assertEquals(listOf("+35.0 pt", "-20.0 pt", "+35.0 pt", "-50.0 pt"), result.rows.map { it.points })
    }

    @Test
    fun `shows zero points without a sign`() {
        val result = confirmScores(listOf("60000", "20000", "15000", "5000"))
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
        assertEquals(listOf(null, "Bのエラー", null, null), result.afterEdits(firstEdited).fieldErrors)
    }

    @Test
    fun `hides the calculation error after an edit`() {
        val result = ScoreConfirmation(calculationError = "計算エラー")
        assertEquals(ScoreConfirmation(), result.afterEdits(firstEdited))
    }

    @Test
    fun `keeps confirmed rows only while all fields have no edit history`() {
        val result = ScoreConfirmation(rows = listOf(ScoreResultRow("選手A", "20,100", 3, "-19.9 pt")))
        assertFalse(isScoreInput("35,000"))
        assertEquals(result, result.afterEdits(noEdits))
        // The history still marks the field as edited when its text returns to the confirmed value.
        assertTrue(result.afterEdits(firstEdited).rows.isEmpty())
    }

    @Test
    fun `shows new places and points in the same player order when confirmed again`() {
        val changed = listOf(scores[1], scores[0], scores[2], scores[3])
        assertTrue(confirmScores(scores).afterEdits(firstEdited).rows.isEmpty())
        val result = confirmScores(changed).afterEdits(noEdits)
        assertEquals(listOf("選手A", "選手B", "選手C", "選手D"), result.rows.map { it.name })
        assertEquals(listOf(1, 3, 4, 2), result.rows.map { it.place })
        assertEquals(listOf("+70.0 pt", "-19.9 pt", "-61.1 pt", "+11.0 pt"), result.rows.map { it.points })
    }

    private fun assertCalculationError(inputs: List<String>, message: String) {
        assertEquals(ScoreConfirmation(calculationError = message), confirmScores(inputs))
    }
}
