package org.chessora.app.ui.tournamentmanager

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chessora.app.data.remote.dto.Pairing
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

private val RESULT_LABELS = mapOf(
    "1-0" to "1 – 0", "0-1" to "0 – 1", "1/2-1/2" to "½ – ½",
    "1-0F" to "1 – 0 (a tavolino)", "0-1F" to "0 – 1 (a tavolino)", "0-0F" to "0 – 0",
)

/** Turni/scacchiere/risultati di un torneo InCorso, lato organizzatore: genera il turno
 * successivo, pubblica un turno in bozza, inserisce/corregge i risultati - stesse azioni
 * di abbinamenti-risultati.html sul sito (senza la correzione manuale degli abbinamenti,
 * fuori scope qui). Ogni azione ricarica i turni dal server, cosi' il sito vede subito i
 * cambiamenti fatti dall'app e viceversa (stessa Api/DB). */
@Composable
fun TournamentManagerRoundsScreen(idTournament: Int) {
    val viewModel = chessoraViewModel { app -> TournamentManagerRoundsViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val context = LocalContext.current
    var selectedRoundNumber by remember { mutableStateOf<Int?>(null) }
    var resultDialogPairing by remember { mutableStateOf<Pairing?>(null) }

    LaunchedEffect(idTournament) { viewModel.load(idTournament) }

    fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    UiStateContent(state = state, onRetry = { viewModel.load(idTournament) }) { rounds ->
        // Solo quando il NUMERO di turni cambia (un turno è stato appena generato) si
        // salta al più recente - pubblicare un turno o inserire un risultato non deve
        // spostare la scheda selezionata.
        LaunchedEffect(rounds.size) {
            selectedRoundNumber = rounds.maxByOrNull { it.roundNumber }?.roundNumber
        }
        val round = rounds.firstOrNull { it.roundNumber == selectedRoundNumber }
        val hasPendingRound = rounds.any { it.status == "Pending" }

        Column(modifier = Modifier.fillMaxSize()) {
            if (rounds.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nessun turno generato ancora.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
            } else {
                if (rounds.size > 1) {
                    TabRow(selectedTabIndex = rounds.indexOfFirst { it.roundNumber == round?.roundNumber }.coerceAtLeast(0)) {
                        rounds.sortedBy { it.roundNumber }.forEach { r ->
                            Tab(
                                selected = r.roundNumber == round?.roundNumber,
                                onClick = { selectedRoundNumber = r.roundNumber },
                                text = { Text("Turno ${r.roundNumber}" + if (r.status == "Pending") " · bozza" else "") },
                            )
                        }
                    }
                }
                if (round != null) {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                        items(round.pairings.sortedBy { it.board }, key = { it.id }) { pairing ->
                            OrganizerPairingRow(pairing, onClick = { resultDialogPairing = pairing })
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!hasPendingRound) {
                    Button(
                        onClick = { viewModel.generateRound(idTournament, onError = ::showError) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Genera turno successivo") }
                }
                if (round?.status == "Pending") {
                    Button(
                        onClick = { viewModel.publishRound(idTournament, round.id, onError = ::showError) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                    ) { Text("Pubblica turno") }
                }
            }
        }
    }

    resultDialogPairing?.let { pairing ->
        ResultDialog(
            pairing = pairing,
            onDismiss = { resultDialogPairing = null },
            onSelect = { result ->
                viewModel.submitResult(idTournament, pairing.id, result, onError = ::showError)
                resultDialogPairing = null
            },
        )
    }
}

@Composable
private fun OrganizerPairingRow(pairing: Pairing, onClick: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    val content: @Composable () -> Unit = {
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
    if (pairing.isBye) {
        Card(modifier = modifier) { content() }
    } else {
        Card(onClick = onClick, modifier = modifier) { content() }
    }
}

@Composable
private fun ResultDialog(pairing: Pairing, onDismiss: () -> Unit, onSelect: (String?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scacchiera #${pairing.board}") },
        text = {
            Column {
                Text(
                    "${pairing.white?.name ?: "—"} – ${pairing.black?.name ?: "—"}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                listOf("1-0", "1/2-1/2", "0-1").forEach { result ->
                    TextButton(onClick = { onSelect(result) }) { Text(RESULT_LABELS.getValue(result)) }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                listOf("1-0F", "0-1F", "0-0F").forEach { result ->
                    TextButton(onClick = { onSelect(result) }) { Text(RESULT_LABELS.getValue(result)) }
                }
                if (pairing.result != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    TextButton(onClick = { onSelect(null) }) { Text("Annulla risultato") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        },
    )
}
