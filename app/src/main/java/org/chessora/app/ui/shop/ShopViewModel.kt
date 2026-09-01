package org.chessora.app.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

class ShopViewModel(private val repository: ChessoraRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<ShopProduct>>>(UiState.Loading)
    val state: StateFlow<UiState<List<ShopProduct>>> = _state.asStateFlow()

    fun load(club: String) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = repository.getShopProducts(club).toUiState()
        }
    }
}
