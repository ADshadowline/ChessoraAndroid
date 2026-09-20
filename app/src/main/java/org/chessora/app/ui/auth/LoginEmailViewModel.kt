package org.chessora.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.apiError
import org.chessora.app.data.repository.ChessoraRepository

sealed interface LoginEmailStep {
    data object Idle : LoginEmailStep
    data object Loading : LoginEmailStep
    data object LoggedIn : LoginEmailStep
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
                    _step.value = LoginEmailStep.LoggedIn
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

    fun resendConfirmation(email: String) {
        viewModelScope.launch { repository.resendConfirmation(email) }
        _step.value = LoginEmailStep.Error("Ti abbiamo inviato una nuova email di conferma.")
    }

    fun resetError() {
        _step.value = LoginEmailStep.Idle
    }
}
