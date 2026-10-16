package app.subtle.player.data.storage

import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Test

class CharsetNormalizerTest {

    private val normalizer = CharsetNormalizer()

    @Test
    fun utf8BytesRoundTrip() {
        val text = "Halo, dunia — naïve"
        assertEquals(text, normalizer.toUtf8(text.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8))
    }

    @Test
    fun windows1252IsReencodedAsUtf8() {
        val original = "café naïve" // é and ï are single bytes in windows-1252
        val encoded = original.toByteArray(Charset.forName("windows-1252"))

        val result = normalizer.toUtf8(encoded).toString(Charsets.UTF_8)

        assertEquals(original, result)
    }

    @Test
    fun explicitOverrideWins() {
        val text = "héllo"
        val encoded = text.toByteArray(Charset.forName("windows-1252"))

        val result = normalizer.toUtf8(encoded, override = "windows-1252").toString(Charsets.UTF_8)

        assertEquals(text, result)
    }

    @Test
    fun utf8BomIsStripped() {
        val withBom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "hello".toByteArray(Charsets.UTF_8)

        val result = normalizer.toUtf8(withBom).toString(Charsets.UTF_8)

        assertEquals("hello", result)
    }
}
