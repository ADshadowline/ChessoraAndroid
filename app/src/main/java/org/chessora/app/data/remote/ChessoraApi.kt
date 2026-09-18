package org.chessora.app.data.remote

import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.remote.dto.ChatMessage
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.remote.dto.ClubSearchResult
import org.chessora.app.data.remote.dto.ClubStats
import org.chessora.app.data.remote.dto.ConversationSummary
import org.chessora.app.data.remote.dto.EventType
import org.chessora.app.data.remote.dto.EventoBandoInfo
import org.chessora.app.data.remote.dto.GoogleReviewsResponse
import org.chessora.app.data.remote.dto.IdentifyNationalRequestDto
import org.chessora.app.data.remote.dto.IdentifyRequestDto
import org.chessora.app.data.remote.dto.IdentifyResultDto
import org.chessora.app.data.remote.dto.MarkReadRequest
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.PerformanceHistoryDto
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.remote.dto.PlayerSearchResult
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.SendMessageRequest
import org.chessora.app.data.remote.dto.StartClubConversationRequest
import org.chessora.app.data.remote.dto.StartConversationResult
import org.chessora.app.data.remote.dto.StartDirectConversationRequest
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ReportDeliveryRequest
import org.chessora.app.data.remote.dto.ResolveClubResponse
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.remote.dto.PreRegistrationRequest
import org.chessora.app.data.remote.dto.TournamentPreRegistrationResult
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.data.remote.dto.VideoNewsItem
import org.chessora.app.data.remote.dto.VideoRow
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    /** Ultime [limit] news su tutti i circoli attivi - scheda News in "modalità
     * piattaforma" (nessun circolo scelto, vedi ui/onboarding/MembershipQuestionScreen.kt). */
    @GET("api/news/all-clubs")
    suspend fun getNewsAllClubs(@Query("limit") limit: Int = 30): List<NewsArticle>

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

    /** Bando di un evento non-torneo del calendario (il bando di un torneo è già
     * dentro CalendarEvent.tournamentBandoPath, non serve questa fetch). */
    @GET("api/eventi/{id}")
    suspend fun getEventoBando(
        @Path("id") id: Int,
        @Query("club") club: String,
    ): EventoBandoInfo

    /** Solo tornei, su tutti i circoli attivi - Home in "modalità piattaforma"
     * (nessun circolo scelto, vedi ui/onboarding/MembershipQuestionScreen.kt). */
    @GET("api/calendar/all-clubs")
    suspend fun getCalendarAllClubs(
        @Query("from") from: String,
        @Query("to") to: String,
    ): List<CalendarEvent>

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

    // ---------- Identificazione socio (facoltativa, vedi ui/identity/) ----------
    // Anche questi sono pubblici come tutto il resto di questa interfaccia: non è un
    // vero login/sessione, solo un modo per allineare email/telefono in anagrafica.

    @POST("api/players/identify")
    suspend fun identifyPlayer(@Body request: IdentifyRequestDto): IdentifyResultDto

    /** Per chi ha dichiarato di non essere socio di alcun circolo (vedi
     * ui/onboarding/MembershipQuestionScreen.kt) - ricerca su tutta l'anagrafica
     * nazionale invece che nel roster di un solo circolo. */
    @POST("api/players/identify-national")
    suspend fun identifyPlayerNational(@Body request: IdentifyNationalRequestDto): IdentifyResultDto

    /** Storico Elo per il grafico "Le mie performance" (ui/performance/). */
    @GET("api/players/{idPlayer}/performance")
    suspend fun getPlayerPerformance(@Path("idPlayer") idPlayer: Int): PerformanceHistoryDto

    /** Foto profilo (fotocamera/galleria) mostrata accanto al nome nelle liste
     * giocatori - salvata come blob in orga.PlayerContacts (ui/profile/). */
    @Multipart
    @POST("api/players/{idPlayer}/photo")
    suspend fun setPlayerPhoto(@Path("idPlayer") idPlayer: Int, @Part photo: MultipartBody.Part)

    @DELETE("api/players/{idPlayer}/photo")
    suspend fun deletePlayerPhoto(@Path("idPlayer") idPlayer: Int)

    // ---------- Messaggi (ui/messaging/) ----------
    // Pubblici come tutto il resto: il chiamante fornisce il proprio idPlayer, già
    // noto dopo l'identificazione (vedi ui/identity/).

    @GET("api/messaging/conversations")
    suspend fun getConversations(@Query("idPlayer") idPlayer: Int): List<ConversationSummary>

    /** Storico notifiche push già ricevute (chat "Chessora", sola lettura) - chiamarlo
     * segna anche tutto come letto lato server. */
    @GET("api/messaging/chessora-thread")
    suspend fun getChessoraThread(@Query("idPlayer") idPlayer: Int): List<ChatMessage>

    @GET("api/messaging/conversations/{idConversation}/messages")
    suspend fun getConversationMessages(@Path("idConversation") idConversation: Int, @Query("idPlayer") idPlayer: Int): List<ChatMessage>

    @POST("api/messaging/conversations/direct")
    suspend fun startDirectConversation(@Body request: StartDirectConversationRequest): StartConversationResult

    @POST("api/messaging/conversations/club")
    suspend fun startClubConversation(@Body request: StartClubConversationRequest): StartConversationResult

    @POST("api/messaging/conversations/{idConversation}/messages")
    suspend fun sendMessage(@Path("idConversation") idConversation: Int, @Body request: SendMessageRequest): ChatMessage

    @POST("api/messaging/conversations/{idConversation}/read")
    suspend fun markConversationRead(@Path("idConversation") idConversation: Int, @Body request: MarkReadRequest)

    @GET("api/messaging/search-players")
    suspend fun searchMessagingPlayers(@Query("query") query: String, @Query("idPlayer") idPlayer: Int): List<PlayerSearchResult>

    @GET("api/messaging/search-clubs")
    suspend fun searchMessagingClubs(@Query("query") query: String): List<ClubSearchResult>

    // ---------- Tornei (dettaglio/iscrizione dalla Home, ui/tournaments/) ----------
    // Pubblici come tutto il resto: orga.TournamentRegistrations non ha una colonna
    // IdClub, quindi l'elenco è per costruzione già "tutti i circoli" - lo stesso
    // endpoint usato da tourn.chessora.org.

    @GET("api/tornei")
    suspend fun getTornei(): List<TournamentSummary>

    /** PREISCRIZIONE (non conferma di partecipazione, vedi PreRegistrationRequest) -
     * richiede un utente autenticato (vedi ClubPreferences.isAuthenticated), socio
     * riconosciuto o meno - vedi TournamentRegistrationService.PreRegisterAsync. */
    @POST("api/tornei/{id}/preiscrivi")
    suspend fun preRegisterForTournament(
        @Path("id") id: Int,
        @Body request: PreRegistrationRequest,
    ): TournamentPreRegistrationResult

    /** Tutti i tornei a cui il chiamante risulta preiscritto - schermata Iscrizioni e
     * segno di spunta in Home. [contactId] (salvato in ClubPreferences dopo una
     * preiscrizione riuscita) è il percorso veloce; idPlayer/email/phoneNumber sono un
     * fallback per risalire al contatto altrimenti. */
    @GET("api/tornei/mie-preiscrizioni")
    suspend fun getMyPreRegistrations(
        @Query("contactId") contactId: Int? = null,
        @Query("idPlayer") idPlayer: Int? = null,
        @Query("email") email: String? = null,
        @Query("phoneNumber") phoneNumber: String? = null,
    ): List<TournamentSummary>

    // ---------- Notifiche push ----------

    @POST("api/devices/register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest)

    @POST("api/devices/messages/{idMessage}/delivered")
    suspend fun reportNotificationDelivered(@Path("idMessage") idMessage: Int, @Body request: ReportDeliveryRequest)

    @POST("api/devices/messages/{idMessage}/read")
    suspend fun reportNotificationRead(@Path("idMessage") idMessage: Int, @Body request: ReportDeliveryRequest)
}
