package org.chessora.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Unica estensione di Context->DataStore dell'app: deve essere dichiarata una
 * volta sola a livello di file (non dentro la classe) per evitare istanze
 * DataStore duplicate sullo stesso file, un errore classico di Jetpack DataStore.
 */
private val Context.dataStore by preferencesDataStore(name = "chessora_prefs")

/**
 * Unico stato persistito localmente da questa app (docs/android-app-spec.md §2
 * "Jetpack DataStore... per salvare in locale il circolo scelto e le
 * preferenze utente"): non c'è un vero account/login, ma dalla schermata di
 * identificazione facoltativa (vedi ui/identity/) l'app ricorda comunque se
 * l'utente si è fatto riconoscere come un socio del circolo o ha rifiutato,
 * cosi' da non richiederglielo più a ogni avvio.
 *
 * - [selectedClub]: il publicCode del circolo scelto in onboarding (null finché
 *   non è mai stato scelto nulla - vedi MainActivity.kt che decide se mostrare
 *   l'onboarding o la home in base a questo valore). Il numero interno IdClub
 *   non viene mai persistito qui: il server non lo accetta più su nessun
 *   endpoint pubblico tranne la registrazione device, dove viene risolto al
 *   volo da questo codice - vedi push/DeviceRegistration.kt.
 * - [notificationsEnabled]: se true, la app registra/aggiorna il token FCM ad
 *   ogni avvio; se false, la disattivazione avviene lato client cancellando la
 *   registrazione (vedi push/DeviceRegistration.kt) - il server non ha un
 *   concetto separato di "utente disiscritto", semplicemente non riceve più
 *   quel token.
 * - [identityResolved]/[identifiedPlayerId]/[identifiedPlayerName]: esito della
 *   schermata di identificazione (vedi ui/identity/IdentityScreen.kt) - true da
 *   solo il momento in cui l'utente si identifica con successo O rifiuta
 *   esplicitamente ("Salta"), cosi' non viene più richiesta finché non cambia
 *   circolo o non la riavvia da Impostazioni.
 */
class ClubPreferences(context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val SELECTED_CLUB_CODE = stringPreferencesKey("selected_club_code")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_REGISTERED_FCM_TOKEN = stringPreferencesKey("last_registered_fcm_token")
        val IDENTITY_RESOLVED = booleanPreferencesKey("identity_resolved")
        val IDENTIFIED_PLAYER_ID = intPreferencesKey("identified_player_id")
        val IDENTIFIED_PLAYER_NAME = stringPreferencesKey("identified_player_name")
        val AUTHENTICATED_EMAIL = stringPreferencesKey("authenticated_email")
        val AUTHENTICATED_PHONE = stringPreferencesKey("authenticated_phone")
        val PRE_REGISTRATION_CONTACT_ID = intPreferencesKey("pre_registration_contact_id")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val SPLASH_BACKGROUND_URI = stringPreferencesKey("splash_background_uri")
        val DESKTOP_BACKGROUND_URI = stringPreferencesKey("desktop_background_uri")
        val DESKTOP_ICON_ORDER = stringPreferencesKey("desktop_icon_order")
        val DESKTOP_HIDDEN_ICONS = stringPreferencesKey("desktop_hidden_icons")
    }

    val selectedClub: Flow<String?> = dataStore.data.map { it[Keys.SELECTED_CLUB_CODE] }

    /** Vero se [selectedClub] è il valore sentinella [PLATFORM_CLUB_CODE], cioè il
     * socio ha scelto "non sono iscritto a nessun circolo" (vedi
     * ui/onboarding/MembershipQuestionScreen.kt) - la app allora mostra Home/News
     * aggregate di tutti i circoli invece che di uno solo. */
    val isPlatformMode: Flow<Boolean> = selectedClub.map { it == PLATFORM_CLUB_CODE }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    /**
     * Ultimo token FCM già inviato con successo a POST /api/devices/register,
     * per evitare di rifare la chiamata di rete a ogni avvio se il token non è
     * cambiato (vedi push/DeviceRegistration.kt.maybeRegister).
     */
    val lastRegisteredFcmToken: Flow<String?> = dataStore.data.map { it[Keys.LAST_REGISTERED_FCM_TOKEN] }

    val identityResolved: Flow<Boolean> = dataStore.data.map { it[Keys.IDENTITY_RESOLVED] ?: false }

    val identifiedPlayerId: Flow<Int?> = dataStore.data.map { it[Keys.IDENTIFIED_PLAYER_ID] }

    val identifiedPlayerName: Flow<String?> = dataStore.data.map { it[Keys.IDENTIFIED_PLAYER_NAME] }

    /** Email verificata via Google Sign-In o numero verificato via SIM/phone hint - salvati
     * qui ANCHE quando l'identificazione non trova un socio corrispondente (a differenza di
     * [identifiedPlayerId], che resta null in quel caso): è la prova di identità richiesta
     * per poter preiscriversi a un torneo (vedi ui/tournaments/), distinta dall'essere un
     * socio riconosciuto. */
    val authenticatedEmail: Flow<String?> = dataStore.data.map { it[Keys.AUTHENTICATED_EMAIL] }
    val authenticatedPhone: Flow<String?> = dataStore.data.map { it[Keys.AUTHENTICATED_PHONE] }

    /** True se l'utente ha completato con successo Google Sign-In o la verifica del numero
     * (indipendentemente dall'esito del match a un socio) - condizione minima per poter
     * preiscriversi a un torneo. */
    val isAuthenticated: Flow<Boolean> = dataStore.data.map {
        !it[Keys.AUTHENTICATED_EMAIL].isNullOrBlank() || !it[Keys.AUTHENTICATED_PHONE].isNullOrBlank()
    }

    /** orga.PlayerContacts.Id restituito dall'ultima preiscrizione riuscita - percorso
     * veloce per GET /api/tornei/mie-preiscrizioni senza dover ripetere la risoluzione per
     * email/telefono lato server. */
    val preRegistrationContactId: Flow<Int?> = dataStore.data.map { it[Keys.PRE_REGISTRATION_CONTACT_ID] }

    /** "classic" (elenco, comportamento attuale) o "desktop" (griglia di icone in Home) -
     * vedi ui/home/HomeScreen.kt. */
    val displayMode: Flow<String> = dataStore.data.map { it[Keys.DISPLAY_MODE] ?: DISPLAY_MODE_CLASSIC }

    /** Uri content:// locali scelti dall'utente per personalizzare l'app - MAI inviati al
     * server, persistiti con takePersistableUriPermission (vedi SettingsScreen.kt) cosi'
     * restano leggibili anche dopo il riavvio del processo. */
    val splashBackgroundUri: Flow<String?> = dataStore.data.map { it[Keys.SPLASH_BACKGROUND_URI] }
    val desktopBackgroundUri: Flow<String?> = dataStore.data.map { it[Keys.DESKTOP_BACKGROUND_URI] }

    /** Ordine personalizzato delle icone della Home desktop (id separati da virgola -
     * vedi ui/home/HomeScreen.kt DESKTOP_ICON_DESCRIPTORS), scelto dall'utente in
     * ui/settings/IconSettingsScreen.kt. Lista vuota = nessuna personalizzazione,
     * resta l'ordine di default; un id assente qui (icona aggiunta in una versione
     * successiva) viene sempre accodato in fondo, mai perso. */
    val desktopIconOrder: Flow<List<String>> = dataStore.data.map {
        it[Keys.DESKTOP_ICON_ORDER]?.split(',')?.filter { id -> id.isNotBlank() } ?: emptyList()
    }

    /** Id delle icone della Home desktop nascoste dall'utente (stesso elenco di id di
     * [desktopIconOrder]) - vedi ui/settings/IconSettingsScreen.kt. */
    val desktopHiddenIcons: Flow<Set<String>> = dataStore.data.map {
        it[Keys.DESKTOP_HIDDEN_ICONS]?.split(',')?.filter { id -> id.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun setDesktopIconOrder(order: List<String>) {
        dataStore.edit { it[Keys.DESKTOP_ICON_ORDER] = order.joinToString(",") }
    }

    suspend fun setDesktopIconHidden(id: String, hidden: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.DESKTOP_HIDDEN_ICONS]?.split(',')?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (hidden) current.add(id) else current.remove(id)
            prefs[Keys.DESKTOP_HIDDEN_ICONS] = current.joinToString(",")
        }
    }

    suspend fun setDisplayMode(mode: String) {
        dataStore.edit { it[Keys.DISPLAY_MODE] = mode }
    }

    suspend fun setSplashBackgroundUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(Keys.SPLASH_BACKGROUND_URI) else it[Keys.SPLASH_BACKGROUND_URI] = uri
        }
    }

    suspend fun setDesktopBackgroundUri(uri: String?) {
        dataStore.edit {
            if (uri == null) it.remove(Keys.DESKTOP_BACKGROUND_URI) else it[Keys.DESKTOP_BACKGROUND_URI] = uri
        }
    }

    suspend fun setSelectedClub(club: String) {
        dataStore.edit { it[Keys.SELECTED_CLUB_CODE] = club }
    }

    suspend fun clearSelectedClub() {
        dataStore.edit { it.remove(Keys.SELECTED_CLUB_CODE) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLastRegisteredFcmToken(token: String?) {
        dataStore.edit {
            if (token == null) it.remove(Keys.LAST_REGISTERED_FCM_TOKEN) else it[Keys.LAST_REGISTERED_FCM_TOKEN] = token
        }
    }

    suspend fun setIdentified(idPlayer: Int, name: String) {
        dataStore.edit {
            it[Keys.IDENTITY_RESOLVED] = true
            it[Keys.IDENTIFIED_PLAYER_ID] = idPlayer
            it[Keys.IDENTIFIED_PLAYER_NAME] = name
        }
    }

    suspend fun setIdentityDeclined() {
        dataStore.edit {
            it[Keys.IDENTITY_RESOLVED] = true
            it.remove(Keys.IDENTIFIED_PLAYER_ID)
            it.remove(Keys.IDENTIFIED_PLAYER_NAME)
        }
    }

    /** Chiamato ogni volta che Google Sign-In/SIM restituiscono un'email o un numero
     * utilizzabile, indipendentemente dal fatto che l'identificazione trovi poi un socio
     * corrispondente - vedi IdentityViewModel.saveAndFinish. */
    suspend fun setAuthenticated(email: String?, phone: String?) {
        dataStore.edit {
            if (!email.isNullOrBlank()) it[Keys.AUTHENTICATED_EMAIL] = email
            if (!phone.isNullOrBlank()) it[Keys.AUTHENTICATED_PHONE] = phone
        }
    }

    suspend fun setPreRegistrationContactId(contactId: Int) {
        dataStore.edit { it[Keys.PRE_REGISTRATION_CONTACT_ID] = contactId }
    }

    /** Usato quando si cambia circolo: i soci sono per-circolo, quindi una
     * identificazione già fatta non ha senso per il nuovo circolo scelto. */
    suspend fun clearIdentity() {
        dataStore.edit {
            it.remove(Keys.IDENTITY_RESOLVED)
            it.remove(Keys.IDENTIFIED_PLAYER_ID)
            it.remove(Keys.IDENTIFIED_PLAYER_NAME)
            it.remove(Keys.AUTHENTICATED_EMAIL)
            it.remove(Keys.AUTHENTICATED_PHONE)
            it.remove(Keys.PRE_REGISTRATION_CONTACT_ID)
        }
    }

    /** "Reimposta impostazioni" da Impostazioni (vedi SessionViewModel.resetAllSettings) -
     * a differenza di [clearIdentity]/[clearSelectedClub] azzera TUTTO (circolo, identità,
     * sfondi, ordine/visibilità icone Home, preferenza notifiche...), riportando l'app allo
     * stato di primissimo avvio. */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    companion object {
        /** Valore sentinella per [selectedClub]: soddisfa "un circolo è già stato
         * scelto" (non fa ripartire l'onboarding) ma NON è un vero publicCode -
         * ogni punto che userebbe [selectedClub] per una chiamata di rete club-scoped
         * deve prima controllare [isPlatformMode] e usare l'equivalente aggregato. */
        const val PLATFORM_CLUB_CODE = "__platform__"

        const val DISPLAY_MODE_CLASSIC = "classic"
        const val DISPLAY_MODE_DESKTOP = "desktop"
    }
}
