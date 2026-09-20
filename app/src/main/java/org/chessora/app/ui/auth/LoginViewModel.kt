package org.chessora.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.local.AuthPreferences
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.apiErrorMessage
import org.chessora.app.data.repository.ChessoraRepository

/** Stati della schermata di accesso (vedi LoginScreen.kt). [LoggedIn]/[NeedsProfile] sono
 * eventi "one-shot" consumati dalla UI in una LaunchedEffect - dopo un login Google
 * riuscito, [NeedsProfile] segnala di proseguire con CompleteProfileScreen (l'account
 * esiste ed è già autenticato, ma non ha ancora IdPlayer). */
sealed interface LoginStep {
    data object Idle : LoginStep
    data object Loading : LoginStep
    data object LoggedIn : LoginStep
    data object NeedsProfile : LoginStep
    data class Error(val message: String) : LoginStep
}

class LoginViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
    private val authPreferences: AuthPreferences,
) : ViewModel() {

    private val _step = MutableStateFlow<LoginStep>(LoginStep.Idle)
    val step: StateFlow<LoginStep> = _step

    /** [idToken] già ottenuto da GoogleSignInHelper.signIn - verificato lato server
     * (Google.Apis.Auth), non più uno scambio con Firebase. */
    fun loginWithGoogle(idToken: String) {
        _step.value = LoginStep.Loading
        viewModelScope.launch {
            repository.loginWithGoogle(idToken)
                .onSuccess { response ->
                    AuthSessionPersister.persist(response.auth, displayName = null, authPreferences, clubPreferences)
                    _step.value = if (response.auth.profileComplete) LoginStep.LoggedIn else LoginStep.NeedsProfile
                }
                .onFailure { _step.value = LoginStep.Error(it.apiErrorMessage() ?: "Accesso Google non riuscito. Riprova.") }
        }
    }

    fun googleSignInFailed() {
        _step.value = LoginStep.Error("Accesso Google non riuscito. Riprova.")
    }

    fun resetError() {
        _step.value = LoginStep.Idle
    }
}
