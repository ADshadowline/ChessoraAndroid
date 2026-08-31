package org.chessora.app.ui.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDate

@Composable
fun NewsDetailScreen(idClub: Int, idNews: Int) {
    val viewModel = chessoraViewModel { app -> NewsDetailViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(idClub, idNews) { viewModel.load(idClub, idNews) }

    UiStateContent(state = state, onRetry = { viewModel.load(idClub, idNews) }) { data ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val imageUrl = NetworkModule.resolveAssetUrl(data.article.image)
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = data.article.title,
                    modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
                )
            }
            Text(data.article.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${data.article.author} · ${(data.article.publishedAt ?: data.article.createdAt).toItalianDate()}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            Text(data.article.body, style = MaterialTheme.typography.bodyLarge)

            if (data.comments.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 24.dp))
                Text("Commenti", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                data.comments.forEach { comment -> CommentRow(comment) }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: NewsComment) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        Text(comment.createdAt.toItalianDate(), style = MaterialTheme.typography.labelLarge)
    }
}
