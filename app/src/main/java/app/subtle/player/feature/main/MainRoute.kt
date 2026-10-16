package app.subtle.player.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.subtle.player.R
import app.subtle.player.feature.browse.BrowseRoute
import app.subtle.player.feature.home.HomeRoute

enum class MainTab { Home, Browse }

@Composable
fun MainRoute(
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Browse's own handler (enabled inside a folder) stays on top of this one.
    BackHandler(enabled = tabIndex == MainTab.Browse.ordinal) {
        tabIndex = MainTab.Home.ordinal
    }

    Scaffold(
        modifier = modifier,
        // Inner screens own the top status-bar inset via their TopAppBar; without this the
        // status bar would be padded twice, leaving a gap above the app bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tabIndex == MainTab.Home.ordinal,
                    onClick = { tabIndex = MainTab.Home.ordinal },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_home)) },
                )
                NavigationBarItem(
                    selected = tabIndex == MainTab.Browse.ordinal,
                    onClick = { tabIndex = MainTab.Browse.ordinal },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_browse)) },
                )
            }
        },
    ) { padding ->
        val contentModifier = Modifier.padding(padding)
        when (MainTab.entries[tabIndex]) {
            MainTab.Home -> HomeRoute(onVideoClick = onVideoClick, modifier = contentModifier)
            MainTab.Browse -> BrowseRoute(onVideoClick = onVideoClick, modifier = contentModifier)
        }
    }
}
