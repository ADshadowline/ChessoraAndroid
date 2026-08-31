package org.chessora.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDateTime

@Composable
fun HomeScreen(idClub: Int, onNewsClick: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> HomeViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(idClub) { viewModel.load(idClub) }

    UiStateContent(state = state, onRetry = { viewModel.load(idClub) }) { data ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text(
                    stringResource(R.string.home_next_tournament),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (data.nextTournament != null) {
                            Text(data.nextTournament.titolo, fontWeight = FontWeight.Bold)
                            Text(data.nextTournament.dataOra.toItalianDateTime(), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text(stringResource(R.string.home_no_tournament))
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.home_latest_news),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(data.latestNews, key = { it.id }) { article ->
                NewsSummaryCard(article = article, onClick = { onNewsClick(article.id) })
            }
        }
    }
}

@Composable
fun NewsSummaryCard(article: NewsArticle, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(article.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (article.excerpt.isNotBlank()) {
                Text(article.excerpt, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
