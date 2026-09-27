package org.chessora.app.ui.performance.lichess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.LichessGame
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.data.repository.LichessRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** [username] null = il socio non ha ancora configurato il proprio username Lichess in
 * Impostazioni (vedi LichessGamesScreen: messaggio diverso da "configurato ma zero
 * partite"). */
data class LichessGamesData(val username: String?, val games: List<LichessGame>)

private const val GAMES_LIMIT = 50

class LichessGamesViewModel(
    private val repository: ChessoraRepository,
    private val lichessRepository: LichessRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<LichessGamesData>>(UiState.Loading)
    val state: StateFlow<UiState<LichessGamesData>> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            val username = idPlayer?.let { repository.getLichessSettings(it).getOrNull()?.lichessUsername }
            _state.value = if (username.isNullOrBlank()) {
                UiState.Success(LichessGamesData(null, emptyList()))
            } else {
                lichessRepository.fetchGames(username, GAMES_LIMIT)
                    .map { games -> LichessGamesData(username, games) }
                    .toUiState()
            }
        }
    }
}
