package org.chessora.app.ui.mygames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.data.repository.MyGameCache
import org.chessora.app.ui.common.toFriendlyMessage

sealed class DeleteGameUiState {
    data object Idle : DeleteGameUiState()
    data object Deleting : DeleteGameUiState()
    data object Deleted : DeleteGameUiState()
    data class Error(val message: String) : DeleteGameUiState()
}

/** Cancellazione di una partita dal visualizzatore (DELETE api/games/{id}). */
class MyGameViewerViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {
    private val _deleteState = MutableStateFlow<DeleteGameUiState>(DeleteGameUiState.Idle)
    val deleteState: StateFlow<DeleteGameUiState> = _deleteState.asStateFlow()

    fun delete(gameId: Int) {
        viewModelScope.launch {
            _deleteState.value = DeleteGameUiState.Deleting
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            if (idPlayer == null) {
                _deleteState.value = DeleteGameUiState.Error("Devi identificarti come socio per eliminare una partita.")
                return@launch
            }
            repository.deleteMyGame(gameId, idPlayer)
                .onSuccess {
                    MyGameCache.remove(gameId)
                    _deleteState.value = DeleteGameUiState.Deleted
                }
                .onFailure { _deleteState.value = DeleteGameUiState.Error(it.toFriendlyMessage()) }
        }
    }
}
