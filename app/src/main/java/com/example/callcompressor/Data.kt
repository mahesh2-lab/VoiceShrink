package com.example.callcompressor

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

object Status {
    const val PENDING = "PENDING"
    const val DONE = "DONE"
    const val FAILED = "FAILED"
    const val DUPLICATE = "DUPLICATE"
}

@Entity(
    tableName = "recordings",
    indices = [
        Index("sha256"),
        Index(value = ["uri"], unique = true),
        Index("status")
    ]
)
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sha256: String,
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val status: String,
    val outSizeBytes: Long = 0,
    val outUri: String? = null,
    val error: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val durationMs: Long = 0L,
)

data class StatusCount(val status: String, val n: Int)

@Dao
interface RecordingDao {
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insert(r: Recording): Long

    @Query("SELECT * FROM recordings WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): Recording?

    // Content-based duplicate check (checks if a DIFFERENT recording file has this exact hash)
    @Query("SELECT COUNT(*) FROM recordings WHERE sha256 = :sha AND uri != :uri AND status != 'DUPLICATE' AND status != 'FAILED'")
    suspend fun hasOtherWithHash(sha: String, uri: String): Int

    @Query("UPDATE recordings SET sha256 = :sha, sizeBytes = :size, lastModified = :lm, status = :status WHERE id = :id")
    suspend fun updateFile(id: Long, sha: String, size: Long, lm: Long, status: String)

    @Query("UPDATE recordings SET durationMs = :durMs WHERE id = :id")
    suspend fun updateDuration(id: Long, durMs: Long)

    @Query("SELECT * FROM recordings WHERE status != 'DUPLICATE' ORDER BY lastModified DESC")
    fun getAll(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE status = 'PENDING' ORDER BY id LIMIT 20")
    suspend fun nextPending(): List<Recording>

    @Query("SELECT COUNT(*) FROM recordings WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM recordings WHERE status = 'FAILED'")
    suspend fun failedCount(): Int

    @Query("UPDATE recordings SET status = 'DONE', outSizeBytes = :out, outUri = :outUri, error = NULL WHERE id = :id")
    suspend fun markDone(id: Long, out: Long, outUri: String?)

    @Query("UPDATE recordings SET status = 'FAILED', error = :err WHERE id = :id")
    suspend fun markFailed(id: Long, err: String?)

    @Query("UPDATE recordings SET status = 'PENDING', error = NULL WHERE status = 'FAILED'")
    suspend fun retryFailed(): Int

    @Query("SELECT status, COUNT(*) AS n FROM recordings GROUP BY status")
    fun counts(): Flow<List<StatusCount>>

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM recordings WHERE status = 'DUPLICATE'")
    suspend fun getDuplicates(): List<Recording>

    @Query("DELETE FROM recordings WHERE status = 'DUPLICATE'")
    suspend fun deleteDuplicates(): Int

    @Query("DELETE FROM recordings")
    suspend fun clearAll()

    // Clean up corrupted duplicate rows (keeps one row per uri and restores false duplicates)
    @Query("DELETE FROM recordings WHERE id NOT IN (SELECT MIN(id) FROM recordings GROUP BY uri)")
    suspend fun removeDuplicateUris()

    @Query("UPDATE recordings SET status = 'PENDING' WHERE status = 'DUPLICATE'")
    suspend fun resetAllDuplicatesToPending()

    @Transaction
    suspend fun cleanCorruptedDuplicates() {
        removeDuplicateUris()
        resetAllDuplicatesToPending()
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE recordings ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [Recording::class], version = 4, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): RecordingDao

    companion object {
        @Volatile private var inst: AppDb? = null

        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "calls.db")
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                .also { inst = it }
        }
    }
}
