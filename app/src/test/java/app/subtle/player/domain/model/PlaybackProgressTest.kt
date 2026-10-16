package app.subtle.player.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackProgressTest {

    @Test
    fun fractionIsPositionOverDuration() {
        assertEquals(0.5f, PlaybackProgress(50_000L, 100_000L).fraction, 0.001f)
    }

    @Test
    fun fractionIsZeroWhenDurationUnknown() {
        assertEquals(0f, PlaybackProgress(50_000L, 0L).fraction, 0.001f)
    }

    @Test
    fun fractionClampsToOne() {
        assertEquals(1f, PlaybackProgress(200_000L, 100_000L).fraction, 0.001f)
    }

    @Test
    fun resumeKeepsSavedPositionInTheMiddle() {
        assertEquals(50_000L, PlaybackProgress(50_000L, 600_000L).resumePositionMs())
    }

    @Test
    fun resumeRestartsNearStart() {
        assertEquals(0L, PlaybackProgress(3_000L, 600_000L).resumePositionMs())
    }

    @Test
    fun resumeRestartsNearEnd() {
        assertEquals(0L, PlaybackProgress(598_000L, 600_000L).resumePositionMs())
    }

    @Test
    fun resumeRestartsWhenDurationUnknown() {
        assertEquals(0L, PlaybackProgress(50_000L, 0L).resumePositionMs())
    }
}
