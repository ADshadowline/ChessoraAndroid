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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import org.chessora.app.data.remote.dto.OpponentColor
import org.chessora.app.data.remote.dto.PlayerGame
import org.chessora.app.data.repository.MyGameCache
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.chessboard.ChessBoardView
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.chessboard.PgnMoveList
import org.chessora.app.ui.performance.CenteredMessage

/** Sola lettura di una partita registrata in "Le mie partite" - stessa struttura di
 * ChessComGameViewerScreen/LichessGameViewerScreen (board a piena larghezza, nav mosse, PGN
 * cliccabile), ma senza nessuna chiamata di rete: il PGN arriva già completo da [MyGameCache]
 * (popolata da MyGamesListScreen all'apertura), Compose Navigation passa solo l'id nella
 * rotta. Dall'intestazione la partita si può modificare ([onEdit], stesso form di
 * inserimento) o eliminare (con conferma, poi [onDeleted]). */
@Composable
fun MyGameViewerScreen(gameId: Int, onEdit: (Int) -> Unit, onDeleted: () -> Unit) {
    val viewModel = chessoraViewModel { app -> MyGameViewerViewModel(app.repository, app.clubPreferences) }
    val deleteState by viewModel.deleteState.collectAsState()
    val game = remember(gameId) { MyGameCache.get(gameId) }

    LaunchedEffect(deleteState) {
        if (deleteState is DeleteGameUiState.Deleted) onDeleted()
    }

    if (game == null) {
        CenteredMessage(message = stringResource(R.string.mygames_not_found))
        return
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var moveIndex by remember { mutableStateOf(0) }
    // Il socio in basso: se l'avversario aveva il Bianco, il socio aveva il Nero.
    var flipped by remember(game.opponentColor) { mutableStateOf(game.opponentColor == OpponentColor.WHITE) }
    val positions = remember(game.pgn) { ChessPositions.fromPgn(game.pgn) }
    val pgnText = remember(game.pgn) { ChessPositions.formatPgnForDisplay(game.pgn) }
    val sanMoves = remember(pgnText, positions.size) { ChessPositions.sanMovesForDisplay(pgnText, positions.size) }

    LaunchedEffect(positions.size) { moveIndex = positions.size - 1 }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.mygames_delete_confirm_title)) },
            text = { Text(stringResource(R.string.mygames_delete_confirm_body, game.opponentName)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(game.id)
                }) { Text(stringResource(R.string.mygames_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.mygames_cancel)) }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GameHeader(game, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            OutlinedButton(onClick = { onEdit(game.id) }, enabled = deleteState !is DeleteGameUiState.Deleting) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(R.string.mygames_edit))
            }
            OutlinedButton(onClick = { showDeleteConfirm = true }, enabled = deleteState !is DeleteGameUiState.Deleting) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(R.string.mygames_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        (deleteState as? DeleteGameUiState.Error)?.let {
            Text(
                it.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            )
        }
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
            opponentColorLabel(game.opponentColor)?.let { add(it) }
            game.tournamentName?.let { add(it) }
            game.round?.let { add(stringResource(R.string.mygames_round_short, it)) }
            add(Cadenza.fromTipologiaTempo(game.tipologiaTempo).label())
        }.joinToString(" · ")
        Text(subtitle, style = MaterialTheme.typography.bodySmall)
    }
}

/** "Avversario col Bianco"/"col Nero", null per le partite salvate senza colore. */
@Composable
internal fun opponentColorLabel(opponentColor: String?): String? = when (opponentColor) {
    OpponentColor.WHITE -> stringResource(R.string.mygames_opponent_plays_white)
    OpponentColor.BLACK -> stringResource(R.string.mygames_opponent_plays_black)
    else -> null
}
