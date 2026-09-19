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
    // Non null solo per le righe generate da un torneo, rispettivamente da un evento
    // non-torneo (mai insieme) - vedi Chessora.Contracts.Calendar.CalendarEventDto.
    val idTournament: Int? = null,
    val idEvento: Int? = null,
    // Valorizzati solo quando idTournament non è null: il bando di un torneo è già
    // qui nella riga di calendario, non serve una fetch dedicata come per gli eventi.
    val tournamentBandoPath: String? = null,
    val tournamentBandoNomeFile: String? = null,
    // Sfondo della card evento in Home (vedi HomeScreen.AppointmentCard) - diverso dal
    // bando, che è il documento PDF/DOC del torneo.
    val tournamentImmagineCopertinaPath: String? = null,
    // Non null solo se il torneo appartiene a un evento con più tornei "fratelli"
    // (stesso EventGroupId lato server) - serve a raggruppare correttamente in Home,
    // altrimenti ogni fratello produrrebbe la propria card duplicata con lo stesso
    // titolo (Titolo = EventoNome quando ci sono fratelli, vedi TournamentCalendarPlanner).
    val tournamentEventGroupId: String? = null,
)

/**
 * Sottoinsieme minimo di Chessora.Contracts.Eventi.EventoDetailDto (GET
 * /api/eventi/{id}?club=...): qui serve solo per aprire il bando di un evento
 * non-torneo dal calendario/Home - ignoreUnknownKeys nel Json condiviso (vedi
 * NetworkModule) fa sì che gli altri campi della risposta vengano ignorati senza
 * doverli dichiarare tutti.
 */
@Serializable
data class EventoBandoInfo(
    val bandoPath: String? = null,
    val bandoNomeFile: String? = null,
)
