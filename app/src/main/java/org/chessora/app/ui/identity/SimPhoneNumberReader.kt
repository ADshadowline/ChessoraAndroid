package org.chessora.app.ui.identity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Lettura silenziosa del numero della SIM (TelephonyManager), usata solo dopo
 * un login Google riuscito: se il telefono ha un'unica SIM/numero disponibile,
 * viene preso automaticamente senza alcun popup di conferma - a differenza del
 * Phone Number Hint di Google (vedi PhoneHintHelper.kt), riservato al tasto
 * esplicito "Usa il numero di telefono". Su molti operatori/dispositivi
 * (doppia SIM, numero non provisionato dall'operatore) il valore può comunque
 * risultare vuoto: in quel caso si procede con la sola email, senza chiedere
 * altro all'utente.
 */
object SimPhoneNumberReader {

    fun read(context: Context): String? {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_NUMBERS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            @Suppress("DEPRECATION")
            telephonyManager?.line1Number?.takeIf { it.isNotBlank() }
        } catch (e: SecurityException) {
            null
        }
    }
}
