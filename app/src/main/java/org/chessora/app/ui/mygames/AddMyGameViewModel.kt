package org.chessora.app.ui.mygames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.SaveGameRequest
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.toFriendlyMessage

sealed class SaveGameUiState {
    data object Idle : SaveGameUiState()
    data object Saving : SaveGameUiState()
    data object Saved : SaveGameUiState()
    data class Error(val message: String) : SaveGameUiState()
}

class AddMyGameViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {
    private val _saveState = MutableStateFlow<SaveGameUiState>(SaveGameUiState.Idle)
    val saveState: StateFlow<SaveGameUiState> = _saveState.asStateFlow()

    fun save(opponentName: String, round: Int?, tournamentName: String?, cadenza: Cadenza, pgn: String) {
        viewModelScope.launch {
            _saveState.value = SaveGameUiState.Saving
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            if (idPlayer == null) {
                _saveState.value = SaveGameUiState.Error("Devi identificarti come socio prima di registrare una partita.")
                return@launch
            }
            repository.saveMyGame(
                SaveGameRequest(
                    idPlayer = idPlayer,
                    opponentName = opponentName,
                    round = round,
                    tournamentName = tournamentName?.takeIf { it.isNotBlank() },
                    tipologiaTempo = cadenza.tipologiaTempo,
                    pgn = pgn,
                ),
            ).onSuccess {
                _saveState.value = SaveGameUiState.Saved
            }.onFailure { e ->
                _saveState.value = SaveGameUiState.Error(e.toFriendlyMessage())
            }
        }
    }
}
