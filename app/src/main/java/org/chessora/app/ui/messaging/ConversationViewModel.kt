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
    /** blockedByMe/blockedMe: solo per le dirette (vedi BlockStatus) - con uno dei due la
     * chat resta leggibile ma il campo di invio è disabilitato. */
    data class Ready(
        val messages: List<ChatMessage>,
        val isReadOnly: Boolean,
        val sending: Boolean = false,
        val blockedByMe: Boolean = false,
        val blockedMe: Boolean = false,
    ) : ConversationState {
        val isBlocked: Boolean get() = blockedByMe || blockedMe
    }
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
    /** L'altro socio di una diretta: da [recipientId] se noto, altrimenti (apertura da una
     * notifica push, recipientId = 0) ricavato dall'elenco conversazioni in [refreshBlockStatus]. */
    private var otherIdPlayer: Int? = recipientId.takeIf { it > 0 && !isClubConversation }

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
            refreshBlockStatus()
        }
    }

    /** Aggiorna blockedByMe/blockedMe nello stato Ready (nessun effetto sulla chat
     * "Chessora" né sulle conversazioni con un circolo, che non si bloccano). */
    private suspend fun refreshBlockStatus() {
        val myIdPlayer = idPlayer ?: return
        if (idConversationArg == 0 || isClubConversation) return

        if (otherIdPlayer == null && idConversation > 0) {
            val summary = repository.getConversations(myIdPlayer).getOrNull()
                ?.firstOrNull { it.idConversation == idConversation }
            if (summary == null || summary.isClubConversation) return
            otherIdPlayer = summary.otherIdPlayer
        }
        val other = otherIdPlayer ?: return
        repository.getBlockStatus(myIdPlayer, other).onSuccess { status ->
            val latest = _state.value as? ConversationState.Ready ?: return@onSuccess
            _state.value = latest.copy(blockedByMe = status.blockedByMe, blockedMe = status.blockedMe)
        }
    }

    /** Sblocca l'altro socio direttamente dalla chat (avviso "Hai bloccato questo giocatore"). */
    fun unblock() {
        val myIdPlayer = idPlayer ?: return
        val other = otherIdPlayer ?: return
        viewModelScope.launch {
            repository.unblockPlayer(myIdPlayer, other).onSuccess { refreshBlockStatus() }
        }
    }

    /** Ricarica i messaggi SENZA passare da ConversationState.Loading (niente spinner/
     * scroll-jump) - solo per aggiornare in background lo stato consegnato/letto dei
     * MIEI messaggi mentre si resta nella schermata (vedi ConversationScreen, poll
     * periodico). Nessun effetto sulla chat "Chessora" (sempre isReadOnly, mai un mio
     * messaggio quindi nessuna spunta da aggiornare) né su una conversazione non ancora
     * creata (idConversation < 0, niente ancora da rileggere). */
    fun refreshSilently() {
        val current = _state.value as? ConversationState.Ready ?: return
        if (current.isReadOnly || idConversation < 0) return
        val myIdPlayer = idPlayer ?: return
        viewModelScope.launch {
            repository.getConversationMessages(idConversation, myIdPlayer).onSuccess { messages ->
                val latest = _state.value as? ConversationState.Ready ?: return@onSuccess
                _state.value = latest.copy(messages = messages)
            }
            // Anche il blocco può cambiare mentre si è nella chat (l'altro mi blocca o mi sblocca).
            refreshBlockStatus()
        }
    }

    fun send(body: String) {
        val trimmed = body.trim()
        val myIdPlayer = idPlayer
        val current = _state.value as? ConversationState.Ready ?: return
        if (trimmed.isEmpty() || myIdPlayer == null || current.sending || current.isBlocked) return

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
                            .onSuccess { _state.value = current.copy(messages = it, sending = false) }
                            .onFailure { _state.value = current.copy(sending = false) }
                    }
                    .onFailure {
                        // Rifiutato perché nel frattempo uno dei due ha bloccato l'altro:
                        // l'avviso nel campo di invio spiega già il perché.
                        _state.value = current.copy(sending = false)
                        refreshBlockStatus()
                        if ((_state.value as? ConversationState.Ready)?.isBlocked != true)
                            _state.value = ConversationState.Error("Invio non riuscito, riprova.")
                    }
            } else {
                repository.sendMessage(idConversation, myIdPlayer, trimmed)
                    .onSuccess { message -> _state.value = current.copy(messages = current.messages + message, sending = false) }
                    .onFailure {
                        _state.value = current.copy(sending = false)
                        refreshBlockStatus()
                    }
            }
        }
    }
}
