package org.chessora.app.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

@Composable
fun HomeScreen(club: String?, onOpenTournament: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> HomeViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { data ->
        val filtered = remember(data.upcoming, query) {
            if (query.isBlank()) data.upcoming else data.upcoming.filter { it.title.contains(query, ignoreCase = true) }
        }
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                placeholder = { Text(stringResource(R.string.home_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (query.isBlank()) stringResource(R.string.home_no_upcoming) else stringResource(R.string.home_no_results))
                }
            } else {
                Text(
                    stringResource(R.string.home_upcoming_appointments),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(filtered, key = { _, event -> event.key }) { index, event ->
                        AppointmentCard(
                            event = event,
                            isNext = index == 0 && query.isBlank(),
                            isRegistered = event.idTournament != null && event.idTournament in data.registeredTournamentIds,
                            onClick = {
                                val idTournament = event.idTournament
                                if (idTournament != null) {
                                    onOpenTournament(idTournament)
                                } else {
                                    scope.launch {
                                        val bandoUrl = viewModel.resolveBandoUrl(event, club)
                                        if (bandoUrl != null) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bandoUrl)))
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.home_no_bando), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(event: UpcomingEvent, isNext: Boolean, isRegistered: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = if (isNext) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    event.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isRegistered) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.home_registered_check),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(formatEventRange(event), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            if (isNext) {
                CountdownTimer(event.startDateTime)
            }
        }
    }
}

/**
 * Conto alla rovescia "d'impatto" per il prossimo appuntamento: sempre tutte e
 * quattro le unità (giorni/ore/minuti/secondi), ciascuna in un riquadro con
 * numero grande - non solo una riga di testo piccola, come richiesto.
 */
@Composable
private fun CountdownTimer(eventDateTime: String) {
    val target = remember(eventDateTime) { runCatching { LocalDateTime.parse(eventDateTime) }.getOrNull() } ?: return
    var days by remember(eventDateTime) { mutableStateOf(0L) }
    var hours by remember(eventDateTime) { mutableStateOf(0L) }
    var minutes by remember(eventDateTime) { mutableStateOf(0L) }
    var seconds by remember(eventDateTime) { mutableStateOf(0L) }
    var expired by remember(eventDateTime) { mutableStateOf(false) }

    LaunchedEffect(eventDateTime) {
        while (true) {
            val duration = Duration.between(LocalDateTime.now(), target)
            if (duration.isNegative) {
                expired = true
                break
            }
            days = duration.toDays()
            hours = duration.toHours() % 24
            minutes = duration.toMinutes() % 60
            seconds = duration.seconds % 60
            delay(1000)
        }
    }

    if (expired) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CountdownUnit(value = days, label = stringResource(R.string.home_countdown_days), modifier = Modifier.weight(1f))
        CountdownUnit(value = hours, label = stringResource(R.string.home_countdown_hours), modifier = Modifier.weight(1f))
        CountdownUnit(value = minutes, label = stringResource(R.string.home_countdown_minutes), modifier = Modifier.weight(1f))
        CountdownUnit(value = seconds, label = stringResource(R.string.home_countdown_seconds), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CountdownUnit(value: Long, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

private val eventDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN)

private fun formatEventRange(event: UpcomingEvent): String {
    val start = runCatching { LocalDateTime.parse(event.startDateTime) }.getOrNull()
    val end = runCatching { LocalDateTime.parse(event.endDateTime) }.getOrNull()
    val startLabel = start?.toLocalDate()?.format(eventDateFormatter)?.replaceFirstChar { it.uppercase() } ?: event.startDateTime
    val withTime = if (event.startTime.isNotBlank()) "$startLabel, ore ${event.startTime}" else startLabel
    if (end == null || start == null || end.toLocalDate() == start.toLocalDate()) return withTime
    val endLabel = end.toLocalDate().format(eventDateFormatter)
    return "$startLabel – $endLabel"
}

@Composable
fun NewsSummaryCard(article: NewsArticle, onClick: () -> Unit) {
    val imageUrl = NetworkModule.resolveAssetUrl(article.image)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (imageUrl != null) 12.dp else 0.dp)) {
                Text(article.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (article.excerpt.isNotBlank()) {
                    Text(article.excerpt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
