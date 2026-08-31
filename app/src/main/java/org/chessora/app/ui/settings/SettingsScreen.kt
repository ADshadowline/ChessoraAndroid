package org.chessora.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.BuildConfig
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

@Composable
fun SettingsScreen(idClub: Int?, onChangeClub: () -> Unit) {
    val viewModel = chessoraViewModel { app -> SettingsViewModel(app.repository, app.clubPreferences) }
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

        OutlinedButton(onClick = onChangeClub, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text(stringResource(R.string.settings_change_club))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it, idClub) },
            )
        }

        Text(
            "${stringResource(R.string.settings_about)}: Chessora ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}
