package com.amazon.ivs.multihostdemo.repository.networking.models.responses.chat

enum class ChatSdkError(
    var rawError: String? = null,
    var rawCode: Int = -1,
) {
    RAW_ERROR,
    CONNECTION_FAILED,
    MESSAGE_SEND_FAILED,
    MESSAGE_DELETE_FAILED;
}
