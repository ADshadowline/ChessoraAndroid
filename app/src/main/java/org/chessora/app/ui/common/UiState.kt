package org.chessora.app.ui.common

/**
 * Stato generico di caricamento dati usato da (quasi) ogni ViewModel di questa
 * app: evita di ripetere lo stesso pattern Loading/Success/Error in ognuno.
 * Non è pensato per casi con paginazione/aggiornamento incrementale - per
 * quelli, un ViewModel può comunque definire il proprio stato più ricco.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/** Scorciatoia per mappare un Result<T> di ChessoraRepository in un UiState<T>. */
fun <T> Result<T>.toUiState(): UiState<T> =
    fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { UiState.Error(it.toFriendlyMessage()) },
    )

/**
 * Messaggio mostrato all'utente per qualunque errore di rete: mai il dettaglio
 * tecnico dell'eccezione (es. "HTTP 400 Bad Request" di retrofit2.HttpException,
 * o "Unable to resolve host" di una UnknownHostException) - contro quel dettaglio
 * l'utente non può fare nulla se non riprovare più tardi, quindi il messaggio è
 * sempre lo stesso, generico e in italiano.
 */
fun Throwable.toFriendlyMessage(): String =
    "Il server al momento non risponde. Riprova più tardi."
