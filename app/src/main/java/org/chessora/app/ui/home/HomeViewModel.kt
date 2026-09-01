package org.chessora.app.ui.home

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

data class HomeData(
    val latestNews: List<NewsArticle>,
)

/** Schermata Home: ultime news del circolo. */
class HomeViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    private var loadedForClub: String? = null

    fun load(club: String) {
        if (loadedForClub == club && _state.value is UiState.Success) return
        loadedForClub = club

        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getNews(club, limit = 5).map { HomeData(it) }.toUiState()
        }
    }
}
