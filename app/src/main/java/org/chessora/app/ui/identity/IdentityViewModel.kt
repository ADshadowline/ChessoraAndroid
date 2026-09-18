package org.chessora.app.ui.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.dto.IdentifyResultDto
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.push.DeviceRegistration

/** Stati della schermata di identificazione (vedi IdentityScreen.kt). Non esiste
 * alcuno stato "scegli il tuo nome dall'elenco soci": chi seleziona un circolo
 * diverso dal proprio non deve poter sfogliare i nominativi dei suoi soci - se
 * email/telefono/nome non trovano corrispondenza si esce semplicemente come non
 * identificato (vedi PlayerIdentityService.IdentifyAsync lato server). */
sealed interface IdentityStep {
    /** Schermata iniziale: Google o telefono (nessuna opzione "Salta" - vedi
     * MembershipQuestionScreen: l'identificazione è sempre richiesta, sia per chi
     * è socio di un circolo sia per chi non lo è). */
    data object Choosing : IdentityStep
    data object Loading : IdentityStep
    data class Done(val matchedName: String?) : IdentityStep
    data class Error(val message: String) : IdentityStep
}

class IdentityViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _step = MutableStateFlow<IdentityStep>(IdentityStep.Choosing)
    val step: StateFlow<IdentityStep> = _step

    // GET api/players/roster prende club=<codice> (come ogni altro endpoint pubblico
    // GET, riscritto lato server - vedi Program.cs) mentre POST identify/choose
    // vogliono l'IdClub numerico già risolto nel body JSON, stessa eccezione già
    // usata da POST /api/devices/register (push/DeviceRegistration.kt): la
    // riscrittura del middleware guarda solo la query string, mai il body.
    private var clubCode: String? = null
    private var idClub: Int? = null

    /** Da chiamare una volta, appena si conosce il publicCode del circolo -
     * [club] null se il socio ha scelto "non sono iscritto a nessun circolo"
     * (ui/onboarding/MembershipQuestionScreen.kt): l'identificazione allora cerca
     * a livello nazionale invece che nel roster di un circolo. */
    fun loadClub(club: String?) {
        clubCode = club
        if (club == null) return
        viewModelScope.launch {
            idClub = repository.resolveClubByCode(club).getOrNull()
        }
    }

    /** [email]/[phoneNumber] ottenuti da Google/Phone Hint, [idFide] solo nel flusso
     * senza circolo (facoltativo, digitato dal socio) - cerca un socio, nel roster
     * del circolo corrente o a livello nazionale (nessun circolo scelto), il cui
     * contatto/IdFide (o, come ultimo tentativo lato server, il cui nome)
     * corrisponda. Se non trova nulla, il socio esce semplicemente come non
     * identificato: non esiste alcun elenco soci da sfogliare (vedi
     * PlayerIdentityService). */
    fun identify(email: String?, phoneNumber: String?, displayName: String? = null, idFide: String? = null) {
        if (email.isNullOrBlank() && phoneNumber.isNullOrBlank() && idFide.isNullOrBlank()) {
            _step.value = IdentityStep.Error("Non riesco a verificare l'identità in questo momento.")
            return
        }
        val nationalMode = clubCode == null
        val club = idClub
        if (!nationalMode && club == null) {
            _step.value = IdentityStep.Error("Non riesco a verificare l'identità in questo momento.")
            return
        }
        _step.value = IdentityStep.Loading
        viewModelScope.launch {
            val result = if (nationalMode) {
                repository.identifyPlayerNational(email, phoneNumber, idFide, displayName)
            } else {
                repository.identifyPlayer(club!!, email, phoneNumber, displayName)
            }
            result.onSuccess { r -> saveAndFinish(r, email, phoneNumber) }
                .onFailure { _step.value = IdentityStep.Error("Connessione non riuscita, riprova.") }
        }
    }

    fun retry() {
        _step.value = IdentityStep.Choosing
    }

    /** [email]/[phoneNumber] vanno salvati come "autenticato" (vedi
     * ClubPreferences.setAuthenticated) SEMPRE che siano stati ottenuti da Google/SIM,
     * indipendentemente dal fatto che [result] trovi poi un socio corrispondente: sono la
     * prova di identità richiesta per preiscriversi a un torneo anche senza essere un socio
     * riconosciuto (vedi ui/tournaments/). */
    private suspend fun saveAndFinish(result: IdentifyResultDto, email: String?, phoneNumber: String?) {
        clubPreferences.setAuthenticated(email, phoneNumber)
        if (result.matched && result.idPlayer != null && result.name != null) {
            clubPreferences.setIdentified(result.idPlayer, result.name)
            // Associa subito il dispositivo al socio appena identificato (vedi
            // DeviceRegistration.kt punto 4), invece di aspettare il prossimo
            // riavvio dell'app - serve alla vista admin "Dispositivi".
            DeviceRegistration.registerCurrentToken(repository, clubPreferences, clubCode)
        } else {
            clubPreferences.setIdentityDeclined()
        }
        _step.value = IdentityStep.Done(result.name)
    }
}
