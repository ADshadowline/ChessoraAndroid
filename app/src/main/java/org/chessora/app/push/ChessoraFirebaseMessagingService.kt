package org.chessora.app.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.chessora.app.ChessoraApplication
import org.chessora.app.MainActivity
import org.chessora.app.R

/**
 * Riceve i messaggi push inviati dal super-amministratore (pagina "Notifiche
 * App", vedi docs/android-app-spec.md §5). Il payload arriva come notifica FCM
 * "Notification" (non "Data": vedi Chessora.Infrastructure/Push/
 * FirebasePushNotificationSender.cs nel repository server, che imposta
 * `Notification = new Notification { Title, Body }`), quindi Android la mostra
 * automaticamente da solo quando l'app è in background o chiusa - [onMessageReceived]
 * viene invocato SOLO quando l'app è in foreground, unico caso in cui serve
 * costruire la notifica a mano (altrimenti l'utente in foreground non vedrebbe
 * nulla, dato che il sistema non mostra le notifiche "Notification" quando la
 * tua stessa app ha il focus).
 *
 * Nessun deep-link verso una schermata specifica in questa prima versione: il
 * messaggio è solo titolo + testo libero, non porta un riferimento a un
 * torneo/news specifico (vedi docs/android-app-spec.md §9 "Domande aperte" nel
 * repository server) - toccare la notifica apre semplicemente l'app sulla Home.
 */
class ChessoraFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as ChessoraApplication
        // onNewToken non è una funzione sospesa: il servizio potrebbe essere
        // distrutto subito dopo il ritorno di questo metodo, quindi usiamo
        // runBlocking invece di un CoroutineScope "fire and forget" che
        // rischierebbe di essere cancellato a metà. La chiamata di rete è
        // comunque breve (un singolo POST).
        runBlocking(Dispatchers.IO) {
            val club = app.clubPreferences.selectedClub.firstOrNull()
            DeviceRegistration.registerCurrentToken(app.repository, app.clubPreferences, club)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: getString(R.string.app_name)
        val body = message.notification?.body ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // NotificationManagerCompat.notify richiede il permesso POST_NOTIFICATIONS
        // (Android 13+, già dichiarato in AndroidManifest.xml) - se l'utente lo
        // ha negato, la chiamata lancia SecurityException: la intercettiamo
        // invece di far crashare l'app, semplicemente la notifica non appare.
        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            // Permesso negato dall'utente: nessuna notifica, nessun crash.
        }
    }
}
