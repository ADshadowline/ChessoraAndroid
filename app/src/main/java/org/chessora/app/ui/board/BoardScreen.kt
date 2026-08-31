package org.chessora.app.ui.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

/**
 * Piramide gerarchica per livello (docs/android-app-spec.md §7.7, "come il
 * sito web"): un livello = una sezione con intestazione, i membri di quel
 * livello elencati sotto in ordine di sortOrder.
 */
@Composable
fun BoardScreen(idClub: Int) {
    val viewModel = chessoraViewModel { app -> BoardViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(idClub) { viewModel.load(idClub) }

    UiStateContent(state = state, onRetry = { viewModel.load(idClub) }) { members ->
        val byLevel = members.groupBy { it.level }.toSortedMap()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            byLevel.forEach { (level, levelMembers) ->
                item {
                    Text(
                        "Livello $level",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    )
                }
                items(levelMembers, key = { it.id }) { member -> BoardMemberRow(member) }
            }
        }
    }
}

@Composable
private fun BoardMemberRow(member: BoardMember) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        val photoUrl = NetworkModule.resolveAssetUrl(member.photoPath)
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = member.fullName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(member.fullName, fontWeight = FontWeight.Bold)
            Text(member.role, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
