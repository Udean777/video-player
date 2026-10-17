package app.subtle.player.core.designsystem

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.palette.graphics.Palette

/** Two-stop screen background gradient derived from the active theme. */
@Composable
fun rememberThemeGradient(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) { listOf(scheme.primaryContainer, scheme.surface) }
}

/** Subtle top-of-screen tint that fades into [MaterialTheme.colorScheme.surface] for a seamless header. */
@Composable
fun rememberHeaderBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        Brush.verticalGradient(listOf(scheme.primaryContainer.copy(alpha = 0.35f), scheme.surface))
    }
}

/**
 * Two-stop gradient pulled from a thumbnail, light stop first. Pure and main-thread safe to
 * call only for small thumbnails; run it off the main thread for full-size images.
 */
fun Bitmap.extractGradientColors(): List<Color> =
    gradientStops(
        Palette.from(this).maximumColorCount(24).generate()
            .swatches
            .filter { it.population > 0 }
            .sortedByDescending { it.population }
            .map { Color(it.rgb) },
    )

/** Picks two distinct stops (light first), falling back to a neutral pair when there are fewer. */
internal fun gradientStops(candidates: List<Color>): List<Color> {
    val stops = candidates.distinct().take(2).let { if (it.size == 2) it else fallbackGradient() }
    return stops.sortedByDescending { it.luminance() }
}

private fun fallbackGradient(): List<Color> = listOf(Color(0xFF59595E), Color(0xFF1A1A1F))
