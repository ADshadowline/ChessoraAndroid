package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Specchio di wwwroot/downloads/version.json (repository server Chessora, NON
 * un endpoint /api - l'app non è sul Play Store, quindi non c'è
 * un meccanismo di aggiornamento automatico: questo file va aggiornato a mano
 * a ogni nuova release insieme all'apk, vedi AppUpdateChecker.kt).
 */
@Serializable
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadsUrl: String = "https://chessora.org/downloads/",
    val notes: String? = null,
)
