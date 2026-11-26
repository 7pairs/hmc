// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

internal const val INITIAL_LEAGUE_ID = "initial"
internal const val INITIAL_RULE_ID = "m-league-v1"
internal const val M_LEAGUE_CALCULATION = "m-league-v1"

internal data class Player(val id: String, val name: String)
internal data class MatchRule(val id: String, val calculation: String, val points: PlacementPoints)
internal enum class Seat { EAST, SOUTH, WEST, NORTH }
internal data class SeatScore(val seat: Seat, val playerId: String, val score: Int)
internal data class MatchRecord(
    val id: String,
    val savedAtMillis: Long,
    val leagueId: String,
    val ruleId: String,
    val seats: List<SeatScore>,
)

internal enum class NameResult { SAVED, EMPTY, DUPLICATE, FAILED }
internal enum class SaveResult { SAVED, MISSING, UNKNOWN, CONFLICT }

internal interface ChronicleRepository {
    suspend fun players(): List<Player>
    suspend fun writePlayer(id: String?, name: String): NameResult
    suspend fun currentRule(): MatchRule
    suspend fun save(record: MatchRecord): SaveResult
    suspend fun reconcile(record: MatchRecord): SaveResult
}
