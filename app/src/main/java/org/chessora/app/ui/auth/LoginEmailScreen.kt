package org.chessora.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/** Login con email o ID FIDE + password (vedi LoginEmailViewModel per la logica). */
@Composable
fun LoginEmailScreen(onLoggedIn: () -> Unit, onForgotPassword: () -> Unit) {
    val viewModel = chessoraViewModel { app -> LoginEmailViewModel(app.repository, app.clubPreferences, app.authPreferences) }
    val step by viewModel.step.collectAsState()
    val context = LocalContext.current
    var emailOrIdFide by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(step) {
        if (step is LoginEmailStep.LoggedIn) onLoggedIn()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.login_email_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = emailOrIdFide,
            onValueChange = { emailOrIdFide = it },
            label = { Text(stringResource(R.string.login_email_field_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.login_password_label),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        when (val current = step) {
            is LoginEmailStep.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
            is LoginEmailStep.Error -> {
                // Niente reset automatico qui: farlo subito (come prima) cancellava il
                // messaggio nello stesso frame in cui appariva, prima che l'utente potesse
                // leggerlo - resta a schermo finché non si ritenta il login (che lo
                // sovrascrive comunque con Loading).
                Text(current.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            }
            is LoginEmailStep.EmailNotConfirmed -> {
                Text(stringResource(R.string.login_email_not_confirmed), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                TextButton(onClick = {
                    viewModel.resendConfirmation(current.email)
                    Toast.makeText(context, context.getString(R.string.login_confirmation_resent), Toast.LENGTH_LONG).show()
                }) { Text(stringResource(R.string.login_resend_confirmation)) }
            }
            else -> Unit
        }

        Button(
            onClick = { viewModel.login(emailOrIdFide, password) },
            enabled = step !is LoginEmailStep.Loading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) { Text(stringResource(R.string.login_submit)) }

        TextButton(onClick = onForgotPassword, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.login_forgot_password))
        }
    }
}
