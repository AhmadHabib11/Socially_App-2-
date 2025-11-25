package com.teamapex.I23_0011_I23_0646

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ============= ENTITIES =============

@Entity(tableName = "messages_cache")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val chatId: String,
    val senderId: String,
    val messageType: String,
    val content: String,
    val mediaPath: String,
    val timestamp: Long,
    val vanishMode: Boolean,
    val isDeleted: Boolean,
    val isEdited: Boolean,
    val seenBy: String,
    val deliveryStatus: String,
    val sharedPostId: Int?,
    val syncStatus: String = "synced" // synced, pending, failed
)

@Entity(tableName = "posts_cache")
data class PostEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val username: String,
    val userProfilePic: String,
    val imagePath: String,
    val imageBase64: String,
    val caption: String,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByCurrentUser: Boolean,
    val syncStatus: String = "synced"
)

@Entity(tableName = "stories_cache")
data class StoryEntity(
    @PrimaryKey val id: Int,
    val userId: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val profilePic: String,
    val mediaPath: String,
    val mediaType: String,
    val mediaBase64: String,
    val createdAt: String,
    val expiresAt: String,
    val views: Int,
    val syncStatus: String = "synced"
)

@Entity(tableName = "chats_cache")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val userId: String,
    val username: String,
    val profileImage: String,
    val lastMessage: String,
    val timestamp: Long,
    val lastMessageSenderId: String,
    val deliveryStatus: String,
    val isOnline: Boolean,
    val lastSeen: Long
)

// ============= QUEUED ACTIONS =============

@Entity(tableName = "queued_actions")
data class QueuedAction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // send_message, upload_post, upload_story, like_post, etc.
    val payload: String, // JSON payload
    val timestamp: Long,
    val retryCount: Int = 0,
    val status: String = "pending", // pending, processing, failed, completed
    val errorMessage: String? = null,
    val localId: String? = null // To match with local optimistic updates
)

// ============= DAOs =============

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages_cache WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesByChatId(chatId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages_cache SET content = :newContent, isEdited = 1 WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: Int, newContent: String)

    @Query("UPDATE messages_cache SET isDeleted = 1 WHERE id = :messageId")
    suspend fun markMessageDeleted(messageId: Int)

    @Query("DELETE FROM messages_cache WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)

    @Query("SELECT * FROM messages_cache WHERE syncStatus = 'pending'")
    suspend fun getPendingMessages(): List<MessageEntity>
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts_cache ORDER BY createdAt DESC")
    suspend fun getAllPosts(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts_cache")
    suspend fun deleteAllPosts()

    @Query("SELECT * FROM posts_cache WHERE syncStatus = 'pending'")
    suspend fun getPendingPosts(): List<PostEntity>
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories_cache ORDER BY createdAt DESC")
    suspend fun getAllStories(): List<StoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("DELETE FROM stories_cache")
    suspend fun deleteAllStories()

    @Query("SELECT * FROM stories_cache WHERE syncStatus = 'pending'")
    suspend fun getPendingStories(): List<StoryEntity>
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats_cache ORDER BY timestamp DESC")
    suspend fun getAllChats(): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats_cache")
    suspend fun deleteAllChats()
}

@Dao
interface QueuedActionDao {
    @Query("SELECT * FROM queued_actions WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingActions(): List<QueuedAction>

    @Insert
    suspend fun insertAction(action: QueuedAction): Long

    @Update
    suspend fun updateAction(action: QueuedAction)

    @Query("UPDATE queued_actions SET status = :status, errorMessage = :error WHERE id = :actionId")
    suspend fun updateActionStatus(actionId: Long, status: String, error: String?)

    @Query("DELETE FROM queued_actions WHERE id = :actionId")
    suspend fun deleteAction(actionId: Long)

    @Query("DELETE FROM queued_actions WHERE status = 'completed'")
    suspend fun deleteCompletedActions()

    @Query("SELECT COUNT(*) FROM queued_actions WHERE status = 'pending'")
    suspend fun getPendingActionsCount(): Int
}

// ============= DATABASE =============

@Database(
    entities = [
        MessageEntity::class,
        PostEntity::class,
        StoryEntity::class,
        ChatEntity::class,
        QueuedAction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun chatDao(): ChatDao
    abstract fun queuedActionDao(): QueuedActionDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        fun getDatabase(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "socially_offline_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}