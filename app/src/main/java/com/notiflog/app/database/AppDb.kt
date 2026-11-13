package com.notiflog.app.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotifDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(n: NotifEntity): Long

    @Query("UPDATE notifs SET mediaPath = :p, mediaType = :t WHERE id = :id")
    suspend fun setMedia(id: Long, p: String, t: String)

    @Query(
        "SELECT chatKey, chatName AS name, text AS lastText, MAX(time) AS lastTime, COUNT(*) AS count, isGroup " +
            "FROM notifs WHERE (:q = '' OR text LIKE '%' || :q || '%' OR chatName LIKE '%' || :q || '%' OR sender LIKE '%' || :q || '%') " +
            "AND (:f = 0 OR (:f = 1 AND pkg LIKE 'com.whatsapp%') OR (:f = 2 AND category = 'status') OR (:f = 3 AND mediaPath IS NOT NULL)) " +
            "GROUP BY chatKey ORDER BY lastTime DESC"
    )
    fun chats(q: String, f: Int): Flow<List<ChatRow>>

    @Query("SELECT * FROM notifs WHERE chatKey = :k ORDER BY time ASC")
    fun messages(k: String): Flow<List<NotifEntity>>

    @Query("SELECT * FROM notifs WHERE mediaPath IS NOT NULL ORDER BY time DESC")
    fun media(): Flow<List<NotifEntity>>

    @Query("SELECT * FROM notifs WHERE time > :since ORDER BY time ASC")
    suspend fun since(since: Long): List<NotifEntity>

    @Query("DELETE FROM notifs")
    suspend fun clear()

    @Query("SELECT * FROM devices ORDER BY lastActive DESC")
    fun devices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices WHERE token = :t LIMIT 1")
    suspend fun deviceByToken(t: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(d: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDevice(id: String)
}

@Database(entities = [NotifEntity::class, DeviceEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): NotifDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "notiflog.db").build().also { inst = it }
        }
    }
}
