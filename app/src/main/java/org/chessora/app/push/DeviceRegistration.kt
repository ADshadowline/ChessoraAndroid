package org.chessora.app.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.chessora.app.BuildConfig
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.repository.ChessoraRepository

/**
 * Registra il token FCM corrente su POST /api/devices/register (vedi
 * docs/android-app-spec.md §5, punto 3). Chiamata da quattro punti (tutti
 * innocui da ripetere: il server fa un upsert per valore di token, non serve
 * nessuna logica di "salta se invariato" - vedi ChessoraRepository.registerDevice):
 * 1. All'avvio dell'app (MainActivity), con il circolo eventualmente già scelto.
 * 2. Quando l'utente sceglie/cambia circolo (ui/onboarding e ui/settings).
 * 3. Quando Firebase invoca onNewToken (ChessoraFirebaseMessagingService) - i
 *    token FCM possono cambiare nel tempo indipendentemente dall'app.
 * 4. Appena l'utente si identifica con successo (ui/identity/IdentityViewModel) -
 *    cosi' il dispositivo risulta subito associato al socio invece di aspettare
 *    il prossimo riavvio, per la vista admin "Dispositivi" (Chessora.Api).
 */
object DeviceRegistration {

    suspend fun registerCurrentToken(
        repository: ChessoraRepository,
        clubPreferences: ClubPreferences,
        club: String?,
    ) {
        // "disattivarle lato app equivale a non registrare/cancellare il token"
        // (docs/android-app-spec.md §7.9): se l'utente ha spento il toggle nelle
        // Impostazioni, semplicemente non chiamiamo mai POST /api/devices/register -
        // non esiste un endpoint di "de-registrazione" lato server, quindi questo
        // è l'unico modo lato client di rispettare la preferenza.
        if (!clubPreferences.notificationsEnabled.first()) return

        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            // Puo' fallire se Google Play Services non è disponibile (es. emulatore
            // senza Play Store, o dispositivo Huawei senza GMS): non è un errore
            // fatale per il resto dell'app, semplicemente questa sessione non
            // riceverà notifiche push.
            return
        }

        // POST /api/devices/register è l'unico endpoint pubblico che vuole ancora
        // l'IdClub numerico (nel body, non in query string): lo risolviamo qui al
        // volo dal publicCode invece di persisterlo mai lato client. Se il circolo
        // non risolve (rete assente, codice non più valido) semplicemente non
        // registriamo l'idClub - come oggi per "nessun circolo scelto", il token
        // resta comunque valido per notifiche non legate a un circolo specifico.
        val idClub = if (club != null && club != ClubPreferences.PLATFORM_CLUB_CODE) {
            repository.resolveClubByCode(club).getOrNull()
        } else {
            null
        }
        val idPlayer = clubPreferences.identifiedPlayerId.first()

        repository.registerDevice(
            token = token, idClub = idClub, idPlayer = idPlayer,
            appVersionName = BuildConfig.VERSION_NAME, appVersionCode = BuildConfig.VERSION_CODE,
        ).onSuccess {
            clubPreferences.setLastRegisteredFcmToken(token)
        }
    }
}
