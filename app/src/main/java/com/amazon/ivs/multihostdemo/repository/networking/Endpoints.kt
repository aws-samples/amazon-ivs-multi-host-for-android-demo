package com.amazon.ivs.multihostdemo.repository.networking

import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.CreateStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.DeleteStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.DisconnectRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.requests.host.JoinStageRequest
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.StageResponse
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.host.CreateStageResponse
import com.amazon.ivs.multihostdemo.repository.networking.models.responses.host.JoinStageResponse
import retrofit2.Response
import retrofit2.http.*

interface Endpoints {
    @POST("create")
    suspend fun createStage(@Body request: CreateStageRequest): CreateStageResponse

    @POST("list")
    suspend fun getStageList(): List<StageResponse>

    @HTTP(method = "DELETE", path = "delete", hasBody = true)
    suspend fun deleteStage(@Body request: DeleteStageRequest): Response<Unit>

    @POST("join")
    suspend fun joinStage(@Body request: JoinStageRequest): JoinStageResponse

    @POST("disconnect")
    suspend fun disconnect(@Body request: DisconnectRequest): Response<Unit>
}
