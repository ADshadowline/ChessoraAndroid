package org.chessora.app.push

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.chessora.app.ChessoraApplication
import org.chessora.app.MainActivity
import org.chessora.app.R

/**
 * Riceve i messaggi push inviati dal super-amministratore (pagina "Notifiche
 * App", vedi docs/android-app-spec.md §5). Il payload arriva come messaggio
 * "Data" puro (non "Notification": vedi Chessora.Infrastructure/Push/
 * FirebasePushNotificationSender.cs nel repository server) apposta - un
 * messaggio "Notification" verrebbe mostrato automaticamente dal sistema
 * quando l'app è in background/chiusa usando SEMPRE il canale di default,
 * ignorando la priorità scelta dall'admin. Con un data message,
 * [onMessageReceived] viene invocato in ogni stato dell'app (foreground,
 * background, chiusa) e sceglie qui il canale giusto in base al campo
 * `data.priority` ("high"/"normal") inviato dal server.
 *
 * Riporta anche indietro al server "consegnato" (appena ricevuto qui - prova
 * reale, a differenza del solo esito lato FCM al momento dell'invio) e "letto"
 * (quando l'utente tocca la notifica - vedi [EXTRA_ID_MESSAGE], letto da
 * MainActivity), per la vista admin "chi l'ha ricevuto/letto".
 *
 * Nessun deep-link verso una schermata specifica in questa prima versione: il
 * messaggio è solo titolo + testo libero, non porta un riferimento a un
 * torneo/news specifico - toccare la notifica apre semplicemente l'app sulla
 * Home.
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

        if (message.data["type"] == "tournament_round") {
            val idTournamentRegistration = message.data["idTournamentRegistration"]?.toIntOrNull() ?: return
            val roundNumber = message.data["roundNumber"]?.toIntOrNull() ?: return
            val tournamentName = message.data["tournamentName"] ?: getString(R.string.app_name)
            if (AppForegroundTracker.isForeground) {
                // App aperta in questo momento: niente notifica di sistema, la schermata a
                // tutto schermo (montata in ChessoraNavHost) la mostra subito da sola.
                TournamentRoundEvents.emit(RoundPublishedEvent(idTournamentRegistration, roundNumber, tournamentName))
            } else {
                showTournamentRoundNotification(idTournamentRegistration, roundNumber, tournamentName)
            }
            return
        }

        if (message.data["type"] == "chat") {
            val idConversation = message.data["idConversation"]?.toIntOrNull() ?: return
            val senderName = message.data["senderName"] ?: getString(R.string.app_name)
            val body = message.data["body"] ?: return
            showChatNotification(idConversation, senderName, body)
            return
        }

        val title = message.data["title"] ?: getString(R.string.app_name)
        val body = message.data["body"] ?: return
        val highPriority = message.data["priority"] == "high"
        val idMessage = message.data["idMessage"]?.toIntOrNull()

        if (idMessage != null) {
            val app = applicationContext as ChessoraApplication
            // Stessa ragione di onNewToken sopra: fire-and-forget ma bloccante,
            // per non rischiare che il servizio venga distrutto a metà chiamata.
            runBlocking(Dispatchers.IO) {
                try {
                    val token = FirebaseMessaging.getInstance().token.await()
                    app.repository.reportNotificationDelivered(idMessage, token)
                } catch (e: Exception) {
                    // Nessuna connessione/Play Services assente: non blocca comunque
                    // la visualizzazione della notifica.
                }
            }
        }

        showNotification(title, body, highPriority, idMessage)
    }

    private fun showNotification(title: String, body: String, highPriority: Boolean, idMessage: Int?) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (idMessage != null) putExtra(EXTRA_ID_MESSAGE, idMessage)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = if (highPriority) {
            getString(R.string.urgent_notification_channel_id)
        } else {
            getString(R.string.default_notification_channel_id)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
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

    /** Notifica di un nuovo messaggio in ui/messaging/ - canale "urgente" (stessa
     * aspettativa "tipo WhatsApp" di consegna rapida), tocco apre la conversazione
     * direttamente (vedi EXTRA_ID_CONVERSATION, letto da ChessoraNavHost). */
    private fun showChatNotification(idConversation: Int, senderName: String, body: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_ID_CONVERSATION, idConversation)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.urgent_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(idConversation, notification)
        } catch (e: SecurityException) {
            // Permesso negato dall'utente: nessuna notifica, nessun crash.
        }
    }

    /** Notifica di un turno pubblicato mentre l'app NON è in primo piano - canale
     * "urgente" (stesso spirito di showChatNotification: l'utente deve accorgersene in
     * fretta), tocco apre direttamente gli abbinamenti di quel torneo (vedi
     * EXTRA_ID_TOURNAMENT, letto da ChessoraNavHost). Ad app aperta questo ramo non
     * viene mai chiamato: vedi il branch "tournament_round" sopra. */
    private fun showTournamentRoundNotification(idTournamentRegistration: Int, roundNumber: Int, tournamentName: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_ID_TOURNAMENT, idTournamentRegistration)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.urgent_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(tournamentName)
            .setContentText(getString(R.string.tournament_round_published_notification, roundNumber))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(idTournamentRegistration, notification)
        } catch (e: SecurityException) {
            // Permesso negato dall'utente: nessuna notifica, nessun crash.
        }
    }

    companion object {
        /** Extra sull'Intent che apre MainActivity al tocco della notifica - letto
         * lì per segnalare "letto" al server (vedi MainActivity.kt). */
        const val EXTRA_ID_MESSAGE = "push_id_message"

        /** Extra sull'Intent che apre MainActivity al tocco di una notifica di chat -
         * letto da ChessoraNavHost per aprire direttamente quella conversazione. */
        const val EXTRA_ID_CONVERSATION = "push_id_conversation"

        /** Extra sull'Intent che apre MainActivity al tocco di una notifica di turno
         * pubblicato (app NON in primo piano al momento dell'invio) - letto da
         * ChessoraNavHost per aprire direttamente gli abbinamenti di quel torneo. */
        const val EXTRA_ID_TOURNAMENT = "push_id_tournament"
    }
}
