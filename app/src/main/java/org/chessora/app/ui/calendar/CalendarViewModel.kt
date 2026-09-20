package org.chessora.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/**
 * Vista mensile a griglia (come il calendario nativo Android/Samsung): un mese
 * alla volta, navigabile con le frecce - month è sempre disponibile (anche
 * durante il caricamento del mese successivo) cosi' l'intestazione/le frecce
 * restano utilizzabili senza aspettare la risposta del server.
 */
class CalendarViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<CalendarEvent>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CalendarEvent>>> = _state.asStateFlow()

    /** Id dei tornei a cui il chiamante risulta preiscritto - per l'evidenziazione
     * "lampeggiante" del giorno (vedi CalendarScreen.DayCell). Stessa logica già usata
     * in HomeViewModel.myPreRegisteredTournamentIds. */
    private val _registeredTournamentIds = MutableStateFlow<Set<Int>>(emptySet())
    val registeredTournamentIds: StateFlow<Set<Int>> = _registeredTournamentIds.asStateFlow()

    private var club: String? = null

    fun load(club: String) {
        if (this.club == club) return
        this.club = club
        fetchMonth(_month.value)
        viewModelScope.launch { _registeredTournamentIds.value = myPreRegisteredTournamentIds() }
    }

    fun goToMonth(deltaMonths: Long) {
        _month.value = _month.value.plusMonths(deltaMonths)
        fetchMonth(_month.value)
    }

    fun retry() = fetchMonth(_month.value)

    /** Url assoluto del bando dell'evento, se ne ha uno - stessa logica di
     * HomeViewModel.resolveBandoUrl: quello di un torneo è già nella riga di
     * calendario, quello di un evento richiede una fetch dedicata. */
    suspend fun resolveBandoUrl(event: CalendarEvent, club: String): String? {
        event.tournamentBandoPath?.let { return NetworkModule.resolveAssetUrl(it) }
        val idEvento = event.idEvento ?: return null
        val bandoPath = repository.getEventoBando(idEvento, club).getOrNull()?.bandoPath ?: return null
        return NetworkModule.resolveAssetUrl(bandoPath)
    }

    private suspend fun myPreRegisteredTournamentIds(): Set<Int> {
        if (!clubPreferences.isAuthenticated.first()) return emptySet()
        return repository.getMyPreRegistrations().getOrNull()?.map { it.id }?.toSet() ?: emptySet()
    }

    private fun fetchMonth(yearMonth: YearMonth) {
        val c = club ?: return
        viewModelScope.launch {
            _state.value = UiState.Loading
            val from = yearMonth.atDay(1).format(isoDate)
            val to = yearMonth.plusMonths(1).atDay(1).format(isoDate)
            _state.value = repository.getCalendar(c, from, to)
                .map { events -> events.sortedBy { it.eventDateTime } }
                .toUiState()
        }
    }
}
