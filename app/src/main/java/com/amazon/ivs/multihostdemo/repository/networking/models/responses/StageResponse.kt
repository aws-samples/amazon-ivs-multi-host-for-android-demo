package com.amazon.ivs.multihostdemo.repository.networking.models.responses

import com.amazon.ivs.multihostdemo.repository.models.Stage
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.UserAttributes
import kotlinx.serialization.Serializable

@Serializable
data class StageResponse(
    val roomId: String,
    val channelId: String,
    val stageAttributes: UserAttributes,
    val groupId: String,
    val stageId: String,
) {
    fun asStageItem() = Stage(
        groupId = groupId,
        stageId = stageId,
        name = stageAttributes.username,
        iconUrl = stageAttributes.avatarUrl
    )
}
