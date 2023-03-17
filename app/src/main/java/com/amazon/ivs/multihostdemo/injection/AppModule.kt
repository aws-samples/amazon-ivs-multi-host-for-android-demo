package com.amazon.ivs.multihostdemo.injection

import android.content.Context
import com.amazon.ivs.multihostdemo.common.extensions.ioScope
import com.amazon.ivs.multihostdemo.common.ivs.chat.ChatManager
import com.amazon.ivs.multihostdemo.common.ivs.chat.ChatManagerImpl
import com.amazon.ivs.multihostdemo.repository.StageRepository
import com.amazon.ivs.multihostdemo.repository.StageRepositoryImpl
import com.amazon.ivs.multihostdemo.repository.UserRepository
import com.amazon.ivs.multihostdemo.repository.UserRepositoryImpl
import com.amazon.ivs.multihostdemo.repository.cache.SecuredPreferenceProvider
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
    fun provideSecuredPrefs(@ApplicationContext context: Context) = SecuredPreferenceProvider(context)

    @Provides
    @Singleton
    fun provideChatManager(): ChatManager = ChatManagerImpl()

    @Provides
    @Singleton
    fun provideUserRepository(
        securedPreferenceProvider: SecuredPreferenceProvider,
    ): UserRepository = UserRepositoryImpl(securedPreferenceProvider)

    @Provides
    @Singleton
    fun provideStageRepository(
        api: Endpoints,
        securedPreferenceProvider: SecuredPreferenceProvider,
    ): StageRepository = StageRepositoryImpl(api, securedPreferenceProvider, ioScope)

    @Provides
    @Singleton
    fun provideStageBroadcastManager(
        @ApplicationContext context: Context,
        securedPreferenceProvider: SecuredPreferenceProvider
    ): StageBroadcastManager =
        StageBroadcastManagerImpl(context, securedPreferenceProvider.username, securedPreferenceProvider.avatarUrl)
}
