package app.subtle.player.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Test

class GradientsTest {

    private val black = Color(0xFF000000)
    private val white = Color(0xFFFFFFFF)
    private val red = Color(0xFFFF0000)

    @Test
    fun picksTwoDistinctStopsLightFirst() {
        assertEquals(listOf(white, black), gradientStops(listOf(black, white)))
    }

    @Test
    fun takesOnlyTheFirstTwoCandidates() {
        assertEquals(listOf(white, black), gradientStops(listOf(black, white, red)))
    }

    @Test
    fun fallsBackWhenCandidatesCollapseToOneColor() {
        assertEquals(2, gradientStops(listOf(Color(0xFF112233), Color(0xFF112233))).size)
    }

    @Test
    fun fallbackIsOrderedLightFirst() {
        val result = gradientStops(emptyList())

        assertEquals(2, result.size)
        assertEquals(true, result[0].luminance() >= result[1].luminance())
    }
}
