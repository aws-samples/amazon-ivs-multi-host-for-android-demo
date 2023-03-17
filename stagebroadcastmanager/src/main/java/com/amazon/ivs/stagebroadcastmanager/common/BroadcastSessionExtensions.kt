package com.amazon.ivs.stagebroadcastmanager.common

import com.amazonaws.ivs.broadcast.BroadcastConfiguration
import com.amazonaws.ivs.broadcast.BroadcastSession

private const val DEFAULT_ROW_COUNT = 2
private const val DEFAULT_COLUMN_COUNT = 2
private const val TOP_OFFSET = 0
private const val BOTTOM_OFFSET = 0

// The Slot Layout controls how the video feed will look like with the playback URL
fun BroadcastSession.updateSlotLayout(participantIds: List<String>) {
    val size = participantIds.size
    val videoSizeY = STAGE_SIZE.y - TOP_OFFSET - BOTTOM_OFFSET
    val numberOfColumns = if (size > 2) DEFAULT_COLUMN_COUNT else 1
    val topOffset = if (size > 1) TOP_OFFSET else 0
    val width = when (size) {
        1 -> STAGE_SIZE.x
        else -> STAGE_SIZE.x / numberOfColumns
    }
    val firstWidth = when (size) {
        1, 2, 3 -> STAGE_SIZE.x
        else -> STAGE_SIZE.x / numberOfColumns
    }
    val height = when (size) {
        1 -> STAGE_SIZE.y
        else -> videoSizeY / DEFAULT_ROW_COUNT
    }

    var index = 0
    repeat(DEFAULT_ROW_COUNT) { currentRow ->
        for (currentColumn in 0 until numberOfColumns) {
            if (index >= size) return

            val participantId = participantIds[index]
            mixer?.slots?.find { it.name == participantId }?.let { participantSlot ->
                participantSlot.changing { slot ->
                    slot.name = participantId
                    slot.aspect = BroadcastConfiguration.AspectMode.FILL
                    slot.size = BroadcastConfiguration.Vec2(if (index == 0) firstWidth else width, height)
                    slot.position = BroadcastConfiguration.Vec2(
                        currentColumn * width,
                        topOffset + currentRow * height
                    )
                    slot.setzIndex(index + 1)
                    slot.gain = 1f
                    slot
                }.run {
                    mixer?.transition(participantId, this, 0.0, null)
                    index += 1
                }
            }

            // Makes the first view span both columns if the size is 3
            if (size == 3 && index == 1) break
        }
    }
}
