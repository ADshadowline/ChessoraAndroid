package org.chessora.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.data.repository.ChessoraRepository

sealed interface ForgotPasswordStep {
    data object Idle : ForgotPasswordStep
    data object Loading : ForgotPasswordStep
    /** Stesso esito sia che l'indirizzo esista sia che non esista (vedi
     * PlayerAuthController.ForgotPassword) - non rivela mai se un'email è registrata. */
    data object Sent : ForgotPasswordStep
    data object NetworkError : ForgotPasswordStep
}

class ForgotPasswordViewModel(private val repository: ChessoraRepository) : ViewModel() {
    private val _step = MutableStateFlow<ForgotPasswordStep>(ForgotPasswordStep.Idle)
    val step: StateFlow<ForgotPasswordStep> = _step

    fun submit(email: String) {
        if (email.isBlank()) return
        _step.value = ForgotPasswordStep.Loading
        viewModelScope.launch {
            repository.forgotPassword(email.trim())
                .onSuccess { _step.value = ForgotPasswordStep.Sent }
                .onFailure { _step.value = ForgotPasswordStep.NetworkError }
        }
    }
}
