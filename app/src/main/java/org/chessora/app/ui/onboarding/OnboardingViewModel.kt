package org.chessora.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/**
 * Schermata di primo avvio (docs/android-app-spec.md §3): niente login, si
 * sceglie un circolo dall'elenco nazionale (ricerca client-side per nome/città
 * su GET /api/clubs/directory) oppure inserendo un codice risolto da GET
 * /api/clubs/resolve. La selezione effettiva (salvataggio + registrazione
 * push) è delegata a SessionViewModel.selectClub - questo ViewModel si occupa
 * solo di caricare/filtrare l'elenco e risolvere un eventuale codice.
 */
class OnboardingViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ClubDirectoryItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ClubDirectoryItem>>> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _codeError = MutableStateFlow<String?>(null)
    val codeError: StateFlow<String?> = _codeError.asStateFlow()

    private var allClubs: List<ClubDirectoryItem> = emptyList()

    init {
        loadDirectory()
    }

    fun loadDirectory() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val result = repository.getClubDirectory()
            result.onSuccess { allClubs = it }
            _state.value = result.map { filterClubs(it, _searchQuery.value) }.toUiState()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _state.value = UiState.Success(filterClubs(allClubs, query))
    }

    private fun filterClubs(clubs: List<ClubDirectoryItem>, query: String): List<ClubDirectoryItem> {
        if (query.isBlank()) return clubs
        val normalized = query.trim().lowercase()
        return clubs.filter {
            it.name.lowercase().contains(normalized) || (it.cityName?.lowercase()?.contains(normalized) == true)
        }
    }

    /**
     * Valida un codice circolo inserito a mano tramite GET /api/clubs/resolve
     * (404 -> non valido) e, se valido, invoca [onResolved] col codice stesso
     * (trim), non con l'idClub restituito dalla risposta: da qui in poi la
     * navigazione/le chiamate usano sempre il publicCode, mai il numero interno
     * - vedi ChessoraNavHost.kt e SessionViewModel.selectClub.
     */
    fun resolveCode(code: String, onResolved: (String) -> Unit) {
        if (code.isBlank()) return
        val trimmed = code.trim()
        viewModelScope.launch {
            _codeError.value = null
            repository.resolveClubByCode(trimmed)
                .onSuccess { onResolved(trimmed) }
                .onFailure { _codeError.value = "Codice circolo non valido." }
        }
    }
}
