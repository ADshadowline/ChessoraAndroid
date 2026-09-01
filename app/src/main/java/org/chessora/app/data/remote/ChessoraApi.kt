package org.chessora.app.data.remote

import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.remote.dto.ClubStats
import org.chessora.app.data.remote.dto.EventType
import org.chessora.app.data.remote.dto.GoogleReviewsResponse
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.remote.dto.NextTournament
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ResolveClubResponse
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.remote.dto.Torneo
import org.chessora.app.data.remote.dto.VideoNewsItem
import org.chessora.app.data.remote.dto.VideoRow
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Specchio 1:1 di docs/android-app-spec.md §6 (repository server Chessora,
 * https://api.chessora.org): TUTTI gli endpoint elencati lì sono pubblici,
 * senza autenticazione, esattamente come dichiarato in cima a quella sezione.
 * Se un endpoint nuovo viene aggiunto lato server, questa è la prima cosa da
 * estendere - dopodiché il DTO corrispondente in data/remote/dto/ e infine il
 * punto di chiamata in data/repository/ChessoraRepository.kt.
 *
 * NON aggiungere qui endpoint che richiedono autenticazione admin (tutto ciò
 * che sta sotto un percorso "…/admin" oppure "api/platform/…"): l'app non ha
 * login, per design (vedi docs/android-app-spec.md §1 "Fuori scope").
 */
interface ChessoraApi {

    // ---------- Circoli ----------

    @GET("api/clubs/directory")
    suspend fun getClubDirectory(): List<ClubDirectoryItem>

    @GET("api/clubs/resolve")
    suspend fun resolveClubByCode(@Query("code") code: String): ResolveClubResponse

    // ---------- News ----------

    @GET("api/news")
    suspend fun getNews(@Query("club") club: String, @Query("limit") limit: Int = 30): List<NewsArticle>

    @GET("api/news/{idNews}/comments")
    suspend fun getNewsComments(@Path("idNews") idNews: Int): List<NewsComment>

    // ---------- Calendario ----------

    @GET("api/event-types")
    suspend fun getEventTypes(): List<EventType>

    /** [from]/[to] nel formato "yyyy-MM-dd", come richiesto dal server. */
    @GET("api/calendar")
    suspend fun getCalendar(
        @Query("club") club: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): List<CalendarEvent>

    // ---------- Tornei e locandine ----------

    @GET("api/tornei")
    suspend fun getTornei(@Query("club") club: String): List<Torneo>

    @GET("api/tornei/next-upcoming")
    suspend fun getNextUpcomingTournament(@Query("club") club: String): NextTournament?

    // Nota: GET /api/bandi/render/{idTorneo} NON è qui - restituisce HTML pronto
    // da caricare direttamente in una WebView (o da aprire con un Intent verso il
    // browser di sistema), non JSON: vedi ui/tornei/TorneoDetailScreen.kt, che
    // compone l'URL a mano (NetworkModule.API_BASE_URL + "api/bandi/render/" + id)
    // invece di passare da Retrofit.

    // ---------- Classifica ----------

    @GET("api/ranking/assoluta")
    suspend fun getRankingAssoluta(@Query("limit") limit: Int = 20): RankingResponse

    @GET("api/ranking/nazionale")
    suspend fun getRankingNazionale(@Query("limit") limit: Int = 20): RankingResponse

    @GET("api/ranking/circolo")
    suspend fun getRankingCircolo(@Query("club") club: String, @Query("limit") limit: Int = 20): RankingResponse

    // ---------- Direttivo ----------

    @GET("api/board/years")
    suspend fun getBoardYears(@Query("club") club: String): List<Int>

    @GET("api/board")
    suspend fun getBoard(@Query("club") club: String, @Query("year") year: Int? = null): List<BoardMember>

    // ---------- Negozio ----------

    @GET("api/shop/products")
    suspend fun getShopProducts(@Query("club") club: String): List<ShopProduct>

    // ---------- Altro ----------

    @GET("api/stats")
    suspend fun getStats(@Query("club") club: String): ClubStats

    @GET("api/google-reviews")
    suspend fun getGoogleReviews(@Query("club") club: String): GoogleReviewsResponse

    @GET("api/video-rows")
    suspend fun getVideoRows(@Query("club") club: String): List<VideoRow>

    @GET("api/video-news")
    suspend fun getVideoNews(@Query("club") club: String, @Query("limit") limit: Int = 20): List<VideoNewsItem>

    @GET("api/site-settings")
    suspend fun getSiteSettings(@Query("club") club: String): SiteSettings

    // ---------- Notifiche push ----------

    @POST("api/devices/register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest)
}
