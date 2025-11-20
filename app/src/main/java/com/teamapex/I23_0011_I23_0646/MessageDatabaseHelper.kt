package com.teamapex.I23_0011_I23_0646

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MessageDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "messages.db"
        private const val DATABASE_VERSION = 1

        // Messages table
        private const val TABLE_MESSAGES = "messages"
        private const val COL_ID = "id"
        private const val COL_CHAT_ID = "chat_id"
        private const val COL_SENDER_ID = "sender_id"
        private const val COL_MESSAGE_TYPE = "message_type"
        private const val COL_CONTENT = "content"
        private const val COL_MEDIA_PATH = "media_path"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_VANISH_MODE = "vanish_mode"
        private const val COL_IS_DELETED = "is_deleted"
        private const val COL_IS_EDITED = "is_edited"
        private const val COL_SEEN_BY = "seen_by"

        // Chats table
        private const val TABLE_CHATS = "chats"
        private const val COL_USER_ID = "user_id"
        private const val COL_USERNAME = "username"
        private const val COL_PROFILE_PIC = "profile_pic"
        private const val COL_LAST_MESSAGE = "last_message"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_ID INTEGER PRIMARY KEY,
                $COL_CHAT_ID TEXT NOT NULL,
                $COL_SENDER_ID TEXT NOT NULL,
                $COL_MESSAGE_TYPE TEXT DEFAULT 'text',
                $COL_CONTENT TEXT,
                $COL_MEDIA_PATH TEXT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_VANISH_MODE INTEGER DEFAULT 0,
                $COL_IS_DELETED INTEGER DEFAULT 0,
                $COL_IS_EDITED INTEGER DEFAULT 0,
                $COL_SEEN_BY TEXT
            )
        """.trimIndent()

        val createChatsTable = """
            CREATE TABLE $TABLE_CHATS (
                $COL_CHAT_ID TEXT PRIMARY KEY,
                $COL_USER_ID TEXT NOT NULL,
                $COL_USERNAME TEXT,
                $COL_PROFILE_PIC TEXT,
                $COL_LAST_MESSAGE TEXT,
                $COL_TIMESTAMP INTEGER
            )
        """.trimIndent()

        db?.execSQL(createMessagesTable)
        db?.execSQL(createChatsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CHATS")
        onCreate(db)
    }

    // Cache message locally
    fun cacheMessage(message: Message) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, message.id)
            put(COL_CHAT_ID, message.chatId)
            put(COL_SENDER_ID, message.senderId)
            put(COL_MESSAGE_TYPE, message.messageType)
            put(COL_CONTENT, message.content)
            put(COL_MEDIA_PATH, message.mediaPath)
            put(COL_TIMESTAMP, message.timestamp)
            put(COL_VANISH_MODE, if (message.vanishMode) 1 else 0)
            put(COL_IS_DELETED, if (message.isDeleted) 1 else 0)
            put(COL_IS_EDITED, if (message.isEdited) 1 else 0)
            put(COL_SEEN_BY, message.seenBy)
        }

        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Get cached messages for a chat
    fun getCachedMessages(chatId: String): List<Message> {
        val messages = mutableListOf<Message>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            "$COL_CHAT_ID = ? AND $COL_IS_DELETED = 0",
            arrayOf(chatId),
            null, null,
            "$COL_TIMESTAMP ASC"
        )

        while (cursor.moveToNext()) {
            messages.add(
                Message(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    chatId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHAT_ID)),
                    senderId = cursor.getString(cursor.getColumnIndexOrThrow(COL_SENDER_ID)),
                    messageType = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_TYPE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)) ?: "",
                    mediaPath = cursor.getString(cursor.getColumnIndexOrThrow(COL_MEDIA_PATH)) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP)),
                    vanishMode = cursor.getInt(cursor.getColumnIndexOrThrow(COL_VANISH_MODE)) == 1,
                    isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_DELETED)) == 1,
                    isEdited = cursor.getInt(cursor.getColumnIndexOrThrow(COL_IS_EDITED)) == 1,
                    seenBy = cursor.getString(cursor.getColumnIndexOrThrow(COL_SEEN_BY)) ?: ""
                )
            )
        }

        cursor.close()
        db.close()
        return messages
    }

    // Cache chat locally
    fun cacheChat(chat: Chat) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CHAT_ID, chat.chatId)
            put(COL_USER_ID, chat.userId)
            put(COL_USERNAME, chat.username)
            put(COL_PROFILE_PIC, chat.profileImage)
            put(COL_LAST_MESSAGE, chat.lastMessage)
            put(COL_TIMESTAMP, chat.timestamp)
        }

        db.insertWithOnConflict(TABLE_CHATS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Get cached chats
    fun getCachedChats(): List<Chat> {
        val chats = mutableListOf<Chat>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_CHATS,
            null, null, null, null, null,
            "$COL_TIMESTAMP DESC"
        )

        while (cursor.moveToNext()) {
            chats.add(
                Chat(
                    chatId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHAT_ID)),
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                    profileImage = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROFILE_PIC)),
                    lastMessage = cursor.getString(cursor.getColumnIndexOrThrow(COL_LAST_MESSAGE)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
                )
            )
        }

        cursor.close()
        db.close()
        return chats
    }

    // Delete vanish mode messages from local cache
    fun deleteVanishMessages(chatId: String, userId: String) {
        val db = writableDatabase
        db.delete(
            TABLE_MESSAGES,
            "$COL_CHAT_ID = ? AND $COL_VANISH_MODE = 1 AND $COL_SEEN_BY = ?",
            arrayOf(chatId, userId)
        )
        db.close()
    }

    // Clear all cached data
    fun clearCache() {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
        db.delete(TABLE_CHATS, null, null)
        db.close()
    }
    // Add these methods to your MessageDatabaseHelper class

    fun updateMessageContent(messageId: Int, newContent: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("content", newContent)
            put("is_edited", 1)
        }
        db.update("messages", values, "id = ?", arrayOf(messageId.toString()))
        db.close()
    }

    fun deleteMessage(messageId: Int) {
        val db = writableDatabase
        db.delete("messages", "id = ?", arrayOf(messageId.toString()))
        db.close()
    }
}
