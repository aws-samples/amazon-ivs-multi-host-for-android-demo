package com.amazon.ivs.multihostdemo.ui.stage

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.amazon.ivs.multihostdemo.common.extensions.launch
import com.amazon.ivs.multihostdemo.common.extensions.toDate
import com.amazon.ivs.multihostdemo.common.ivs.chat.ChatManager
import com.amazon.ivs.multihostdemo.repository.StageRepository
import com.amazon.ivs.multihostdemo.repository.UserRepository
import com.amazon.ivs.stagebroadcastmanager.StageBroadcastManager
import com.amazon.ivs.stagebroadcastmanager.models.BroadcastState
import com.amazonaws.ivs.chat.messaging.ChatToken
import com.amazonaws.ivs.chat.messaging.requests.SendMessageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class StageViewModel @Inject constructor(
    private val stageRepository: StageRepository,
    private val broadcastManager: StageBroadcastManager,
    private val chatManager: ChatManager,
    savedStateHandle: SavedStateHandle,
    userRepository: UserRepository,
) : ViewModel() {
    private val navArgs = savedStateHandle.get<StageNavArgs>("stageNavArgs")!!
    private val stageId = navArgs.stageId
    private val chatId = navArgs.chatId
    private val token = navArgs.token
    private val chatToken = navArgs.chatToken
    private val sessionExpirationTime = navArgs.sessionExpirationTime.toDate()
    private val tokenExpirationTime = navArgs.tokenExpirationTime.toDate()

    private val _isChatOpen = MutableStateFlow(true)
    private val _isClosingStage = MutableStateFlow(false)

    val isHost = navArgs.isHost
    val hostName = navArgs.hostName
    val isChatOpen = _isChatOpen.asStateFlow()
    val userAvatar = userRepository.userAvatar
    val isClosingStage = _isClosingStage.asStateFlow()
    val connectedParticipants = broadcastManager.connectedParticipants
    val broadcastEvents = broadcastManager.stageEvents
    val broadcastState = broadcastManager.broadcastState
    val onMicrophoneMuted = broadcastManager.onMicrophoneMuted
    val onVideoMuted = broadcastManager.onVideoMuted
    val messages = chatManager.messages

    init {
        launch {
            broadcastManager.createBroadcastSession()
            broadcastManager.joinStage(token)
        }
        launch {
            chatManager.joinRoom(chatId, ChatToken(chatToken, sessionExpirationTime, tokenExpirationTime))
        }
    }

    fun toggleMute() {
        broadcastManager.toggleAudio()
    }

    fun toggleVideo() {
        broadcastManager.toggleVideo()
    }

    fun toggleChat() {
        _isChatOpen.update { !it }
    }

    fun flipCameraDirection() = launch {
        broadcastManager.flipLocalCameraDirection()
    }

    fun toggleBroadcasting() {
        val streamKey = navArgs.streamKey!!
        val ingestEndpoint = navArgs.ingestEndpoint

        when (broadcastState.value) {
            BroadcastState.BROADCASTING -> broadcastManager.stopBroadcast()
            BroadcastState.LOADING -> { /* Ignored */ }
            BroadcastState.NOT_BROADCASTING -> {
                Timber.d("Starting broadcast with $streamKey and $ingestEndpoint")
                broadcastManager.startBroadcast(streamKey, "rtmps://$ingestEndpoint")
            }
        }
    }

    fun leaveStage() {
        _isClosingStage.update { true }
        if (isHost) {
            Timber.d("Deleting stage $stageId")
            deleteStage()
        } else {
            broadcastManager.release()
        }
    }

    fun sendMessage(message: String) {
        Timber.d("Sending message: $message")
        val attrs = mutableMapOf<String, String>()
        attrs["type"] = "MESSAGE"
        attrs["userId"] = stageId

        chatManager.sendMessage(SendMessageRequest(message, attrs))
    }

    fun reloadCameraPreview() {
        broadcastManager.reloadCameraPreview()
    }

    fun removeParticipant(participant: com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant) {
        launch {
            if (isHost && participant.isLocal) {
                deleteStage()
            } else {
                stageRepository.removeParticipant(stageId, participant.id).fold(
                    onSuccess = {
                        Timber.d("Disconnected participant!")
                    },
                    onFailure = { error ->
                        Timber.w(error, "Couldn't kick participant - ${error.message}")
                    }
                )
            }
        }
    }

    fun muteParticipant(participant: com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant) {
        if (participant.isLocal) {
            toggleMute()
        } else {
            broadcastManager.toggleMuteParticipant(participant)
        }
    }

    fun hideVideoOfParticipant(participant: com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant) {
        if (participant.isLocal) {
            toggleVideo()
        } else {
            broadcastManager.hideVideoOfParticipant(participant)
        }
    }

    private fun deleteStage() = launch {
        // If you won't stop broadcast before deleting - it can lead to internal errors
        broadcastManager.stopBroadcast()
        stageRepository.deleteStage(stageId).fold(
            onSuccess = {
                broadcastManager.release()
            },
            onFailure = { error ->
                Timber.d("Couldn't delete the stage - ${error.message}")
            }
        )
    }
}
