package org.chessora.app.ui.registrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Una riga della schermata "I miei tornei": un torneo a cui il chiamante risulta
 * PREISCRITTO (vedi TournamentRegistration.PreRegisteredPlayers - non è una conferma di
 * partecipazione) - id serve per aprirne il dettaglio. isInProgress (LifecycleStatus
 * InCorso) porta al pallino verde lampeggiante e cambia la destinazione al tocco:
 * abbinamenti/classifica (ui/pairings/) invece del solito dettaglio torneo, dove non si
 * potrebbe comunque più fare nulla (le iscrizioni sono chiuse una volta avviato). */
data class TournamentRegistrationEntry(
    val id: Int,
    val title: String,
    val dateLabel: String,
    val isInProgress: Boolean,
)

/** Schermata "Iscrizioni" (sostituisce il Calendario in bottom bar, spostato dentro
 * "Altro"): elenco dei tornei a cui il chiamante risulta preiscritto (vedi
 * ui/identity/) - orga.TournamentRegistrations non ha una colonna IdClub, quindi questa
 * lista non dipende dal circolo scelto (funziona anche in "modalità piattaforma").
 * Richiede solo di essersi autenticati (Google/telefono), non necessariamente di essere
 * un socio riconosciuto - vedi TournamentDetailViewModel per lo stesso concetto. */
class RegistrationsViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<TournamentRegistrationEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TournamentRegistrationEntry>>> = _state.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun load() {
        viewModelScope.launch { fetch() }
    }

    fun retry() {
        viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        _state.value = UiState.Loading
        val authenticated = clubPreferences.isAuthenticated.first()
        _isAuthenticated.value = authenticated

        if (!authenticated) {
            _state.value = UiState.Success(emptyList())
            return
        }

        _state.value = repository.getMyPreRegistrations()
            .map { tornei ->
                tornei.map { t ->
                    TournamentRegistrationEntry(
                        id = t.id,
                        title = t.eventoNome,
                        dateLabel = formatDateLabel(t.inizio, t.fine),
                        isInProgress = t.lifecycleStatus == 1,
                    )
                }
            }
            .toUiState()
    }

    private fun formatDateLabel(inizioIso: String, fineIso: String): String {
        val inizio = runCatching { java.time.LocalDateTime.parse(inizioIso) }.getOrNull() ?: return inizioIso
        val fine = runCatching { java.time.LocalDateTime.parse(fineIso) }.getOrNull()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.ITALIAN)
        val startLabel = inizio.toLocalDate().format(formatter).replaceFirstChar { it.uppercase() }
        if (fine == null || fine.toLocalDate() == inizio.toLocalDate()) return startLabel
        return "$startLabel – ${fine.toLocalDate().format(formatter)}"
    }
}
