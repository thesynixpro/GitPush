package com.aprax.gitpush.storage

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Local push history. Never leaves the device. */
@Entity(tableName = "push_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoFullName: String,
    val branch: String,
    val folderName: String,
    val fileCount: Int,
    val folderCount: Int,
    val totalBytes: Long,
    val timestampMs: Long,
    val status: String,      // SUCCESS / PARTIAL / FAILED / CANCELLED
    val commitSha: String?
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM push_history ORDER BY timestampMs DESC LIMIT 200")
    fun observe(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM push_history ORDER BY timestampMs DESC LIMIT 200")
    suspend fun list(): List<HistoryEntity>

    @Insert
    suspend fun insert(e: HistoryEntity): Long

    @Query("DELETE FROM push_history")
    suspend fun clear()

    @Query("DELETE FROM push_history WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var I: HistoryDatabase? = null
        fun get(ctx: Context): HistoryDatabase =
            I ?: synchronized(this) {
                I ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    HistoryDatabase::class.java,
                    "gitpush_history.db"
                ).fallbackToDestructiveMigration().build().also { I = it }
            }
    }
}
