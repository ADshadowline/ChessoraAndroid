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
import org.chessora.app.data.local.AuthSession
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

    /** Chiamato quando una risposta torna 401 (token JWT assente/scaduto/revocato) - non
     * c'è un refresh-token in questa prima versione (vedi ui/auth/), quindi la sessione va
     * invalidata subito e l'utente rimandato al login. Registrato da ChessoraNavHost
     * all'avvio, cosi' l'interceptor (che non ha accesso a un NavController) può comunque
     * far scattare la navigazione. */
    var onUnauthorized: (() -> Unit)? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // Le chiamate "organizzatore" (api/tornei/admin/**, vedi OrganizerSession)
                // passano il proprio Bearer esplicitamente come @Header per-richiesta,
                // scavalcando questo default: senza il controllo su header() qui sotto la
                // richiesta finirebbe con DUE header Authorization, e un 401/403 su un
                // token organizzatore scaduto disconnetterebbe per errore anche la
                // sessione giocatore, che non c'entra nulla.
                val original = chain.request()
                val hasExplicitAuth = original.header("Authorization") != null
                val token = AuthSession.accessToken
                val request = if (!hasExplicitAuth && !token.isNullOrBlank()) {
                    original.newBuilder().addHeader("Authorization", "Bearer $token").build()
                } else {
                    original
                }
                val response = chain.proceed(request)
                if (!hasExplicitAuth && response.code == 401 && !token.isNullOrBlank()) {
                    AuthSession.accessToken = null
                    onUnauthorized?.invoke()
                }
                response
            }
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

    /** Scarica [url] in [destination] (sovrascritto se già presente) - usato da
     * ui/calendar/BandoViewerScreen.kt per aprire un bando PDF con PdfRenderer, che
     * richiede un file locale (ParcelFileDescriptor), non uno stream di rete diretto. */
    suspend fun downloadToFile(url: String, destination: java.io.File): Unit = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("Risposta vuota")
            destination.outputStream().use { out -> body.byteStream().copyTo(out) }
        }
    }

    /** Client HTTP SEPARATO da [okHttpClient] apposta: quello sopra allega in automatico
     * il Bearer del giocatore (vedi il suo interceptor) a OGNI richiesta priva di un
     * header Authorization esplicito - un token dell'Api Chessora non deve mai arrivare
     * a un host di terze parti come lichess.org. Nessun interceptor di autenticazione
     * qui: le partite pubbliche di un utente Lichess si leggono senza credenziali.
     *
     * BUG REALE trovato in produzione (28/09/2026): lo User-Agent di default di OkHttp
     * ("okhttp/4.x") viene riconosciuto e bloccato dalla protezione anti-scraping di
     * Lichess, che risponde con un 404 "Not found" indistinguibile da uno username
     * inesistente (verificato replicando esattamente lo stesso User-Agent via curl:
     * stesso errore) - da qui lo User-Agent "da browser" esplicito sotto, che invece
     * passa. */
    private val lichessHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", LICHESS_USER_AGENT).build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
    }

    private const val LICHESS_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    /** Le ultime [max] partite pubbliche di [username] su Lichess, più recenti prima
     * (comportamento di default dell'endpoint) - NDJSON, un oggetto JSON per riga, non
     * un array: vedi LichessRepository.fetchGames per il parsing riga per riga. */
    suspend fun fetchLichessGamesNdjson(username: String, max: Int = 50): String = withContext(Dispatchers.IO) {
        val url = "https://lichess.org/api/games/user/$username?max=$max&moves=true&opening=true"
        val request = Request.Builder().url(url).header("Accept", "application/x-ndjson").build()
        lichessHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }

    /** Una singola partita (JSON, non NDJSON) - usata da LichessGameViewerScreen per
     * ricaricare solo la partita aperta invece di portarsi dietro l'intero elenco tra le
     * schermate.
     *
     * BUG REALE trovato in produzione (28/09/2026): il percorso corretto per questo
     * endpoint è "/game/export/{id}" SENZA il prefisso "/api/" (a differenza di
     * fetchLichessGamesNdjson sopra, che invece usa "/api/games/user/..." - due famiglie
     * di endpoint con convenzioni diverse, verificato sullo spec ufficiale di Lichess). */
    suspend fun fetchLichessGameJson(gameId: String): String = withContext(Dispatchers.IO) {
        val url = "https://lichess.org/game/export/$gameId?moves=true&opening=true"
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        lichessHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: ""
        }
    }
}
