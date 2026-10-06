package org.chessora.app.ui.performance.online

/** Da quale sito arriva una riga di "Scacchi Online" - determina sia l'icona/badge
 * mostrata accanto alla partita, sia quale visualizzatore aprire al tocco (vedi
 * OnlineGamesScreen/ChessoraNavHost). */
enum class OnlineGameSource { LICHESS, CHESSCOM }

enum class OnlineGameResult { WIN, LOSS, DRAW }

/** Riga unificata per l'elenco "Scacchi Online" (partite Lichess e Chess.com nello stesso
 * elenco, ordinate per data) - Lichess e Chess.com hanno DTO/domini diversi (LichessGame in
 * data/remote/dto/, ChessComGame in data/repository/ChessComRepository.kt), questo è il
 * sottoinsieme comune già risolto dal punto di vista del socio, sufficiente per disegnare la
 * riga e per sapere quale visualizzatore aprire al tocco. */
data class OnlineGameSummary(
    val source: OnlineGameSource,
    val id: String,
    val opponentName: String,
    val myRating: Int?,
    val result: OnlineGameResult,
    val speedLabel: String,
    val createdAt: Long,
    /** Nome dell'apertura giocata, null se la fonte non l'ha valorizzata per questa partita
     * (vedi LichessOpening.name/ChessComGame.openingName) - usato per le statistiche aperture
     * in OnlineGamesScreen (scheda "Aperture") e per il filtro per apertura. */
    val openingName: String?,
)
