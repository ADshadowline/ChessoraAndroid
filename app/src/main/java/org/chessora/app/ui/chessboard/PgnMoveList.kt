package org.chessora.app.ui.chessboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Elenco delle mosse PGN come "etichette" cliccabili che vanno a capo (FlowRow) invece del
 * testo unico precedente: cliccando una mossa la scacchiera salta subito a quella
 * posizione, e la mossa che corrisponde a [currentMoveIndex] (stesso indice usato in
 * ChessPositions.fromPgn/fromMoves, 0 = posizione di partenza) resta evidenziata - cosi'
 * muovendosi con i pulsanti avanti/indietro si vede subito a che punto della partita ci si
 * trova anche qui, non solo sulla scacchiera. [moves] è una mossa SAN per ply (vedi
 * ChessPositions.sanMovesForDisplay), senza numeri di turno: quelli li aggiunge questa UI
 * solo davanti alle mosse del Bianco.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PgnMoveList(
    moves: List<String>,
    currentMoveIndex: Int,
    onMoveClick: (positionIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (moves.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        moves.forEachIndexed { index, san ->
            val positionIndex = index + 1
            if (index % 2 == 0) {
                Text(
                    text = "${index / 2 + 1}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp, top = 3.dp),
                )
            }
            val selected = positionIndex == currentMoveIndex
            Text(
                text = san,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onMoveClick(positionIndex) }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}
