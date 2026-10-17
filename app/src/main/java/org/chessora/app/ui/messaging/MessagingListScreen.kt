package org.chessora.app.ui.messaging

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
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
 *
 * Pressione prolungata su una chat = modalità selezione (tocchi successivi aggiungono/
 * tolgono), con il cestino nella barra in alto per cancellarle dal proprio elenco. Il menu
 * ⋮ di una chat diretta permette di bloccare/sbloccare l'altro socio. La chat "Chessora"
 * non è né selezionabile né bloccabile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingListScreen(onOpenConversation: (ConversationSummary) -> Unit, onNewMessage: () -> Unit, onIdentify: () -> Unit) {
    val viewModel = chessoraViewModel { app -> MessagingListViewModel(app.repository, app.clubPreferences) }
    val state by viewModel.state.collectAsState()
    val actionFailed by viewModel.actionFailed.collectAsState()

    var selected by remember { mutableStateOf(emptySet<Int>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var blockCandidate by remember { mutableStateOf<ConversationSummary?>(null) }
    val selectionMode = selected.isNotEmpty()

    LaunchedEffect(Unit) { viewModel.load() }
    BackHandler(enabled = selectionMode) { selected = emptySet() }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { selected = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.messaging_cancel_selection))
                        }
                    },
                    title = { Text(pluralStringResource(R.plurals.messaging_selected_count, selected.size, selected.size)) },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.messaging_delete_selected))
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (state is MessagingListState.Ready && !selectionMode) {
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
                        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                    ) {
                        items(current.conversations, key = { it.idConversation }) { conversation ->
                            val selectable = conversation.idConversation > 0
                            ConversationRow(
                                conversation = conversation,
                                isSelected = conversation.idConversation in selected,
                                selectionMode = selectionMode,
                                onClick = {
                                    when {
                                        !selectionMode -> onOpenConversation(conversation)
                                        selectable -> selected = selected.toggle(conversation.idConversation)
                                    }
                                },
                                onLongClick = { if (selectable) selected = selected.toggle(conversation.idConversation) },
                                onBlock = { blockCandidate = conversation },
                                onUnblock = { conversation.otherIdPlayer?.let(viewModel::unblockPlayer) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(pluralStringResource(R.plurals.messaging_delete_confirm_title, selected.size, selected.size)) },
            text = { Text(stringResource(R.string.messaging_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.hideConversations(selected)
                    selected = emptySet()
                }) { Text(stringResource(R.string.messaging_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.messaging_cancel)) }
            },
        )
    }

    blockCandidate?.let { conversation ->
        AlertDialog(
            onDismissRequest = { blockCandidate = null },
            title = { Text(stringResource(R.string.messaging_block_confirm_title, conversation.displayName)) },
            text = { Text(stringResource(R.string.messaging_block_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    blockCandidate = null
                    conversation.otherIdPlayer?.let(viewModel::blockPlayer)
                }) { Text(stringResource(R.string.messaging_block_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { blockCandidate = null }) { Text(stringResource(R.string.messaging_cancel)) }
            },
        )
    }

    if (actionFailed) {
        AlertDialog(
            onDismissRequest = { viewModel.clearActionFailed() },
            text = { Text(stringResource(R.string.messaging_action_failed)) },
            confirmButton = { TextButton(onClick = { viewModel.clearActionFailed() }) { Text("OK") } },
        )
    }
}

private fun Set<Int>.toggle(id: Int): Set<Int> = if (id in this) this - id else this + id

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val canBlock = !conversation.isClubConversation && conversation.otherIdPlayer != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(start = 16.dp, end = if (canBlock && !selectionMode) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelected) SelectedAvatar() else ConversationAvatar(conversation)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    conversation.displayName,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (conversation.blockedByMe) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = stringResource(R.string.messaging_blocked_label),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
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
        if (canBlock && !selectionMode) {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.messaging_more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (conversation.blockedByMe) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.messaging_unblock)) },
                            onClick = { menuOpen = false; onUnblock() },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.messaging_block)) },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = { menuOpen = false; onBlock() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedAvatar() {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
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
