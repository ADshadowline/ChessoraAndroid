package org.chessora.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.VideoItem
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Elenco video del circolo (GET api/video-rows, tabella orga.Videos) - a differenza del
 * sito, dove ogni riga ha un proprio titolo preso da site-settings, qui si appiattiscono
 * TUTTE le righe in un'unica lista ordinata per data più recente: le righe distinguono
 * la fonte (circolo/network/FIDE/canale) solo per il sito, non serve replicarlo in app
 * per una semplice consultazione. */
class VideoViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<VideoItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<VideoItem>>> = _state.asStateFlow()

    fun load(club: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getVideoRows(club)
                .map { rows ->
                    // Lo stesso video può comparire in più righe (es. sia nella riga "solo
                    // il circolo" sia in una riga "rete"/canale che lo include a sua volta) -
                    // distinctBy evita sia doppioni visivi sia un crash certo di LazyColumn
                    // (items(key = { it.id })) che non ammette chiavi ripetute.
                    rows.flatMap { it.items }.distinctBy { it.id }.sortedByDescending { it.publishedAt ?: it.createdAt }
                }
                .toUiState()
        }
    }
}
