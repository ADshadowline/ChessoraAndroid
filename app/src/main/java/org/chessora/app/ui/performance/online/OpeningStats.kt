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
/** Chess.com attacca le mosse alla parola precedente ("Defense...3.Nf3", "Gambit2.c4"):
 * qui si separano, così la parola resta nel nome e le mosse finiscono nella variante. */
private fun normalizeOpeningName(name: String): String =
    name.trim()
        .replace(Regex("([A-Za-z])(\\.{3}\\d)"), "$1 $2")
        .replace(Regex("([A-Za-z])(\\d+\\.)"), "$1 $2")
        .replace(Regex("\\s+"), " ")

fun openingFamilyName(rawName: String): String {
    val fullName = normalizeOpeningName(rawName)
    val hasColon = fullName.contains(':')
    val base = fullName.substringBefore(':').substringBefore(',').trim()
    var words = base.split(' ').filter { it.isNotBlank() }
    val cut = words.indexOfFirst { word -> word.any(Char::isDigit) }
    if (cut >= 0) words = words.take(cut)
    // Chess.com non separa la variante con i due punti ("Alekhines Defense Scandinavian
    // Variation"): la variante principale finisce alla prima parola "di famiglia".
    if (!hasColon) {
        val key = words.indexOfFirst { it.lowercase() in FAMILY_END_WORDS }
        if (key >= 0) words = words.take(key + 1)
    }
    val family = words.joinToString(" ").trim()
    return family.ifBlank { fullName.trim() }
}

private val FAMILY_END_WORDS = setOf("defense", "defence", "opening", "game", "gambit")

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

/** Sottovariante di un'apertura: quello che resta del nome completo togliendo la famiglia
 * (vedi [openingFamilyName]) - es. "Sicilian Defense: Alapin Variation, 2...Nf6" ->
 * "Alapin Variation, 2...Nf6". Null quando il nome è solo la famiglia (linea principale). */
fun openingVariantName(fullName: String): String? {
    val full = normalizeOpeningName(fullName)
    val family = openingFamilyName(full)
    val rest = if (full.startsWith(family)) full.substring(family.length) else full.substringAfter(':', "")
    val cleaned = rest.trim().trimStart(':', ',', '-', ' ').trim()
    return cleaned.ifBlank { null }
}

val OnlineGameSummary.openingVariant: String? get() = openingName?.let(::openingVariantName)

/** Filtro dell'elenco partite scelto nella scheda "Aperture": tutta la famiglia (variant=null
 * e allVariants=true) oppure una sola sottovariante (variant=null = linea principale). */
data class OpeningFilter(val family: String, val variant: String?, val allVariants: Boolean) {
    fun matches(game: OnlineGameSummary): Boolean =
        game.openingFamily == family && (allVariants || game.openingVariant == variant)

    val label: String get() = if (allVariants || variant == null) family else "$family: $variant"
}

/** Famiglia di aperture (variante principale) con le sue sottovarianti. */
data class OpeningGroup(val stats: OpeningStats, val variants: List<OpeningVariantStats>)

/** Una sottovariante: variant=null = linea principale (nome senza dettaglio). */
data class OpeningVariantStats(val variant: String?, val stats: OpeningStats)

private fun statsOf(name: String, games: List<OnlineGameSummary>) = OpeningStats(
    name = name,
    totalGames = games.size,
    lichessCount = games.count { it.source == OnlineGameSource.LICHESS },
    chessComCount = games.count { it.source == OnlineGameSource.CHESSCOM },
    wins = games.count { it.result == OnlineGameResult.WIN },
    draws = games.count { it.result == OnlineGameResult.DRAW },
    losses = games.count { it.result == OnlineGameResult.LOSS },
)

private val alphabetical: Comparator<String> = String.CASE_INSENSITIVE_ORDER

/** Partite senza apertura nota (fonte che non l'ha valorizzata) escluse dal conteggio: non
 * avrebbe senso una riga "apertura sconosciuta" tra le aperture vere e proprie. Famiglie in
 * ordine alfabetico; dentro ciascuna, prima la linea principale e poi le sottovarianti in
 * ordine alfabetico. */
fun List<OnlineGameSummary>.computeOpeningGroups(): List<OpeningGroup> =
    this.mapNotNull { game -> game.openingFamily?.let { it to game } }
        .groupBy({ it.first }, { it.second })
        .map { (family, games) ->
            val variants = games.groupBy { it.openingVariant }
                .map { (variant, vGames) -> OpeningVariantStats(variant, statsOf(variant ?: family, vGames)) }
                .sortedWith(compareBy<OpeningVariantStats> { it.variant != null }.thenBy(alphabetical) { it.variant ?: "" })
            OpeningGroup(statsOf(family, games), variants)
        }
        .sortedWith(compareBy(alphabetical) { it.stats.name })

fun List<OnlineGameSummary>.computeOpeningStats(): List<OpeningStats> = computeOpeningGroups().map { it.stats }
