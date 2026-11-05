package org.chessora.app.ui.performance.online

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.chessora.app.R
import org.chessora.app.data.repository.ChessComRepository
import org.chessora.app.data.repository.LichessRepository
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.performance.CenteredMessage
import org.chessora.app.ui.theme.ChessoraError
import org.chessora.app.ui.theme.ChessoraGreen

/** Partite pubbliche giocate online dal socio, su Lichess e/o Chess.com (username
 * configurati in Impostazioni, vedi SettingsScreen), mescolate in un solo elenco ordinato
 * per data - ciascuna riga porta un piccolo badge di provenienza (vedi SourceBadge). GET
 * diretto verso lichess.org/api.chess.com, nessun dato passa dal server Chessora (vedi
 * LichessRepository/ChessComRepository). */
private enum class OnlineGamesTab { GAMES, OPENINGS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineGamesScreen(onOpenSettings: () -> Unit, onOpenLichessGame: (String) -> Unit, onOpenChessComGame: (String) -> Unit) {
    val viewModel = chessoraViewModel { app -> OnlineGamesViewModel(app.repository, LichessRepository(), ChessComRepository(), app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    var tab by remember { mutableStateOf(OnlineGamesTab.GAMES) }
    var searchQuery by remember { mutableStateOf("") }
    // Nome dell'apertura scelta nella scheda "Aperture" (vedi OpeningStatsRow.onClick) - null
    // quando l'elenco partite non è filtrato per apertura, cosi' resta combinabile con la
    // ricerca per avversario (entrambi i filtri si applicano insieme).
    var openingFilter by remember { mutableStateOf<OpeningFilter?>(null) }
    // Famiglie aperte nella scheda "Aperture" (mostrano le sottovarianti).
    var expandedFamilies by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) { viewModel.load() }

    UiStateContent(state = state, onRetry = { viewModel.load() }) { data ->
        if (data.lichessUsername == null && data.chessComUsername == null) {
            CenteredMessage(
                message = stringResource(R.string.online_games_not_configured),
                actionLabel = stringResource(R.string.settings_title),
                onAction = onOpenSettings,
            )
        } else if (data.games.isEmpty()) {
            CenteredMessage(message = stringResource(R.string.online_games_empty))
        } else {
        val openingGroups = remember(data.games) { data.games.computeOpeningGroups() }
        val filteredGames = remember(data.games, searchQuery, openingFilter) {
            data.games.filter { game ->
                (openingFilter?.matches(game) ?: true) &&
                    (searchQuery.isBlank() || game.opponentName.contains(searchQuery, ignoreCase = true))
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = tab == OnlineGamesTab.GAMES,
                    onClick = { tab = OnlineGamesTab.GAMES },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.online_games_tab_games)) }
                SegmentedButton(
                    selected = tab == OnlineGamesTab.OPENINGS,
                    onClick = { tab = OnlineGamesTab.OPENINGS },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.online_games_tab_openings)) }
            }

            if (tab == OnlineGamesTab.GAMES) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    placeholder = { Text(stringResource(R.string.online_games_search_hint)) },
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
                // Barra a tre colori (perse / patte / vinte) delle partite mostrate qui sotto:
                // segue ricerca e filtro apertura.
                ResultsBar(
                    losses = filteredGames.count { it.result == OnlineGameResult.LOSS },
                    draws = filteredGames.count { it.result == OnlineGameResult.DRAW },
                    wins = filteredGames.count { it.result == OnlineGameResult.WIN },
                    modifier = Modifier.padding(top = 10.dp),
                )
                openingFilter?.let { filter ->
                    FilterChip(
                        selected = true,
                        onClick = { openingFilter = null },
                        label = { Text(stringResource(R.string.online_games_opening_filter_active, filter.label)) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (filteredGames.isEmpty()) {
                    CenteredMessage(message = stringResource(R.string.online_games_no_results))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                        items(filteredGames, key = { "${it.source}:${it.id}" }) { game ->
                            OnlineGameRow(
                                game,
                                onClick = {
                                    when (game.source) {
                                        OnlineGameSource.LICHESS -> onOpenLichessGame(game.id)
                                        OnlineGameSource.CHESSCOM -> onOpenChessComGame(game.id)
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                if (openingGroups.isEmpty()) {
                    CenteredMessage(message = stringResource(R.string.online_games_openings_empty))
                } else {
                    // Varianti principali in ordine alfabetico; un tocco apre/chiude le
                    // sottovarianti, un tocco su una sottovariante filtra l'elenco partite.
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                        items(openingGroups, key = { it.stats.name }) { group ->
                            val family = group.stats.name
                            OpeningGroupCard(
                                group = group,
                                expanded = family in expandedFamilies,
                                onToggle = {
                                    expandedFamilies = if (family in expandedFamilies) expandedFamilies - family else expandedFamilies + family
                                },
                                onSelectAll = {
                                    openingFilter = OpeningFilter(family, null, allVariants = true)
                                    tab = OnlineGamesTab.GAMES
                                },
                                onSelectVariant = { variant ->
                                    openingFilter = OpeningFilter(family, variant, allVariants = false)
                                    tab = OnlineGamesTab.GAMES
                                },
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun OnlineGameRow(game: OnlineGameSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ResultBadge(game.result)
            SourceBadge(game.source, modifier = Modifier.padding(start = 6.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.lichess_game_vs, game.opponentName),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${game.speedLabel} · ${formatEpochMillis(game.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            game.myRating?.let {
                Text(it.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Barra a tre colori proporzionale a partite perse (rosso), patte (grigio) e vinte (verde),
 * con il numero dentro ogni tratto abbastanza largo e una riga di riepilogo sotto. Con
 * [compact] solo la barra sottile, senza numeri né riepilogo. */
@Composable
private fun ResultsBar(losses: Int, draws: Int, wins: Int, modifier: Modifier = Modifier, compact: Boolean = false) {
    val total = losses + draws + wins
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 6.dp else 22.dp)
                .clip(RoundedCornerShape(if (compact) 3.dp else 6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (total > 0) {
                listOf(losses to ChessoraError, draws to Color.Gray, wins to ChessoraGreen).forEach { (count, color) ->
                    if (count > 0) {
                        Box(
                            modifier = Modifier.weight(count.toFloat()).fillMaxHeight().background(color),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!compact && count * 100 / total >= 8) {
                                Text(count.toString(), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        if (!compact) {
            Text(
                stringResource(R.string.online_games_results_bar, losses, draws, wins),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** Variante principale (famiglia) nella scheda "Aperture": nome, partite per sito, barra e
 * percentuali. Il tocco apre/chiude l'elenco delle sottovarianti; ogni sottovariante (e la
 * voce "Tutte le partite") filtra l'elenco partite (vedi OnlineGamesScreen). */
@Composable
private fun OpeningGroupCard(
    group: OpeningGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectVariant: (String?) -> Unit,
) {
    val stats = group.stats
    Card(onClick = onToggle, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stats.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.online_games_opening_count, stats.totalGames, stats.lichessCount, stats.chessComCount) +
                            (if (group.variants.size > 1) " · " + stringResource(R.string.online_games_opening_variants, group.variants.size) else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ResultsBar(stats.losses, stats.draws, stats.wins, modifier = Modifier.padding(top = 8.dp), compact = true)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResultPercentBadge(stringResource(R.string.lichess_result_win), stats.winPercent, ChessoraGreen)
                ResultPercentBadge(stringResource(R.string.lichess_result_draw), stats.drawPercent, Color.Gray)
                ResultPercentBadge(stringResource(R.string.lichess_result_loss), stats.lossPercent, ChessoraError)
            }
            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                group.variants.forEach { v ->
                    VariantRow(
                        name = v.variant ?: stringResource(R.string.online_games_opening_main_line),
                        stats = v.stats,
                        onClick = { onSelectVariant(v.variant) },
                    )
                }
                TextButton(onClick = onSelectAll, modifier = Modifier.padding(top = 2.dp)) {
                    Text(stringResource(R.string.online_games_opening_all))
                }
            }
        }
    }
}

@Composable
private fun VariantRow(name: String, stats: OpeningStats, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                "${stats.totalGames} · ${stats.winPercent}% / ${stats.drawPercent}% / ${stats.lossPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        ResultsBar(stats.losses, stats.draws, stats.wins, modifier = Modifier.padding(top = 4.dp), compact = true)
    }
}

@Composable
private fun ResultPercentBadge(label: String, percent: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            "$label $percent%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** Colori reali di ciascun sito (Lichess: verde oliva del loro tema; Chess.com: verde del
 * loro tema) - un piccolo badge testuale ("Li"/"CC"), non i loro loghi ufficiali (Chess.com
 * non è open source, non c'è un asset da riusare legittimamente come per il set di pezzi
 * cburnett di Lichess) - sufficiente per distinguere la provenienza a colpo d'occhio. */
private val LichessBadgeColor = Color(0xFF759900)
private val ChessComBadgeColor = Color(0xFF81B64C)

@Composable
private fun SourceBadge(source: OnlineGameSource, modifier: Modifier = Modifier) {
    val (label, color) = when (source) {
        OnlineGameSource.LICHESS -> "Li" to LichessBadgeColor
        OnlineGameSource.CHESSCOM -> "CC" to ChessComBadgeColor
    }
    Box(
        modifier = modifier.size(22.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ResultBadge(result: OnlineGameResult) {
    val (label, color) = when (result) {
        OnlineGameResult.WIN -> stringResource(R.string.lichess_result_win) to ChessoraGreen
        OnlineGameResult.LOSS -> stringResource(R.string.lichess_result_loss) to ChessoraError
        OnlineGameResult.DRAW -> stringResource(R.string.lichess_result_draw) to Color.Gray
    }
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

private fun formatEpochMillis(millis: Long): String {
    if (millis <= 0) return ""
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormatter)
}
