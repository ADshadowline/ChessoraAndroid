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

    /** Username Lichess/Chess.com per "Scacchi Online" in Le mie performance (vedi
     * ui/performance/) - mai una password, nessuno dei due la prevede per app di terze
     * parti (le partite di un utente sono pubbliche). Null finché non caricato/mai
     * configurato. Un'unica chiamata (GET api/lichess-settings) restituisce entrambi, vedi
     * loadOnlineChessUsernames. */
    private val _lichessUsername = MutableStateFlow<String?>(null)
    val lichessUsername: StateFlow<String?> = _lichessUsername.asStateFlow()

    private val _chessComUsername = MutableStateFlow<String?>(null)
    val chessComUsername: StateFlow<String?> = _chessComUsername.asStateFlow()

    fun loadOnlineChessUsernames() {
        viewModelScope.launch {
            val idPlayer = clubPreferences.identifiedPlayerId.first() ?: return@launch
            repository.getLichessSettings(idPlayer).onSuccess {
                _lichessUsername.value = it.lichessUsername
                _chessComUsername.value = it.chessComUsername
            }
        }
    }

    fun setLichessUsername(username: String?) {
        val trimmed = username?.trim()?.takeIf { it.isNotEmpty() }
        _lichessUsername.value = trimmed
        viewModelScope.launch {
            val idPlayer = clubPreferences.identifiedPlayerId.first() ?: return@launch
            repository.setLichessUsername(idPlayer, trimmed)
        }
    }

    fun setChessComUsername(username: String?) {
        val trimmed = username?.trim()?.takeIf { it.isNotEmpty() }
        _chessComUsername.value = trimmed
        viewModelScope.launch {
            val idPlayer = clubPreferences.identifiedPlayerId.first() ?: return@launch
            repository.setChessComUsername(idPlayer, trimmed)
        }
    }
}
