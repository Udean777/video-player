package app.subtle.player.engine.vlc

import app.subtle.player.core.player.AspectRatio

/** libVLC video layout: a forced display [aspectRatio] (null = automatic) plus a zoom [scale]. */
internal data class VideoLayoutParams(val aspectRatio: String?, val scale: Float)

/**
 * Maps an [AspectRatio] to the `setAspectRatio`/`setScale` pair libVLC expects, mirroring its own
 * `VideoHelper`. Crop (VLC "fit screen") needs the display and video sizes to zoom without distortion.
 */
internal fun aspectRatioParams(
    ratio: AspectRatio,
    displayWidth: Int,
    displayHeight: Int,
    videoWidth: Int,
    videoHeight: Int,
    orientationSwapped: Boolean = false,
    sarNum: Int = 1,
    sarDen: Int = 1,
): VideoLayoutParams = when (ratio) {
    AspectRatio.Fit -> VideoLayoutParams(null, 0f)
    AspectRatio.Ratio16x9 -> VideoLayoutParams("16:9", 0f)
    AspectRatio.Ratio4x3 -> VideoLayoutParams("4:3", 0f)
    AspectRatio.Fill ->
        if (displayWidth <= 0 || displayHeight <= 0) {
            VideoLayoutParams(null, 0f)
        } else {
            val aspect = if (orientationSwapped) "$displayHeight:$displayWidth" else "$displayWidth:$displayHeight"
            VideoLayoutParams(aspect, 0f)
        }
    AspectRatio.Crop -> {
        val width = if (orientationSwapped) videoHeight else videoWidth
        val height = if (orientationSwapped) videoWidth else videoHeight
        val correctedWidth = if (sarDen != 0 && sarNum != sarDen) width * sarNum / sarDen else width
        if (displayWidth <= 0 || displayHeight <= 0 || correctedWidth <= 0 || height <= 0) {
            VideoLayoutParams(null, 0f)
        } else {
            val aspect = correctedWidth.toFloat() / height
            val displayAspect = displayWidth.toFloat() / displayHeight
            val scale = if (displayAspect >= aspect) displayWidth.toFloat() / correctedWidth else displayHeight.toFloat() / height
            VideoLayoutParams(null, scale)
        }
    }
}
