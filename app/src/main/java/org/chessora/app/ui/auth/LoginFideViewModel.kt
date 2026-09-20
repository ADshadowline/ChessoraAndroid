package org.chessora.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.apiErrorMessage
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.remote.dto.ClubRosterEntryDto
import org.chessora.app.data.remote.dto.FidePlayerSearchResultDto
import org.chessora.app.data.remote.dto.RegisterWithFideRequestDto
import org.chessora.app.data.repository.ChessoraRepository

/** Fasi della schermata "Accesso con ID FIDE" (vedi LoginFideScreen.kt) - a differenza di
 * un semplice sealed state con un dato per fase, i dati intermedi (idFide risolto, nome,
 * club scelto...) restano su campi mutabili del ViewModel: si accumulano attraverso più
 * fasi (es. idFide resta noto sia in [EnterPassword] sia in [EnterCredentials]) ed
 * esporli tutti su ogni sottotipo sarebbe più boilerplate che chiarezza. */
sealed interface FideUiPhase {
    data object EnterIdFide : FideUiPhase
    data object Loading : FideUiPhase
    /** ID FIDE già associato a un account - serve solo la password (vedi
     * PlayerAuthController.Login, che accetta l'ID FIDE come "emailOrIdFide"). */
    data object EnterPassword : FideUiPhase
    /** ID FIDE valido, non ancora associato a nessuno, club non auto-risolvibile da
     * orga.PlayerCard - chiede se il socio è socio di un circolo. */
    data object AskClubMembership : FideUiPhase
    data object PickClub : FideUiPhase
    data object PickRoster : FideUiPhase
    /** Registrazione: email + password + conferma (isClubMember/selectedIdClub/
     * selectedIdPlayer già risolti nei passi precedenti). */
    data object EnterCredentials : FideUiPhase
    data object Registered : FideUiPhase
    data object LoggedIn : FideUiPhase
    data class Error(val message: String) : FideUiPhase
}

class LoginFideViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _phase = MutableStateFlow<FideUiPhase>(FideUiPhase.EnterIdFide)
    val phase: StateFlow<FideUiPhase> = _phase

    private val _searchResults = MutableStateFlow<List<FidePlayerSearchResultDto>>(emptyList())
    val searchResults: StateFlow<List<FidePlayerSearchResultDto>> = _searchResults

    private val _clubs = MutableStateFlow<List<ClubDirectoryItem>>(emptyList())
    val clubs: StateFlow<List<ClubDirectoryItem>> = _clubs

    private val _roster = MutableStateFlow<List<ClubRosterEntryDto>>(emptyList())
    val roster: StateFlow<List<ClubRosterEntryDto>> = _roster

    var resolvedName: String = ""; private set

    private var idFide: Int? = null
    private var resolvedIdClub: Int? = null
    private var isClubMember: Boolean = false
    private var selectedIdClub: Int? = null
    private var selectedIdPlayer: Int? = null

    fun searchByName(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.searchFide(query).onSuccess { _searchResults.value = it }
        }
    }

    fun selectFromSearch(result: FidePlayerSearchResultDto) = checkFide(result.idFide)

    fun submitIdFide(value: String) {
        val parsed = value.trim().toIntOrNull()
        if (parsed == null) {
            _phase.value = FideUiPhase.Error("Inserisci un ID FIDE valido (solo numeri).")
            return
        }
        checkFide(parsed)
    }

    private fun checkFide(value: Int) {
        idFide = value
        _phase.value = FideUiPhase.Loading
        viewModelScope.launch {
            repository.checkFide(value)
                .onSuccess { result ->
                    if (result == null) {
                        _phase.value = FideUiPhase.Error("ID FIDE non trovato.")
                        return@onSuccess
                    }
                    resolvedName = result.name
                    if (result.alreadyRegistered) {
                        _phase.value = FideUiPhase.EnterPassword
                    } else if (result.resolvedIdClub != null) {
                        resolvedIdClub = result.resolvedIdClub
                        isClubMember = true
                        _phase.value = FideUiPhase.EnterCredentials
                    } else {
                        _phase.value = FideUiPhase.AskClubMembership
                    }
                }
                .onFailure { _phase.value = FideUiPhase.Error(it.apiErrorMessage() ?: "Connessione non riuscita, riprova.") }
        }
    }

    fun answerClubMembership(member: Boolean) {
        isClubMember = member
        if (!member) {
            _phase.value = FideUiPhase.EnterCredentials
        } else {
            _phase.value = FideUiPhase.Loading
            viewModelScope.launch {
                repository.getClubDirectory()
                    .onSuccess { _clubs.value = it; _phase.value = FideUiPhase.PickClub }
                    .onFailure { _phase.value = FideUiPhase.Error("Impossibile caricare l'elenco circoli. Riprova.") }
            }
        }
    }

    fun selectClub(club: ClubDirectoryItem) {
        _phase.value = FideUiPhase.Loading
        viewModelScope.launch {
            repository.resolveClubByCode(club.publicCode)
                .onSuccess { resolvedIdClub ->
                    selectedIdClub = resolvedIdClub
                    repository.getClubRoster(resolvedIdClub)
                        .onSuccess { _roster.value = it; _phase.value = FideUiPhase.PickRoster }
                        .onFailure { _phase.value = FideUiPhase.Error("Impossibile caricare l'elenco soci. Riprova.") }
                }
                .onFailure { _phase.value = FideUiPhase.Error("Circolo non valido. Riprova.") }
        }
    }

    fun selectRosterEntry(entry: ClubRosterEntryDto) {
        selectedIdPlayer = entry.idPlayer
        _phase.value = FideUiPhase.EnterCredentials
    }

    fun loginWithPassword(password: String) {
        val value = idFide ?: return
        _phase.value = FideUiPhase.Loading
        viewModelScope.launch {
            repository.playerLogin(value.toString(), password)
                .onSuccess { auth ->
                    AuthSessionPersister.persist(auth, resolvedName, authPreferences, clubPreferences)
                    _phase.value = FideUiPhase.LoggedIn
                }
                .onFailure { _phase.value = FideUiPhase.Error(it.apiErrorMessage() ?: "Credenziali non valide.") }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        val value = idFide ?: return
        if (email.isBlank() || password.isBlank()) {
            _phase.value = FideUiPhase.Error("Inserisci email e password.")
            return
        }
        if (password != confirmPassword) {
            _phase.value = FideUiPhase.Error("Le due password non coincidono.")
            return
        }
        _phase.value = FideUiPhase.Loading
        viewModelScope.launch {
            repository.registerWithFide(
                RegisterWithFideRequestDto(
                    idFide = value,
                    email = email.trim(),
                    password = password,
                    isClubMember = isClubMember,
                    selectedIdClub = selectedIdClub ?: resolvedIdClub,
                    selectedIdPlayer = selectedIdPlayer,
                ),
            )
                .onSuccess { _phase.value = FideUiPhase.Registered }
                .onFailure { _phase.value = FideUiPhase.Error(it.apiErrorMessage() ?: "Registrazione non riuscita. Riprova.") }
        }
    }

    fun retry() {
        _phase.value = FideUiPhase.EnterIdFide
    }
}
