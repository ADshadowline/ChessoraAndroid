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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlinx.coroutines.delay
import org.chessora.app.data.remote.dto.EntrantDto
import org.chessora.app.data.remote.dto.Pairing
import org.chessora.app.data.remote.dto.StandingsRow
import org.chessora.app.data.remote.dto.TournamentViewMode
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

private val RESULT_LABELS = mapOf(
    "1-0" to "1 – 0", "0-1" to "0 – 1", "1/2-1/2" to "½ – ½",
    "1-0F" to "1 – 0 (a tavolino)", "0-1F" to "0 – 1 (a tavolino)", "0-0F" to "0 – 0",
)

private const val ROUNDS_POLL_INTERVAL_MS = 8_000L

/** Turni/scacchiere/risultati di un torneo InCorso, lato organizzatore: mostra gli
 * iscritti effettivi finché non è ancora stato generato alcun turno, genera il turno
 * successivo, pubblica un turno in bozza, inserisce/corregge i risultati e infine (a
 * torneo concluso, vedi [totalRounds]) mostra la premiazione - stesse azioni di
 * abbinamenti-risultati.html sul sito (senza la correzione manuale degli abbinamenti,
 * fuori scope qui). Ogni azione ricarica i turni dal server, cosi' il sito vede subito i
 * cambiamenti fatti dall'app e viceversa (stessa Api/DB). */
@Composable
fun TournamentManagerRoundsScreen(idTournament: Int, totalRounds: Int) {
    val viewModel = chessoraViewModel { app -> TournamentManagerRoundsViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val standings by viewModel.standings.collectAsState()
    val entrants by viewModel.entrants.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val context = LocalContext.current
    var selectedRoundNumber by remember { mutableStateOf<Int?>(null) }
    var resultDialogPairing by remember { mutableStateOf<Pairing?>(null) }

    // Aggiornamento quasi in tempo reale: un risultato/turno può cambiare dal sito (o da
    // un altro organizzatore) mentre questa schermata resta aperta - senza un
    // ricaricamento periodico il pulsante "Pubblica turno"/i risultati mostrati
    // resterebbero indietro finché non si esce e rientra. Il loop si ferma da solo
    // (cancellazione della coroutine) quando si lascia la schermata.
    LaunchedEffect(idTournament) {
        while (true) {
            viewModel.load(idTournament)
            delay(ROUNDS_POLL_INTERVAL_MS)
        }
    }

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
        // "Genera turno successivo" ha senso solo quando l'ultimo turno generato è
        // completo (tutte le scacchiere hanno un risultato, bye compresi - vedi
        // Pairing.result, sempre valorizzato per un bye): generare un nuovo turno a metà
        // dell'attuale produrrebbe abbinamenti basati su punteggi incompleti.
        val latestRound = rounds.maxByOrNull { it.roundNumber }
        val latestRoundComplete = latestRound == null || latestRound.pairings.all { it.result != null }
        // Come sul sito (isFinal in abbinamenti-risultati.html): "ultimo turno del
        // torneo" si sa solo confrontando col numero di turni previsti, mai dalla sola
        // assenza di un turno successivo (i turni si generano uno alla volta). Se
        // totalRounds non è impostato (torneo creato prima che il campo esistesse) non si
        // può mai essere certi di essere all'ultimo turno: si resta sempre su "Genera
        // turno", mai forzati a mostrare "Premiazione" per errore.
        val isFinalRound = totalRounds > 0 && (latestRound?.roundNumber ?: 0) >= totalRounds
        // idPlayer→titolo di ogni iscritto, ricostruito dalle scacchiere già caricate (mai
        // una fetch a parte): serve solo per la Premiazione, per la stessa regola di
        // categoria del sito (titolo FIDE se c'è, altrimenti fasce Elo).
        val titleByEntrantId = remember(rounds) {
            val map = mutableMapOf<Int, String?>()
            rounds.forEach { r ->
                r.pairings.forEach { p ->
                    p.white?.let { map[it.id] = it.title }
                    p.black?.let { map[it.id] = it.title }
                }
            }
            map
        }

        Column(modifier = Modifier.fillMaxSize()) {
            when {
                viewMode == TournamentViewMode.PREMIAZIONE -> {
                    val sections = remember(standings, titleByEntrantId) { computePrizeSections(standings, titleByEntrantId) }
                    PremiazioneContent(sections, modifier = Modifier.weight(1f))
                }
                rounds.isEmpty() -> EntrantsContent(entrants, modifier = Modifier.weight(1f))
                viewMode == TournamentViewMode.STANDINGS -> {
                    if (standings.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("Classifica non ancora disponibile.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                            items(standings, key = { it.entrantId }) { row -> OrganizerStandingsRow(row) }
                        }
                    }
                }
                else -> {
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
            }
            // "Classifica provvisoria"/"Premiazione" e "Genera turno successivo" non
            // compaiono mai insieme: finché l'ultimo turno non è completo ha senso solo
            // generare (anche se ancora disabilitato); una volta completo si passa PRIMA
            // alla classifica - "Genera turno" ricompare lì finché non è l'ultimo turno
            // del torneo, altrimenti (torneo finito) è "Premiazione" a comparire al suo
            // posto, mai insieme.
            val showPremiazione = viewMode == TournamentViewMode.PREMIAZIONE
            val showStandingsToggle = !showPremiazione && (viewMode == TournamentViewMode.STANDINGS || (rounds.isNotEmpty() && !hasPendingRound && latestRoundComplete))
            val showGenerate = rounds.isEmpty() || (!hasPendingRound && !isFinalRound && (viewMode == TournamentViewMode.STANDINGS || !latestRoundComplete))
            val showPremiazioneButton = !showPremiazione && viewMode == TournamentViewMode.STANDINGS && !hasPendingRound && latestRoundComplete && isFinalRound
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showPremiazione) {
                    // Condivisa col sito come lo stato Pairings/Standings (vedi sopra):
                    // tornare alla classifica da qui sposta anche tourn.chessora.org, e
                    // viceversa passare da "Premiazione" a "Classifica finale" sul sito
                    // sposta anche questa schermata entro pochi secondi.
                    OutlinedButton(onClick = { viewModel.setViewMode(idTournament, TournamentViewMode.STANDINGS) }, modifier = Modifier.weight(1f)) {
                        Text("Torna alla classifica")
                    }
                } else {
                    // Condivisa col sito (vedi TournamentManagerRoundsViewModel.setViewMode):
                    // se sposti la vista da qui, tourn.chessora.org la segue entro pochi
                    // secondi, e viceversa. Mai mostrata prima che esista un turno.
                    if (showStandingsToggle) {
                        OutlinedButton(
                            onClick = {
                                val next = if (viewMode == TournamentViewMode.PAIRINGS) TournamentViewMode.STANDINGS else TournamentViewMode.PAIRINGS
                                viewModel.setViewMode(idTournament, next)
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (viewMode == TournamentViewMode.PAIRINGS) "Classifica provvisoria" else "Torna agli abbinamenti") }
                    }

                    if (showGenerate) {
                        Button(
                            onClick = { viewModel.generateRound(idTournament, onError = ::showError) },
                            enabled = !busy && latestRoundComplete,
                            modifier = Modifier.weight(1f),
                        ) { Text(if (rounds.isEmpty()) "Genera turno 1" else "Genera turno successivo") }
                    }

                    if (showPremiazioneButton) {
                        Button(onClick = { viewModel.setViewMode(idTournament, TournamentViewMode.PREMIAZIONE) }, modifier = Modifier.weight(1f)) { Text("Premiazione") }
                    }

                    if (viewMode == TournamentViewMode.PAIRINGS && round?.status == "Pending") {
                        Button(
                            onClick = { viewModel.publishRound(idTournament, round.id, onError = ::showError) },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                        ) { Text("Pubblica turno") }
                    }
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
private fun EntrantsContent(entrants: List<EntrantDto>, modifier: Modifier = Modifier) {
    if (entrants.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Nessun turno generato ancora. Nessun iscritto trovato.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Iscritti effettivi (${entrants.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            items(entrants, key = { it.id }) { e -> EntrantRow(e) }
        }
    }
}

@Composable
private fun EntrantRow(entrant: EntrantDto) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(entrant.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            val meta = listOfNotNull(
                entrant.title?.takeIf { it.isNotBlank() },
                entrant.federation?.takeIf { it.isNotBlank() },
                entrant.rating?.let { "Elo $it" },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
        }
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
private fun OrganizerStandingsRow(row: StandingsRow) {
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

private fun formatScore(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString().replace('.', ',')

// ---------------- Premiazione ----------------
// Stessa logica di abbinamenti-risultati.html (Assoluti poi Fasce Elo poi Categorie, mai
// cumulabili: chi vince in un gruppo non ricompare nei successivi), con gli importi di
// DEFAULT del sito (monte premi/fasce/percentuali) - la configurazione personalizzata di
// un organizzatore vive SOLO nel localStorage del suo browser, il server non la conosce e
// quindi l'app non può leggerla (vedi la richiesta dell'utente/la spiegazione data prima
// di implementare questa schermata).

private data class PrizeFascia(val label: String, val min: Int?, val max: Int?)
private data class PrizeRankConfig(val percent: Boolean, val value: Int)
private data class PrizeRow(val entrantId: Int, val rank: Int, val name: String, val title: String?, val rating: Int?, val amount: Int)
private data class PrizeGroup(val label: String, val rows: List<PrizeRow>)
private data class PrizeSections(val assoluti: List<PrizeRow>, val fasce: List<PrizeGroup>, val categorie: List<PrizeGroup>)

private object PrizeDefaults {
    const val MONTE_PREMI = 1000
    val FASCE_ELO = listOf(
        PrizeFascia("Over 2000", 2000, null),
        PrizeFascia("1800–1999", 1800, 1999),
        PrizeFascia("1600–1799", 1600, 1799),
        PrizeFascia("1400–1599", 1400, 1599),
        PrizeFascia("Under 1400", null, 1399),
    )
    const val ASSOLUTI_COUNT = 3
    const val FASCE_COUNT = 3
    const val CATEGORIE_COUNT = 3
    val ASSOLUTI_RANKS = listOf(PrizeRankConfig(true, 25), PrizeRankConfig(true, 15), PrizeRankConfig(true, 10))
    val FASCE_RANKS = listOf(PrizeRankConfig(false, 40), PrizeRankConfig(false, 25), PrizeRankConfig(false, 15))
    val CATEGORIE_RANKS = listOf(PrizeRankConfig(false, 30), PrizeRankConfig(false, 20), PrizeRankConfig(false, 10))
}

private val CATEGORY_ORDER = listOf("GM", "IM", "FM", "CM", "1N", "2N", "3N", "NC")

private fun categoryFor(title: String?, rating: Int?): String {
    if (!title.isNullOrBlank()) return title
    val elo = rating ?: 0
    return when {
        elo >= 1800 -> "1N"
        elo >= 1600 -> "2N"
        elo >= 1400 -> "3N"
        else -> "NC"
    }
}

private fun prizeAmount(rank: PrizeRankConfig?): Int {
    if (rank == null) return 0
    val raw = if (rank.percent) PrizeDefaults.MONTE_PREMI * rank.value / 100.0 else rank.value.toDouble()
    return (Math.round(raw / 5.0) * 5).toInt()
}

private fun computePrizeSections(standings: List<StandingsRow>, titleByEntrantId: Map<Int, String?>): PrizeSections {
    fun toRow(r: StandingsRow, rank: Int, rankConfig: PrizeRankConfig?) =
        PrizeRow(r.entrantId, rank, r.name, titleByEntrantId[r.entrantId], r.rating, prizeAmount(rankConfig))

    val awarded = mutableSetOf<Int>()

    val assoluti = standings.take(PrizeDefaults.ASSOLUTI_COUNT)
        .mapIndexed { i, r -> toRow(r, i + 1, PrizeDefaults.ASSOLUTI_RANKS.getOrNull(i)) }
    assoluti.forEach { awarded += it.entrantId }

    var remaining = standings.filter { it.entrantId !in awarded }
    val fasce = PrizeDefaults.FASCE_ELO.mapNotNull { f ->
        val min = f.min ?: Int.MIN_VALUE
        val max = f.max ?: Int.MAX_VALUE
        val source = remaining.filter { (it.rating ?: 0) in min..max }.take(PrizeDefaults.FASCE_COUNT)
        if (source.isEmpty()) return@mapNotNull null
        PrizeGroup(f.label, source.mapIndexed { i, r -> toRow(r, i + 1, PrizeDefaults.FASCE_RANKS.getOrNull(i)) })
    }
    fasce.forEach { g -> g.rows.forEach { awarded += it.entrantId } }

    remaining = standings.filter { it.entrantId !in awarded }
    val byCategory = remaining.groupBy { categoryFor(titleByEntrantId[it.entrantId], it.rating) }
    val categorie = CATEGORY_ORDER.mapNotNull { cat ->
        val source = byCategory[cat] ?: return@mapNotNull null
        PrizeGroup(cat, source.take(PrizeDefaults.CATEGORIE_COUNT).mapIndexed { i, r -> toRow(r, i + 1, PrizeDefaults.CATEGORIE_RANKS.getOrNull(i)) })
    }

    return PrizeSections(assoluti, fasce, categorie)
}

private fun formatEuro(n: Int): String = "€ $n"

@Composable
private fun PremiazioneContent(sections: PrizeSections, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Text("Premiazione", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        }
        item { PrizeSectionTitle("Assoluti") }
        if (sections.assoluti.isEmpty()) {
            item { Text("Nessun dato disponibile.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(sections.assoluti, key = { "a-${it.entrantId}" }) { PrizeRowCard(it) }
        }
        if (sections.fasce.isNotEmpty()) {
            item { PrizeSectionTitle("Fasce Elo") }
            sections.fasce.forEach { group ->
                item { PrizeGroupLabel(group) }
                items(group.rows, key = { "f-${group.label}-${it.entrantId}" }) { PrizeRowCard(it) }
            }
        }
        if (sections.categorie.isNotEmpty()) {
            item { PrizeSectionTitle("Categorie") }
            sections.categorie.forEach { group ->
                item { PrizeGroupLabel(group) }
                items(group.rows, key = { "c-${group.label}-${it.entrantId}" }) { PrizeRowCard(it) }
            }
        }
    }
}

@Composable
private fun PrizeSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
private fun PrizeGroupLabel(group: PrizeGroup) {
    Text(
        "${group.label} (${group.rows.size})",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun PrizeRowCard(row: PrizeRow) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        colors = if (row.rank <= 3) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${row.rank}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.name + (row.title?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text("Elo ${row.rating ?: "—"}", style = MaterialTheme.typography.bodySmall)
            }
            Text(formatEuro(row.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
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
