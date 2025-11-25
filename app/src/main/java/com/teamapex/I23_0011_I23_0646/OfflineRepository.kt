package com.teamapex.I23_0011_I23_0646

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class OfflineRepository(context: Context) {

    private val database = OfflineDatabase.getDatabase(context)
    private val messageDao = database.messageDao()
    private val postDao = database.postDao()
    private val storyDao = database.storyDao()
    private val chatDao = database.chatDao()
    private val queuedActionDao = database.queuedActionDao()

    // ============= MESSAGE OPERATIONS =============

    suspend fun cacheMessage(message: Message) = withContext(Dispatchers.IO) {
        val entity = MessageEntity(
            id = message.id,
            chatId = message.chatId,
            senderId = message.senderId,
            messageType = message.messageType,
            content = message.content,
            mediaPath = message.mediaPath,
            timestamp = message.timestamp,
            vanishMode = message.vanishMode,
            isDeleted = message.isDeleted,
            isEdited = message.isEdited,
            seenBy = message.seenBy,
            deliveryStatus = message.deliveryStatus,
            sharedPostId = message.sharedPostId
        )
        messageDao.insertMessage(entity)
    }

    suspend fun getCachedMessages(chatId: String): List<Message> = withContext(Dispatchers.IO) {
        messageDao.getMessagesByChatId(chatId).map { it.toMessage() }
    }

    suspend fun queueMessageSend(
        chatId: String,
        senderId: String,
        messageType: String,
        content: String = "",
        mediaData: String = "",
        vanishMode: Boolean = false,
        sharedPostId: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("chat_id", chatId)
            put("sender_id", senderId)
            put("message_type", messageType)
            put("content", content)
            if (mediaData.isNotEmpty()) put("media_data", mediaData)
            put("vanish_mode", if (vanishMode) "1" else "0")
            sharedPostId?.let { put("shared_post_id", it) }
        }.toString()

        val action = QueuedAction(
            actionType = "send_message",
            payload = payload,
            timestamp = System.currentTimeMillis() / 1000,
            localId = "msg_${System.currentTimeMillis()}"
        )

        queuedActionDao.insertAction(action)
    }

    // ============= POST OPERATIONS =============

    suspend fun cachePosts(posts: List<Post>) = withContext(Dispatchers.IO) {
        val entities = posts.map { it.toEntity() }
        postDao.insertPosts(entities)
    }

    suspend fun getCachedPosts(): List<Post> = withContext(Dispatchers.IO) {
        postDao.getAllPosts().map { it.toPost() }
    }

    suspend fun queuePostUpload(
        userId: String,
        imageData: String,
        caption: String
    ): Long = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("image_data", imageData)
            put("caption", caption)
        }.toString()

        val action = QueuedAction(
            actionType = "upload_post",
            payload = payload,
            timestamp = System.currentTimeMillis() / 1000,
            localId = "post_${System.currentTimeMillis()}"
        )

        queuedActionDao.insertAction(action)
    }

    // ============= STORY OPERATIONS =============

    suspend fun cacheStories(stories: List<Story>) = withContext(Dispatchers.IO) {
        val entities = stories.map { it.toEntity() }
        storyDao.insertStories(entities)
    }

    suspend fun getCachedStories(): List<Story> = withContext(Dispatchers.IO) {
        storyDao.getAllStories().map { it.toStory() }
    }

    suspend fun queueStoryUpload(
        userId: String,
        mediaData: String,
        mediaType: String
    ): Long = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("media_data", mediaData)
            put("media_type", mediaType)
        }.toString()

        val action = QueuedAction(
            actionType = "upload_story",
            payload = payload,
            timestamp = System.currentTimeMillis() / 1000,
            localId = "story_${System.currentTimeMillis()}"
        )

        queuedActionDao.insertAction(action)
    }

    // ============= CHAT OPERATIONS =============

    suspend fun cacheChats(chats: List<Chat>) = withContext(Dispatchers.IO) {
        val entities = chats.map { it.toEntity() }
        chatDao.insertChats(entities)
    }

    suspend fun getCachedChats(): List<Chat> = withContext(Dispatchers.IO) {
        chatDao.getAllChats().map { it.toChat() }
    }

    // ============= QUEUED ACTIONS =============

    suspend fun getPendingActions(): List<QueuedAction> = withContext(Dispatchers.IO) {
        queuedActionDao.getPendingActions()
    }

    suspend fun updateActionStatus(actionId: Long, status: String, error: String? = null) =
        withContext(Dispatchers.IO) {
            queuedActionDao.updateActionStatus(actionId, status, error)
        }

    suspend fun deleteAction(actionId: Long) = withContext(Dispatchers.IO) {
        queuedActionDao.deleteAction(actionId)
    }

    suspend fun getPendingActionsCount(): Int = withContext(Dispatchers.IO) {
        queuedActionDao.getPendingActionsCount()
    }

    suspend fun deleteCompletedActions() = withContext(Dispatchers.IO) {
        queuedActionDao.deleteCompletedActions()
    }

    // ============= LIKE POST ACTION =============

    suspend fun queueLikePost(userId: String, postId: Int): Long = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("user_id", userId)
            put("post_id", postId)
        }.toString()

        val action = QueuedAction(
            actionType = "like_post",
            payload = payload,
            timestamp = System.currentTimeMillis() / 1000
        )

        queuedActionDao.insertAction(action)
    }

    // ============= EXTENSION FUNCTIONS =============

    private fun MessageEntity.toMessage() = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        messageType = messageType,
        content = content,
        mediaPath = mediaPath,
        timestamp = timestamp,
        vanishMode = vanishMode,
        isDeleted = isDeleted,
        isEdited = isEdited,
        seenBy = seenBy,
        deliveryStatus = deliveryStatus,
        sharedPostId = sharedPostId
    )

    private fun Post.toEntity() = PostEntity(
        id = id,
        userId = userId,
        username = username,
        userProfilePic = userProfilePic,
        imagePath = imagePath,
        imageBase64 = imageBase64,
        caption = caption,
        createdAt = createdAt,
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLikedByCurrentUser = isLikedByCurrentUser
    )

    private fun PostEntity.toPost() = Post(
        id = id,
        userId = userId,
        username = username,
        userProfilePic = userProfilePic,
        imagePath = imagePath,
        imageBase64 = imageBase64,
        caption = caption,
        createdAt = createdAt,
        likesCount = likesCount,
        commentsCount = commentsCount,
        isLikedByCurrentUser = isLikedByCurrentUser
    )

    private fun Story.toEntity() = StoryEntity(
        id = id,
        userId = userId,
        username = username,
        firstName = firstName,
        lastName = lastName,
        profilePic = profilePic,
        mediaPath = mediaPath,
        mediaType = mediaType,
        mediaBase64 = mediaBase64,
        createdAt = createdAt,
        expiresAt = expiresAt,
        views = views
    )

    private fun StoryEntity.toStory() = Story(
        id = id,
        userId = userId,
        username = username,
        firstName = firstName,
        lastName = lastName,
        profilePic = profilePic,
        mediaPath = mediaPath,
        mediaType = mediaType,
        mediaBase64 = mediaBase64,
        createdAt = createdAt,
        expiresAt = expiresAt,
        views = views
    )

    private fun Chat.toEntity() = ChatEntity(
        chatId = chatId,
        userId = userId,
        username = username,
        profileImage = profileImage,
        lastMessage = lastMessage,
        timestamp = timestamp,
        lastMessageSenderId = lastMessageSenderId,
        deliveryStatus = deliveryStatus,
        isOnline = isOnline,
        lastSeen = lastSeen
    )

    private fun ChatEntity.toChat() = Chat(
        chatId = chatId,
        userId = userId,
        username = username,
        profileImage = profileImage,
        lastMessage = lastMessage,
        timestamp = timestamp,
        lastMessageSenderId = lastMessageSenderId,
        deliveryStatus = deliveryStatus,
        isOnline = isOnline,
        lastSeen = lastSeen
    )
}