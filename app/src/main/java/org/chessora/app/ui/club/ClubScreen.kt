package org.chessora.app.ui.club

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Sotto-menu "Il Circolo" (voce unica in Home/Altro che prima apriva direttamente
 * BoardScreen, vedi HomeScreen.kt/MoreScreen.kt) - Direttivo, Statuto, Contatti, Dove
 * raggiungerlo, tutti dati già disponibili dal blob site-settings tranne Direttivo (che
 * resta la sua schermata dedicata, invariata). */
@Composable
fun ClubScreen(
    club: String,
    onOpenDirettivo: () -> Unit,
    onOpenStatuto: (url: String) -> Unit,
    onOpenContatti: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenVideo: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenRanking: () -> Unit,
) {
    val viewModel = chessoraViewModel { app -> ClubViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { settings ->
        val statutoUrl = NetworkModule.resolveAssetUrl(settings.statuto.path)
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.desktop_icon_board)) },
                leadingContent = { Icon(Icons.Default.Groups, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenDirettivo),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.club_menu_statuto)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (statutoUrl != null) {
                        onOpenStatuto(statutoUrl)
                    } else {
                        Toast.makeText(context, context.getString(R.string.club_menu_statuto_empty), Toast.LENGTH_SHORT).show()
                    }
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.club_menu_contacts)) },
                leadingContent = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenContatti),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.club_menu_location)) },
                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenLocation),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_home)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenEvents),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.more_calendar)) },
                leadingContent = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenCalendar),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.desktop_icon_video)) },
                leadingContent = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenVideo),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_news)) },
                leadingContent = { Icon(Icons.Default.Newspaper, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenNews),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_ranking)) },
                leadingContent = { Icon(Icons.Default.Leaderboard, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenRanking),
            )
        }
    }
}
