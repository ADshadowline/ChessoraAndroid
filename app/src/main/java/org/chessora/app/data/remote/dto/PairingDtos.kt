package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Tournaments.PairingPlayerDto - un giocatore così come
 * compare su una scacchiera (nome/rating/titolo). idPlayer/photoPath (null se l'iscritto
 * non ha un IdPlayer risolto) servono a ui/pairings/RoundPublishedOverlay.kt per trovare
 * "qual è la mia scacchiera" quando arriva la push di turno pubblicato (che non porta un
 * riferimento diretto alla scacchiera del destinatario) e per mostrarne la foto. */
@Serializable
data class PairingPlayer(
    val id: Int,
    val name: String,
    val rating: Int? = null,
    val title: String? = null,
    val idPlayer: Int? = null,
    val photoPath: String? = null,
)

/** Specchio di Chessora.Contracts.Tournaments.PairingDto - una scacchiera di un turno.
 * Black è null per un bye (isBye=true, il giocatore riposa e riceve 1 punto). Result usa
 * gli stessi codici compatti del sito ("1-0","0-1","1/2-1/2","1-0F","0-1F","0-0F", null se
 * non ancora giocata). */
@Serializable
data class Pairing(
    val id: Int,
    val board: Int,
    val white: PairingPlayer? = null,
    val black: PairingPlayer? = null,
    val isBye: Boolean = false,
    val result: String? = null,
)

/** Specchio di Chessora.Contracts.Tournaments.RoundDto - usato sia da GET
 * /api/tornei/{id}/rounds (pubblico, solo turni Published) sia da GET
 * /api/tornei/admin/{id}/rounds (organizzatore, vedi ui/tournamentmanager/: include anche
 * i turni Pending ancora in revisione, non pubblicati). */
@Serializable
data class TournamentRound(
    val id: Int,
    val roundNumber: Int,
    val status: String,
    val pairings: List<Pairing> = emptyList(),
)

/** Specchio di Chessora.Contracts.Tournaments.StandingsRowDto (GET
 * /api/tornei/{id}/standings, pubblico) - Buchholz è l'unico spareggio calcolato in
 * questa prima fase (somma dei punteggi finali degli avversari affrontati). */
@Serializable
data class StandingsRow(
    val rank: Int,
    val entrantId: Int,
    val name: String,
    val rating: Int? = null,
    val score: Double = 0.0,
    val buchholz: Double = 0.0,
)

/** Specchio di Chessora.Contracts.Tournaments.EntrantDto (GET
 * api/tornei/admin/{id}/entrants, organizzatore) - un iscritto EFFETTIVO al torneo,
 * mostrato in ui/tournamentmanager/ prima che venga generato il primo turno (quando non
 * c'è ancora nessuna scacchiera da vedere). */
@Serializable
data class EntrantDto(
    val id: Int,
    val name: String,
    val idFide: String? = null,
    val idPlayer: Int? = null,
    val rating: Int? = null,
    val title: String? = null,
    val federation: String? = null,
    val tpn: Int = 0,
    val active: Boolean = true,
)

/** Specchio di Chessora.Contracts.Tournaments.SubmitResultRequest - corpo di PUT
 * api/tornei/admin/{id}/pairings/{pairingId}/result (organizzatore, vedi
 * ui/tournamentmanager/). result=null annulla un risultato già inserito. */
@Serializable
data class SubmitResultRequestDto(val result: String? = null)

/** Specchio di Chessora.Contracts.Tournaments.TournamentViewStateDto - su quale vista è
 * "puntato" un torneo in questo momento per il suo organizzatore (sia risposta di GET che
 * corpo di PUT .../view-state), condivisa tra sito e app: vedi
 * ui/tournamentmanager/TournamentViewMode. */
@Serializable
data class TournamentViewStateDto(val view: String)

/** "Pairings"/"Standings"/"Prizes" lato Kotlin, mai una stringa libera in giro -
 * [wireValue] è esattamente TournamentViewStateCodes lato server. PREMIAZIONE è
 * condivisa come le altre due: se questo client apre/chiude la premiazione, il sito la
 * segue entro pochi secondi (vedi pushViewState/pollViewState in
 * abbinamenti-risultati.html) e viceversa. */
enum class TournamentViewMode(val wireValue: String) {
    PAIRINGS("Pairings"),
    STANDINGS("Standings"),
    PREMIAZIONE("Prizes"),
    ;

    companion object {
        fun fromWire(value: String): TournamentViewMode = entries.firstOrNull { it.wireValue == value } ?: PAIRINGS
    }
}
