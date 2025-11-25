package com.teamapex.I23_0011_I23_0646

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MessageDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "messages.db"
        private const val DATABASE_VERSION = 4

        // Messages table
        const val TABLE_MESSAGES = "messages"
        const val COLUMN_MESSAGE_ID = "id"
        const val COLUMN_CHAT_ID = "chat_id"
        const val COLUMN_SENDER_ID = "sender_id"
        const val COLUMN_MESSAGE_TYPE = "message_type"
        const val COLUMN_CONTENT = "content"
        const val COLUMN_MEDIA_PATH = "media_path"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_VANISH_MODE = "vanish_mode"
        const val COLUMN_IS_DELETED = "is_deleted"
        const val COLUMN_IS_EDITED = "is_edited"
        const val COLUMN_SEEN_BY = "seen_by"
        const val COLUMN_DELIVERY_STATUS = "delivery_status"
        const val COLUMN_SHARED_POST_ID = "shared_post_id"

        // Chats table
        const val TABLE_CHATS = "chats"
        const val COLUMN_CHAT_ID_PK = "chat_id"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PROFILE_IMAGE = "profile_image"
        const val COLUMN_LAST_MESSAGE = "last_message"
        const val COLUMN_LAST_TIMESTAMP = "last_timestamp"
        const val COLUMN_LAST_MESSAGE_SENDER_ID = "last_message_sender_id"
        const val COLUMN_CHAT_DELIVERY_STATUS = "delivery_status"

        private const val CREATE_MESSAGES_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
                $COLUMN_MESSAGE_ID INTEGER PRIMARY KEY,
                $COLUMN_CHAT_ID TEXT,
                $COLUMN_SENDER_ID TEXT,
                $COLUMN_MESSAGE_TYPE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_MEDIA_PATH TEXT,
                $COLUMN_TIMESTAMP INTEGER,
                $COLUMN_VANISH_MODE INTEGER,
                $COLUMN_IS_DELETED INTEGER,
                $COLUMN_IS_EDITED INTEGER,
                $COLUMN_SEEN_BY TEXT,
                $COLUMN_DELIVERY_STATUS TEXT,
                $COLUMN_SHARED_POST_ID INTEGER
            )
        """

        private const val CREATE_CHATS_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE_CHATS (
                $COLUMN_CHAT_ID_PK TEXT PRIMARY KEY,
                $COLUMN_USER_ID TEXT,
                $COLUMN_USERNAME TEXT,
                $COLUMN_PROFILE_IMAGE TEXT,
                $COLUMN_LAST_MESSAGE TEXT,
                $COLUMN_LAST_TIMESTAMP INTEGER,
                $COLUMN_LAST_MESSAGE_SENDER_ID TEXT,
                $COLUMN_CHAT_DELIVERY_STATUS TEXT
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_MESSAGES_TABLE)
        db.execSQL(CREATE_CHATS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COLUMN_SHARED_POST_ID INTEGER")
            } catch (e: Exception) {
                // Column might already exist
            }
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_CHATS")
                db.execSQL(CREATE_CHATS_TABLE)
            } catch (e: Exception) {
                android.util.Log.e("DBUpgrade", "Error upgrading chats table: ${e.message}")
            }
        }
    }

    // Cache a message
    fun cacheMessage(message: Message) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_MESSAGE_ID, message.id)
            put(COLUMN_CHAT_ID, message.chatId)
            put(COLUMN_SENDER_ID, message.senderId)
            put(COLUMN_MESSAGE_TYPE, message.messageType)
            put(COLUMN_CONTENT, message.content)
            put(COLUMN_MEDIA_PATH, message.mediaPath)
            put(COLUMN_TIMESTAMP, message.timestamp)
            put(COLUMN_VANISH_MODE, if (message.vanishMode) 1 else 0)
            put(COLUMN_IS_DELETED, if (message.isDeleted) 1 else 0)
            put(COLUMN_IS_EDITED, if (message.isEdited) 1 else 0)
            put(COLUMN_SEEN_BY, message.seenBy)
            put(COLUMN_DELIVERY_STATUS, message.deliveryStatus)
            put(COLUMN_SHARED_POST_ID, message.sharedPostId)
        }

        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Get cached messages for a specific chat
    fun getCachedMessages(chatId: String): List<Message> {
        val messages = mutableListOf<Message>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "$COLUMN_CHAT_ID = ?",
            arrayOf(chatId),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val message = Message(
                    id = it.getInt(it.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)),
                    chatId = it.getString(it.getColumnIndexOrThrow(COLUMN_CHAT_ID)),
                    senderId = it.getString(it.getColumnIndexOrThrow(COLUMN_SENDER_ID)),
                    messageType = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE)),
                    content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT)),
                    mediaPath = it.getString(it.getColumnIndexOrThrow(COLUMN_MEDIA_PATH)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    vanishMode = it.getInt(it.getColumnIndexOrThrow(COLUMN_VANISH_MODE)) == 1,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1,
                    isEdited = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_EDITED)) == 1,
                    seenBy = it.getString(it.getColumnIndexOrThrow(COLUMN_SEEN_BY)),
                    deliveryStatus = it.getString(it.getColumnIndexOrThrow(COLUMN_DELIVERY_STATUS)),
                    sharedPostId = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_SHARED_POST_ID)))
                        null
                    else
                        it.getInt(it.getColumnIndexOrThrow(COLUMN_SHARED_POST_ID))
                )
                messages.add(message)
            }
        }

        return messages
    }

    // Cache a chat
    fun cacheChat(chat: Chat) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CHAT_ID_PK, chat.chatId)
            put(COLUMN_USER_ID, chat.userId)
            put(COLUMN_USERNAME, chat.username)
            put(COLUMN_PROFILE_IMAGE, chat.profileImage)
            put(COLUMN_LAST_MESSAGE, chat.lastMessage)
            put(COLUMN_LAST_TIMESTAMP, chat.timestamp)
            put(COLUMN_LAST_MESSAGE_SENDER_ID, chat.lastMessageSenderId)
            put(COLUMN_CHAT_DELIVERY_STATUS, chat.deliveryStatus)
        }

        db.insertWithOnConflict(TABLE_CHATS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Get all cached chats
    fun getCachedChats(): List<Chat> {
        val chats = mutableListOf<Chat>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_CHATS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_LAST_TIMESTAMP DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val chat = Chat(
                    chatId = it.getString(it.getColumnIndexOrThrow(COLUMN_CHAT_ID_PK)),
                    userId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    username = it.getString(it.getColumnIndexOrThrow(COLUMN_USERNAME)),
                    profileImage = it.getString(it.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE)),
                    lastMessage = it.getString(it.getColumnIndexOrThrow(COLUMN_LAST_MESSAGE)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_LAST_TIMESTAMP)),
                    lastMessageSenderId = it.getString(it.getColumnIndexOrThrow(COLUMN_LAST_MESSAGE_SENDER_ID)),
                    deliveryStatus = it.getString(it.getColumnIndexOrThrow(COLUMN_CHAT_DELIVERY_STATUS))
                )
                chats.add(chat)
            }
        }

        return chats
    }

    // Update message content (for editing)
    fun updateMessageContent(messageId: Int, newContent: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CONTENT, newContent)
            put(COLUMN_IS_EDITED, 1)
        }

        db.update(
            TABLE_MESSAGES,
            values,
            "$COLUMN_MESSAGE_ID = ?",
            arrayOf(messageId.toString())
        )
    }

    // Delete a message (mark as deleted)
    fun deleteMessage(messageId: Int) {
        val db = writableDatabase

        // Actually delete from database (not just mark as deleted)
        val deleted = db.delete(
            TABLE_MESSAGES,
            "$COLUMN_MESSAGE_ID = ?",
            arrayOf(messageId.toString())
        )

        android.util.Log.d("MessageDB", "Deleted message $messageId from local cache: $deleted rows affected")
    }

    // NEW: Delete all vanish mode messages for a specific chat and user
    fun deleteVanishMessages(chatId: String, userId: String) {
        val db = writableDatabase

        // Get all vanish mode messages
        val cursor = db.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_MESSAGE_ID, COLUMN_SENDER_ID, COLUMN_SEEN_BY),
            "$COLUMN_CHAT_ID = ? AND $COLUMN_VANISH_MODE = 1",
            arrayOf(chatId),
            null,
            null,
            null
        )

        val messagesToDelete = mutableListOf<Int>()

        cursor.use {
            while (it.moveToNext()) {
                val messageId = it.getInt(0)
                val senderId = it.getString(1)
                val seenBy = it.getString(2)

                // Check if message has been seen by both users
                val seenByList = seenBy.split(",").filter { id -> id.isNotEmpty() }

                // If we received this message and saw it, delete it
                if (senderId != userId && seenByList.contains(userId)) {
                    messagesToDelete.add(messageId)
                }
            }
        }

        // Delete the messages
        if (messagesToDelete.isNotEmpty()) {
            val placeholders = messagesToDelete.joinToString(",") { "?" }
            val args = messagesToDelete.map { it.toString() }.toTypedArray()

            val deleted = db.delete(
                TABLE_MESSAGES,
                "$COLUMN_MESSAGE_ID IN ($placeholders)",
                args
            )

            android.util.Log.d("MessageDB", "Deleted $deleted vanish messages from local cache")
        }
    }

    // NEW: Get all vanish mode messages for a chat
    fun getVanishMessages(chatId: String): List<Message> {
        val messages = mutableListOf<Message>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "$COLUMN_CHAT_ID = ? AND $COLUMN_VANISH_MODE = 1",
            arrayOf(chatId),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val message = Message(
                    id = it.getInt(it.getColumnIndexOrThrow(COLUMN_MESSAGE_ID)),
                    chatId = it.getString(it.getColumnIndexOrThrow(COLUMN_CHAT_ID)),
                    senderId = it.getString(it.getColumnIndexOrThrow(COLUMN_SENDER_ID)),
                    messageType = it.getString(it.getColumnIndexOrThrow(COLUMN_MESSAGE_TYPE)),
                    content = it.getString(it.getColumnIndexOrThrow(COLUMN_CONTENT)),
                    mediaPath = it.getString(it.getColumnIndexOrThrow(COLUMN_MEDIA_PATH)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)),
                    vanishMode = true,
                    isDeleted = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1,
                    isEdited = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_EDITED)) == 1,
                    seenBy = it.getString(it.getColumnIndexOrThrow(COLUMN_SEEN_BY)),
                    deliveryStatus = it.getString(it.getColumnIndexOrThrow(COLUMN_DELIVERY_STATUS)),
                    sharedPostId = if (it.isNull(it.getColumnIndexOrThrow(COLUMN_SHARED_POST_ID)))
                        null
                    else
                        it.getInt(it.getColumnIndexOrThrow(COLUMN_SHARED_POST_ID))
                )
                messages.add(message)
            }
        }

        return messages
    }

    // Clear all messages for a specific chat
    fun clearChatMessages(chatId: String) {
        val db = writableDatabase
        db.delete(
            TABLE_MESSAGES,
            "$COLUMN_CHAT_ID = ?",
            arrayOf(chatId)
        )
    }

    // Clear all cached data
    fun clearAllCache() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_MESSAGES")
        db.execSQL("DELETE FROM $TABLE_CHATS")
    }

    // NEW: Remove deleted messages from cache
    fun removeDeletedMessages(chatId: String, validMessageIds: List<Int>) {
        val db = writableDatabase

        // Get all cached message IDs for this chat
        val allIds = mutableListOf<Int>()
        val cursor = db.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_MESSAGE_ID),
            "$COLUMN_CHAT_ID = ?",
            arrayOf(chatId),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                allIds.add(it.getInt(0))
            }
        }

        // Find IDs that should be deleted (exist locally but not on server)
        val idsToDelete = allIds.filter { !validMessageIds.contains(it) }

        if (idsToDelete.isNotEmpty()) {
            val placeholders = idsToDelete.joinToString(",") { "?" }
            val args = idsToDelete.map { it.toString() }.toTypedArray()

            val deleted = db.delete(
                TABLE_MESSAGES,
                "$COLUMN_MESSAGE_ID IN ($placeholders)",
                args
            )

            android.util.Log.d("MessageDB", "Removed $deleted deleted messages from cache")
        }
    }

    // NEW: Remove deleted chat from cache
    fun removeDeletedChat(chatId: String) {
        val db = writableDatabase

        // Delete the chat
        db.delete(
            TABLE_CHATS,
            "$COLUMN_CHAT_ID_PK = ?",
            arrayOf(chatId)
        )

        // Also delete all messages for this chat
        db.delete(
            TABLE_MESSAGES,
            "$COLUMN_CHAT_ID = ?",
            arrayOf(chatId)
        )

        android.util.Log.d("MessageDB", "Removed chat $chatId and all its messages from cache")
    }

    // NEW: Get cached message IDs
    fun getCachedMessageIds(chatId: String): List<Int> {
        val db = readableDatabase
        val ids = mutableListOf<Int>()

        val cursor = db.query(
            TABLE_MESSAGES,
            arrayOf(COLUMN_MESSAGE_ID),
            "$COLUMN_CHAT_ID = ?",
            arrayOf(chatId),
            null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                ids.add(it.getInt(0))
            }
        }

        return ids
    }

    // NEW: Get cached chat IDs
    fun getCachedChatIds(): List<String> {
        val db = readableDatabase
        val ids = mutableListOf<String>()

        val cursor = db.query(
            TABLE_CHATS,
            arrayOf(COLUMN_CHAT_ID_PK),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                ids.add(it.getString(0))
            }
        }

        return ids
    }
}