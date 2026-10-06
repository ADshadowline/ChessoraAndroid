package org.chessora.app.ui.performance.lichess

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.LichessGame
import org.chessora.app.data.repository.LichessRepository
import org.chessora.app.ui.chessboard.ChessBoardView
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.chessboard.PgnMoveList
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Scacchiera di una singola partita Lichess, con controlli per scorrere le mosse -
 * vedi ChessBoardView (disegnata a mano) e ChessPositions (motore/parser chesslib). */
@Composable
fun LichessGameViewerScreen(gameId: String) {
    val viewModel = chessoraViewModel { app -> LichessGameViewerViewModel(LichessRepository()) }
    val state by viewModel.state.collectAsState()
    var moveIndex by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(gameId) { viewModel.load(gameId) }

    UiStateContent(state = state, onRetry = { viewModel.load(gameId) }) { data ->
        LaunchedEffect(data.positions.size) { moveIndex = data.positions.size - 1 }
        val pgnText = remember(data.game.moves) { ChessPositions.formatLichessMovesForDisplay(data.game.moves) }
        val sanMoves = remember(pgnText, data.positions.size) { ChessPositions.sanMovesForDisplay(pgnText, data.positions.size) }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            GameHeader(data.game, modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
            if (data.game.variant != "standard") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        stringResource(R.string.lichess_variant_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://lichess.org/${data.game.id}"))) },
                        modifier = Modifier.padding(top = 12.dp),
                    ) { Text(stringResource(R.string.lichess_open_on_lichess)) }
                }
            } else {
                // Scacchiera a piena larghezza, senza margini laterali: la priorità è
                // vederla il più grande possibile (richiesta esplicita), i controlli sotto
                // restano invece con il solito margine di lettura.
                ChessBoardView(
                    fen = data.positions.getOrElse(moveIndex) { ChessPositions.STANDARD_START_FEN },
                    modifier = Modifier.fillMaxWidth(),
                    flipped = flipped,
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        stringResource(R.string.lichess_move_of, moveIndex, (data.positions.size - 1).coerceAtLeast(0)),
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
                            onClick = { moveIndex = (moveIndex + 1).coerceAtMost(data.positions.size - 1) },
                            enabled = moveIndex < data.positions.size - 1,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = stringResource(R.string.lichess_move_next))
                        }
                        IconButton(
                            onClick = { moveIndex = data.positions.size - 1 },
                            enabled = moveIndex < data.positions.size - 1,
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
    }
}

@Composable
private fun GameHeader(game: LichessGame, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Text(
            "${game.players.white.displayName()} – ${game.players.black.displayName()}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(game.speed.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
    }
}
