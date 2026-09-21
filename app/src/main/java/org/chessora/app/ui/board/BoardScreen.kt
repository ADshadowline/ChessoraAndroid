package org.chessora.app.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.ui.common.UiStateContent
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraGold

/**
 * Piramide gerarchica per livello, graficamente come Direttivo.cshtml sul sito
 * (card con foto tonda bordata d'oro, nome in grassetto, ruolo in oro sotto):
 * un livello = una riga che si allarga/restringe in base a quanti membri
 * contiene, i livelli impilati dall'alto verso il basso in ordine crescente.
 */
@Composable
fun BoardScreen(club: String, onOpenConversation: (idPlayer: Int, displayName: String) -> Unit) {
    val viewModel = chessoraViewModel { app -> BoardViewModel(app.repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(club) { viewModel.load(club) }

    UiStateContent(state = state, onRetry = { viewModel.load(club) }) { members ->
        val byLevel = members.groupBy { it.level }.toSortedMap()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            byLevel.forEach { (_, levelMembers) ->
                item {
                    BoardLevelRow(levelMembers.sortedBy { it.sortOrder }, onOpenConversation)
                }
            }
        }
    }
}

/** Scorrimento orizzontale invece di un semplice Row che va a capo (FlowRow, versione
 * precedente): con 3+ persone in un livello (es. i Consiglieri) l'ultima finiva su una
 * riga a sé scomoda invece che scorrere in linea con le altre. */
@Composable
private fun BoardLevelRow(levelMembers: List<BoardMember>, onOpenConversation: (idPlayer: Int, displayName: String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(levelMembers, key = { it.idPlayer }) { member -> BoardMemberCard(member, onOpenConversation) }
    }
}

/** Il click sulla card (foto compresa) apre direttamente la Messaggistica con questa
 * persona già selezionata come destinataria - stesso meccanismo già usato da "Nuovo
 * messaggio" (ChessoraNavHost.kt, ChessoraDestinations.conversation con idConversation=-1),
 * pronta a scrivere e inviare il primo messaggio. */
@Composable
private fun BoardMemberCard(member: BoardMember, onOpenConversation: (idPlayer: Int, displayName: String) -> Unit) {
    val photoUrl = NetworkModule.resolveAssetUrl(member.photoPath)
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { onOpenConversation(member.idPlayer, member.fullName) }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, ChessoraGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = member.fullName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(Icons.Filled.Person, contentDescription = null, tint = ChessoraGold, modifier = Modifier.size(36.dp))
            }
        }
        Text(
            member.fullName,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            member.roles.joinToString(" · ").uppercase(),
            color = ChessoraGold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
