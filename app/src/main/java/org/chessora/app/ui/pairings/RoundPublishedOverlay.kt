package org.chessora.app.ui.pairings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.Pairing
import org.chessora.app.data.remote.dto.PairingPlayer
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.push.RoundPublishedEvent
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toUiState

/** Dati per la schermata a tutto schermo di "turno pubblicato" (vedi
 * RoundPublishedOverlay sotto) - [myBoard] è null se il destinatario non ha (più) una
 * scacchiera in questo turno (es. push arrivata in ritardo su un turno già superato). */
private data class RoundPublishedData(
    val tournamentName: String,
    val roundNumber: Int,
    val myBoard: Pairing?,
    val scoreByEntrantId: Map<Int, Double>,
)

private class RoundPublishedViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<RoundPublishedData>>(UiState.Loading)
    val state: StateFlow<UiState<RoundPublishedData>> = _state.asStateFlow()

    fun load(event: RoundPublishedEvent) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val myIdPlayer = clubPreferences.identifiedPlayerId.first()
            _state.value = repository.getTournamentRounds(event.idTournamentRegistration)
                .mapCatching { rounds ->
                    val round = rounds.firstOrNull { it.roundNumber == event.roundNumber }
                    val myBoard = round?.pairings?.firstOrNull { p ->
                        (myIdPlayer != null) && (p.white?.idPlayer == myIdPlayer || p.black?.idPlayer == myIdPlayer)
                    }
                    val standings = repository.getTournamentStandings(event.idTournamentRegistration).getOrDefault(emptyList())
                    RoundPublishedData(
                        tournamentName = event.tournamentName,
                        roundNumber = event.roundNumber,
                        myBoard = myBoard,
                        scoreByEntrantId = standings.associate { it.entrantId to it.score },
                    )
                }
                .toUiState()
        }
    }
}

/** Vibra [durationMs] millisecondi (branch per VibratorManager, API 31+, vs Vibrator
 * legacy) - richiede android.permission.VIBRATE (permesso "normale", dichiarato in
 * AndroidManifest.xml, nessuna richiesta a runtime). */
private object VibrationHelper {
    fun vibrate(context: Context, durationMs: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Schermata a tutto schermo mostrata quando un turno viene pubblicato mentre l'app è in
 * primo piano (vedi ChessoraFirebaseMessagingService/TournamentRoundEvents, montata una
 * sola volta in cima a ChessoraNavHost): nome torneo + turno, i due giocatori della
 * propria scacchiera (o uno solo su un bye) con foto/nome/Elo/punteggio nel torneo,
 * pulsante di chiusura, auto-chiusura dopo 5 minuti, vibrazione di 5 secondi
 * all'apertura. */
@Composable
fun RoundPublishedOverlay(event: RoundPublishedEvent, onDismiss: () -> Unit) {
    val viewModel = chessoraViewModel { app -> RoundPublishedViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(event) { viewModel.load(event) }
    LaunchedEffect(event) { VibrationHelper.vibrate(context, 5_000L) }
    LaunchedEffect(Unit) {
        delay(5 * 60 * 1000L)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            UiStateContent(state = state, onRetry = { viewModel.load(event) }) { data ->
                Text(data.tournamentName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Turno ${data.roundNumber}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp))

                val board = data.myBoard
                if (board == null) {
                    Text("Nessuna scacchiera trovata per questo turno.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                } else {
                    PlayerCard(board.white, data.scoreByEntrantId)
                    if (!board.isBye) {
                        Text("vs", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 16.dp))
                        PlayerCard(board.black, data.scoreByEntrantId)
                    } else {
                        Text("Riposo in questo turno", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
                    }
                }

                Button(onClick = onDismiss, modifier = Modifier.padding(top = 40.dp)) { Text("Chiudi") }
            }
        }
    }
}

@Composable
private fun PlayerCard(player: PairingPlayer?, scoreByEntrantId: Map<Int, Double>) {
    if (player == null) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = NetworkModule.resolveAssetUrl(player.photoPath),
            contentDescription = null,
            modifier = Modifier.size(96.dp).clip(CircleShape),
        )
        Text(player.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Elo ${player.rating ?: "—"} · Punteggio ${formatScore(scoreByEntrantId[player.id] ?: 0.0)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatScore(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString().replace('.', ',')
