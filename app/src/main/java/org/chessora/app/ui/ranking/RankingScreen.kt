package org.chessora.app.ui.ranking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.RankingPlayer
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel

private val SCOPES = listOf(RankingScope.CIRCOLO to "Circolo", RankingScope.NAZIONALE to "Nazionale", RankingScope.ASSOLUTA to "Assoluta")
private val CADENCES = listOf("Standard", "Rapid", "Blitz")

@Composable
fun RankingScreen(club: String?) {
    val viewModel = chessoraViewModel { app -> RankingViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    var scopeIndex by remember { mutableIntStateOf(0) }
    var cadenceIndex by remember { mutableIntStateOf(0) }
    val scope = SCOPES[scopeIndex].first

    LaunchedEffect(scope, club) { viewModel.load(scope, club) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = scopeIndex) {
            SCOPES.forEachIndexed { index, (_, label) ->
                Tab(selected = scopeIndex == index, onClick = { scopeIndex = index }, text = { Text(label) })
            }
        }
        TabRow(selectedTabIndex = cadenceIndex) {
            CADENCES.forEachIndexed { index, label ->
                Tab(selected = cadenceIndex == index, onClick = { cadenceIndex = index }, text = { Text(label) })
            }
        }

        UiStateContent(state = state, onRetry = { viewModel.load(scope, club) }) { ranking ->
            val players = when (cadenceIndex) {
                0 -> ranking.standard
                1 -> ranking.rapid
                else -> ranking.blitz
            }
            RankingList(players)
        }
    }
}

@Composable
private fun RankingList(players: List<RankingPlayer>) {
    if (players.isEmpty()) {
        Text(stringResource(R.string.ranking_no_data), modifier = Modifier.fillMaxSize().padding(16.dp))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        itemsIndexed(players) { index, player ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerAvatar(idPlayer = player.idPlayer, modifier = Modifier.padding(end = 10.dp))
                    Text("${index + 1}. ${player.name}", fontWeight = FontWeight.Medium)
                }
                Text(player.elo?.toString() ?: "—", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Foto profilo del socio (ui/profile/), se ne ha scelta una - con [idPlayer]
 * null (classifica Assoluta, giocatori FIDE nel mondo) mostra subito l'icona
 * generica senza fare alcuna chiamata; altrimenti prova a caricarla e ricade
 * sulla stessa icona se il socio non ne ha ancora caricata una (404). */
@Composable
private fun PlayerAvatar(idPlayer: Int?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(32.dp), contentAlignment = Alignment.Center) {
        if (idPlayer != null) {
            SubcomposeAsyncImage(
                model = NetworkModule.resolveAssetUrl("/api/players/$idPlayer/photo"),
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(CircleShape),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else PlaceholderAvatarIcon()
            }
        } else {
            PlaceholderAvatarIcon()
        }
    }
}

@Composable
private fun PlaceholderAvatarIcon() {
    Icon(
        Icons.Default.AccountCircle,
        contentDescription = null,
        modifier = Modifier.size(32.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
