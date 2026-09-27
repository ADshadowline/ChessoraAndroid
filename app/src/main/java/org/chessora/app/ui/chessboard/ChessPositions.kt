package org.chessora.app.ui.chessboard

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList

/** Ricostruisce, con chesslib (motore/parser di scacchi open source, vedi
 * app/build.gradle.kts), la sequenza di posizioni FEN attraversate da una partita a
 * partire dalle sue mosse SAN (es. il campo "moves" dell'export di Lichess, separato da
 * spazi e senza numeri di turno) - la scacchiera vera e propria (ChessBoardView) resta
 * disegnata a mano, questa libreria serve solo per validare le mosse e sapere dove va
 * ogni pezzo dopo ciascuna. */
object ChessPositions {
    const val STANDARD_START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    /** Sempre almeno un elemento (la posizione di partenza) - se il parsing si ferma a
     * metà (mossa malformata/notazione non standard) restituisce comunque tutte le
     * posizioni valide ricostruite fino a quel punto, mai un errore che nasconderebbe
     * l'intera partita per un singolo problema di parsing. */
    fun fromMoves(movesText: String?): List<String> {
        val board = Board()
        val positions = mutableListOf(board.fen)
        if (movesText.isNullOrBlank()) return positions
        return try {
            val moveList = MoveList()
            moveList.loadFromSan(movesText)
            for (move in moveList) {
                board.doMove(move)
                positions.add(board.fen)
            }
            positions
        } catch (e: Exception) {
            positions
        }
    }
}
