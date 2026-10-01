package org.chessora.app.ui.performance.chesscom

import androidx.lifecycle.ViewModel
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.chessora.app.data.repository.ChessComGame
import org.chessora.app.data.repository.ChessComGameCache
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.common.UiState

/** [positions] ha sempre almeno un elemento (la posizione di partenza) - vedi
 * ChessPositions.fromPgn. Per una variante diversa da "chess" (standard) contiene solo la
 * posizione di partenza standard, stesso comportamento di LichessGameViewerViewModel per le
 * varianti non standard. */
data class ChessComGameViewerData(val game: ChessComGame, val positions: List<String>)

/** A differenza di LichessGameViewerViewModel, non fa alcuna chiamata di rete: l'API
 * pubblica di Chess.com non offre un endpoint per rileggere una singola partita per id, la
 * partita (PGN incluso) è già stata messa da parte in ChessComGameCache quando l'elenco
 * "Scacchi Online" l'ha caricata (vedi OnlineGamesViewModel). Se la cache è vuota (es. il
 * processo è stato ricreato da zero direttamente su questa schermata) mostra un errore
 * invece di un crash, invitando a tornare all'elenco. */
class ChessComGameViewerViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState<ChessComGameViewerData>>(UiState.Loading)
    val state: StateFlow<UiState<ChessComGameViewerData>> = _state.asStateFlow()

    fun load(gameId: String) {
        _state.value = try {
            val game = ChessComGameCache.get(gameId)
            if (game == null) {
                // TEMPORANEO (diagnosi bug segnalato in produzione): porta con sé l'id
                // cercato, per distinguere subito un "cache vuota" (processo ricreato) da
                // un vero mismatch di id tra elenco e cache - vedi ChessComGameCache.
                UiState.Error("Partita non più disponibile (id=$gameId) - torna indietro e riprova dall'elenco.")
            } else {
                val positions = if (game.rules == "chess") ChessPositions.fromPgn(game.pgn) else listOf(ChessPositions.STANDARD_START_FEN)
                UiState.Success(ChessComGameViewerData(game, positions))
            }
        } catch (e: Exception) {
            // Mai lasciare che un imprevisto (dati di una partita in un formato non
            // ancora visto) mandi in crash l'intera app: meglio un errore gestito che
            // l'utente può chiudere, come per qualunque altro errore di caricamento.
            // recordException manda lo stack trace a Firebase Crashlytics come evento
            // non fatale; il messaggio include ANCHE tipo+dettaglio dell'eccezione
            // (TEMPORANEO, solo per diagnosi: vedi commento sopra) cosi' non serve
            // aspettare/consultare Crashlytics per saperlo.
            FirebaseCrashlytics.getInstance().recordException(e)
            UiState.Error("Impossibile aprire questa partita (${e::class.simpleName}: ${e.message}).")
        }
    }
}
