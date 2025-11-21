package com.teamapex.I23_0011_I23_0646

data class FollowRequest(
    val followId: Int,
    val followerId: Int,
    val username: String,
    val firstName: String,
    val lastName: String,
    val profilePic: String,
    val createdAt: String
)