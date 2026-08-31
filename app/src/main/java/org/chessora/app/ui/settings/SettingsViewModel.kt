package org.chessora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.push.DeviceRegistration

class SettingsViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = clubPreferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** [idClub] serve solo per ri-registrare subito il token se l'utente riaccende il toggle. */
    fun setNotificationsEnabled(enabled: Boolean, idClub: Int?) {
        viewModelScope.launch {
            clubPreferences.setNotificationsEnabled(enabled)
            if (enabled) {
                DeviceRegistration.registerCurrentToken(repository, clubPreferences, idClub)
            }
        }
    }
}
