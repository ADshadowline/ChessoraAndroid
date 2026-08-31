package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Tornei.TorneoDataDto - una singola sessione di gioco. */
@Serializable
data class TorneoData(
    val dataOra: String,
    val idCalendar: Int? = null,
    val sortOrder: Int = 0,
)

/** Specchio di Chessora.Contracts.Tornei.TorneoCostoDto - costo per categoria tesseramento. */
@Serializable
data class TorneoCosto(
    val idTypeSubscription: Int,
    val typeSubscriptionNome: String,
    val costo: Double,
)

/** Specchio di Chessora.Contracts.Tornei.TorneoPremioDto. */
@Serializable
data class TorneoPremio(
    val descrizione: String,
    val importo: Double,
    val sortOrder: Int = 0,
)

/**
 * Specchio completo di Chessora.Contracts.Tornei.TorneoDto (GET /api/tornei).
 * [bandoValori] è presente solo se [idBandoTemplate] non è null: quando c'è,
 * la schermata di dettaglio torneo mostra un pulsante "Vedi locandina" che apre
 * GET /api/bandi/render/{id} (HTML pronto, va aperto in una WebView o nel
 * browser di sistema - vedi ui/tornei/TorneoDetailScreen.kt).
 */
@Serializable
data class Torneo(
    val id: Int,
    val idEventType: Int,
    val eventTypeDescription: String? = null,
    val titolo: String,
    val note: String? = null,
    val idSetChessWatch: Int? = null,
    val cadenzaNome: String,
    val minutiIniziali: Int,
    val incrementoSecondi: Int,
    val ritardoSecondi: Int,
    val mosseControllo: Int? = null,
    val minutiAggiuntivi: Int? = null,
    val variazioneElo: Boolean = false,
    val idArbitri: List<Int> = emptyList(),
    val arbitriNomiCompleti: List<String> = emptyList(),
    val montePremi: Double = 0.0,
    val idBandoTemplate: Int? = null,
    val bandoValori: Map<String, String> = emptyMap(),
    val date: List<TorneoData> = emptyList(),
    val costi: List<TorneoCosto> = emptyList(),
    val premi: List<TorneoPremio> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

/** Specchio di Chessora.Contracts.Tornei.NextTournamentDto (GET /api/tornei/next-upcoming). */
@Serializable
data class NextTournament(
    val titolo: String,
    val dataOra: String,
)
