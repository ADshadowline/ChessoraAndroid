package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Specchio di Chessora.Contracts.Board.BoardMemberDto (GET /api/board). [level]
 * è la posizione nella piramide gerarchica del direttivo (1 = vertice, es.
 * Presidente) - la UI raggruppa per level e mostra i livelli più bassi come
 * "base" della piramide (vedi ui/board/BoardScreen.kt, che replica lo stesso
 * criterio del sito web pubblico).
 */
@Serializable
data class BoardMember(
    val id: Int,
    val year: Int,
    val fullName: String,
    val role: String,
    val photoPath: String? = null,
    val level: Int,
    val sortOrder: Int = 0,
)
