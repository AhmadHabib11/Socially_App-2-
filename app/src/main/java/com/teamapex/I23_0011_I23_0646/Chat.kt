package com.teamapex.I23_0011_I23_0646

data class Chat(
    val chatId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImage: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0,
    val lastMessageSenderId: String = "",
    val deliveryStatus: String = "sent",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0
)