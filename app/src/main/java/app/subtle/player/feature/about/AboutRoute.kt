package app.subtle.player.feature.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AboutRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AboutScreen(onBack = onBack, modifier = modifier)
}
