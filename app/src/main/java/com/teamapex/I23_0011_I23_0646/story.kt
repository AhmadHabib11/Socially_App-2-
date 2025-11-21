package com.teamapex.I23_0011_I23_0646

data class Story(
    val id: Int,
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
    val views: Int
) {
    // Helper function to get display name
    fun getDisplayName(currentUserId: String): String {
        return if (userId.toString() == currentUserId) {
            "Your Story"
        } else {
            username
        }
    }
}