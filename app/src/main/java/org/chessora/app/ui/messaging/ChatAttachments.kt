package org.chessora.app.ui.messaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.delay
import org.chessora.app.data.local.AuthSession
import org.chessora.app.data.remote.NetworkModule

/** URL completo di un allegato: il server lo restituisce solo autenticati e con l'idPlayer del
 * chiamante, che deve partecipare alla conversazione (vedi MessagingController.GetAttachment). */
fun chatAttachmentUrl(relativeUrl: String, idPlayer: Int): String? =
    NetworkModule.resolveAssetUrl(relativeUrl)?.let { "$it?idPlayer=$idPlayer" }

private fun authHeaders(): Map<String, String> =
    AuthSession.accessToken?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()

/** Legge la foto scelta (galleria o fotocamera), la raddrizza secondo l'EXIF, la riduce a
 * [maxSide] px sul lato lungo e la ricomprime in JPEG: una foto da 12 MP passa da 4-6 MB a
 * circa 300 KB, senza che in chat si noti differenza. Null se il file non è un'immagine. */
fun compressChatImage(context: Context, uri: Uri, maxSide: Int = 1600, quality: Int = 82): ByteArray? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        null
    } else {
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        decoded?.let { bitmap ->
            val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            val scale = minOf(1f, maxSide.toFloat() / maxOf(bitmap.width, bitmap.height))
            val matrix = Matrix().apply {
                if (scale < 1f) postScale(scale, scale)
                if (degrees != 0f) postRotate(degrees)
            }
            val output = if (scale < 1f || degrees != 0f) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            ByteArrayOutputStream().use { out ->
                output.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray()
            }
        }
    }
} catch (e: Exception) {
    null
}

/** Registratore dei messaggi vocali: AAC in contenitore MPEG-4 (.m4a), mono, 64 kbps - un
 * minuto pesa circa 500 KB. Durata massima [MAX_DURATION_MS]: oltre si ferma da solo. */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(onMaxDurationReached: () -> Unit): Boolean {
        val output = File(context.cacheDir, "vocale_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioChannels(1)
            mr.setAudioSamplingRate(44_100)
            mr.setAudioEncodingBitRate(64_000)
            mr.setMaxDuration(MAX_DURATION_MS)
            mr.setOutputFile(output.absolutePath)
            mr.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onMaxDurationReached()
            }
            mr.prepare()
            mr.start()
            recorder = mr
            file = output
            startedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            mr.release()
            output.delete()
            false
        }
    }

    /** Ferma e ritorna file + durata, null se la registrazione è troppo breve (< 1 s) o fallita. */
    fun stop(): Pair<File, Int>? {
        val mr = recorder ?: return null
        val output = file
        val duration = (System.currentTimeMillis() - startedAt).toInt().coerceAtMost(MAX_DURATION_MS)
        recorder = null
        file = null
        val ok = try {
            mr.stop()
            true
        } catch (e: Exception) {
            false // stop() lancia se non è stato registrato quasi nulla
        } finally {
            mr.release()
        }
        if (!ok || output == null || duration < 1_000) {
            output?.delete()
            return null
        }
        return output to duration
    }

    fun cancel() {
        stop()?.first?.delete()
    }

    fun elapsedMs(): Int = if (recorder != null) (System.currentTimeMillis() - startedAt).toInt() else 0

    companion object {
        const val MAX_DURATION_MS = 120_000
    }
}

fun formatDuration(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Foto in una bolla: si carica autenticata (Coil, header Authorization); un tocco la apre
 * a tutto schermo. */
@Composable
fun ChatImage(relativeUrl: String, idPlayer: Int) {
    val context = LocalContext.current
    val url = remember(relativeUrl, idPlayer) { chatAttachmentUrl(relativeUrl, idPlayer) }
    var fullScreen by remember { mutableStateOf(false) }
    val request = remember(url) {
        ImageRequest.Builder(context).data(url).apply { authHeaders().forEach { (k, v) -> addHeader(k, v) } }.crossfade(true).build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 120.dp, max = 300.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { fullScreen = true },
        loading = {
            Box(Modifier.width(220.dp).padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
        },
        error = {
            Box(Modifier.width(220.dp).padding(40.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.BrokenImage, contentDescription = null) }
        },
    )
    if (fullScreen) {
        Dialog(onDismissRequest = { fullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black).clickable { fullScreen = false },
                contentAlignment = Alignment.Center,
            ) {
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** Lettore di un messaggio vocale: play/pausa, barra di avanzamento e durata. Lo stream
 * arriva dall'endpoint autenticato (MediaPlayer con header Authorization). */
@Composable
fun ChatAudioPlayer(relativeUrl: String, idPlayer: Int, declaredDurationMs: Int?) {
    val context = LocalContext.current
    val url = remember(relativeUrl, idPlayer) { chatAttachmentUrl(relativeUrl, idPlayer) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(declaredDurationMs ?: 0) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(url) {
        onDispose {
            player?.release()
            player = null
        }
    }
    LaunchedEffect(playing) {
        while (playing) {
            player?.let { p ->
                positionMs = p.currentPosition
                if (p.duration > 0) {
                    durationMs = p.duration
                    progress = p.currentPosition.toFloat() / p.duration
                }
            }
            delay(200)
        }
    }

    fun toggle() {
        val current = player
        when {
            current != null && playing -> { current.pause(); playing = false }
            current != null -> { current.start(); playing = true }
            url != null -> {
                preparing = true
                failed = false
                val mp = MediaPlayer()
                try {
                    mp.setDataSource(context, Uri.parse(url), authHeaders())
                    mp.setOnPreparedListener { it.start(); preparing = false; playing = true; durationMs = it.duration }
                    mp.setOnCompletionListener { playing = false; progress = 0f; positionMs = 0; it.seekTo(0) }
                    mp.setOnErrorListener { _, _, _ -> preparing = false; playing = false; failed = true; true }
                    mp.prepareAsync()
                    player = mp
                } catch (e: Exception) {
                    mp.release()
                    preparing = false
                    failed = true
                }
            }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(230.dp)) {
        if (preparing) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
        } else {
            IconButton(onClick = { toggle() }) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Text(
            if (failed) "!" else formatDuration(if (playing || positionMs > 0) positionMs else durationMs),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
