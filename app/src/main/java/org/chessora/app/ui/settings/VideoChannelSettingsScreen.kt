package org.chessora.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.VideoChannelPreference
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Elenco spuntabile dei canali YouTube della sezione Video (vedi
 * VideoChannelSettingsViewModel) - raggiungibile da Impostazioni solo con un giocatore
 * identificato. */
@Composable
fun VideoChannelSettingsScreen() {
    val viewModel = chessoraViewModel { app -> VideoChannelSettingsViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.video_settings_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.video_settings_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        UiStateContent(state = state, onRetry = { viewModel.load() }) { channels ->
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(channels, key = { it.id }) { channel: VideoChannelPreference ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    ) {
                        Text(channel.description, modifier = Modifier.weight(1f))
                        Switch(
                            checked = channel.selected,
                            onCheckedChange = { viewModel.setSelected(channel.id, it) },
                        )
                    }
                }
            }
        }
    }
}
