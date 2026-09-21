package org.chessora.app.ui.performance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.chessora.app.R
import org.chessora.app.data.remote.dto.PerformanceHistoryDto
import org.chessora.app.data.remote.dto.PerformancePointDto
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraCream
import org.chessora.app.ui.theme.ChessoraError
import org.chessora.app.ui.theme.ChessoraGold
import org.chessora.app.ui.theme.ChessoraGreen

// Colori scelti per essere ben leggibili sullo sfondo scuro dell'app (vedi
// ui/theme/Theme.kt: un solo colorScheme, sempre scuro) - il bug segnalato
// ("non si vede il punteggio standard") era proprio questo: il colore
// precedente per Standard coincideva quasi esattamente con lo sfondo. Non
// private: riusati anche per i badge Elo nell'header (ChessoraNavHost.kt).
val PerfStandard = ChessoraCream
val PerfRapid = ChessoraGold
val PerfBlitz = Color(0xFFE0708A)

/** Cadenza su cui la schermata apre "a fuoco" quando si arriva da un punteggio Elo
 * specifico cliccato in barra (vedi ChessoraNavHost.ClubBrandingTopBar) - null mostra
 * tutte e tre le cadenze insieme, il comportamento preesistente. */
enum class EloRatingType(val routeValue: String) {
    STANDARD("standard"), RAPID("rapid"), BLITZ("blitz");

    companion object {
        fun fromRouteValue(value: String?): EloRatingType? = entries.firstOrNull { it.routeValue == value }
    }
}

private fun EloRatingType.selector(): (PerformancePointDto) -> Int? = when (this) {
    EloRatingType.STANDARD -> { p -> p.standard }
    EloRatingType.RAPID -> { p -> p.rapid }
    EloRatingType.BLITZ -> { p -> p.blitz }
}

fun EloRatingType.color(): Color = when (this) {
    EloRatingType.STANDARD -> PerfStandard
    EloRatingType.RAPID -> PerfRapid
    EloRatingType.BLITZ -> PerfBlitz
}

/** Icona che rappresenta la cadenza - stessa associazione ovunque compaia un punteggio
 * Elo (qui, e nei badge dell'header in ChessoraNavHost.kt): orologio per lo Standard
 * (tempo lungo e disteso), un fulmine "attenuato" per il Rapid, un fulmine pieno per il
 * Blitz (il più veloce). */
fun EloRatingType.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    EloRatingType.STANDARD -> Icons.Default.AccessTime
    EloRatingType.RAPID -> Icons.Default.Speed
    EloRatingType.BLITZ -> Icons.Default.Bolt
}

@Composable
private fun EloRatingType.label(): String = when (this) {
    EloRatingType.STANDARD -> stringResource(R.string.performance_standard)
    EloRatingType.RAPID -> stringResource(R.string.performance_rapid)
    EloRatingType.BLITZ -> stringResource(R.string.performance_blitz)
}

/** Intervallo mostrato nel grafico/valori attuali - filtrato lato client (la serie è
 * comunque un solo punto al mese, mai troppo grande) rispetto all'ULTIMO mese con dati
 * disponibili (non alla data odierna reale): l'aggiornamento Elo può essere indietro di
 * qualche mese, ancorare a "oggi" farebbe apparire vuoto un filtro "6 mesi" se l'ultimo
 * aggiornamento risalisse a più di 6 mesi fa. */
enum class PerformancePeriod(val months: Int?) {
    SIX_MONTHS(6), ONE_YEAR(12), FIVE_YEARS(60), ALL(null);
}

@Composable
private fun PerformancePeriod.label(): String = when (this) {
    PerformancePeriod.SIX_MONTHS -> stringResource(R.string.performance_period_6m)
    PerformancePeriod.ONE_YEAR -> stringResource(R.string.performance_period_1y)
    PerformancePeriod.FIVE_YEARS -> stringResource(R.string.performance_period_5y)
    PerformancePeriod.ALL -> stringResource(R.string.performance_period_all)
}

private fun filterByPeriod(points: List<PerformancePointDto>, period: PerformancePeriod): List<PerformancePointDto> {
    val months = period.months ?: return points
    val last = points.lastOrNull() ?: return points
    val lastOrdinal = last.year * 12 + last.month
    val cutoff = lastOrdinal - months
    return points.filter { it.year * 12 + it.month > cutoff }
}

@Composable
fun PerformanceScreen(onIdentify: () -> Unit, focus: EloRatingType? = null) {
    val viewModel = chessoraViewModel { app -> PerformanceViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    UiStateContent(state = state, onRetry = { viewModel.load() }) { data ->
        when {
            data == null -> CenteredMessage(
                message = stringResource(R.string.performance_not_identified),
                actionLabel = stringResource(R.string.settings_identity_button),
                onAction = onIdentify,
            )
            !data.hasFide -> CenteredMessage(message = stringResource(R.string.performance_no_fide))
            data.points.isEmpty() -> CenteredMessage(message = stringResource(R.string.performance_empty))
            else -> PerformanceContent(data, focus)
        }
    }
}

@Composable
private fun CenteredMessage(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 16.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
private fun PerformanceContent(data: PerformanceHistoryDto, focus: EloRatingType?) {
    var selectedPeriod by remember { mutableStateOf(PerformancePeriod.ALL) }
    val filteredPoints = remember(data.points, selectedPeriod) { filterByPeriod(data.points, selectedPeriod) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.performance_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        PeriodSelector(selectedPeriod, onSelect = { selectedPeriod = it }, modifier = Modifier.padding(top = 8.dp))
        LatestValuesRow(filteredPoints, focus)
        Legend(focus, modifier = Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.performance_zoom_hint),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        // key() forza EloChartCard a ripartire da zero (zoom/pan/selezione azzerati)
        // quando cambia il periodo, invece di mantenere una finestra di zoom pensata
        // per la lista di punti precedente (di lunghezza diversa).
        key(selectedPeriod) {
            EloChartCard(points = filteredPoints, focus = focus, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PeriodSelector(selected: PerformancePeriod, onSelect: (PerformancePeriod) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PerformancePeriod.entries.forEach { period ->
            FilterChip(
                selected = period == selected,
                onClick = { onSelect(period) },
                label = { Text(period.label()) },
            )
        }
    }
}

@Composable
private fun LatestValuesRow(points: List<PerformancePointDto>, focus: EloRatingType?) {
    val types = focus?.let { listOf(it) } ?: EloRatingType.entries
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        types.forEach { type ->
            // Ultimi due valori non-null della serie (nel grafico attualmente mostrato,
            // vedi PerformancePeriod) - il penultimo è il "valore precedente" a cui si
            // confronta la freccia su/giù.
            val nonNullValues = points.mapNotNull { type.selector()(it) }
            val latest = nonNullValues.lastOrNull()
            val previous = if (nonNullValues.size >= 2) nonNullValues[nonNullValues.size - 2] else null
            LatestValueCell(type.label(), latest, previous, type.color(), Modifier.weight(1f))
        }
    }
}

@Composable
private fun LatestValueCell(label: String, value: Int?, previous: Int?, color: Color, modifier: Modifier = Modifier) {
    val delta = if (value != null && previous != null) value - previous else null
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value?.toString() ?: "-",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            if (delta != null && delta != 0) {
                val isUp = delta > 0
                val deltaColor = if (isUp) ChessoraGreen else ChessoraError
                Icon(
                    if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = stringResource(if (isUp) R.string.performance_delta_up else R.string.performance_delta_down),
                    tint = deltaColor,
                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                )
                Text(
                    (if (isUp) "+$delta" else "$delta"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor,
                )
            }
        }
    }
}

@Composable
private fun Legend(focus: EloRatingType?, modifier: Modifier = Modifier) {
    val types = focus?.let { listOf(it) } ?: EloRatingType.entries
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        types.forEach { type -> LegendItem(type.color(), type.label()) }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun monthLabel(point: PerformancePointDto): String {
    val shortYear = point.year % 100
    return "${point.month.toString().padStart(2, '0')}/$shortYear"
}

/**
 * Scheda che ospita il grafico vero e proprio più i controlli attorno (pulsante
 * "reset zoom", suggerimento). Lo stato di zoom/scorrimento/punto selezionato
 * vive qui (non nel ViewModel: è pura interazione UI, non dati).
 */
@Composable
private fun EloChartCard(points: List<PerformancePointDto>, focus: EloRatingType?, modifier: Modifier = Modifier) {
    var visibleFraction by remember { mutableFloatStateOf(1f) }
    var startFraction by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val minVisibleFraction = (6f / points.size).coerceAtMost(1f)
    val zoomed = visibleFraction < 0.999f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            EloLineChart(
                points = points,
                focus = focus,
                visibleFraction = visibleFraction,
                startFraction = startFraction,
                selectedIndex = selectedIndex,
                minVisibleFraction = minVisibleFraction,
                onTransform = { zoomChange, panDeltaFraction ->
                    val newVisible = (visibleFraction / zoomChange).coerceIn(minVisibleFraction, 1f)
                    val center = startFraction + visibleFraction / 2f
                    var newStart = center - newVisible / 2f + panDeltaFraction
                    newStart = newStart.coerceIn(0f, (1f - newVisible).coerceAtLeast(0f))
                    visibleFraction = newVisible
                    startFraction = newStart
                },
                onTap = { index -> selectedIndex = index },
                modifier = Modifier.fillMaxWidth().height(260.dp),
            )
        }
        if (zoomed) {
            IconButton(
                onClick = { visibleFraction = 1f; startFraction = 0f; selectedIndex = null },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.performance_reset_zoom))
            }
        }
    }
}

/**
 * Grafico a linee disegnato a mano su Canvas (nessuna libreria di grafici: coerente
 * con la filosofia di dipendenze minime del progetto). Tre serie (Standard/Rapid/
 * Blitz), un punto per mese - i mesi senza rating in una cadenza interrompono
 * quella linea invece di scendere a zero. Pizzico per zoomare (la scala Y si
 * riadatta alla sola finestra visibile), trascinamento per scorrere, tocco per
 * vedere data e punteggi esatti di un punto.
 */
@Composable
private fun EloLineChart(
    points: List<PerformancePointDto>,
    focus: EloRatingType?,
    visibleFraction: Float,
    startFraction: Float,
    selectedIndex: Int?,
    minVisibleFraction: Float,
    onTransform: (zoomChange: Float, panDeltaFraction: Float) -> Unit,
    onTap: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = points.size
    val startIndex = (startFraction * n).roundToInt().coerceIn(0, n - 1)
    val visibleCount = (visibleFraction * n).roundToInt().coerceIn(minOf(2, n), n - startIndex)
    val visiblePoints = points.subList(startIndex, (startIndex + visibleCount).coerceAtMost(n))

    val relevantValues: (PerformancePointDto) -> List<Int> = { p ->
        focus?.let { listOfNotNull(it.selector()(p)) } ?: listOfNotNull(p.standard, p.rapid, p.blitz)
    }
    val allValues = visiblePoints.flatMap(relevantValues).ifEmpty { points.flatMap(relevantValues) }
    if (allValues.isEmpty()) return

    val minY = allValues.min() - 20
    val maxY = (allValues.max() + 20).coerceAtLeast(minY + 1)
    val range = maxY - minY

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val axisTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val selected = selectedIndex?.let { idx -> points.getOrNull(idx) }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            val panFraction = -pan.x / size.width.toFloat() * visibleFraction
                            onTransform(zoomChange, panFraction)
                        }
                    }
                    .pointerInput(visiblePoints, startIndex) {
                        detectTapGestures { offset ->
                            if (visiblePoints.size < 2) return@detectTapGestures
                            val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val localIndex = (fraction * (visiblePoints.size - 1)).roundToInt()
                            onTap(startIndex + localIndex)
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val count = visiblePoints.size

                fun xFor(localIndex: Int) = if (count <= 1) w / 2f else w * localIndex / (count - 1).toFloat()
                fun yFor(value: Int) = h - (value - minY).toFloat() / range * h

                repeat(5) { i ->
                    val y = h * i / 4f
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
                }

                fun drawSeries(selector: (PerformancePointDto) -> Int?, color: Color) {
                    var last: Offset? = null
                    visiblePoints.forEachIndexed { index, point ->
                        val value = selector(point)
                        if (value == null) {
                            last = null
                            return@forEachIndexed
                        }
                        val current = Offset(xFor(index), yFor(value))
                        val previous = last
                        if (previous != null) {
                            drawLine(color = color, start = previous, end = current, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                        }
                        drawCircle(color = color, radius = 3.dp.toPx(), center = current)
                        last = current
                    }
                }

                if (focus != null) {
                    drawSeries(focus.selector(), focus.color())
                } else {
                    drawSeries({ it.standard }, PerfStandard)
                    drawSeries({ it.rapid }, PerfRapid)
                    drawSeries({ it.blitz }, PerfBlitz)
                }

                if (selected != null) {
                    val localIndex = points.indexOf(selected) - startIndex
                    if (localIndex in 0 until count) {
                        val x = xFor(localIndex)
                        drawLine(
                            color = axisTextColor,
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
            }

            if (selected != null) {
                SelectedPointTooltip(selected, focus, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(monthLabel(visiblePoints.first()), style = MaterialTheme.typography.labelSmall, color = axisTextColor, modifier = Modifier.weight(1f))
            Text(
                monthLabel(visiblePoints.last()),
                style = MaterialTheme.typography.labelSmall,
                color = axisTextColor,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SelectedPointTooltip(point: PerformancePointDto, focus: EloRatingType?, modifier: Modifier = Modifier) {
    val types = focus?.let { listOf(it) } ?: EloRatingType.entries
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text("${point.month.toString().padStart(2, '0')}/${point.year}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        types.forEach { type -> type.selector()(point)?.let { TooltipValueLine(type.label(), it, type.color()) } }
    }
}

@Composable
private fun TooltipValueLine(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(" $label: $value", style = MaterialTheme.typography.labelSmall)
    }
}
