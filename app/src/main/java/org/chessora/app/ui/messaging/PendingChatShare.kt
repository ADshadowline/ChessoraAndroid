package org.chessora.app.ui.messaging

import java.io.File

/** PDF da inviare in chat (es. "Le mie partite" > Condividi > Invia in chat Chessora): si
 * sceglie il destinatario in NewMessageScreen e la chat che si apre lo invia da sola appena
 * pronta (vedi ConversationViewModel.load), poi lo svuota. */
object PendingChatShare {
    @Volatile
    var pdf: File? = null

    fun take(): File? = pdf.also { pdf = null }
}
