package org.chessora.app.ui.tournamentmanager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Elenco dei tornei InCorso gestibili dall'organizzatore - punto di ingresso dell'icona
 * Home "Gestione tornei" (self-service: la claim del ruolo organizzatore avviene al primo
 * caricamento, vedi ChessoraRepository.ensureOrganizerAuth, non serve un'azione esplicita
 * dell'utente). Tap su un torneo apre TournamentManagerRoundsScreen. */
@Composable
fun TournamentManagerListScreen(onOpenTournament: (idTournament: Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> TournamentManagerListViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    UiStateContent(state = state, onRetry = { viewModel.load() }) { tornei ->
        if (tornei.isEmpty()) {
            EmptyContent()
        } else {
            TournamentManagerList(tornei, onOpenTournament = onOpenTournament)
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            "Nessun torneo in corso da gestire.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TournamentManagerList(tornei: List<TournamentSummary>, onOpenTournament: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(tornei, key = { it.id }) { t ->
            Card(
                onClick = { onOpenTournament(t.id) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(t.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (t.eventoNome != t.nome) {
                        Text(t.eventoNome, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
