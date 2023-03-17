package com.amazon.ivs.multihostdemo.repository.networking.models.requests.host

import com.amazon.ivs.multihostdemo.repository.networking.models.requests.UserAttributes
import kotlinx.serialization.Serializable

@Serializable
data class CreateStageRequest(
    val userId: String,
    val attributes: UserAttributes,
    val groupId: String? = null
)
