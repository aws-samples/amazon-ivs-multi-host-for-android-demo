package com.amazon.ivs.multihostdemo.repository

import com.amazon.ivs.multihostdemo.common.AVATARS
import com.amazon.ivs.multihostdemo.repository.cache.PreferenceProvider
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val preferenceProvider: PreferenceProvider,
) {
    val username get() = preferenceProvider.username ?: ""
    val userAvatar
        get() = AVATARS.firstOrNull { it.url == preferenceProvider.avatarUrl } ?: AVATARS[0]

    fun saveUserName(username: String) {
        preferenceProvider.username = username
    }

    fun saveAvatarUrl(url: String) {
        preferenceProvider.avatarUrl = url
    }

    fun generateUserId() {
        preferenceProvider.userId = UUID.randomUUID()
            .toString()
            .replace("-", "")
    }
}
