package org.chessora.app.ui.news

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDate

@Composable
fun NewsDetailScreen(club: String, idNews: Int) {
    val viewModel = chessoraViewModel { app -> NewsDetailViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(club, idNews) { viewModel.load(club, idNews) }

    UiStateContent(state = state, onRetry = { viewModel.load(club, idNews) }) { data ->
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
            ArticleBodyHtml(html = data.article.body)

            if (data.comments.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 24.dp))
                Text("Commenti", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                data.comments.forEach { comment -> CommentRow(comment) }
            }
        }
    }
}

// L'articolo (data.article.body) è HTML prodotto dall'editor Quill del wizard (paragrafi,
// grassetto, link...): un Text() semplice mostrerebbe i tag letteralmente. TextView +
// HtmlCompat.fromHtml è il modo standard per renderizzare quell'HTML in Compose senza
// portarsi dietro una WebView (più pesante, e qui non serve JS/CSS).
@Composable
private fun ArticleBodyHtml(html: String) {
    val bodyColorArgb = LocalContentColor.current.toArgb()
    val bodyTextSizeSp = MaterialTheme.typography.bodyLarge.fontSize.value
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            textView.setTextColor(bodyColorArgb)
            textView.textSize = bodyTextSizeSp
        },
    )
}

@Composable
private fun CommentRow(comment: NewsComment) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(comment.author, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        Text(comment.createdAt.toItalianDate(), style = MaterialTheme.typography.labelLarge)
    }
}
