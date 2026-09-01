package org.chessora.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
 * preferenze utente"): niente account personale, niente login, quindi niente
 * altro da ricordare tra un avvio e l'altro.
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
 */
class ClubPreferences(context: Context) {
    private val dataStore = context.dataStore

    private object Keys {
        val SELECTED_CLUB_CODE = stringPreferencesKey("selected_club_code")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_REGISTERED_FCM_TOKEN = stringPreferencesKey("last_registered_fcm_token")
    }

    val selectedClub: Flow<String?> = dataStore.data.map { it[Keys.SELECTED_CLUB_CODE] }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    /**
     * Ultimo token FCM già inviato con successo a POST /api/devices/register,
     * per evitare di rifare la chiamata di rete a ogni avvio se il token non è
     * cambiato (vedi push/DeviceRegistration.kt.maybeRegister).
     */
    val lastRegisteredFcmToken: Flow<String?> = dataStore.data.map { it[Keys.LAST_REGISTERED_FCM_TOKEN] }

    suspend fun setSelectedClub(club: String) {
        dataStore.edit { it[Keys.SELECTED_CLUB_CODE] = club }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLastRegisteredFcmToken(token: String?) {
        dataStore.edit {
            if (token == null) it.remove(Keys.LAST_REGISTERED_FCM_TOKEN) else it[Keys.LAST_REGISTERED_FCM_TOKEN] = token
        }
    }
}
