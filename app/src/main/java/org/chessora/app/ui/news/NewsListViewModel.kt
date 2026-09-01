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

    fun load(club: String) {
        if (loadedForClub == club && _state.value is UiState.Success) return
        loadedForClub = club
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getNews(club, limit = 30).toUiState()
        }
    }
}
