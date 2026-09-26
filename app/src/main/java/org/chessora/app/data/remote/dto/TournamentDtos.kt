package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Tournaments.PublicTournamentSummaryDto (GET
 * /api/tornei) - elenco pubblico di TUTTI i tornei, su tutti i circoli (la tabella
 * orga.TournamentRegistrations non ha una colonna IdClub). Usato per la pagina di
 * dettaglio/iscrizione di un torneo aperto dalla Home - niente turno/giorni di gioco,
 * solo l'evento nel suo insieme. */
@Serializable
data class TournamentSummary(
    val id: Int,
    val nome: String,
    val eventoNome: String,
    val eventGroupId: String? = null,
    val inizio: String,
    val fine: String,
    val localita: String? = null,
    val provincia: String? = null,
    val sede: String? = null,
    val federazione: String,
    val tipologiaTorneo: Int,
    val tipologiaTempo: Int,
    val variante: Int,
    val modalitaPartecipazione: Int,
    val bandoPath: String? = null,
    val bandoNomeFile: String? = null,
    val immagineCopertinaPath: String? = null,
    val immagineCopertinaNomeFile: String? = null,
    val paginaWeb: String? = null,
    val nPreRegisteredPlayers: Int? = null,
    val nPlayers: Int? = null,
    val limiteIscrizioni: Int = -1,
    // Tempo di gioco (es. "15+10") - mostrato per ogni torneo "fratello" dello stesso
    // evento nella schermata di dettaglio, per poter scegliere a colpo d'occhio.
    val tempoMinuti: Int? = null,
    val tempoIncremento: Int? = null,
    val tempoMosse: Int? = null,
    // Periodo di iscrizione (null = nessun vincolo su quel lato) - l'enforcement reale
    // resta lato server (TournamentRegistrationService.PreRegisterAsync), questi campi
    // servono solo a mostrare/disabilitare subito il pulsante "Preiscriviti" senza
    // dover tentare la chiamata e mostrare l'errore del server.
    val inizioIscrizioni: String? = null,
    val fineIscrizioni: String? = null,
    // Ciclo di vita reale (0=Creato,1=InCorso,2=Concluso - vedi
    // TournamentRegistrationCodes.LifecycleStatus) - un torneo InCorso non accetta più
    // preiscrizioni (stesso controllo lato server) e in "Iscrizioni ai tornei"/Home mostra
    // il pallino verde lampeggiante che porta agli abbinamenti/classifica invece che al
    // dettaglio torneo (vedi RegistrationsScreen/ui/pairings/).
    val lifecycleStatus: Int = 0,
    // Numero di turni previsti - serve a ui/tournamentmanager/ per sapere quando l'ultimo
    // turno generato è anche l'ultimo del torneo (mostra "Premiazione" invece di "Genera
    // turno successivo", vedi TournamentManagerRoundsScreen). 0 = non impostato/sconosciuto
    // (torneo creato prima che questo campo esistesse).
    val turni: Int = 0,
)

/** Specchio di Chessora.Contracts.Tournaments.TournamentRegistrationCountDto (risposta
 * di POST /api/tornei/{id}/registrati). */
@Serializable
data class TournamentRegistrationCount(
    val nPreRegisteredPlayers: Int,
    val limiteIscrizioni: Int,
)

/** Corpo vuoto ma esplicito per POST /api/tornei/{id}/registrati - IIS (Aruba,
 * produzione) rifiuta con 411 "Length Required" una POST senza alcun body/
 * Content-Length (stesso problema riscontrato e risolto sul sito tourn.chessora.org). */
@Serializable
class EmptyRequestBody

/** Specchio di Chessora.Contracts.Tournaments.TournamentPreRegistrationResultDto -
 * PREISCRIZIONE (non conferma di partecipazione, richiede login - vedi ui/auth/):
 * userId è l'AspNetUsers.Id dell'utente autenticato che ha effettuato la preiscrizione. */
@Serializable
data class TournamentPreRegistrationResult(
    val userId: String,
    val nPreRegisteredPlayers: Int,
    val limiteIscrizioni: Int,
)

/** Specchio di Chessora.Contracts.Tournaments.PublicRegisteredPlayerDto (GET
 * /api/tornei/{id}/iscritti) - elenco pubblico dei preiscritti a un torneo, nessuna
 * autenticazione richiesta. rating è sempre valorizzato (1440 di default per chi non
 * ha un Elo FIDE reale, risolto lato server). idPlayer è null se non risolvibile da
 * idFide (nessuna chat possibile con questo iscritto, vedi TournamentDetailScreen). */
@Serializable
data class RegisteredPlayer(
    val name: String,
    val idFide: String? = null,
    val rating: Int,
    val idPlayer: Int? = null,
)
