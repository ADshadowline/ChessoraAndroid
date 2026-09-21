package org.chessora.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.AuthSession
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.SiteBranding
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.push.DeviceRegistration
import org.chessora.app.ui.auth.AuthSessionPersister

/**
 * Unico ViewModel creato a livello di MainActivity (non per-schermata) e
 * condiviso da tutto il grafo di navigazione: tiene il circolo scelto e il
 * branding (nome/logo) caricato da /api/site-settings, cosi' ogni schermata
 * puo' leggerli senza rifare la stessa chiamata o duplicare la lettura da
 * DataStore. Vedi ui/navigation/ChessoraNavHost.kt per come viene istanziato
 * e passato giù.
 */
class SessionViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    /** True appena AuthSession.accessToken (già popolato in modo sincrono da
     * ChessoraApplication.onCreate) risulta non vuoto - fonte di verità per "l'utente ha
     * fatto un vero login" (vedi ui/auth/), a differenza di [ClubPreferences.isAuthenticated]
     * che [AuthSessionPersister] valorizza ANCHE lui a ogni login riuscito ma che, per
     * installazioni esistenti aggiornate da questa versione, potrebbe già risultare true
     * dalla vecchia identificazione debole senza che esista alcun token. */
    private val _isLoggedIn = MutableStateFlow(!AuthSession.accessToken.isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    /** Richiamata da ChessoraNavHost quando NetworkModule.onUnauthorized scatta (risposta
     * 401): il token non è più valido, azzera tutto e rimanda l'utente al login. */
    fun onUnauthorized() {
        _isLoggedIn.value = false
        viewModelScope.launch { AuthSessionPersister.clear(authPreferences, clubPreferences) }
    }

    /** Richiamata da ui/auth/ subito dopo un login/registrazione completata con successo. */
    fun onLoggedIn() {
        _isLoggedIn.value = true
        viewModelScope.launch { _selectedClub.value = clubPreferences.selectedClub.first() }
    }

    /** Richiamata da Impostazioni ("Esci"): azzera sessione E circolo scelto, cosi' un
     * prossimo utente sullo stesso dispositivo non eredita niente del precedente. Le altre
     * preferenze locali (sfondi, ordine icone, notifiche...) restano intatte - per quelle
     * vedi [resetAllSettings]. */
    fun logout() {
        _isLoggedIn.value = false
        _selectedClub.value = null
        _branding.value = null
        _membersCount.value = null
        _identityResolved.value = false
        _identifiedPlayerName.value = null
        _isTournamentManager.value = false
        viewModelScope.launch {
            AuthSessionPersister.clear(authPreferences, clubPreferences)
            clubPreferences.clearSelectedClub()
        }
    }

    /** Richiamata da Impostazioni ("Reimposta impostazioni"): a differenza di [logout],
     * azzera OGNI preferenza locale (circolo scelto, sessione, sfondi, ordine/visibilità
     * delle icone Home, notifiche...) - non solo l'identità - riportando l'app allo stato
     * di primissimo avvio, poi richiede di autenticarsi di nuovo da zero. */
    fun resetAllSettings() {
        _isLoggedIn.value = false
        _selectedClub.value = null
        _branding.value = null
        _membersCount.value = null
        _identityResolved.value = false
        _identifiedPlayerName.value = null
        _isTournamentManager.value = false
        viewModelScope.launch {
            authPreferences.clearSession()
            AuthSession.accessToken = null
            clubPreferences.clearAll()
        }
    }

    // MutableStateFlow di proprietà del ViewModel (non più un semplice stateIn() sopra
    // clubPreferences.selectedClub): clearSelectedClub() deve poter azzerare il valore
    // SINCRONAMENTE, prima ancora che la scrittura su DataStore (asincrona) sia
    // completata - altrimenti "Cambia circolo" naviga a ONBOARDING mentre selectedClub
    // è ancora quello vecchio per un istante, e la guardia più sotto in
    // ChessoraNavHost.kt (pensata solo per saltare l'onboarding ai riavvii a freddo)
    // rimbalza subito l'utente via prima che veda il selettore.
    private val _selectedClub = MutableStateFlow<String?>(null)
    val selectedClub: StateFlow<String?> = _selectedClub

    private val _branding = MutableStateFlow<SiteBranding?>(null)
    val branding: StateFlow<SiteBranding?> = _branding

    // Stessa ragione di _selectedClub sopra: SplashScreen/ChessoraNavHost devono poter
    // decidere la destinazione iniziale leggendo un valore già in memoria, non un Flow
    // DataStore che emetterebbe con un frame di ritardo.
    private val _identityResolved = MutableStateFlow(false)
    val identityResolved: StateFlow<Boolean> = _identityResolved

    private val _identifiedPlayerName = MutableStateFlow<String?>(null)
    val identifiedPlayerName: StateFlow<String?> = _identifiedPlayerName

    /** Numero di soci del circolo scelto, mostrato in barra accanto al nome del
     * circolo (vedi ChessoraNavHost.ClubBrandingTopBar) - null finché non ancora
     * caricato. */
    private val _membersCount = MutableStateFlow<Int?>(null)
    val membersCount: StateFlow<Int?> = _membersCount

    /** True se il socio identificato ha il ruolo pubblico "Responsabile dei tornei" nel
     * circolo scelto (vedi GET /api/players/{idPlayer}/roles, nessun login richiesto) -
     * mostra l'icona "Gestione tornei" in ChessoraNavHost.ClubBrandingTopBar. */
    private val _isTournamentManager = MutableStateFlow(false)
    val isTournamentManager: StateFlow<Boolean> = _isTournamentManager

    /** Immagine di sfondo scelta in Impostazioni per lo SplashScreen (mai inviata al
     * server, vedi ClubPreferences.splashBackgroundUri) - letta qui perché SplashScreen è
     * mostrato prima ancora che esista un circolo scelto/identità risolta. */
    val splashBackgroundUri: StateFlow<String?> = clubPreferences.splashBackgroundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "classic" o "desktop" - letto qui (non solo in HomeViewModel) perché
     * ChessoraNavHost deve nascondere la bottom bar su OGNI schermata quando la
     * navigazione avviene tramite la griglia di icone (vedi ui/home/HomeScreen.kt). */
    val displayMode: StateFlow<String> = clubPreferences.displayMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClubPreferences.DISPLAY_MODE_CLASSIC)

    init {
        // Al primo avvio, se un circolo era già stato scelto in una sessione
        // precedente, carica subito il suo branding e registra il device (il
        // token FCM potrebbe essere lo stesso di sempre, ma repository.registerDevice
        // è un upsert innocuo da ripetere - vedi push/DeviceRegistration.kt).
        viewModelScope.launch {
            val club = clubPreferences.selectedClub.first()
            _selectedClub.value = club
            if (club != null) {
                loadBranding(club)
            }
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
        }
        viewModelScope.launch {
            _identityResolved.value = clubPreferences.identityResolved.first()
            _identifiedPlayerName.value = clubPreferences.identifiedPlayerName.first()
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            if (idPlayer != null) {
                clubPreferences.selectedClub.first()?.let { loadTournamentManagerStatus(idPlayer, it) }
            }
        }
    }

    /** Chiamata dall'onboarding (o dalle Impostazioni, per cambiare circolo). */
    fun selectClub(club: String) {
        _selectedClub.value = club
        viewModelScope.launch {
            clubPreferences.setSelectedClub(club)
            loadBranding(club)
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
        }
    }

    /** Chiamata da "Cambia circolo" nelle Impostazioni, PRIMA di navigare a
     * ONBOARDING (vedi ChessoraNavHost.kt): azzera subito il circolo in memoria
     * cosi' l'utente vede davvero il selettore invece di un rimbalzo istantaneo
     * a Home - vedi il commento su [_selectedClub] qui sopra. L'identificazione è
     * per-circolo, quindi viene azzerata insieme: il nuovo circolo la richiederà
     * di nuovo. */
    fun clearSelectedClub() {
        _selectedClub.value = null
        _branding.value = null
        _membersCount.value = null
        _identityResolved.value = false
        _identifiedPlayerName.value = null
        _isTournamentManager.value = false
        viewModelScope.launch {
            clubPreferences.clearSelectedClub()
            clubPreferences.clearIdentity()
        }
    }

    /** Richiamata dal NavHost dopo che IdentityScreen ha salvato il proprio esito
     * (identificato o rifiutato) su DataStore, per riflettere subito il nuovo stato
     * senza aspettare la prossima ricomposizione da un Flow. */
    fun refreshIdentity() {
        viewModelScope.launch {
            _identityResolved.value = clubPreferences.identityResolved.first()
            _identifiedPlayerName.value = clubPreferences.identifiedPlayerName.first()
            val idPlayer = clubPreferences.identifiedPlayerId.first()
            _isTournamentManager.value = false
            if (idPlayer != null) {
                clubPreferences.selectedClub.first()?.let { loadTournamentManagerStatus(idPlayer, it) }
            }
        }
    }

    private suspend fun loadBranding(club: String) {
        if (club == ClubPreferences.PLATFORM_CLUB_CODE) {
            // "Modalità piattaforma" (nessun circolo, vedi MembershipQuestionScreen):
            // nessuna chiamata di rete club-scoped, branding sintetico con il nome/logo
            // di Chessora stessa (strings.xml platform_branding_name) invece del nome
            // di un circolo, nessun conteggio soci da mostrare.
            _branding.value = SiteBranding(namePrefix = "", nameHighlight = "Chessora Platform", logoImage = null)
            _membersCount.value = null
            return
        }
        repository.getSiteSettings(club).onSuccess { settings ->
            _branding.value = settings.site
        }
        repository.getStats(club).onSuccess { stats ->
            _membersCount.value = stats.membersCount
        }
    }

    private suspend fun loadTournamentManagerStatus(idPlayer: Int, club: String) {
        if (club == ClubPreferences.PLATFORM_CLUB_CODE) return
        repository.getPlayerRoles(idPlayer, club).onSuccess { roles ->
            _isTournamentManager.value = roles.contains("Responsabile dei tornei")
        }
    }

}
