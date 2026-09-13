package org.chessora.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.chessora.app.ui.navigation.ChessoraNavHost
import org.chessora.app.ui.theme.ChessoraTheme
import org.chessora.app.ui.update.UpdateAvailableDialog

/**
 * Unica Activity dell'app (Compose gestisce tutta la navigazione interna via
 * ChessoraNavHost - vedi ui/navigation/ChessoraNavHost.kt): coerente con
 * un'app "di sola consultazione" senza flussi che richiedano più task/Activity
 * separate (docs/android-app-spec.md §1).
 */
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Se negato, semplicemente non arriveranno notifiche - nessuna azione ulteriore. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askNotificationPermissionIfNeeded()

        setContent {
            ChessoraTheme {
                ChessoraNavHost()
                UpdateAvailableDialog()
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
