package com.amazon.ivs.multihostdemo.common

import com.amazon.ivs.multihostdemo.repository.models.Avatar

const val PARTICIPANTS_POPUP_TAG = "participants_popup_tag"
const val TIMBER_TAG = "MultiHostDemo"
const val POPUP_TIMEOUT = 2 * 1000L
const val SELF_PARTICIPANT_ID = "self_participant_id"
const val MOCK_AVATAR_URL = "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/dog.png"
const val SOURCE_CODE_URL = "https://github.com/aws-samples/amazon-ivs-multi-host-for-android-demo"

val AVATARS
    get() = listOf(
        "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/dog.png",
        "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/dog2.png",
        "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/horse.png",
        "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/giraffe.png",
        "https://d39ii5l128t5ul.cloudfront.net/assets/animals_square/hedgehog.png",
    ).mapIndexed { index, url -> Avatar(index, url) }
