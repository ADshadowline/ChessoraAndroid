package org.chessora.app.ui.identity

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.tasks.await

/**
 * "Phone Number Hint" di Google: mostra un popup nativo con il/i numero/i
 * associato/i alla SIM o all'account Google del dispositivo, che l'utente
 * conferma con un tap - su Android moderno non esiste modo di leggere il
 * numero della SIM in silenzio, questa è l'API ufficiale più vicina.
 */
object PhoneHintHelper {

    /** Costruisce l'IntentSender da lanciare con un ActivityResultLauncher
     * (StartIntentSenderForResult); null se Play Services non è disponibile o
     * non ci sono numeri da proporre. */
    suspend fun buildHintIntentSender(activity: Activity): IntentSender? = try {
        val request = GetPhoneNumberHintIntentRequest.builder().build()
        val pendingIntent = Identity.getSignInClient(activity).getPhoneNumberHintIntent(request).await()
        pendingIntent.intentSender
    } catch (e: Exception) {
        null
    }

    /** Da chiamare nel callback del launcher con l'Intent restituito. */
    fun extractPhoneNumber(activity: Activity, data: Intent?): String? = try {
        data?.let { Identity.getSignInClient(activity).getPhoneNumberFromIntent(it) }
    } catch (e: Exception) {
        null
    }
}
