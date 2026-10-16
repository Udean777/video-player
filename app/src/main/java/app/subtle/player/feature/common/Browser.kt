package app.subtle.player.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.domain.model.FolderEntry
import app.subtle.player.domain.model.Video

enum class BrowserViewMode { List, Grid }

@Composable
fun RowScope.BrowserActions(
    mode: BrowserViewMode,
    onViewModeChange: (BrowserViewMode) -> Unit,
    onRefresh: () -> Unit,
) {
    IconButton(
        onClick = {
            onViewModeChange(if (mode == BrowserViewMode.List) BrowserViewMode.Grid else BrowserViewMode.List)
        },
    ) {
        if (mode == BrowserViewMode.List) {
            Icon(Icons.Filled.GridView, stringResource(R.string.grid_view))
        } else {
            Icon(Icons.Filled.List, stringResource(R.string.list_view))
        }
    }
    IconButton(onClick = onRefresh) {
        Icon(Icons.Filled.Refresh, stringResource(R.string.refresh_list))
    }
}

@Composable
fun FolderRow(folder: FolderEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                pluralStringResource(R.plurals.video_count, folder.videoCount, folder.videoCount),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun VideoRow(video: Video, progressFraction: Float?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbnailWithProgress(
            uri = video.uri,
            progressFraction = progressFraction,
            modifier = Modifier.size(width = 84.dp, height = 48.dp).clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(video.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatSize(video.sizeBytes), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun FolderCell(folder: FolderEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            folder.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            pluralStringResource(R.plurals.video_count, folder.videoCount, folder.videoCount),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun VideoCell(video: Video, progressFraction: Float?, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ThumbnailWithProgress(
            uri = video.uri,
            progressFraction = progressFraction,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            video.displayName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(formatSize(video.sizeBytes), style = MaterialTheme.typography.bodySmall)
    }
}

/** Thumbnail with a thin watched-progress bar pinned to its bottom edge. */
@Composable
private fun ThumbnailWithProgress(uri: String, progressFraction: Float?, modifier: Modifier = Modifier) {
    Box(modifier) {
        VideoThumbnail(uri = uri, modifier = Modifier.matchParentSize())
        if (progressFraction != null) {
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),
            )
        }
    }
}

fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = sizeBytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "%d %s".format(sizeBytes, units[unit]) else "%.1f %s".format(value, units[unit])
}
