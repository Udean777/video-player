package app.subtle.player.domain.model

enum class SubtitleSize { Small, Normal, Big, Huge }

enum class SubtitleColor(val rgb: Int) {
    White(0xFFFFFF),
    Yellow(0xFFFF00),
    Cyan(0x00FFFF),
}

enum class SubtitleBackground { None, Subtle, Solid }

/**
 * Text-subtitle appearance. The engine reads this when it is created, so a change applies to the
 * next playback session. Only text subtitles are affected (not ASS "file style" or image subtitles).
 */
data class SubtitleStyle(
    val size: SubtitleSize = SubtitleSize.Normal,
    val color: SubtitleColor = SubtitleColor.White,
    val outline: Boolean = true,
    val background: SubtitleBackground = SubtitleBackground.None,
    val assUseMyStyle: Boolean = false,
)
