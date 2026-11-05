package org.chessora.app.ui.utilities

/**
 * Posizioni iniziali degli scacchi Fischer Random (Chess960), numerate 0..959 secondo lo
 * schema standard di Reinhard Scharnagl (lo stesso usato da FIDE, Lichess e Chess.com):
 * la 518 è la posizione classica RNBQKBNR.
 *
 * Regole rispettate per costruzione: alfieri su case di colore diverso, re fra le due torri,
 * pedoni al loro posto e pezzi neri a specchio di quelli bianchi.
 */
object Chess960 {
    const val COUNT = 960
    const val CLASSIC = 518

    // Coppie di cavalli sulle 5 case libere rimaste (tabella KRN di Scharnagl).
    private val KNIGHTS = listOf(0 to 1, 0 to 2, 0 to 3, 0 to 4, 1 to 2, 1 to 3, 1 to 4, 2 to 3, 2 to 4, 3 to 4)

    /** Prima traversa del Bianco (colonne a..h) in lettere FEN maiuscole, es. "RNBQKBNR". */
    fun backRank(number: Int): String {
        require(number in 0 until COUNT) { "Numero fuori intervallo: $number" }
        val rank = CharArray(8) { ' ' }
        var n = number

        // Alfiere su casa chiara (colonne b, d, f, h) e su casa scura (a, c, e, g).
        rank[n % 4 * 2 + 1] = 'B'; n /= 4
        rank[n % 4 * 2] = 'B'; n /= 4

        // Donna sulla q-esima casa libera.
        val q = n % 6; n /= 6
        rank[emptyFiles(rank)[q]] = 'Q'

        // Cavalli secondo la tabella, sulle 5 case libere.
        val (k1, k2) = KNIGHTS[n]
        val free = emptyFiles(rank)
        rank[free[k1]] = 'N'
        rank[free[k2]] = 'N'

        // Le ultime 3 case: torre, re, torre (il re resta sempre fra le torri).
        val last = emptyFiles(rank)
        rank[last[0]] = 'R'; rank[last[1]] = 'K'; rank[last[2]] = 'R'
        return String(rank)
    }

    /** FEN completo della posizione iniziale, con arrocco in notazione Shredder (colonne
     * delle torri, es. "HAha") come vuole lo standard Chess960. */
    fun fen(number: Int): String {
        val white = backRank(number)
        val rookFiles = white.indices.filter { white[it] == 'R' }.map { 'a' + it }
        val castling = rookFiles.reversed().joinToString("") { it.uppercase() } + rookFiles.reversed().joinToString("")
        return "${white.lowercase()}/pppppppp/8/8/8/8/PPPPPPPP/$white w $castling - 0 1"
    }

    /** Prima traversa con le iniziali italiane dei pezzi (T C A D R), es. "T C A D R A C T". */
    fun italianBackRank(number: Int): String =
        backRank(number).map { ITALIAN[it] ?: it }.joinToString(" ")

    private val ITALIAN = mapOf('R' to 'T', 'N' to 'C', 'B' to 'A', 'Q' to 'D', 'K' to 'R')

    private fun emptyFiles(rank: CharArray) = rank.indices.filter { rank[it] == ' ' }
}
