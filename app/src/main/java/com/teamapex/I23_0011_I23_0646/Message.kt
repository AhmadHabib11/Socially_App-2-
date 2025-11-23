package com.teamapex.I23_0011_I23_0646

data class Message(
    val id: Int,
    val chatId: String,
    val senderId: String,
    val messageType: String, // "text", "image", "video", "file", "shared_post"
    val content: String = "",
    val mediaPath: String = "",
    val timestamp: Long,
    val vanishMode: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val seenBy: String = "",
    val deliveryStatus: String = "sent", // "sent", "delivered", "seen"
    val sharedPostId: Int? = null, // For shared posts
    var sharedPost: SharedPostData? = null // Post details loaded separately
)

data class SharedPostData(
    val postId: Int,
    val userId: Int,
    val username: String,
    val profilePic: String,
    val imageBase64: String,
    val caption: String,
    val likesCount: Int,
    val commentsCount: Int
)