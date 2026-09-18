package org.chessora.app.ui.tournaments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.PreRegistrationRequest
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

data class TournamentDetailData(
    val tournament: TournamentSummary,
    /** Altri tornei dello stesso evento (stesso eventGroupId), es. "Open A"/"Open B" -
     * vuoto se questo torneo è da solo. */
    val siblings: List<TournamentSummary>,
    /** False = l'utente non si è mai identificato (Google/telefono): la preiscrizione
     * richiede prima di identificarsi, anche solo per provare di essere una persona reale
     * (vedi ClubPreferences.isAuthenticated). */
    val isAuthenticated: Boolean,
    /** Non null solo se l'utente autenticato è anche un socio riconosciuto - altrimenti
     * la preiscrizione chiede prima idFide o nome+cognome (vedi
     * registerWithManualIdentity). */
    val identifiedPlayerId: Int?,
    val alreadyRegistered: Boolean,
)

/**
 * Dettaglio di UN torneo (l'evento nel suo insieme, mai i singoli giorni di gioco -
 * quelli restano solo nel calendario) più PREISCRIZIONE (non conferma di
 * partecipazione, la conferma reale avviene in loco al torneo): un secondo tocco è
 * idempotente (lato server, vedi TournamentRegistrationService.PreRegisterAsync) - qui
 * in più si nasconde subito il pulsante se [TournamentDetailData.alreadyRegistered] è
 * già true, cosi' non serve nemmeno affidarsi solo all'idempotenza server-side.
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
            val isAuthenticated = clubPreferences.isAuthenticated.first()
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            val myTorneiIds = myPreRegisteredTournamentIds()
            _state.value = repository.getTornei().mapCatching { list ->
                val tournament = list.first { it.id == idTournament }
                val siblings = tournament.eventGroupId
                    ?.let { groupId -> list.filter { it.eventGroupId == groupId && it.id != tournament.id } }
                    ?: emptyList()
                TournamentDetailData(tournament, siblings, isAuthenticated, idPlayer, idTournament in myTorneiIds)
            }.toUiState()
        }
    }

    private suspend fun myPreRegisteredTournamentIds(): Set<Int> {
        val contactId = clubPreferences.preRegistrationContactId.first()
        val idPlayer = clubPreferences.identifiedPlayerId.first()
        val email = clubPreferences.authenticatedEmail.first()
        val phone = clubPreferences.authenticatedPhone.first()
        if (contactId == null && idPlayer == null && email == null && phone == null) return emptySet()
        return repository.getMyPreRegistrations(contactId, idPlayer, email, phone)
            .getOrNull()?.map { it.id }?.toSet() ?: emptySet()
    }

    /** Tre esiti possibili, decisi da [TournamentDetailData]: non autenticato →
     * [onNeedsIdentification] (schermata di identificazione); autenticato ma non socio
     * riconosciuto → [onNeedsManualIdentity] (dialog idFide/nome+cognome, poi
     * registerWithManualIdentity); socio riconosciuto → preiscrizione diretta. */
    fun register(idTournament: Int, onNeedsIdentification: () -> Unit, onNeedsManualIdentity: () -> Unit, onError: (String) -> Unit) {
        val current = _state.value
        if (current !is UiState.Success) return
        if (!current.data.isAuthenticated) {
            onNeedsIdentification()
            return
        }
        val idPlayer = current.data.identifiedPlayerId
        if (idPlayer == null) {
            onNeedsManualIdentity()
            return
        }
        viewModelScope.launch {
            val email = clubPreferences.authenticatedEmail.first()
            val phone = clubPreferences.authenticatedPhone.first()
            doRegister(idTournament, PreRegistrationRequest(idPlayer = idPlayer, email = email, phoneNumber = phone), onError)
        }
    }

    /** Completa la preiscrizione di un utente autenticato ma non (ancora) socio
     * riconosciuto, dopo che ha inserito idFide o nome+cognome nella dialog (almeno uno
     * dei due obbligatorio, validato anche lato server). */
    fun registerWithManualIdentity(idTournament: Int, idFideManuale: String?, displayName: String?, onError: (String) -> Unit) {
        viewModelScope.launch {
            val email = clubPreferences.authenticatedEmail.first()
            val phone = clubPreferences.authenticatedPhone.first()
            doRegister(
                idTournament,
                PreRegistrationRequest(email = email, phoneNumber = phone, displayName = displayName, idFideManuale = idFideManuale),
                onError,
            )
        }
    }

    private suspend fun doRegister(idTournament: Int, request: PreRegistrationRequest, onError: (String) -> Unit) {
        val current = _state.value
        if (current !is UiState.Success) return
        _registering.value = true
        repository.preRegisterForTournament(idTournament, request)
            .onSuccess { result ->
                clubPreferences.setPreRegistrationContactId(result.contactId)
                val updated = current.data.tournament.copy(
                    nPreRegisteredPlayers = result.nPreRegisteredPlayers,
                    limiteIscrizioni = result.limiteIscrizioni,
                )
                _state.value = UiState.Success(current.data.copy(tournament = updated, alreadyRegistered = true))
            }
            .onFailure { onError("Impossibile completare la preiscrizione. Riprova.") }
        _registering.value = false
    }
}
