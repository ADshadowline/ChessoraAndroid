package org.chessora.app.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.chessora.app.R
import org.chessora.app.data.remote.AppUpdateChecker
import org.chessora.app.data.remote.dto.AppVersionInfo

/**
 * Controllo di aggiornamento "ogni tanto": una volta per apertura dell'app
 * (non un job periodico in background - l'app è di sola consultazione,
 * aprirla di tanto in tanto è già l'occasione naturale per il controllo).
 * Va piazzato una sola volta, in cima all'albero Compose (vedi MainActivity),
 * non dentro una singola schermata - altrimenti si ripeterebbe ogni volta che
 * si torna su quella schermata invece che una volta per sessione.
 */
@Composable
fun UpdateAvailableDialog() {
    var updateInfo by remember { mutableStateOf<AppVersionInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        updateInfo = AppUpdateChecker.checkForUpdate()
    }

    val info = updateInfo
    if (info != null && !dismissed) {
        AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Text(
                    stringResource(R.string.update_available_body, info.versionName) +
                        (info.notes?.takeIf { it.isNotBlank() }?.let { "\n\n$it" } ?: "") +
                        "\n\n" + stringResource(R.string.update_available_question),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dismissed = true
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadsUrl)))
                }) { Text(stringResource(R.string.update_available_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) { Text(stringResource(R.string.update_available_dismiss)) }
            },
        )
    }
}
