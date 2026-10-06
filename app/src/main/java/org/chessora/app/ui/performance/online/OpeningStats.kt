package org.chessora.app.ui.performance.online

import kotlin.math.roundToInt

/** Nome di famiglia di un'apertura, senza il dettaglio della variante/sequenza di mosse - il
 * nome completo dato da Lichess ("Sicilian Defense: Alapin Variation, 2...Nf6") o dall'URL di
 * Chess.com ("Alapin-Sicilian-Defense-2...Qa5", vedi ChessComRepository.openingNameFromEcoUrl)
 * è troppo specifico per una statistica utile (quasi una riga per partita): qui si tiene solo
 * la parte prima dei due punti/virgola (Lichess) e si toglie la coda di notazione mosse
 * (qualunque parola con una cifra, es. "2...Nf6"/"3.Nf3") da entrambe le fonti. Lichess e
 * Chess.com restano comunque tassonomie indipendenti (nessuna sinonimia tra le due, es. "Sicilian
 * Defense" vs "Alapin Sicilian Defense" non si fondono), ma la stessa fonte ora raggruppa bene
 * le proprie varianti sotto la stessa famiglia. */
fun openingFamilyName(fullName: String): String {
    val base = fullName.substringBefore(':').substringBefore(',').trim()
    val words = base.split(' ').filter { it.isNotBlank() }
    val cut = words.indexOfFirst { word -> word.any(Char::isDigit) }
    val family = (if (cut >= 0) words.take(cut) else words).joinToString(" ").trim()
    return family.ifBlank { fullName.trim() }
}

/** Famiglia dell'apertura giocata in questa partita (vedi [openingFamilyName]), null se la
 * fonte non ha valorizzato nessun nome per questa partita. */
val OnlineGameSummary.openingFamily: String? get() = openingName?.let(::openingFamilyName)

/**
 * Statistiche aggregate per una famiglia di aperture (vedi [openingFamily]). Mostrata nella
 * scheda "Aperture" di OnlineGamesScreen, selezionabile per filtrare l'elenco partite.
 */
data class OpeningStats(
    val name: String,
    val totalGames: Int,
    val lichessCount: Int,
    val chessComCount: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
) {
    val winPercent: Int get() = percentOf(wins)
    val drawPercent: Int get() = percentOf(draws)
    val lossPercent: Int get() = percentOf(losses)

    private fun percentOf(count: Int): Int = if (totalGames == 0) 0 else (count * 100f / totalGames).roundToInt()
}

/** Partite senza apertura nota (fonte che non l'ha valorizzata) escluse dal conteggio: non
 * avrebbe senso una riga "apertura sconosciuta" tra le aperture vere e proprie. Ordinate dalla
 * più giocata alla meno giocata. */
fun List<OnlineGameSummary>.computeOpeningStats(): List<OpeningStats> =
    this.mapNotNull { game -> game.openingFamily?.let { it to game } }
        .groupBy({ it.first }, { it.second })
        .map { (name, games) ->
            OpeningStats(
                name = name,
                totalGames = games.size,
                lichessCount = games.count { it.source == OnlineGameSource.LICHESS },
                chessComCount = games.count { it.source == OnlineGameSource.CHESSCOM },
                wins = games.count { it.result == OnlineGameResult.WIN },
                draws = games.count { it.result == OnlineGameResult.DRAW },
                losses = games.count { it.result == OnlineGameResult.LOSS },
            )
        }
        .sortedByDescending { it.totalGames }
