package org.chessora.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.chessora.app.ChessoraApplication
import org.chessora.app.R

/** Mostrata dopo una registrazione via ID FIDE (RegisterResponse.requiresEmailConfirmation
 * è sempre true, vedi LoginFideViewModel.register) - il link di conferma apre una pagina
 * web (PlayerAuthController.ConfirmEmail), non un deep-link nell'app: da qui l'utente
 * torna semplicemente al login dopo aver confermato altrove. Nessun ViewModel: solo
 * un'azione fire-and-forget (reinvio email), niente stato da sopravvivere a una rotazione. */
@Composable
fun EmailPendingScreen(email: String, onBackToLogin: () -> Unit) {
    val context = LocalContext.current
    val repository = (context.applicationContext as ChessoraApplication).repository
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.MarkEmailUnread,
            contentDescription = null,
            modifier = Modifier.padding(top = 48.dp, bottom = 24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.email_pending_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.email_pending_body, email),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        OutlinedButton(onClick = {
            scope.launch {
                repository.resendConfirmation(email)
                Toast.makeText(context, context.getString(R.string.login_confirmation_resent), Toast.LENGTH_LONG).show()
            }
        }) { Text(stringResource(R.string.login_resend_confirmation)) }
        OutlinedButton(onClick = onBackToLogin, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.email_pending_back_to_login))
        }
    }
}
