package app.subtle.player.data.storage

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import javax.inject.Inject

/**
 * Decodes subtitle text bytes and re-encodes them as UTF-8. Legacy `.srt` files are frequently
 * Windows-1252 rather than UTF-8, which is why libVLC shows broken glyphs without this step.
 *
 * Detection: strict UTF-8 (fallback to Windows-1252, the usual legacy encoding), or UTF-16 by BOM.
 * ponytail: no statistical CharsetDetector (android.icu is not in the compile SDK). The user can
 * still override the charset from the subtitle sheet; add a detector if real files need it.
 */
class CharsetNormalizer @Inject constructor() {

    fun toUtf8(bytes: ByteArray, override: String? = null): ByteArray {
        val charset = override?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: detect(bytes)
        val text = bytes.toString(charset).removePrefix("\uFEFF")
        return text.toByteArray(Charsets.UTF_8)
    }

    fun detect(bytes: ByteArray): Charset = when {
        looksLikeUtf8(bytes) -> Charsets.UTF_8
        hasBom(bytes, 0xFF, 0xFE) -> Charsets.UTF_16LE
        hasBom(bytes, 0xFE, 0xFF) -> Charsets.UTF_16BE
        else -> WINDOWS_1252
    }

    private fun looksLikeUtf8(bytes: ByteArray): Boolean = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
    }.isSuccess

    private fun hasBom(bytes: ByteArray, first: Int, second: Int): Boolean =
        bytes.size >= 2 && bytes[0] == first.toByte() && bytes[1] == second.toByte()

    private companion object {
        val WINDOWS_1252: Charset = Charset.forName("windows-1252")
    }
}
