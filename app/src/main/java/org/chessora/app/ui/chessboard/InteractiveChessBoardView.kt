package org.chessora.app.ui.chessboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.PieceType
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.github.bhlangonijr.chesslib.move.MoveList
import org.chessora.app.R

/**
 * Stato di una partita in composizione a tocchi (vedi ui/mygames/AddMyGameScreen.kt,
 * modalità "Componi sulla scacchiera") - tiene una [Board] chesslib live PIÙ una semplice
 * lista di [Move] Kotlin, MAI una [MoveList] chesslib durante la composizione: [Board.
 * undoMove] non la terrebbe sincronizzata (vedi nota su [toPgn]). La [MoveList] chesslib
 * viene costruita una sola volta, al salvataggio, da [toPgn].
 */
class InteractiveGameState {
    private val board = Board()
    private val moves = mutableStateListOf<Move>()
    var selectedSquare by mutableStateOf<Square?>(null)
        private set
    var pendingPromotion by mutableStateOf<PendingPromotion?>(null)
        private set
    var fen by mutableStateOf(board.fen)
        private set

    val moveCount: Int get() = moves.size
    val canUndo: Boolean get() = moves.isNotEmpty()

    fun legalDestinationsFrom(square: Square): Set<Square> =
        board.legalMoves().filter { it.from == square }.map { it.to }.toSet()

    fun onSquareTapped(square: Square) {
        if (pendingPromotion != null) return
        val selected = selectedSquare
        if (selected == null) {
            if (isOwnPieceAt(square)) selectedSquare = square
            return
        }
        if (selected == square) {
            selectedSquare = null
            return
        }
        val candidates = board.legalMoves().filter { it.from == selected && it.to == square }
        when {
            candidates.isEmpty() -> selectedSquare = if (isOwnPieceAt(square)) square else null
            candidates.size == 1 -> {
                applyMove(candidates[0])
                selectedSquare = null
            }
            else -> pendingPromotion = PendingPromotion(selected, square, candidates)
        }
    }

    fun choosePromotion(pieceType: PieceType) {
        val pending = pendingPromotion ?: return
        val move = pending.candidates.firstOrNull { it.promotion?.pieceType == pieceType } ?: return
        applyMove(move)
        pendingPromotion = null
        selectedSquare = null
    }

    fun cancelPromotion() {
        pendingPromotion = null
    }

    fun undoLast() {
        if (moves.isEmpty()) return
        board.undoMove()
        moves.removeAt(moves.lastIndex)
        fen = board.fen
        selectedSquare = null
        pendingPromotion = null
    }

    private fun applyMove(move: Move) {
        board.doMove(move)
        moves.add(move)
        fen = board.fen
    }

    private fun isOwnPieceAt(square: Square): Boolean {
        val piece = board.getPiece(square)
        return piece != Piece.NONE && piece.pieceSide == board.sideToMove
    }

    /** Testo PGN completo ("1. e4 e5 2. Nf3 ..."), null se non è stata ancora fatta
     * nessuna mossa. La MoveList chesslib costruita qui rigioca da sola le mosse dalla
     * posizione iniziale per generare la notazione SAN (vedi doc di ChessPositions.
     * validateStrict per il motivo per cui questo È sicuro, a differenza di addSanMove con
     * replay=false): nessun collegamento con la [board] di composizione sopra. */
    fun toPgn(): String? {
        if (moves.isEmpty()) return null
        return try {
            MoveList().apply { addAll(moves) }.toSanWithMoveNumbers().trim()
        } catch (e: Exception) {
            null
        }
    }
}

data class PendingPromotion(val from: Square, val to: Square, val candidates: List<Move>)

@Composable
fun rememberInteractiveGameState(): InteractiveGameState = remember { InteractiveGameState() }

/** Scacchiera componibile a tocchi: disegna la posizione corrente con [ChessBoardView] (mai
 * duplicata) e sovrappone una griglia trasparente identica per geometria che gestisce i
 * tocchi, evidenzia la casella selezionata e le destinazioni legali. Mostra un dialogo di
 * scelta pezzo quando una mossa di pedone ha più candidate legali (promozione). */
@Composable
fun InteractiveChessBoardView(state: InteractiveGameState, modifier: Modifier = Modifier) {
    val legalDestinations = state.selectedSquare?.let { state.legalDestinationsFrom(it) } ?: emptySet()

    Box(modifier = modifier) {
        ChessBoardView(fen = state.fen, modifier = Modifier.fillMaxWidth())
        TapOverlayGrid(
            selectedSquare = state.selectedSquare,
            legalDestinations = legalDestinations,
            onSquareTapped = state::onSquareTapped,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (state.pendingPromotion != null) {
        PromotionDialog(onChoose = state::choosePromotion, onDismiss = state::cancelPromotion)
    }
}

@Composable
private fun TapOverlayGrid(
    selectedSquare: Square?,
    legalDestinations: Set<Square>,
    onSquareTapped: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.then(Modifier.fillMaxWidth())) {
        for (displayRank in 0..7) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (displayFile in 0..7) {
                    val square = squareAt(displayRank, displayFile)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSquareTapped(square) },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            square == selectedSquare -> Box(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight().border(3.dp, SelectionColor),
                            )
                            square in legalDestinations -> Box(
                                modifier = Modifier.size(18.dp).clip(CircleShape).background(DestinationDotColor),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Stesso adattamento rank/file di ChessBoardView (mai capovolta qui: comporre una partita
 * nuova parte sempre dalla prospettiva del Bianco in basso). */
private fun squareAt(displayRank: Int, displayFile: Int): Square =
    Square.valueOf("${'A' + displayFile}${8 - displayRank}")

private val SelectionColor = Color(0xCCFFC107)
private val DestinationDotColor = Color(0x992E7D32)

@Composable
private fun PromotionDialog(onChoose: (PieceType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mygames_promotion_title)) },
        text = {
            Column {
                PromotionChoiceRow(PieceType.QUEEN, stringResource(R.string.mygames_promotion_queen), onChoose)
                PromotionChoiceRow(PieceType.ROOK, stringResource(R.string.mygames_promotion_rook), onChoose)
                PromotionChoiceRow(PieceType.BISHOP, stringResource(R.string.mygames_promotion_bishop), onChoose)
                PromotionChoiceRow(PieceType.KNIGHT, stringResource(R.string.mygames_promotion_knight), onChoose)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.mygames_cancel)) } },
    )
}

@Composable
private fun PromotionChoiceRow(pieceType: PieceType, label: String, onChoose: (PieceType) -> Unit) {
    TextButton(onClick = { onChoose(pieceType) }, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
    }
}
