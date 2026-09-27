package org.chessora.app.ui.performance.lichess

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
import org.chessora.app.data.remote.dto.LichessGame
import org.chessora.app.data.repository.LichessRepository
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.performance.CenteredMessage
import org.chessora.app.ui.theme.ChessoraError
import org.chessora.app.ui.theme.ChessoraGreen

private enum class LichessResult { WIN, LOSS, DRAW }

/** Partite pubbliche giocate online su Lichess dal socio (username configurato in
 * Impostazioni, vedi SettingsScreen) - GET diretto verso lichess.org, nessun dato passa
 * dal server Chessora (vedi LichessRepository). */
@Composable
fun LichessGamesScreen(onOpenSettings: () -> Unit, onOpenGame: (String) -> Unit) {
    val viewModel = chessoraViewModel { app -> LichessGamesViewModel(app.repository, LichessRepository(), app.clubPreferences) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    UiStateContent(state = state, onRetry = { viewModel.load() }) { data ->
        when {
            data.username == null -> CenteredMessage(
                message = stringResource(R.string.lichess_not_configured),
                actionLabel = stringResource(R.string.settings_lichess_title),
                onAction = onOpenSettings,
            )
            data.games.isEmpty() -> CenteredMessage(message = stringResource(R.string.lichess_games_empty))
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(data.games, key = { it.id }) { game ->
                    LichessGameRow(game, data.username, onClick = { onOpenGame(game.id) })
                }
            }
        }
    }
}

@Composable
private fun LichessGameRow(game: LichessGame, myUsername: String, onClick: () -> Unit) {
    val amIWhite = game.players.white.user?.name.equals(myUsername, ignoreCase = true)
    val me = if (amIWhite) game.players.white else game.players.black
    val opponent = if (amIWhite) game.players.black else game.players.white
    val result = when {
        game.winner == null -> LichessResult.DRAW
        (game.winner == "white") == amIWhite -> LichessResult.WIN
        else -> LichessResult.LOSS
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ResultBadge(result)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    stringResource(R.string.lichess_game_vs, opponent.displayName()),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${game.speed.replaceFirstChar { it.uppercase() }} · ${formatEpochMillis(game.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            me.rating?.let {
                Text(it.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultBadge(result: LichessResult) {
    val (label, color) = when (result) {
        LichessResult.WIN -> stringResource(R.string.lichess_result_win) to ChessoraGreen
        LichessResult.LOSS -> stringResource(R.string.lichess_result_loss) to ChessoraError
        LichessResult.DRAW -> stringResource(R.string.lichess_result_draw) to Color.Gray
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
