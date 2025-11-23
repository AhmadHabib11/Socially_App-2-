package com.teamapex.I23_0011_I23_0646

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MessageDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "messages.db"
        private const val DATABASE_VERSION = 4

        // Messages table - FIXED: Changed message_id to id to match your database
        const val TABLE_MESSAGES = "messages"
        const val COLUMN_MESSAGE_ID = "id"  // ← CHANGED FROM "message_id" to "id"
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
        // Handle version 1 -> 2+ : add shared_post_id to messages
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_MESSAGES ADD COLUMN $COLUMN_SHARED_POST_ID INTEGER")
            } catch (e: Exception) {
                // Column might already exist, ignore
            }
        }

        // Handle version 3 -> 4: Recreate chats table with all columns
        if (oldVersion < 4) {
            try {
                // Drop old chats table
                db.execSQL("DROP TABLE IF EXISTS $TABLE_CHATS")
                // Recreate with correct schema
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
        val values = ContentValues().apply {
            put(COLUMN_IS_DELETED, 1)
        }

        db.update(
            TABLE_MESSAGES,
            values,
            "$COLUMN_MESSAGE_ID = ?",
            arrayOf(messageId.toString())
        )
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
}