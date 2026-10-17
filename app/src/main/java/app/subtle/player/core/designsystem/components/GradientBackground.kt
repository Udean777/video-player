package app.subtle.player.core.designsystem.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.subtle.player.core.designsystem.rememberThemeGradient

/**
 * Vertical theme gradient with two soft colour blobs on top. The blobs only draw where
 * [Modifier.blur] actually renders (API 31+); older devices get the clean gradient alone
 * rather than hard-edged circles.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = rememberThemeGradient(),
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
        if (blurSupported) {
            Box(
                Modifier
                    .size(320.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 96.dp, y = (-64).dp)
                    .blur(90.dp, BlurredEdgeTreatment.Unbounded)
                    .clip(CircleShape)
                    .background(scheme.primary.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .size(260.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-72).dp, y = 64.dp)
                    .blur(100.dp, BlurredEdgeTreatment.Unbounded)
                    .clip(CircleShape)
                    .background(scheme.tertiary.copy(alpha = 0.30f)),
            )
        }
        content()
    }
}
