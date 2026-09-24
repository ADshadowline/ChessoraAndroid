package org.chessora.app.ui.tournamentmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.apiErrorMessage
import org.chessora.app.data.remote.dto.TournamentRound
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Turni (bozza E pubblicati) di un torneo InCorso, lato organizzatore - a differenza di
 * PairingsViewModel (sola lettura, solo turni Published) qui si generano/pubblicano
 * turni e si inseriscono risultati, vedi ChessoraRepository (sezione "Gestione tornei"). */
class TournamentManagerRoundsViewModel(private val repository: ChessoraRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<TournamentRound>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TournamentRound>>> = _state.asStateFlow()

    /** True mentre una generazione/pubblicazione/inserimento risultato è in corso - disabilita
     * i pulsanti per evitare doppie chiamate (es. due tap veloci su "Genera turno"). */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun load(idTournament: Int) {
        viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            _state.value = repository.getOrganizerRounds(idTournament).toUiState()
        }
    }

    fun generateRound(idTournament: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            repository.generateRound(idTournament)
                .onSuccess { load(idTournament) }
                .onFailure { onError(it.apiErrorMessage() ?: "Impossibile generare il turno. Riprova.") }
            _busy.value = false
        }
    }

    fun publishRound(idTournament: Int, roundId: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            repository.publishRound(idTournament, roundId)
                .onSuccess { load(idTournament) }
                .onFailure { onError(it.apiErrorMessage() ?: "Impossibile pubblicare il turno. Riprova.") }
            _busy.value = false
        }
    }

    fun submitResult(idTournament: Int, pairingId: Int, result: String?, onError: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            repository.submitOrganizerResult(idTournament, pairingId, result)
                .onSuccess { load(idTournament) }
                .onFailure { onError(it.apiErrorMessage() ?: "Impossibile salvare il risultato. Riprova.") }
            _busy.value = false
        }
    }
}
