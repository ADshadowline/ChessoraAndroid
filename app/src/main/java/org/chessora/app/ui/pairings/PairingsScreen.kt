package org.chessora.app.ui.pairings

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.chessora.app.data.remote.dto.Pairing
import org.chessora.app.data.remote.dto.StandingsRow
import org.chessora.app.data.remote.dto.TournamentRound
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraGreen

private val RESULT_LABELS = mapOf(
    "1-0" to "1 – 0", "0-1" to "0 – 1", "1/2-1/2" to "½ – ½",
    "1-0F" to "1 – 0 (a tavolino)", "0-1F" to "0 – 1 (a tavolino)", "0-0F" to "0 – 0",
)

/** Abbinamenti e classifica di un torneo AVVIATO - stesso dato mostrato da
 * abbinamenti-risultati.html sul sito (solo lettura qui, la correzione/pubblicazione dei
 * turni resta un'azione da organizzatore, solo sul sito). Aperta da RegistrationsScreen
 * quando il torneo preiscritto risulta InCorso, al posto del solito TournamentDetailScreen. */
@Composable
fun PairingsScreen(idTournament: Int) {
    val viewModel = chessoraViewModel { app -> PairingsViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val pollingIntervalMs by viewModel.pollingIntervalMs.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }

    // Aggiornamento quasi in tempo reale: un risultato/turno può cambiare dal sito o da
    // "Gestione tornei" mentre questa schermata resta aperta - senza un ricaricamento
    // periodico l'unico modo per vederlo sarebbe uscire e rientrare. Il loop si ferma da
    // solo (cancellazione della coroutine) quando si lascia la schermata. L'intervallo è
    // configurabile per circolo dalla superamministrazione (vedi PairingsViewModel).
    LaunchedEffect(idTournament) {
        while (true) {
            viewModel.load(idTournament)
            delay(pollingIntervalMs)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Abbinamenti") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Classifica") })
        }
        UiStateContent(state = state, onRetry = { viewModel.load(idTournament) }) { data ->
            if (data.rounds.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nessun turno pubblicato ancora.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            } else if (tabIndex == 0) {
                PairingsTab(data.rounds, data.currentRound)
            } else {
                StandingsTab(data.standings)
            }
        }
    }
}

@Composable
private fun PairingsTab(rounds: List<TournamentRound>, defaultRound: TournamentRound?) {
    var selectedRoundNumber by remember(rounds) { mutableStateOf(defaultRound?.roundNumber ?: rounds.first().roundNumber) }
    val round = rounds.firstOrNull { it.roundNumber == selectedRoundNumber } ?: rounds.first()

    Column(modifier = Modifier.fillMaxSize()) {
        if (rounds.size > 1) {
            TabRow(selectedTabIndex = rounds.indexOfFirst { it.roundNumber == round.roundNumber }.coerceAtLeast(0)) {
                rounds.sortedBy { it.roundNumber }.forEach { r ->
                    Tab(
                        selected = r.roundNumber == round.roundNumber,
                        onClick = { selectedRoundNumber = r.roundNumber },
                        text = { Text("Turno ${r.roundNumber}") },
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(round.pairings.sortedBy { it.board }, key = { it.id }) { pairing ->
                PairingRow(pairing)
            }
        }
    }
}

@Composable
private fun PairingRow(pairing: Pairing) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${pairing.board}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(pairing.white?.name ?: "—", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                if (pairing.isBye) {
                    Text("Riposo · 1 punto", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(pairing.black?.name ?: "—", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Text(
                pairing.result?.let { RESULT_LABELS[it] ?: it } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StandingsTab(standings: List<StandingsRow>) {
    if (standings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Classifica non ancora disponibile.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(standings, key = { it.entrantId }) { row ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                colors = if (row.rank <= 3) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors(),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${row.rank}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Elo ${row.rating ?: "—"} · Buchholz ${formatScore(row.buchholz)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(formatScore(row.score), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatScore(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString().replace('.', ',')

/** Pallino verde lampeggiante per un torneo InCorso - stesso spirito del bagliore già
 * usato in CalendarScreen.kt per un giorno preiscritto (rememberInfiniteTransition, tween
 * 2400ms, alpha 0.25↔0.9 in reverse), qui come pallino pieno invece che come bordo. Riusato
 * da HomeScreen (icona "I miei tornei") e RegistrationsScreen (riga del torneo). */
@Composable
fun LiveTournamentDot(modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "liveTournamentDot")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(animation = tween(2400), repeatMode = RepeatMode.Reverse),
        label = "liveTournamentDotAlpha",
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ChessoraGreen.copy(alpha = alpha)),
    )
}
