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

    private val _actionFailed = MutableStateFlow(false)
    /** True dopo una cancellazione/blocco non riusciti - la schermata mostra un avviso e
     * lo azzera con [clearActionFailed]. */
    val actionFailed: StateFlow<Boolean> = _actionFailed.asStateFlow()

    fun clearActionFailed() { _actionFailed.value = false }

    /** Ricarica l'elenco senza passare da Loading (dopo una cancellazione/blocco). */
    private suspend fun reloadSilently(idPlayer: Int) {
        repository.getConversations(idPlayer).onSuccess { _state.value = MessagingListState.Ready(it) }
    }

    /** Cancella le conversazioni dal solo elenco di questo socio - tolte subito dalla
     * lista, poi riallineate col server. */
    fun hideConversations(idConversations: Set<Int>) {
        val idPlayer = identifiedPlayerId ?: return
        val ids = idConversations.filter { it > 0 }
        if (ids.isEmpty()) return
        (_state.value as? MessagingListState.Ready)?.let { current ->
            _state.value = MessagingListState.Ready(current.conversations.filterNot { it.idConversation in ids })
        }
        viewModelScope.launch {
            repository.hideConversations(idPlayer, ids).onFailure { _actionFailed.value = true }
            reloadSilently(idPlayer)
        }
    }

    fun blockPlayer(otherIdPlayer: Int) = updateBlock { idPlayer -> repository.blockPlayer(idPlayer, otherIdPlayer) }

    fun unblockPlayer(otherIdPlayer: Int) = updateBlock { idPlayer -> repository.unblockPlayer(idPlayer, otherIdPlayer) }

    private fun updateBlock(call: suspend (Int) -> Result<Unit>) {
        val idPlayer = identifiedPlayerId ?: return
        viewModelScope.launch {
            call(idPlayer).onFailure { _actionFailed.value = true }
            reloadSilently(idPlayer)
        }
    }
}
