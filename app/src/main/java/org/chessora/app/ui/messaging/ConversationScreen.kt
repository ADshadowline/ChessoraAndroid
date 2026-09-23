package org.chessora.app.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.chessora.app.R
import org.chessora.app.data.remote.dto.ChatMessage
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.theme.ChessoraSkyBlue

/**
 * Thread di una conversazione (ui/messaging/): bolle allineate a destra (miei
 * messaggi) o sinistra, con un campo di invio in basso - assente per la chat
 * "Chessora" (isReadOnly, storico notifiche push).
 */
@Composable
fun ConversationScreen(idConversation: Int, isClubConversation: Boolean, recipientId: Int, displayName: String) {
    val viewModel = chessoraViewModel {
        ConversationViewModel(it.repository, it.clubPreferences, idConversation, isClubConversation, recipientId)
    }
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }
    // Aggiorna in silenzio (nessun flash di caricamento) lo stato consegnato/letto dei
    // MIEI messaggi mentre si resta dentro la conversazione - senza questo, la spunta
    // resterebbe quella del momento dell'invio finché non si esce e si rientra
    // (viewModel.load() gira solo una volta, al primo ingresso).
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            viewModel.refreshSilently()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            displayName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        )
        when (val current = state) {
            is ConversationState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is ConversationState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(current.message)
            }
            is ConversationState.Ready -> {
                LaunchedEffect(current.messages.size) {
                    if (current.messages.isNotEmpty()) listState.animateScrollToItem(current.messages.size - 1)
                }
                if (current.messages.isEmpty()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(if (current.isReadOnly) R.string.messaging_chessora_empty else R.string.messaging_conversation_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                    ) {
                        items(current.messages, key = { it.id }) { message -> MessageBubble(message) }
                    }
                }

                if (!current.isReadOnly) {
                    var text by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.messaging_type_hint)) },
                            enabled = !current.sending,
                        )
                        IconButton(
                            onClick = {
                                val toSend = text
                                text = ""
                                scope.launch { viewModel.send(toSend) }
                            },
                            enabled = !current.sending && text.isNotBlank(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.messaging_send))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isMine = message.isMine
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    if (isMine) ChessoraSkyBlue else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!isMine) {
                Text(
                    message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(message.body, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    formatTime(message.sentAtUtc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Doppia spunta stile WhatsApp, solo sui MIEI messaggi: grigia (Done/
                // DoneAll) se non ancora letta, colorata (oro, per contrasto sullo sfondo
                // azzurro della bolla) se il destinatario l'ha letta - vedi
                // ChatMessage.isDelivered/isRead (null/false se il destinatario ha
                // disattivato le conferme in Impostazioni, vedi backend).
                if (isMine) {
                    Icon(
                        if (message.isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (message.isRead) {
                            stringResource(R.string.messaging_status_read)
                        } else if (message.isDelivered) {
                            stringResource(R.string.messaging_status_delivered)
                        } else {
                            stringResource(R.string.messaging_status_sent)
                        },
                        tint = if (message.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp).size(14.dp),
                    )
                }
            }
        }
    }
}

private fun formatTime(iso: String): String = try {
    val normalized = if (iso.endsWith("Z") || iso.contains('+')) iso else "${iso}Z"
    val instant = Instant.parse(normalized)
    instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
} catch (e: Exception) {
    ""
}
