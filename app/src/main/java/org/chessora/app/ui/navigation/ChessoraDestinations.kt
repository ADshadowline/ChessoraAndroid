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
    /** Solo al primissimo avvio (vedi hasChosenLanguage in ui/language/AppLanguage.kt),
     * prima ancora del login - senza questo l'utente non capirebbe come cambiare lingua. */
    const val LANGUAGE_PICKER = "language-picker"
    const val MEMBERSHIP_QUESTION = "membership-question"
    const val ONBOARDING = "onboarding"

    // ---------- Autenticazione (ui/auth/) - accesso obbligatorio, vedi SPLASH ----------
    const val AUTH_LOGIN = "auth/login"
    const val AUTH_LOGIN_EMAIL = "auth/login-email"
    const val AUTH_LOGIN_FIDE = "auth/login-fide"
    const val AUTH_FORGOT_PASSWORD = "auth/forgot-password"
    /** Secondo passo dopo un login Google con profilo incompleto - vedi
     * CompleteProfileScreen.kt. */
    const val AUTH_COMPLETE_PROFILE = "auth/complete-profile"
    /** [email] mostrato in "Controlla la tua posta" dopo una registrazione via ID FIDE -
     * vedi EmailPendingScreen.kt. */
    const val AUTH_EMAIL_PENDING = "auth/email-pending/{email}"

    const val HOME = "home"
    /** Elenco eventi "classico" - stesso contenuto della Home in visualizzazione classica,
     * raggiungibile anche dalla griglia di icone quando la Home è in visualizzazione
     * desktop (vedi ui/home/HomeScreen.kt, DesktopHomeGrid). */
    const val EVENTS = "events"
    const val NEWS_LIST = "news"
    const val REGISTRATIONS = "registrations"
    const val MESSAGING = "messaging"
    const val MORE = "more"

    /** [club] è il publicCode di provenienza (o "_all_" se aperta dall'elenco "nel
     * desktop", su tutti i circoli) - serve perché NewsDetailViewModel non ha un
     * endpoint "singolo articolo per id" e deve rifare la stessa fetch di lista già usata
     * per raggiungere questo articolo (vedi NewsDetailViewModel). */
    const val NEWS_DETAIL = "news/{idNews}/{club}"
    /** Sotto-menu "Il Circolo" (Direttivo/Statuto/Contatti/Dove raggiungerlo, più
     * Eventi/Calendario/Video/News/Classifica filtrati sul solo circolo selezionato) -
     * vedi ui/club/ClubScreen.kt. Prima l'icona apriva direttamente BOARD; ora apre
     * questo menu, da cui "Direttivo" apre BOARD invariato. */
    const val CLUB = "club"
    const val CLUB_CONTACTS = "club-contacts"
    const val CLUB_LOCATION = "club-location"
    /** Eventi/Calendario/Video/News/Classifica del solo circolo selezionato, raggiunte da
     * "Il Circolo" - a differenza delle omonime EVENTS/CALENDAR/VIDEO/NEWS_LIST/RANKING
     * (che sul desktop mostrano invece sempre tutti i circoli), queste passano sempre il
     * circolo corrente. */
    const val CLUB_EVENTS = "club-events"
    const val CLUB_CALENDAR = "club-calendar"
    const val CLUB_VIDEO = "club-video"
    const val CLUB_NEWS = "club-news"
    const val CLUB_RANKING = "club-ranking"
    const val BOARD = "board"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    /** Elenco riordinabile/attivabile delle icone della Home desktop - vedi
     * ui/settings/IconSettingsScreen.kt, raggiungibile solo da Impostazioni. */
    const val ICON_SETTINGS = "icon-settings"
    /** Canali YouTube spuntabili per la sezione Video - vedi
     * ui/settings/VideoChannelSettingsScreen.kt, raggiungibile solo da Impostazioni. */
    const val VIDEO_CHANNEL_SETTINGS = "video-channel-settings"
    // Non più in bottom bar (spostati dentro "Altro") - raggiungibili solo da lì,
    // come Direttivo/Negozio.
    const val CALENDAR = "calendar"
    const val RANKING = "ranking"
    const val VIDEO = "video"
    // [focus] facoltativo ("standard"/"rapid"/"blitz") pre-seleziona quella cadenza -
    // vedi il click sui punteggi Elo in ChessoraNavHost.ClubBrandingTopBar. Stringa
    // vuota di default (non null: NavType.StringType non ammette argomenti opzionali
    // nulli) equivale a "mostra tutte e tre le cadenze", il comportamento preesistente.
    const val PERFORMANCE = "performance?focus={focus}"
    /** Scacchiera di una partita Lichess/Chess.com aperta dalla scheda "Scacchi Online" di
     * Le mie performance - vedi ui/performance/online/OnlineGamesScreen.kt e
     * ui/performance/lichess/LichessGameViewerScreen.kt /
     * ui/performance/chesscom/ChessComGameViewerScreen.kt. */
    const val LICHESS_GAME_VIEWER = "lichess-game/{gameId}"
    const val CHESSCOM_GAME_VIEWER = "chesscom-game/{gameId}"
    const val PROFILE_PHOTO = "profile-photo"
    const val TOURNAMENT_DETAIL = "tornei/{idTournament}"
    /** Abbinamenti/classifica di un torneo AVVIATO (LifecycleStatus InCorso/Concluso) -
     * aperta da RegistrationsScreen al posto di TOURNAMENT_DETAIL quando il torneo
     * preiscritto non accetta più iscrizioni, vedi ui/pairings/PairingsScreen.kt. */
    const val TOURNAMENT_PAIRINGS = "tornei/{idTournament}/abbinamenti"
    /** [url] è l'URL assoluto del bando, URL-encoded - vedi BandoViewerScreen. */
    const val BANDO_VIEWER = "bando-viewer/{url}"

    // ---------- Gestione tornei (organizzatore, ui/tournamentmanager/) ----------
    /** Elenco dei tornei InCorso che l'utente (auto-promosso a organizzatore via la
     * claim self-service, vedi OrganizerSession) può gestire. */
    const val TOURNAMENT_MANAGER_LIST = "gestione-tornei"
    /** Turni/scacchiere/risultati di un singolo torneo, lato organizzatore - genera/
     * pubblica turni e inserisce risultati (a differenza di TOURNAMENT_PAIRINGS, che è
     * sola lettura per un giocatore preiscritto). [turni] (TournamentSummary.turni,
     * portato dalla lista invece di rifare una fetch qui) serve per sapere quando l'ultimo
     * turno generato è anche l'ultimo del torneo (mostra "Premiazione" invece di "Genera
     * turno successivo"). */
    const val TOURNAMENT_MANAGER_ROUNDS = "gestione-tornei/{idTournament}/{turni}"

    const val NEW_MESSAGE = "messaging/new"

    /** recipientId è l'idPlayer (diretta) o l'idClub (con un circolo) del
     * destinatario - serve solo quando idConversation è ancora -1 (conversazione
     * non ancora creata, primo messaggio in arrivo da NewMessageScreen); per una
     * conversazione già esistente vale 0 e non viene usato (vedi ConversationViewModel). */
    const val CONVERSATION = "messaging/conversation/{idConversation}/{isClubConversation}/{recipientId}/{displayName}"

    fun emailPending(email: String) = "auth/email-pending/${java.net.URLEncoder.encode(email, "UTF-8")}"

    /** [club] null = articolo raggiunto dall'elenco "nel desktop" (tutti i circoli). */
    fun newsDetail(idNews: Int, club: String?) =
        "news/$idNews/${if (club != null) java.net.URLEncoder.encode(club, "UTF-8") else "_all_"}"

    fun performance(focus: String? = null) = if (focus != null) "performance?focus=$focus" else "performance"

    fun lichessGameViewer(gameId: String) = "lichess-game/${java.net.URLEncoder.encode(gameId, "UTF-8")}"

    fun chessComGameViewer(gameId: String) = "chesscom-game/${java.net.URLEncoder.encode(gameId, "UTF-8")}"

    fun tournamentDetail(idTournament: Int) = "tornei/$idTournament"

    fun tournamentPairings(idTournament: Int) = "tornei/$idTournament/abbinamenti"

    fun tournamentManagerRounds(idTournament: Int, turni: Int) = "gestione-tornei/$idTournament/$turni"

    fun bandoViewer(url: String) = "bando-viewer/${java.net.URLEncoder.encode(url, "UTF-8")}"

    fun conversation(idConversation: Int, isClubConversation: Boolean, recipientId: Int, displayName: String): String {
        val encodedName = java.net.URLEncoder.encode(displayName, "UTF-8")
        return "messaging/conversation/$idConversation/$isClubConversation/$recipientId/$encodedName"
    }

    /** Route che mostrano la bottom bar - tutte le altre sono "a schermo pieno" con solo la freccia indietro. */
    val BOTTOM_BAR_ROUTES = setOf(HOME, NEWS_LIST, REGISTRATIONS, MESSAGING, MORE)
}
