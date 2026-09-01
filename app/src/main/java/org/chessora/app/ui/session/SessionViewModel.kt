package org.chessora.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.SiteBranding
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.push.DeviceRegistration

/**
 * Unico ViewModel creato a livello di MainActivity (non per-schermata) e
 * condiviso da tutto il grafo di navigazione: tiene il circolo scelto e il
 * branding (nome/logo) caricato da /api/site-settings, cosi' ogni schermata
 * puo' leggerli senza rifare la stessa chiamata o duplicare la lettura da
 * DataStore. Vedi ui/navigation/ChessoraNavHost.kt per come viene istanziato
 * e passato giù.
 */
class SessionViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    val selectedClub: StateFlow<String?> = clubPreferences.selectedClub
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _branding = MutableStateFlow<SiteBranding?>(null)
    val branding: StateFlow<SiteBranding?> = _branding

    init {
        // Al primo avvio, se un circolo era già stato scelto in una sessione
        // precedente, carica subito il suo branding e registra il device (il
        // token FCM potrebbe essere lo stesso di sempre, ma repository.registerDevice
        // è un upsert innocuo da ripetere - vedi push/DeviceRegistration.kt).
        // Nota: leggiamo clubPreferences.selectedClub.first() direttamente
        // (sospendendo finché DataStore non emette il primo valore reale),
        // NON selectedClub.value: quest'ultimo, derivato con stateIn(), parte
        // dal seed `null` finché non arriva la prima emissione, quindi qui
        // potrebbe leggersi come "nessun circolo" anche quando uno era già stato
        // salvato in una sessione precedente.
        viewModelScope.launch {
            val club = clubPreferences.selectedClub.first()
            if (club != null) {
                loadBranding(club)
            }
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
        }
    }

    /** Chiamata dall'onboarding (o dalle Impostazioni, per cambiare circolo). */
    fun selectClub(club: String) {
        viewModelScope.launch {
            clubPreferences.setSelectedClub(club)
            loadBranding(club)
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
        }
    }

    private suspend fun loadBranding(club: String) {
        repository.getSiteSettings(club).onSuccess { settings ->
            _branding.value = settings.site
        }
    }
}
