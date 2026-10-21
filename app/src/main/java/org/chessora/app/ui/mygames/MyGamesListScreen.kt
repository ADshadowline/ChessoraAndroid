package org.chessora.app.ui.mygames

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.chessora.app.R
import org.chessora.app.data.remote.dto.PlayerGame
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDate
import org.chessora.app.ui.performance.CenteredMessage

/** "Le mie partite": elenco di quelle registrate a mano dal socio (PGN incollato o composto
 * sulla scacchiera, vedi AddMyGameScreen), con ricerca per avversario/torneo. Un solo GET
 * all'apertura schermo (dataset personale, presumibilmente piccolo), filtro successivo SOLO
 * lato client - stesso spirito della ricerca avversario in "Scacchi Online"
 * (ui/performance/online/OnlineGamesScreen.kt). Tenendo premuta una partita si entra in
 * selezione multipla, da cui si genera un PDF delle partite scelte (vedi MyGamesPdf) da
 * condividere/salvare/stampare. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGamesListScreen(onIdentify: () -> Unit, onAddGame: () -> Unit, onOpenGame: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> MyGamesViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val pdfState by viewModel.pdfState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Int>()) }
    val selectionMode = selected.isNotEmpty()

    LaunchedEffect(Unit) { viewModel.load() }
    BackHandler(enabled = selectionMode) { selected = emptySet() }

    LaunchedEffect(pdfState) {
        when (val current = pdfState) {
            is MyGamesPdfState.Ready -> {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", current.file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(Intent.createChooser(send, context.getString(R.string.mygames_pdf_share_title)))
                    selected = emptySet()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.mygames_pdf_failed, Toast.LENGTH_LONG).show()
                }
                viewModel.pdfHandled()
            }
            is MyGamesPdfState.Failed -> {
                Toast.makeText(context, R.string.mygames_pdf_failed, Toast.LENGTH_LONG).show()
                viewModel.pdfHandled()
            }
            else -> Unit
        }
    }

    val allGames = ((state as? UiState.Success)?.data).orEmpty()

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selected = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.mygames_cancel_selection))
                        }
                    },
                    title = { Text(pluralStringResource(R.plurals.mygames_selected_count, selected.size, selected.size)) },
                    actions = {
                        IconButton(onClick = { selected = allGames.map { it.id }.toSet() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.mygames_select_all))
                        }
                        if (pdfState is MyGamesPdfState.Generating) {
                            CircularProgressIndicator(modifier = Modifier.padding(horizontal = 12.dp).size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = {
                                // Stesso ordine dell'elenco, non quello in cui sono state toccate.
                                viewModel.generatePdf(context, allGames.filter { it.id in selected })
                            }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource(R.string.mygames_pdf_create))
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = onAddGame) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.mygames_add))
                }
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
                        if (!selectionMode) {
                            Text(
                                stringResource(R.string.mygames_select_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                            items(filtered, key = { it.id }) { game ->
                                MyGameRow(
                                    game,
                                    isSelected = game.id in selected,
                                    onClick = {
                                        if (selectionMode) selected = selected.toggle(game.id) else onOpenGame(game.id)
                                    },
                                    onLongClick = { selected = selected.toggle(game.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Set<Int>.toggle(id: Int): Set<Int> = if (id in this) this - id else this + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MyGameRow(game: PlayerGame, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.lichess_game_vs, game.opponentName),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val cadenzaLabel = Cadenza.fromTipologiaTempo(game.tipologiaTempo).label()
                    Text(game.timeControl?.let { "$cadenzaLabel $it" } ?: cadenzaLabel, style = MaterialTheme.typography.labelMedium)
                }
                val subtitle = buildList {
                    opponentColorLabel(game.opponentColor)?.let { add(it) }
                    game.tournamentName?.let { add(it) }
                    game.round?.let { add(stringResource(R.string.mygames_round_short, it)) }
                    add(game.datePlayed.toItalianDate())
                }.joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
