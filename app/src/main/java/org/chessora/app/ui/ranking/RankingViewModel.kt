package org.chessora.app.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

enum class RankingScope { CIRCOLO, NAZIONALE, ASSOLUTA }

/**
 * Le tre classifiche (docs/android-app-spec.md §7.6) condividono la stessa
 * forma di risposta (RankingResponse: standard/rapid/blitz) ma tre endpoint
 * diversi - un solo stato in memoria per lo [scope] correntemente selezionato,
 * ricaricato ogni volta che cambia tab (vedi RankingScreen.kt).
 */
class RankingViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<RankingResponse>>(UiState.Loading)
    val state: StateFlow<UiState<RankingResponse>> = _state.asStateFlow()

    private var loadedFor: Pair<RankingScope, Int?>? = null

    fun load(scope: RankingScope, idClub: Int?) {
        val key = scope to idClub
        if (loadedFor == key && _state.value is UiState.Success) return
        loadedFor = key

        viewModelScope.launch {
            _state.value = UiState.Loading
            val result = when (scope) {
                RankingScope.CIRCOLO -> idClub?.let { repository.getRankingCircolo(it) }
                    ?: Result.failure(IllegalStateException("Nessun circolo selezionato"))
                RankingScope.NAZIONALE -> repository.getRankingNazionale()
                RankingScope.ASSOLUTA -> repository.getRankingAssoluta()
            }
            _state.value = result.toUiState()
        }
    }
}
