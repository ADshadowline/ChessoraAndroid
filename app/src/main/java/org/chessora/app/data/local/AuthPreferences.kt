package org.chessora.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "chessora_auth_prefs")

/**
 * Sessione del nuovo sistema di login (Google/ID FIDE/email+password) - DataStore
 * SEPARATO da chessora_prefs (vedi ClubPreferences) apposta: contiene l'unico dato
 * davvero sensibile dell'app (il token JWT), cosi' un logout può azzerare solo questo
 * file senza toccare le altre preferenze (sfondi, ordine icone, ecc.).
 *
 * "Chi sono" in senso più ampio (idPlayer/nome) resta deliberatamente su
 * ClubPreferences (identifiedPlayerId/authenticatedEmail/isAuthenticated) - popolati
 * qui SUBITO dopo un login riuscito (vedi ui/auth/AuthSessionPersister.kt) invece che
 * dal vecchio flusso di identificazione debole: questo evita di dover riscrivere
 * SessionViewModel/TournamentDetailViewModel/RegistrationsViewModel, che continuano a
 * leggere esattamente quei campi.
 */
class AuthPreferences(context: Context) {
    private val dataStore = context.authDataStore

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val ID_FIDE = intPreferencesKey("id_fide")
        val ID_CLUB = intPreferencesKey("id_club")
        val CLUB_PUBLIC_CODE = stringPreferencesKey("club_public_code")
        val EMAIL_CONFIRMED = booleanPreferencesKey("email_confirmed")
        val PROFILE_COMPLETE = booleanPreferencesKey("profile_complete")
    }

    val accessToken: Flow<String?> = dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val isLoggedIn: Flow<Boolean> = accessToken.map { !it.isNullOrBlank() }
    val idFide: Flow<Int?> = dataStore.data.map { it[Keys.ID_FIDE] }
    val idClub: Flow<Int?> = dataStore.data.map { it[Keys.ID_CLUB] }
    val clubPublicCode: Flow<String?> = dataStore.data.map { it[Keys.CLUB_PUBLIC_CODE] }
    val emailConfirmed: Flow<Boolean> = dataStore.data.map { it[Keys.EMAIL_CONFIRMED] ?: false }
    val profileComplete: Flow<Boolean> = dataStore.data.map { it[Keys.PROFILE_COMPLETE] ?: false }

    suspend fun saveSession(
        accessToken: String,
        idFide: Int?,
        idClub: Int?,
        clubPublicCode: String?,
        emailConfirmed: Boolean,
        profileComplete: Boolean,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            if (idFide != null) prefs[Keys.ID_FIDE] = idFide else prefs.remove(Keys.ID_FIDE)
            if (idClub != null) prefs[Keys.ID_CLUB] = idClub else prefs.remove(Keys.ID_CLUB)
            if (clubPublicCode != null) prefs[Keys.CLUB_PUBLIC_CODE] = clubPublicCode else prefs.remove(Keys.CLUB_PUBLIC_CODE)
            prefs[Keys.EMAIL_CONFIRMED] = emailConfirmed
            prefs[Keys.PROFILE_COMPLETE] = profileComplete
        }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }
}
