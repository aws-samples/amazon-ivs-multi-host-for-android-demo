package com.amazon.ivs.multihostdemo.repository.networking.models.requests.host
import kotlinx.serialization.Serializable

@Serializable
data class DisconnectRequest(
    val groupId: String,
    val participantId: String,
    val reason: String,
    val userId: String
)
