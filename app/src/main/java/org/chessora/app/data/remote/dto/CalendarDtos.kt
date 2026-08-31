package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Calendar.EventTypeDto (GET /api/event-types). */
@Serializable
data class EventType(
    val id: Int,
    val description: String? = null,
    val eloVariation: Boolean? = null,
    val isTournament: Boolean? = null,
    val isTraining: Boolean? = null,
    val isConvention: Boolean? = null,
)

/**
 * Specchio di Chessora.Contracts.Calendar.CalendarEventDto (GET /api/calendar).
 * [eventDate] e [startTime] sono già stringhe pre-formattate lato server
 * (rispettivamente "yyyy-MM-dd" e "HH:mm" circa) pensate per essere mostrate
 * direttamente senza ulteriore parsing - [eventDateTime] invece è il timestamp
 * completo ISO-8601, utile per ordinare/raggruppare gli eventi per giorno.
 */
@Serializable
data class CalendarEvent(
    val id: Int,
    val eventTypeId: Int,
    val eventTypeDescription: String? = null,
    val eventTypeEloCapable: Boolean? = null,
    val eventTypeIsTournament: Boolean? = null,
    val eventTypeIsTraining: Boolean? = null,
    val eventTypeIsConvention: Boolean? = null,
    val title: String? = null,
    val eventDateTime: String,
    val eventDate: String,
    val startTime: String,
    val eloVariation: Boolean? = null,
    val note: String? = null,
)
