package org.chessora.app.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.ChatMessage
import org.chessora.app.data.repository.ChessoraRepository

sealed interface ConversationState {
    data object Loading : ConversationState
    data class Ready(val messages: List<ChatMessage>, val isReadOnly: Boolean, val sending: Boolean = false) : ConversationState
    data class Error(val message: String) : ConversationState
}

/** [idConversationArg] = 0 -> chat "Chessora" (sola lettura); &lt; 0 -> conversazione
 * non ancora creata (primo messaggio, vedi NewMessageScreen: [recipientId] è
 * l'idPlayer del destinatario, o l'idClub se [isClubConversation]); altrimenti una
 * conversazione reale già esistente. */
class ConversationViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
    private val idConversationArg: Int,
    private val isClubConversation: Boolean,
    private val recipientId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow<ConversationState>(ConversationState.Loading)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private var idConversation = idConversationArg
    private var idPlayer: Int? = null

    fun load() {
        viewModelScope.launch {
            _state.value = ConversationState.Loading
            idPlayer = clubPreferences.identifiedPlayerId.first()

            when {
                idConversationArg == 0 -> repository.getChessoraThread(idPlayer ?: 0)
                    .onSuccess { _state.value = ConversationState.Ready(it, isReadOnly = true) }
                    .onFailure { _state.value = ConversationState.Error("Connessione non riuscita, riprova.") }
                idConversationArg < 0 -> _state.value = ConversationState.Ready(emptyList(), isReadOnly = false)
                else -> repository.getConversationMessages(idConversationArg, idPlayer ?: 0)
                    .onSuccess { _state.value = ConversationState.Ready(it, isReadOnly = false) }
                    .onFailure { _state.value = ConversationState.Error("Connessione non riuscita, riprova.") }
            }
        }
    }

    fun send(body: String) {
        val trimmed = body.trim()
        val myIdPlayer = idPlayer
        val current = _state.value as? ConversationState.Ready ?: return
        if (trimmed.isEmpty() || myIdPlayer == null || current.sending) return

        viewModelScope.launch {
            _state.value = current.copy(sending = true)

            if (idConversation < 0) {
                val startResult = if (isClubConversation) {
                    repository.startClubConversation(myIdPlayer, recipientId, trimmed)
                } else {
                    repository.startDirectConversation(myIdPlayer, recipientId, trimmed)
                }
                startResult
                    .onSuccess { newId ->
                        if (newId == null) {
                            _state.value = ConversationState.Error("Destinatario non valido.")
                            return@onSuccess
                        }
                        idConversation = newId
                        // Ricarica dal server invece di ricostruire localmente il
                        // primo messaggio, cosi' il nome mittente/formattazione
                        // restano coerenti con quanto risolto lato server.
                        repository.getConversationMessages(newId, myIdPlayer)
                            .onSuccess { _state.value = ConversationState.Ready(it, isReadOnly = false) }
                            .onFailure { _state.value = ConversationState.Ready(current.messages, isReadOnly = false) }
                    }
                    .onFailure { _state.value = ConversationState.Error("Invio non riuscito, riprova.") }
            } else {
                repository.sendMessage(idConversation, myIdPlayer, trimmed)
                    .onSuccess { message -> _state.value = ConversationState.Ready(current.messages + message, isReadOnly = false) }
                    .onFailure { _state.value = current.copy(sending = false) }
            }
        }
    }
}
