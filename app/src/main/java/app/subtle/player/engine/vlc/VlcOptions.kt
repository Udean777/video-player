package app.subtle.player.engine.vlc

import app.subtle.player.core.player.PlayerConfig
import app.subtle.player.domain.model.SubtitleBackground
import app.subtle.player.domain.model.SubtitleSize

/** libVLC option builder. Pure function so it stays unit-testable on the JVM. */
internal fun buildVlcArgs(config: PlayerConfig): ArrayList<String> =
    ArrayList<String>().apply {
        if (config.forceSoftwareDecoding) {
            add("--avcodec-hw=none")
        }
        val style = config.subtitleStyle
        add("--freetype-rel-fontsize=${style.size.relFontSize}")
        add("--freetype-color=${style.color.rgb}")
        add("--freetype-outline-thickness=${if (style.outline) 4 else 0}")
        if (style.background != SubtitleBackground.None) {
            add("--freetype-background-opacity=${style.background.opacity}")
        }
        if (style.assUseMyStyle) {
            add("--sub-ass-override=force")
        }
    }

/** Smaller value = larger text (VLC's relative font size). */
private val SubtitleSize.relFontSize: Int
    get() = when (this) {
        SubtitleSize.Small -> 22
        SubtitleSize.Normal -> 16
        SubtitleSize.Big -> 12
        SubtitleSize.Huge -> 8
    }

private val SubtitleBackground.opacity: Int
    get() = when (this) {
        SubtitleBackground.None -> 0
        SubtitleBackground.Subtle -> 128
        SubtitleBackground.Solid -> 255
    }
