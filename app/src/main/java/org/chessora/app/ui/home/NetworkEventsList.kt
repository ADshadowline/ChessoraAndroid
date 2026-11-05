package org.chessora.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.data.remote.dto.NetworkTournament
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.chessoraViewModel

/** Stato dell'elenco "In rete": pagine caricate finora, totale lato server, errori. */
data class NetworkEventsState(
    val items: List<NetworkTournament> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = false,
    val error: Boolean = false,
    val page: Int = 0,
) {
    val canLoadMore: Boolean get() = items.size < totalCount
}

/** Tornei in rete da oggi in avanti (GET api/tornei/in-rete?periodo=futuri) - ricerca e
 * paginazione lato server: sono centinaia, non si caricano tutti in una volta. */
class NetworkEventsViewModel(private val repository: ChessoraRepository) : ViewModel() {
    private val _state = MutableStateFlow(NetworkEventsState())
    val state: StateFlow<NetworkEventsState> = _state.asStateFlow()

    private var search: String? = null
    private var job: Job? = null

    /** Nuova ricerca (debounce 350ms quando cambia il testo): riparte dalla prima pagina. */
    fun setSearch(value: String, debounce: Boolean) {
        val v = value.trim().ifBlank { null }
        if (v == search && _state.value.page > 0) return
        search = v
        job?.cancel()
        job = viewModelScope.launch {
            if (debounce) delay(350)
            _state.value = NetworkEventsState(loading = true)
            fetch(1)
        }
    }

    fun loadMore() {
        val s = _state.value
        if (s.loading || !s.canLoadMore) return
        job = viewModelScope.launch {
            _state.value = s.copy(loading = true, error = false)
            fetch(s.page + 1)
        }
    }

    fun retry() {
        val s = _state.value
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = s.copy(loading = true, error = false)
            fetch(if (s.page == 0) 1 else s.page + 1)
        }
    }

    private suspend fun fetch(page: Int) {
        repository.getNetworkTournaments(page, PAGE_SIZE, search)
            .onSuccess { data ->
                val prev = if (page == 1) emptyList() else _state.value.items
                _state.value = NetworkEventsState(items = prev + data.items, totalCount = data.totalCount, page = page)
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = true) }
    }

    companion object { const val PAGE_SIZE = 30 }
}

private const val TOURN_DETAIL_URL = "https://tourn.chessora.org/prototype/torneo-dettaglio.html?id="

/** Elenco "In rete" della schermata Eventi: [query] è lo stesso campo di ricerca in alto. */
@Composable
fun NetworkEventsList(query: String) {
    val viewModel = chessoraViewModel { app -> NetworkEventsViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(query) { viewModel.setSearch(query, debounce = state.page > 0) }

    // Pagina successiva quando si arriva in fondo.
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 5
        }
    }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd && state.page > 0) viewModel.loadMore() }

    when {
        state.items.isEmpty() && state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.items.isEmpty() && state.error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.events_network_error))
                TextButton(onClick = viewModel::retry) { Text(stringResource(R.string.events_network_retry)) }
            }
        }
        state.items.isEmpty() && state.page > 0 -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(if (query.isBlank()) stringResource(R.string.events_network_empty) else stringResource(R.string.home_no_results))
        }
        else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    stringResource(R.string.events_network_count, state.totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            itemsIndexed(state.items, key = { i, t -> "${t.fonte}:${t.idEventoVesus ?: t.id ?: i}:$i" }) { _, t ->
                NetworkTournamentCard(t) {
                    val url = t.vesusUrl ?: t.id?.takeIf { t.fonte == "Chessora" }?.let { TOURN_DETAIL_URL + it }
                    if (url != null) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkTournamentCard(t: NetworkTournament, onClick: () -> Unit) {
    val clickable = t.vesusUrl != null || (t.fonte == "Chessora" && t.id != null)
    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t.nome, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    formatNetworkDates(t.inizio, t.fine, utc = t.fonte == "Vesus"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                val place = listOfNotNull(t.localita, t.provincia?.takeIf { it != t.localita }?.let { "($it)" }).joinToString(" ")
                if (place.isNotBlank()) {
                    Text(place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                (t.clubNameChessora ?: t.organizzatore)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Image(
                painter = painterResource(if (t.fonte == "Vesus") R.drawable.vesus_logo else R.drawable.ic_launcher_foreground),
                contentDescription = t.fonte,
                modifier = Modifier.padding(start = 10.dp).size(if (t.fonte == "Vesus") 26.dp else 34.dp),
            )
        }
    }
    if (clickable) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { content() }
    } else {
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { content() }
    }
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

// Le date vesus.org sono salvate in UTC (es. 22:00 = mezzanotte in Italia): si riportano al
// fuso del telefono; quelle dei tornei Chessora sono già locali.
private fun parseDate(s: String?, utc: Boolean): LocalDateTime? = s?.let {
    runCatching {
        val t = LocalDateTime.parse(it.take(19))
        if (utc) t.atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(java.time.ZoneId.systemDefault()).toLocalDateTime() else t
    }.getOrNull()
}

private fun formatNetworkDates(inizio: String?, fine: String?, utc: Boolean): String {
    val a = parseDate(inizio, utc) ?: return ""
    val b = parseDate(fine, utc)
    return if (b == null || b.toLocalDate() == a.toLocalDate()) a.format(dayMonthYear)
    else a.format(dayMonth) + " – " + b.format(dayMonthYear)
}
