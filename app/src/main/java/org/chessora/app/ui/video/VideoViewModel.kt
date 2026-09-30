package org.chessora.app.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.VideoFeedItem
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Elenco video del circolo (GET api/video-feed), già filtrato lato server per i canali
 * che questo giocatore ha selezionato in Impostazioni (default: tutti) più gli eventuali
 * video locali del circolo - dal più recente al più vecchio. Ricerca e "cartelle" per
 * canale (vedi VideoScreen) restano un filtro puramente locale sull'elenco già caricato,
 * nessuna nuova chiamata di rete. [club] null = video di tutti i circoli attivi (sezione
 * Video "nel desktop"), non null = solo quelli del circolo (sezione dentro "Il Circolo"). */
class VideoViewModel(private val repository: ChessoraRepository, private val clubPreferences: ClubPreferences) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<VideoFeedItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<VideoFeedItem>>> = _state.asStateFlow()

    fun load(club: String?) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            _state.value = (if (club != null) repository.getVideoFeed(club, idPlayer) else repository.getVideoFeedAllClubs(idPlayer)).toUiState()
        }
    }
}
