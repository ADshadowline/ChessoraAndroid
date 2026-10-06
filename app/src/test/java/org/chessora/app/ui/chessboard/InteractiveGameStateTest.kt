package org.chessora.app.ui.chessboard

import com.github.bhlangonijr.chesslib.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica la logica di [InteractiveGameState] (componente a tocchi di "Le mie partite")
 * senza passare da Compose: la classe usa mutableStateOf/mutableStateListOf ma è pura logica
 * di scacchi, leggibile/scrivibile da un test JVM come un oggetto Kotlin qualunque. Copre
 * esattamente il rischio concreto di questa classe: generare un PGN corretto a partire da
 * mosse applicate a tocchi, tenendo Board e la lista mosse sincronizzati attraverso annulla. */
class InteractiveGameStateTest {

    @Test
    fun `selezionare un pezzo proprio e poi una destinazione legale applica la mossa`() {
        val state = InteractiveGameState()
        assertEquals(0, state.moveCount)

        state.onSquareTapped(Square.E2)
        assertEquals(Square.E2, state.selectedSquare)

        state.onSquareTapped(Square.E4)
        assertEquals(1, state.moveCount)
        assertNull(state.selectedSquare)
        assertTrue("Il pedone bianco deve essere ora su e4", state.fen.startsWith("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR"))
    }

    @Test
    fun `toccare una casella vuota senza selezione non seleziona nulla`() {
        val state = InteractiveGameState()
        state.onSquareTapped(Square.E4) // vuota all'inizio, nessun pezzo bianco li'
        assertNull(state.selectedSquare)
        assertEquals(0, state.moveCount)
    }

    @Test
    fun `toccare di nuovo la casella selezionata la deseleziona`() {
        val state = InteractiveGameState()
        state.onSquareTapped(Square.E2)
        state.onSquareTapped(Square.E2)
        assertNull(state.selectedSquare)
    }

    @Test
    fun `legalDestinationsFrom per il pedone e2 all'inizio partita`() {
        val state = InteractiveGameState()
        val destinations = state.legalDestinationsFrom(Square.E2)
        assertEquals(setOf(Square.E3, Square.E4), destinations)
    }

    @Test
    fun `annulla ultima mossa ripristina la posizione e il conteggio`() {
        val state = InteractiveGameState()
        val startFen = state.fen
        state.onSquareTapped(Square.E2)
        state.onSquareTapped(Square.E4)
        assertEquals(1, state.moveCount)

        state.undoLast()
        assertEquals(0, state.moveCount)
        assertFalse(state.canUndo)
        assertEquals(startFen, state.fen)
    }

    @Test
    fun `toPgn produce il testo PGN corretto per una breve sequenza di mosse`() {
        val state = InteractiveGameState()
        assertNull(state.toPgn())

        state.onSquareTapped(Square.E2)
        state.onSquareTapped(Square.E4)
        state.onSquareTapped(Square.E7)
        state.onSquareTapped(Square.E5)
        state.onSquareTapped(Square.G1)
        state.onSquareTapped(Square.F3)

        assertEquals(3, state.moveCount)
        assertEquals("1. e4 e5 2. Nf3", state.toPgn())

        // Il PGN generato deve anche essere accettato dal validatore severo usato per il
        // percorso "Incolla PGN" - le due modalità di inserimento devono produrre dati
        // compatibili.
        val validation = ChessPositions.validateStrict(state.toPgn()!!)
        assertEquals(StrictPgnValidation.Valid(3), validation)
    }

    @Test
    fun `una mossa di cattura con due candidati non ambigui non genera una promozione`() {
        val state = InteractiveGameState()
        state.onSquareTapped(Square.E2)
        state.onSquareTapped(Square.E4)
        state.onSquareTapped(Square.D7)
        state.onSquareTapped(Square.D5)
        state.onSquareTapped(Square.E4)
        state.onSquareTapped(Square.D5) // exd5, cattura semplice, nessuna scelta di promozione
        assertNull(state.pendingPromotion)
        assertEquals(3, state.moveCount) // e4, d5, exd5
    }
}
