package com.amazon.ivs.multihostdemo.common.ivs.chat

import com.amazon.ivs.multihostdemo.repository.models.StageChatMessage
import com.amazon.ivs.multihostdemo.repository.models.toStageChatMessage
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.chat.ChatSdkError
import com.amazon.ivs.stagebroadcastmanager.common.updateList
import com.amazonaws.ivs.chat.messaging.ChatRoom
import com.amazonaws.ivs.chat.messaging.ChatRoomListener
import com.amazonaws.ivs.chat.messaging.ChatToken
import com.amazonaws.ivs.chat.messaging.DeleteMessageCallback
import com.amazonaws.ivs.chat.messaging.DisconnectReason
import com.amazonaws.ivs.chat.messaging.DisconnectUserCallback
import com.amazonaws.ivs.chat.messaging.SendMessageCallback
import com.amazonaws.ivs.chat.messaging.entities.ChatError
import com.amazonaws.ivs.chat.messaging.entities.ChatEvent
import com.amazonaws.ivs.chat.messaging.entities.ChatMessage
import com.amazonaws.ivs.chat.messaging.entities.DeleteMessageEvent
import com.amazonaws.ivs.chat.messaging.entities.DisconnectUserEvent
import com.amazonaws.ivs.chat.messaging.logger.ChatLogLevel
import com.amazonaws.ivs.chat.messaging.requests.DeleteMessageRequest
import com.amazonaws.ivs.chat.messaging.requests.DisconnectUserRequest
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatManager @Inject constructor() {
    private var showDisconnectError = false
    private var chatRoom: ChatRoom? = null
    private var roomListener: ChatRoomListener? = null
    private val userId = UUID.randomUUID().toString()
    private var arn: String = ""
    private val isConnected get() = chatRoom?.state == ChatRoom.State.CONNECTED

    private val _onRoomConnected = Channel<Unit>()
    private val _onRemoteKicked = Channel<String>()
    private val _onJoinRequestResponse = Channel<Boolean>()
    private val _onError = Channel<ChatSdkError>()
    private val _onLocalKicked = Channel<Unit>()
    private val _messages = MutableStateFlow(emptyList<StageChatMessage>())

    val messages = _messages.asStateFlow()
    val onRoomConnected = _onRoomConnected.receiveAsFlow()
    val onRemoteKicked = _onRemoteKicked.receiveAsFlow()
    val onJoinRequestResponse = _onJoinRequestResponse.receiveAsFlow()
    val onError = _onError.receiveAsFlow()

    val onLocalKicked = _onLocalKicked.receiveAsFlow()

    fun joinRoom(arn: String, chatToken: ChatToken) {
        this.arn = arn
        val region = arn.split(":")[3]
        Timber.d("Initializing chat room: [$region]")
        showDisconnectError = false
        clearPreviousChat()

        chatRoom = ChatRoom(
            regionOrUrl = region,
            tokenProvider = { it.onSuccess(chatToken) },
            maxReconnectAttempts = 0
        )

        roomListener = object : ChatRoomListener {
            override fun onConnected(room: ChatRoom) {
                Timber.d("On connected ${room.id} ")
                showDisconnectError = true
                _onRoomConnected.trySend(Unit)
            }

            override fun onConnecting(room: ChatRoom) {
                Timber.d("On connecting ${room.id}")
            }

            override fun onDisconnected(room: ChatRoom, reason: DisconnectReason) {
                Timber.d("On disconnected ${room.id} ${reason.name}")
                if (showDisconnectError) {
                    _onError.trySend(ChatSdkError.RAW_ERROR.apply {
                        rawError = reason.name
                    })
                }
            }

            override fun onEventReceived(room: ChatRoom, event: ChatEvent) {
                Timber.d("On event received ${room.id} ${event.eventName}")
            }

            override fun onMessageDeleted(room: ChatRoom, event: DeleteMessageEvent) {
                Timber.d("On message deleted ${room.id} ${event.attributes}")
                _messages.updateList { remove(find { it.id == event.messageId }) }
            }

            override fun onMessageReceived(room: ChatRoom, message: ChatMessage) {
                Timber.d("Message: $message")

                _messages.updateList {
                    // Per the business requirement of only keeping 50 chat messages in
                    if (count() == 50) {
                        removeAt(0)
                    }
                    add(message.toStageChatMessage())
                }
            }

            override fun onUserDisconnected(room: ChatRoom, event: DisconnectUserEvent) {
                Timber.d("On user disconnected $userId ${event.userId}")
                if (userId == event.userId) {
                    _onLocalKicked.trySend(Unit)
                    showDisconnectError = false
                } else {
                    _onRemoteKicked.trySend(event.userId)
                }
            }
        }
        chatRoom?.logLevel = ChatLogLevel.INFO
        chatRoom?.listener = roomListener
        connect()
    }

    /**
     * Connection must be done in OnResume
     */
    fun connect() {
        Timber.d("Connecting to chat: ${chatRoom?.state}")
        chatRoom?.connect()
    }

    fun sendMessage(chatMessageRequest: SendMessageRequest) {
        if (chatRoom?.state != ChatRoom.State.CONNECTED) {
            _onError.trySend(ChatSdkError.MESSAGE_SEND_FAILED)
            return
        }
        Timber.d("Sending message: $chatMessageRequest")
        chatRoom?.sendMessage(chatMessageRequest, object : SendMessageCallback {
            override fun onConfirmed(request: SendMessageRequest, response: ChatMessage) {
                Timber.d("Message sent: ${request.requestId} ${response.content}")
            }

            override fun onRejected(request: SendMessageRequest, error: ChatError) {
                Timber.d("Message send rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(ChatSdkError.MESSAGE_SEND_FAILED.apply {
                    this.rawCode = error.errorCode
                    this.rawError = error.errorMessage
                })
            }
        })
    }

    fun deleteMessage(id: String) {
        if (chatRoom?.state != ChatRoom.State.CONNECTED) {
            _onError.trySend(ChatSdkError.MESSAGE_DELETE_FAILED)
            return
        }
        Timber.d("Deleting message: $id")
        chatRoom?.deleteMessage(DeleteMessageRequest(id), object : DeleteMessageCallback {
            override fun onConfirmed(request: DeleteMessageRequest, response: DeleteMessageEvent) {
                Timber.d("Message deleted: ${request.requestId} ${response.attributes}")
            }

            override fun onRejected(request: DeleteMessageRequest, error: ChatError) {
                Timber.d("Delete message rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(ChatSdkError.MESSAGE_DELETE_FAILED.apply {
                    this.rawCode = error.errorCode
                    this.rawError = error.errorMessage
                })
            }
        })
    }

    fun disconnectUser(userId: String) {
        if (chatRoom?.state != ChatRoom.State.CONNECTED) {
            _onError.trySend(ChatSdkError.CONNECTION_FAILED)
            return
        }
        Timber.d("Disconnecting user: $userId")
        chatRoom?.disconnectUser(DisconnectUserRequest(userId), object : DisconnectUserCallback {
            override fun onConfirmed(
                request: DisconnectUserRequest,
                response: DisconnectUserEvent
            ) {
                Timber.d("User disconnected: ${request.requestId} ${response.attributes}")
            }

            override fun onRejected(request: DisconnectUserRequest, error: ChatError) {
                Timber.d("Disconnect user rejected: ${request.requestId} ${error.errorMessage}")
                _onError.trySend(ChatSdkError.RAW_ERROR.apply {
                    this.rawCode = error.errorCode
                    this.rawError = error.errorMessage
                })
            }
        })
    }

    fun onResume() {
        Timber.d("View resumed: connect to room")
        if (!isConnected) {
            chatRoom?.connect()
        }
    }

    private fun clearPreviousChat() {
        chatRoom?.disconnect()
        chatRoom = null
        roomListener = null
        _messages.update { emptyList() }
    }
}
