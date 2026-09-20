package org.chessora.app.data.local

/**
 * Cache in-memory del token di accesso, letta sincronicamente dall'interceptor OkHttp
 * (vedi data/remote/NetworkModule.kt) - DataStore è asincrono (Flow), un Interceptor
 * gira invece su un thread pool sincrono di OkHttp, quindi serve un accesso diretto.
 * Popolata all'avvio da AuthPreferences.accessToken (vedi ChessoraApplication.onCreate)
 * e aggiornata subito a ogni login/logout (vedi ui/auth/AuthSessionPersister.kt) - mai
 * letta da DataStore per il solo scopo di allegare l'header.
 */
object AuthSession {
    @Volatile
    var accessToken: String? = null
}
