package org.chessora.app.data.local

/**
 * Cache in-memory del token "organizzatore" (ruolo GestoreTornei/Admin, ottenuto via
 * POST api/tourn-organizers/claim - vedi ChessoraRepository.ensureOrganizerAuth) -
 * SEPARATO da AuthSession (il token giocatore): le chiamate verso api/tornei/admin/...
 * lo passano esplicitamente come header Authorization per-richiesta (vedi ChessoraApi),
 * non tramite l'interceptor globale, così un 401/403 su questo token non disconnette la
 * sessione giocatore (vedi NetworkModule). In-memory soltanto: si riottiene in un colpo
 * dalla claim, non serve persisterlo su DataStore. Azzerato al logout (vedi
 * SessionViewModel).
 */
object OrganizerSession {
    @Volatile
    var token: String? = null
}
