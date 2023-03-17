package com.amazon.ivs.multihostdemo.repository

import com.amazon.ivs.multihostdemo.common.AVATARS
import com.amazon.ivs.multihostdemo.repository.cache.SecuredPreferenceProvider
import com.amazon.ivs.multihostdemo.repository.models.Avatar
import java.util.UUID

interface UserRepository {
    val username: String
    val userAvatar: Avatar
    fun saveUserName(username: String)
    fun saveAvatarUrl(url: String)
    fun generateUserId()
}

class UserRepositoryImpl(
    private val securedPreferenceProvider: SecuredPreferenceProvider,
) : UserRepository {
    override val username get() = securedPreferenceProvider.username ?: ""
    override val userAvatar
        get() = AVATARS.firstOrNull { it.url == securedPreferenceProvider.avatarUrl } ?: AVATARS[0]

    override fun saveUserName(username: String) {
        securedPreferenceProvider.username = username
    }

    override fun saveAvatarUrl(url: String) {
        securedPreferenceProvider.avatarUrl = url
    }

    override fun generateUserId() {
        securedPreferenceProvider.userId = UUID.randomUUID()
            .toString()
            .replace("-", "")
    }
}
