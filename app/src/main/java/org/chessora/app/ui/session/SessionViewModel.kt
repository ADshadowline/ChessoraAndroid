package org.chessora.app.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    // MutableStateFlow di proprietà del ViewModel (non più un semplice stateIn() sopra
    // clubPreferences.selectedClub): clearSelectedClub() deve poter azzerare il valore
    // SINCRONAMENTE, prima ancora che la scrittura su DataStore (asincrona) sia
    // completata - altrimenti "Cambia circolo" naviga a ONBOARDING mentre selectedClub
    // è ancora quello vecchio per un istante, e la guardia più sotto in
    // ChessoraNavHost.kt (pensata solo per saltare l'onboarding ai riavvii a freddo)
    // rimbalza subito l'utente via prima che veda il selettore.
    private val _selectedClub = MutableStateFlow<String?>(null)
    val selectedClub: StateFlow<String?> = _selectedClub

    private val _branding = MutableStateFlow<SiteBranding?>(null)
    val branding: StateFlow<SiteBranding?> = _branding

    init {
        // Al primo avvio, se un circolo era già stato scelto in una sessione
        // precedente, carica subito il suo branding e registra il device (il
        // token FCM potrebbe essere lo stesso di sempre, ma repository.registerDevice
        // è un upsert innocuo da ripetere - vedi push/DeviceRegistration.kt).
        viewModelScope.launch {
            val club = clubPreferences.selectedClub.first()
            _selectedClub.value = club
            if (club != null) {
                loadBranding(club)
            }
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
        }
    }

    /** Chiamata dall'onboarding (o dalle Impostazioni, per cambiare circolo). */
    fun selectClub(club: String) {
        _selectedClub.value = club
        viewModelScope.launch {
            clubPreferences.setSelectedClub(club)
            loadBranding(club)
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, club)
        }
    }

    /** Chiamata da "Cambia circolo" nelle Impostazioni, PRIMA di navigare a
     * ONBOARDING (vedi ChessoraNavHost.kt): azzera subito il circolo in memoria
     * cosi' l'utente vede davvero il selettore invece di un rimbalzo istantaneo
     * a Home - vedi il commento su [_selectedClub] qui sopra. */
    fun clearSelectedClub() {
        _selectedClub.value = null
        _branding.value = null
        viewModelScope.launch { clubPreferences.clearSelectedClub() }
    }

    private suspend fun loadBranding(club: String) {
        repository.getSiteSettings(club).onSuccess { settings ->
            _branding.value = settings.site
        }
    }
}
