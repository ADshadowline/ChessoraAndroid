package org.chessora.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/** Un solo campo email, sempre lo stesso messaggio di esito (vedi ForgotPasswordViewModel) -
 * il reset vero e proprio avviene su una pagina web (link nell'email), non nell'app. */
@Composable
fun ForgotPasswordScreen() {
    val viewModel = chessoraViewModel { app -> ForgotPasswordViewModel(app.repository) }
    val step by viewModel.step.collectAsState()
    var email by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.forgot_password_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.forgot_password_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_email_field_label)) },
            singleLine = true,
            enabled = step !is ForgotPasswordStep.Sent,
            modifier = Modifier.fillMaxWidth(),
        )
        when (step) {
            is ForgotPasswordStep.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
            is ForgotPasswordStep.Sent -> Text(
                stringResource(R.string.forgot_password_sent),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp),
            )
            is ForgotPasswordStep.NetworkError -> Text(
                stringResource(R.string.forgot_password_network_error),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 20.dp),
            )
            else -> Unit
        }
        if (step !is ForgotPasswordStep.Sent) {
            Button(
                onClick = { viewModel.submit(email) },
                enabled = step !is ForgotPasswordStep.Loading,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) { Text(stringResource(R.string.forgot_password_submit)) }
        }
    }
}
