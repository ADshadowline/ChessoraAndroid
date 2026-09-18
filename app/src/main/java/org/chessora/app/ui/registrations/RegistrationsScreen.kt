package org.chessora.app.ui.registrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

@Composable
fun RegistrationsScreen(onIdentify: () -> Unit, onOpenTournament: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> RegistrationsViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    UiStateContent(state = state, onRetry = { viewModel.retry() }) { registrations ->
        when {
            !isAuthenticated -> NotIdentifiedContent(onIdentify = onIdentify)
            registrations.isEmpty() -> EmptyContent()
            else -> RegistrationsList(registrations, onOpenTournament = onOpenTournament)
        }
    }
}

@Composable
private fun NotIdentifiedContent(onIdentify: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.registrations_not_identified),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onIdentify, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.settings_identity_button))
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.registrations_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RegistrationsList(registrations: List<TournamentRegistrationEntry>, onOpenTournament: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(registrations, key = { it.id }) { entry ->
            Card(
                onClick = { onOpenTournament(entry.id) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(entry.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(entry.dateLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
