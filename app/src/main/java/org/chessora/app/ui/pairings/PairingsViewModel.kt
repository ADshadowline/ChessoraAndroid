package org.chessora.app.ui.pairings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.chessora.app.data.remote.dto.StandingsRow
import org.chessora.app.data.remote.dto.TournamentRound
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Turni pubblicati e classifica di un torneo avviato - stesso dato mostrato da
 * abbinamenti-risultati.html sul sito, entrambi pubblici (nessun login richiesto, vedi
 * PublicTorneoController.GetRounds/GetStandings lato server). */
data class PairingsData(
    val rounds: List<TournamentRound>,
    val standings: List<StandingsRow>,
    /** Coefficiente di patta per le probabilità delle singole partite (vedi TournamentOdds). */
    val drawCoefficient: Double,
    /** Turni ancora da giocare (0 se il torneo è concluso o il numero di turni non è noto). */
    val roundsLeft: Int,
    /** Probabilità di vincere il torneo / andare a podio per entrantId - null se non ci sono
     * turni da simulare (torneo concluso). */
    val chances: Map<Int, TournamentChance>?,
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

    /** Turni previsti, tipo di tempo e stato del torneo: letti una volta sola. */
    private var tournament: TournamentSummary? = null

    /** Ultima simulazione e i dati da cui è nata: il polling ricarica ogni pochi secondi,
     * la simulazione si rifà solo quando cambiano punti, Elo o turni rimasti. */
    private var lastSimulationKey: Any? = null
    private var lastChances: Map<Int, TournamentChance>? = null

    /** Non azzera a Loading se già in Success (vedi PairingsScreen: chiamato ogni pochi
     * secondi per un aggiornamento quasi in tempo reale - un risultato/turno può cambiare
     * dal sito o da "Gestione tornei" mentre questa schermata resta aperta - altrimenti
     * lampeggerebbe uno spinner a ogni giro). */
    fun load(idTournament: Int) {
        viewModelScope.launch {
            if (_state.value !is UiState.Success) _state.value = UiState.Loading
            if (tournament == null) {
                tournament = repository.getTornei().getOrNull()?.firstOrNull { it.id == idTournament }
            }
            _state.value = repository.getTournamentRounds(idTournament)
                .mapCatching { rounds ->
                    val standings = repository.getTournamentStandings(idTournament).getOrDefault(emptyList())
                    buildData(rounds, standings)
                }
                .toUiState()
            repository.getPollingIntervalMs(idTournament).onSuccess { _pollingIntervalMs.value = it.toLong() }
        }
    }

    private suspend fun buildData(rounds: List<TournamentRound>, standings: List<StandingsRow>): PairingsData {
        val t = tournament
        val k = TournamentOdds.drawCoefficient(t?.tipologiaTempo ?: -1, rounds)
        val completed = TournamentOdds.completedRounds(rounds)
        val concluded = t?.lifecycleStatus == 2
        val roundsLeft = if (concluded || t == null) 0 else maxOf(0, (if (t.turni > 0) t.turni else completed) - completed)
        val key = Triple(standings.map { Triple(it.entrantId, it.rating, it.score) }, roundsLeft, k)
        if (key != lastSimulationKey) {
            lastChances = withContext(Dispatchers.Default) {
                TournamentOdds.simulate(standings.map { it.rating }, standings.map { it.score }, roundsLeft, k)
                    ?.let { list -> standings.mapIndexed { i, row -> row.entrantId to list[i] }.toMap() }
            }
            lastSimulationKey = key
        }
        return PairingsData(rounds, standings, k, roundsLeft, lastChances)
    }
}
