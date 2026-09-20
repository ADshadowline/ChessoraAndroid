package org.chessora.app.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Schermata di accesso - primo punto obbligato dell'app se non si è già loggati (vedi
 * ChessoraNavHost SPLASH): tre modi per accedere, nessuna consultazione anonima. La
 * creazione di un nuovo account "amatoriale" senza alcun ID FIDE avviene solo come
 * sotto-passo di un login Google (CompleteProfileScreen) o ID FIDE (LoginFideScreen),
 * non da qui - non esiste un punto di ingresso "Registrati" separato in questa versione.
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNeedsProfile: () -> Unit,
    onLoginEmail: () -> Unit,
    onLoginFide: () -> Unit,
) {
    val viewModel = chessoraViewModel { app -> LoginViewModel(app.repository, app.clubPreferences, app.authPreferences) }
    val step by viewModel.step.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.google_signin_web_client_id)

    LaunchedEffect(step) {
        when (step) {
            is LoginStep.LoggedIn -> onLoggedIn()
            is LoginStep.NeedsProfile -> onNeedsProfile()
            else -> Unit
        }
    }

    LaunchedEffect(step) {
        val current = step
        if (current is LoginStep.Error) {
            Toast.makeText(context, current.message, Toast.LENGTH_LONG).show()
            viewModel.resetError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (step is LoginStep.Loading) {
            CircularProgressIndicator()
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
                )
                Button(
                    onClick = {
                        scope.launch {
                            val idToken = GoogleSignInHelper.signIn(context, webClientId)
                            if (idToken != null) viewModel.loginWithGoogle(idToken) else viewModel.googleSignInFailed()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.login_google)) }
                OutlinedButton(onClick = onLoginFide, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(stringResource(R.string.login_fide))
                }
                OutlinedButton(onClick = onLoginEmail, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(stringResource(R.string.login_email))
                }
            }
        }
    }
}
