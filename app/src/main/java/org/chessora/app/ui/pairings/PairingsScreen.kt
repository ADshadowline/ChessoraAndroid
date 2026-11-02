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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
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
 * quando il torneo preiscritto risulta InCorso, al posto del solito TournamentDetailScreen, e
 * da "Risultati e classifica" nel dettaglio evento per un torneo avviato. Come su tourn mostra
 * le probabilità di ogni partita e, in classifica, di vincere il torneo (vedi TournamentOdds). */
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
                PairingsTab(data.rounds, data.currentRound, data.drawCoefficient)
            } else {
                StandingsTab(data.standings, data.chances, data.roundsLeft)
            }
        }
    }
}

@Composable
private fun PairingsTab(rounds: List<TournamentRound>, defaultRound: TournamentRound?, drawCoefficient: Double) {
    var selectedRoundNumber by remember(rounds) { mutableStateOf(defaultRound?.roundNumber ?: rounds.first().roundNumber) }
    val round = rounds.firstOrNull { it.roundNumber == selectedRoundNumber } ?: rounds.first()

    Column(modifier = Modifier.fillMaxSize()) {
        if (rounds.size > 1) {
            TabRow(selectedTabIndex = rounds.indexOfFirst { it.roundNumber == round.roundNumber }.coerceAtLeast(0)) {
                rounds.sortedBy { it.roundNumber }.forEach { r ->
                    Tab(
                        selected = r.roundNumber == round.roundNumber,
                        onClick = { selectedRoundNumber = r.roundNumber },
                        text = { Text("${r.roundNumber}°") },
                    )
                }
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(round.pairings.sortedBy { it.board }, key = { it.id }) { pairing ->
                PairingRow(pairing, drawCoefficient)
            }
        }
    }
}

@Composable
private fun PairingRow(pairing: Pairing, drawCoefficient: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            val white = pairing.white
            val black = pairing.black
            if (!pairing.isBye && white != null && black != null) {
                OddsBar(TournamentOdds.gameOdds(white.rating, black.rating, drawCoefficient), modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Barra Vittoria Bianco / Patta / Vittoria Nero (prima della partita, dall'Elo) - stessa
 * barra dell'"Andamento giocatore" di abbinamenti-risultati.html sul sito. */
@Composable
private fun OddsBar(odds: GameOdds, modifier: Modifier = Modifier) {
    val segments = listOf(
        Triple(odds.whiteWin, Color(0xFFF1F1F1), Color(0xFF222222)),
        Triple(odds.draw, Color(0xFF9E9E9E), Color.White),
        Triple(odds.blackWin, Color(0xFF2B2B2B), Color.White),
    ).filter { it.first > 0.0 }
    Row(modifier = modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(4.dp))) {
        segments.forEach { (value, background, textColor) ->
            Box(
                modifier = Modifier.weight(value.toFloat()).fillMaxHeight().background(background),
                contentAlignment = Alignment.Center,
            ) {
                if (value >= 0.12) {
                    Text(TournamentOdds.formatPct(value), color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StandingsTab(standings: List<StandingsRow>, chances: Map<Int, TournamentChance>?, roundsLeft: Int) {
    if (standings.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Classifica non ancora disponibile.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        return
    }
    val showPodium = standings.size > 3 // con 3 o meno il podio è certo per tutti
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (chances != null) {
            item(key = "odds-note") {
                Text(
                    "Probabilità di vittoria: stima su ${"%,d".format(Locale.ITALIAN, TournamentOdds.SIMULATIONS)} simulazioni " +
                        (if (roundsLeft == 1) "dell'ultimo turno" else "dei $roundsLeft turni che mancano") +
                        ", partendo dai punti attuali e dall'Elo.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
        }
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
                    val chance = chances?.get(row.entrantId)
                    if (chance != null) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 14.dp)) {
                            Text(
                                TournamentOdds.formatPct(chance.win),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                if (showPodium) "podio ${TournamentOdds.formatPct(chance.podium)}" else "vittoria",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
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
