package org.chessora.app.ui.tournamentmanager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

private const val LIFECYCLE_IN_CORSO = 1

/** Elenco dei tornei InCorso gestibili dall'utente (auto-promosso a organizzatore via la
 * claim self-service, vedi ChessoraRepository.ensureOrganizerAuth) - GET api/tornei/admin
 * restituisce ogni torneo dell'organizzatore, il filtro InCorso è qui: un torneo Creato
 * si gestisce dal sito (iscritti/avvio), uno Concluso non ha più turni da generare. */
class TournamentManagerListViewModel(private val repository: ChessoraRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<TournamentSummary>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TournamentSummary>>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getOrganizedTournaments()
                .mapCatching { tornei -> tornei.filter { it.lifecycleStatus == LIFECYCLE_IN_CORSO } }
                .toUiState()
        }
    }
}
