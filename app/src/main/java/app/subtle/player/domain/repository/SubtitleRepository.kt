package app.subtle.player.domain.repository

import app.subtle.player.domain.model.SubtitleFile

interface SubtitleRepository {

    /**
     * Copies [subtitle] into app cache as UTF-8 text (or verbatim for image formats) and returns a
     * `file://` URI string that libVLC can read reliably.
     */
    suspend fun prepareForPlayback(subtitle: SubtitleFile, charsetOverride: String?): String
}
