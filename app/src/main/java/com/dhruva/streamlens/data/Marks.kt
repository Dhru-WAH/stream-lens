package com.dhruva.streamlens.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/** What I did with a title. Lives only on the phone; Settings › Backup copies it out. */
@Serializable
@Entity(tableName = "marks")
data class Mark(
    @PrimaryKey val id: String,
    val watched: Boolean = false,
    val watchedAt: Long? = null,
    val watchlist: Boolean = false,
    val rating: Int = 0,
    val hidden: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Dao
interface MarkDao {
    @Query("SELECT * FROM marks")
    fun all(): Flow<List<Mark>>

    @Query("SELECT * FROM marks")
    suspend fun allOnce(): List<Mark>

    @Upsert
    suspend fun upsert(mark: Mark)

    @Upsert
    suspend fun upsertAll(marks: List<Mark>)
}

@Database(entities = [Mark::class], version = 1, exportSchema = true)
abstract class AppDb : RoomDatabase() {
    abstract fun marks(): MarkDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDb::class.java, "streamlens.db")
                .build().also { instance = it }
        }
    }
}
