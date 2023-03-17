package com.amazon.ivs.multihostdemo.repository.networking.models.responses.host

import kotlinx.serialization.Serializable

@Serializable
data class JoinStageResponse(
    val chat: ChatRoomBody,
    val stage: StageBody,
)
