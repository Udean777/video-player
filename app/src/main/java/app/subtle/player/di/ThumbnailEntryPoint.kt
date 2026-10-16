package app.subtle.player.di

import app.subtle.player.data.storage.VideoThumbnailLoader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets composables reach the app-scoped thumbnail cache without a ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThumbnailEntryPoint {
    fun videoThumbnailLoader(): VideoThumbnailLoader
}
