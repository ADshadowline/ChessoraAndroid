package org.chessora.app.ui.mygames

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import org.chessora.app.R
import org.chessora.app.data.remote.dto.OpponentColor
import org.chessora.app.data.remote.dto.PlayerGame
import org.chessora.app.ui.chessboard.ChessPositions
import org.chessora.app.ui.chessboard.PIECE_DRAWABLES
import org.chessora.app.ui.chessboard.parseFenPlacement
import org.chessora.app.ui.common.toItalianDate
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** PDF delle partite scelte in "Le mie partite": una partita per pagina (le mosse proseguono
 * sulla pagina successiva se non ci stanno), con intestazione, risultato, posizione finale e
 * mosse numerate su tre colonne. Generato sul telefono con [PdfDocument] di Android, senza
 * librerie né chiamate di rete: i dati sono già tutti nel PGN. */
object MyGamesPdf {
    private const val PAGE_WIDTH = 595 // A4 in punti PDF (1/72 di pollice)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val BOARD_SIZE = 216f
    private const val MOVE_LINE_HEIGHT = 14f
    private const val MOVE_COLUMNS = 3
    private const val FOOTER_HEIGHT = 30f

    private val LIGHT_SQUARE = Color.rgb(0xEE, 0xDD, 0xB0)
    private val DARK_SQUARE = Color.rgb(0xB4, 0x79, 0x4F)

    /** Scrive il PDF in cache/pdf e restituisce il file, da condividere via FileProvider. */
    fun write(context: Context, games: List<PlayerGame>, myName: String?): File {
        val document = PdfDocument()
        val generatedAt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date())
        var pageNumber = 0
        try {
            games.forEach { game ->
                val positions = ChessPositions.fromPgn(game.pgn)
                val moves = ChessPositions.sanMovesForDisplay(ChessPositions.formatPgnForDisplay(game.pgn), positions.size)
                // Righe "12. Nf3 Nc6": una per mossa piena.
                val moveLines = moves.chunked(2).mapIndexed { i, pair -> "${i + 1}. ${pair.joinToString(" ")}" }

                var lineIndex = 0
                var firstPage = true
                while (firstPage || lineIndex < moveLines.size) {
                    pageNumber++
                    val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    val canvas = page.canvas
                    var y = MARGIN
                    if (firstPage) {
                        y = drawHeader(context, canvas, game, myName, y)
                        drawBoard(context, canvas, positions.last(), MARGIN + (PAGE_WIDTH - 2 * MARGIN - BOARD_SIZE) / 2, y,
                            flipped = game.opponentColor == OpponentColor.WHITE)
                        y += BOARD_SIZE + 24f
                    } else {
                        y = drawContinuationHeader(context, canvas, game, y)
                    }
                    lineIndex = drawMoves(canvas, moveLines, lineIndex, y)
                    drawFooter(context, canvas, generatedAt, pageNumber)
                    document.finishPage(page)
                    firstPage = false
                }
            }

            val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // solo l'ultimo PDF generato, niente accumulo in cache
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ITALY).format(Date())
            val file = File(dir, "partite-$stamp.pdf")
            file.outputStream().use { document.writeTo(it) }
            return file
        } finally {
            document.close()
        }
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun drawHeader(context: Context, canvas: Canvas, game: PlayerGame, myName: String?, top: Float): Float {
        val me = myName?.takeIf { it.isNotBlank() } ?: context.getString(R.string.mygames_pdf_me)
        val (white, black) = when (game.opponentColor) {
            OpponentColor.WHITE -> game.opponentName to me
            OpponentColor.BLACK -> me to game.opponentName
            else -> me to game.opponentName
        }
        var y = top + 18f
        val title = textPaint(16f, bold = true)
        canvas.drawText(fitText("$white – $black", title, PAGE_WIDTH - 2 * MARGIN - 70f), MARGIN, y, title)
        resultOf(game.pgn)?.let { result ->
            val resultPaint = textPaint(16f, bold = true).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(result, PAGE_WIDTH - MARGIN, y, resultPaint)
        }
        y += 16f
        val sub = textPaint(10f, color = Color.DKGRAY)
        val colours = if (game.opponentColor == null) "" else
            context.getString(R.string.mygames_pdf_colours, white, black) + " · "
        val details = buildList {
            game.tournamentName?.let { add(it) }
            game.round?.let { add(context.getString(R.string.mygames_round_short, it)) }
            add(game.datePlayed.toItalianDate())
            val cadenza = context.getString(Cadenza.fromTipologiaTempo(game.tipologiaTempo).labelRes)
            add(game.timeControl?.let { "$cadenza $it" } ?: cadenza)
        }.joinToString(" · ")
        canvas.drawText(fitText(colours + details, sub, PAGE_WIDTH - 2 * MARGIN), MARGIN, y, sub)
        y += 10f
        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, line)
        return y + 16f
    }

    private fun drawContinuationHeader(context: Context, canvas: Canvas, game: PlayerGame, top: Float): Float {
        val paint = textPaint(10f, color = Color.DKGRAY)
        canvas.drawText(context.getString(R.string.mygames_pdf_continued, game.opponentName), MARGIN, top + 10f, paint)
        return top + 30f
    }

    /** Posizione finale, dal punto di vista del socio (come nel visualizzatore). I pezzi sono
     * gli stessi vettoriali della scacchiera dell'app, disegnati a risoluzione 4x e poi
     * rimpiccioliti: i VectorDrawable passano da una bitmap, che alla scala 1:1 dei punti
     * PDF verrebbe sgranata. */
    private fun drawBoard(context: Context, canvas: Canvas, fen: String, left: Float, top: Float, flipped: Boolean) {
        val board = parseFenPlacement(fen)
        val square = BOARD_SIZE / 8
        val fill = Paint()
        val coordinate = textPaint(6f, bold = true)
        for (displayRank in 0..7) {
            for (displayFile in 0..7) {
                val rank = if (flipped) 7 - displayRank else displayRank
                val file = if (flipped) 7 - displayFile else displayFile
                val isLight = (rank + file) % 2 == 0
                val x = left + displayFile * square
                val y = top + displayRank * square
                fill.color = if (isLight) LIGHT_SQUARE else DARK_SQUARE
                canvas.drawRect(x, y, x + square, y + square, fill)
                PIECE_DRAWABLES[board[rank][file]]?.let { res ->
                    ContextCompat.getDrawable(context, res)?.let { drawable ->
                        val scale = 4f
                        canvas.save()
                        canvas.translate(x, y)
                        canvas.scale(1 / scale, 1 / scale)
                        val inset = (square * 0.06f * scale).toInt()
                        drawable.setBounds(inset, inset, (square * scale).toInt() - inset, (square * scale).toInt() - inset)
                        drawable.draw(canvas)
                        canvas.restore()
                    }
                }
                coordinate.color = if (isLight) DARK_SQUARE else LIGHT_SQUARE
                if (displayFile == 0) canvas.drawText((8 - rank).toString(), x + 1.5f, y + 7f, coordinate)
                if (displayRank == 7) canvas.drawText(('a' + file).toString(), x + square - 5f, y + square - 2f, coordinate)
            }
        }
        val border = Paint().apply { style = Paint.Style.STROKE; color = Color.DKGRAY; strokeWidth = 0.8f }
        canvas.drawRect(left, top, left + BOARD_SIZE, top + BOARD_SIZE, border)
    }

    /** Mosse su [MOVE_COLUMNS] colonne, dall'alto in basso e poi a destra; restituisce
     * l'indice della prima riga che non ci è stata (da continuare sulla pagina dopo). */
    private fun drawMoves(canvas: Canvas, lines: List<String>, from: Int, top: Float): Int {
        if (lines.isEmpty()) return 0
        val paint = textPaint(10f)
        val columnWidth = (PAGE_WIDTH - 2 * MARGIN) / MOVE_COLUMNS
        val rowsPerColumn = ((PAGE_HEIGHT - MARGIN - FOOTER_HEIGHT - top) / MOVE_LINE_HEIGHT).toInt().coerceAtLeast(1)
        var index = from
        for (column in 0 until MOVE_COLUMNS) {
            for (row in 0 until rowsPerColumn) {
                if (index >= lines.size) return index
                canvas.drawText(
                    fitText(lines[index], paint, columnWidth - 8f),
                    MARGIN + column * columnWidth,
                    top + row * MOVE_LINE_HEIGHT + 10f,
                    paint,
                )
                index++
            }
        }
        return index
    }

    private fun drawFooter(context: Context, canvas: Canvas, generatedAt: String, pageNumber: Int) {
        val paint = textPaint(8f, color = Color.GRAY)
        val y = PAGE_HEIGHT - MARGIN / 2
        canvas.drawText(context.getString(R.string.mygames_pdf_footer, generatedAt), MARGIN, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(pageNumber.toString(), PAGE_WIDTH - MARGIN, y, paint)
    }

    /** Risultato dal tag [Result] del PGN, o dal simbolo finale delle mosse; null se assente/"*". */
    internal fun resultOf(pgn: String): String? {
        val tag = Regex("\\[Result\\s+\"([^\"]+)\"\\]").find(pgn)?.groupValues?.get(1)
        val raw = tag ?: Regex("(1-0|0-1|1/2-1/2)\\s*$").find(pgn.trim())?.value
        return when (raw?.trim()) {
            "1-0" -> "1-0"
            "0-1" -> "0-1"
            "1/2-1/2" -> "½-½"
            else -> null
        }
    }

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) end--
        return text.substring(0, end) + "…"
    }
}
