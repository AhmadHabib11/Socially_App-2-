package com.teamapex.I23_0011_I23_0646

data class Post(
    val id: Int,
    val userId: Int,
    val username: String,
    val userProfilePic: String,
    val imagePath: String,
    val imageBase64: String,
    val caption: String,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByCurrentUser: Boolean = false
)