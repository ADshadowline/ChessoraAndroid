package org.chessora.app.ui.chessboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica [ChessPositions.fromPgn]/[formatPgnForDisplay] contro una vera partita Chess.com
 * (scaricata via API pubblica il 2026-09-08, vedi commento sulla costante) - copre
 * esattamente il rischio concreto di questa pipeline: un solo token del PGN non ripulito
 * correttamente (intestazioni, commenti orologio "{[%clk ...]}", arrocco "O-O-O", scacco
 * "+", risultato finale) manda in errore l'INTERO parsing (vedi fromMoves), quindi senza
 * questo test un regressione nella pulizia del PGN non verrebbe notata finché qualcuno non
 * apre davvero una partita Chess.com nell'app. */
class ChessPositionsTest {

    @Test
    fun `fromPgn ricostruisce tutte le mosse e la posizione finale coincide con quella riportata da Chess_com`() {
        val positions = ChessPositions.fromPgn(SAMPLE_CHESSCOM_PGN)

        // 36 mosse complete = 72 semimosse + la posizione di partenza.
        assertEquals(73, positions.size)

        // CurrentPosition dichiarata nell'intestazione del PGN stesso - se la pulizia dei
        // token (numeri di turno/commenti/arrocco/scacco) avesse anche un solo errore, la
        // partita si fermerebbe prima e questo confronto fallirebbe.
        val expectedBoardAndTurn = "1k5r/1p3R1p/p2b2p1/3Kp3/3r4/P6P/6P1/8 w - -"
        assertTrue(
            "Posizione finale inattesa: ${positions.last()}",
            positions.last().startsWith(expectedBoardAndTurn),
        )
    }

    @Test
    fun `formatPgnForDisplay produce testo leggibile senza intestazioni, orologio o marcatori del nero`() {
        val display = ChessPositions.formatPgnForDisplay(SAMPLE_CHESSCOM_PGN)

        assertTrue(display.startsWith("1. e4 c5 2. c3 Qa5"))
        assertTrue("Non deve contenere intestazioni PGN", !display.contains("[Event"))
        assertTrue("Non deve contenere l'orologio", !display.contains("%clk"))
        assertTrue("Non deve contenere i marcatori di mossa del nero", !display.contains("..."))
        assertTrue("Non deve contenere il risultato finale", !display.trimEnd().endsWith("0-1"))
        assertTrue("Deve contenere l'arrocco lungo", display.contains("O-O-O"))
    }

    @Test
    fun `formatLichessMovesForDisplay numera le mosse SAN pure di Lichess`() {
        val display = ChessPositions.formatLichessMovesForDisplay("e4 e5 Nf3 Nc6 Bb5")
        assertEquals("1. e4 e5 2. Nf3 Nc6 3. Bb5", display)
    }

    @Test
    fun `fromMoves e fromPgn concordano su una sequenza di mosse pure`() {
        val plain = "e4 e5 Nf3 Nc6 Bb5 a6"
        val viaMoves = ChessPositions.fromMoves(plain)
        val viaPgn = ChessPositions.fromPgn("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6")
        assertEquals(viaMoves, viaPgn)
    }

    @Test
    fun `validateStrict riconosce un PGN completamente valido`() {
        val result = ChessPositions.validateStrict("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6")
        assertEquals(StrictPgnValidation.Valid(6), result)
    }

    @Test
    fun `validateStrict riconosce la partita Chess_com reale di esempio`() {
        val result = ChessPositions.validateStrict(SAMPLE_CHESSCOM_PGN)
        assertEquals(StrictPgnValidation.Valid(72), result)
    }

    @Test
    fun `validateStrict si ferma esattamente al primo token non valido, senza troncare in silenzio`() {
        // "Z9" non è una mossa SAN valida - a differenza di fromMoves (che tornerebbe solo
        // le prime 4 semimosse senza segnalare nulla), qui l'esito deve dire chiaramente
        // "le prime 4 mosse erano buone, la quinta no, il testo ne dichiarava 6".
        val result = ChessPositions.validateStrict("1. e4 e5 2. Nf3 Nc6 3. Z9 a6")
        assertEquals(StrictPgnValidation.Invalid(4, 6), result)
    }

    @Test
    fun `validateStrict su testo vuoto non dichiara nessuna mossa valida`() {
        assertEquals(StrictPgnValidation.Invalid(0, 0), ChessPositions.validateStrict(""))
        assertEquals(StrictPgnValidation.Invalid(0, 0), ChessPositions.validateStrict("   "))
    }

    private companion object {
        /** Partita reale Hikaru vs michobr, Titled Tuesday Blitz dell'8 settembre 2026,
         * scaricata da https://api.chess.com/pub/player/hikaru/games/2026/09 - include
         * intestazioni, commenti orologio su ogni mossa, un arrocco lungo (O-O-O) e un
         * finale con scacchi multipli, proprio i casi che la pulizia deve gestire. */
        const val SAMPLE_CHESSCOM_PGN = """[Event "Live Chess"]
[Site "Chess.com"]
[Date "2026.09.08"]
[Round "-"]
[White "michobr"]
[Black "Hikaru"]
[Result "0-1"]
[Tournament "https://www.chess.com/tournament/live/titled-tuesday-blitz-september-08-2026-31085409"]
[CurrentPosition "1k5r/1p3R1p/p2b2p1/3Kp3/3r4/P6P/6P1/8 w - - 2 37"]
[Timezone "UTC"]
[ECO "B22"]
[ECOUrl "https://www.chess.com/openings/Alapin-Sicilian-Defense-2...Qa5"]
[UTCDate "2026.09.08"]
[UTCTime "15:00:01"]
[WhiteElo "2709"]
[BlackElo "3370"]
[TimeControl "300"]
[Termination "Hikaru won by resignation"]
[StartTime "15:00:01"]
[EndDate "2026.09.08"]
[EndTime "15:07:53"]
[Link "https://www.chess.com/game/live/183193101523"]

1. e4 {[%clk 0:04:53.5]} 1... c5 {[%clk 0:04:54.8]} 2. c3 {[%clk 0:04:52.3]} 2... Qa5 {[%clk 0:04:49.7]} 3. Na3 {[%clk 0:04:50.7]} 3... e6 {[%clk 0:04:37.5]} 4. d4 {[%clk 0:04:49.7]} 4... cxd4 {[%clk 0:04:36.2]} 5. Nc4 {[%clk 0:04:48.9]} 5... Qc7 {[%clk 0:04:35.1]} 6. cxd4 {[%clk 0:04:45.6]} 6... Nf6 {[%clk 0:04:33.5]} 7. e5 {[%clk 0:04:38.8]} 7... Nd5 {[%clk 0:04:29.5]} 8. a3 {[%clk 0:04:33.4]} 8... d6 {[%clk 0:04:24.4]} 9. Bd3 {[%clk 0:04:10.4]} 9... dxe5 {[%clk 0:04:16.2]} 10. dxe5 {[%clk 0:04:09.2]} 10... a6 {[%clk 0:03:58.2]} 11. Qg4 {[%clk 0:03:23.8]} 11... g6 {[%clk 0:03:24.4]} 12. Qg3 {[%clk 0:03:05.7]} 12... Nd7 {[%clk 0:03:17.8]} 13. Nf3 {[%clk 0:02:55.5]} 13... Nc5 {[%clk 0:03:06.1]} 14. Bc2 {[%clk 0:02:41.6]} 14... Ne4 {[%clk 0:03:01.4]} 15. Qg4 {[%clk 0:01:58]} 15... f5 {[%clk 0:02:35.8]} 16. exf6 {[%clk 0:01:37.7]} 16... Ndxf6 {[%clk 0:02:34.7]} 17. Qf4 {[%clk 0:01:00.3]} 17... Qxc4 {[%clk 0:02:33.6]} 18. Bd1 {[%clk 0:00:47]} 18... Bd6 {[%clk 0:02:28.5]} 19. Qe3 {[%clk 0:00:45.3]} 19... Ng4 {[%clk 0:02:26.1]} 20. Qb3 {[%clk 0:00:36.8]} 20... Qxb3 {[%clk 0:02:23.9]} 21. Bxb3 {[%clk 0:00:36.7]} 21... Ngxf2 {[%clk 0:02:23.5]} 22. Rf1 {[%clk 0:00:35.4]} 22... Nd3+ {[%clk 0:02:20.4]} 23. Ke2 {[%clk 0:00:35.1]} 23... Nxc1+ {[%clk 0:02:20]} 24. Raxc1 {[%clk 0:00:35]} 24... Bd7 {[%clk 0:02:19.1]} 25. Nd4 {[%clk 0:00:34.5]} 25... Nc5 {[%clk 0:02:17]} 26. Bc4 {[%clk 0:00:33.4]} 26... e5 {[%clk 0:02:16.5]} 27. Nf3 {[%clk 0:00:32.5]} 27... Bg4 {[%clk 0:02:12.3]} 28. Rcd1 {[%clk 0:00:30.8]} 28... O-O-O {[%clk 0:02:10.7]} 29. Ke3 {[%clk 0:00:30.4]} 29... Na4 {[%clk 0:02:09.3]} 30. h3 {[%clk 0:00:27.8]} 30... Nxb2 {[%clk 0:02:07.9]} 31. Rc1 {[%clk 0:00:23]} 31... Bxf3 {[%clk 0:02:07]} 32. Rxf3 {[%clk 0:00:18.8]} 32... Nxc4+ {[%clk 0:02:05.7]} 33. Rxc4+ {[%clk 0:00:18.7]} 33... Kb8 {[%clk 0:02:05.5]} 34. Rf7 {[%clk 0:00:18.6]} 34... Rc8 {[%clk 0:02:03.6]} 35. Ke4 {[%clk 0:00:18.5]} 35... Rxc4+ {[%clk 0:02:02.7]} 36. Kd5 {[%clk 0:00:18.4]} 36... Rd4+ {[%clk 0:02:02.1]} 0-1
"""
    }
}
