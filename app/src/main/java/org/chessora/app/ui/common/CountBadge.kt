package org.chessora.app.ui.common

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.chessora.app.ui.theme.ChessoraSkyBlueBadge

/** Badge numerico condiviso da tutte le icone con un conteggio (Messaggi non letti,
 * Iscrizioni ai tornei) - sfondo azzurrino con una leggera ombra, testo bianco per
 * contrasto, posizionato in alto a destra dell'icona da [androidx.compose.material3.BadgedBox]
 * (comportamento di default, nessuna posizione custom necessaria). */
@Composable
fun ChessoraCountBadge(count: Int, modifier: Modifier = Modifier) {
    Badge(
        modifier = modifier.shadow(elevation = 2.dp, shape = CircleShape),
        containerColor = ChessoraSkyBlueBadge,
        contentColor = Color.White,
    ) {
        Text(count.toString())
    }
}
