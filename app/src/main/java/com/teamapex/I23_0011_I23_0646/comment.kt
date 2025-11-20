package com.teamapex.I23_0011_I23_0646

data class Comment(
    val id: Int,
    val postId: Int,
    val userId: Int,
    val username: String,
    val userProfilePic: String,
    val commentText: String,
    val createdAt: String
)