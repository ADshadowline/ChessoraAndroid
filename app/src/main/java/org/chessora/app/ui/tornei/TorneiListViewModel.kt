package org.chessora.app.ui.tornei

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.Torneo
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

class TorneiListViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Torneo>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Torneo>>> = _state.asStateFlow()

    private var loadedForClubId: Int? = null

    fun load(idClub: Int) {
        if (loadedForClubId == idClub && _state.value is UiState.Success) return
        loadedForClubId = idClub
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getTornei(idClub)
                .map { list -> list.sortedByDescending { it.date.maxOfOrNull { d -> d.dataOra } ?: "" } }
                .toUiState()
        }
    }
}
