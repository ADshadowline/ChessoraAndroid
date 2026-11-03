package org.chessora.app.engine

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Reti neurali NNUE di Stockfish 17.1 (la parte pesante del motore, ~75 MB in tutto): non sono
 * nell'APK, si scaricano al primo uso dell'analisi da chessora.org e restano in filesDir/engine.
 * Sono file di DATI che il motore legge (setoption EvalFile), quindi Android permette di
 * usarli anche se scaricati - a differenza di un eseguibile (vedi StockfishEngine).
 *
 * Stockfish dà a ogni rete il nome con le prime 12 cifre esadecimali del suo SHA-256: il
 * download è verificato su quello, una rete corrotta o diversa viene scartata.
 */
object EngineNetworks {
    const val BIG = "nn-1c0000000000.nnue"
    const val SMALL = "nn-37f18f62d772.nnue"
    private val ALL = listOf(BIG, SMALL)

    /** Su chessora.org con estensione .bin: IIS non serve estensioni sconosciute come .nnue. */
    private const val BASE_URL = "https://chessora.org/downloads/engine/"

    /** Dimensione totale circa, per la barra di avanzamento prima che risponda il server. */
    const val TOTAL_SIZE_MB = 75

    private fun dir(context: Context) = File(context.filesDir, "engine").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    fun isReady(context: Context): Boolean = ALL.all { file(context, it).let { f -> f.isFile && f.length() > 0 } }

    /** Scarica le reti mancanti; [onProgress] da 0 a 1 sul totale dei byte. */
    suspend fun download(context: Context, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val missing = ALL.filter { !file(context, it).let { f -> f.isFile && f.length() > 0 } }
        val sizes = missing.associateWith { contentLength(it) }
        val total = sizes.values.sum().coerceAtLeast(1L)
        var done = 0L
        for (name in missing) {
            val target = file(context, name)
            val part = File(target.path + ".part")
            val digest = MessageDigest.getInstance("SHA-256")
            val connection = (URL(BASE_URL + name.removeSuffix(".nnue") + ".bin").openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            try {
                if (connection.responseCode != 200) error("Download della rete non riuscito (HTTP ${connection.responseCode}).")
                connection.inputStream.use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            done += read
                            onProgress((done.toFloat() / total).coerceAtMost(1f))
                        }
                    }
                }
            } catch (e: Exception) {
                part.delete()
                throw e
            } finally {
                connection.disconnect()
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val expected = name.removePrefix("nn-").removeSuffix(".nnue")
            if (!hash.startsWith(expected)) {
                part.delete()
                error("La rete scaricata non è integra: riprova.")
            }
            if (!part.renameTo(target)) error("Impossibile salvare la rete sul telefono.")
        }
        onProgress(1f)
    }

    private fun contentLength(name: String): Long = runCatching {
        val c = URL(BASE_URL + name.removeSuffix(".nnue") + ".bin").openConnection() as HttpURLConnection
        c.requestMethod = "HEAD"
        c.connectTimeout = 10_000
        try { c.contentLengthLong.coerceAtLeast(0) } finally { c.disconnect() }
    }.getOrDefault(0L).takeIf { it > 0 } ?: if (name == BIG) 74_874_478L else 3_519_630L
}
