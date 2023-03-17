package com.amazon.ivs.multihostdemo.repository.networking.models.requests

import com.amazon.ivs.multihostdemo.common.MOCK_AVATAR_URL
import kotlinx.serialization.Serializable

@Serializable
data class UserAttributes(
    val avatarUrl: String = MOCK_AVATAR_URL,
    val username: String = "Absent",
)
