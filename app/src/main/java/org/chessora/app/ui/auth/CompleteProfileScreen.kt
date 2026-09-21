package org.chessora.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Completamento profilo dopo un primo login Google (vedi CompleteProfileViewModel):
 * "hai un ID FIDE oppure sei socio di un circolo?" - se sì, stesso percorso di
 * LoginFideScreen (ricerca/verifica ID FIDE + eventuale scelta circolo/socio) ma senza
 * email/password (l'account esiste già); se no, richiede solo nome/cognome/password
 * per un account amatoriale.
 */
@Composable
fun CompleteProfileScreen(onDone: () -> Unit) {
    val viewModel = chessoraViewModel { app -> CompleteProfileViewModel(app.repository, app.clubPreferences, app.authPreferences) }
    val phase by viewModel.phase.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val clubs by viewModel.clubs.collectAsState()
    val roster by viewModel.roster.collectAsState()

    var idFideInput by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var clubQuery by rememberSaveable { mutableStateOf("") }
    var rosterQuery by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(phase) {
        if (phase is ProfileUiPhase.Done) onDone()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        when (val current = phase) {
            is ProfileUiPhase.Loading -> CircularProgressIndicator()

            is ProfileUiPhase.AskHasFide -> {
                Text(stringResource(R.string.complete_profile_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.complete_profile_ask_fide),
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
                )
                Button(onClick = { viewModel.answerHasFide(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.common_yes))
                }
                OutlinedButton(onClick = { viewModel.answerHasFide(false) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(stringResource(R.string.common_no))
                }
            }

            is ProfileUiPhase.EnterIdFide -> {
                Text(stringResource(R.string.login_fide_title), style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = idFideInput,
                    onValueChange = { idFideInput = it },
                    label = { Text(stringResource(R.string.login_fide_field_label)) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
                Button(onClick = { viewModel.submitIdFide(idFideInput) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(stringResource(R.string.login_fide_submit))
                }
                TextButton(onClick = { showSearch = !showSearch }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.login_fide_dont_know))
                }
                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; viewModel.searchByName(it) },
                        label = { Text(stringResource(R.string.login_fide_search_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    if (isSearching) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.common_loading), modifier = Modifier.padding(start = 8.dp))
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            items(searchResults, key = { it.idFide }) { result ->
                                Card(onClick = { viewModel.selectFromSearch(result) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(result.name, style = MaterialTheme.typography.titleSmall)
                                        Text("ID FIDE ${result.idFide}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is ProfileUiPhase.AskClubMembership -> {
                Text(stringResource(R.string.login_fide_ask_club_membership), style = MaterialTheme.typography.titleMedium)
                Button(onClick = { viewModel.answerClubMembership(true) }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Text(stringResource(R.string.common_yes))
                }
                OutlinedButton(onClick = { viewModel.answerClubMembership(false) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(stringResource(R.string.common_no))
                }
            }

            is ProfileUiPhase.PickClub -> {
                Text(stringResource(R.string.pick_club_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = clubQuery,
                    onValueChange = { clubQuery = it },
                    label = { Text(stringResource(R.string.onboarding_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                )
                val filtered = remember(clubs, clubQuery) {
                    if (clubQuery.isBlank()) clubs else clubs.filter { it.name.contains(clubQuery, ignoreCase = true) }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.publicCode }) { club ->
                        Card(onClick = { viewModel.selectClub(club) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(club.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            is ProfileUiPhase.PickRoster -> {
                Text(stringResource(R.string.pick_roster_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = rosterQuery,
                    onValueChange = { rosterQuery = it },
                    label = { Text(stringResource(R.string.pick_roster_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                )
                val filtered = remember(roster, rosterQuery) {
                    if (rosterQuery.isBlank()) roster else roster.filter { it.name.contains(rosterQuery, ignoreCase = true) }
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filtered, key = { it.idPlayer }) { entry ->
                        Card(onClick = { viewModel.selectRosterEntry(entry) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(entry.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            is ProfileUiPhase.ConfirmFide -> {
                Text(stringResource(R.string.login_fide_found, current.name), style = MaterialTheme.typography.titleMedium)
                Button(onClick = { viewModel.confirmFide() }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Text(stringResource(R.string.common_confirm))
                }
            }

            is ProfileUiPhase.AmateurDetails -> {
                Text(stringResource(R.string.complete_profile_amateur_title), style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(stringResource(R.string.register_first_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(R.string.register_last_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.register_confirm_password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Button(
                    onClick = { viewModel.submitAmateur(firstName, lastName, password, confirmPassword) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text(stringResource(R.string.register_submit)) }
            }

            is ProfileUiPhase.Error -> {
                Text(current.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.retry() }, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.identity_error_retry))
                }
            }

            else -> Unit
        }
    }
}
