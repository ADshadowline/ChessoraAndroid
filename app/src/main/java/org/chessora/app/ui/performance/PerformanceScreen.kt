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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.chessora.app.ui.theme.ChessoraGold

// Colori scelti per essere ben leggibili sullo sfondo scuro dell'app (vedi
// ui/theme/Theme.kt: un solo colorScheme, sempre scuro) - il bug segnalato
// ("non si vede il punteggio standard") era proprio questo: il colore
// precedente per Standard coincideva quasi esattamente con lo sfondo.
private val PerfStandard = ChessoraCream
private val PerfRapid = ChessoraGold
private val PerfBlitz = Color(0xFFE0708A)

@Composable
fun PerformanceScreen(onIdentify: () -> Unit) {
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
            else -> PerformanceContent(data)
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
private fun PerformanceContent(data: PerformanceHistoryDto) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.performance_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        LatestValuesRow(data.points)
        Legend(modifier = Modifier.padding(top = 12.dp))
        Text(
            stringResource(R.string.performance_zoom_hint),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        EloChartCard(points = data.points, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun LatestValuesRow(points: List<PerformancePointDto>) {
    val latestStandard = points.lastOrNull { it.standard != null }?.standard
    val latestRapid = points.lastOrNull { it.rapid != null }?.rapid
    val latestBlitz = points.lastOrNull { it.blitz != null }?.blitz

    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        LatestValueCell(stringResource(R.string.performance_standard), latestStandard, PerfStandard, Modifier.weight(1f))
        LatestValueCell(stringResource(R.string.performance_rapid), latestRapid, PerfRapid, Modifier.weight(1f))
        LatestValueCell(stringResource(R.string.performance_blitz), latestBlitz, PerfBlitz, Modifier.weight(1f))
    }
}

@Composable
private fun LatestValueCell(label: String, value: Int?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            value?.toString() ?: "-",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(PerfStandard, stringResource(R.string.performance_standard))
        LegendItem(PerfRapid, stringResource(R.string.performance_rapid))
        LegendItem(PerfBlitz, stringResource(R.string.performance_blitz))
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
private fun EloChartCard(points: List<PerformancePointDto>, modifier: Modifier = Modifier) {
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

    val allValues = visiblePoints.flatMap { listOfNotNull(it.standard, it.rapid, it.blitz) }
        .ifEmpty { points.flatMap { listOfNotNull(it.standard, it.rapid, it.blitz) } }
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

                drawSeries({ it.standard }, PerfStandard)
                drawSeries({ it.rapid }, PerfRapid)
                drawSeries({ it.blitz }, PerfBlitz)

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
                SelectedPointTooltip(selected, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
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
private fun SelectedPointTooltip(point: PerformancePointDto, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text("${point.month.toString().padStart(2, '0')}/${point.year}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        point.standard?.let { TooltipValueLine(stringResource(R.string.performance_standard), it, PerfStandard) }
        point.rapid?.let { TooltipValueLine(stringResource(R.string.performance_rapid), it, PerfRapid) }
        point.blitz?.let { TooltipValueLine(stringResource(R.string.performance_blitz), it, PerfBlitz) }
    }
}

@Composable
private fun TooltipValueLine(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(" $label: $value", style = MaterialTheme.typography.labelSmall)
    }
}
