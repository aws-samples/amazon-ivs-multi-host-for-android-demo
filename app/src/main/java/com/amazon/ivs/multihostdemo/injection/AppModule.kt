package com.amazon.ivs.multihostdemo.injection

import android.content.Context
import com.amazon.ivs.multihostdemo.repository.cache.PreferenceProvider
import com.amazon.ivs.multihostdemo.repository.networking.Endpoints
import com.amazon.ivs.multihostdemo.repository.networking.NetworkClient
import com.amazon.ivs.stagebroadcastmanager.StageBroadcastManager
import com.amazon.ivs.stagebroadcastmanager.StageBroadcastManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAPI(): Endpoints = NetworkClient().api

    @Provides
    @Singleton
    fun provideStageBroadcastManager(
        @ApplicationContext context: Context,
        preferenceProvider: PreferenceProvider
    ): StageBroadcastManager = StageBroadcastManagerImpl(
        context = context,
        localUsername = preferenceProvider.username,
        localAvatarUrl = preferenceProvider.avatarUrl
    )
}
