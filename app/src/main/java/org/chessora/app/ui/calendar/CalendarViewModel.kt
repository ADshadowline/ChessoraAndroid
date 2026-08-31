package org.chessora.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.DateRange
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/**
 * Vista "lista degli eventi futuri" (una delle due opzioni previste da
 * docs/android-app-spec.md §7.4 - la vista mensile a griglia è un possibile
 * miglioramento futuro, non implementata in questa prima versione: vedi
 * README.md "Cosa NON è stato fatto"). Intervallo fisso: da oggi ai prossimi
 * 90 giorni, sufficiente per il calendario di un singolo circolo.
 */
class CalendarViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<CalendarEvent>>>(UiState.Loading)
    val state: StateFlow<UiState<List<CalendarEvent>>> = _state.asStateFlow()

    private var loadedForClubId: Int? = null

    fun load(idClub: Int) {
        if (loadedForClubId == idClub && _state.value is UiState.Success) return
        loadedForClubId = idClub
        viewModelScope.launch {
            _state.value = UiState.Loading
            val from = DateRange.todayIso()
            val to = DateRange.todayPlusDaysIso(90)
            _state.value = repository.getCalendar(idClub, from, to)
                .map { events -> events.sortedBy { it.eventDateTime } }
                .toUiState()
        }
    }
}
