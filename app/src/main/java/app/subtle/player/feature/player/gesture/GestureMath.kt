package app.subtle.player.feature.player.gesture

/**
 * Pure math for the player gestures, kept out of composables so it can be unit-tested.
 *
 * All drag fractions are signed: positive means the finger moved down/right.
 */
internal object GestureMath {

    const val MAX_LEVEL = 100

    /** A full-width (or full-height) drag changes the value by this factor. */
    const val SENSITIVITY = 1.5f

    /** A full-width horizontal drag scrubs this much of the timeline. */
    const val SEEK_WINDOW_MS = 120_000L

    /** Vertical drag; drag up (negative) raises the volume. */
    fun volumeFromDrag(startVolume: Int, dragFraction: Float): Int =
        (startVolume - dragFraction * MAX_LEVEL * SENSITIVITY).toInt().coerceIn(0, MAX_LEVEL)

    /** Vertical drag; drag up (negative) raises the brightness. */
    fun brightnessFromDrag(startBrightness: Float, dragFraction: Float): Float =
        (startBrightness - dragFraction * SENSITIVITY).coerceIn(0.01f, 1f)

    /** Horizontal drag; drag right (positive) seeks forward. */
    fun seekFromDrag(positionMs: Long, durationMs: Long, dragFraction: Float): Long {
        if (durationMs <= 0L) return positionMs
        return (positionMs + (dragFraction * SEEK_WINDOW_MS)).toLong().coerceIn(0L, durationMs)
    }
}
