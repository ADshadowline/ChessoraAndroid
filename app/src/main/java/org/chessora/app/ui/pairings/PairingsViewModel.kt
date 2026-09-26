package org.chessora.app.ui.pairings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.StandingsRow
import org.chessora.app.data.remote.dto.TournamentRound
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Turni pubblicati e classifica di un torneo avviato - stesso dato mostrato da
 * abbinamenti-risultati.html sul sito, entrambi pubblici (nessun login richiesto, vedi
 * PublicTorneoController.GetRounds/GetStandings lato server). */
data class PairingsData(
    val rounds: List<TournamentRound>,
    val standings: List<StandingsRow>,
) {
    /** L'ultimo turno pubblicato è quello "in corso" dal punto di vista di chi guarda -
     * i turni precedenti restano comunque consultabili tramite [rounds]. */
    val currentRound: TournamentRound? get() = rounds.maxByOrNull { it.roundNumber }
}

class PairingsViewModel(private val repository: ChessoraRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<PairingsData>>(UiState.Loading)
    val state: StateFlow<UiState<PairingsData>> = _state.asStateFlow()

    /** Intervallo di polling (Club.PollingIntervalMs, configurabile per circolo dalla
     * superamministrazione) - riletto a ogni [load] cosi' un cambio impostato mentre questa
     * schermata è aperta si applica entro un ciclo. 8000 finché il primo giro non ha ancora
     * risposto (stesso default applicato lato server). */
    private val _pollingIntervalMs = MutableStateFlow(8_000L)
    val pollingIntervalMs: StateFlow<Long> = _pollingIntervalMs.asStateFlow()

    /** Non azzera a Loading se già in Success (vedi PairingsScreen: chiamato ogni pochi
     * secondi per un aggiornamento quasi in tempo reale - un risultato/turno può cambiare
     * dal sito o da "Gestione tornei" mentre questa schermata resta aperta - altrimenti
     * lampeggerebbe uno spinner a ogni giro). */
    fun load(idTournament: Int) {
        viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            _state.value = repository.getTournamentRounds(idTournament)
                .mapCatching { rounds ->
                    val standings = repository.getTournamentStandings(idTournament).getOrDefault(emptyList())
                    PairingsData(rounds = rounds, standings = standings)
                }
                .toUiState()
            repository.getPollingIntervalMs(idTournament).onSuccess { _pollingIntervalMs.value = it.toLong() }
        }
    }
}
