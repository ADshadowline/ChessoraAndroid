package org.chessora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.VideoChannelPreference
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Canali YouTube spuntabili per la sezione Video (vedi VideoScreen) - di default sono
 * tutti selezionati (assenza di eccezione salvata, vedi PlayerYouTubeChannelExclusion lato
 * server), togliere la spunta esclude i video di quel canale dall'elenco. Richiede un
 * giocatore identificato (idPlayer): senza, la preferenza non avrebbe a chi essere
 * associata - vedi il gate in SettingsScreen. */
class VideoChannelSettingsViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<VideoChannelPreference>>>(UiState.Loading)
    val state: StateFlow<UiState<List<VideoChannelPreference>>> = _state.asStateFlow()

    private var idPlayer: Int? = null

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            idPlayer = clubPreferences.identifiedPlayerId.first()
            _state.value = repository.getVideoChannelPreferences(idPlayer).toUiState()
        }
    }

    /** Aggiorna subito la UI (spunta), poi salva l'insieme completo dei canali selezionati
     * - mai un diff riga per riga, vedi ChessoraRepository.setVideoChannelPreferences. */
    fun setSelected(channelId: Int, selected: Boolean) {
        val current = (_state.value as? UiState.Success)?.data ?: return
        val updated = current.map { if (it.id == channelId) it.copy(selected = selected) else it }
        _state.value = UiState.Success(updated)
        val player = idPlayer ?: return
        viewModelScope.launch {
            repository.setVideoChannelPreferences(player, updated.filter { it.selected }.map { it.id })
        }
    }
}
