package org.chessora.app.data.repository

import kotlinx.serialization.json.Json
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.ChessComArchivesResponse
import org.chessora.app.data.remote.dto.ChessComGameRaw
import org.chessora.app.data.remote.dto.ChessComGamesResponse

enum class ChessComResult { WIN, LOSS, DRAW }

/** Partita Chess.com già risolta dal punto di vista dello username richiesto (chi è
 * l'avversario, ho vinto/perso/pareggiato) - stessa logica del riferimento TypeScript in
 * C:\Projects\lichess\src\site\chesscom.ts (ChessComService/mapGame), riadattata qui in
 * Kotlin. A differenza di Lichess, l'API pubblica di Chess.com non ha un endpoint per
 * rileggere una singola partita per id: il PGN completo resta quindi imbustato in questo
 * oggetto finché la partita è aperta (vedi ChessComGameCache), non ricaricato dal
 * visualizzatore. */
data class ChessComGame(
    val id: String,
    val createdAt: Long, // epoch millis, per coerenza con LichessGame.createdAt (Chess.com dà epoch secondi)
    val speed: String,
    /** "chess", "chess960", "bughouse", "kingofthehill", "threecheck", "crazyhouse" - solo
     * "chess" (standard) è supportato dalla scacchiera incorporata, vedi ChessComGameViewerScreen. */
    val rules: String,
    val rated: Boolean,
    val whiteName: String,
    val blackName: String,
    val whiteRating: Int?,
    val blackRating: Int?,
    /** "white"/"black" - il colore giocato dallo username richiesto. */
    val color: String,
    val opponentName: String,
    val myRating: Int?,
    val result: ChessComResult,
    val url: String,
    val pgn: String,
)

/** I codici risultato che Chess.com usa per indicare una patta (entrambi i lati portano lo
 * stesso codice) - qualunque altro codice non-"win" (checkmated, resigned, timeout,
 * abandoned, lose, e quelli specifici di varianti) è una sconfitta. */
private val DRAW_CODES = setOf("agreed", "repetition", "stalemate", "insufficient", "50move", "timevsinsufficient")

private fun idFromUrl(url: String): String = url.substringAfterLast('/').ifBlank { url }

private fun mapGame(raw: ChessComGameRaw, username: String): ChessComGame? {
    val white = raw.white ?: return null
    val black = raw.black ?: return null
    val whiteName = white.username ?: return null
    val blackName = black.username ?: return null
    val whiteIsMe = whiteName.equals(username, ignoreCase = true)
    val me = if (whiteIsMe) white else black
    val opponent = if (whiteIsMe) black else white
    val myResult = me.result ?: ""
    val result = when {
        myResult == "win" -> ChessComResult.WIN
        DRAW_CODES.contains(myResult) -> ChessComResult.DRAW
        else -> ChessComResult.LOSS
    }
    val url = raw.url ?: ""
    return ChessComGame(
        id = raw.uuid ?: idFromUrl(url),
        createdAt = raw.endTime * 1000,
        speed = raw.timeClass,
        rules = raw.rules,
        rated = raw.rated,
        whiteName = whiteName,
        blackName = blackName,
        whiteRating = white.rating,
        blackRating = black.rating,
        color = if (whiteIsMe) "white" else "black",
        opponentName = opponent.username ?: "?",
        myRating = me.rating,
        result = result,
        url = url,
        pgn = raw.pgn ?: "",
    )
}

/** Cache in-memory (mai persistita, svuotata al riavvio del processo) delle ultime partite
 * Chess.com caricate in "Scacchi Online" - serve solo a portare il PGN completo dall'elenco
 * al visualizzatore (vedi OnlineGamesViewModel/ChessComGameViewerViewModel): a differenza di
 * Lichess, l'API pubblica di Chess.com non offre un endpoint per rileggere una singola
 * partita per id, l'unico modo per riaverla è tenerla da parte. */
object ChessComGameCache {
    private val games = mutableMapOf<String, ChessComGame>()

    fun putAll(list: List<ChessComGame>) {
        list.forEach { games[it.id] = it }
    }

    fun get(id: String): ChessComGame? = games[id]
}

/** Wrapper sull'API pubblica "Published-Data" di Chess.com (nessuna autenticazione: le
 * partite di un utente sono pubbliche, vedi ui/settings/ per la sola configurazione dello
 * username) - stessa logica del riferimento TypeScript in
 * C:\Projects\lichess\src\site\chesscom.ts (ChessComService), da cui questa classe riprende
 * la stessa risoluzione/paginazione. Separata da ChessoraRepository perché parla con un
 * host completamente diverso (api.chess.com, non api.chessora.org), vedi
 * NetworkModule.fetchChessComArchivesJson/fetchChessComMonthJson. */
class ChessComRepository {
    private val json = Json { ignoreUnknownKeys = true }

    /** Le ultime [max] partite pubbliche di [username], più recenti prima - Chess.com
     * pagina solo per mese (mai per numero di partite come Lichess): si scorrono gli
     * archivi dal più recente finché non si raggiunge [max] partite o si esauriscono i mesi. */
    suspend fun fetchGames(username: String, max: Int = 50): Result<List<ChessComGame>> = runCatching {
        val trimmed = username.trim()
        val archivesBody = NetworkModule.fetchChessComArchivesJson(trimmed)
        val archives = json.decodeFromString(ChessComArchivesResponse.serializer(), archivesBody).archives
        val out = mutableListOf<ChessComGame>()
        var monthIndex = archives.lastIndex
        while (monthIndex >= 0 && out.size < max) {
            val monthBody = NetworkModule.fetchChessComMonthJson(archives[monthIndex])
            val games = json.decodeFromString(ChessComGamesResponse.serializer(), monthBody).games
            // Il mese arriva in ordine ascendente (la più vecchia per prima) - qui serve dal
            // più recente al più vecchio, quindi si scorre a ritroso.
            for (i in games.indices.reversed()) {
                if (out.size >= max) break
                mapGame(games[i], trimmed)?.let { out.add(it) }
            }
            monthIndex--
        }
        out
    }
}
