package org.chessora.app.ui.tornei

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.Torneo
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState

/**
 * Come NewsDetailViewModel.kt: l'Api non ha un endpoint "singolo torneo per
 * id", quindi filtriamo lato client sull'elenco completo di GET /api/tornei.
 * Indipendente da TorneiListViewModel (ogni destinazione di navigazione ha il
 * proprio ViewModel per default in Navigation Compose) - la duplicazione della
 * chiamata di rete è un compromesso accettato per non introdurre uno store
 * condiviso solo per questo.
 */
class TorneoDetailViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Torneo>>(UiState.Loading)
    val state: StateFlow<UiState<Torneo>> = _state.asStateFlow()

    fun load(idClub: Int, idTorneo: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val torneo = repository.getTornei(idClub).getOrNull()?.firstOrNull { it.id == idTorneo }
            _state.value = if (torneo != null) UiState.Success(torneo) else UiState.Error("Torneo non trovato.")
        }
    }
}
