package org.chessora.app.data.repository

import kotlinx.serialization.json.Json
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.LichessGame

/** Wrapper minimale sull'API pubblica di Lichess (nessuna autenticazione: le partite di
 * un utente sono pubbliche, vedi ui/settings/ per la sola configurazione dello username)
 * - separato da ChessoraRepository perché parla con un host completamente diverso
 * (lichess.org, non api.chessora.org), vedi NetworkModule.fetchLichessGamesNdjson/
 * fetchLichessGameJson. */
class LichessRepository {
    private val json = Json { ignoreUnknownKeys = true }

    /** Le ultime [max] partite pubbliche di [username] - vedi
     * ui/performance/LichessGamesViewModel. */
    suspend fun fetchGames(username: String, max: Int = 50): Result<List<LichessGame>> = runCatching {
        val body = NetworkModule.fetchLichessGamesNdjson(username.trim(), max)
        body.lineSequence()
            .filter { it.isNotBlank() }
            .map { json.decodeFromString(LichessGame.serializer(), it) }
            .toList()
    }

    /** Una singola partita, per LichessGameViewerScreen. */
    suspend fun fetchGame(gameId: String): Result<LichessGame> = runCatching {
        val body = NetworkModule.fetchLichessGameJson(gameId)
        json.decodeFromString(LichessGame.serializer(), body)
    }
}
