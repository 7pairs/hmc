// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.room.deferredTransaction
import androidx.room.immediateTransaction
import androidx.room.useReaderConnection
import androidx.room.useWriterConnection
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

internal class RoomChronicleRepository(private val database: ChronicleDatabase) : ChronicleRepository {
    private val dao = database.chronicle()

    override suspend fun players(): List<Player> = dao.players().map { Player(it.id, it.name) }

    override suspend fun writePlayer(id: String?, name: String): NameResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return NameResult.EMPTY
        return try {
            database.useWriterConnection { connection ->
                connection.immediateTransaction {
                    val existingId = dao.playerIdByName(trimmed)
                    if (existingId != null && existingId != id) return@immediateTransaction NameResult.DUPLICATE
                    if (id == null) {
                        val newId = Uuid.random().toString()
                        dao.insertPlayer(PlayerEntity(newId, trimmed))
                        dao.insertMember(LeagueMemberEntity(INITIAL_LEAGUE_ID, newId))
                    } else if (dao.rename(id, trimmed) != 1) {
                        return@immediateTransaction NameResult.FAILED
                    }
                    NameResult.SAVED
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            NameResult.FAILED
        }
    }

    override suspend fun currentRule(): MatchRule = dao.currentRule(INITIAL_LEAGUE_ID).let {
        MatchRule(it.id, it.calculation, PlacementPoints(it.first, it.second, it.third, it.fourth))
    }

    override suspend fun save(record: MatchRecord): SaveResult = try {
        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                val existing = compareRecord(record)
                if (existing != SaveResult.MISSING) return@immediateTransaction existing
                val players = record.seats.map { it.playerId }
                if (record.leagueId != INITIAL_LEAGUE_ID ||
                    record.seats.size != Seat.entries.size ||
                    record.seats.map { it.seat }.toSet() != Seat.entries.toSet() ||
                    players.distinct().size != Seat.entries.size ||
                    dao.members(record.leagueId, players).size != Seat.entries.size
                ) {
                    return@immediateTransaction SaveResult.MISSING
                }
                dao.insertMatch(record.matchEntity())
                dao.insertSeats(record.seatEntities())
                SaveResult.SAVED
            }
        }
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        // The writer transaction has finished. Check what actually reached durable storage.
        reconcile(record)
    }

    override suspend fun reconcile(record: MatchRecord): SaveResult = try {
        database.useReaderConnection { connection ->
            connection.deferredTransaction { compareRecord(record) }
        }
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        SaveResult.UNKNOWN
    }

    private suspend fun compareRecord(record: MatchRecord): SaveResult {
        val existing = dao.match(record.id) ?: return SaveResult.MISSING
        return if (existing == record.matchEntity() &&
            dao.seats(record.id).sortedBy { it.seat } == record.seatEntities().sortedBy { it.seat }
        ) {
            SaveResult.SAVED
        } else {
            SaveResult.CONFLICT
        }
    }

    private fun MatchRecord.matchEntity() = MatchEntity(id, savedAtMillis, leagueId, ruleId)

    private fun MatchRecord.seatEntities() = seats.map { SeatEntity(id, it.seat.name, it.playerId, it.score) }
}
