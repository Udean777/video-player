package app.subtle.player.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFileFilterTest {

    @Test
    fun videoMimeIsAccepted() {
        assertTrue(isVideoFile("video/mp4", "anything.bin"))
        assertTrue(isVideoFile("video/x-matroska", "movie"))
    }

    @Test
    fun knownExtensionsAreAcceptedWithoutMime() {
        assertTrue(isVideoFile(null, "movie.mkv"))
        assertTrue(isVideoFile("application/octet-stream", "clip.MP4"))
        assertTrue(isVideoFile(null, "episode.m2ts"))
        assertTrue(isVideoFile(null, "recording.3gp"))
    }

    @Test
    fun nonVideoIsRejected() {
        assertFalse(isVideoFile("image/jpeg", "photo.jpg"))
        assertFalse(isVideoFile("audio/mpeg", "song.mp3"))
        assertFalse(isVideoFile(null, "cover.jpg"))
        assertFalse(isVideoFile(null, "subtitle.srt"))
        assertFalse(isVideoFile(null, "noextension"))
        assertFalse(isVideoFile("application/pdf", "doc.pdf"))
    }
}
