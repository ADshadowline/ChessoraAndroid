package org.chessora.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/** Login con email + password, oppure "Crea nuovo utente" (email + password, account
 * attivo solo dopo la conferma email) - vedi LoginEmailViewModel per la logica. Dopo il
 * primo accesso di un utente appena creato, [onNeedsProfile] porta a
 * CompleteProfileScreen (ID FIDE / socio di un circolo / utente della piattaforma). */
@Composable
fun LoginEmailScreen(
    onLoggedIn: () -> Unit,
    onNeedsProfile: () -> Unit,
    onRegistered: (email: String) -> Unit,
    onForgotPassword: () -> Unit,
) {
    val viewModel = chessoraViewModel { app -> LoginEmailViewModel(app.repository, app.clubPreferences, app.authPreferences) }
    val step by viewModel.step.collectAsState()
    val context = LocalContext.current
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(step) {
        when (val current = step) {
            is LoginEmailStep.LoggedIn -> onLoggedIn()
            is LoginEmailStep.NeedsProfile -> onNeedsProfile()
            is LoginEmailStep.Registered -> onRegistered(current.email)
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text(
            stringResource(if (isRegistering) R.string.register_email_title else R.string.login_email_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_email_field_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = stringResource(R.string.login_password_label),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        if (isRegistering) {
            PasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = stringResource(R.string.register_confirm_password_label),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Text(
                stringResource(R.string.register_password_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

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
            onClick = {
                if (isRegistering) viewModel.register(email, password, confirmPassword) else viewModel.login(email, password)
            },
            enabled = step !is LoginEmailStep.Loading,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) { Text(stringResource(if (isRegistering) R.string.register_submit else R.string.login_submit)) }

        if (!isRegistering) {
            TextButton(onClick = onForgotPassword, modifier = Modifier.padding(top = 8.dp)) {
                Text(stringResource(R.string.login_forgot_password))
            }
        }
        TextButton(
            onClick = {
                isRegistering = !isRegistering
                confirmPassword = ""
                viewModel.clearMessage()
            },
            modifier = Modifier.padding(top = if (isRegistering) 8.dp else 0.dp),
        ) {
            Text(stringResource(if (isRegistering) R.string.login_have_account else R.string.login_create_account))
        }
    }
}
