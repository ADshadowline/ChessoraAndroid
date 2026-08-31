package org.chessora.app.ui.tornei

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chessora.app.data.remote.dto.Torneo
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.common.toItalianDate

@Composable
fun TorneiListScreen(idClub: Int, onTorneoClick: (Int) -> Unit) {
    val viewModel = chessoraViewModel { app -> TorneiListViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(idClub) { viewModel.load(idClub) }

    UiStateContent(state = state, onRetry = { viewModel.load(idClub) }) { tornei ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(tornei, key = { it.id }) { torneo ->
                TorneoRow(torneo = torneo, onClick = { onTorneoClick(torneo.id) })
            }
        }
    }
}

@Composable
private fun TorneoRow(torneo: Torneo, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(torneo.titolo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(torneo.cadenzaNome, style = MaterialTheme.typography.bodyMedium)
            val firstDate = torneo.date.minByOrNull { it.dataOra }
            if (firstDate != null) {
                Text(firstDate.dataOra.toItalianDate(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
