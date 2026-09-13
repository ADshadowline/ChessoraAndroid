package org.chessora.app.data.remote

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.chessora.app.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Costruzione manuale delle dipendenze di rete (niente Hilt/Dagger: un solo
 * modulo applicativo con poche dipendenze non giustifica l'overhead di un
 * framework di dependency injection con annotation processing - vedi
 * ChessoraApplication.kt per come questo oggetto viene istanziato una sola
 * volta e condiviso da tutti i ViewModel).
 */
object NetworkModule {

    /**
     * URL base dell'Api, letto da BuildConfig (vedi app/build.gradle.kts) invece
     * che scritto qui: un domani una build "debug" potrebbe puntare a un
     * ambiente diverso da produzione senza toccare questo file.
     */
    val API_BASE_URL: String = BuildConfig.API_BASE_URL

    private val json = Json {
        // Il server puo' aggiungere campi ai DTO in futuro senza che l'app - che
        // magari non è ancora stata aggiornata dall'utente - smetta di funzionare.
        ignoreUnknownKeys = true
        // I DTO usano valori di default per i campi opzionali (vedi data/remote/dto/*):
        // coerente con l'idea che un campo nullable/opzionale assente nel JSON
        // non deve far fallire la deserializzazione.
        explicitNulls = false
        // BUG REALE trovato testando su dispositivo (31/08/2026): senza questo,
        // kotlinx.serialization NON serializza i campi che hanno il valore di
        // default dichiarato (es. RegisterDeviceRequest.platform = "android"),
        // quindi POST /api/devices/register partiva senza "platform" nel body e
        // il server rispondeva 400 "The Platform field is required." - il campo
        // era lì nel DTO Kotlin, semplicemente non veniva mai scritto nel JSON.
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // Log delle chiamate HTTP solo in build debug: mai in release,
                    // per non scrivere URL/risposte nel logcat di produzione.
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }

    val api: ChessoraApi by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChessoraApi::class.java)
    }

    /**
     * Risolve un percorso relativo restituito dal server (es. "/repository/Clubs/123/logo.png")
     * in un URL assoluto scaricabile - vedi docs/android-app-spec.md §6 "Tutte le
     * immagini/allegati... sono percorsi relativi... vanno risolti concatenando
     * https://api.chessora.org davanti". Se [path] è già un URL assoluto (es. un
     * link a un video YouTube) o è null/vuoto, viene restituito così com'è.
     */
    fun resolveAssetUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return API_BASE_URL.trimEnd('/') + path
    }

    /**
     * GET generico fuori da Retrofit/ChessoraApi: serve solo per
     * AppUpdateChecker, che legge un file statico su chessora.org (non
     * api.chessora.org, quindi fuori dalla baseUrl di Retrofit sopra) e non
     * un vero endpoint /api.
     */
    suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }
}
