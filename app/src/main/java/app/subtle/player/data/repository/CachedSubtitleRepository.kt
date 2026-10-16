package app.subtle.player.data.repository

import app.subtle.player.data.storage.SubtitleCache
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.repository.SubtitleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedSubtitleRepository @Inject constructor(
    private val cache: SubtitleCache,
) : SubtitleRepository {

    override suspend fun prepareForPlayback(subtitle: SubtitleFile, charsetOverride: String?): String =
        cache.prepare(subtitle.uri, subtitle.displayName, charsetOverride)
}
