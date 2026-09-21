package org.chessora.app.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
fun HomeScreen(
    club: String?,
    onOpenTournament: (Int) -> Unit,
    desktop: DesktopHomeCallbacks,
    isPlatformMode: Boolean = false,
    registeredTournamentsCount: Int = 0,
) {
    val viewModel = chessoraViewModel { app -> HomeViewModel(app.repository, app.clubPreferences) }
    val displayMode by viewModel.displayMode.collectAsState()
    val desktopBackgroundUri by viewModel.desktopBackgroundUri.collectAsState()
    val desktopIconOrder by viewModel.desktopIconOrder.collectAsState()
    val desktopHiddenIcons by viewModel.desktopHiddenIcons.collectAsState()

    if (displayMode == ClubPreferences.DISPLAY_MODE_DESKTOP) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Come SplashScreen: se l'utente non ha scelto un proprio sfondo da
            // Impostazioni, mostra quello incluso nell'app invece di lasciare la griglia
            // su sfondo vuoto.
            if (desktopBackgroundUri != null) {
                BackgroundImageWithScrim(uri = desktopBackgroundUri!!)
            } else {
                BackgroundImageWithScrim(painter = painterResource(R.drawable.desktop_background_default))
            }
            DesktopHomeGrid(
                desktop = desktop,
                isPlatformMode = isPlatformMode,
                iconOrder = desktopIconOrder,
                hiddenIcons = desktopHiddenIcons,
                onReorder = viewModel::setDesktopIconOrder,
                registeredTournamentsCount = registeredTournamentsCount,
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
    // Tornare qui dal dettaglio di un torneo (dopo una (pre)iscrizione/ritiro) non
    // ricrea questo ViewModel - senza questo, il segno di spunta resterebbe quello di
    // prima di aver toccato "Preiscriviti"/"Ritira".
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshRegisteredIds() }

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

private data class DesktopIcon(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
    /** Numero mostrato come badge sull'icona (0 = nessun badge) - solo "Iscrizioni ai
     * tornei" ne ha uno per ora, vedi DesktopHomeGrid. */
    val badgeCount: Int = 0,
)

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
 * Griglia riordinabile trascinando le icone con un dito (tieni premuto e sposta), pensata
 * per sentirsi come la Home di Android: al tocco prolungato una piccola vibrazione
 * ([HapticFeedbackType.LongPress]) conferma la presa, l'icona trascinata si "alza"
 * (zoom + ombra, entrambi animati con la stessa molla vivace) e SEGUE LIBERAMENTE IL DITO
 * per tutta la durata del trascinamento (nessun riordino delle altre icone finché non la
 * rilasci - restano ferme, a differenza di una versione precedente che le "spostava"
 * già durante il trascinamento). Solo al rilascio si calcola la cella più vicina
 * (riga/colonna a partire dalla posizione ATTUALE dell'icona, clampate separatamente ai
 * bordi della griglia così un rilascio vicino al bordo non "sborda" mai nella riga sopra/
 * sotto per un riporto della divisione intera) e l'icona vi si "sistema" con una piccola
 * molla ([spring]), mentre le altre scorrono nella loro nuova posizione con
 * un'animazione fluida ([Modifier.animateItem]). Il nuovo ordine viene persistito solo al
 * rilascio ([onReorder]).
 */
@Composable
private fun DesktopHomeGrid(
    desktop: DesktopHomeCallbacks,
    isPlatformMode: Boolean,
    iconOrder: List<String>,
    hiddenIcons: Set<String>,
    onReorder: (List<String>) -> Unit,
    registeredTournamentsCount: Int = 0,
) {
    // Messaggi e Impostazioni sono ancorate agli angoli in basso (sinistra/destra), non
    // parte della griglia scorrevole - posizione fissa richiesta esplicitamente, non
    // riordinabili/nascondibili da Impostazioni > Icone Home.
    val baseIcons = remember(desktop, isPlatformMode, iconOrder, hiddenIcons, registeredTournamentsCount) {
        val defaults = DESKTOP_ICON_DESCRIPTORS.filter { !(isPlatformMode && it.hiddenInPlatformMode) }
        applyIconPreferences(defaults, iconOrder, hiddenIcons).mapNotNull { descriptor ->
            callbackFor(descriptor.id, desktop)?.let { onClick ->
                val badgeCount = if (descriptor.id == "registrations") registeredTournamentsCount else 0
                DesktopIcon(descriptor.id, descriptor.labelRes, descriptor.icon, onClick, badgeCount)
            }
        }
    }
    // Durante un trascinamento è questa lista, non [baseIcons], a decidere l'ordine
    // mostrato a schermo (aggiornata subito, prima che il DataStore confermi la
    // scrittura - altrimenti l'icona trascinata "scatterebbe" indietro al rilascio).
    var icons by remember(baseIcons) { mutableStateOf(baseIcons) }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var cellStepPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val spacingPx = with(density) { DESKTOP_GRID_SPACING.toPx() }
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val liftSpec = remember { spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) }

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
                // Un Animatable (non un semplice mutableStateOf) perché al rilascio l'offset
                // residuo deve tornare a zero con una piccola animazione (animateTo) invece di
                // scattare - durante il trascinamento attivo si usa invece snapTo, che non
                // anima: il dito deve essere seguito senza alcun ritardo percepibile.
                val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                val scale by animateFloatAsState(if (isDragging) 1.08f else 1f, animationSpec = liftSpec, label = "desktopIconScale")
                val elevation by animateFloatAsState(if (isDragging) 12f else 0f, animationSpec = liftSpec, label = "desktopIconElevation")

                DesktopIconTile(
                    entry,
                    modifier = Modifier
                        .then(if (isDragging) Modifier else Modifier.animateItem())
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation
                            translationX = offset.value.x
                            translationY = offset.value.y
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .pointerInput(entry.id, icons) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = entry.id
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    // Nessun calcolo di cella qui: durante il trascinamento l'icona segue
                                    // il dito liberamente, punto e basta - il riordino avviene solo al
                                    // rilascio (onDragEnd).
                                    change.consume()
                                    coroutineScope.launch { offset.snapTo(offset.value + amount) }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    val finalOffset = offset.value
                                    val step = cellStepPx
                                    val currentIndex = icons.indexOfFirst { it.id == entry.id }
                                    // Compenso da applicare con snapTo PRIMA della molla verso zero (nella
                                    // stessa coroutine, per garantire l'ordine: Animatable ammette una sola
                                    // operazione alla volta, un secondo launch separato potrebbe intrecciarsi).
                                    var settleFrom = finalOffset
                                    if (step > 0f && currentIndex != -1) {
                                        // Riga/colonna ATTUALI dell'icona trascinata, non un indice piatto: il
                                        // target è calcolato clampando riga e colonna separatamente ai bordi
                                        // della griglia, cosi' un rilascio vicino al bordo non "sborda" mai
                                        // nella riga sopra/sotto per effetto del riporto della divisione intera.
                                        val currentRow = currentIndex / DESKTOP_GRID_COLUMNS
                                        val currentCol = currentIndex % DESKTOP_GRID_COLUMNS
                                        val colDelta = (finalOffset.x / step).roundToInt()
                                        val rowDelta = (finalOffset.y / step).roundToInt()
                                        val maxRow = icons.lastIndex / DESKTOP_GRID_COLUMNS
                                        val targetCol = (currentCol + colDelta).coerceIn(0, DESKTOP_GRID_COLUMNS - 1)
                                        val targetRow = (currentRow + rowDelta).coerceIn(0, maxRow)
                                        val targetIndex = (targetRow * DESKTOP_GRID_COLUMNS + targetCol).coerceIn(0, icons.lastIndex)
                                        if (targetIndex != currentIndex) {
                                            icons = icons.toMutableList().apply { add(targetIndex, removeAt(currentIndex)) }
                                            // La cella è cambiata: la molla deve partire dalla posizione visiva
                                            // attuale (dove il dito l'ha lasciata) rispetto alla NUOVA cella,
                                            // altrimenti l'icona scatterebbe di colpo prima di animare.
                                            settleFrom = finalOffset - Offset(x = (targetCol - currentCol) * step, y = (targetRow - currentRow) * step)
                                        }
                                        onReorder(icons.map { it.id })
                                    }
                                    coroutineScope.launch {
                                        offset.snapTo(settleFrom)
                                        offset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                },
                                onDragCancel = {
                                    draggingId = null
                                    coroutineScope.launch { offset.animateTo(Offset.Zero) }
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
            if (entry.badgeCount > 0) {
                BadgedBox(badge = { Badge { Text(entry.badgeCount.toString()) } }) {
                    Icon(entry.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                }
            } else {
                Icon(entry.icon, contentDescription = null, modifier = Modifier.size(36.dp))
            }
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
                    when {
                        isRegistered -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.home_registered_check),
                            tint = if (imageUrl != null) onImageColor else MaterialTheme.colorScheme.primary,
                        )
                        // Un lucchetto sostituisce il segno di spunta quando le iscrizioni
                        // non sono (ancora, o più) aperte - non ha senso mostrarlo se il
                        // chiamante è già preiscritto (caso sopra).
                        event.registrationsOpen == false -> Icon(
                            Icons.Default.Lock,
                            contentDescription = stringResource(R.string.home_registrations_locked),
                            tint = if (imageUrl != null) onImageColor else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Text(formatEventRange(event), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
                if (event.organizzatore != null || event.registeredPlayersCount != null) {
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        event.organizzatore?.let {
                            Text(
                                stringResource(R.string.home_organizer, it),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        event.registeredPlayersCount?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    stringResource(R.string.home_registered_players_count, it),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }
                }
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
