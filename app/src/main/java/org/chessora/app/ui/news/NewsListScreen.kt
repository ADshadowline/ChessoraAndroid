package org.chessora.app.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.R
import org.chessora.app.data.remote.dto.NetworkNewsItem
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.home.NewsSummaryCard

private val SCOPES = listOf(NewsScope.CIRCOLO, NewsScope.MONDO)

@Composable
fun NewsListScreen(club: String?, onNewsClick: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> NewsListViewModel(app.repository) }
    val state by viewModel.state.collectAsState()
    val scope by viewModel.scope.collectAsState()
    val query by viewModel.query.collectAsState()
    val context = LocalContext.current
    val scopeIndex = SCOPES.indexOf(scope).coerceAtLeast(0)

    LaunchedEffect(club) { viewModel.load(club) }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.setQuery(it) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text(stringResource(R.string.news_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        TabRow(selectedTabIndex = scopeIndex) {
            Tab(
                selected = scope == NewsScope.CIRCOLO,
                onClick = { viewModel.setScope(NewsScope.CIRCOLO) },
                text = { Text(stringResource(R.string.news_scope_circolo)) },
            )
            Tab(
                selected = scope == NewsScope.MONDO,
                onClick = { viewModel.setScope(NewsScope.MONDO) },
                text = { Text(stringResource(R.string.news_scope_mondo)) },
            )
        }

        UiStateContent(state = state, onRetry = { viewModel.retry() }) { items ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(items, key = { it.key() }) { item ->
                    when (item) {
                        is NewsListItem.Club -> NewsSummaryCard(article = item.article, onClick = { onNewsClick(item.article.id) })
                        is NewsListItem.Network -> NetworkNewsSummaryCard(
                            item = item.item,
                            onClick = {
                                item.item.link?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun NewsListItem.key(): String = when (this) {
    is NewsListItem.Club -> "club-${article.id}"
    is NewsListItem.Network -> "network-${item.id}"
}

/** Notizia FIDE/globale (filtro "Mondo") - solo titolo/fonte/data, apre [item.link]
 * esternamente al tap (non ha un corpo articolo proprio da mostrare in-app). */
@Composable
private fun NetworkNewsSummaryCard(item: NetworkNewsItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(item.source, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
            }
            if (item.link != null) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
