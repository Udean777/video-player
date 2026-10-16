package app.subtle.player.di

import app.subtle.player.data.repository.CachedSubtitleRepository
import app.subtle.player.data.repository.FileStorageRepository
import app.subtle.player.data.repository.PreferencesPlaybackProgressRepository
import app.subtle.player.data.repository.PreferencesSettingsRepository
import app.subtle.player.data.repository.PreferencesVideoPreferencesRepository
import app.subtle.player.data.storage.AndroidStorageAccess
import app.subtle.player.data.storage.StorageAccess
import app.subtle.player.domain.repository.PlaybackProgressRepository
import app.subtle.player.domain.repository.SettingsRepository
import app.subtle.player.domain.repository.StorageRepository
import app.subtle.player.domain.repository.SubtitleRepository
import app.subtle.player.domain.repository.VideoPreferencesRepository
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
    abstract fun bindSubtitleRepository(impl: CachedSubtitleRepository): SubtitleRepository

    @Binds
    abstract fun bindVideoPreferencesRepository(
        impl: PreferencesVideoPreferencesRepository,
    ): VideoPreferencesRepository

    @Binds
    abstract fun bindSettingsRepository(impl: PreferencesSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindStorageAccess(impl: AndroidStorageAccess): StorageAccess
}
