package org.chessora.app.ui.navigation

/**
 * Costanti delle route di navigazione (Jetpack Navigation Compose). Le
 * schermate elencate in docs/android-app-spec.md §7 sono divise in due gruppi:
 * - le 6 raggiungibili dalla bottom bar ([BOTTOM_BAR_ROUTES]): Home, News,
 *   Calendario, Tornei, Classifica, e "Altro" (che raccoglie Direttivo,
 *   Negozio, Impostazioni in un unico menu, invece di occupare altri 3 slot
 *   nella barra - 9 tab in una bottom bar sarebbero illeggibili su schermo).
 * - le schermate di dettaglio/secondarie, aperte sopra le prime senza bottom
 *   bar (dettaglio news, dettaglio torneo, Direttivo, Negozio, Impostazioni).
 */
object ChessoraDestinations {
    const val ONBOARDING = "onboarding"

    const val HOME = "home"
    const val NEWS_LIST = "news"
    const val CALENDAR = "calendar"
    const val TORNEI_LIST = "tornei"
    const val RANKING = "ranking"
    const val MORE = "more"

    const val NEWS_DETAIL = "news/{idNews}"
    const val TORNEO_DETAIL = "tornei/{idTorneo}"
    const val BOARD = "board"
    const val SHOP = "shop"
    const val SETTINGS = "settings"

    fun newsDetail(idNews: Int) = "news/$idNews"
    fun torneoDetail(idTorneo: Int) = "tornei/$idTorneo"

    /** Route che mostrano la bottom bar - tutte le altre sono "a schermo pieno" con solo la freccia indietro. */
    val BOTTOM_BAR_ROUTES = setOf(HOME, NEWS_LIST, CALENDAR, TORNEI_LIST, RANKING, MORE)
}
