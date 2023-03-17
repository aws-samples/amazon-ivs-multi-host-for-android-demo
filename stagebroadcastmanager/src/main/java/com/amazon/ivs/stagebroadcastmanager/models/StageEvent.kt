package com.amazon.ivs.stagebroadcastmanager.models

import android.view.View

sealed interface StageEvent {
    data class AddParticipantView(
        val viewToAdd: View?,
        val participantId: String,
        val name: String,
        val avatar: String,
        val isVideoOff: Boolean,
    ) : StageEvent

    data class RemoveParticipantView(val participantId: String) : StageEvent
    data class PreviewUpdated(val updatedView: View, val participantId: String) : StageEvent
    object StageClosed : StageEvent
    object StageDisconnected : StageEvent
}
