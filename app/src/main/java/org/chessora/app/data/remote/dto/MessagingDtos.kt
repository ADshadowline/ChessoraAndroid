package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Messaging.ConversationSummaryDto (GET
 * /api/messaging/conversations). idConversation=0 è il segnaposto riservato alla
 * chat "Chessora" (storico notifiche push, sola lettura, sempre in testa - il
 * server la restituisce sempre come prima voce). */
@Serializable
data class ConversationSummary(
    val idConversation: Int,
    val isClubConversation: Boolean,
    val displayName: String,
    val photoPath: String? = null,
    val lastMessageBody: String? = null,
    val lastMessageAtUtc: String? = null,
    val unreadCount: Int = 0,
)

@Serializable
data class ChatMessage(
    val id: Int,
    val senderName: String,
    val isMine: Boolean,
    val body: String,
    val sentAtUtc: String,
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
