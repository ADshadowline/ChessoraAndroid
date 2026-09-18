package org.chessora.app.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.ConversationSummary
import org.chessora.app.data.repository.ChessoraRepository

sealed interface MessagingListState {
    data object Loading : MessagingListState
    data object NotIdentified : MessagingListState
    data class Ready(val conversations: List<ConversationSummary>) : MessagingListState
    data class Error(val message: String) : MessagingListState
}

class MessagingListViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<MessagingListState>(MessagingListState.Loading)
    val state: StateFlow<MessagingListState> = _state.asStateFlow()

    var identifiedPlayerId: Int? = null
        private set

    fun load() {
        viewModelScope.launch {
            _state.value = MessagingListState.Loading
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            identifiedPlayerId = idPlayer
            if (idPlayer == null) {
                _state.value = MessagingListState.NotIdentified
                return@launch
            }
            repository.getConversations(idPlayer)
                .onSuccess { _state.value = MessagingListState.Ready(it) }
                .onFailure { _state.value = MessagingListState.Error("Connessione non riuscita, riprova.") }
        }
    }
}
