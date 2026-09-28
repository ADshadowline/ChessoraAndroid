package org.chessora.app.ui.club

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Backs le tre schermate del sotto-menu "Il Circolo" (ClubScreen, ClubContactsScreen,
 * ClubLocationScreen) che leggono dati dal blob site-settings (statuto/contatti) - stesso
 * GET api/site-settings?club= già usato per nome/logo (vedi SessionViewModel), qui letto
 * di nuovo perché queste schermate non fanno parte della sessione condivisa. */
class ClubViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<SiteSettings>>(UiState.Loading)
    val state: StateFlow<UiState<SiteSettings>> = _state.asStateFlow()

    fun load(club: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getSiteSettings(club).toUiState()
        }
    }
}
