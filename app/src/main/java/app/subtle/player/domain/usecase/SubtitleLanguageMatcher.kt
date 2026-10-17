package app.subtle.player.domain.usecase

import app.subtle.player.domain.model.SubtitleLanguage

/**
 * Picks a subtitle track by language. Pure Kotlin, unit-tested.
 *
 * Matching is token based so a track name like "Track 2 - English" or "[eng]" is recognised, while
 * unrelated words that merely contain a code ("scene") are not.
 */
object SubtitleLanguageMatcher {

    private val ALIASES = mapOf(
        SubtitleLanguage.English to setOf("en", "eng", "english"),
        SubtitleLanguage.Indonesian to setOf("id", "ind", "indonesian", "bahasa"),
    )

    fun matches(language: SubtitleLanguage, hint: String?): Boolean {
        val aliases = ALIASES[language] ?: return false
        val tokens = hint
            ?.lowercase()
            ?.split(Regex("[^a-z0-9]+"))
            ?.filter { it.isNotBlank() }
            .orEmpty()
        return tokens.any { it in aliases }
    }
}
