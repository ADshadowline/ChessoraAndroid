package org.chessora.app.ui.performance.online

import kotlin.math.roundToInt

/**
 * Statistiche aggregate per una singola apertura (raggruppata per nome esatto - Lichess e
 * Chess.com non condividono una tassonomia comune, vedi ChessComRepository.openingNameFromEcoUrl,
 * quindi la stessa apertura può comparire due volte se i due siti la chiamano in modo diverso:
 * accettabile, è comunque il nome reale dato da ciascuna fonte, non un'approssimazione). Mostrata
 * nella scheda "Aperture" di OnlineGamesScreen, selezionabile per filtrare l'elenco partite.
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
    this.mapNotNull { game -> game.openingName?.let { it to game } }
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
