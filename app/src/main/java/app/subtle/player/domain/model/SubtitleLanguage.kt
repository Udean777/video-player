package app.subtle.player.domain.model

/** Preferred subtitle language. [None] leaves track selection to the engine. */
enum class SubtitleLanguage(val code: String?) {
    None(null),
    English("en"),
    Indonesian("id"),
}
