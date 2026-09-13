package org.chessora.app.data.remote

import kotlinx.serialization.json.Json
import org.chessora.app.BuildConfig
import org.chessora.app.data.remote.dto.AppVersionInfo

/**
 * L'app non è sul Play Store (distribuita via chessora.org/downloads, vedi
 * README del repository server), quindi non esiste un canale di
 * aggiornamento automatico: questo controllo sostituisce quel meccanismo,
 * confrontando BuildConfig.VERSION_CODE con wwwroot/downloads/version.json
 * (aggiornato a mano a ogni release, insieme all'apk).
 */
object AppUpdateChecker {
    private const val VERSION_URL = "https://chessora.org/downloads/version.json"
    private val json = Json { ignoreUnknownKeys = true }

    /** null sia se non c'è una versione più recente, sia in caso di errore di rete - un
     * controllo di aggiornamento che fallisce non deve mai interrompere l'uso dell'app. */
    suspend fun checkForUpdate(): AppVersionInfo? = try {
        val body = NetworkModule.fetchText(VERSION_URL)
        val info = json.decodeFromString<AppVersionInfo>(body)
        info.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    } catch (e: Exception) {
        null
    }
}
