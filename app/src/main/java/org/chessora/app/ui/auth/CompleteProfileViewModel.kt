package org.chessora.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.apiErrorMessage
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.remote.dto.ClubRosterEntryDto
import org.chessora.app.data.remote.dto.CompletePlayerProfileRequestDto
import org.chessora.app.data.remote.dto.FidePlayerSearchResultDto
import org.chessora.app.data.repository.ChessoraRepository

/** Secondo passo dopo un primo login Google (account già autenticato, Bearer già
 * valido, ma senza ancora un IdPlayer) - vedi CompleteProfileScreen.kt. A differenza di
 * [LoginFideViewModel] non serve mai email/password: l'account esiste già. */
sealed interface ProfileUiPhase {
    data object AskHasFide : ProfileUiPhase
    data object EnterIdFide : ProfileUiPhase
    data object Loading : ProfileUiPhase
    data object AskClubMembership : ProfileUiPhase
    data object PickClub : ProfileUiPhase
    data object PickRoster : ProfileUiPhase
    /** Nome trovato in anagrafica FIDE, pronto per completare il profilo con
     * [CompleteProfileViewModel.confirmFide]. */
    data class ConfirmFide(val name: String) : ProfileUiPhase
    data object AmateurDetails : ProfileUiPhase
    data object Done : ProfileUiPhase
    data class Error(val message: String) : ProfileUiPhase
}

class CompleteProfileViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _phase = MutableStateFlow<ProfileUiPhase>(ProfileUiPhase.AskHasFide)
    val phase: StateFlow<ProfileUiPhase> = _phase

    private val _searchResults = MutableStateFlow<List<FidePlayerSearchResultDto>>(emptyList())
    val searchResults: StateFlow<List<FidePlayerSearchResultDto>> = _searchResults

    private val _clubs = MutableStateFlow<List<ClubDirectoryItem>>(emptyList())
    val clubs: StateFlow<List<ClubDirectoryItem>> = _clubs

    private val _roster = MutableStateFlow<List<ClubRosterEntryDto>>(emptyList())
    val roster: StateFlow<List<ClubRosterEntryDto>> = _roster

    private var idFide: Int? = null
    private var resolvedName: String = ""
    private var resolvedIdClub: Int? = null
    private var isClubMember: Boolean = false
    private var selectedIdClub: Int? = null
    private var selectedIdPlayer: Int? = null

    fun answerHasFide(hasFide: Boolean) {
        _phase.value = if (hasFide) ProfileUiPhase.EnterIdFide else ProfileUiPhase.AmateurDetails
    }

    private var searchJob: Job? = null

    /** Cerca automaticamente man mano che si digita, a partire dal 4° carattere - stesso
     * comportamento/motivazione di LoginFideViewModel.searchByName. */
    fun searchByName(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 4) {
            _searchResults.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            repository.searchFide(query).onSuccess { _searchResults.value = it }
        }
    }

    fun selectFromSearch(result: FidePlayerSearchResultDto) = checkFide(result.idFide)

    fun submitIdFide(value: String) {
        val parsed = value.trim().toIntOrNull()
        if (parsed == null) {
            _phase.value = ProfileUiPhase.Error("Inserisci un ID FIDE valido (solo numeri).")
            return
        }
        checkFide(parsed)
    }

    private fun checkFide(value: Int) {
        idFide = value
        _phase.value = ProfileUiPhase.Loading
        viewModelScope.launch {
            repository.checkFide(value)
                .onSuccess { result ->
                    if (result == null) {
                        _phase.value = ProfileUiPhase.Error("ID FIDE non trovato.")
                        return@onSuccess
                    }
                    if (result.alreadyRegistered) {
                        _phase.value = ProfileUiPhase.Error("Questo ID FIDE è già associato a un altro utente.")
                        return@onSuccess
                    }
                    resolvedName = result.name
                    if (result.resolvedIdClub != null) {
                        resolvedIdClub = result.resolvedIdClub
                        isClubMember = true
                        _phase.value = ProfileUiPhase.ConfirmFide(result.name)
                    } else {
                        _phase.value = ProfileUiPhase.AskClubMembership
                    }
                }
                .onFailure { _phase.value = ProfileUiPhase.Error(it.apiErrorMessage() ?: "Connessione non riuscita, riprova.") }
        }
    }

    fun answerClubMembership(member: Boolean) {
        isClubMember = member
        if (!member) {
            _phase.value = ProfileUiPhase.ConfirmFide(resolvedName)
        } else {
            _phase.value = ProfileUiPhase.Loading
            viewModelScope.launch {
                repository.getClubDirectory()
                    .onSuccess { _clubs.value = it; _phase.value = ProfileUiPhase.PickClub }
                    .onFailure { _phase.value = ProfileUiPhase.Error("Impossibile caricare l'elenco circoli. Riprova.") }
            }
        }
    }

    fun selectClub(club: ClubDirectoryItem) {
        _phase.value = ProfileUiPhase.Loading
        viewModelScope.launch {
            repository.resolveClubByCode(club.publicCode)
                .onSuccess { resolvedId ->
                    selectedIdClub = resolvedId
                    repository.getClubRoster(resolvedId)
                        .onSuccess { _roster.value = it; _phase.value = ProfileUiPhase.PickRoster }
                        .onFailure { _phase.value = ProfileUiPhase.Error("Impossibile caricare l'elenco soci. Riprova.") }
                }
                .onFailure { _phase.value = ProfileUiPhase.Error("Circolo non valido. Riprova.") }
        }
    }

    fun selectRosterEntry(entry: ClubRosterEntryDto) {
        selectedIdPlayer = entry.idPlayer
        _phase.value = ProfileUiPhase.ConfirmFide(resolvedName)
    }

    fun confirmFide() {
        val value = idFide ?: return
        _phase.value = ProfileUiPhase.Loading
        viewModelScope.launch {
            repository.completeProfile(
                CompletePlayerProfileRequestDto(
                    hasIdFide = true,
                    idFide = value,
                    isClubMember = isClubMember,
                    selectedIdClub = selectedIdClub ?: resolvedIdClub,
                    selectedIdPlayer = selectedIdPlayer,
                ),
            )
                .onSuccess { auth ->
                    AuthSessionPersister.persist(auth, resolvedName, authPreferences, clubPreferences)
                    _phase.value = ProfileUiPhase.Done
                }
                .onFailure { _phase.value = ProfileUiPhase.Error(it.apiErrorMessage() ?: "Impossibile completare il profilo. Riprova.") }
        }
    }

    fun submitAmateur(firstName: String, lastName: String, password: String, confirmPassword: String) {
        if (firstName.isBlank() || lastName.isBlank() || password.isBlank()) {
            _phase.value = ProfileUiPhase.Error("Compila tutti i campi.")
            return
        }
        if (password != confirmPassword) {
            _phase.value = ProfileUiPhase.Error("Le due password non coincidono.")
            return
        }
        _phase.value = ProfileUiPhase.Loading
        viewModelScope.launch {
            repository.completeProfile(
                CompletePlayerProfileRequestDto(
                    hasIdFide = false,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    password = password,
                    confirmPassword = confirmPassword,
                ),
            )
                .onSuccess { auth ->
                    AuthSessionPersister.persist(auth, "${firstName.trim()} ${lastName.trim()}", authPreferences, clubPreferences)
                    _phase.value = ProfileUiPhase.Done
                }
                .onFailure { _phase.value = ProfileUiPhase.Error(it.apiErrorMessage() ?: "Impossibile completare il profilo. Riprova.") }
        }
    }

    fun retry() {
        _phase.value = ProfileUiPhase.AskHasFide
    }
}
