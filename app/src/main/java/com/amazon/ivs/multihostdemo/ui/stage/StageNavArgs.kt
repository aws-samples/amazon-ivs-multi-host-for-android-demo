package com.amazon.ivs.multihostdemo.ui.stage

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class StageNavArgs(
    val stageId: String,
    val chatId: String,
    val token: String,
    val chatToken: String,
    val sessionExpirationTime: String,
    val tokenExpirationTime: String,
    val hostName: String,
    val isHost: Boolean,
    val streamKey: String? = null,
    val ingestEndpoint: String? = null,
    val playbackUrl: String? = null,
) : Parcelable
