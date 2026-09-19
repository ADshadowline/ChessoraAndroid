package org.chessora.app.ui.calendar

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule

private sealed interface BandoState {
    data object Loading : BandoState
    data class Pages(val bitmaps: List<Bitmap>) : BandoState
    data object Unsupported : BandoState
    data class Error(val message: String) : BandoState
}

/**
 * Bando di un torneo aperto dal calendario, renderizzato DENTRO l'app (mai un
 * redirect al sito) - nessuna azione di iscrizione qui, a differenza di
 * TournamentDetailScreen: questa schermata è di sola consultazione.
 *
 * PDF (il caso di gran lunga più comune, vedi upload lato wizard) è renderizzato
 * pagina per pagina con `android.graphics.pdf.PdfRenderer`, già disponibile in
 * piattaforma - nessuna libreria esterna. DOC/DOCX non hanno un rendering in-app
 * ragionevole senza una libreria pesante: restano un'apertura esterna esplicita,
 * con un avviso invece di un tentativo silenzioso.
 */
@Composable
fun BandoViewerScreen(url: String) {
    val context = LocalContext.current
    var state by remember(url) { mutableStateOf<BandoState>(BandoState.Loading) }

    LaunchedEffect(url) {
        state = BandoState.Loading
        val isPdf = url.substringBefore('?').lowercase().endsWith(".pdf")
        if (!isPdf) {
            state = BandoState.Unsupported
            return@LaunchedEffect
        }
        state = try {
            val bitmaps = withContext(Dispatchers.IO) {
                val cacheFile = File(context.cacheDir, "bando_${url.hashCode()}.pdf")
                NetworkModule.downloadToFile(url, cacheFile)
                renderPdfPages(cacheFile)
            }
            BandoState.Pages(bitmaps)
        } catch (e: Exception) {
            BandoState.Error(context.getString(R.string.bando_viewer_error))
        }
    }

    when (val current = state) {
        is BandoState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is BandoState.Pages -> LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            items(current.bitmaps) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
        is BandoState.Unsupported -> UnsupportedBandoContent(url, context)
        is BandoState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(current.message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun UnsupportedBandoContent(url: String, context: Context) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.bando_viewer_unsupported), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.bando_viewer_open_external))
        }
    }
}

/** Risoluzione 2x la dimensione nativa della pagina PDF: leggibile su schermo senza
 * appesantire troppo la memoria (i bandi sono tipicamente 1-3 pagine). */
private fun renderPdfPages(file: File): List<Bitmap> =
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { renderer ->
            (0 until renderer.pageCount).map { index ->
                renderer.openPage(index).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }
