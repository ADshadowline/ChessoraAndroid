package org.chessora.app.ui.mygames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.PlayerGame
import org.chessora.app.data.repository.MyGameCache
import org.chessora.app.ui.chessboard.ChessBoardView
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.chessboard.PgnMoveList
import org.chessora.app.ui.performance.CenteredMessage

/** Sola lettura di una partita registrata in "Le mie partite" - stessa struttura di
 * ChessComGameViewerScreen/LichessGameViewerScreen (board a piena larghezza, nav mosse, PGN
 * cliccabile), ma senza nessuna chiamata di rete: il PGN arriva già completo da [MyGameCache]
 * (popolata da MyGamesListScreen all'apertura), Compose Navigation passa solo l'id nella
 * rotta. */
@Composable
fun MyGameViewerScreen(gameId: Int) {
    val game = remember(gameId) { MyGameCache.get(gameId) }
    if (game == null) {
        CenteredMessage(message = stringResource(R.string.mygames_not_found))
        return
    }

    var moveIndex by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val positions = remember(game.pgn) { ChessPositions.fromPgn(game.pgn) }
    val pgnText = remember(game.pgn) { ChessPositions.formatPgnForDisplay(game.pgn) }
    val sanMoves = remember(pgnText, positions.size) { ChessPositions.sanMovesForDisplay(pgnText, positions.size) }

    LaunchedEffect(positions.size) { moveIndex = positions.size - 1 }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GameHeader(game, modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        ChessBoardView(
            fen = positions.getOrElse(moveIndex) { ChessPositions.STANDARD_START_FEN },
            modifier = Modifier.fillMaxWidth(),
            flipped = flipped,
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.lichess_move_of, moveIndex, (positions.size - 1).coerceAtLeast(0)),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { moveIndex = 0 }, enabled = moveIndex > 0) {
                    Icon(Icons.Default.FirstPage, contentDescription = stringResource(R.string.lichess_move_first))
                }
                IconButton(onClick = { moveIndex = (moveIndex - 1).coerceAtLeast(0) }, enabled = moveIndex > 0) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = stringResource(R.string.lichess_move_prev))
                }
                IconButton(
                    onClick = { moveIndex = (moveIndex + 1).coerceAtMost(positions.size - 1) },
                    enabled = moveIndex < positions.size - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = stringResource(R.string.lichess_move_next))
                }
                IconButton(
                    onClick = { moveIndex = positions.size - 1 },
                    enabled = moveIndex < positions.size - 1,
                ) {
                    Icon(Icons.AutoMirrored.Filled.LastPage, contentDescription = stringResource(R.string.lichess_move_last))
                }
                IconButton(onClick = { flipped = !flipped }) {
                    Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.chessboard_flip))
                }
            }
            PgnMoveList(
                moves = sanMoves,
                currentMoveIndex = moveIndex,
                onMoveClick = { moveIndex = it },
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun GameHeader(game: PlayerGame, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.lichess_game_vs, game.opponentName),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        val subtitle = buildList {
            game.tournamentName?.let { add(it) }
            game.round?.let { add(stringResource(R.string.mygames_round_short, it)) }
            add(Cadenza.fromTipologiaTempo(game.tipologiaTempo).label())
        }.joinToString(" · ")
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}
