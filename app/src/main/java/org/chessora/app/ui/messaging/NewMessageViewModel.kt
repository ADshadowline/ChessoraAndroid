package org.chessora.app.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.ClubSearchResult
import org.chessora.app.data.remote.dto.PlayerSearchResult
import org.chessora.app.data.repository.ChessoraRepository

enum class NewMessageTab { PLAYERS, CLUBS }

data class NewMessageState(
    val tab: NewMessageTab = NewMessageTab.PLAYERS,
    val query: String = "",
    val loading: Boolean = false,
    val players: List<PlayerSearchResult> = emptyList(),
    val clubs: List<ClubSearchResult> = emptyList(),
)

class NewMessageViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(NewMessageState())
    val state: StateFlow<NewMessageState> = _state.asStateFlow()

    private var identifiedPlayerId: Int? = null
    private var searchJob: Job? = null

    fun init() {
        viewModelScope.launch { identifiedPlayerId = clubPreferences.identifiedPlayerId.first() }
    }

    fun selectTab(tab: NewMessageTab) {
        _state.value = _state.value.copy(tab = tab)
        search(_state.value.query)
    }

    fun onQueryChange(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            search(query)
        }
    }

    private fun search(query: String) {
        if (query.trim().length < 2) {
            _state.value = _state.value.copy(players = emptyList(), clubs = emptyList(), loading = false)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            when (_state.value.tab) {
                NewMessageTab.PLAYERS -> repository.searchMessagingPlayers(query, identifiedPlayerId ?: 0)
                    .onSuccess { _state.value = _state.value.copy(players = it, loading = false) }
                    .onFailure { _state.value = _state.value.copy(loading = false) }
                NewMessageTab.CLUBS -> repository.searchMessagingClubs(query)
                    .onSuccess { _state.value = _state.value.copy(clubs = it, loading = false) }
                    .onFailure { _state.value = _state.value.copy(loading = false) }
            }
        }
    }
}
