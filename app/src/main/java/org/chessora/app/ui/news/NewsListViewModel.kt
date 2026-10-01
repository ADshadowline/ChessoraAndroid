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
 * apre NewsDetailScreen) o una notizia FIDE/globale (solo titolo/data/link esterno, mai un
 * dettaglio in-app). */
sealed interface NewsListItem {
    data class Club(val article: NewsArticle) : NewsListItem
    data class Network(val item: NetworkNewsItem) : NewsListItem
}

/** [club] null = "modalità piattaforma" o schermata "News" del desktop/bottom bar (vedi
 * [mergeAllSources]) - il circolo mostra allora le news di TUTTI i circoli invece che di
 * uno solo (comportamento preesistente, invariato). [mergeAllSources] true = oltre alle
 * news di circolo (tutti i circoli, [club] è sempre null in questo caso) carica ANCHE le
 * notizie FIDE/globali e le mostra mescolate in un solo elenco ordinato per data - usato
 * dalla schermata "News" del desktop/bottom bar (NEWS_LIST); false = solo le news del
 * circolo indicato da [club], nessuna chiamata a api/network-news - usato da "Il Circolo" >
 * News (CLUB_NEWS), dove ha senso vedere solo le notizie del proprio circolo. */
class NewsListViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<NewsListItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NewsListItem>>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var club: String? = null
    private var mergeAllSources: Boolean = false
    private var searchJob: Job? = null
    private var fetchJob: Job? = null

    fun load(club: String?, mergeAllSources: Boolean = false) {
        this.club = club
        this.mergeAllSources = mergeAllSources
        fetch()
    }

    /** La ricerca copre TUTTE le news disponibili (non solo l'ultima pagina caricata):
     * quando non è vuota si chiede al server un limite più alto (vedi NewsController.cs),
     * mai un filtro solo client-side sulla pagina già in memoria. Debounce di 350ms per non
     * mandare una richiesta a ogni carattere digitato. In modalità [mergeAllSources] la
     * stessa query va a entrambe le fonti (circolo + mondo). */
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
     * senza questo, cambiare rapidamente ricerca può far vincere una risposta vecchia
     * arrivata dopo quella nuova, mostrando dati della ricerca sbagliata. */
    private fun fetch() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val search = _query.value.trim().ifBlank { null }
            val limit = if (search != null) 200 else 30
            val result = if (mergeAllSources) fetchMerged(limit, search) else fetchClubOnly(club, limit, search)
            _state.value = result.toUiState()
        }
    }

    private suspend fun fetchClubOnly(club: String?, limit: Int, search: String?): Result<List<NewsListItem>> =
        (if (club != null) repository.getNews(club, limit, search) else repository.getNewsAllClubs(limit, search))
            .map { list -> list.map { NewsListItem.Club(it) } }

    /** Circolo (sempre tutti i circoli qui, vedi classe sopra) + mondo, mescolati e
     * ordinati per data decrescente - se una delle due fonti fallisce, l'altra basta
     * comunque a mostrare qualcosa invece di far fallire l'intero elenco misto (stesso
     * spirito di OnlineGamesViewModel per Lichess/Chess.com). */
    private suspend fun fetchMerged(limit: Int, search: String?): Result<List<NewsListItem>> {
        val clubResult = repository.getNewsAllClubs(limit, search)
        val networkResult = repository.getNetworkNews(limit, search)
        if (clubResult.isFailure && networkResult.isFailure) {
            return Result.failure(clubResult.exceptionOrNull() ?: networkResult.exceptionOrNull()!!)
        }
        val clubItems = clubResult.getOrDefault(emptyList()).map { NewsListItem.Club(it) }
        val networkItems = networkResult.getOrDefault(emptyList()).map { NewsListItem.Network(it) }
        val merged = (clubItems + networkItems).sortedByDescending { it.sortKey() }
        return Result.success(merged)
    }

    /** Data ISO 8601 (confrontabile come stringa, stesso formato su entrambe le fonti) usata
     * per ordinare l'elenco misto - un articolo di circolo usa la pubblicazione se c'è,
     * altrimenti la creazione (bozza mai pubblicata, caso raro in questo contesto). */
    private fun NewsListItem.sortKey(): String = when (this) {
        is NewsListItem.Club -> article.publishedAt ?: article.createdAt
        is NewsListItem.Network -> item.createdAt
    }
}
