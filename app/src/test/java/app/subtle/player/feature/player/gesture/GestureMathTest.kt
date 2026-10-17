package app.subtle.player.feature.player.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class GestureMathTest {

    @Test
    fun volumeDragUpIncreasesAndClamps() {
        assertEquals(100, GestureMath.volumeFromDrag(50, -0.5f))
        assertEquals(25, GestureMath.volumeFromDrag(100, 0.5f))
        assertEquals(0, GestureMath.volumeFromDrag(10, 1f))
    }

    @Test
    fun brightnessDragUpIncreasesAndClamps() {
        assertEquals(1f, GestureMath.brightnessFromDrag(0.5f, -0.5f), 0.0001f)
        assertEquals(0.2f, GestureMath.brightnessFromDrag(0.95f, 0.5f), 0.0001f)
        assertEquals(0.01f, GestureMath.brightnessFromDrag(0.05f, 1f), 0.0001f)
    }

    @Test
    fun seekDragRightForwardAndClampsToDuration() {
        assertEquals(60_000L, GestureMath.seekFromDrag(0L, 600_000L, 0.5f))
        assertEquals(0L, GestureMath.seekFromDrag(10_000L, 600_000L, -1f))
        assertEquals(600_000L, GestureMath.seekFromDrag(590_000L, 600_000L, 1f))
    }

    @Test
    fun seekWithUnknownDurationReturnsPosition() {
        assertEquals(42_000L, GestureMath.seekFromDrag(42_000L, 0L, 0.5f))
    }
}
