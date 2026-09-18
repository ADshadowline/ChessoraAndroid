package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Ranking.RankingPlayerDto. idPlayer è null solo per
 * la classifica Assoluta (giocatori FIDE nel mondo) - solo quando è valorizzato si
 * può provare a mostrare la foto profilo (GET /api/players/{idPlayer}/photo). */
@Serializable
data class RankingPlayer(
    val name: String,
    val elo: Int? = null,
    val flag: String? = null,
    val image: String? = null,
    val idPlayer: Int? = null,
)

/**
 * Specchio di Chessora.Contracts.Ranking.RankingResponseDto - forma comune
 * restituita da tutti e tre gli endpoint di classifica (assoluta/nazionale/
 * circolo), un elenco per ciascuna cadenza di gioco.
 */
@Serializable
data class RankingResponse(
    val standard: List<RankingPlayer> = emptyList(),
    val rapid: List<RankingPlayer> = emptyList(),
    val blitz: List<RankingPlayer> = emptyList(),
)
