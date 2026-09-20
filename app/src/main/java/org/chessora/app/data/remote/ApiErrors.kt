package org.chessora.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class ApiErrorBody(val error: String? = null, val code: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/** [message] è già scritto per essere letto dall'utente (vedi PlayerAuthController.
 * DescribeError lato server), [code] è il nome dell'enum PlayerAuthErrorCode (es.
 * "EmailNotConfirmed") - usato solo dagli endpoint di ui/auth/ per offrire un'azione
 * dedicata (es. "Invia di nuovo l'email") invece del semplice messaggio. Null per
 * qualunque errore che non è una risposta HTTP del nostro Api (rete assente, ecc.). */
data class ApiError(val message: String?, val code: String?)

/** Il corpo di una risposta d'errore HTTP può essere letto una sola volta
 * (ResponseBody.string() chiude lo stream) - questa è l'UNICA funzione che lo fa,
 * [apiErrorMessage] sotto le si appoggia invece di leggerlo di nuovo. */
fun Throwable.apiError(): ApiError? {
    if (this !is HttpException) return null
    val body = response()?.errorBody()?.string() ?: return null
    val parsed = runCatching { errorJson.decodeFromString<ApiErrorBody>(body) }.getOrNull() ?: return null
    return ApiError(parsed.error, parsed.code)
}

/** Messaggio di validazione restituito dal nostro stesso Api (BadRequest/409/... con corpo
 * {"error":"..."}) - a differenza dei messaggi tecnici generici (vedi
 * ui/common/UiState.toFriendlyMessage, che non li mostra mai apposta), questi sono scritti
 * per essere letti dall'utente (spiegano cosa fare, es. "ritira prima l'altra
 * preiscrizione"), quindi vale la pena mostrarli. Null per qualunque altro tipo di errore
 * (rete assente, 500, ecc.) - in quel caso il chiamante mostra il proprio messaggio
 * generico. Non chiamarla insieme ad [apiError]/[apiErrorCode] sulla stessa eccezione: il
 * corpo della risposta si legge una sola volta, usare [apiError] quando servono entrambi. */
fun Throwable.apiErrorMessage(): String? = apiError()?.message
