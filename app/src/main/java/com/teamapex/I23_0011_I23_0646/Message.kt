package com.teamapex.I23_0011_I23_0646

data class Message(
    val id: Int = 0,
    val chatId: String = "",
    val senderId: String = "",
    val messageType: String = "text",
    val content: String = "",
    val mediaPath: String = "",
    val timestamp: Long = 0,
    val vanishMode: Boolean = false,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    val seenBy: String = "",
    val deliveryStatus: String = "sent" // sent, delivered, seen
)