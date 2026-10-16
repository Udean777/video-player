package app.subtle.player.feature.player.subtitle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.core.player.PlayerState
import app.subtle.player.core.player.Track
import app.subtle.player.domain.model.SubtitleFile
import app.subtle.player.domain.model.VideoPreferences

private val CHARSET_OPTIONS = listOf<String?>(null, "UTF-8", "windows-1252", "ISO-8859-1", "UTF-16")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSheet(
    player: PlayerState,
    externalSubtitles: List<SubtitleFile>,
    preferences: VideoPreferences,
    onDismiss: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onLoadSubtitle: (SubtitleFile) -> Unit,
    onPickFile: () -> Unit,
    onDelayDelta: (Long) -> Unit,
    onResetDelay: () -> Unit,
    onHardSubsChange: (Boolean) -> Unit,
    onCharsetChange: (String?) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(stringResource(R.string.subtitles_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            SubtitleOption(stringResource(R.string.subtitle_off), player.selectedSubtitleTrackId == null) {
                onSelectTrack(Track.DISABLED_ID)
            }
            player.subtitleTracks.forEach { track ->
                SubtitleOption(
                    label = track.name.ifBlank { stringResource(R.string.subtitle_track_fallback, track.id) },
                    selected = player.selectedSubtitleTrackId == track.id,
                    onClick = { onSelectTrack(track.id) },
                )
            }
            externalSubtitles.forEach { file ->
                SubtitleOption(label = file.displayName, selected = false, trailing = file.languageHint) {
                    onLoadSubtitle(file)
                }
            }
            if (player.subtitleTracks.isEmpty() && externalSubtitles.isEmpty()) {
                Text(
                    text = stringResource(R.string.subtitle_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            TextButton(onClick = onPickFile) { Text(stringResource(R.string.subtitle_load_file)) }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            DelayRow(delayMs = player.subtitleDelayMs, onDelta = onDelayDelta, onReset = onResetDelay)

            Spacer(Modifier.height(12.dp))
            CharsetRow(current = preferences.charsetOverride, onCharsetChange = onCharsetChange)

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.subtitle_hard_subs), modifier = Modifier.weight(1f))
                Switch(checked = preferences.hasHardSubs, onCheckedChange = onHardSubsChange)
            }
        }
    }
}

@Composable
private fun SubtitleOption(
    label: String,
    selected: Boolean,
    trailing: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DelayRow(delayMs: Long, onDelta: (Long) -> Unit, onReset: () -> Unit) {
    Column {
        Text(stringResource(R.string.subtitle_delay), style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DelayButton(-100, onDelta)
            DelayButton(-10, onDelta)
            Text(
                text = "%+d ms".format(delayMs),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            DelayButton(10, onDelta)
            DelayButton(100, onDelta)
        }
        TextButton(onClick = onReset) { Text(stringResource(R.string.subtitle_delay_reset)) }
    }
}

@Composable
private fun DelayButton(ms: Int, onDelta: (Long) -> Unit) {
    val label = if (ms < 0) {
        stringResource(R.string.subtitle_delay_minus, -ms)
    } else {
        stringResource(R.string.subtitle_delay_plus, ms)
    }
    TextButton(onClick = { onDelta(ms.toLong()) }) { Text(label) }
}

@Composable
private fun CharsetRow(current: String?, onCharsetChange: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(stringResource(R.string.subtitle_charset), style = MaterialTheme.typography.titleSmall)
        TextButton(onClick = { expanded = true }) {
            Text(current ?: stringResource(R.string.subtitle_charset_auto))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CHARSET_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option ?: stringResource(R.string.subtitle_charset_auto)) },
                    onClick = {
                        expanded = false
                        onCharsetChange(option)
                    },
                )
            }
        }
    }
}
