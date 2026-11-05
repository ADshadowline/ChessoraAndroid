package org.chessora.app.ui.utilities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R

/** Sotto-menu "Utilità" (icona della Home, prima "Orologio"), stessa forma di "Il mio gioco"
 * (ui/performance/MyGameMenuScreen.kt): l'orologio da scacchi e la combinazione 960. */
@Composable
fun UtilitiesMenuScreen(
    onOpenClock: () -> Unit,
    onOpenChess960: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.chess_clock_title)) },
            leadingContent = { Icon(Icons.Default.Timer, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenClock),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.utilities_chess960)) },
            leadingContent = { Icon(Icons.Default.Casino, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenChess960),
        )
    }
}
