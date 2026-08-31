package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Specchio di Chessora.Contracts.Push.RegisterDeviceRequest (POST
 * /api/devices/register). [platform] è sempre la stringa costante "android"
 * per questa app (vedi push/DeviceRegistration.kt) - il campo esiste lato
 * server per un domani supportare anche iOS con lo stesso endpoint.
 */
@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String = "android",
    val idClub: Int? = null,
)
