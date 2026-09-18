package org.chessora.app.ui.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Ricerca "a chi scrivo" (ui/messaging/): soci identificati in app di qualsiasi
 * circolo, oppure un circolo (recapitato a chi ne è il Referente della
 * messaggistica). Selezionando un risultato si apre subito il thread (ancora senza
 * messaggi: la conversazione viene creata al primo invio, vedi ConversationViewModel).
 */
@Composable
fun NewMessageScreen(onSelectPlayer: (idPlayer: Int, displayName: String) -> Unit, onSelectClub: (idClub: Int, displayName: String) -> Unit) {
    val viewModel = chessoraViewModel { app -> NewMessageViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.init() }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.tab.ordinal) {
            Tab(
                selected = state.tab == NewMessageTab.PLAYERS,
                onClick = { viewModel.selectTab(NewMessageTab.PLAYERS) },
                text = { Text(stringResource(R.string.messaging_tab_players)) },
            )
            Tab(
                selected = state.tab == NewMessageTab.CLUBS,
                onClick = { viewModel.selectTab(NewMessageTab.CLUBS) },
                text = { Text(stringResource(R.string.messaging_tab_clubs)) },
            )
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text(stringResource(R.string.messaging_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        when (state.tab) {
            NewMessageTab.PLAYERS -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.players, key = { it.idPlayer }) { player ->
                    ListItem(
                        headlineContent = { Text(player.fullName) },
                        supportingContent = if (player.clubName.isNotBlank()) { { Text(player.clubName) } } else null,
                        modifier = Modifier.clickable { onSelectPlayer(player.idPlayer, player.fullName) },
                    )
                }
            }
            NewMessageTab.CLUBS -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(state.clubs, key = { it.idClub }) { club ->
                    ListItem(
                        headlineContent = { Text(club.name) },
                        supportingContent = if (!club.hasReferente) { { Text(stringResource(R.string.messaging_club_no_referente), color = MaterialTheme.colorScheme.error) } } else null,
                        modifier = Modifier.clickable { onSelectClub(club.idClub, club.name) },
                    )
                }
            }
        }
    }
}
