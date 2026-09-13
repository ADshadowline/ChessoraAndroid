package org.chessora.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraGold

private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE

@Composable
fun CalendarScreen(club: String) {
    val viewModel = chessoraViewModel { app -> CalendarViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val month by viewModel.month.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    LaunchedEffect(club) { viewModel.load(club) }
    // Se l'utente naviga a un mese diverso, la selezione del giorno precedente
    // non ha più senso (potrebbe non esistere in quel mese) - riparte da "oggi"
    // se il mese mostrato è quello corrente, altrimenti nessun giorno selezionato.
    LaunchedEffect(month) {
        selectedDate = LocalDate.now().takeIf { YearMonth.from(it) == month }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthHeader(month = month, onPrevious = { viewModel.goToMonth(-1) }, onNext = { viewModel.goToMonth(1) })
        UiStateContent(state = state, onRetry = { viewModel.retry() }) { events ->
            val eventsByDate = events.groupBy { it.eventDate }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    MonthGrid(
                        month = month,
                        eventsByDate = eventsByDate,
                        selectedDate = selectedDate,
                        onDaySelected = { selectedDate = it },
                    )
                }
                val selected = selectedDate
                if (selected != null) {
                    val dayEvents = eventsByDate[selected.format(ISO_DATE)].orEmpty()
                    item {
                        Text(
                            selected.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    if (dayEvents.isEmpty()) {
                        item {
                            Text(
                                "Nessun evento in programma.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        items(dayEvents, key = { it.id }) { event ->
                            EventRow(event, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Mese precedente") }
        Text(
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN)).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = "Mese successivo") }
    }
}

// Griglia mensile stile calendario nativo Android (Samsung/AOSP): un pallino
// sotto il numero del giorno segnala che ci sono eventi quel giorno, il
// riquadro pieno oro è il giorno selezionato, il bordo oro è "oggi".
@Composable
private fun MonthGrid(
    month: YearMonth,
    eventsByDate: Map<String, List<CalendarEvent>>,
    selectedDate: LocalDate?,
    onDaySelected: (LocalDate) -> Unit,
) {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1 // Lunedì=1 -> 0 celle vuote
    val daysInMonth = month.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val weeks = (totalCells + 6) / 7
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (dow in 1..7) {
                Text(
                    DayOfWeek.of(dow).getDisplayName(TextStyle.NARROW, Locale.ITALIAN).uppercase(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (week in 0 until weeks) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val dayNum = week * 7 + col - leadingBlanks + 1
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val date = month.atDay(dayNum)
                            DayCell(
                                day = dayNum,
                                hasEvents = eventsByDate.containsKey(date.format(ISO_DATE)),
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                onClick = { onDaySelected(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, hasEvents: Boolean, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (isSelected) ChessoraGold else Color.Transparent)
            .border(if (isToday && !isSelected) 1.5.dp else 0.dp, ChessoraGold, CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            day.toString(),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .clip(CircleShape)
                .background(if (hasEvents) (if (isSelected) Color.White else ChessoraGold) else Color.Transparent),
        )
    }
}

@Composable
private fun EventRow(event: CalendarEvent, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
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
