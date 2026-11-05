package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Tournaments.NetworkTournamentDto (GET
 * api/tornei/in-rete) - tornei della rete vesus.org più quelli creati su Chessora non
 * ancora pubblicati lì, gli stessi di "Tornei in rete" su tourn.chessora.org. */
@Serializable
data class NetworkTournament(
    val nome: String,
    val inizio: String? = null,
    val fine: String? = null,
    val localita: String? = null,
    val provincia: String? = null,
    val regione: String? = null,
    val organizzatore: String? = null,
    val federazione: String? = null,
    val sito: String? = null,
    val fonte: String = "",
    val id: Int? = null,
    val idEventoVesus: Int? = null,
    val clubNameChessora: String? = null,
    val vesusUrl: String? = null,
)

@Serializable
data class NetworkTournamentsPage(
    val items: List<NetworkTournament> = emptyList(),
    val totalCount: Int = 0,
)
