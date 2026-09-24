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

    fun load(idTournament: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getTournamentRounds(idTournament)
                .mapCatching { rounds ->
                    val standings = repository.getTournamentStandings(idTournament).getOrDefault(emptyList())
                    PairingsData(rounds = rounds, standings = standings)
                }
                .toUiState()
        }
    }
}
