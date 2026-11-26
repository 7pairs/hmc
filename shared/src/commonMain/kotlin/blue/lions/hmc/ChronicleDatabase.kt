// SPDX-FileCopyrightText: 2026 HASEBA Junya (7pairs)
// SPDX-License-Identifier: Apache-2.0
// Source: https://github.com/7pairs/hmc

package blue.lions.hmc

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.time.Clock
import kotlin.uuid.Uuid

internal const val CHRONICLE_FILENAME = "chronicle.db"

@Entity(tableName = "players", indices = [Index(value = ["name"], unique = true)])
internal data class PlayerEntity(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "rules")
internal data class RuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val calculation: String,
    val first: Int,
    val second: Int,
    val third: Int,
    val fourth: Int,
)

@Entity(
    tableName = "leagues",
    foreignKeys = [ForeignKey(entity = RuleEntity::class, parentColumns = ["id"], childColumns = ["ruleId"])],
    indices = [Index("ruleId")],
)
internal data class LeagueEntity(@PrimaryKey val id: String, val name: String, val ruleId: String)

@Entity(
    tableName = "league_members",
    primaryKeys = ["leagueId", "playerId"],
    foreignKeys = [
        ForeignKey(entity = LeagueEntity::class, parentColumns = ["id"], childColumns = ["leagueId"]),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"]),
    ],
    indices = [Index("playerId")],
)
internal data class LeagueMemberEntity(val leagueId: String, val playerId: String)

@Entity(
    tableName = "matches",
    foreignKeys = [
        ForeignKey(entity = LeagueEntity::class, parentColumns = ["id"], childColumns = ["leagueId"]),
        ForeignKey(entity = RuleEntity::class, parentColumns = ["id"], childColumns = ["ruleId"]),
    ],
    indices = [Index("leagueId"), Index("ruleId")],
)
internal data class MatchEntity(
    @PrimaryKey val id: String,
    val savedAtMillis: Long,
    val leagueId: String,
    val ruleId: String,
)

@Entity(
    tableName = "match_seats",
    primaryKeys = ["matchId", "seat"],
    foreignKeys = [
        ForeignKey(entity = MatchEntity::class, parentColumns = ["id"], childColumns = ["matchId"]),
        ForeignKey(entity = PlayerEntity::class, parentColumns = ["id"], childColumns = ["playerId"]),
    ],
    indices = [Index("playerId"), Index(value = ["matchId", "playerId"], unique = true)],
)
internal data class SeatEntity(val matchId: String, val seat: String, val playerId: String, val score: Int)

@Dao
internal interface ChronicleDao {
    @Query("SELECT * FROM players ORDER BY rowid")
    suspend fun players(): List<PlayerEntity>

    @Query("SELECT id FROM players WHERE name = :name")
    suspend fun playerIdByName(name: String): String?

    @Insert
    suspend fun insertPlayer(player: PlayerEntity)

    @Insert
    suspend fun insertMember(member: LeagueMemberEntity)

    @Query("UPDATE players SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String): Int

    @Query("SELECT rules.* FROM rules JOIN leagues ON leagues.ruleId = rules.id WHERE leagues.id = :leagueId")
    suspend fun currentRule(leagueId: String): RuleEntity

    @Query("SELECT playerId FROM league_members WHERE leagueId = :leagueId AND playerId IN (:ids)")
    suspend fun members(leagueId: String, ids: List<String>): List<String>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun match(id: String): MatchEntity?

    @Query("SELECT * FROM match_seats WHERE matchId = :id")
    suspend fun seats(id: String): List<SeatEntity>

    @Insert
    suspend fun insertMatch(match: MatchEntity)

    @Insert
    suspend fun insertSeats(seats: List<SeatEntity>)
}

@Database(
    entities = [
        PlayerEntity::class, RuleEntity::class, LeagueEntity::class,
        LeagueMemberEntity::class, MatchEntity::class, SeatEntity::class,
    ],
    version = 1,
)
@ConstructedBy(ChronicleDatabaseConstructor::class)
internal abstract class ChronicleDatabase : RoomDatabase() {
    abstract fun chronicle(): ChronicleDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect object ChronicleDatabaseConstructor : RoomDatabaseConstructor<ChronicleDatabase> {
    override fun initialize(): ChronicleDatabase
}

internal fun openChronicle(builder: RoomDatabase.Builder<ChronicleDatabase>): ChronicleDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(connection: SQLiteConnection) {
                connection.execSQL(
                    "INSERT INTO rules (id, name, calculation, first, second, third, fourth) " +
                        "VALUES ('$INITIAL_RULE_ID', 'Mリーグ', '$M_LEAGUE_CALCULATION', 50, 10, -10, -30)",
                )
                connection.execSQL(
                    "INSERT INTO leagues (id, name, ruleId) VALUES ('$INITIAL_LEAGUE_ID', '初期リーグ', '$INITIAL_RULE_ID')",
                )
            }
        })
        .build()

class AppContainer internal constructor(database: ChronicleDatabase) {
    internal val repository: ChronicleRepository = RoomChronicleRepository(database)

    fun createState(): ChronicleState = createState(draft = null)

    internal fun createState(draft: ChronicleTaskDraft?): ChronicleState =
        ChronicleState(repository, { Uuid.random().toString() }, { Clock.System.now().toEpochMilliseconds() }, draft)
}
