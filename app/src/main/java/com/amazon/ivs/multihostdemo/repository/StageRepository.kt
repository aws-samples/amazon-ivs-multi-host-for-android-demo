package com.amazon.ivs.multihostdemo.repository

import com.amazon.ivs.multihostdemo.common.extensions.ioScope
import com.amazon.ivs.multihostdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.multihostdemo.repository.networking.Endpoints
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.UserAttributes
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.CreateStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.DeleteStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.DisconnectRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.JoinStageRequest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StageRepository @Inject constructor(
    private val api: Endpoints,
    private val preferenceProvider: PreferenceProvider,
) {
    init {
        ensureCreatedStageWasDeleted()
    }

    suspend fun deleteStage(stageId: String) = runCatching {
        Timber.d("Deleting stage $stageId")
        api.deleteStage(DeleteStageRequest(stageId))
        Timber.d("Deleted stage $stageId")
        preferenceProvider.stageIdToDeleteOnExit = null
    }

    suspend fun createStage() = runCatching {
        val userId = preferenceProvider.userId!!
        Timber.d("Creating stage [${preferenceProvider.username!!}'s Stage]")
        val response = api.createStage(
            CreateStageRequest(
                userId,
                UserAttributes(
                    username = preferenceProvider.username!!,
                    avatarUrl = preferenceProvider.avatarUrl!!
                )
            )
        )
        preferenceProvider.stageIdToDeleteOnExit = response.groupId
        response
    }

    suspend fun getStages() = runCatching {
        api.getStageList()
    }

    suspend fun joinStage(groupId: String) = runCatching {
        api.joinStage(
            JoinStageRequest(
                groupId = groupId,
                userId = preferenceProvider.userId!!,
                attributes = UserAttributes(
                    username = preferenceProvider.username!!,
                    avatarUrl = preferenceProvider.avatarUrl!!,
                )
            )
        )
    }

    suspend fun removeParticipant(stageId: String, participantId: String): Result<Unit> = runCatching {
        api.disconnect(
            DisconnectRequest(
                groupId = stageId,
                participantId = participantId,
                reason = "Kicked from stage",
                userId = preferenceProvider.userId!!
            )
        )
    }

    private fun ensureCreatedStageWasDeleted() {
        ioScope.launch {
            val stageIdToDeleteOnExit = preferenceProvider.stageIdToDeleteOnExit ?: return@launch
            deleteStage(stageIdToDeleteOnExit)
        }
    }
}
