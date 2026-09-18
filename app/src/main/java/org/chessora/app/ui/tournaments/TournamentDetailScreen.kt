package org.chessora.app.ui.tournaments

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

private val TIPOLOGIA_LABELS = mapOf(0 to "Individuale", 1 to "A squadre", 2 to "CIS")
private val TEMPO_LABELS = mapOf(0 to "Standard", 1 to "Rapid", 2 to "Blitz", 3 to "Corrispondenza")
private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)

/** Dettaglio di un EVENTO (mai i singoli giorni di gioco - quelli restano solo nel
 * calendario) aperto dalla Home - mostra luogo, formato, link al sito/bando (condivisi
 * da tutto l'evento), e l'elenco dei tornei dell'evento (uno solo se non ha "fratelli")
 * tra cui scegliere UNA SOLA preiscrizione: richiede di essersi autenticati (Google/
 * telefono), e se non si risulta un socio riconosciuto chiede prima idFide o
 * nome+cognome (vedi ManualIdentityDialog). */
@Composable
fun TournamentDetailScreen(idTournament: Int, onIdentify: () -> Unit) {
    val viewModel = chessoraViewModel { app -> TournamentDetailViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val registering by viewModel.registering.collectAsState()
    val context = LocalContext.current
    var manualIdentityTarget by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(idTournament) { viewModel.load(idTournament) }

    UiStateContent(state = state, onRetry = { viewModel.load(idTournament) }) { data ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            EventHeaderCard(
                tournament = data.tournament,
                eventoNome = data.eventoNome,
                isAuthenticated = data.isAuthenticated,
                onOpenLink = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            )
            Text(
                if (data.options.size > 1) stringResource(R.string.tournament_choose_one) else stringResource(R.string.tournament_section_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
            )
            data.options.forEach { option ->
                TournamentOptionCard(
                    option = option,
                    registering = registering,
                    isAuthenticated = data.isAuthenticated,
                    disabledByOther = data.hasAnyRegistration && !option.isRegistered,
                    onRegister = {
                        viewModel.register(
                            option.tournament.id,
                            onNeedsIdentification = onIdentify,
                            onNeedsManualIdentity = { manualIdentityTarget = option.tournament.id },
                            onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() },
                        )
                    },
                    onCancel = {
                        viewModel.cancelRegistration(option.tournament.id) { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }

    manualIdentityTarget?.let { targetId ->
        ManualIdentityDialog(
            onDismiss = { manualIdentityTarget = null },
            onConfirm = { idFide, displayName ->
                manualIdentityTarget = null
                viewModel.registerWithManualIdentity(
                    targetId,
                    idFideManuale = idFide,
                    displayName = displayName,
                    onError = { error -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show() },
                )
            },
        )
    }
}

/** Chiesta prima di completare la preiscrizione quando l'utente è autenticato ma non
 * risulta un socio riconosciuto (vedi TournamentDetailViewModel.registerWithManualIdentity)
 * - almeno uno dei due campi è obbligatorio (validato anche lato server), servono a
 * mostrare il giocatore su tourn.chessora.org anche senza un socio collegato. */
@Composable
private fun ManualIdentityDialog(onDismiss: () -> Unit, onConfirm: (idFide: String?, displayName: String?) -> Unit) {
    var idFide by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    val canConfirm = idFide.isNotBlank() || displayName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tournament_manual_identity_title)) },
        text = {
            Column {
                Text(stringResource(R.string.tournament_manual_identity_body), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = idFide,
                    onValueChange = { idFide = it },
                    label = { Text(stringResource(R.string.tournament_manual_identity_idfide_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.tournament_manual_identity_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(idFide.ifBlank { null }, displayName.ifBlank { null }) },
                enabled = canConfirm,
            ) {
                Text(stringResource(R.string.tournament_register))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.tournament_manual_identity_cancel)) }
        },
    )
}

/** Informazioni condivise da tutto l'evento (identiche per ogni torneo "fratello", vedi
 * VesusTournamentRequestService/TournamentRegistrationService.AddTorneoAsync lato server
 * che le copia dal torneo "anchor"): luogo, formato generale, link al sito/bando. La
 * preiscrizione vera e propria è nelle card sotto, una per torneo. */
@Composable
private fun EventHeaderCard(
    tournament: TournamentSummary,
    eventoNome: String,
    isAuthenticated: Boolean,
    onOpenLink: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistPill(TIPOLOGIA_LABELS[tournament.tipologiaTorneo] ?: "Torneo")
                AssistPill(tournament.federazione)
            }
            Text(
                eventoNome,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                formatRange(tournament.inizio, tournament.fine),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            val luogo = listOfNotNull(tournament.sede, tournament.localita, tournament.provincia).joinToString(", ")
            if (luogo.isNotBlank()) {
                Text(luogo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            }

            val paginaWeb = tournament.paginaWeb
            val bandoUrl = tournament.bandoPath?.let { NetworkModule.resolveAssetUrl(it) }
            if (paginaWeb != null || bandoUrl != null) {
                Row(modifier = Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paginaWeb != null) {
                        OutlinedButton(onClick = { onOpenLink(paginaWeb) }) { Text(stringResource(R.string.tournament_website)) }
                    }
                    if (bandoUrl != null) {
                        OutlinedButton(onClick = { onOpenLink(bandoUrl) }) { Text(stringResource(R.string.tournament_download_bando)) }
                    }
                }
            }

            if (!isAuthenticated) {
                Text(
                    stringResource(R.string.tournament_identify_to_register),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun AssistPill(text: String) {
    if (text.isBlank()) return
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

/** Una card per ciascun torneo dell'evento (uno solo se non ha "fratelli"): nome, tipo/
 * tempo di gioco, contatore preiscritti, e l'azione di preiscrizione/ritiro. Se il
 * chiamante è già preiscritto a un ALTRO torneo dello stesso evento
 * ([disabledByOther]), il pulsante "Preiscriviti" è disabilitato con una nota - un solo
 * torneo per evento, va ritirata la preiscrizione corrente per sceglierne un altro. */
@Composable
private fun TournamentOptionCard(
    option: TournamentEventOption,
    registering: Boolean,
    isAuthenticated: Boolean,
    disabledByOther: Boolean,
    onRegister: () -> Unit,
    onCancel: () -> Unit,
) {
    val tournament = option.tournament
    val count = tournament.nPreRegisteredPlayers ?: 0
    val unlimited = tournament.limiteIscrizioni <= 0
    val full = !unlimited && count >= tournament.limiteIscrizioni

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(tournament.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistPill(TIPOLOGIA_LABELS[tournament.tipologiaTorneo] ?: "")
                AssistPill(formatTempo(tournament))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        if (unlimited) stringResource(R.string.tournament_registered_count, count)
                        else stringResource(R.string.tournament_registered_count_limit, count, tournament.limiteIscrizioni),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        when {
                            option.isRegistered -> stringResource(R.string.tournament_already_registered)
                            disabledByOther -> stringResource(R.string.tournament_registered_other_option)
                            full -> stringResource(R.string.tournament_seats_full)
                            unlimited -> stringResource(R.string.tournament_seats_unlimited)
                            else -> stringResource(R.string.tournament_seats_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                when {
                    option.isRegistered -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        OutlinedButton(onClick = onCancel, enabled = !registering) {
                            Text(stringResource(R.string.tournament_cancel_registration))
                        }
                    }
                    else -> Button(
                        onClick = onRegister,
                        enabled = isAuthenticated.not() || (!full && !registering && !disabledByOther),
                    ) {
                        Text(if (registering) stringResource(R.string.tournament_registering) else stringResource(R.string.tournament_register))
                    }
                }
            }
        }
    }
}

private fun formatTempo(tournament: TournamentSummary): String {
    val minuti = tournament.tempoMinuti
    if (minuti != null) {
        val incremento = tournament.tempoIncremento ?: 0
        return "$minuti+$incremento"
    }
    return TEMPO_LABELS[tournament.tipologiaTempo] ?: ""
}

private fun formatRange(inizioIso: String, fineIso: String): String {
    val inizio = runCatching { LocalDateTime.parse(inizioIso) }.getOrNull() ?: return inizioIso
    val fine = runCatching { LocalDateTime.parse(fineIso) }.getOrNull()
    val startLabel = inizio.toLocalDate().format(dateFormatter).replaceFirstChar { it.uppercase() }
    if (fine == null || fine.toLocalDate() == inizio.toLocalDate()) return startLabel
    val endLabel = fine.toLocalDate().format(dateFormatter)
    return "$startLabel – $endLabel"
}
