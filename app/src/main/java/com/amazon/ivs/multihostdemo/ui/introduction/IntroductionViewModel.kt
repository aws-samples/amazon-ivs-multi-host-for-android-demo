package com.amazon.ivs.multihostdemo.ui.introduction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazon.ivs.multihostdemo.common.AVATARS
import com.amazon.ivs.multihostdemo.repository.UserRepository
import com.amazon.ivs.multihostdemo.repository.models.Avatar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class IntroductionViewModel @Inject constructor(
    private val repository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val selectedAvatarIndexKey = "selectedAvatarIndex"
    private val displayNameKey = "displayName"
    private val selectedAvatarIndex = savedStateHandle
        .getStateFlow(selectedAvatarIndexKey, AVATARS.indexOf(repository.userAvatar))

    val displayName = savedStateHandle.getStateFlow(displayNameKey, repository.username)
    val avatars = selectedAvatarIndex.map { selectedIndex ->
        val avatars = AVATARS
        avatars[selectedIndex].isSelected = true
        avatars
    }
    val isUserInfoSet = selectedAvatarIndex
        .combine(displayName) { _, _ -> isUserInfoSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), isUserInfoSet())

    fun selectAvatar(avatar: Avatar) {
        savedStateHandle[selectedAvatarIndexKey] = avatar.id
        repository.saveAvatarUrl(avatar.url)
    }

    fun setDisplayName(name: String) {
        savedStateHandle[displayNameKey] = name
        repository.saveUserName(name)
    }

    fun onSignIn() {
        repository.generateUserId()
    }

    private fun isUserInfoSet() = displayName.value.isNotBlank() && selectedAvatarIndex.value != -1
}
