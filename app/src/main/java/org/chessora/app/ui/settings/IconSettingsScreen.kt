package org.chessora.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import org.chessora.app.R
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.home.DESKTOP_ICON_DESCRIPTORS
import org.chessora.app.ui.home.DesktopIconDescriptor

private val ROW_HEIGHT = 56.dp

/**
 * Elenco riordinabile (tieni premuta la maniglia e trascina) e attivabile/disattivabile
 * delle icone mostrate nella Home in "Visualizzazione desktop" (vedi
 * ui/home/HomeScreen.kt, DesktopHomeGrid). Messaggi e Impostazioni non compaiono qui:
 * restano ancorate agli angoli in basso per scelta esplicita, vedi il commento su
 * DesktopIconDescriptor.
 *
 * Il riordino aggiorna subito la lista visibile a schermo (prima che il DataStore
 * confermi la scrittura), altrimenti l'elemento trascinato "scatterebbe" indietro al
 * rilascio in attesa della persistenza asincrona.
 */
@Composable
fun IconSettingsScreen(isPlatformMode: Boolean) {
    val viewModel = chessoraViewModel { app -> IconSettingsViewModel(app.clubPreferences) }
    val storedOrder by viewModel.iconOrder.collectAsState()
    val hiddenIcons by viewModel.hiddenIcons.collectAsState()

    val defaults = remember(isPlatformMode) {
        DESKTOP_ICON_DESCRIPTORS.filter { !(isPlatformMode && it.hiddenInPlatformMode) }
    }
    var items by remember(defaults, storedOrder) {
        mutableStateOf(orderDescriptors(defaults, storedOrder))
    }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.icon_settings_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.icon_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(items, key = { _, item -> item.id }) { _, descriptor ->
                val isDragging = draggingId == descriptor.id
                val hidden = descriptor.id in hiddenIcons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                        .zIndex(if (isDragging) 1f else 0f)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = stringResource(R.string.icon_settings_drag_handle),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .pointerInput(descriptor.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = descriptor.id
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val currentIndex = items.indexOfFirst { it.id == descriptor.id }
                                        if (currentIndex != -1) {
                                            val step = (dragOffset / rowHeightPx).roundToInt()
                                            val targetIndex = (currentIndex + step).coerceIn(0, items.lastIndex)
                                            if (targetIndex != currentIndex) {
                                                items = items.toMutableList().apply {
                                                    add(targetIndex, removeAt(currentIndex))
                                                }
                                                dragOffset -= (targetIndex - currentIndex) * rowHeightPx
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffset = 0f
                                        viewModel.setOrder(items.map { it.id })
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                )
                            },
                    )
                    Icon(descriptor.icon, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                    Text(stringResource(descriptor.labelRes), modifier = Modifier.weight(1f))
                    Switch(
                        checked = !hidden,
                        onCheckedChange = { visible -> viewModel.setHidden(descriptor.id, !visible) },
                    )
                }
            }
        }
    }
}

/** Applica l'ordine salvato a [defaults]: un id in [order] non più tra i default (es.
 * modalità piattaforma) viene ignorato, uno nuovo non ancora in [order] viene accodato
 * in fondo - stessa logica di HomeScreen.applyIconPreferences, qui senza il filtro sulle
 * icone nascoste (che in questa schermata restano visibili, solo con lo switch spento). */
private fun orderDescriptors(defaults: List<DesktopIconDescriptor>, order: List<String>): List<DesktopIconDescriptor> {
    if (order.isEmpty()) return defaults
    val byId = defaults.associateBy { it.id }
    return order.mapNotNull { byId[it] } + defaults.filter { it.id !in order }
}
