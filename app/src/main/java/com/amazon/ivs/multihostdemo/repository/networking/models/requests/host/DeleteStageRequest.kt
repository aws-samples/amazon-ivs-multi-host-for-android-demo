package com.amazon.ivs.multihostdemo.repository.networking.models.requests.host

import kotlinx.serialization.Serializable

@Serializable
data class DeleteStageRequest(
    val groupId: String
)
