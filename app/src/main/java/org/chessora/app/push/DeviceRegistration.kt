package org.chessora.app.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.repository.ChessoraRepository

/**
 * Registra il token FCM corrente su POST /api/devices/register (vedi
 * docs/android-app-spec.md §5, punto 3). Chiamata da tre punti (tutti
 * innocui da ripetere: il server fa un upsert per valore di token, non serve
 * nessuna logica di "salta se invariato" - vedi ChessoraRepository.registerDevice):
 * 1. All'avvio dell'app (MainActivity), con l'idClub eventualmente già scelto.
 * 2. Quando l'utente sceglie/cambia circolo (ui/onboarding e ui/settings).
 * 3. Quando Firebase invoca onNewToken (ChessoraFirebaseMessagingService) - i
 *    token FCM possono cambiare nel tempo indipendentemente dall'app.
 */
object DeviceRegistration {

    suspend fun registerCurrentToken(
        repository: ChessoraRepository,
        clubPreferences: ClubPreferences,
        idClub: Int?,
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

        repository.registerDevice(token = token, idClub = idClub).onSuccess {
            clubPreferences.setLastRegisteredFcmToken(token)
        }
    }
}
