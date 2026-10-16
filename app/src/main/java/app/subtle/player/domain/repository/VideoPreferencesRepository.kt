package app.subtle.player.domain.repository

import app.subtle.player.domain.model.VideoPreferences

interface VideoPreferencesRepository {

    suspend fun get(videoUri: String): VideoPreferences

    suspend fun save(videoUri: String, preferences: VideoPreferences)
}
