package org.chessora.app.ui.video

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.chessora.app.R
import org.chessora.app.data.remote.dto.VideoFeedItem
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Elenco video del circolo (YouTube, gestiti dal wizard) - sola consultazione, il tocco
 * apre il video nell'app YouTube/browser (stesso meccanismo di bando/sito web nel
 * dettaglio torneo), nessuna riproduzione incorporata. Mostra solo i canali selezionati
 * in Impostazioni > Video (vedi VideoChannelSettingsScreen) più gli eventuali video locali
 * del circolo - ricerca testuale e "cartelle" per canale sotto il campo di ricerca. */
@Composable
fun VideoScreen(club: String?) {
    val viewModel = chessoraViewModel { app -> VideoViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { videos ->
        // "Cartelle" = i canali (YouTubeChannel.Description) effettivamente presenti tra i
        // video caricati, non l'intero catalogo - un canale senza video da mostrare non
        // avrebbe senso come filtro. I video locali del circolo (channelDescription null)
        // restano raggiungibili solo dalla cartella "Tutti".
        val folders = remember(videos) { videos.mapNotNull { it.channelDescription }.distinct().sorted() }
        val filtered = remember(videos, query, selectedFolder) {
            videos
                .filter { selectedFolder == null || it.channelDescription == selectedFolder }
                .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp),
                placeholder = { Text(stringResource(R.string.video_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            if (folders.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    item {
                        FilterChip(selected = selectedFolder == null, onClick = { selectedFolder = null }, label = { Text(stringResource(R.string.video_folder_all)) })
                    }
                    items(folders, key = { it }) { folder ->
                        FilterChip(selected = selectedFolder == folder, onClick = { selectedFolder = if (selectedFolder == folder) null else folder }, label = { Text(folder) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (videos.isEmpty()) stringResource(R.string.video_empty) else stringResource(R.string.home_no_results))
                }
            } else {
                Text(
                    stringResource(R.string.video_count, filtered.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(filtered, key = { it.id }) { video ->
                        VideoCard(video, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.link))) })
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoFeedItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            val thumbnailUrl = youTubeThumbnailUrl(video.link)
            if (thumbnailUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(thumbnailUrl),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(
                    video.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (video.channelDescription != null) {
                Text(
                    video.channelDescription,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                formatVideoDate(video),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private val videoDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)

/** publishedAt non è mai valorizzato sui dati reali (vedi VideoFeedService lato server) -
 * si mostra sempre createdAt, ma il nome del campo lato utente resta "data di
 * pubblicazione" perché è così che viene percepito (quando il video è stato importato
 * nell'app coincide di fatto con quando è stato pubblicato/reso disponibile). */
private fun formatVideoDate(video: VideoFeedItem): String {
    val date = video.publishedAt ?: video.createdAt
    return runCatching { LocalDateTime.parse(date).format(videoDateFormatter) }.getOrDefault(date)
}

/** Stessi pattern usati dal sito (Chessora.Web Index.cshtml, extractYouTubeId) per
 * ricavare la miniatura ufficiale di YouTube dal link - null (nessuna miniatura, solo
 * testo) per link non-YouTube. */
private val YOUTUBE_ID_PATTERNS = listOf(
    Regex("""youtube\.com/watch\?v=([\w-]{6,})"""),
    Regex("""youtu\.be/([\w-]{6,})"""),
    Regex("""youtube\.com/embed/([\w-]{6,})"""),
)

private fun youTubeThumbnailUrl(link: String): String? {
    for (pattern in YOUTUBE_ID_PATTERNS) {
        val id = pattern.find(link)?.groupValues?.getOrNull(1)
        if (id != null) return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }
    return null
}
