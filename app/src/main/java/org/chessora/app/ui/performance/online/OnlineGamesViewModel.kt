package org.chessora.app.ui.performance.online

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.LichessGame
import org.chessora.app.data.repository.ChessComGame
import org.chessora.app.data.repository.ChessComGameCache
import org.chessora.app.data.repository.ChessComRepository
import org.chessora.app.data.repository.ChessComResult
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.toFriendlyMessage
import org.chessora.app.data.repository.LichessRepository
import org.chessora.app.ui.common.UiState

/** [lichessUsername]/[chessComUsername] null = il socio non ha ancora configurato quel sito
 * in Impostazioni (vedi OnlineGamesScreen: messaggio "non configurato" solo se ENTRAMBI sono
 * null - con uno solo configurato si mostrano comunque le sue partite). */
data class OnlineGamesData(val lichessUsername: String?, val chessComUsername: String?, val games: List<OnlineGameSummary>)

private const val GAMES_LIMIT_PER_SOURCE = 50

/** Elenco unico "Scacchi Online": partite Lichess e Chess.com mescolate e ordinate per data
 * (più recenti prima), ciascuna riga porta con sé [OnlineGameSummary.source] per il badge di
 * provenienza (vedi OnlineGamesScreen) e per sapere quale visualizzatore aprire al tocco
 * (LichessGameViewerScreen o ChessComGameViewerScreen, vedi ChessoraNavHost). Un sito con lo
 * username non configurato viene semplicemente saltato, non è un errore; un sito configurato
 * ma che risponde con un errore (username inesistente, timeout, ecc.) degrada a "zero
 * partite da quel sito" invece di far fallire l'intero elenco misto. */
class OnlineGamesViewModel(
    private val repository: ChessoraRepository,
    private val lichessRepository: LichessRepository,
    private val chessComRepository: ChessComRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<OnlineGamesData>>(UiState.Loading)
    val state: StateFlow<UiState<OnlineGamesData>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            // Mai lasciare che un imprevisto (dato malformato da uno dei due siti, ecc.)
            // mandi in crash l'intera app: una coroutine lanciata da viewModelScope con
            // un'eccezione non catturata la farebbe comunque, a differenza dei singoli
            // fetchGames già protetti da Result/getOrDefault qui sotto.
            try {
                val idPlayer = clubPreferences.identifiedPlayerId.first()
                val settings = idPlayer?.let { repository.getLichessSettings(it).getOrNull() }
                val lichessUsername = settings?.lichessUsername?.takeIf { it.isNotBlank() }
                val chessComUsername = settings?.chessComUsername?.takeIf { it.isNotBlank() }

                if (lichessUsername == null && chessComUsername == null) {
                    _state.value = UiState.Success(OnlineGamesData(null, null, emptyList()))
                    return@launch
                }

                val lichessGames = lichessUsername
                    ?.let { lichessRepository.fetchGames(it, GAMES_LIMIT_PER_SOURCE).getOrDefault(emptyList()) }
                    ?: emptyList()
                val chessComGames = chessComUsername
                    ?.let { chessComRepository.fetchGames(it, GAMES_LIMIT_PER_SOURCE).getOrDefault(emptyList()) }
                    ?: emptyList()
                ChessComGameCache.putAll(chessComGames)

                val summaries = (lichessGames.map { it.toSummary(lichessUsername ?: "") } + chessComGames.map { it.toSummary() })
                    .sortedByDescending { it.createdAt }

                _state.value = UiState.Success(OnlineGamesData(lichessUsername, chessComUsername, summaries))
            } catch (e: Exception) {
                _state.value = UiState.Error(e.toFriendlyMessage())
            }
        }
    }
}

private fun LichessGame.toSummary(myUsername: String): OnlineGameSummary {
    val amIWhite = players.white.user?.name.equals(myUsername, ignoreCase = true)
    val me = if (amIWhite) players.white else players.black
    val opponent = if (amIWhite) players.black else players.white
    val result = when {
        winner == null -> OnlineGameResult.DRAW
        (winner == "white") == amIWhite -> OnlineGameResult.WIN
        else -> OnlineGameResult.LOSS
    }
    return OnlineGameSummary(
        source = OnlineGameSource.LICHESS,
        id = id,
        opponentName = opponent.displayName(),
        myRating = me.rating,
        result = result,
        speedLabel = speed.replaceFirstChar { it.uppercase() },
        createdAt = createdAt,
    )
}

private fun ChessComGame.toSummary(): OnlineGameSummary = OnlineGameSummary(
    source = OnlineGameSource.CHESSCOM,
    id = id,
    opponentName = opponentName,
    myRating = myRating,
    result = when (result) {
        ChessComResult.WIN -> OnlineGameResult.WIN
        ChessComResult.LOSS -> OnlineGameResult.LOSS
        ChessComResult.DRAW -> OnlineGameResult.DRAW
    },
    speedLabel = speed.replaceFirstChar { it.uppercase() },
    createdAt = createdAt,
)
