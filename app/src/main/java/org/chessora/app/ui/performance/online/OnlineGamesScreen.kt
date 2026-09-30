package org.chessora.app.ui.performance.online

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
@Composable
fun OnlineGamesScreen(onOpenSettings: () -> Unit, onOpenLichessGame: (String) -> Unit, onOpenChessComGame: (String) -> Unit) {
    val viewModel = chessoraViewModel { app -> OnlineGamesViewModel(app.repository, LichessRepository(), ChessComRepository(), app.clubPreferences) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    UiStateContent(state = state, onRetry = { viewModel.load() }) { data ->
        when {
            data.lichessUsername == null && data.chessComUsername == null -> CenteredMessage(
                message = stringResource(R.string.online_games_not_configured),
                actionLabel = stringResource(R.string.settings_title),
                onAction = onOpenSettings,
            )
            data.games.isEmpty() -> CenteredMessage(message = stringResource(R.string.online_games_empty))
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(data.games, key = { "${it.source}:${it.id}" }) { game ->
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
