package org.chessora.app.ui.mygames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.ui.chessboard.InteractiveChessBoardView
import org.chessora.app.ui.chessboard.StrictPgnValidation
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.chessboard.rememberInteractiveGameState
import org.chessora.app.ui.common.chessoraViewModel

private enum class AddGameMode { PASTE, COMPOSE }

/** Form di "Le mie partite": campi anagrafici (avversario/turno/torneo/cadenza) in testa,
 * poi un selettore a due modalità per il contenuto della partita - incollare un PGN già
 * pronto o comporlo a tocchi sulla scacchiera (vedi InteractiveChessBoardView). "Salva" resta
 * disabilitato finché non c'è almeno una mossa riconosciuta, mai un salvataggio parziale
 * silenzioso (vedi ChessPositions.validateStrict). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddMyGameScreen(onSaved: () -> Unit) {
    val viewModel = chessoraViewModel { app -> AddMyGameViewModel(app.repository, app.clubPreferences) }
    val saveState by viewModel.saveState.collectAsState()

    var opponentName by remember { mutableStateOf("") }
    var roundText by remember { mutableStateOf("") }
    var tournamentName by remember { mutableStateOf("") }
    var cadenza by remember { mutableStateOf(Cadenza.STANDARD) }
    var mode by remember { mutableStateOf(AddGameMode.PASTE) }
    var pgnText by remember { mutableStateOf("") }
    val gameState = rememberInteractiveGameState()

    LaunchedEffect(saveState) {
        if (saveState is SaveGameUiState.Saved) onSaved()
    }

    val pgnValidation = remember(pgnText) { if (pgnText.isBlank()) null else ChessPositions.validateStrict(pgnText) }
    val pgnToSave = when (mode) {
        AddGameMode.PASTE -> (pgnValidation as? StrictPgnValidation.Valid)?.let { pgnText.trim() }
        AddGameMode.COMPOSE -> gameState.toPgn()
    }
    val canSave = opponentName.isNotBlank() && pgnToSave != null && saveState !is SaveGameUiState.Saving

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        OutlinedTextField(
            value = opponentName,
            onValueChange = { opponentName = it },
            label = { Text(stringResource(R.string.mygames_opponent_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = roundText,
            onValueChange = { roundText = it.filter(Char::isDigit) },
            label = { Text(stringResource(R.string.mygames_round_label)) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        OutlinedTextField(
            value = tournamentName,
            onValueChange = { tournamentName = it },
            label = { Text(stringResource(R.string.mygames_tournament_label)) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true,
        )

        Text(
            stringResource(R.string.mygames_cadenza_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cadenza.entries.forEach { option ->
                FilterChip(selected = cadenza == option, onClick = { cadenza = option }, label = { Text(option.label()) })
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            SegmentedButton(
                selected = mode == AddGameMode.PASTE,
                onClick = { mode = AddGameMode.PASTE },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.mygames_mode_paste)) }
            SegmentedButton(
                selected = mode == AddGameMode.COMPOSE,
                onClick = { mode = AddGameMode.COMPOSE },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.mygames_mode_compose)) }
        }

        if (mode == AddGameMode.PASTE) {
            OutlinedTextField(
                value = pgnText,
                onValueChange = { pgnText = it },
                label = { Text(stringResource(R.string.chess_pgn_label)) },
                placeholder = { Text(stringResource(R.string.mygames_pgn_hint)) },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                minLines = 5,
            )
            val validation = pgnValidation
            when (validation) {
                null -> {}
                is StrictPgnValidation.Valid -> Text(
                    stringResource(R.string.mygames_pgn_valid, validation.moveCount),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
                is StrictPgnValidation.Invalid -> Text(
                    if (validation.totalTokenCount == 0) {
                        stringResource(R.string.mygames_pgn_empty)
                    } else {
                        stringResource(
                            R.string.mygames_pgn_invalid,
                            validation.parsedMoveCount + 1,
                            validation.parsedMoveCount,
                            validation.totalTokenCount,
                        )
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        } else {
            InteractiveChessBoardView(state = gameState, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            Text(
                stringResource(R.string.mygames_moves_count, gameState.moveCount),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(
                onClick = { gameState.undoLast() },
                enabled = gameState.canUndo,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text(stringResource(R.string.mygames_undo_move)) }
        }

        if (saveState is SaveGameUiState.Error) {
            Text(
                (saveState as SaveGameUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Button(
            onClick = { pgnToSave?.let { viewModel.save(opponentName.trim(), roundText.toIntOrNull(), tournamentName, cadenza, it) } },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        ) { Text(stringResource(R.string.mygames_save)) }
    }
}
