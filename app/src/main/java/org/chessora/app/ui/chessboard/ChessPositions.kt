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

    /** Come [fromMoves] ma a partire da un PGN COMPLETO (intestazioni + mosse numerate +
     * eventuali commenti/orologio tra parentesi graffe + simbolo di risultato finale - es. il
     * campo "pgn" dell'export di Chess.com, che include sempre l'orologio tra parentesi
     * graffe per ogni mossa). chesslib gestisce già da sé i numeri di turno ("1.", "12...")
     * token per token, ma non le intestazioni tra parentesi quadre né i commenti tra
     * parentesi graffe: un solo token malformato manda in errore l'INTERO parsing (nessuna
     * mossa buona verrebbe applicata, a differenza di un errore a metà partita), quindi
     * vanno ripuliti qui PRIMA di passare a [fromMoves]. */
    fun fromPgn(pgn: String?): List<String> {
        if (pgn.isNullOrBlank()) return fromMoves(null)
        val movetext = pgn.lineSequence()
            .filterNot { it.trim().startsWith("[") }
            .joinToString(" ")
            .replace(Regex("\\{[^}]*}"), " ")
            .replace(Regex("\\([^)]*\\)"), " ")
            .replace(Regex("(1-0|0-1|1/2-1/2|\\*)\\s*$"), "")
        return fromMoves(movetext)
    }
}
