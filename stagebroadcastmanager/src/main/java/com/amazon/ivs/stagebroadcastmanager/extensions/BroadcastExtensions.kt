package com.amazon.ivs.stagebroadcastmanager.extensions

import com.amazon.ivs.stagebroadcastmanager.common.MOCK_AVATAR_URL
import com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant
import com.amazonaws.ivs.broadcast.ParticipantInfo

fun ParticipantInfo.asParticipantItem(
    isLocalVideoStopped: Boolean,
    isLocalAudioMuted: Boolean,
    isRemoteVideoStopped: Boolean,
    isRemoteAudioMuted: Boolean
) = ConnectedParticipant(
    id = participantId,
    name = getUsername(),
    iconUrl = userInfo["avatarUrl"] ?: MOCK_AVATAR_URL,
    isLocal = isLocal,
    isRemoteVideoStopped = isRemoteVideoStopped,
    isRemoteAudioMuted = isRemoteAudioMuted,
    isLocalVideoStopped = isLocalVideoStopped,
    isLocalAudioMuted = isLocalAudioMuted,
)

fun ParticipantInfo.getUsername(): String = userInfo["username"] ?: "unknown"
