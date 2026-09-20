package org.chessora.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.AuthSession
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.repository.ChessoraRepository

/**
 * Application class: crea una sola volta le dipendenze condivise da tutta l'app
 * (niente Hilt/Dagger - vedi data/remote/NetworkModule.kt per la motivazione) e
 * le espone come proprietà pubbliche. I ViewModel le recuperano tramite
 * [ChessoraViewModelFactory] (vedi ui/common/ChessoraViewModelFactory.kt),
 * risalendo a questa Application da `LocalContext.current.applicationContext`.
 */
class ChessoraApplication : Application() {

    lateinit var clubPreferences: ClubPreferences
        private set

    lateinit var authPreferences: AuthPreferences
        private set

    lateinit var repository: ChessoraRepository
        private set

    override fun onCreate() {
        super.onCreate()
        clubPreferences = ClubPreferences(this)
        authPreferences = AuthPreferences(this)
        repository = ChessoraRepository(NetworkModule.api)
        // Lettura locale bloccante ma rapida (DataStore su file, nessuna rete): serve
        // che AuthSession.accessToken sia già valorizzato PRIMA della primissima
        // chiamata di rete di SessionViewModel/SplashScreen, che parte quasi subito
        // dopo onCreate - un populamento asincrono lascerebbe una finestra in cui
        // l'interceptor (data/remote/NetworkModule.kt) non allegherebbe il Bearer.
        AuthSession.accessToken = runBlocking { authPreferences.accessToken.first() }
        createNotificationChannel()
    }

    /**
     * I canali di notifica vanno creati prima che arrivi il primo messaggio FCM
     * (richiesto da Android 8+, che è comunque il nostro minSdk): lo facciamo
     * qui invece che dentro ChessoraFirebaseMessagingService cosi' esistono già
     * anche se l'app non ha mai ricevuto un push.
     *
     * Due canali, non uno solo: dalla pagina super-admin "Notifiche App" si può
     * scegliere la priorità del messaggio (vedi ChessoraFirebaseMessagingService,
     * che sceglie il canale in base al campo "priority" del messaggio):
     * - [default_notification_channel_id] (IMPORTANCE_DEFAULT): finisce solo
     *   nella tendina delle notifiche, come oggi.
     * - [urgent_notification_channel_id] (IMPORTANCE_HIGH): banner "heads-up" in
     *   alto allo schermo con suono, anche sopra un'altra app aperta - stesso
     *   comportamento delle notifiche urgenti delle app bancarie.
     * L'importanza di un canale, una volta creato sul dispositivo di un utente,
     * non può più essere cambiata da un aggiornamento dell'app: usare due ID di
     * canale distinti fin da subito evita di dover reinstallare l'app in futuro
     * per introdurre la priorità alta.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val defaultChannel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.default_notification_channel_description)
        }
        manager.createNotificationChannel(defaultChannel)

        val urgentChannel = NotificationChannel(
            getString(R.string.urgent_notification_channel_id),
            getString(R.string.urgent_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.urgent_notification_channel_description)
        }
        manager.createNotificationChannel(urgentChannel)
    }
}
