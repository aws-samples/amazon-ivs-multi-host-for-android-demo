package com.amazon.ivs.stagebroadcastmanager.models

import com.amazonaws.ivs.broadcast.Device

data class ConnectedParticipant(
    val id: String,
    val name: String,
    val iconUrl: String,
    val isLocal: Boolean,
    var isRemoteVideoStopped: Boolean,
    var isRemoteAudioMuted: Boolean,
    var isLocalVideoStopped: Boolean,
    var isLocalAudioMuted: Boolean,
    var device: Device? = null
) {
    val isVideoOff get() = isRemoteVideoStopped || isLocalVideoStopped
}
