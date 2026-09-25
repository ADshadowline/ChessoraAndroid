package org.chessora.app.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Un turno è stato pubblicato mentre l'app era in primo piano (vedi
 * ChessoraFirebaseMessagingService/AppForegroundTracker) - dati minimi, l'app recupera da
 * sola turno/classifica (già pubblici) per trovare la propria scacchiera, vedi
 * ui/pairings/RoundPublishedOverlay.kt. */
data class RoundPublishedEvent(val idTournamentRegistration: Int, val roundNumber: Int, val tournamentName: String)

/** Canale dal servizio FCM (non-Compose) alla UI - montato una sola volta in cima
 * all'albero di navigazione (vedi ChessoraNavHost), cosi' l'overlay compare sopra
 * QUALUNQUE schermata l'utente stia guardando in quel momento. extraBufferCapacity=1:
 * se nessuno sta ancora collezionando (avvio app appena completato) l'evento non va
 * perso, resta in buffer per il primo collector. */
object TournamentRoundEvents {
    private val _events = MutableSharedFlow<RoundPublishedEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<RoundPublishedEvent> = _events

    fun emit(event: RoundPublishedEvent) {
        _events.tryEmit(event)
    }
}
