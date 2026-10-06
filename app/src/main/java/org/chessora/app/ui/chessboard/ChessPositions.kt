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
        return fromMoves(cleanMovetext(pgn))
    }

    /** Testo PGN leggibile da mostrare sotto la scacchiera (es. "1. e4 c5 2. c3 Qa5 ..."),
     * a partire dal PGN completo di Chess.com - stessa pulizia di [fromPgn] (intestazioni/
     * commenti orologio/risultato finale via [cleanMovetext]), più la rimozione dei
     * marcatori "N..." del nero: ridondanti per la lettura dato che la sua mossa segue già
     * subito quella del bianco sulla stessa riga. */
    fun formatPgnForDisplay(pgn: String?): String {
        if (pgn.isNullOrBlank()) return ""
        return cleanMovetext(pgn)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && !it.contains("...") }
            .joinToString(" ")
    }

    /** Come [formatPgnForDisplay] ma a partire dalle mosse SAN pure di Lichess (es. il
     * campo "moves" dell'export, senza numeri di turno) - li aggiunge nel formato PGN
     * standard ("1. e4 e5 2. Nf3 Nc6 ..."). */
    fun formatLichessMovesForDisplay(moves: String?): String {
        if (moves.isNullOrBlank()) return ""
        val tokens = moves.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val sb = StringBuilder()
        tokens.forEachIndexed { index, token ->
            if (index % 2 == 0) {
                if (index > 0) sb.append(' ')
                sb.append(index / 2 + 1).append(". ")
            } else {
                sb.append(' ')
            }
            sb.append(token)
        }
        return sb.toString()
    }

    /** Mosse SAN pure (una per ply, senza numeri di turno), ricavate ri-tokenizzando lo
     * stesso testo già prodotto da [formatPgnForDisplay]/[formatLichessMovesForDisplay] -
     * cosi' l'elenco cliccabile mostrato nella UI (vedi PgnMoveList) usa esattamente le
     * stesse mosse del testo PGN leggibile. Troncata a [positionsCount] - 1: se
     * [fromPgn]/[fromMoves] si sono fermate a metà partita per una mossa malformata, la
     * lista cliccabile si ferma allo stesso punto, mai oltre le posizioni realmente
     * disponibili (eviterebbe un click su una mossa senza posizione corrispondente). */
    fun sanMovesForDisplay(formattedPgn: String, positionsCount: Int): List<String> {
        if (formattedPgn.isBlank()) return emptyList()
        val maxMoves = (positionsCount - 1).coerceAtLeast(0)
        return formattedPgn.split(Regex("\\s+"))
            .filter { it.isNotBlank() && !Regex("^\\d+\\.+$").matches(it) }
            .take(maxMoves)
    }

    /** Toglie dal PGN tutto ciò che non serve per ricostruire/mostrare le mosse: righe di
     * intestazione tra parentesi quadre, commenti/orologio tra parentesi graffe, eventuali
     * varianti tra parentesi tonde, e il simbolo di risultato finale. Usato sia per
     * ricostruire le posizioni FEN ([fromPgn]) sia per il testo mostrato sotto la
     * scacchiera ([formatPgnForDisplay]). */
    /** BUG REALE trovato in produzione (01/10/2026, segnalato come crash "PatternSyntaxException"
     * aprendo una partita Chess.com): la graffa di chiusura qui sotto era scritta "}" invece di
     * "\\}" - il motore regex ICU di Android (diverso da quello, più permissivo, della JVM
     * desktop dove giravano i test di questo file) la rifiuta come sintassi non valida. Ogni
     * parentesi letterale in questi pattern va SEMPRE escapata su entrambi i lati. */
    private fun cleanMovetext(pgn: String): String = pgn.lineSequence()
        .filterNot { it.trim().startsWith("[") }
        .joinToString(" ")
        .replace(Regex("\\{[^}]*\\}"), " ")
        .replace(Regex("\\([^)]*\\)"), " ")
        .replace(Regex("(1-0|0-1|1/2-1/2|\\*)\\s*$"), "")
}
