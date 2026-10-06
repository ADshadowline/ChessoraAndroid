package org.chessora.app.data.repository

import org.chessora.app.data.remote.dto.PlayerGame

/** Cache in-memory (mai persistita, svuotata al riavvio del processo) delle ultime partite di
 * "Le mie partite" caricate - stesso scopo/stesso pattern di [ChessComGameCache]: porta il PGN
 * completo dall'elenco (MyGamesListScreen) al visualizzatore (MyGameViewerScreen) senza una
 * seconda chiamata di rete (Compose Navigation passa solo stringhe negli argomenti di rotta). */
object MyGameCache {
    private val games = mutableMapOf<Int, PlayerGame>()

    fun putAll(list: List<PlayerGame>) {
        list.forEach { games[it.id] = it }
    }

    fun get(id: Int): PlayerGame? = games[id]
}
