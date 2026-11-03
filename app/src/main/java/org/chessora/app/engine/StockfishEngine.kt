package org.chessora.app.engine

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Una linea del motore (MultiPV): punteggio dal punto di vista di chi muove nella posizione
 * analizzata - [mate] in mosse (negativo = subisce matto), altrimenti [cp] in centipedoni. */
data class EngineLine(val multiPv: Int, val depth: Int, val cp: Int?, val mate: Int?, val pv: List<String>)

data class EngineResult(val lines: List<EngineLine>, val bestMove: String?)

/**
 * Stockfish 17.1 come processo separato, pilotato con il protocollo UCI su stdin/stdout.
 * L'eseguibile è nell'APK (jniLibs/<abi>/libstockfish.so, estratto in nativeLibraryDir: da
 * Android 10 è l'unica cartella da cui un'app può eseguire un programma); le reti NNUE sono
 * scaricate a parte (EngineNetworks). Stockfish è software libero (GPL v3): il programma è
 * distinto dall'app, che lo usa solo come processo esterno.
 */
class StockfishEngine(private val context: Context) {
    private var process: Process? = null
    private var writer: BufferedWriter? = null
    private var output = Channel<String>(Channel.UNLIMITED)

    val isRunning: Boolean get() = process?.isAlive == true

    private fun executable() = File(context.applicationInfo.nativeLibraryDir, "libstockfish.so")

    suspend fun start(threads: Int, hashMb: Int) = withContext(Dispatchers.IO) {
        if (!isRunning) {
            close()
            val exe = executable()
            if (!exe.exists()) error("Motore non disponibile su questo dispositivo.")
            val p = ProcessBuilder(exe.absolutePath).redirectErrorStream(true).start()
            val channel = Channel<String>(Channel.UNLIMITED)
            output = channel
            process = p
            writer = p.outputStream.bufferedWriter()
            thread(name = "stockfish-out", isDaemon = true) {
                try {
                    p.inputStream.bufferedReader().forEachLine { channel.trySend(it) }
                } catch (_: Exception) {
                } finally {
                    channel.close()
                }
            }
            send("uci")
            awaitLine(10_000) { it == "uciok" }
            send("setoption name EvalFile value ${EngineNetworks.file(context, EngineNetworks.BIG).absolutePath}")
            send("setoption name EvalFileSmall value ${EngineNetworks.file(context, EngineNetworks.SMALL).absolutePath}")
        }
        send("setoption name Threads value $threads")
        send("setoption name Hash value $hashMb")
        send("isready")
        awaitLine(20_000) { it == "readyok" }
    }

    /** Analizza [fen] fino a [depth] o per [movetimeMs]; [onUpdate] riceve le linee a ogni
     * aggiornamento. Annullando la coroutine il motore viene fermato ("stop"). */
    suspend fun analyse(
        fen: String,
        depth: Int?,
        movetimeMs: Int?,
        multiPv: Int,
        onUpdate: (List<EngineLine>) -> Unit = {},
    ): EngineResult = withContext(Dispatchers.IO) {
        drain()
        send("setoption name MultiPV value $multiPv")
        send("position fen $fen")
        send(if (movetimeMs != null) "go movetime $movetimeMs" else "go depth ${depth ?: 18}")
        val lines = sortedMapOf<Int, EngineLine>()
        var finished = false
        try {
            while (true) {
                val line = output.receiveCatching().getOrNull()
                    ?: error("Il motore si è interrotto. Se è la prima analisi, prova a riscaricare la rete.")
                if (line.startsWith("bestmove")) {
                    finished = true
                    val best = line.split(" ").getOrNull(1)?.takeIf { it != "(none)" }
                    return@withContext EngineResult(lines.values.toList(), best)
                }
                parseInfo(line)?.let {
                    lines[it.multiPv] = it
                    onUpdate(lines.values.toList())
                }
            }
            @Suppress("UNREACHABLE_CODE")
            EngineResult(emptyList(), null)
        } finally {
            if (!finished && isRunning) withContext(NonCancellable) {
                send("stop")
                withTimeoutOrNull(3_000) { while (true) { val l = output.receiveCatching().getOrNull() ?: break; if (l.startsWith("bestmove")) break } }
            }
        }
    }

    fun close() {
        runCatching { send("quit") }
        runCatching { writer?.close() }
        process?.destroy()
        process = null
        writer = null
    }

    private fun send(command: String) {
        val w = writer ?: return
        w.write(command)
        w.newLine()
        w.flush()
    }

    private fun drain() {
        while (output.tryReceive().isSuccess) { /* righe di ricerche precedenti */ }
    }

    private suspend fun awaitLine(timeoutMs: Long, predicate: (String) -> Boolean) = withTimeout(timeoutMs) {
        while (true) {
            val line = output.receiveCatching().getOrNull() ?: error("Il motore non si è avviato.")
            if (predicate(line)) break
        }
    }

    companion object {
        /** "info depth 20 ... multipv 1 score cp -41 ... pv b8c6 f1b5 ..." -> EngineLine; null
         * per le righe senza linea completa (currmove, bound intermedi, info string). */
        fun parseInfo(line: String): EngineLine? {
            if (!line.startsWith("info ") || " pv " !in line || " score " !in line) return null
            if (" lowerbound" in line || " upperbound" in line) return null
            val t = line.split(" ")
            fun intAfter(key: String) = t.indexOf(key).takeIf { it >= 0 }?.let { t.getOrNull(it + 1)?.toIntOrNull() }
            val scoreIdx = t.indexOf("score")
            val kind = t.getOrNull(scoreIdx + 1)
            val value = t.getOrNull(scoreIdx + 2)?.toIntOrNull() ?: return null
            val pvIdx = t.indexOf("pv")
            return EngineLine(
                multiPv = intAfter("multipv") ?: 1,
                depth = intAfter("depth") ?: 0,
                cp = if (kind == "cp") value else null,
                mate = if (kind == "mate") value else null,
                pv = t.drop(pvIdx + 1).filter { it.isNotBlank() },
            )
        }
    }
}
