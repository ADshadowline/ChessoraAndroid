package org.chessora.app.ui.tournamentmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.apiErrorMessage
import org.chessora.app.data.remote.dto.EntrantDto
import org.chessora.app.data.remote.dto.StandingsRow
import org.chessora.app.data.remote.dto.TournamentRound
import org.chessora.app.data.remote.dto.TournamentViewMode
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Turni (bozza E pubblicati) di un torneo InCorso, lato organizzatore - a differenza di
 * PairingsViewModel (sola lettura, solo turni Published) qui si generano/pubblicano
 * turni e si inseriscono risultati, vedi ChessoraRepository (sezione "Gestione tornei"). */
class TournamentManagerRoundsViewModel(private val repository: ChessoraRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<TournamentRound>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TournamentRound>>> = _state.asStateFlow()

    private val _standings = MutableStateFlow<List<StandingsRow>>(emptyList())
    val standings: StateFlow<List<StandingsRow>> = _standings.asStateFlow()

    /** Iscritti effettivi - servono solo quando non c'è ancora nessun turno generato (vedi
     * TournamentManagerRoundsScreen), ma vengono comunque ricaricati a ogni giro insieme al
     * resto: sono pochi e cambiano raramente, non vale la pena una fetch condizionale. */
    private val _entrants = MutableStateFlow<List<EntrantDto>>(emptyList())
    val entrants: StateFlow<List<EntrantDto>> = _entrants.asStateFlow()

    /** Abbinamenti o classifica provvisoria - condivisa col sito (vedi
     * ChessoraRepository.getTournamentViewState/setTournamentViewState): [load] la rilegge
     * a ogni giro del polling per seguire un cambio fatto altrove (sito, o un altro
     * organizzatore), [setViewMode] la scrive quando è QUESTO client a cambiarla. */
    private val _viewMode = MutableStateFlow(TournamentViewMode.PAIRINGS)
    val viewMode: StateFlow<TournamentViewMode> = _viewMode.asStateFlow()

    /** True mentre una generazione/pubblicazione/inserimento risultato è in corso - disabilita
     * i pulsanti per evitare doppie chiamate (es. due tap veloci su "Genera turno"). */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Intervallo di polling (Club.PollingIntervalMs, configurabile per circolo dalla
     * superamministrazione) - riletto a ogni [load] cosi' un cambio impostato mentre
     * questa schermata è aperta si applica entro un ciclo. 8000 finché il primo giro non
     * ha ancora risposto (stesso default applicato lato server). */
    private val _pollingIntervalMs = MutableStateFlow(8_000L)
    val pollingIntervalMs: StateFlow<Long> = _pollingIntervalMs.asStateFlow()

    fun load(idTournament: Int) {
        viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            _state.value = repository.getOrganizerRounds(idTournament).toUiState()
            repository.getTournamentStandings(idTournament).onSuccess { _standings.value = it }
            repository.getTournamentViewState(idTournament).onSuccess { remote -> _viewMode.value = remote }
            repository.getEntrants(idTournament).onSuccess { _entrants.value = it }
            repository.getPollingIntervalMs(idTournament).onSuccess { _pollingIntervalMs.value = it.toLong() }
        }
    }

    fun setViewMode(idTournament: Int, mode: TournamentViewMode) {
        if (_viewMode.value == mode) return
        _viewMode.value = mode
        viewModelScope.launch { repository.setTournamentViewState(idTournament, mode) }
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
