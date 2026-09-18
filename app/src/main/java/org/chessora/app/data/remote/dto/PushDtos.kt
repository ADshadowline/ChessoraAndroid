package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Specchio di Chessora.Contracts.Push.RegisterDeviceRequest (POST
 * /api/devices/register). [platform] è sempre la stringa costante "android"
 * per questa app (vedi push/DeviceRegistration.kt) - il campo esiste lato
 * server per un domani supportare anche iOS con lo stesso endpoint. [idPlayer]
 * è valorizzato solo se l'utente si è identificato (vedi ui/identity/).
 * [appVersionName]/[appVersionCode] sono BuildConfig.VERSION_NAME/VERSION_CODE -
 * registrati lato server per sapere quale versione ha scaricato ogni socio e
 * quando l'ha aggiornata (mostrato in "I Soci" nel wizard).
 */
@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String = "android",
    val idClub: Int? = null,
    val idPlayer: Int? = null,
    val appVersionName: String? = null,
    val appVersionCode: Int? = null,
)

/** Specchio di Chessora.Contracts.Push.ReportDeliveryRequest (POST
 * /api/devices/messages/{idMessage}/delivered|read). */
@Serializable
data class ReportDeliveryRequest(val token: String)
