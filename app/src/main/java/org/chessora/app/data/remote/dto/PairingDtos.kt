package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Tournaments.PairingPlayerDto - un giocatore così come
 * compare su una scacchiera (nome/rating/titolo, mai l'IdPlayer: qui basta cosa mostrare,
 * non serve messaggiarlo). */
@Serializable
data class PairingPlayer(
    val id: Int,
    val name: String,
    val rating: Int? = null,
    val title: String? = null,
)

/** Specchio di Chessora.Contracts.Tournaments.PairingDto - una scacchiera di un turno.
 * Black è null per un bye (isBye=true, il giocatore riposa e riceve 1 punto). Result usa
 * gli stessi codici compatti del sito ("1-0","0-1","1/2-1/2","1-0F","0-1F","0-0F", null se
 * non ancora giocata). */
@Serializable
data class Pairing(
    val id: Int,
    val board: Int,
    val white: PairingPlayer? = null,
    val black: PairingPlayer? = null,
    val isBye: Boolean = false,
    val result: String? = null,
)

/** Specchio di Chessora.Contracts.Tournaments.RoundDto (GET /api/tornei/{id}/rounds,
 * pubblico - solo i turni Published, mai i turni Pending ancora in revisione
 * dall'organizzatore). */
@Serializable
data class TournamentRound(
    val id: Int,
    val roundNumber: Int,
    val status: String,
    val pairings: List<Pairing> = emptyList(),
)

/** Specchio di Chessora.Contracts.Tournaments.StandingsRowDto (GET
 * /api/tornei/{id}/standings, pubblico) - Buchholz è l'unico spareggio calcolato in
 * questa prima fase (somma dei punteggi finali degli avversari affrontati). */
@Serializable
data class StandingsRow(
    val rank: Int,
    val entrantId: Int,
    val name: String,
    val rating: Int? = null,
    val score: Double = 0.0,
    val buchholz: Double = 0.0,
)
