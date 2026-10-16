package app.subtle.player.di

import app.subtle.player.data.repository.FileStorageRepository
import app.subtle.player.data.repository.PreferencesPlaybackProgressRepository
import app.subtle.player.data.storage.AndroidStorageAccess
import app.subtle.player.data.storage.StorageAccess
import app.subtle.player.domain.repository.PlaybackProgressRepository
import app.subtle.player.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindStorageRepository(impl: FileStorageRepository): StorageRepository

    @Binds
    abstract fun bindPlaybackProgressRepository(
        impl: PreferencesPlaybackProgressRepository,
    ): PlaybackProgressRepository

    @Binds
    @Singleton
    abstract fun bindStorageAccess(impl: AndroidStorageAccess): StorageAccess
}
