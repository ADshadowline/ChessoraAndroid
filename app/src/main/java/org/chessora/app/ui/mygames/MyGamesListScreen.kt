package org.chessora.app.ui.mygames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.PlayerGame
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDate
import org.chessora.app.ui.performance.CenteredMessage

/** "Le mie partite": elenco di quelle registrate a mano dal socio (PGN incollato o composto
 * sulla scacchiera, vedi AddMyGameScreen), con ricerca per avversario/torneo. Un solo GET
 * all'apertura schermo (dataset personale, presumibilmente piccolo), filtro successivo SOLO
 * lato client - stesso spirito della ricerca avversario in "Scacchi Online"
 * (ui/performance/online/OnlineGamesScreen.kt). */
@Composable
fun MyGamesListScreen(onIdentify: () -> Unit, onAddGame: () -> Unit, onOpenGame: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> MyGamesViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGame) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.mygames_add))
            }
        },
    ) { padding ->
        UiStateContent(state = state, onRetry = { viewModel.load() }) { games ->
            if (games == null) {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    CenteredMessage(
                        message = stringResource(R.string.mygames_not_identified),
                        actionLabel = stringResource(R.string.settings_identity_button),
                        onAction = onIdentify,
                    )
                }
            } else {
                val filtered = remember(games, searchQuery) {
                    if (searchQuery.isBlank()) {
                        games
                    } else {
                        games.filter {
                            it.opponentName.contains(searchQuery, ignoreCase = true) ||
                                it.tournamentName?.contains(searchQuery, ignoreCase = true) == true
                        }
                    }
                }
                Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.mygames_search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.online_games_search_clear))
                                }
                            }
                        },
                        singleLine = true,
                    )
                    if (games.isEmpty()) {
                        CenteredMessage(message = stringResource(R.string.mygames_empty))
                    } else if (filtered.isEmpty()) {
                        CenteredMessage(message = stringResource(R.string.online_games_no_results))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                            items(filtered, key = { it.id }) { game ->
                                MyGameRow(game, onClick = { onOpenGame(game.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyGameRow(game: PlayerGame, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.lichess_game_vs, game.opponentName),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(Cadenza.fromTipologiaTempo(game.tipologiaTempo).label(), style = MaterialTheme.typography.labelMedium)
            }
            val subtitle = buildList {
                game.tournamentName?.let { add(it) }
                game.round?.let { add(stringResource(R.string.mygames_round_short, it)) }
                add(game.createdAt.toItalianDate())
            }.joinToString(" · ")
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
