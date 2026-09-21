package org.chessora.app.ui.calendar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraError
import org.chessora.app.ui.theme.ChessoraGold

private val ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE

/** [onOpenBando] apre il bando DENTRO l'app (BandoViewerScreen) - nessuna iscrizione
 * possibile da qui, solo consultazione (vedi ChessoraNavHost). */
@Composable
fun CalendarScreen(club: String, onOpenBando: (url: String) -> Unit) {
    val viewModel = chessoraViewModel { app -> CalendarViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val month by viewModel.month.collectAsState()
    val registeredTournamentIds by viewModel.registeredTournamentIds.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(club) { viewModel.load(club) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshRegisteredIds() }
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
            // Un giorno "lampeggia" se una delle sue righe è un torneo (o un fratello
            // dello stesso evento, vedi tournamentEventGroupId) a cui il chiamante
            // risulta preiscritto - stesso Set già usato in Home per il segno di spunta.
            val preRegisteredDates = events
                .filter { it.idTournament != null && it.idTournament in registeredTournamentIds }
                .mapTo(mutableSetOf()) { it.eventDate }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    MonthGrid(
                        month = month,
                        eventsByDate = eventsByDate,
                        preRegisteredDates = preRegisteredDates,
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
                            EventRow(
                                event,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                onClick = {
                                    scope.launch {
                                        val bandoUrl = viewModel.resolveBandoUrl(event, club)
                                        if (bandoUrl != null) {
                                            onOpenBando(bandoUrl)
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

// Griglia mensile stile calendario nativo Android (Samsung/AOSP): l'intero
// cerchio del giorno si colora (in tono tenue) se ci sono eventi quel giorno,
// il riquadro pieno oro è il giorno selezionato, il bordo oro è "oggi", il
// numero è colorato diversamente se il giorno è festivo (domenica o festività
// nazionale, vedi isItalianHoliday sotto).
@Composable
private fun MonthGrid(
    month: YearMonth,
    eventsByDate: Map<String, List<CalendarEvent>>,
    preRegisteredDates: Set<String>,
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
                                isPreRegistered = date.format(ISO_DATE) in preRegisteredDates,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                isHoliday = isItalianHoliday(date),
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
private fun DayCell(
    day: Int,
    hasEvents: Boolean,
    isPreRegistered: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isHoliday: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isSelected -> ChessoraGold
        hasEvents -> ChessoraGold.copy(alpha = 0.28f)
        else -> Color.Transparent
    }
    val numberColor = when {
        isSelected -> Color.White
        isHoliday -> ChessoraError
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(backgroundColor)
            .border(if (isToday && !isSelected) 1.5.dp else 0.dp, ChessoraGold, CircleShape)
            .let {
                // Bagliore lento e discreto sul bordo per il giorno a cui si è preiscritti -
                // un ciclo di ~2.4s tra appena visibile e ben visibile, mai un lampeggio a
                // scatti. Animazione infinita creata solo per i giorni preiscritti (non per
                // tutte le celle del mese), altrimenti gira inutilmente su ~42 celle.
                if (isPreRegistered) {
                    val infiniteTransition = rememberInfiniteTransition(label = "preRegisteredGlow")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(animation = tween(2400), repeatMode = RepeatMode.Reverse),
                        label = "preRegisteredGlowAlpha",
                    )
                    it.border(2.dp, ChessoraGold.copy(alpha = glowAlpha), CircleShape)
                } else {
                    it
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            color = numberColor,
            fontWeight = if (isToday || isSelected || isHoliday) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val FIXED_ITALIAN_HOLIDAYS = setOf(
    1 to 1, 1 to 6, 4 to 25, 5 to 1, 6 to 2, 8 to 15, 11 to 1, 12 to 8, 12 to 25, 12 to 26,
)

private fun isItalianHoliday(date: LocalDate): Boolean {
    if (date.dayOfWeek == DayOfWeek.SUNDAY) return true
    if ((date.monthValue to date.dayOfMonth) in FIXED_ITALIAN_HOLIDAYS) return true
    val easter = easterSunday(date.year)
    return date == easter || date == easter.plusDays(1)
}

private fun easterSunday(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = ((h + l - 7 * m + 114) % 31) + 1
    return LocalDate.of(year, month, day)
}

@Composable
private fun EventRow(event: CalendarEvent, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
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
