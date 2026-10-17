package app.subtle.player.domain.model

enum class ThemeMode { System, Light, Dark }

/** App-wide settings, remembered across sessions. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val forceSoftwareDecoding: Boolean = false,
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val subtitleLanguage: SubtitleLanguage = SubtitleLanguage.None,
    val autoPlayNext: Boolean = true,
    val language: AppLanguage = AppLanguage.System,
)

/** In-app language. [System] follows the device setting. */
enum class AppLanguage(val tag: String?) {
    System(null),
    English("en"),
    Indonesian("id"),
}
