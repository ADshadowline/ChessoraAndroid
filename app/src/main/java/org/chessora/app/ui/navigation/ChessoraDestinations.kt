package org.chessora.app.ui.navigation

/**
 * Costanti delle route di navigazione (Jetpack Navigation Compose). Le
 * schermate sono divise in due gruppi:
 * - le 5 raggiungibili dalla bottom bar ([BOTTOM_BAR_ROUTES]): Home, News,
 *   Calendario, Classifica, e "Altro" (che raccoglie Direttivo, Negozio,
 *   Impostazioni in un unico menu, invece di occupare altri 3 slot nella
 *   barra - troppe tab in una bottom bar sarebbero illeggibili su schermo).
 * - le schermate di dettaglio/secondarie, aperte sopra le prime senza bottom
 *   bar (dettaglio news, Direttivo, Negozio, Impostazioni).
 */
object ChessoraDestinations {
    const val ONBOARDING = "onboarding"

    const val HOME = "home"
    const val NEWS_LIST = "news"
    const val CALENDAR = "calendar"
    const val RANKING = "ranking"
    const val MORE = "more"

    const val NEWS_DETAIL = "news/{idNews}"
    const val BOARD = "board"
    const val SHOP = "shop"
    const val SETTINGS = "settings"

    fun newsDetail(idNews: Int) = "news/$idNews"

    /** Route che mostrano la bottom bar - tutte le altre sono "a schermo pieno" con solo la freccia indietro. */
    val BOTTOM_BAR_ROUTES = setOf(HOME, NEWS_LIST, CALENDAR, RANKING, MORE)
}
