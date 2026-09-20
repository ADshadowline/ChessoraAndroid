package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Un punto mensile dello storico rating Fide - null nelle tre cadenze significa
 * "nessuna partita valutata quel mese", non zero. */
@Serializable
data class PerformancePointDto(
    val year: Int,
    val month: Int,
    val standard: Int? = null,
    val rapid: Int? = null,
    val blitz: Int? = null,
)

/** hasFide=false quando il socio non ha un IdFide associato in anagrafica: in quel
 * caso points è sempre vuoto e la UI non deve provare a disegnare il grafico. */
@Serializable
data class PerformanceHistoryDto(
    val hasFide: Boolean,
    val points: List<PerformancePointDto> = emptyList(),
)
