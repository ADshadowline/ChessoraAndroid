package org.chessora.app.ui.tornei

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDateTime

@Composable
fun TorneoDetailScreen(club: String, idTorneo: Int) {
    val viewModel = chessoraViewModel { app -> TorneoDetailViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(club, idTorneo) { viewModel.load(club, idTorneo) }

    UiStateContent(state = state, onRetry = { viewModel.load(club, idTorneo) }) { torneo ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(torneo.titolo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(torneo.cadenzaNome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))

            Text("Date", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            torneo.date.sortedBy { it.dataOra }.forEach { data ->
                Text("• ${data.dataOra.toItalianDateTime()}")
            }

            if (torneo.variazioneElo) {
                Text(
                    "Torneo con variazione Elo" + if (torneo.arbitriNomiCompleti.isNotEmpty()) {
                        " · Arbitri: ${torneo.arbitriNomiCompleti.joinToString(", ")}"
                    } else "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (torneo.costi.isNotEmpty()) {
                Text("Costi di iscrizione", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                torneo.costi.forEach { costo ->
                    Text("${costo.typeSubscriptionNome}: € ${costo.costo}")
                }
            }

            if (torneo.premi.isNotEmpty()) {
                Text("Premi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                torneo.premi.sortedBy { it.sortOrder }.forEach { premio ->
                    Text("${premio.descrizione}: € ${premio.importo}")
                }
            }

            if (torneo.note?.isNotBlank() == true) {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text(torneo.note, style = MaterialTheme.typography.bodyMedium)
            }

            // La locandina (se presente) è HTML pronto da GET /api/bandi/render/{id},
            // non JSON: la apriamo nel browser di sistema invece di incorporare una
            // WebView, per restare più semplici in questa prima versione (vedi
            // docs/android-app-spec.md §7.5, che prevede entrambe le opzioni come
            // valide - "una WebView (o Intent.ACTION_VIEW nel browser di sistema)").
            if (torneo.idBandoTemplate != null) {
                Button(
                    onClick = {
                        val url = NetworkModule.API_BASE_URL.trimEnd('/') + "/api/bandi/render/${torneo.id}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                ) {
                    Text("Vedi locandina")
                }
            }
        }
    }
}
