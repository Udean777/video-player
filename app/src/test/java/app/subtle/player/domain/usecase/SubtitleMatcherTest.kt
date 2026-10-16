package app.subtle.player.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleMatcherTest {

    @Test
    fun exactBaseNameMatches() {
        assertTrue(SubtitleMatcher.matches("Movie.mkv", "Movie.srt"))
    }

    @Test
    fun languageSuffixMatches() {
        assertTrue(SubtitleMatcher.matches("Movie.mkv", "Movie.en.srt"))
        assertTrue(SubtitleMatcher.matches("Movie.mkv", "Movie.en.forced.srt"))
    }

    @Test
    fun commonPrefixDoesNotMatch() {
        assertFalse(SubtitleMatcher.matches("Movie.mkv", "Movie 2.srt"))
    }

    @Test
    fun matchingIsCaseInsensitive() {
        assertTrue(SubtitleMatcher.matches("Movie.mkv", "movie.SRT"))
    }

    @Test
    fun namesWithMultipleDotsMatch() {
        assertTrue(SubtitleMatcher.matches("My.Movie.2020.mkv", "My.Movie.2020.en.srt"))
    }

    @Test
    fun exactNameHasNoLanguageHint() {
        assertNull(SubtitleMatcher.languageHint("Movie.mkv", "Movie.srt"))
    }

    @Test
    fun languageSuffixBecomesHint() {
        assertEquals("en", SubtitleMatcher.languageHint("Movie.mkv", "Movie.en.srt"))
        assertEquals("eng.forced", SubtitleMatcher.languageHint("Movie.mkv", "Movie.eng.forced.srt"))
    }
}
