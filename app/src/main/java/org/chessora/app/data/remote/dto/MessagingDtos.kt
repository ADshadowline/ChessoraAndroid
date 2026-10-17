package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Messaging.ConversationSummaryDto (GET
 * /api/messaging/conversations). idConversation=0 è il segnaposto riservato alla
 * chat "Chessora" (storico notifiche push, sola lettura, sempre in testa - il
 * server la restituisce sempre come prima voce). otherIdPlayer/blockedByMe/blockedMe
 * valgono solo per le conversazioni dirette (menu Blocca/Sblocca, invio disabilitato). */
@Serializable
data class ConversationSummary(
    val idConversation: Int,
    val isClubConversation: Boolean,
    val displayName: String,
    val photoPath: String? = null,
    val lastMessageBody: String? = null,
    val lastMessageAtUtc: String? = null,
    val unreadCount: Int = 0,
    val otherIdPlayer: Int? = null,
    val blockedByMe: Boolean = false,
    val blockedMe: Boolean = false,
)

@Serializable
data class HideConversationsRequest(val idPlayer: Int, val idConversations: List<Int>)

@Serializable
data class BlockPlayerRequest(val idPlayer: Int, val blockedIdPlayer: Int)

/** blockedByMe = l'ho bloccato io (posso sbloccarlo); blockedMe = mi ha bloccato lui. In
 * entrambi i casi la chat diretta resta visibile ma non si può più scrivere. */
@Serializable
data class BlockStatus(val blockedByMe: Boolean = false, val blockedMe: Boolean = false)

/** isDelivered/isRead (doppia spunta) sono significativi solo quando isMine è true -
 * vedi Chessora.Contracts.Messaging.MessageDto. */
@Serializable
data class ChatMessage(
    val id: Int,
    val senderName: String,
    val isMine: Boolean,
    val body: String,
    val sentAtUtc: String,
    val isDelivered: Boolean = false,
    val isRead: Boolean = false,
)

@Serializable
data class PlayerSearchResult(
    val idPlayer: Int,
    val fullName: String,
    val photoPath: String? = null,
    val clubName: String = "",
)

@Serializable
data class ClubSearchResult(
    val idClub: Int,
    val name: String,
    val photoPath: String? = null,
    val hasReferente: Boolean = false,
)

@Serializable
data class StartDirectConversationRequest(val fromIdPlayer: Int, val toIdPlayer: Int, val body: String)

@Serializable
data class StartClubConversationRequest(val fromIdPlayer: Int, val idClub: Int, val body: String)

@Serializable
data class SendMessageRequest(val fromIdPlayer: Int, val body: String)

@Serializable
data class MarkReadRequest(val idPlayer: Int)

@Serializable
data class StartConversationResult(val idConversation: Int? = null)

/** Specchio di Chessora.Contracts.Messaging.MessagingPlayerSettingsDto (GET
 * /api/messaging/settings) - privacy delle conferme di consegna/lettura. */
@Serializable
data class MessagingPlayerSettings(val hideDeliveryAndReadStatus: Boolean = false)

@Serializable
data class SetMessagingPlayerSettingsRequest(val idPlayer: Int, val hideDeliveryAndReadStatus: Boolean)
