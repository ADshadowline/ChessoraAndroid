package org.chessora.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.NextTournament
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toFriendlyMessage

data class HomeData(
    val nextTournament: NextTournament?,
    val latestNews: List<NewsArticle>,
)

/**
 * Schermata Home (docs/android-app-spec.md §7.2): widget "prossimo torneo" +
 * ultime news. Le due chiamate di rete sono indipendenti e volutamente non
 * bloccanti l'una sull'altra: se GET /api/tornei/next-upcoming fallisce, le
 * news vengono comunque mostrate (e viceversa) - vedi [load], che tratta un
 * fallimento del solo prossimo torneo come "nessun torneo in programma"
 * invece che come errore dell'intera schermata.
 */
class HomeViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    private var loadedForClub: String? = null

    fun load(club: String) {
        if (loadedForClub == club && _state.value is UiState.Success) return
        loadedForClub = club

        viewModelScope.launch {
            _state.value = UiState.Loading
            val newsResult = repository.getNews(club, limit = 5)
            if (newsResult.isFailure) {
                _state.value = UiState.Error(newsResult.exceptionOrNull()?.toFriendlyMessage() ?: "Errore sconosciuto")
                return@launch
            }
            val nextTournament = repository.getNextUpcomingTournament(club).getOrNull()
            _state.value = UiState.Success(HomeData(nextTournament, newsResult.getOrDefault(emptyList())))
        }
    }
}
