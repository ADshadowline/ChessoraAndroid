package org.chessora.app.ui.messaging

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
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
                        items(current.messages, key = { it.id }) { message -> MessageBubble(message, viewModel.myIdPlayer) }
                    }
                }

                if (!current.isReadOnly && current.isBlocked) {
                    // Chat con un blocco attivo (in qualunque direzione): resta leggibile,
                    // ma al posto del campo di invio un avviso - con "Sblocca" se il
                    // blocco l'ho messo io.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(if (current.blockedByMe) R.string.messaging_blocked_by_me_notice else R.string.messaging_blocked_me_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        if (current.blockedByMe) {
                            TextButton(onClick = { viewModel.unblock() }) {
                                Text(stringResource(R.string.messaging_unblock))
                            }
                        }
                    }
                } else if (!current.isReadOnly) {
                    ChatInputBar(
                        sending = current.sending,
                        onSendText = { toSend -> scope.launch { viewModel.send(toSend) } },
                        onSendImage = viewModel::sendImage,
                        onSendVoice = viewModel::sendVoice,
                    )
                }
            }
        }
    }

    val attachmentError by viewModel.attachmentError.collectAsState()
    attachmentError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAttachmentError() },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { viewModel.clearAttachmentError() }) { Text("OK") } },
        )
    }
}

/** Barra di scrittura: allegato foto (galleria o fotocamera) a sinistra; a destra Invia se
 * c'è del testo, altrimenti il microfono. Toccando il microfono parte la registrazione del
 * vocale (max 2 minuti): la barra mostra il tempo con Annulla e Invia. */
@Composable
private fun ChatInputBar(
    sending: Boolean,
    onSendText: (String) -> Unit,
    onSendImage: (ByteArray) -> Unit,
    onSendVoice: (java.io.File, Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var preparingImage by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableIntStateOf(0) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    DisposableEffect(Unit) { onDispose { if (recorder.isRecording) recorder.cancel() } }
    LaunchedEffect(recording) {
        while (recording) {
            elapsedMs = recorder.elapsedMs()
            delay(250)
        }
    }

    fun sendPicked(uri: Uri) {
        preparingImage = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) { compressChatImage(context, uri) }
            preparingImage = false
            if (bytes != null) onSendImage(bytes)
        }
    }

    fun finishRecording() {
        recording = false
        recorder.stop()?.let { (file, duration) -> onSendVoice(file, duration) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) sendPicked(uri)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) sendPicked(uri)
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            recording = recorder.start(onMaxDurationReached = { finishRecording() })
        } else {
            micDenied = true
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (recording) {
            IconButton(onClick = { recording = false; recorder.cancel() }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.messaging_cancel_recording))
            }
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, androidx.compose.foundation.shape.CircleShape))
            Text(
                stringResource(R.string.messaging_recording) + "  " + formatDuration(elapsedMs) + " / " + formatDuration(VoiceRecorder.MAX_DURATION_MS),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = { finishRecording() }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.messaging_send_voice))
            }
            return@Row
        }

        Box {
            if (preparingImage) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp)) }
            } else {
                IconButton(onClick = { menuOpen = true }, enabled = !sending) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = stringResource(R.string.messaging_attach_photo))
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.messaging_gallery)) },
                    leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.messaging_camera)) },
                    leadingIcon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        val dir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
                        val file = java.io.File(dir, "foto_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                )
            }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.messaging_type_hint)) },
            enabled = !sending,
        )
        if (text.isNotBlank()) {
            IconButton(
                onClick = {
                    val toSend = text
                    text = ""
                    onSendText(toSend)
                },
                enabled = !sending,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.messaging_send))
            }
        } else {
            IconButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        recording = recorder.start(onMaxDurationReached = { finishRecording() })
                    } else {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = !sending,
            ) {
                Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.messaging_record_voice))
            }
        }
    }

    if (micDenied) {
        AlertDialog(
            onDismissRequest = { micDenied = false },
            text = { Text(stringResource(R.string.messaging_mic_permission_denied)) },
            confirmButton = { TextButton(onClick = { micDenied = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, myIdPlayer: Int) {
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
            val attachmentUrl = message.attachmentUrl
            when {
                attachmentUrl != null && message.attachmentKind == "image" -> ChatImage(attachmentUrl, myIdPlayer)
                attachmentUrl != null && message.attachmentKind == "audio" -> ChatAudioPlayer(attachmentUrl, myIdPlayer, message.attachmentDurationMs)
            }
            if (message.body.isNotBlank()) {
                Text(message.body, style = MaterialTheme.typography.bodyMedium)
            }
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
