package com.amazon.ivs.multihostdemo.ui.stage_list

import androidx.lifecycle.ViewModel
import com.amazon.ivs.multihostdemo.common.extensions.launch
import com.amazon.ivs.multihostdemo.repository.StageRepository
import com.amazon.ivs.multihostdemo.repository.UserRepository
import com.amazon.ivs.multihostdemo.repository.models.Stage
import com.amazon.ivs.multihostdemo.ui.stage.StageNavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StageListViewModel @Inject constructor(
    private val repository: StageRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _stageList = MutableStateFlow<List<Stage>?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _isRefreshingStages = MutableStateFlow(false)
    private val _onNavigateToStage = Channel<StageNavArgs>()

    val stageList = _stageList.asStateFlow()
    val isLoading = _isLoading.asStateFlow()
    val isRefreshingStages = _isRefreshingStages.asStateFlow()
    val onNavigateToStage = _onNavigateToStage.receiveAsFlow()

    fun getStages() = launch {
        _isRefreshingStages.update { true }
        repository.getStages().fold(
            onSuccess = { stageList ->
                _isRefreshingStages.update { false }
                Timber.d("Received new stages: $stageList")
                _stageList.update { stageList.map { it.asStageItem() } }
            },
            onFailure = { error ->
                _isRefreshingStages.update { false }
                Timber.e("Error retrieving stages: ${error.message}")
            }
        )
    }

    fun createStage() = launch {
        _isLoading.update { true }
        repository.createStage().fold(
            onSuccess = { hostStageResponse ->
                _onNavigateToStage.send(
                    StageNavArgs(
                        stageId = hostStageResponse.groupId,
                        chatId = hostStageResponse.chat.id,
                        token = hostStageResponse.stage.token.token,
                        chatToken = hostStageResponse.chat.token.token,
                        hostName = userRepository.username,
                        streamKey = hostStageResponse.channel.streamKey,
                        ingestEndpoint = hostStageResponse.channel.ingestEndpoint,
                        playbackUrl = hostStageResponse.channel.playbackUrl,
                        isHost = true,
                        sessionExpirationTime = hostStageResponse.chat.token.sessionExpirationTime,
                        tokenExpirationTime = hostStageResponse.chat.token.tokenExpirationTime,
                    )
                )
                _isLoading.update { false }
            },
            onFailure = { error ->
                _isLoading.update { false }
                Timber.e(error, "Failed to create stage")
            }
        )
    }

    fun deleteStage(stageId: String) = launch {
        _isRefreshingStages.update { true }
        repository.deleteStage(stageId).fold(
            onSuccess = {
                getStages()
            },
            onFailure = { error ->
                Timber.d("Couldn't delete the stage - ${error.message}")
                _isRefreshingStages.update { false }
            }
        )
    }
}
