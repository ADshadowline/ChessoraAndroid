package org.chessora.app.ui.analysis

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList
import kotlin.math.exp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.chessora.app.ChessoraApplication
import org.chessora.app.engine.EngineLine
import org.chessora.app.engine.EngineNetworks
import org.chessora.app.engine.StockfishEngine
import org.chessora.app.ui.common.chessoraViewModel

/* ------------------------------------------------------------------ parametri */

enum class AnalysisMode { POSITION, GAME }

/** Parametri scelti nel dialogo "Analisi computer" (ricordati tra un'apertura e l'altra). */
data class AnalysisSettings(
    val mode: AnalysisMode = AnalysisMode.POSITION,
    /** true = si ferma a [depth]; false = [seconds] di riflessione per posizione. */
    val byDepth: Boolean = true,
    val depth: Int = 18,
    val seconds: Int = 2,
    /** Linee migliori (MultiPV), solo per l'analisi della posizione. */
    val lines: Int = 3,
    val threads: Int = defaultThreads(),
    val hashMb: Int = 64,
) {
    companion object {
        val HASH_OPTIONS = listOf(16, 32, 64, 128, 256)
        fun maxThreads() = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)
        fun defaultThreads() = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)

        private const val PREFS = "engine_analysis"
        fun load(context: Context): AnalysisSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val d = AnalysisSettings()
            return AnalysisSettings(
                mode = runCatching { AnalysisMode.valueOf(p.getString("mode", d.mode.name)!!) }.getOrDefault(d.mode),
                byDepth = p.getBoolean("byDepth", d.byDepth),
                depth = p.getInt("depth", d.depth),
                seconds = p.getInt("seconds", d.seconds),
                lines = p.getInt("lines", d.lines),
                threads = p.getInt("threads", d.threads).coerceIn(1, maxThreads()),
                hashMb = p.getInt("hashMb", d.hashMb),
            )
        }

        fun save(context: Context, s: AnalysisSettings) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("mode", s.mode.name).putBoolean("byDepth", s.byDepth).putInt("depth", s.depth)
                .putInt("seconds", s.seconds).putInt("lines", s.lines).putInt("threads", s.threads)
                .putInt("hashMb", s.hashMb).apply()
        }
    }
}

/* ------------------------------------------------------------------ valutazioni */

/** Valutazione dal punto di vista del Bianco: [mate] in mosse (positivo = vince il Bianco)
 * oppure [pawns] in pedoni. */
data class WhiteEval(val pawns: Double?, val mate: Int?) {
    fun label(): String = when {
        mate != null -> if (mate == 0) "#" else "#$mate"
        pawns != null -> "%+.2f".format(pawns).replace('.', ',')
        else -> "—"
    }

    /** Probabilità di vittoria del Bianco 0..100 (stessa formula di Lichess), per il grafico
     * e per misurare quanto una mossa ha peggiorato la posizione. */
    fun whiteWinPct(): Double {
        val cp = when {
            mate != null -> if (mate > 0) 10_000.0 else -10_000.0
            else -> (pawns ?: 0.0) * 100
        }.coerceIn(-1000.0, 1000.0)
        return 50 + 50 * (2 / (1 + exp(-0.00368208 * cp)) - 1)
    }
}

private fun whiteToMove(fen: String) = fen.split(" ").getOrNull(1) != "b"

fun EngineLine.toWhiteEval(fen: String): WhiteEval {
    val sign = if (whiteToMove(fen)) 1 else -1
    return WhiteEval(cp?.let { it * sign / 100.0 }, mate?.let { it * sign })
}

/** Linea del motore (mosse UCI) in notazione algebrica numerata: "13... Cc6 14. Ab5 a6". */
fun uciLineToText(fen: String, uci: List<String>, max: Int = 10): String = runCatching {
    val board = Board().apply { loadFromFen(fen) }
    val list = MoveList(fen)
    for (u in uci.take(max)) {
        val move = board.legalMoves().firstOrNull { it.toString().equals(u, ignoreCase = true) } ?: break
        list.add(move)
        board.doMove(move)
    }
    val san = list.toSanArray().toList()
    val parts = fen.split(" ")
    var white = parts.getOrNull(1) != "b"
    var number = parts.getOrNull(5)?.toIntOrNull() ?: 1
    buildString {
        san.forEachIndexed { i, m ->
            if (i > 0) append(' ')
            if (white) append("$number. ") else if (i == 0) append("$number... ")
            append(m)
            if (!white) number++
            white = !white
        }
    }
}.getOrDefault("")

/** Categoria di ogni mossa della partita, in base a quanto ha fatto scendere la probabilità di
 * vittoria di chi l'ha giocata (soglie di Lichess per imprecisione/errore/errore grave:
 * 10/20/30 punti percentuali). [negative] = mosse da segnalare nell'elenco degli sbagli. */
enum class MoveJudgement(val symbol: String, val label: String, val plural: String, val color: Color, val negative: Boolean) {
    GREAT("!", "Grande", "grandi", Color(0xFF749BBF), false),
    BEST("★", "Migliore", "migliori", Color(0xFF81B64C), false),
    EXCELLENT("👍", "Ottima", "ottime", Color(0xFF96BC4B), false),
    GOOD("✓", "Buona", "buone", Color(0xFF8FA678), false),
    INACCURACY("?!", "Imprecisione", "imprecisioni", Color(0xFFF7C631), true),
    MISTAKE("?", "Errore", "errori", Color(0xFFFFA459), true),
    MISS("✕", "Mossa mancata", "mosse mancate", Color(0xFFFF7769), true),
    BLUNDER("??", "Errore grave", "errori gravi", Color(0xFFFA412D), true),
    ;

    /** "1 imprecisione", "2 imprecisioni", "0 errori". */
    fun count(n: Int) = "$n " + if (n == 1) label.lowercase() else plural
}

data class JudgedMove(val ply: Int, val san: String, val judgement: MoveJudgement, val best: String?, val whiteMoved: Boolean)

/* ------------------------------------------------------------------ stato */

sealed interface AnalysisStatus {
    data object Idle : AnalysisStatus
    data class Downloading(val progress: Float) : AnalysisStatus
    data object Starting : AnalysisStatus
    data object Running : AnalysisStatus
    data object Done : AnalysisStatus
    data class Error(val message: String) : AnalysisStatus
}

class ComputerAnalysisViewModel(private val app: ChessoraApplication) : ViewModel() {
    private val engine = StockfishEngine(app)
    private var job: Job? = null

    private val _settings = MutableStateFlow(AnalysisSettings.load(app))
    val settings: StateFlow<AnalysisSettings> = _settings
    private val _status = MutableStateFlow<AnalysisStatus>(AnalysisStatus.Idle)
    val status: StateFlow<AnalysisStatus> = _status
    /** Modalità in corso (null = pannello chiuso). */
    private val _active = MutableStateFlow<AnalysisMode?>(null)
    val active: StateFlow<AnalysisMode?> = _active

    private val _lines = MutableStateFlow<Pair<String, List<EngineLine>>?>(null)
    val lines: StateFlow<Pair<String, List<EngineLine>>?> = _lines

    /** Partita intera: valutazione di ogni posizione e mossa migliore (UCI) in ciascuna. */
    private val _gameEvals = MutableStateFlow<List<WhiteEval?>>(emptyList())
    val gameEvals: StateFlow<List<WhiteEval?>> = _gameEvals
    private val _gameBest = MutableStateFlow<List<String?>>(emptyList())
    val gameBest: StateFlow<List<String?>> = _gameBest
    /** Valutazione della seconda scelta del motore in ogni posizione (per le mosse "Grande"). */
    private val _gameSecond = MutableStateFlow<List<WhiteEval?>>(emptyList())
    val gameSecond: StateFlow<List<WhiteEval?>> = _gameSecond

    fun start(settings: AnalysisSettings, positions: List<String>, index: Int) {
        AnalysisSettings.save(app, settings)
        _settings.value = settings
        _active.value = settings.mode
        job?.cancel()
        job = viewModelScope.launch {
            run {
                prepare(settings)
                when (settings.mode) {
                    AnalysisMode.POSITION -> analysePosition(positions.getOrElse(index) { positions.first() })
                    AnalysisMode.GAME -> analyseGame(positions)
                }
            }
        }
    }

    /** Analisi della posizione: riparte sulla nuova posizione quando si scorrono le mosse. */
    fun onPositionChanged(fen: String) {
        if (_active.value != AnalysisMode.POSITION || _status.value is AnalysisStatus.Downloading ||
            _status.value is AnalysisStatus.Error || !engine.isRunning) return
        if (_lines.value?.first == fen) return
        job?.cancel()
        job = viewModelScope.launch { run { analysePosition(fen) } }
    }

    fun stop() {
        job?.cancel()
        if (_status.value !is AnalysisStatus.Error) _status.value = AnalysisStatus.Done
    }

    fun close() {
        job?.cancel()
        _active.value = null
        _status.value = AnalysisStatus.Idle
        _lines.value = null
    }

    private suspend fun run(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _status.value = AnalysisStatus.Error(e.message ?: "Analisi non riuscita.")
            engine.close()
        }
    }

    private suspend fun prepare(s: AnalysisSettings) {
        if (!EngineNetworks.isReady(app)) {
            _status.value = AnalysisStatus.Downloading(0f)
            EngineNetworks.download(app) { _status.value = AnalysisStatus.Downloading(it) }
        }
        _status.value = AnalysisStatus.Starting
        engine.start(s.threads, s.hashMb)
    }

    private suspend fun analysePosition(fen: String) {
        val s = _settings.value
        _status.value = AnalysisStatus.Running
        _lines.value = fen to emptyList()
        val result = engine.analyse(fen, if (s.byDepth) s.depth else null, if (s.byDepth) null else s.seconds * 1000, s.lines) {
            _lines.value = fen to it
        }
        if (result.lines.isNotEmpty()) _lines.value = fen to result.lines
        _status.value = AnalysisStatus.Done
    }

    private suspend fun analyseGame(positions: List<String>) {
        val s = _settings.value
        _status.value = AnalysisStatus.Running
        val evals = MutableList<WhiteEval?>(positions.size) { null }
        val best = MutableList<String?>(positions.size) { null }
        val second = MutableList<WhiteEval?>(positions.size) { null }
        _gameEvals.value = evals.toList()
        _gameBest.value = best.toList()
        _gameSecond.value = second.toList()
        positions.forEachIndexed { i, fen ->
            // 2 linee: la seconda serve a riconoscere le mosse "Grande" (unica mossa buona).
            val result = engine.analyse(fen, if (s.byDepth) s.depth else null, if (s.byDepth) null else s.seconds * 1000, 2)
            evals[i] = result.lines.firstOrNull()?.toWhiteEval(fen) ?: terminalEval(fen)
            second[i] = result.lines.getOrNull(1)?.toWhiteEval(fen)
            best[i] = result.bestMove
            _gameEvals.value = evals.toList()
            _gameBest.value = best.toList()
            _gameSecond.value = second.toList()
        }
        _status.value = AnalysisStatus.Done
    }

    /** Posizione finale senza mosse: matto (perde chi muove) o stallo. */
    private fun terminalEval(fen: String): WhiteEval = runCatching {
        val board = Board().apply { loadFromFen(fen) }
        when {
            !board.isMated -> WhiteEval(0.0, null)
            whiteToMove(fen) -> WhiteEval(-100.0, null) // il Bianco ha subito matto
            else -> WhiteEval(100.0, null)
        }
    }.getOrDefault(WhiteEval(0.0, null))

    override fun onCleared() {
        job?.cancel()
        engine.close()
    }
}

/** Mossa giocata tra due posizioni, in UCI ("e2e4"): la mossa legale che porta da [from] a
 * [to] (confronto della disposizione dei pezzi). Null se non si trova. */
internal fun playedUci(from: String, to: String): String? = runCatching {
    val board = Board().apply { loadFromFen(from) }
    val target = to.substringBefore(' ')
    board.legalMoves().firstOrNull { m ->
        board.doMove(m)
        val match = board.fen.substringBefore(' ') == target
        board.undoMove()
        match
    }?.toString()
}.getOrNull()

/** Categoria di OGNI mossa della partita, dalle valutazioni posizione per posizione. */
fun judgeMoves(
    positions: List<String>,
    sanMoves: List<String>,
    evals: List<WhiteEval?>,
    best: List<String?>,
    second: List<WhiteEval?> = emptyList(),
): List<JudgedMove> {
    val out = mutableListOf<JudgedMove>()
    for (ply in 1 until positions.size) {
        val before = evals.getOrNull(ply - 1) ?: continue
        val after = evals.getOrNull(ply) ?: continue
        val played = sanMoves.getOrNull(ply - 1) ?: continue
        val whiteMoved = whiteToMove(positions[ply - 1])
        fun moverPct(e: WhiteEval) = if (whiteMoved) e.whiteWinPct() else 100 - e.whiteWinPct()
        val moverBefore = moverPct(before)
        val moverAfter = moverPct(after)
        val loss = moverBefore - moverAfter
        val bestUci = best.getOrNull(ply - 1)
        val isBest = bestUci != null && bestUci.equals(playedUci(positions[ply - 1], positions[ply]), ignoreCase = true)
        // "Grande": la migliore quando la seconda scelta faceva perdere molto (unica mossa buona).
        val secondGap = second.getOrNull(ply - 1)?.let { moverBefore - moverPct(it) } ?: 0.0
        val judgement = when {
            loss >= 30 -> MoveJudgement.BLUNDER
            moverBefore >= 80 && moverAfter < 65 && loss >= 10 -> MoveJudgement.MISS // vittoria lasciata sfuggire
            loss >= 20 -> MoveJudgement.MISTAKE
            loss >= 10 -> MoveJudgement.INACCURACY
            isBest && secondGap >= 15 -> MoveJudgement.GREAT
            isBest -> MoveJudgement.BEST
            loss < 2 -> MoveJudgement.EXCELLENT
            else -> MoveJudgement.GOOD
        }
        val bestText = if (judgement.negative) bestUci?.let { uciLineToText(positions[ply - 1], listOf(it), 1) }?.substringAfterLast(' ') else null
        out += JudgedMove(ply, played, judgement, bestText?.takeIf { it.isNotBlank() && it != played }, whiteMoved)
    }
    return out
}

/* ------------------------------------------------------------------ UI */

/**
 * Pulsante "Analisi computer" + parametri + risultati (Stockfish 17.1), da mettere sotto la
 * scacchiera nelle schermate di una partita (Le mie partite, Lichess, Chess.com).
 * [positions] = FEN di ogni posizione (0 = iniziale), [sanMoves] = mossa che porta alla
 * posizione i+1, [moveIndex] = posizione mostrata, [onJumpTo] = vai a quella posizione.
 */
@Composable
fun ComputerAnalysisSection(
    positions: List<String>,
    sanMoves: List<String>,
    moveIndex: Int,
    onJumpTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm = chessoraViewModel { app -> ComputerAnalysisViewModel(app) }
    val settings by vm.settings.collectAsState()
    val status by vm.status.collectAsState()
    val active by vm.active.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(moveIndex, active) {
        positions.getOrNull(moveIndex)?.let { vm.onPositionChanged(it) }
    }

    if (showDialog) {
        AnalysisSettingsDialog(
            initial = settings,
            onDismiss = { showDialog = false },
            onStart = {
                showDialog = false
                vm.start(it, positions, moveIndex)
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (active == null) {
            OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Analisi computer")
            }
            return@Column
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        if (active == AnalysisMode.POSITION) "Analisi della posizione" else "Analisi della partita",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showDialog = true }) { Text("Parametri") }
                    if (status is AnalysisStatus.Running || status is AnalysisStatus.Starting || status is AnalysisStatus.Downloading) {
                        IconButton(onClick = { vm.stop() }) { Icon(Icons.Default.Stop, contentDescription = "Ferma") }
                    }
                    IconButton(onClick = { vm.close() }) { Icon(Icons.Default.Close, contentDescription = "Chiudi analisi") }
                }
                when (val s = status) {
                    is AnalysisStatus.Downloading -> {
                        Text(
                            "Primo uso: scarico la rete neurale di Stockfish (circa ${EngineNetworks.TOTAL_SIZE_MB} MB, una volta sola)… ${(s.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    AnalysisStatus.Starting -> Text("Avvio del motore…", style = MaterialTheme.typography.bodySmall)
                    is AnalysisStatus.Error -> Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    else -> {}
                }
                if (active == AnalysisMode.POSITION) PositionLines(vm) else GameSummary(vm, positions, sanMoves, moveIndex, onJumpTo)
            }
        }
    }
}

@Composable
private fun PositionLines(vm: ComputerAnalysisViewModel) {
    val data by vm.lines.collectAsState()
    val (fen, lines) = data ?: return
    if (lines.isEmpty()) return
    val first = lines.first()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
        Text(first.toWhiteEval(fen).label(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("  profondità ${first.depth}", style = MaterialTheme.typography.bodySmall)
    }
    lines.forEach { line ->
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Row {
            Text(
                line.toWhiteEval(fen).label(),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(64.dp),
            )
            Text(uciLineToText(fen, line.pv), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GameSummary(
    vm: ComputerAnalysisViewModel,
    positions: List<String>,
    sanMoves: List<String>,
    moveIndex: Int,
    onJumpTo: (Int) -> Unit,
) {
    val evals by vm.gameEvals.collectAsState()
    val best by vm.gameBest.collectAsState()
    val second by vm.gameSecond.collectAsState()
    val status by vm.status.collectAsState()
    val done = evals.count { it != null }
    if (evals.isEmpty()) return
    if (status is AnalysisStatus.Running) {
        Text("Posizione $done di ${evals.size}…", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 6.dp))
        LinearProgressIndicator(progress = { done.toFloat() / evals.size }, modifier = Modifier.fillMaxWidth())
    }
    EvalGraph(evals, moveIndex, onJumpTo, modifier = Modifier.padding(top = 8.dp))
    evals.getOrNull(moveIndex)?.let {
        Text("Valutazione dopo questa mossa: ${it.label()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
    if (status !is AnalysisStatus.Running && done == evals.size) {
        val all = remember(evals, best, second) { judgeMoves(positions, sanMoves, evals, best, second) }
        val judged = all.filter { it.judgement.negative }
        val counts = judged.groupingBy { it.judgement }.eachCount()
        val negatives = MoveJudgement.entries.filter { it.negative }
        Text(
            if (judged.isEmpty()) "Nessuna imprecisione rilevata."
            else negatives.joinToString(" · ") { it.count(counts[it] ?: 0) },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        // Niente scorrimento proprio: la schermata della partita scorre già tutta.
        Column {
            judged.forEach { j ->
                val number = (j.ply + 1) / 2
                val prefix = if (j.ply % 2 == 1) "$number." else "$number..."
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { onJumpTo(j.ply) }.padding(vertical = 4.dp),
                ) {
                    Text("$prefix ${j.san}${j.judgement.symbol}", color = j.judgement.color, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
                    Text(
                        j.judgement.label + (j.best?.let { " · meglio $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        JudgementLegend(all, modifier = Modifier.padding(top = 12.dp))
    }
}

/** Cerchio colorato con il simbolo della categoria (legenda e scacchiera). */
@Composable
private fun JudgementBadge(j: MoveJudgement, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size).clip(CircleShape).background(j.color),
    ) {
        val base = size.value * 0.55f
        Text(j.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (if (j.symbol.length > 1) base * 0.8f else base).sp)
    }
}

/**
 * Da mettere SOPRA la scacchiera (stesse dimensioni): dopo l'analisi della partita intera,
 * a ogni cambio di mossa mostra per un secondo, nell'angolo in alto a destra della casella
 * d'arrivo, il simbolo della qualità della mossa appena giocata.
 */
@Composable
fun MoveQualityBoardOverlay(
    positions: List<String>,
    sanMoves: List<String>,
    moveIndex: Int,
    flipped: Boolean,
    modifier: Modifier = Modifier,
) {
    val vm = chessoraViewModel { app -> ComputerAnalysisViewModel(app) }
    val active by vm.active.collectAsState()
    val evals by vm.gameEvals.collectAsState()
    val best by vm.gameBest.collectAsState()
    val second by vm.gameSecond.collectAsState()
    if (active != AnalysisMode.GAME || moveIndex < 1) return
    val judged = remember(evals, best, second, positions) { judgeMoves(positions, sanMoves, evals, best, second) }
    val judgement = judged.firstOrNull { it.ply == moveIndex }?.judgement ?: return
    val uci = remember(moveIndex, positions) { playedUci(positions[moveIndex - 1], positions[moveIndex]) } ?: return
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(moveIndex) {
        visible = true
        kotlinx.coroutines.delay(1000)
        visible = false
    }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        val square = maxWidth / 8
        val badge = square * 0.5f
        val file = uci[2] - 'a'
        val rankFromTop = 8 - (uci[3] - '0')
        val col = if (flipped) 7 - file else file
        val row = if (flipped) 7 - rankFromTop else rankFromTop
        // Angolo in alto a destra della casella, un po' sporgente, mai fuori dalla scacchiera.
        val x = (square * (col + 1) - badge * 0.8f).coerceIn(0.dp, maxWidth - badge)
        val y = (square * row - badge * 0.2f).coerceIn(0.dp, maxWidth - badge)
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.6f),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.offset(x = x, y = y),
        ) {
            JudgementBadge(judgement, badge)
        }
    }
}

/** Legenda finale: per ogni categoria quante mosse ha giocato il Bianco (a sinistra) e il Nero
 * (a destra), con il simbolo colorato in mezzo. */
@Composable
private fun JudgementLegend(moves: List<JudgedMove>, modifier: Modifier = Modifier) {
    if (moves.isEmpty()) return
    val white = moves.filter { it.whiteMoved }.groupingBy { it.judgement }.eachCount()
    val black = moves.filter { !it.whiteMoved }.groupingBy { it.judgement }.eachCount()
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("", modifier = Modifier.weight(1f))
            Text("Bianco", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
            Box(modifier = Modifier.width(40.dp))
            Text("Nero", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
        }
        MoveJudgement.entries.forEach { j ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                Text(j.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${white[j] ?: 0}", color = j.color, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(40.dp),
                ) {
                    JudgementBadge(j, 24.dp)
                }
                Text("${black[j] ?: 0}", color = j.color, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
            }
        }
    }
}

/** Grafico della valutazione (probabilità di vittoria del Bianco): sopra il Bianco, sotto il
 * Nero; tocca per andare a quella mossa. */
@Composable
private fun EvalGraph(evals: List<WhiteEval?>, moveIndex: Int, onJumpTo: (Int) -> Unit, modifier: Modifier = Modifier) {
    val n = evals.size
    if (n < 2) return
    Canvas(
        modifier = modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(6.dp))
            .pointerInput(n) {
                detectTapGestures { offset -> onJumpTo(((offset.x / size.width) * (n - 1)).toInt().coerceIn(0, n - 1)) }
            },
    ) {
        drawRect(Color(0xFF2B2B2B))
        val step = size.width / (n - 1)
        val path = Path().apply {
            moveTo(0f, size.height)
            evals.forEachIndexed { i, e ->
                val y = size.height * (1 - ((e?.whiteWinPct() ?: 50.0) / 100).toFloat())
                lineTo(i * step, y)
            }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, Color(0xFFEDEDED))
        drawLine(Color(0x55888888), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
        drawLine(Color(0xFFE6B800), Offset(moveIndex * step, 0f), Offset(moveIndex * step, size.height), strokeWidth = 3f)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnalysisSettingsDialog(initial: AnalysisSettings, onDismiss: () -> Unit, onStart: (AnalysisSettings) -> Unit) {
    var s by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Analisi computer") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingLabel("Cosa analizzare")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(s.mode == AnalysisMode.POSITION, { s = s.copy(mode = AnalysisMode.POSITION) }, { Text("Posizione") })
                    FilterChip(s.mode == AnalysisMode.GAME, { s = s.copy(mode = AnalysisMode.GAME) }, { Text("Partita intera") })
                }
                Text(
                    if (s.mode == AnalysisMode.POSITION) "Valutazione e linee migliori della posizione sulla scacchiera, aggiornate mentre scorri le mosse."
                    else "Valuta ogni mossa: grafico della partita e imprecisioni (?!), errori (?) e sviste (??).",
                    style = MaterialTheme.typography.bodySmall,
                )
                SettingLabel("Limite di calcolo per posizione")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(s.byDepth, { s = s.copy(byDepth = true) }, { Text("Profondità") })
                    FilterChip(!s.byDepth, { s = s.copy(byDepth = false) }, { Text("Tempo") })
                }
                if (s.byDepth) {
                    SliderSetting("Profondità: ${s.depth} semimosse", s.depth, 8..30) { s = s.copy(depth = it) }
                } else {
                    SliderSetting("Tempo: ${s.seconds} s per posizione", s.seconds, 1..15) { s = s.copy(seconds = it) }
                }
                if (s.mode == AnalysisMode.POSITION) {
                    SliderSetting("Linee migliori: ${s.lines}", s.lines, 1..5) { s = s.copy(lines = it) }
                }
                SliderSetting("Processori (thread): ${s.threads}", s.threads, 1..AnalysisSettings.maxThreads()) { s = s.copy(threads = it) }
                SettingLabel("Memoria (hash)")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AnalysisSettings.HASH_OPTIONS.forEach { mb ->
                        FilterChip(s.hashMb == mb, { s = s.copy(hashMb = mb) }, { Text("$mb MB") })
                    }
                }
                Text(
                    "Più profondità o tempo = analisi più precisa ma più lenta. Motore Stockfish 17.1 (software libero, licenza GPL v3, sorgenti su github.com/official-stockfish/Stockfish).",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onStart(s) }) { Text("Avvia") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

@Composable
private fun SettingLabel(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

@Composable
private fun SliderSetting(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
        )
    }
}
