package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Games.PlayerGameDto (GET /api/games) - una partita
 * registrata manualmente dal socio in "Le mie partite" (PGN incollato o composto a tocchi
 * sulla scacchiera), vedi ui/mygames/. tipologiaTempo usa gli stessi valori di
 * TournamentRegistrationCodes.TipologiaTempo (Standard=0, Rapid=1, Blitz=2,
 * Corrispondenza=3), vedi ui/mygames/Cadenza.kt. */
@Serializable
data class PlayerGame(
    val id: Int,
    val opponentName: String,
    val round: Int? = null,
    val tournamentName: String? = null,
    val tipologiaTempo: Int,
    val pgn: String,
    val createdAt: String,
)

/** Specchio di Chessora.Contracts.Games.SaveGameRequest (POST /api/games). */
@Serializable
data class SaveGameRequest(
    val idPlayer: Int,
    val opponentName: String,
    val round: Int? = null,
    val tournamentName: String? = null,
    val tipologiaTempo: Int,
    val pgn: String,
)
