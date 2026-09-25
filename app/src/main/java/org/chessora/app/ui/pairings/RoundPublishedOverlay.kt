package org.chessora.app.ui.pairings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Sfondo e colori del testo FISSI (non presi da MaterialTheme): questa è una schermata
// "scenica" a sé, non deve dipendere dal tema chiaro/scuro dell'app - un testo scuro di
// default su questo sfondo era il bug segnalato ("non si vedono bene le scritte").
private val OverlayBackground = Color(0xFF16151B)
private val OverlayTextPrimary = Color.White
private val OverlayTextSecondary = Color(0xFFC7C6D6)
private val OverlayAccent = Color(0xFFF2C744)

private enum class OverlayPhase { COUNTDOWN, FLASH, CONTENT }

/** Schermata a tutto schermo mostrata quando un turno viene pubblicato mentre l'app è in
 * primo piano (vedi ChessoraFirebaseMessagingService/TournamentRoundEvents, montata una
 * sola volta in cima a ChessoraNavHost): vibrazione di 4 secondi mentre appare un
 * conto alla rovescia (3-2-1), un lampo, poi torneo/turno e i due giocatori della
 * propria scacchiera (o uno solo su un bye) con foto/nome/Elo/punteggio nel torneo -
 * pulsante di chiusura, auto-chiusura dopo 5 minuti. */
@Composable
fun RoundPublishedOverlay(event: RoundPublishedEvent, onDismiss: () -> Unit) {
    val viewModel = chessoraViewModel { app -> RoundPublishedViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var phase by remember { mutableStateOf(OverlayPhase.COUNTDOWN) }
    var countdownValue by remember { mutableIntStateOf(3) }

    LaunchedEffect(event) { viewModel.load(event) }
    LaunchedEffect(event) { VibrationHelper.vibrate(context, 4_000L) }
    LaunchedEffect(event) {
        for (n in 3 downTo 1) {
            countdownValue = n
            delay(650)
        }
        phase = OverlayPhase.FLASH
        delay(350)
        phase = OverlayPhase.CONTENT
    }
    LaunchedEffect(Unit) {
        delay(5 * 60 * 1000L)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(OverlayBackground),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            OverlayPhase.COUNTDOWN -> CountdownContent(countdownValue)
            OverlayPhase.FLASH -> FlashContent()
            OverlayPhase.CONTENT -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(450)) + scaleIn(tween(450), initialScale = 0.85f),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        UiStateContent(state = state, onRetry = { viewModel.load(event) }) { data ->
                            Text(
                                data.tournamentName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                                color = OverlayTextPrimary, textAlign = TextAlign.Center,
                            )
                            Text(
                                "Turno ${data.roundNumber}", style = MaterialTheme.typography.titleMedium,
                                color = OverlayTextSecondary, modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                            )

                            val board = data.myBoard
                            if (board == null) {
                                Text(
                                    "Nessuna scacchiera trovata per questo turno.", style = MaterialTheme.typography.bodyLarge,
                                    color = OverlayTextPrimary, textAlign = TextAlign.Center,
                                )
                            } else {
                                PlayerCard(board.white, data.scoreByEntrantId)
                                if (!board.isBye) {
                                    Text(
                                        "vs", style = MaterialTheme.typography.titleMedium,
                                        color = OverlayTextSecondary, modifier = Modifier.padding(vertical = 16.dp),
                                    )
                                    PlayerCard(board.black, data.scoreByEntrantId)
                                } else {
                                    Text(
                                        "Riposo in questo turno", style = MaterialTheme.typography.bodyMedium,
                                        color = OverlayTextSecondary, modifier = Modifier.padding(top = 16.dp),
                                    )
                                }
                            }

                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = OverlayAccent, contentColor = Color(0xFF1B1C22)),
                                modifier = Modifier.padding(top = 40.dp),
                            ) { Text("Chiudi") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownContent(value: Int) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (scaleIn(tween(250), initialScale = 1.6f) + fadeIn(tween(150))) togetherWith
                (scaleOut(tween(200), targetScale = 0.6f) + fadeOut(tween(150)))
        },
        label = "countdown",
    ) { n ->
        Text(
            n.toString(),
            fontSize = 120.sp,
            fontWeight = FontWeight.Black,
            color = OverlayAccent,
        )
    }
}

@Composable
private fun FlashContent() {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = null,
            tint = OverlayAccent,
            modifier = Modifier.size(160.dp),
        )
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
        Text(
            player.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = OverlayTextPrimary, modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            "Elo ${player.rating ?: "—"} · Punteggio ${formatScore(scoreByEntrantId[player.id] ?: 0.0)}",
            style = MaterialTheme.typography.bodyMedium,
            color = OverlayTextSecondary,
        )
    }
}

private fun formatScore(n: Double): String = if (n % 1.0 == 0.0) n.toInt().toString() else n.toString().replace('.', ',')
