package org.chessora.app.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDate

@Composable
fun CalendarScreen(club: String) {
    val viewModel = chessoraViewModel { app -> CalendarViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { events ->
        if (events.isEmpty()) {
            Text(
                "Nessun evento in programma nei prossimi 90 giorni.",
                modifier = Modifier.fillMaxSize().padding(16.dp),
            )
            return@UiStateContent
        }

        // Raggruppa per giorno (eventDate è già una stringa "yyyy-MM-dd" pronta
        // dal server, vedi CalendarDtos.kt) cosi' più eventi lo stesso giorno
        // condividono un'unica intestazione di data.
        val grouped = events.groupBy { it.eventDate }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            grouped.forEach { (date, dayEvents) ->
                item {
                    Text(
                        date.toItalianDate(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(dayEvents, key = { it.id }) { event -> EventRow(event) }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(modifier = Modifier.padding(12.dp)) {
            Column {
                Text(event.title ?: event.eventTypeDescription ?: "Evento", fontWeight = FontWeight.Bold)
                Text(event.startTime, style = MaterialTheme.typography.bodyMedium)
                event.note?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
