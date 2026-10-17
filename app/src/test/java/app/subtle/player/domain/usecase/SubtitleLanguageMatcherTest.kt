package app.subtle.player.domain.usecase

import app.subtle.player.domain.model.SubtitleLanguage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleLanguageMatcherTest {

    @Test
    fun matchesEnglishByCodeAndName() {
        assertTrue(SubtitleLanguageMatcher.matches(SubtitleLanguage.English, "Track 1 - [English]"))
        assertTrue(SubtitleLanguageMatcher.matches(SubtitleLanguage.English, "eng"))
        assertTrue(SubtitleLanguageMatcher.matches(SubtitleLanguage.English, "en"))
    }

    @Test
    fun matchesIndonesianByCodeAndName() {
        assertTrue(SubtitleLanguageMatcher.matches(SubtitleLanguage.Indonesian, "bahasa indonesia"))
        assertTrue(SubtitleLanguageMatcher.matches(SubtitleLanguage.Indonesian, "id"))
    }

    @Test
    fun rejectsUnrelatedOrEmptyHints() {
        assertFalse(SubtitleLanguageMatcher.matches(SubtitleLanguage.English, "scene"))
        assertFalse(SubtitleLanguageMatcher.matches(SubtitleLanguage.English, null))
        assertFalse(SubtitleLanguageMatcher.matches(SubtitleLanguage.None, "en"))
    }
}
