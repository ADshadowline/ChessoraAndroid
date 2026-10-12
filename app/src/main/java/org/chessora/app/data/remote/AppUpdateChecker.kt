package org.chessora.app.data.remote

import kotlinx.serialization.json.Json
import org.chessora.app.BuildConfig
import org.chessora.app.data.remote.dto.AppVersionInfo

/**
 * Solo per l'apk distribuito da chessora.org/downloads (flavor "sideload"): lì non esiste
 * un canale di aggiornamento automatico, quindi questo controllo lo sostituisce,
 * confrontando BuildConfig.VERSION_CODE con wwwroot/downloads/version.json (aggiornato a
 * ogni release, insieme all'apk). Nel flavor "play" (Google Play Store) è spento
 * (BuildConfig.SELF_UPDATE=false): gli aggiornamenti li gestisce lo store, e le sue norme
 * vietano di proporre aggiornamenti da altre fonti.
 */
object AppUpdateChecker {
    private const val VERSION_URL = "https://chessora.org/downloads/version.json"
    private val json = Json { ignoreUnknownKeys = true }

    /** null sia se non c'è una versione più recente, sia in caso di errore di rete - un
     * controllo di aggiornamento che fallisce non deve mai interrompere l'uso dell'app. */
    suspend fun checkForUpdate(): AppVersionInfo? = if (!BuildConfig.SELF_UPDATE) null else try {
        val body = NetworkModule.fetchText(VERSION_URL)
        val info = json.decodeFromString<AppVersionInfo>(body)
        info.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    } catch (e: Exception) {
        null
    }
}
