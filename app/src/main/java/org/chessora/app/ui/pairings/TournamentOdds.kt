package org.chessora.app.ui.pairings

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import org.chessora.app.data.remote.dto.TournamentRound

/** Probabilità di vittoria/patta/sconfitta di una partita, dal lato del Bianco. */
data class GameOdds(val whiteWin: Double, val draw: Double, val blackWin: Double)

/** Esito delle simulazioni per un giocatore: probabilità di vincere il torneo e di finire
 * sul podio. */
data class TournamentChance(val win: Double, val podium: Double)

/**
 * Stesso modello di tourn.chessora.org (abbinamenti-risultati.html "Andamento giocatore" e
 * torneo-dettaglio.html "probabilità di vittoria"): punteggio atteso FIDE
 * 1/(1+10^(-diff/400)), patta tanto più probabile quanto più la partita è equilibrata con un
 * coefficiente per tipo di tempo, corretto dalle patte già viste nel torneo. Indicativo: non
 * conosce chi si è già incontrato né gli spareggi veri.
 */
object TournamentOdds {
    const val SIMULATIONS = 4000
    private const val DEFAULT_RATING = 1399
    private val DRAW_BASE_BY_TEMPO = mapOf(0 to 0.34, 1 to 0.24, 2 to 0.18, 3 to 0.45)

    private fun expectedScore(own: Int, opp: Int): Double = 1.0 / (1.0 + 10.0.pow((opp - own) / 400.0))
    private fun balance(e: Double): Double = 1.0 - abs(2.0 * e - 1.0) // 1 a parità, 0 a disparità totale

    /** Coefficiente di patta k (P(patta) = k * balance(E)): prior per tipo di tempo e
     * livello medio, aggiornato con le partite già giocate (media pesata, prior = 10 partite). */
    fun drawCoefficient(tipologiaTempo: Int, rounds: List<TournamentRound>): Double {
        val base = DRAW_BASE_BY_TEMPO[tipologiaTempo] ?: 0.28
        val elos = mutableListOf<Int>()
        var draws = 0
        var weight = 0.0
        rounds.forEach { r ->
            r.pairings.forEach { p ->
                val w = p.white ?: return@forEach
                val b = p.black ?: return@forEach
                if (p.isBye) return@forEach
                val we = w.rating ?: DEFAULT_RATING
                val be = b.rating ?: DEFAULT_RATING
                elos += we
                elos += be
                if (p.result == "1-0" || p.result == "0-1" || p.result == "1/2-1/2") {
                    weight += balance(expectedScore(we, be))
                    if (p.result == "1/2-1/2") draws++
                }
            }
        }
        val avg = if (elos.isEmpty()) 1500.0 else elos.average()
        val prior = base * min(1.3, max(0.6, 0.6 + (avg - 1400) / 1500))
        val priorGames = 10
        return min(0.9, (prior * priorGames + draws) / (priorGames + weight))
    }

    fun gameOdds(white: Int?, black: Int?, k: Double): GameOdds {
        val e = expectedScore(white ?: DEFAULT_RATING, black ?: DEFAULT_RATING)
        val d = k * balance(e)
        return GameOdds(whiteWin = max(0.0, e - d / 2), draw = d, blackWin = max(0.0, 1 - e - d / 2))
    }

    /** Monte Carlo sui [roundsLeft] turni che mancano, partendo dai punti attuali: svizzero
     * semplificato (gruppi di punteggio, metà alta contro metà bassa per Elo, chi avanza
     * scende, bye all'ultimo). A pari punti finali la vittoria si divide in parti uguali.
     * Giocatori con stesso Elo e stessi punti ricevono la media del loro gruppo, così due
     * pari Elo hanno esattamente la stessa probabilità invece del solo rumore della simulazione. */
    fun simulate(ratings: List<Int?>, scores: List<Double>, roundsLeft: Int, k: Double): List<TournamentChance>? {
        val n = ratings.size
        if (n < 2 || roundsLeft < 1) return null
        val rating = IntArray(n) { ratings[it] ?: DEFAULT_RATING }
        val wins = DoubleArray(n)
        val podium = DoubleArray(n)
        val random = Random(42)
        val score = DoubleArray(n)

        fun play(a: Int, b: Int) {
            val e = expectedScore(rating[a], rating[b])
            val d = k * balance(e)
            val x = random.nextDouble()
            when {
                x < e - d / 2 -> score[a] += 1.0
                x < e + d / 2 -> { score[a] += 0.5; score[b] += 0.5 }
                else -> score[b] += 1.0
            }
        }

        repeat(SIMULATIONS) {
            for (i in 0 until n) score[i] = scores[i]
            repeat(roundsLeft) {
                val order = (0 until n).sortedWith(compareByDescending<Int> { score[it] }.thenByDescending { rating[it] }).toMutableList()
                if (order.size % 2 == 1) score[order.removeAt(order.size - 1)] += 1.0
                var carry = mutableListOf<Int>()
                var i = 0
                while (i < order.size || carry.isNotEmpty()) {
                    val group = carry.toMutableList()
                    val s = if (i < order.size) score[order[i]] else null
                    while (i < order.size && score[order[i]] == s) group += order[i++]
                    carry = mutableListOf()
                    if (group.size % 2 == 1 && i < order.size) carry += group.removeAt(group.size - 1)
                    val half = group.size / 2
                    for (j in 0 until half) play(group[j], group[j + half])
                }
            }
            val best = score.max()
            val winners = (0 until n).filter { score[it] == best }
            winners.forEach { wins[it] += 1.0 / winners.size }
            val third = score.sortedDescending()[min(2, n - 1)]
            for (idx in 0 until n) if (score[idx] >= third) podium[idx] += 1.0
        }

        val result = arrayOfNulls<TournamentChance>(n)
        (0 until n).groupBy { rating[it] to scores[it] }.values.forEach { idxs ->
            val chance = TournamentChance(
                win = idxs.sumOf { wins[it] } / idxs.size / SIMULATIONS,
                podium = idxs.sumOf { podium[it] } / idxs.size / SIMULATIONS,
            )
            idxs.forEach { result[it] = chance }
        }
        return result.map { it!! }
    }

    /** Turni con tutte le scacchiere refertate (o bye): servono a sapere quanti ne mancano. */
    fun completedRounds(rounds: List<TournamentRound>): Int =
        rounds.count { r -> r.pairings.isNotEmpty() && r.pairings.all { it.isBye || it.result != null } }

    fun formatPct(v: Double): String = when {
        v >= 0.9995 -> "100%"
        v >= 0.995 -> ">99%"
        v > 0 && v < 0.005 -> "<1%"
        else -> "${(v * 100).roundToInt()}%"
    }
}
