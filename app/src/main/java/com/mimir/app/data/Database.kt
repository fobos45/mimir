package com.mimir.app.data

import androidx.room.*

// ── Entities ──────────────────────────────────────────────────────────────────

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val pubkeyHex: String,
    val nickname: String,
    val info: String = "",
    val avatarPath: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,          // unix millis
    val unreadCount: Int = 0,
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val guid: Long,
    val contactPubkey: String,
    val isOutgoing: Boolean,
    val msgType: Int,                  // 1=text, 2=file, 3=image, 4=audio
    val text: String = "",
    val filePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val sendTime: Long = System.currentTimeMillis(),
    val delivered: Boolean = false,
    val uploadProgress: Float = -1f,  // -1 = not uploading, 0..1 = progress
    val downloadProgress: Float = -1f,
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY lastSeen DESC")
    fun allContacts(): kotlinx.coroutines.flow.Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE pubkeyHex = :hex LIMIT 1")
    suspend fun byKey(hex: String): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: Contact)

    @Query("UPDATE contacts SET isOnline = :online, lastSeen = :ts WHERE pubkeyHex = :hex")
    suspend fun setOnline(hex: String, online: Boolean, ts: Long)

    @Query("UPDATE contacts SET unreadCount = unreadCount + 1 WHERE pubkeyHex = :hex")
    suspend fun incrementUnread(hex: String)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE pubkeyHex = :hex")
    suspend fun clearUnread(hex: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE contactPubkey = :hex ORDER BY sendTime ASC")
    fun forContact(hex: String): kotlinx.coroutines.flow.Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE contactPubkey = :hex ORDER BY sendTime DESC LIMIT 1")
    suspend fun last(hex: String): Message?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(msg: Message)

    @Query("UPDATE messages SET delivered = 1 WHERE guid = :guid")
    suspend fun markDelivered(guid: Long)

    @Query("UPDATE messages SET uploadProgress = :progress WHERE guid = :guid")
    suspend fun updateUploadProgress(guid: Long, progress: Float)

    @Query("UPDATE messages SET downloadProgress = :progress WHERE guid = :guid")
    suspend fun updateDownloadProgress(guid: Long, progress: Float)
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(entities = [Contact::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contacts(): ContactDao
    abstract fun messages(): MessageDao
}
