package com.amazon.ivs.stagebroadcastmanager.extensions

import com.amazon.ivs.stagebroadcastmanager.models.ConnectedParticipant
import com.amazonaws.ivs.broadcast.Device
import com.amazonaws.ivs.broadcast.DeviceDiscovery
import com.amazonaws.ivs.broadcast.ParticipantInfo

fun DeviceDiscovery.getSortedCameraDeviceList(): List<Device> =
    listLocalDevices().sortedBy { it.descriptor.deviceId }.filter {
        it.descriptor.type == Device.Descriptor.DeviceType.CAMERA
    }

fun List<ConnectedParticipant>.indexOrNull(participantInfo: ParticipantInfo) = indexOfFirst {
    it.id == participantInfo.participantId
}.takeIf { it != -1 }

fun List<ConnectedParticipant>.indexOrNull(participant: ConnectedParticipant) = indexOfFirst {
    it.id == participant.id
}.takeIf { it != -1 }
