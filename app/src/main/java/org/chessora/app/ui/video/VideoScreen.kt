package org.chessora.app.ui.video

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import org.chessora.app.R
import org.chessora.app.data.remote.dto.VideoItem
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/** Elenco video del circolo (YouTube, gestiti dal wizard) - sola consultazione, il tocco
 * apre il video nell'app YouTube/browser (stesso meccanismo di bando/sito web nel
 * dettaglio torneo), nessuna riproduzione incorporata. */
@Composable
fun VideoScreen(club: String) {
    val viewModel = chessoraViewModel { app -> VideoViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { videos ->
        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.video_empty))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(video, onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.link))) })
                }
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoItem, onClick: () -> Unit) {
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
            if (video.description.isNotBlank()) {
                Text(
                    video.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
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
