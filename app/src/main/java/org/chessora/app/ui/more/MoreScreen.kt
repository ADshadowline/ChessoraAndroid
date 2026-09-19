package org.chessora.app.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R

/**
 * Raccoglie le schermate secondarie (Calendario, Direttivo, Negozio,
 * Impostazioni) in un unico menu invece di occupare altri slot nella bottom
 * bar - vedi ui/navigation/ChessoraDestinations.kt. Il Calendario è stato
 * spostato qui (il suo slot in bottom bar ora è "Iscrizioni",
 * ui/registrations/) su richiesta esplicita. "La mia foto" è stata spostata
 * dentro Impostazioni (ui/settings/SettingsScreen.kt).
 */
@Composable
fun MoreScreen(
    onCalendarClick: () -> Unit,
    onRankingClick: () -> Unit,
    onPerformanceClick: () -> Unit,
    onBoardClick: () -> Unit,
    onShopClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isPlatformMode: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        // Calendario/Direttivo/Negozio sono concetti di UN circolo: nessun senso in
        // "modalità piattaforma" (nessun circolo scelto, vedi MembershipQuestionScreen)
        // - nascosti invece di far fallire una chiamata di rete club-scoped.
        if (!isPlatformMode) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_calendar)) },
                leadingContent = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onCalendarClick),
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.nav_ranking)) },
            leadingContent = { Icon(Icons.Default.Leaderboard, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onRankingClick),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.more_performance)) },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onPerformanceClick),
        )
        if (!isPlatformMode) {
            ListItem(
                headlineContent = { Text("Direttivo") },
                leadingContent = { Icon(Icons.Default.People, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onBoardClick),
            )
            ListItem(
                headlineContent = { Text("Negozio") },
                leadingContent = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onShopClick),
            )
        }
        ListItem(
            headlineContent = { Text("Impostazioni") },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onSettingsClick),
        )
    }
}
