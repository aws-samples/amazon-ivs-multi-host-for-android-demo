package com.amazon.ivs.multihostdemo.ui.stage_list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.amazon.ivs.multihostdemo.common.extensions.launch
import com.amazon.ivs.multihostdemo.repository.StageRepository
import com.amazon.ivs.multihostdemo.ui.stage.StageNavArgs
import com.amazon.ivs.stagebroadcastmanager.StageBroadcastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class JoinPreviewPopoverViewModel @Inject constructor(
    private val repository: StageRepository,
    private val broadcastManager: StageBroadcastManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.get<String>("groupId")!!
    private val stageName = savedStateHandle.get<String>("stageName")!!
    private val _isLoading = MutableStateFlow(false)
    private val _onNavigateToStage = Channel<StageNavArgs>()

    val isLoading = _isLoading.asStateFlow()
    val onNavigateToStage = _onNavigateToStage.receiveAsFlow()
    val broadcastEvents = broadcastManager.stageEvents
    val onMicrophoneMuted = broadcastManager.onMicrophoneMuted
    val onVideoMuted = broadcastManager.onVideoMuted

    init {
        broadcastManager.createBroadcastSession(true)
    }

    fun toggleMute() {
        broadcastManager.toggleAudio()
    }

    fun toggleVideo() {
        broadcastManager.toggleVideo()
    }

    fun flipCameraDirection() = launch {
        broadcastManager.flipLocalCameraDirection()
    }

    fun joinStage() = launch {
        _isLoading.update { true }
        repository.joinStage(groupId).fold(
            onSuccess = { stageJoinResponse ->
                _onNavigateToStage.send(
                    StageNavArgs(
                        stageId = groupId,
                        chatId = stageJoinResponse.chat.id,
                        token = stageJoinResponse.stage.token.token,
                        chatToken = stageJoinResponse.chat.token.token,
                        hostName = stageName,
                        isHost = false,
                        sessionExpirationTime = stageJoinResponse.chat.token.sessionExpirationTime,
                        tokenExpirationTime = stageJoinResponse.chat.token.tokenExpirationTime,
                    )
                )
                _isLoading.update { false }
            },
            onFailure = {
                _isLoading.update { false }
                Timber.w("Failed to join stage: ${it.message}")
            }
        )
    }

    fun reloadCameraPreview() {
        broadcastManager.reloadCameraPreview()
    }
}
