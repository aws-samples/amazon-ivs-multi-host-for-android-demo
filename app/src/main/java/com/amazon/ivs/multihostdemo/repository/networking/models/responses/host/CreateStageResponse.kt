package com.amazon.ivs.multihostdemo.repository.networking.models.responses.host

import kotlinx.serialization.Serializable

@Serializable
data class CreateStageResponse(
    val groupId: String,
    val channel: ChannelBody,
    val stage: StageBody,
    val chat: ChatRoomBody
)

@Serializable
data class StageBody(
    val id: String,
    val token: ParticipantToken
)

@Serializable
data class ParticipantToken(
    val expirationTime: String,
    val participantId: String,
    val token: String
)

@Serializable
data class ChannelBody(
    val id: String,
    val playbackUrl: String,
    val ingestEndpoint: String,
    val streamKey: String
)

@Serializable
data class ChatRoomBody(
    val id: String,
    val token: ChatToken
)

@Serializable
data class ChatToken(
    val sessionExpirationTime: String,
    val token: String,
    val tokenExpirationTime: String
)
