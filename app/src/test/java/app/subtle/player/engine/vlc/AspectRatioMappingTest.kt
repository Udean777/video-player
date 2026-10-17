package app.subtle.player.engine.vlc

import app.subtle.player.core.player.AspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AspectRatioMappingTest {

    @Test
    fun fitAndRatiosForceNoScale() {
        assertNull(aspectRatioParams(AspectRatio.Fit, 1920, 1080, 1280, 720).aspectRatio)
        assertEquals(VideoLayoutParams("16:9", 0f), aspectRatioParams(AspectRatio.Ratio16x9, 0, 0, 0, 0))
        assertEquals(VideoLayoutParams("4:3", 0f), aspectRatioParams(AspectRatio.Ratio4x3, 0, 0, 0, 0))
    }

    @Test
    fun fillUsesDisplaySize() {
        val params = aspectRatioParams(AspectRatio.Fill, displayWidth = 1920, displayHeight = 1080, videoWidth = 0, videoHeight = 0)

        assertEquals("1920:1080", params.aspectRatio)
        assertEquals(0f, params.scale, 0f)
    }

    @Test
    fun fillWithoutDisplaySizeFallsBackToFit() {
        assertNull(aspectRatioParams(AspectRatio.Fill, 0, 0, 0, 0).aspectRatio)
    }

    @Test
    fun cropZoomsToCoverWhenDisplayMatchesVideoAspect() {
        val params = aspectRatioParams(AspectRatio.Crop, displayWidth = 1920, displayHeight = 1080, videoWidth = 1280, videoHeight = 720)

        assertNull(params.aspectRatio)
        assertEquals(1.5f, params.scale, 0.0001f)
    }

    @Test
    fun cropPicksSmallerAxisWhenDisplayIsWiderThanVideo() {
        val params = aspectRatioParams(AspectRatio.Crop, displayWidth = 1000, displayHeight = 1000, videoWidth = 1920, videoHeight = 1080)

        assertEquals(1000f / 1080f, params.scale, 0.0001f)
    }

    @Test
    fun cropWithoutVideoSizeFallsBackToFit() {
        assertNull(aspectRatioParams(AspectRatio.Crop, 1920, 1080, 0, 0).aspectRatio)
        assertEquals(0f, aspectRatioParams(AspectRatio.Crop, 1920, 1080, 0, 0).scale, 0f)
    }
}
