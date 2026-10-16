package app.subtle.player.engine.vlc

import app.subtle.player.core.player.PlayerConfig

/** libVLC option builder. Pure function so it stays unit-testable on the JVM. */
internal fun buildVlcArgs(config: PlayerConfig): ArrayList<String> =
    ArrayList<String>().apply {
        if (config.forceSoftwareDecoding) {
            add("--avcodec-hw=none")
        }
    }
