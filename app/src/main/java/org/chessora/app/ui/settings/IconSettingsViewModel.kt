package org.chessora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences

class IconSettingsViewModel(private val clubPreferences: ClubPreferences) : ViewModel() {

    val iconOrder: StateFlow<List<String>> = clubPreferences.desktopIconOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hiddenIcons: StateFlow<Set<String>> = clubPreferences.desktopHiddenIcons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun setOrder(order: List<String>) {
        viewModelScope.launch { clubPreferences.setDesktopIconOrder(order) }
    }

    fun setHidden(id: String, hidden: Boolean) {
        viewModelScope.launch { clubPreferences.setDesktopIconHidden(id, hidden) }
    }
}
