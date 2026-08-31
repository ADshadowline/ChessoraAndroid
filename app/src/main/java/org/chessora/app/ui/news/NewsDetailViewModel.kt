package org.chessora.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState

data class NewsDetailData(
    val article: NewsArticle,
    val comments: List<NewsComment>,
)

/**
 * L'Api non espone un endpoint "singolo articolo per id" (solo GET /api/news,
 * elenco, e GET /api/news/{id}/comments): questo ViewModel quindi richiede
 * l'elenco completo del circolo e filtra per [idNews] lato client - accettabile
 * per un elenco news di un singolo circolo, tipicamente poche decine di voci.
 * Se in futuro serve un endpoint dedicato lato server, questo è l'unico punto
 * da cambiare per usarlo.
 */
class NewsDetailViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NewsDetailData>>(UiState.Loading)
    val state: StateFlow<UiState<NewsDetailData>> = _state.asStateFlow()

    fun load(idClub: Int, idNews: Int) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val newsResult = repository.getNews(idClub, limit = 100)
            val article = newsResult.getOrNull()?.firstOrNull { it.id == idNews }
            if (article == null) {
                _state.value = UiState.Error("Articolo non trovato.")
                return@launch
            }
            val comments = repository.getNewsComments(idNews).getOrDefault(emptyList())
            _state.value = UiState.Success(NewsDetailData(article, comments))
        }
    }
}
