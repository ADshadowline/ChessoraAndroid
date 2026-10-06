package org.chessora.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Elenco dei mesi con partite di un utente Chess.com (GET .../games/archives) - ogni
 * stringa è l'URL completo del mese, es. "https://api.chess.com/pub/player/x/games/2024/03",
 * dal più vecchio al più recente. Chess.com non ha un endpoint "ultime N partite": si
 * paginano i mesi a ritroso, vedi ChessComRepository. */
@Serializable
data class ChessComArchivesResponse(
    val archives: List<String> = emptyList(),
)

@Serializable
data class ChessComGamesResponse(
    val games: List<ChessComGameRaw> = emptyList(),
)

@Serializable
data class ChessComPlayerRaw(
    val username: String? = null,
    val rating: Int? = null,
    /** "win", oppure il motivo della non-vittoria ("checkmated", "resigned", "agreed",
     * "repetition", "timeout", "stalemate", "insufficient", "50move", ecc.) - mai un
     * semplice "loss"/"draw", va derivato confrontando le due righe (vedi ChessComRepository). */
    val result: String? = null,
)

/** Una partita così come la restituisce l'API pubblica di Chess.com (un mese di
 * .../games/archives) - solo i campi che questa app usa. Il PGN è già incluso per intero
 * (a differenza di Lichess non esiste un endpoint per rileggere una singola partita per id,
 * quindi il PGN va portato con sé finché la partita resta aperta, vedi ChessComGameCache). */
@Serializable
data class ChessComGameRaw(
    val white: ChessComPlayerRaw? = null,
    val black: ChessComPlayerRaw? = null,
    val pgn: String? = null,
    val url: String? = null,
    val rated: Boolean = false,
    /** "chess", "chess960", "bughouse", "kingofthehill", "threecheck", "crazyhouse". */
    val rules: String = "chess",
    /** "bullet"/"blitz"/"rapid"/"daily". */
    @SerialName("time_class") val timeClass: String = "?",
    /** Epoch secondi (non millisecondi, a differenza di LichessGame.createdAt). */
    @SerialName("end_time") val endTime: Long = 0,
    /** Identificativo stabile della partita, sempre presente - usato come id invece di
     * ricavarlo dall'ultimo segmento di [url] (che varia forma tra partite "live" e
     * "daily" e potrebbe non essere un numero pulito, vedi ChessComRepository.mapGame). */
    val uuid: String? = null,
    /** URL alla pagina dell'apertura sul sito, es.
     * "https://www.chess.com/openings/Alapin-Sicilian-Defense-2...Qa5" - a differenza di
     * Lichess (vedi LichessOpening) non c'è un campo separato con nome/codice ECO puliti,
     * il nome va ricavato dall'ultimo segmento dell'URL (vedi
     * ChessComRepository.openingNameFromEcoUrl). Assente per alcune partite "daily"/vecchie. */
    val eco: String? = null,
)
