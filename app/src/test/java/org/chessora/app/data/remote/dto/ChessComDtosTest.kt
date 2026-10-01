package org.chessora.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica che i DTO di Chess.com si decodifichino correttamente contro una vera risposta
 * dell'API pubblica (un mese di .../games/archives, scaricato il 2026-09-30) - copre il
 * rischio concreto di questa integrazione: un campo mancante/di tipo diverso da quello
 * atteso farebbe fallire silenziosamente kotlinx.serialization (eccezione catturata da
 * ChessComRepository.fetchGames via runCatching), facendo sparire tutte le partite
 * dall'elenco "Scacchi Online" senza alcun errore visibile. */
class ChessComDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodifica una risposta reale di un mese di partite`() {
        val response = json.decodeFromString(ChessComGamesResponse.serializer(), SAMPLE_MONTH_RESPONSE)

        assertEquals(1, response.games.size)
        val game = response.games[0]
        assertEquals("https://www.chess.com/game/live/183193101523", game.url)
        assertEquals("blitz", game.timeClass)
        assertEquals("chess", game.rules)
        assertTrue(game.rated)
        assertEquals(1788880073L, game.endTime)
        assertNotNull(game.pgn)

        assertEquals("michobr", game.white?.username)
        assertEquals(2709, game.white?.rating)
        assertEquals("resigned", game.white?.result)

        assertEquals("Hikaru", game.black?.username)
        assertEquals(3370, game.black?.rating)
        assertEquals("win", game.black?.result)
    }

    @Test
    fun `decodifica l'elenco degli archivi mensili`() {
        val response = json.decodeFromString(
            ChessComArchivesResponse.serializer(),
            """{"archives":["https://api.chess.com/pub/player/hikaru/games/2026/08","https://api.chess.com/pub/player/hikaru/games/2026/09"]}""",
        )
        assertEquals(2, response.archives.size)
        assertEquals("https://api.chess.com/pub/player/hikaru/games/2026/09", response.archives[1])
    }

    private companion object {
        /** Un singolo oggetto preso da una vera risposta di
         * https://api.chess.com/pub/player/hikaru/games/2026/09 (campi "accuracies"/"tcn"/
         * "uuid"/"initial_setup"/"fen"/"eco"/"tournament"/"@id"/"uuid" per-player lasciati
         * apposta - nessuno di questi è nel DTO, verificano che ignoreUnknownKeys funzioni
         * davvero sul payload reale, non solo su un fixture già ridotto ai campi usati). */
        const val SAMPLE_MONTH_RESPONSE = """{"games":[{
          "url": "https://www.chess.com/game/live/183193101523",
          "pgn": "[Event \"Live Chess\"]\n1. e4 c5 0-1\n",
          "time_control": "300",
          "end_time": 1788880073,
          "rated": true,
          "accuracies": {"white": 85.96, "black": 93.5},
          "tcn": "mCYIks7Gbq0SlBIBqAGYsB",
          "uuid": "f78fcfe7-ab95-11f1-8ffd-6cfe54652c60",
          "initial_setup": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          "fen": "1k5r/1p3R1p/p2b2p1/3Kp3/3r4/P6P/6P1/8 w - - 2 37",
          "time_class": "blitz",
          "rules": "chess",
          "white": {"rating": 2709, "result": "resigned", "@id": "https://api.chess.com/pub/player/michobr", "username": "michobr", "uuid": "b721e79c-cbd4-11e7-8028-000000000000"},
          "black": {"rating": 3370, "result": "win", "@id": "https://api.chess.com/pub/player/hikaru", "username": "Hikaru", "uuid": "6f4deb88-7718-11e3-8016-000000000000"},
          "eco": "https://www.chess.com/openings/Alapin-Sicilian-Defense-2...Qa5",
          "tournament": "https://api.chess.com/pub/tournament/titled-tuesday-blitz-september-08-2026-31085409"
        }]}"""
    }
}
