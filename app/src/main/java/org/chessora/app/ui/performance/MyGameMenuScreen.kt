package org.chessora.app.ui.performance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R

/** Sotto-menu "Il mio gioco" (icona della Home, prima "I miei progressi"), con la stessa
 * forma di quello de "Il Circolo" (ui/club/ClubScreen.kt): andamento Elo, partite giocate di
 * persona ("Le mie partite", ui/mygames/) e partite online (Lichess/Chess.com). */
@Composable
fun MyGameMenuScreen(
    onOpenElo: () -> Unit,
    onOpenMyGames: () -> Unit,
    onOpenOnlineGames: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.performance_tab_elo)) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenElo),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.my_game_menu_otb)) },
            leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenMyGames),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.my_game_menu_online)) },
            leadingContent = { Icon(Icons.Default.Computer, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenOnlineGames),
        )
    }
}
