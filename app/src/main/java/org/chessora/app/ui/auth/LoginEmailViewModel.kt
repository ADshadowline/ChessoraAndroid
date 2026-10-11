package org.chessora.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.apiError
import org.chessora.app.data.remote.dto.RegisterWithEmailRequestDto
import org.chessora.app.data.repository.ChessoraRepository

sealed interface LoginEmailStep {
    data object Idle : LoginEmailStep
    data object Loading : LoginEmailStep
    data object LoggedIn : LoginEmailStep
    /** Login riuscito ma profilo ancora da completare (account creato con "Crea nuovo
     * utente"): si prosegue con CompleteProfileScreen, come dopo un login Google. */
    data object NeedsProfile : LoginEmailStep
    /** "Crea nuovo utente" riuscito: account creato, in attesa della conferma email. */
    data class Registered(val email: String) : LoginEmailStep
    /** Il server ha risposto EmailNotConfirmed - offre "Invia di nuovo l'email" invece
     * del solo messaggio generico (vedi LoginEmailScreen.kt). */
    data class EmailNotConfirmed(val email: String) : LoginEmailStep
    data class Error(val message: String) : LoginEmailStep
}

/** Login con email O ID FIDE (come stringa numerica) + password - lo stesso campo,
 * l'utente digita quello che ha (vedi PlayerAuthController.Login lato server). */
class LoginEmailViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _step = MutableStateFlow<LoginEmailStep>(LoginEmailStep.Idle)
    val step: StateFlow<LoginEmailStep> = _step

    fun login(emailOrIdFide: String, password: String) {
        if (emailOrIdFide.isBlank() || password.isBlank()) {
            _step.value = LoginEmailStep.Error("Inserisci email/ID FIDE e password.")
            return
        }
        _step.value = LoginEmailStep.Loading
        viewModelScope.launch {
            repository.playerLogin(emailOrIdFide.trim(), password)
                .onSuccess { auth ->
                    AuthSessionPersister.persist(auth, displayName = null, authPreferences, clubPreferences)
                    _step.value = if (auth.profileComplete) LoginEmailStep.LoggedIn else LoginEmailStep.NeedsProfile
                }
                .onFailure { error ->
                    val apiError = error.apiError()
                    _step.value = if (apiError?.code == "EmailNotConfirmed" && emailOrIdFide.contains('@')) {
                        LoginEmailStep.EmailNotConfirmed(emailOrIdFide.trim())
                    } else {
                        LoginEmailStep.Error(apiError?.message ?: "Connessione non riuscita, riprova.")
                    }
                }
        }
    }

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _step.value = LoginEmailStep.Error("Inserisci email e password.")
            return
        }
        if (password != confirmPassword) {
            _step.value = LoginEmailStep.Error("Le due password non coincidono.")
            return
        }
        _step.value = LoginEmailStep.Loading
        viewModelScope.launch {
            repository.registerWithEmail(RegisterWithEmailRequestDto(email.trim(), password, confirmPassword))
                .onSuccess { _step.value = LoginEmailStep.Registered(it.email) }
                .onFailure { _step.value = LoginEmailStep.Error(it.apiError()?.message ?: "Connessione non riuscita, riprova.") }
        }
    }

    /** Il passaggio tra "Accedi" e "Crea nuovo utente" azzera l'eventuale errore a schermo. */
    fun clearMessage() {
        _step.value = LoginEmailStep.Idle
    }

    fun resendConfirmation(email: String) {
        viewModelScope.launch { repository.resendConfirmation(email) }
        _step.value = LoginEmailStep.Error("Ti abbiamo inviato una nuova email di conferma.")
    }
}
