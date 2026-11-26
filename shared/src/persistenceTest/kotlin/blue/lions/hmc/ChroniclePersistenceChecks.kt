// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ChroniclePersistenceChecks(private val open: () -> ChronicleDatabase) {
    suspend fun roundTrip() {
        val expected = withDatabase { db, repository ->
            assertEquals(INITIAL_RULE_ID, repository.currentRule().id)
            assertEquals(1, db.count("rules"))
            assertEquals(1, db.count("leagues"))
            db.execute("CREATE TRIGGER reject_member BEFORE INSERT ON league_members BEGIN SELECT RAISE(ABORT, 'test'); END")
            assertEquals(NameResult.FAILED, repository.writePlayer(null, "Rollback"))
            assertEquals(emptyList(), repository.players())
            db.execute("DROP TRIGGER reject_member")

            val roster = registerPlayers(repository)
            assertEquals(NameResult.DUPLICATE, repository.writePlayer(null, " A "))
            assertEquals(NameResult.DUPLICATE, repository.writePlayer(roster.first().id, "B"))
            db.execute("INSERT INTO rules (id, name, calculation, first, second, third, fourth) VALUES ('A', 'A', '$M_LEAGUE_CALCULATION', 60, 10, -10, -60)")
            db.execute("UPDATE leagues SET ruleId = 'A' WHERE id = '$INITIAL_LEAGUE_ID'")

            val state = ChronicleState(repository, { "first" }, { 123L })
            state.loadPlayers()
            val scores = listOf(20100, 50000, -1100, 31000)
            scores.forEachIndexed { index, score ->
                state.select(index, roster[index].id)
                state.fields[index].edit { append(score.toString()) }
            }
            state.confirm()
            assertEquals("+80.0 pt", state.visibleConfirmation.rows[1].points)
            db.execute("UPDATE leagues SET ruleId = '$INITIAL_RULE_ID' WHERE id = '$INITIAL_LEAGUE_ID'")
            state.save()
            assertEquals(SaveState.SAVED, state.saveState)
            val first = match("first", roster, ruleId = "A")
            val second = match("second", roster.drop(1).reversed())
            assertEquals(SaveResult.SAVED, repository.save(second))
            assertEquals(NameResult.SAVED, repository.writePlayer(roster.first().id, "A changed"))
            listOf(first, second)
        }

        withDatabase { db, repository ->
            assertEquals(5, db.count("players"))
            assertEquals(5, db.count("league_members"))
            assertEquals(5, db.chronicle().members(INITIAL_LEAGUE_ID, repository.players().map { it.id }).size)
            assertEquals("A changed", repository.players().first().name)
            assertEquals(2, db.count("matches"))
            assertEquals(8, db.count("match_seats"))
            expected.forEach { assertEquals(SaveResult.SAVED, repository.reconcile(it)) }
            assertEquals("A", assertNotNull(db.chronicle().match("first")).ruleId)
            val points = db.useReaderConnection { connection ->
                connection.usePrepared("SELECT first, second, third, fourth FROM rules WHERE id = 'A'") {
                    check(it.step())
                    PlacementPoints(it.getInt(0), it.getInt(1), it.getInt(2), it.getInt(3))
                }
            }
            val savedSeats = db.chronicle().seats("first").associateBy { it.seat }
            val seats = Seat.entries.map { savedSeats.getValue(it.name) }
            val names = repository.players().associate { it.id to it.name }
            val recalculated = confirmValidScores(seats.map { it.score }, seats.map { it.playerId }, names, points)
            assertEquals("+80.0 pt", recalculated.rows[1].points)
        }
    }

    suspend fun atomicityAndParallelSaves() {
        val saved = withDatabase { db, repository ->
            val roster = registerPlayers(repository)
            val original = match("first", roster)
            assertEquals(SaveResult.SAVED, repository.save(original))
            assertEquals(SaveResult.SAVED, repository.save(original.copy(id = "identical-input")))
            db.execute("CREATE TRIGGER reject_north BEFORE INSERT ON match_seats WHEN NEW.seat = 'NORTH' BEGIN SELECT RAISE(ABORT, 'test'); END")
            assertEquals(SaveResult.MISSING, repository.save(original.copy(id = "interrupted")))
            assertNull(db.chronicle().match("interrupted"))
            assertEquals(0, db.chronicle().seats("interrupted").size)
            db.execute("DROP TRIGGER reject_north")

            db.execute("INSERT INTO leagues (id, name, ruleId) VALUES ('other', 'Other', '$INITIAL_RULE_ID')")
            db.execute("INSERT INTO players (id, name) VALUES ('unaffiliated', 'Unaffiliated')")
            val invalid = original.copy(id = "invalid")
            val seats = invalid.seats
            val invalidRecords = listOf(
                invalid.copy(leagueId = "other"),
                invalid.copy(seats = seats.dropLast(1) + seats.last().copy(playerId = "unaffiliated")),
                invalid.copy(seats = seats.dropLast(1)),
                invalid.copy(seats = seats.dropLast(1) + seats.last().copy(seat = Seat.EAST)),
                invalid.copy(seats = seats.dropLast(1) + seats.last().copy(playerId = seats.first().playerId)),
            )
            invalidRecords.forEach { assertEquals(SaveResult.MISSING, repository.save(it)) }

            val same = original.copy(id = "parallel-same")
            val sameResults = coroutineScope {
                val left = async { repository.save(same) }
                val right = async { repository.save(same) }
                listOf(left.await(), right.await())
            }
            assertEquals(listOf(SaveResult.SAVED, SaveResult.SAVED), sameResults)
            val left = original.copy(id = "parallel-different")
            val right = left.copy(seats = left.seats.reversed().mapIndexed { index, seat -> seat.copy(seat = Seat.entries[index]) })
            val results = coroutineScope {
                val a = async { repository.save(left) }
                val b = async { repository.save(right) }
                listOf(a.await(), b.await())
            }
            assertEquals(setOf(SaveResult.SAVED, SaveResult.CONFLICT), results.toSet())
            val winner = if (results.first() == SaveResult.SAVED) left else right
            val loser = if (winner == left) right else left
            assertEquals(SaveResult.CONFLICT, repository.save(loser))
            listOf(original, same, winner)
        }

        withDatabase { db, repository ->
            saved.forEach { assertEquals(SaveResult.SAVED, repository.reconcile(it)) }
            assertEquals(4, db.count("matches"))
            assertEquals(16, db.count("match_seats"))
            assertNull(db.chronicle().match("interrupted"))
            assertNull(db.chronicle().match("invalid"))
        }
    }

    private suspend fun registerPlayers(repository: ChronicleRepository): List<Player> {
        listOf("A", "B", "C", "D", "E").forEach { assertEquals(NameResult.SAVED, repository.writePlayer(null, it)) }
        return repository.players()
    }

    private fun match(id: String, roster: List<Player>, ruleId: String = INITIAL_RULE_ID): MatchRecord {
        val scores = listOf(20100, 50000, -1100, 31000)
        return MatchRecord(
            id, 123L, INITIAL_LEAGUE_ID, ruleId,
            Seat.entries.mapIndexed { index, seat -> SeatScore(seat, roster[index].id, scores[index]) },
        )
    }

    private suspend fun <T> withDatabase(block: suspend (ChronicleDatabase, RoomChronicleRepository) -> T): T {
        val db = open()
        return try {
            block(db, RoomChronicleRepository(db))
        } finally {
            db.close()
        }
    }

    private suspend fun ChronicleDatabase.execute(sql: String) = useWriterConnection { connection ->
        connection.usePrepared(sql) { it.step() }
    }

    private suspend fun ChronicleDatabase.count(table: String): Int = intQuery("SELECT count(*) FROM $table")

    private suspend fun ChronicleDatabase.intQuery(sql: String): Int = useReaderConnection { connection ->
        connection.usePrepared(sql) {
            check(it.step())
            it.getInt(0)
        }
    }
}
