package org.chessora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    /** "classic" o "desktop" - vedi ClubPreferences.DISPLAY_MODE_* e HomeScreen.kt. */
    val displayMode: StateFlow<String> = clubPreferences.displayMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClubPreferences.DISPLAY_MODE_DESKTOP)

    val splashBackgroundUri: StateFlow<String?> = clubPreferences.splashBackgroundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val desktopBackgroundUri: StateFlow<String?> = clubPreferences.desktopBackgroundUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** [club] serve solo per ri-registrare subito il token se l'utente riaccende il toggle. */
    fun setNotificationsEnabled(enabled: Boolean, club: String?) {
        viewModelScope.launch {
            clubPreferences.setNotificationsEnabled(enabled)
            if (enabled) {
                DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
            }
        }
    }

    fun setDisplayMode(mode: String) {
        viewModelScope.launch { clubPreferences.setDisplayMode(mode) }
    }

    fun setSplashBackgroundUri(uri: String?) {
        viewModelScope.launch { clubPreferences.setSplashBackgroundUri(uri) }
    }

    fun setDesktopBackgroundUri(uri: String?) {
        viewModelScope.launch { clubPreferences.setDesktopBackgroundUri(uri) }
    }

    /** Privacy delle conferme di consegna/lettura in Messaggistica (doppia spunta) -
     * a differenza delle altre preferenze qui sopra questa va anche al server (vedi
     * MessagingRepository.GetMessagesAsync lato backend, che la legge per decidere se
     * mostrare agli ALTRI quando questo socio ha ricevuto/letto i loro messaggi): non può
     * restare solo-locale come notificationsEnabled. */
    private val _hideReadReceipts = MutableStateFlow(false)
    val hideReadReceipts: StateFlow<Boolean> = _hideReadReceipts.asStateFlow()

    fun loadHideReadReceipts() {
        viewModelScope.launch {
            val idPlayer = clubPreferences.identifiedPlayerId.first() ?: return@launch
            repository.getMessagingSettings(idPlayer).onSuccess { _hideReadReceipts.value = it.hideDeliveryAndReadStatus }
        }
    }

    fun setHideReadReceipts(hide: Boolean) {
        _hideReadReceipts.value = hide
        viewModelScope.launch {
            val idPlayer = clubPreferences.identifiedPlayerId.first() ?: return@launch
            repository.setMessagingSettings(idPlayer, hide)
        }
    }
}
