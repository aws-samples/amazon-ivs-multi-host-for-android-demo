package com.amazon.ivs.multihostdemo.repository.models

import com.amazonaws.ivs.chat.messaging.entities.ChatMessage

data class StageChatMessage(
    val id: String,
    val avatarUrl: String,
    val username: String,
    val message: String,
)

fun ChatMessage.toStageChatMessage() = StageChatMessage(
    id = id,
    // TODO: While it is expected that the sender attributes contains these properties,
    //  it may be good to consider having some null checking and returning the whole as a null
    avatarUrl = sender.attributes!!["avatarUrl"]!!,
    username = sender.attributes!!["username"]!!,
    message = content,
)
