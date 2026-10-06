package org.chessora.app.ui.chessboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.chessora.app.R

private val LightSquare = Color(0xFFEEDDB0)
private val DarkSquare = Color(0xFFB4794F)

/** Scacchiera disegnata a mano (Row/Box, non un Canvas: 64 celle bastano, niente disegno
 * a basso livello) a partire da una posizione FEN - nessuna libreria UI di scacchiera:
 * coerente con la filosofia di dipendenze minime già seguita per il grafico Elo in
 * ui/performance/PerformanceScreen.kt ("nessuna libreria di grafici... disegnato a
 * mano"). Solo la LOGICA (validare le mosse, sapere dove sono i pezzi) viene da una
 * libreria dedicata, vedi ChessPositions. I pezzi sono il set open source "cburnett"
 * (lo stesso usato da Lichess/Wikipedia, CC BY-SA 3.0 - Colin M.L. Burnett), convertito
 * da SVG a vector drawable Android (vedi PIECE_DRAWABLES), non più semplici glifi
 * Unicode: molto più leggibili e curati. [flipped] mostra la scacchiera dal punto di
 * vista del Nero (usato quando il socio ha giocato con i pezzi neri). */
@Composable
fun ChessBoardView(fen: String, modifier: Modifier = Modifier, flipped: Boolean = false) {
    val board = remember(fen) { parseFenPlacement(fen) }
    Column(modifier = modifier.aspectRatio(1f)) {
        for (displayRank in 0..7) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (displayFile in 0..7) {
                    val rank = if (flipped) 7 - displayRank else displayRank
                    val file = if (flipped) 7 - displayFile else displayFile
                    val isLight = (rank + file) % 2 == 0
                    val piece = board[rank][file]
                    val labelColor = if (isLight) DarkSquare else LightSquare
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isLight) LightSquare else DarkSquare),
                        contentAlignment = Alignment.Center,
                    ) {
                        PIECE_DRAWABLES[piece]?.let { drawableRes ->
                            Image(
                                painter = painterResource(drawableRes),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                            )
                        }
                        // Coordinate solo sul bordo (prima colonna per il numero di
                        // traversa, ultima riga per la lettera di colonna) - stesso
                        // posizionamento di Lichess/Chess.com, niente righe/colonne extra
                        // che cambierebbero l'aspect ratio 1:1 della scacchiera.
                        if (displayFile == 0) {
                            Text(
                                text = (8 - rank).toString(),
                                color = labelColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopStart).padding(1.dp),
                            )
                        }
                        if (displayRank == 7) {
                            Text(
                                text = ('a' + file).toString(),
                                color = labelColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Solo la piazzatura dei pezzi (il primo campo del FEN) - il resto (turno, arrocchi,
 * en passant, orologi) non serve per disegnare la scacchiera. Righe FEN nell'ordine
 * standard: la prima è la traversa 8 (in alto per il Bianco), l'ultima la traversa 1. */
private fun parseFenPlacement(fen: String): Array<CharArray> {
    val board = Array(8) { CharArray(8) { ' ' } }
    val placement = fen.substringBefore(' ')
    placement.split("/").forEachIndexed { rankIndex, row ->
        if (rankIndex > 7) return@forEachIndexed
        var file = 0
        for (c in row) {
            if (file > 7) break
            if (c.isDigit()) {
                file += c.digitToInt()
            } else {
                board[rankIndex][file] = c
                file++
            }
        }
    }
    return board
}

private val PIECE_DRAWABLES = mapOf(
    'K' to R.drawable.piece_wk, 'Q' to R.drawable.piece_wq, 'R' to R.drawable.piece_wr,
    'B' to R.drawable.piece_wb, 'N' to R.drawable.piece_wn, 'P' to R.drawable.piece_wp,
    'k' to R.drawable.piece_bk, 'q' to R.drawable.piece_bq, 'r' to R.drawable.piece_br,
    'b' to R.drawable.piece_bb, 'n' to R.drawable.piece_bn, 'p' to R.drawable.piece_bp,
)
