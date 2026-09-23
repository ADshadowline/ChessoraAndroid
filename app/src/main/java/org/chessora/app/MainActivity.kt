package org.chessora.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.chessora.app.push.ChessoraFirebaseMessagingService
import org.chessora.app.ui.navigation.ChessoraNavHost
import org.chessora.app.ui.theme.ChessoraTheme
import org.chessora.app.ui.update.UpdateAvailableDialog

/**
 * Unica Activity dell'app (Compose gestisce tutta la navigazione interna via
 * ChessoraNavHost - vedi ui/navigation/ChessoraNavHost.kt): coerente con
 * un'app "di sola consultazione" senza flussi che richiedano più task/Activity
 * separate (docs/android-app-spec.md §1).
 *
 * AppCompatActivity (non un semplice ComponentActivity) SOLO per il cambio lingua
 * per-app (ui/settings/, AppCompatDelegate.setApplicationLocales): è l'unico modo
 * documentato per far ricreare automaticamente l'Activity con la nuova lingua su
 * ogni versione di Android supportata (su un ComponentActivity puro la preferenza
 * verrebbe salvata ma mai applicata alla sessione corrente sotto Android 13). Non
 * introduce nessun widget/tema AppCompat "vero": tutta la UI resta Compose, vedi
 * il tema "ponte" Theme.Chessora in res/values/themes.xml (ora Theme.AppCompat.*,
 * richiesto da AppCompatActivity, invariato nella sostanza per Compose).
 */
class MainActivity : AppCompatActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Se negato, semplicemente non arriveranno notifiche - nessuna azione ulteriore. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermissionIfNeeded()
        reportNotificationReadIfNeeded()

        val pendingConversationId = intent?.getIntExtra(ChessoraFirebaseMessagingService.EXTRA_ID_CONVERSATION, -1)
            ?.takeIf { it >= 0 }

        setContent {
            ChessoraTheme {
                ChessoraNavHost(pendingConversationId = pendingConversationId)
                UpdateAvailableDialog()
            }
        }
    }

    /**
     * Se l'Activity è stata aperta toccando una notifica push (vedi
     * ChessoraFirebaseMessagingService.showNotification, che mette l'id del
     * messaggio in questo extra), segnala al server che l'utente l'ha letta -
     * per la vista admin "chi l'ha ricevuto/letto". FLAG_ACTIVITY_CLEAR_TASK
     * nell'Intent della notifica garantisce che onCreate giri sempre da capo a
     * ogni tocco, quindi qui non serve nessuna guardia contro segnalazioni
     * duplicate su ricreazioni della stessa Activity.
     */
    private fun reportNotificationReadIfNeeded() {
        val idMessage = intent?.getIntExtra(ChessoraFirebaseMessagingService.EXTRA_ID_MESSAGE, -1) ?: -1
        if (idMessage < 0) return

        val app = application as ChessoraApplication
        lifecycleScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                app.repository.reportNotificationRead(idMessage, token)
            } catch (e: Exception) {
                // Nessuna connessione/Play Services assente: non è un problema per
                // il resto dell'app, semplicemente non risulterà "letta".
            }
        }
    }

    /**
     * POST_NOTIFICATIONS va richiesto a runtime solo da Android 13 (API 33) in
     * poi - sotto quella versione il permesso è concesso automaticamente
     * all'installazione (comportamento di sistema, nessun codice necessario).
     */
    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!alreadyGranted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
