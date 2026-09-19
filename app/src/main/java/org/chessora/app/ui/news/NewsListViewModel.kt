package org.chessora.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.NetworkNewsItem
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Una riga della schermata News: o un articolo di circolo (con corpo/immagine/commenti,
 * apre NewsDetailScreen) o una notizia FIDE/globale (filtro "Mondo" - solo titolo/data/
 * link esterno, mai un dettaglio in-app). */
sealed interface NewsListItem {
    data class Club(val article: NewsArticle) : NewsListItem
    data class Network(val item: NetworkNewsItem) : NewsListItem
}

enum class NewsScope { CIRCOLO, MONDO }

/** [club] null = "modalità piattaforma" (nessun circolo scelto, vedi
 * ui/onboarding/MembershipQuestionScreen.kt) - il filtro "Circolo" mostra allora le news
 * di TUTTI i circoli invece che di uno solo (comportamento preesistente, invariato). Il
 * filtro "Mondo" (vedi [NewsScope.MONDO]) è invece sempre lo stesso indipendentemente dal
 * circolo scelto: notizie FIDE/globali, GET /api/network-news. */
class NewsListViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<NewsListItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NewsListItem>>> = _state.asStateFlow()

    private val _scope = MutableStateFlow(NewsScope.CIRCOLO)
    val scope: StateFlow<NewsScope> = _scope.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var club: String? = null
    private var searchJob: Job? = null
    private var fetchJob: Job? = null

    fun load(club: String?) {
        this.club = club
        fetch()
    }

    fun setScope(scope: NewsScope) {
        if (_scope.value == scope) return
        _scope.value = scope
        fetch()
    }

    /** La ricerca copre TUTTE le news disponibili (non solo l'ultima pagina caricata):
     * quando non è vuota si chiede al server un limite più alto (vedi NewsController.cs),
     * mai un filtro solo client-side sulla pagina già in memoria. Debounce di 350ms per non
     * mandare una richiesta a ogni carattere digitato. */
    fun setQuery(query: String) {
        _query.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            fetch()
        }
    }

    fun retry() = fetch()

    /** Cancella qualunque fetch precedente ancora in volo prima di lanciarne una nuova:
     * senza questo, cambiare rapidamente scope/query può far vincere una risposta vecchia
     * arrivata dopo quella nuova, mostrando dati della scope/ricerca sbagliata. */
    private fun fetch() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val search = _query.value.trim().ifBlank { null }
            val limit = if (search != null) 200 else 30
            _state.value = when (_scope.value) {
                NewsScope.MONDO -> repository.getNetworkNews(limit, search)
                    .map { list -> list.map { NewsListItem.Network(it) } }
                NewsScope.CIRCOLO -> {
                    val club = this@NewsListViewModel.club
                    (if (club != null) repository.getNews(club, limit, search) else repository.getNewsAllClubs(limit, search))
                        .map { list -> list.map { NewsListItem.Club(it) } }
                }
            }.toUiState()
        }
    }
}
