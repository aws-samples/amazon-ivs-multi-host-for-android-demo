package com.amazon.ivs.multihostdemo.repository.networking.models.requests.host

import com.amazon.ivs.multihostdemo.repository.networking.models.requests.UserAttributes
import kotlinx.serialization.Serializable

@Serializable
data class JoinStageRequest(
    val groupId: String,
    val userId: String,
    val attributes: UserAttributes,
)
