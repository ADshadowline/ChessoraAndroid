package org.chessora.app.ui.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.PerformanceHistoryDto
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** null = utente non identificato (nessun modo di sapere quale storico mostrare). */
class PerformanceViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<PerformanceHistoryDto?>>(UiState.Loading)
    val state: StateFlow<UiState<PerformanceHistoryDto?>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            if (idPlayer == null) {
                _state.value = UiState.Success(null)
                return@launch
            }
            _state.value = repository.getPlayerPerformance(idPlayer).toUiState()
        }
    }
}
