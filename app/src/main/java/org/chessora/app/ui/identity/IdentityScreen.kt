package org.chessora.app.ui.identity

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Schermata di identificazione facoltativa (vedi il piano approvato): mostrata
 * una sola volta dopo la scelta del circolo (ChessoraNavHost.kt), o richiamata a
 * mano da Impostazioni. Nessun dato riservato viene sbloccato: serve solo a
 * tenere allineati email/telefono del socio in anagrafica (PlayerContacts lato
 * server).
 */
@Composable
fun IdentityScreen(club: String?, clubName: String? = null, onDone: () -> Unit) {
    val viewModel = chessoraViewModel { app -> IdentityViewModel(app.repository, app.clubPreferences) }
    val step by viewModel.step.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.google_signin_web_client_id)

    // Solo nel flusso senza circolo (MembershipQuestionScreen: "non sono iscritto a
    // nessun circolo"): campo facoltativo, inviato con email/telefono per la
    // ricerca a livello nazionale (vedi IdentityViewModel.identify).
    var idFide by remember { mutableStateOf("") }

    LaunchedEffect(club) { viewModel.loadClub(club) }

    // Valorizzata solo mentre è in corso la richiesta del permesso READ_PHONE_NUMBERS
    // subito DOPO un login Google riuscito (vedi onGoogle sotto e
    // SimPhoneNumberReader.kt): niente popup applicativo, se il telefono ha una
    // sola SIM il numero viene preso in automatico appena il permesso è concesso.
    var pendingGoogleEmail by remember { mutableStateOf<String?>(null) }

    // Nome dell'account Google (se disponibile), inviato insieme a email/telefono
    // come ultimo fallback lato server per un confronto per nome (vedi
    // PlayerIdentityService.IdentifyAsync) - mai usato per mostrare un elenco soci
    // sul client.
    var pendingGoogleDisplayName by remember { mutableStateOf<String?>(null) }

    // true mentre è mostrato il dialog "perché ti chiediamo il numero" (vedi sotto):
    // spiega lo scopo PRIMA del popup di sistema Android, che per il gruppo di
    // permessi "Telefono" mostra sempre un testo generico ("gestire le telefonate")
    // che l'app non può personalizzare - il nostro dialog dà il contesto reale.
    var showPhoneRationale by remember { mutableStateOf(false) }

    // Usato solo dal tasto esplicito "Usa il numero di telefono" (onPhone sotto):
    // qui il popup nativo di Google (Phone Number Hint) è corretto, perché
    // l'utente ha chiesto lui stesso di usare il numero.
    val phoneHintLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { activityResult ->
        val act = activity
        val phone = if (act != null) PhoneHintHelper.extractPhoneNumber(act, activityResult.data) else null
        viewModel.identify(pendingGoogleEmail, phone, pendingGoogleDisplayName, idFide.ifBlank { null })
        pendingGoogleEmail = null
        pendingGoogleDisplayName = null
    }

    fun requestPhoneHint(fallbackEmail: String?) {
        scope.launch {
            val act = activity
            val sender = if (act != null) PhoneHintHelper.buildHintIntentSender(act) else null
            if (sender != null) {
                pendingGoogleEmail = fallbackEmail
                phoneHintLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                // Nessun numero disponibile da proporre (Play Services assente/nessuna
                // SIM): procede con quello che si ha già (solo email, se veniamo dal
                // login Google) invece di bloccare l'utente.
                viewModel.identify(fallbackEmail, null, idFide = idFide.ifBlank { null })
            }
        }
    }

    // Dopo un login Google, prova a leggere in automatico e senza alcun popup
    // applicativo il numero della SIM (se il permesso è già concesso e il
    // telefono ne ha uno solo), invece di mostrare il Phone Number Hint di
    // Google (che richiederebbe sempre un tocco di conferma).
    val phonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val phone = if (granted) SimPhoneNumberReader.read(context) else null
        viewModel.identify(pendingGoogleEmail, phone, pendingGoogleDisplayName, idFide.ifBlank { null })
        pendingGoogleEmail = null
        pendingGoogleDisplayName = null
    }

    fun identifyWithGoogleEmail(email: String, displayName: String?) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_NUMBERS,
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.identify(email, SimPhoneNumberReader.read(context), displayName, idFide.ifBlank { null })
        } else {
            // Prima il nostro dialog con lo scopo reale, solo dopo (se l'utente
            // accetta) il popup di sistema Android per il permesso.
            pendingGoogleEmail = email
            pendingGoogleDisplayName = displayName
            showPhoneRationale = true
        }
    }

    LaunchedEffect(step) {
        if (step is IdentityStep.Done) {
            delay(900)
            onDone()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        when (val current = step) {
            is IdentityStep.Choosing -> ChoosingContent(
                showIdFideField = club == null,
                idFide = idFide,
                onIdFideChange = { idFide = it },
                onGoogle = {
                    scope.launch {
                        val account = GoogleSignInHelper.signIn(context, webClientId)
                        if (account != null) {
                            // Anche dopo un login Google, prova a registrare il numero di
                            // telefono: letto in automatico dalla SIM, senza alcun popup
                            // (vedi identifyWithGoogleEmail sopra).
                            identifyWithGoogleEmail(account.email, account.displayName)
                        }
                    }
                },
                onPhone = { requestPhoneHint(fallbackEmail = null) },
            )
            is IdentityStep.Loading -> CircularProgressIndicator()
            is IdentityStep.Done -> Text(
                text = current.matchedName?.let { stringResource(R.string.identity_welcome, it) }
                    ?: stringResource(R.string.identity_not_identified),
                style = MaterialTheme.typography.titleMedium,
            )
            is IdentityStep.Error -> ErrorContent(message = current.message, onRetry = { viewModel.retry() })
        }
    }

    if (showPhoneRationale) {
        AlertDialog(
            onDismissRequest = {
                showPhoneRationale = false
                viewModel.identify(pendingGoogleEmail, null, pendingGoogleDisplayName, idFide.ifBlank { null })
                pendingGoogleEmail = null
                pendingGoogleDisplayName = null
            },
            title = { Text(stringResource(R.string.identity_phone_rationale_title)) },
            text = {
                Text(
                    clubName?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(R.string.identity_phone_rationale_body_club, it) }
                        ?: stringResource(R.string.identity_phone_rationale_body_generic),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPhoneRationale = false
                    phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_NUMBERS)
                }) {
                    Text(stringResource(R.string.identity_phone_rationale_allow))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhoneRationale = false
                    viewModel.identify(pendingGoogleEmail, null, pendingGoogleDisplayName, idFide.ifBlank { null })
                    pendingGoogleEmail = null
                    pendingGoogleDisplayName = null
                }) {
                    Text(stringResource(R.string.identity_phone_rationale_deny))
                }
            },
        )
    }
}

@Composable
private fun ChoosingContent(
    showIdFideField: Boolean,
    idFide: String,
    onIdFideChange: (String) -> Unit,
    onGoogle: () -> Unit,
    onPhone: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.identity_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.identity_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        if (showIdFideField) {
            OutlinedTextField(
                value = idFide,
                onValueChange = onIdFideChange,
                label = { Text(stringResource(R.string.identity_idfide_label)) },
                placeholder = { Text(stringResource(R.string.identity_idfide_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
        Button(onClick = onGoogle, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.identity_google))
        }
        OutlinedButton(onClick = onPhone, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(R.string.identity_phone))
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.identity_error_retry))
        }
    }
}
