package org.chessora.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Serializable
private data class ApiErrorBody(val error: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/** Messaggio di validazione restituito dal nostro stesso Api (BadRequest con corpo
 * {"error":"..."}) - a differenza dei messaggi tecnici generici (vedi
 * ui/common/UiState.toFriendlyMessage, che non li mostra mai apposta), questi sono scritti
 * per essere letti dall'utente (spiegano cosa fare, es. "ritira prima l'altra
 * preiscrizione"), quindi vale la pena mostrarli. Null per qualunque altro tipo di errore
 * (rete assente, 500, ecc.) - in quel caso il chiamante mostra il proprio messaggio
 * generico. */
fun Throwable.apiErrorMessage(): String? {
    if (this !is HttpException) return null
    val body = response()?.errorBody()?.string() ?: return null
    return runCatching { errorJson.decodeFromString<ApiErrorBody>(body).error }.getOrNull()
}
