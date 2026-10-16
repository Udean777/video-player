package app.subtle.player.domain.model

enum class ThemeMode { System, Light, Dark }

/** App-wide settings, remembered across sessions. */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val forceSoftwareDecoding: Boolean = false,
)
