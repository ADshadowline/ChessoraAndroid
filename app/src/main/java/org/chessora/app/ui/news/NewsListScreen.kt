package org.chessora.app.ui.news

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.home.NewsSummaryCard

@Composable
fun NewsListScreen(idClub: Int, onNewsClick: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> NewsListViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(idClub) { viewModel.load(idClub) }

    UiStateContent(state = state, onRetry = { viewModel.load(idClub) }) { articles ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(articles, key = { it.id }) { article ->
                NewsSummaryCard(article = article, onClick = { onNewsClick(article.id) })
            }
        }
    }
}
