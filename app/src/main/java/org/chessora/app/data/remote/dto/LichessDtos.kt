package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Riferimento a un account Lichess dentro LichessPlayerInfo - assente per un
 * avversario "Stockfish" (vedi LichessPlayerInfo.aiLevel in quel caso). */
@Serializable
data class LichessUserRef(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class LichessPlayerInfo(
    val user: LichessUserRef? = null,
    val rating: Int? = null,
    val ratingDiff: Int? = null,
    /** Valorizzato al posto di [user] quando l'avversario è il motore Stockfish di
     * Lichess (partita "vs computer"), mai insieme a user sulla stessa riga. */
    val aiLevel: Int? = null,
) {
    fun displayName(): String = user?.name ?: aiLevel?.let { "Stockfish (livello $it)" } ?: "?"
}

@Serializable
data class LichessPlayers(
    val white: LichessPlayerInfo = LichessPlayerInfo(),
    val black: LichessPlayerInfo = LichessPlayerInfo(),
)

@Serializable
data class LichessOpening(
    val eco: String? = null,
    val name: String? = null,
)

/** Una partita dell'export di Lichess (NDJSON per l'elenco, GET
 * https://lichess.org/api/games/user/{username}; un solo oggetto per GET
 * https://lichess.org/api/game/export/{id}) - vedi LichessRepository. Solo i campi che
 * questa app usa (l'export ne porta molti altri). */
@Serializable
data class LichessGame(
    val id: String,
    val rated: Boolean = false,
    /** "standard", "chess960", "kingOfTheHill", "atomic", ecc. - la scacchiera
     * incorporata (vedi LichessGameViewerScreen) supporta solo "standard": per le
     * altre varianti si apre la partita su Lichess invece di provare a ricostruire una
     * posizione iniziale non standard. */
    val variant: String = "standard",
    val speed: String = "correspondence",
    val createdAt: Long = 0,
    val status: String = "",
    val players: LichessPlayers = LichessPlayers(),
    /** "white"/"black", assente in caso di patta. */
    val winner: String? = null,
    val opening: LichessOpening? = null,
    /** Mosse SAN separate da spazio, senza numeri di turno (es. "e4 e5 Nf3 Nc6 ...") -
     * usate per ricostruire la scacchiera mossa per mossa (vedi
     * LichessGameViewerViewModel), non il PGN completo. */
    val moves: String? = null,
)
