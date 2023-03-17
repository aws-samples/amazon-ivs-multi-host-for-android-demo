package com.amazon.ivs.multihostdemo.repository.models

import com.amazon.ivs.multihostdemo.common.MOCK_AVATAR_URL

data class Stage(
    val groupId: String,
    val stageId: String,
    val name: String,
    val iconUrl: String = MOCK_AVATAR_URL,
)
