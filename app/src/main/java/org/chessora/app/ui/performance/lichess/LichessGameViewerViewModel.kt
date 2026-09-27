package org.chessora.app.ui.performance.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.LichessGame
import org.chessora.app.data.repository.LichessRepository
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** [positions] ha sempre almeno un elemento (la posizione di partenza) - vedi
 * ChessPositions.fromMoves. Per una variante diversa da "standard" contiene solo la
 * posizione di partenza standard (segnaposto: la scacchiera di quella variante non è
 * quella con cui è iniziata la partita, vedi LichessGameViewerScreen che in quel caso
 * mostra un messaggio invece della scacchiera). */
data class LichessGameViewerData(val game: LichessGame, val positions: List<String>)

class LichessGameViewerViewModel(private val lichessRepository: LichessRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<LichessGameViewerData>>(UiState.Loading)
    val state: StateFlow<UiState<LichessGameViewerData>> = _state.asStateFlow()

    fun load(gameId: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = lichessRepository.fetchGame(gameId)
                .map { game ->
                    val positions = if (game.variant == "standard") ChessPositions.fromMoves(game.moves) else listOf(ChessPositions.STANDARD_START_FEN)
                    LichessGameViewerData(game, positions)
                }
                .toUiState()
        }
    }
}
