package org.chessora.app.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

@Composable
fun HomeScreen(club: String) {
    val viewModel = chessoraViewModel { app -> HomeViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { data ->
        if (data.upcoming.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.home_no_upcoming))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text(
                        stringResource(R.string.home_upcoming_appointments),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                itemsIndexed(data.upcoming, key = { _, event -> event.id }) { index, event ->
                    AppointmentCard(
                        event = event,
                        isNext = index == 0,
                        onClick = {
                            scope.launch {
                                val bandoUrl = viewModel.resolveBandoUrl(event, club)
                                if (bandoUrl != null) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(bandoUrl)))
                                } else {
                                    Toast.makeText(context, context.getString(R.string.home_no_bando), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(event: CalendarEvent, isNext: Boolean, onClick: () -> Unit) {
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
            Text(
                event.title ?: event.eventTypeDescription ?: stringResource(R.string.home_generic_event),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(formatEventDateTime(event), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
            if (isNext) {
                CountdownText(event.eventDateTime)
            }
        }
    }
}

@Composable
private fun CountdownText(eventDateTime: String) {
    val target = remember(eventDateTime) { runCatching { LocalDateTime.parse(eventDateTime) }.getOrNull() } ?: return
    var remaining by remember(eventDateTime) { mutableStateOf("") }

    LaunchedEffect(eventDateTime) {
        while (true) {
            val duration = Duration.between(LocalDateTime.now(), target)
            if (duration.isNegative) { remaining = ""; break }
            val days = duration.toDays()
            val hours = duration.toHours() % 24
            val minutes = duration.toMinutes() % 60
            val seconds = duration.seconds % 60
            remaining = when {
                days > 0 -> "Tra ${days}g ${hours}h ${minutes}m"
                hours > 0 -> "Tra ${hours}h ${minutes}m ${seconds}s"
                else -> "Tra ${minutes}m ${seconds}s"
            }
            delay(1000)
        }
    }

    if (remaining.isNotEmpty()) {
        Text(
            remaining,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

private val eventDateFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ITALIAN)

private fun formatEventDateTime(event: CalendarEvent): String {
    val dateLabel = runCatching { LocalDate.parse(event.eventDate) }.getOrNull()
        ?.format(eventDateFormatter)
        ?.replaceFirstChar { it.uppercase() }
        ?: event.eventDate
    return if (event.startTime.isNotBlank()) "$dateLabel, ore ${event.startTime}" else dateLabel
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
