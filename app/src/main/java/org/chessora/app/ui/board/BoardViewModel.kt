package org.chessora.app.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/**
 * GET /api/board senza [year] restituisce già l'anno più recente disponibile
 * (vedi il commento su BoardController.GetBoard nel repository server: "Year
 * omitted (or 0) -> most recent year that has any members"), quindi questo
 * ViewModel non richiama GET /api/board/years in questa prima versione - non
 * c'è ancora un selettore di anno nella UI (vedi README.md "Cosa NON è stato
 * fatto" se si vuole aggiungerlo).
 */
class BoardViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<BoardMember>>>(UiState.Loading)
    val state: StateFlow<UiState<List<BoardMember>>> = _state.asStateFlow()

    fun load(club: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getBoard(club)
                .map { members -> members.sortedWith(compareBy({ it.level }, { it.sortOrder })) }
                .toUiState()
        }
    }
}
