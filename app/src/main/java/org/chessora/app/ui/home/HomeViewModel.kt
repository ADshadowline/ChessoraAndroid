package org.chessora.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

data class HomeData(
    val upcoming: List<CalendarEvent>,
)

/** Schermata Home: prossimi appuntamenti dal calendario, in ordine cronologico
 * (non più le news, che restano comunque raggiungibili dalla propria scheda). */
class HomeViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    private var loadedForClub: String? = null

    fun load(club: String) {
        if (loadedForClub == club && _state.value is UiState.Success) return
        loadedForClub = club
        fetch(club)
    }

    fun retry(club: String) = fetch(club)

    private fun fetch(club: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
            val today = LocalDate.now()
            val from = today.format(isoDate)
            val to = today.plusDays(WINDOW_DAYS).format(isoDate)
            _state.value = repository.getCalendar(club, from, to)
                .map { events ->
                    val now = LocalDateTime.now()
                    HomeData(
                        events
                            .filter { isUpcoming(it, now) }
                            .sortedBy { it.eventDateTime }
                            .take(MAX_ITEMS),
                    )
                }
                .toUiState()
        }
    }

    private fun isUpcoming(event: CalendarEvent, now: LocalDateTime): Boolean =
        runCatching { LocalDateTime.parse(event.eventDateTime) }.getOrNull()?.isAfter(now) == true

    /** Url assoluto del bando dell'appuntamento, se ne ha uno - quello di un torneo
     * è già nella riga di calendario, quello di un evento richiede una fetch dedicata. */
    suspend fun resolveBandoUrl(event: CalendarEvent, club: String): String? {
        event.tournamentBandoPath?.let { return NetworkModule.resolveAssetUrl(it) }
        val idEvento = event.idEvento ?: return null
        val bandoPath = repository.getEventoBando(idEvento, club).getOrNull()?.bandoPath ?: return null
        return NetworkModule.resolveAssetUrl(bandoPath)
    }

    private companion object {
        const val WINDOW_DAYS = 60L
        const val MAX_ITEMS = 20
    }
}
