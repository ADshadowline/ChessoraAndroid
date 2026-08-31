package org.chessora.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
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

    lateinit var repository: ChessoraRepository
        private set

    override fun onCreate() {
        super.onCreate()
        clubPreferences = ClubPreferences(this)
        repository = ChessoraRepository(NetworkModule.api)
        createNotificationChannel()
    }

    /**
     * Il canale di notifica va creato prima che arrivi il primo messaggio FCM
     * (richiesto da Android 8+, che è comunque il nostro minSdk): lo facciamo
     * qui invece che dentro ChessoraFirebaseMessagingService cosi' esiste già
     * anche se l'app non ha mai ricevuto un push, utile per un domani se si
     * aggiungono impostazioni di canale nelle Impostazioni di sistema.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.default_notification_channel_description)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
