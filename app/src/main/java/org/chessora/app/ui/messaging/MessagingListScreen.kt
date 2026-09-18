package org.chessora.app.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import org.chessora.app.R
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.ConversationSummary
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraError
import org.chessora.app.ui.theme.ChessoraGold

/**
 * Elenco conversazioni (ui/messaging/): "Chessora" (storico notifiche push, sola
 * lettura) sempre in testa - così come restituita dal server - seguita dalle
 * conversazioni dirette con altri soci (di qualsiasi circolo, se identificati in
 * app) e con i circoli. Il pulsante "+" apre la ricerca per iniziarne una nuova.
 */
@Composable
fun MessagingListScreen(onOpenConversation: (ConversationSummary) -> Unit, onNewMessage: () -> Unit, onIdentify: () -> Unit) {
    val viewModel = chessoraViewModel { app -> MessagingListViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        floatingActionButton = {
            if (state is MessagingListState.Ready) {
                FloatingActionButton(onClick = onNewMessage) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.messaging_new))
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            is MessagingListState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is MessagingListState.NotIdentified -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.messaging_not_identified), style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onIdentify, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.settings_identity_button))
                    }
                }
            }
            is MessagingListState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(current.message)
            }
            is MessagingListState.Ready -> {
                if (current.conversations.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.messaging_empty))
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = padding.calculateBottomPadding())) {
                        items(current.conversations, key = { it.idConversation }) { conversation ->
                            ConversationRow(conversation, onClick = { onOpenConversation(conversation) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ConversationSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConversationAvatar(conversation)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(
                conversation.displayName,
                fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            conversation.lastMessageBody?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (conversation.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ChessoraError),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    conversation.unreadCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ConversationAvatar(conversation: ConversationSummary) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        when {
            conversation.idConversation == 0 -> Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(ChessoraGold),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White)
            }
            conversation.photoPath != null -> SubcomposeAsyncImage(
                model = NetworkModule.resolveAssetUrl(conversation.photoPath),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
            ) {
                if (painter.state is AsyncImagePainter.State.Success) SubcomposeAsyncImageContent() else FallbackAvatarIcon(conversation.isClubConversation)
            }
            else -> FallbackAvatarIcon(conversation.isClubConversation)
        }
    }
}

@Composable
private fun FallbackAvatarIcon(isClubConversation: Boolean) {
    Icon(
        if (isClubConversation) Icons.Default.Group else Icons.Default.Person,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
