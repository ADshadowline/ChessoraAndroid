package org.chessora.app.ui.navigation

/**
 * Costanti delle route di navigazione (Jetpack Navigation Compose). Le
 * schermate sono divise in due gruppi:
 * - le 5 raggiungibili dalla bottom bar ([BOTTOM_BAR_ROUTES]): Home, News,
 *   Iscrizioni, Messaggi, e "Altro" (che raccoglie Direttivo, Classifica,
 *   Negozio, Impostazioni in un unico menu, invece di occupare altri slot
 *   nella barra - troppe tab in una bottom bar sarebbero illeggibili su
 *   schermo).
 * - le schermate di dettaglio/secondarie, aperte sopra le prime senza bottom
 *   bar (dettaglio news, Direttivo, Classifica, Negozio, Impostazioni,
 *   conversazione/nuovo messaggio).
 */
object ChessoraDestinations {
    const val SPLASH = "splash"
    const val MEMBERSHIP_QUESTION = "membership-question"
    const val ONBOARDING = "onboarding"
    const val IDENTITY = "identity"

    const val HOME = "home"
    /** Elenco eventi "classico" - stesso contenuto della Home in visualizzazione classica,
     * raggiungibile anche dalla griglia di icone quando la Home è in visualizzazione
     * desktop (vedi ui/home/HomeScreen.kt, DesktopHomeGrid). */
    const val EVENTS = "events"
    const val NEWS_LIST = "news"
    const val REGISTRATIONS = "registrations"
    const val MESSAGING = "messaging"
    const val MORE = "more"

    const val NEWS_DETAIL = "news/{idNews}"
    const val BOARD = "board"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    /** Elenco riordinabile/attivabile delle icone della Home desktop - vedi
     * ui/settings/IconSettingsScreen.kt, raggiungibile solo da Impostazioni. */
    const val ICON_SETTINGS = "icon-settings"
    // Non più in bottom bar (spostati dentro "Altro") - raggiungibili solo da lì,
    // come Direttivo/Negozio.
    const val CALENDAR = "calendar"
    const val RANKING = "ranking"
    // [focus] facoltativo ("standard"/"rapid"/"blitz") pre-seleziona quella cadenza -
    // vedi il click sui punteggi Elo in ChessoraNavHost.ClubBrandingTopBar. Stringa
    // vuota di default (non null: NavType.StringType non ammette argomenti opzionali
    // nulli) equivale a "mostra tutte e tre le cadenze", il comportamento preesistente.
    const val PERFORMANCE = "performance?focus={focus}"
    const val PROFILE_PHOTO = "profile-photo"
    const val TOURNAMENT_DETAIL = "tornei/{idTournament}"
    /** [url] è l'URL assoluto del bando, URL-encoded - vedi BandoViewerScreen. */
    const val BANDO_VIEWER = "bando-viewer/{url}"

    const val NEW_MESSAGE = "messaging/new"

    /** recipientId è l'idPlayer (diretta) o l'idClub (con un circolo) del
     * destinatario - serve solo quando idConversation è ancora -1 (conversazione
     * non ancora creata, primo messaggio in arrivo da NewMessageScreen); per una
     * conversazione già esistente vale 0 e non viene usato (vedi ConversationViewModel). */
    const val CONVERSATION = "messaging/conversation/{idConversation}/{isClubConversation}/{recipientId}/{displayName}"

    fun newsDetail(idNews: Int) = "news/$idNews"

    fun performance(focus: String? = null) = if (focus != null) "performance?focus=$focus" else "performance"

    fun tournamentDetail(idTournament: Int) = "tornei/$idTournament"

    fun bandoViewer(url: String) = "bando-viewer/${java.net.URLEncoder.encode(url, "UTF-8")}"

    fun conversation(idConversation: Int, isClubConversation: Boolean, recipientId: Int, displayName: String): String {
        val encodedName = java.net.URLEncoder.encode(displayName, "UTF-8")
        return "messaging/conversation/$idConversation/$isClubConversation/$recipientId/$encodedName"
    }

    /** Route che mostrano la bottom bar - tutte le altre sono "a schermo pieno" con solo la freccia indietro. */
    val BOTTOM_BAR_ROUTES = setOf(HOME, NEWS_LIST, REGISTRATIONS, MESSAGING, MORE)
}
