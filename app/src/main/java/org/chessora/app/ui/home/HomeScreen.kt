package org.chessora.app.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.ui.common.BackgroundImageWithScrim
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Voci di navigazione mostrate come icone quando l'utente ha scelto la
 * "Visualizzazione desktop" in Impostazioni (vedi ClubPreferences.displayMode) - stesse
 * destinazioni già raggiungibili da bottom bar/ui/more/MoreScreen.kt, solo un layout Home
 * alternativo: le singole schermate di destinazione restano invariate.
 */
data class DesktopHomeCallbacks(
    val onOpenEvents: () -> Unit,
    val onOpenCalendar: () -> Unit,
    val onOpenNews: () -> Unit,
    val onOpenRegistrations: () -> Unit,
    val onOpenMessaging: () -> Unit,
    val onOpenRanking: () -> Unit,
    val onOpenPerformance: () -> Unit,
    val onOpenBoard: () -> Unit,
    val onOpenShop: () -> Unit,
    val onOpenSettings: () -> Unit,
)

@Composable
fun HomeScreen(club: String?, onOpenTournament: (Int) -> Unit, desktop: DesktopHomeCallbacks, isPlatformMode: Boolean = false) {
    val viewModel = chessoraViewModel { app -> HomeViewModel(app.repository, app.clubPreferences) }
    val displayMode by viewModel.displayMode.collectAsState()
    val desktopBackgroundUri by viewModel.desktopBackgroundUri.collectAsState()
    val desktopIconOrder by viewModel.desktopIconOrder.collectAsState()
    val desktopHiddenIcons by viewModel.desktopHiddenIcons.collectAsState()

    if (displayMode == ClubPreferences.DISPLAY_MODE_DESKTOP) {
        Box(modifier = Modifier.fillMaxSize()) {
            desktopBackgroundUri?.let { BackgroundImageWithScrim(uri = it) }
            DesktopHomeGrid(
                desktop = desktop,
                isPlatformMode = isPlatformMode,
                iconOrder = desktopIconOrder,
                hiddenIcons = desktopHiddenIcons,
                onReorder = viewModel::setDesktopIconOrder,
            )
        }
    } else {
        EventsListScreen(club = club, onOpenTournament = onOpenTournament)
    }
}

/** Elenco eventi in ordine cronologico - contenuto della Home in visualizzazione classica
 * (vedi [HomeScreen]), raggiungibile anche dalla griglia di icone (route EVENTS, tile
 * "Eventi") quando la Home è in visualizzazione desktop. */
@Composable
fun EventsListScreen(club: String?, onOpenTournament: (Int) -> Unit) {
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
                            isRegistered = event.tournamentIds.any { it in data.registeredTournamentIds },
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

private data class DesktopIcon(val id: String, val labelRes: Int, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Descrittore statico di una icona della griglia Home desktop - id stabile usato per
 * persistere ordine/visibilità (vedi ClubPreferences.desktopIconOrder/desktopHiddenIcons)
 * e per l'elenco riordinabile di ui/settings/IconSettingsScreen.kt. Messaggi e
 * Impostazioni NON compaiono qui: restano ancorate agli angoli in basso per scelta
 * esplicita, non fanno parte dell'ordine/visibilità personalizzabile.
 */
data class DesktopIconDescriptor(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    /** true = l'icona non ha senso in "modalità piattaforma" (nessun circolo scelto,
     * vedi ClubPreferences.isPlatformMode) e viene sempre esclusa in quel caso, a
     * prescindere da ordine/visibilità salvati. */
    val hiddenInPlatformMode: Boolean = false,
)

val DESKTOP_ICON_DESCRIPTORS = listOf(
    DesktopIconDescriptor("events", R.string.nav_home, Icons.AutoMirrored.Filled.EventNote),
    DesktopIconDescriptor("calendar", R.string.more_calendar, Icons.Default.CalendarMonth, hiddenInPlatformMode = true),
    DesktopIconDescriptor("news", R.string.nav_news, Icons.Default.Newspaper),
    DesktopIconDescriptor("registrations", R.string.nav_registrations, Icons.Default.HowToReg),
    DesktopIconDescriptor("ranking", R.string.nav_ranking, Icons.Default.Leaderboard),
    DesktopIconDescriptor("performance", R.string.more_performance, Icons.AutoMirrored.Filled.ShowChart),
    DesktopIconDescriptor("board", R.string.desktop_icon_board, Icons.Default.People, hiddenInPlatformMode = true),
    DesktopIconDescriptor("shop", R.string.desktop_icon_shop, Icons.Default.ShoppingCart, hiddenInPlatformMode = true),
)

/** Applica ordine personalizzato e icone nascoste (vedi ClubPreferences) all'elenco di
 * default: un id salvato ma non più tra [defaults] (es. modalità piattaforma) viene
 * ignorato, uno nuovo non ancora salvato in [order] viene accodato in fondo. */
private fun applyIconPreferences(
    defaults: List<DesktopIconDescriptor>,
    order: List<String>,
    hidden: Set<String>,
): List<DesktopIconDescriptor> {
    val visible = defaults.filter { it.id !in hidden }
    if (order.isEmpty()) return visible
    val byId = visible.associateBy { it.id }
    return order.mapNotNull { byId[it] } + visible.filter { it.id !in order }
}

private fun callbackFor(id: String, desktop: DesktopHomeCallbacks): (() -> Unit)? = when (id) {
    "events" -> desktop.onOpenEvents
    "calendar" -> desktop.onOpenCalendar
    "news" -> desktop.onOpenNews
    "registrations" -> desktop.onOpenRegistrations
    "ranking" -> desktop.onOpenRanking
    "performance" -> desktop.onOpenPerformance
    "board" -> desktop.onOpenBoard
    "shop" -> desktop.onOpenShop
    else -> null
}

private const val DESKTOP_GRID_COLUMNS = 3
private val DESKTOP_GRID_SPACING = 12.dp

/**
 * Griglia riordinabile trascinando le icone con un dito (tieni premuto e sposta) -
 * stessa idea di ui/settings/IconSettingsScreen.kt (lista), qui adattata a una griglia
 * 2D: l'offset di trascinamento accumulato viene convertito in "quante celle" (righe *
 * colonne + colonne) tramite [DESKTOP_GRID_SPACING]/la dimensione di cella misurata a
 * runtime, poi l'elemento trascinato viene spostato in quella posizione nella lista -
 * un'approssimazione a "indice piatto" (non un vero drop-target per posizione XY), che
 * resta comunque naturale per un utente che trascina in una direzione. Il nuovo ordine
 * viene persistito solo al rilascio ([onReorder]), non a ogni micro-spostamento.
 */
@Composable
private fun DesktopHomeGrid(
    desktop: DesktopHomeCallbacks,
    isPlatformMode: Boolean,
    iconOrder: List<String>,
    hiddenIcons: Set<String>,
    onReorder: (List<String>) -> Unit,
) {
    // Messaggi e Impostazioni sono ancorate agli angoli in basso (sinistra/destra), non
    // parte della griglia scorrevole - posizione fissa richiesta esplicitamente, non
    // riordinabili/nascondibili da Impostazioni > Icone Home.
    val baseIcons = remember(desktop, isPlatformMode, iconOrder, hiddenIcons) {
        val defaults = DESKTOP_ICON_DESCRIPTORS.filter { !(isPlatformMode && it.hiddenInPlatformMode) }
        applyIconPreferences(defaults, iconOrder, hiddenIcons).mapNotNull { descriptor ->
            callbackFor(descriptor.id, desktop)?.let { onClick ->
                DesktopIcon(descriptor.id, descriptor.labelRes, descriptor.icon, onClick)
            }
        }
    }
    // Durante un trascinamento è questa lista, non [baseIcons], a decidere l'ordine
    // mostrato a schermo (aggiornata subito, prima che il DataStore confermi la
    // scrittura - altrimenti l'icona trascinata "scatterebbe" indietro al rilascio).
    var icons by remember(baseIcons) { mutableStateOf(baseIcons) }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var cellStepPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val spacingPx = with(density) { DESKTOP_GRID_SPACING.toPx() }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(DESKTOP_GRID_COLUMNS),
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .onSizeChanged { size ->
                    val cellWidthPx = (size.width - spacingPx * (DESKTOP_GRID_COLUMNS - 1)) / DESKTOP_GRID_COLUMNS
                    cellStepPx = cellWidthPx + spacingPx
                },
            horizontalArrangement = Arrangement.spacedBy(DESKTOP_GRID_SPACING),
            verticalArrangement = Arrangement.spacedBy(DESKTOP_GRID_SPACING),
        ) {
            items(icons, key = { it.id }) { entry ->
                val isDragging = draggingId == entry.id
                DesktopIconTile(
                    entry,
                    modifier = Modifier
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                            }
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .pointerInput(entry.id, icons) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = entry.id
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                    val step = cellStepPx
                                    if (step <= 0f) return@detectDragGesturesAfterLongPress
                                    val currentIndex = icons.indexOfFirst { it.id == entry.id }
                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress
                                    val colDelta = (dragOffset.x / step).roundToInt()
                                    val rowDelta = (dragOffset.y / step).roundToInt()
                                    val indexDelta = rowDelta * DESKTOP_GRID_COLUMNS + colDelta
                                    val targetIndex = (currentIndex + indexDelta).coerceIn(0, icons.lastIndex)
                                    if (targetIndex != currentIndex) {
                                        icons = icons.toMutableList().apply { add(targetIndex, removeAt(currentIndex)) }
                                        // Compensa l'offset per la porzione di trascinamento già "consumata" dallo
                                        // spostamento appena applicato - stesso trucco della lista in
                                        // IconSettingsScreen.kt, qui scomposto in riga/colonna: la divisione/resto
                                        // intera ricostruisce sempre esattamente lo spostamento applicato
                                        // (targetIndex - currentIndex), qualunque sia il segno.
                                        val consumed = targetIndex - currentIndex
                                        dragOffset -= Offset(
                                            x = (consumed % DESKTOP_GRID_COLUMNS) * step,
                                            y = (consumed / DESKTOP_GRID_COLUMNS) * step,
                                        )
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffset = Offset.Zero
                                    onReorder(icons.map { it.id })
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DesktopIconTile(
                DesktopIcon("messaging", R.string.nav_messaging, Icons.AutoMirrored.Filled.Chat, desktop.onOpenMessaging),
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 120.dp),
            )
            DesktopIconTile(
                DesktopIcon("settings", R.string.settings_title, Icons.Default.Settings, desktop.onOpenSettings),
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 120.dp),
            )
        }
    }
}

@Composable
private fun DesktopIconTile(entry: DesktopIcon, modifier: Modifier = Modifier) {
    Card(onClick = entry.onClick, modifier = modifier.aspectRatio(1f)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(entry.icon, contentDescription = null, modifier = Modifier.size(36.dp))
            Text(
                stringResource(entry.labelRes),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun AppointmentCard(event: UpcomingEvent, isNext: Boolean, isRegistered: Boolean, onClick: () -> Unit) {
    val imageUrl = NetworkModule.resolveAssetUrl(event.tournamentImmagineCopertinaPath)
    // Con un'immagine di sfondo il testo va forzato chiaro (leggibile sopra lo scrim
    // scuro sempre applicato) invece di seguire il tema/i colori "isNext" di sotto.
    val onImageColor = Color.White
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = if (imageUrl != null) {
            CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = onImageColor)
        } else if (isNext) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Box {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)))
            }
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
                            tint = if (imageUrl != null) onImageColor else MaterialTheme.colorScheme.primary,
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
