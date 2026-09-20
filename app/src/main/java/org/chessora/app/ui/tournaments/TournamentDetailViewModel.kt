package org.chessora.app.ui.tournaments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.apiErrorMessage
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Un torneo scelto tra quelli dello stesso evento (l'evento nel suo insieme può avere
 * più tornei "fratelli", es. "Open A"/"Open B" o le categorie di un CIS) - un socio può
 * preiscriversi a un SOLO torneo per evento (vedi TournamentDetailData.canRegisterAnother
 * e TournamentRegistrationService.PreRegisterAsync per il controllo lato server). */
data class TournamentEventOption(
    val tournament: TournamentSummary,
    val isRegistered: Boolean,
)

data class TournamentDetailData(
    /** Il torneo con cui è stato aperto il dettaglio (dalla Home) - usato solo per le
     * informazioni condivise dall'intero evento (luogo, bando, link), identiche per
     * ogni fratello. */
    val tournament: TournamentSummary,
    val eventoNome: String,
    /** Un solo elemento se il torneo non ha fratelli, altrimenti uno per ciascun
     * torneo dell'evento (compreso [tournament]), ordinati per id. */
    val options: List<TournamentEventOption>,
    /** False = l'utente non ha effettuato il login (Google/ID FIDE/email) - la
     * preiscrizione richiede un account (vedi ui/auth/). */
    val isLoggedIn: Boolean,
) {
    /** True se il chiamante è già preiscritto a un torneo di questo evento (qualunque
     * esso sia) - gli altri tornei dell'evento vanno mostrati disabilitati finché non
     * ritira quella preiscrizione (un solo torneo per evento). */
    val hasAnyRegistration: Boolean get() = options.any { it.isRegistered }
}

/**
 * Dettaglio di UN EVENTO (mai i singoli giorni di gioco - quelli restano solo nel
 * calendario): se l'evento ha più tornei "fratelli" li mostra tutti, ciascuno con il
 * proprio tempo di gioco, e permette la PREISCRIZIONE (non conferma di partecipazione,
 * la conferma reale avviene in loco al torneo) A UNO SOLO di essi per volta - per
 * sceglierne un altro va prima ritirata la preiscrizione corrente (cancelRegistration).
 * Un secondo tocco sullo stesso torneo è idempotente (lato server, vedi
 * TournamentRegistrationService.PreRegisterAsync). Richiede login (ui/auth/): l'identità
 * del chiamante è sempre quella dell'utente autenticato, mai passata dal client.
 */
class TournamentDetailViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TournamentDetailData>>(UiState.Loading)
    val state: StateFlow<UiState<TournamentDetailData>> = _state.asStateFlow()

    private val _registering = MutableStateFlow(false)
    val registering: StateFlow<Boolean> = _registering.asStateFlow()

    fun load(idTournament: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val isLoggedIn = clubPreferences.isAuthenticated.first()
            val myTorneiIds = if (isLoggedIn) myPreRegisteredTournamentIds() else emptySet()
            _state.value = repository.getTornei().mapCatching { list ->
                val tournament = list.first { it.id == idTournament }
                val eventGroupId = tournament.eventGroupId
                val eventoOptions = (if (eventGroupId != null) list.filter { it.eventGroupId == eventGroupId } else listOf(tournament))
                    .sortedBy { it.id }
                    .map { TournamentEventOption(it, it.id in myTorneiIds) }
                TournamentDetailData(tournament, tournament.eventoNome, eventoOptions, isLoggedIn)
            }.toUiState()
        }
    }

    private suspend fun myPreRegisteredTournamentIds(): Set<Int> =
        repository.getMyPreRegistrations().getOrNull()?.map { it.id }?.toSet() ?: emptySet()

    /** Due esiti possibili, decisi da [TournamentDetailData]: non loggato →
     * [onNeedsLogin]; loggato → preiscrizione diretta a [idTournament] (uno specifico
     * tra le [TournamentDetailData.options]), l'identità è quella dell'utente
     * autenticato (Bearer), mai passata dal client. */
    fun register(idTournament: Int, onNeedsLogin: () -> Unit, onError: (String) -> Unit) {
        val current = _state.value
        if (current !is UiState.Success) return
        if (!current.data.isLoggedIn) {
            onNeedsLogin()
            return
        }
        viewModelScope.launch {
            _registering.value = true
            repository.preRegisterForTournament(idTournament)
                .onSuccess { result ->
                    _state.value = UiState.Success(withUpdatedOption(current.data, idTournament, isRegistered = true, result.nPreRegisteredPlayers, result.limiteIscrizioni))
                }
                .onFailure { onError(it.apiErrorMessage() ?: "Impossibile completare la preiscrizione. Riprova.") }
            _registering.value = false
        }
    }

    /** Ritira la preiscrizione a [idTournament] - permette di sceglierne un altro tra i
     * fratelli dello stesso evento (un solo torneo per evento, vedi
     * TournamentDetailData.hasAnyRegistration). */
    fun cancelRegistration(idTournament: Int, onError: (String) -> Unit) {
        val current = _state.value
        if (current !is UiState.Success) return
        viewModelScope.launch {
            _registering.value = true
            repository.cancelPreRegistration(idTournament)
                .onSuccess { result ->
                    _state.value = UiState.Success(withUpdatedOption(current.data, idTournament, isRegistered = false, result.nPreRegisteredPlayers, result.limiteIscrizioni))
                }
                .onFailure { onError(it.apiErrorMessage() ?: "Impossibile ritirare la preiscrizione. Riprova.") }
            _registering.value = false
        }
    }

    private fun withUpdatedOption(data: TournamentDetailData, idTournament: Int, isRegistered: Boolean, nPreRegisteredPlayers: Int, limiteIscrizioni: Int): TournamentDetailData {
        val options = data.options.map { option ->
            if (option.tournament.id != idTournament) option
            else option.copy(
                isRegistered = isRegistered,
                tournament = option.tournament.copy(nPreRegisteredPlayers = nPreRegisteredPlayers, limiteIscrizioni = limiteIscrizioni),
            )
        }
        val tournament = if (data.tournament.id == idTournament) options.first { it.tournament.id == idTournament }.tournament else data.tournament
        return data.copy(tournament = tournament, options = options)
    }
}
