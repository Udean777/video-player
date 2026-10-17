package app.subtle.player.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.feature.browse.BrowseRoute
import app.subtle.player.feature.home.HomeRoute

enum class MainTab { Home, Browse }

@Composable
fun MainRoute(
    tabIndex: Int,
    onTabChange: (Int) -> Unit,
    onVideoClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Browse's own handler (enabled inside a folder) stays on top of this one.
    BackHandler(enabled = tabIndex == MainTab.Browse.ordinal) {
        onTabChange(MainTab.Home.ordinal)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        // Inner screens own the top status-bar inset via their TopAppBar; without this the
        // status bar would be padded twice, leaving a gap above the app bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            FloatingNavigationBar(selected = tabIndex, onSelect = onTabChange)
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (MainTab.entries[tabIndex]) {
            MainTab.Home -> HomeRoute(
                onVideoClick = onVideoClick,
                onOpenSettings = onOpenSettings,
                modifier = contentModifier,
            )
            MainTab.Browse -> BrowseRoute(onVideoClick = onVideoClick, modifier = contentModifier)
        }
    }
}

/** Rounded, floating navigation bar that lifts off the bottom edge instead of spanning it. */
@Composable
private fun FloatingNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TabItem(
                    selected = selected == MainTab.Home.ordinal,
                    icon = Icons.Filled.Home,
                    label = stringResource(R.string.tab_home),
                    onClick = { onSelect(MainTab.Home.ordinal) },
                )
                TabItem(
                    selected = selected == MainTab.Browse.ordinal,
                    icon = Icons.Filled.Folder,
                    label = stringResource(R.string.tab_browse),
                    onClick = { onSelect(MainTab.Browse.ordinal) },
                )
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        modifier = Modifier.weight(1f),
    )
}
