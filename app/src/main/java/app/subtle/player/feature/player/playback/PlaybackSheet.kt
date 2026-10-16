package app.subtle.player.feature.player.playback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.subtle.player.R
import app.subtle.player.core.player.PlayerState

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSheet(
    player: PlayerState,
    onDismiss: () -> Unit,
    onSelectAudioTrack: (Int) -> Unit,
    onAudioDelayDelta: (Long) -> Unit,
    onResetAudioDelay: () -> Unit,
    onSpeedChange: (Float) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(stringResource(R.string.playback_title), style = MaterialTheme.typography.titleLarge)

            if (player.audioTracks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.audio_track), style = MaterialTheme.typography.titleSmall)
                player.audioTracks.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAudioTrack(track.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = player.selectedAudioTrackId == track.id,
                            onClick = { onSelectAudioTrack(track.id) },
                        )
                        Text(
                            text = track.name.ifBlank { stringResource(R.string.subtitle_track_fallback, track.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            AudioDelayRow(delayMs = player.audioDelayMs, onDelta = onAudioDelayDelta, onReset = onResetAudioDelay)

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.playback_speed), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SPEED_OPTIONS.forEach { rate ->
                    FilterChip(
                        selected = player.speed == rate,
                        onClick = { onSpeedChange(rate) },
                        label = { Text(stringResource(R.string.playback_speed_option, speedLabel(rate))) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioDelayRow(delayMs: Long, onDelta: (Long) -> Unit, onReset: () -> Unit) {
    Column {
        Text(stringResource(R.string.audio_delay), style = MaterialTheme.typography.titleSmall)
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

private fun speedLabel(rate: Float): String =
    if (rate == rate.toInt().toFloat()) rate.toInt().toString() else rate.toString()
