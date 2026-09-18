package org.chessora.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

class NewsListViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<NewsArticle>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NewsArticle>>> = _state.asStateFlow()

    private var loadedForClub: String? = null
    private var hasLoadedOnce = false

    /** [club] null = "modalità piattaforma" (nessun circolo scelto): ultime 30 news
     * su TUTTI i circoli invece che di uno solo. */
    fun load(club: String?) {
        if (hasLoadedOnce && loadedForClub == club && _state.value is UiState.Success) return
        loadedForClub = club
        hasLoadedOnce = true
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = (if (club != null) repository.getNews(club, limit = 30) else repository.getNewsAllClubs(limit = 30)).toUiState()
        }
    }
}
