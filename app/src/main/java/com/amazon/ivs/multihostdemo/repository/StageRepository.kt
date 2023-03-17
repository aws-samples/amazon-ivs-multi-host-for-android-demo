package com.amazon.ivs.multihostdemo.repository

import com.amazon.ivs.multihostdemo.repository.cache.SecuredPreferenceProvider
import com.amazon.ivs.multihostdemo.repository.networking.Endpoints
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.UserAttributes
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.CreateStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.DeleteStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.DisconnectRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.JoinStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.StageResponse
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.host.CreateStageResponse
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.host.JoinStageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

interface StageRepository {
    suspend fun deleteStage(stageId: String): Result<Unit>
    suspend fun createStage(): Result<CreateStageResponse>
    suspend fun getStages(): Result<List<StageResponse>>
    suspend fun joinStage(groupId: String): Result<JoinStageResponse>

    suspend fun removeParticipant(stageId: String, participantId: String): Result<Unit>
}

class StageRepositoryImpl(
    private val api: Endpoints,
    private val securedPreferenceProvider: SecuredPreferenceProvider,
    private val ioScope: CoroutineScope,
) : StageRepository {
    init {
        ensureCreatedStageWasDeleted()
    }

    override suspend fun deleteStage(stageId: String) = runCatching {
        Timber.d("Deleting stage $stageId")
        api.deleteStage(DeleteStageRequest(stageId))
        Timber.d("Deleted stage $stageId")
        securedPreferenceProvider.stageIdToDeleteOnExit = null
    }

    override suspend fun createStage() = runCatching {
        val userId = securedPreferenceProvider.userId!!
        Timber.d("Creating stage [${securedPreferenceProvider.username!!}'s Stage]")
        val response = api.createStage(
            CreateStageRequest(
                userId,
                UserAttributes(
                    username = securedPreferenceProvider.username!!,
                    avatarUrl = securedPreferenceProvider.avatarUrl!!
                )
            )
        )
        securedPreferenceProvider.stageIdToDeleteOnExit = response.groupId
        response
    }

    override suspend fun getStages() = runCatching {
        api.getStageList()
    }

    override suspend fun joinStage(groupId: String) = runCatching {
        api.joinStage(
            JoinStageRequest(
                groupId = groupId,
                userId = securedPreferenceProvider.userId!!,
                attributes = UserAttributes(
                    username = securedPreferenceProvider.username!!,
                    avatarUrl = securedPreferenceProvider.avatarUrl!!,
                )
            )
        )
    }

    override suspend fun removeParticipant(stageId: String, participantId: String): Result<Unit> = runCatching {
        api.disconnect(
            DisconnectRequest(
                groupId = stageId,
                participantId = participantId,
                reason = "Kicked from stage",
                userId = securedPreferenceProvider.userId!!
            )
        )
    }

    private fun ensureCreatedStageWasDeleted() {
        ioScope.launch {
            val stageIdToDeleteOnExit = securedPreferenceProvider.stageIdToDeleteOnExit ?: return@launch
            deleteStage(stageIdToDeleteOnExit)
        }
    }
}
