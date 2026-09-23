package org.chessora.app.data.remote

import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.remote.dto.ChatMessage
import org.chessora.app.data.remote.dto.CheckFideResultDto
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.remote.dto.ClubRosterEntryDto
import org.chessora.app.data.remote.dto.ClubSearchResult
import org.chessora.app.data.remote.dto.ClubStats
import org.chessora.app.data.remote.dto.CompletePlayerProfileRequestDto
import org.chessora.app.data.remote.dto.ConversationSummary
import org.chessora.app.data.remote.dto.EventType
import org.chessora.app.data.remote.dto.EmptyRequestBody
import org.chessora.app.data.remote.dto.EventoBandoInfo
import org.chessora.app.data.remote.dto.FidePlayerSearchResultDto
import org.chessora.app.data.remote.dto.ForgotPasswordRequestDto
import org.chessora.app.data.remote.dto.GoogleLoginRequestDto
import org.chessora.app.data.remote.dto.GoogleLoginResponseDto
import org.chessora.app.data.remote.dto.GoogleReviewsResponse
import org.chessora.app.data.remote.dto.MarkReadRequest
import org.chessora.app.data.remote.dto.MessagingPlayerSettings
import org.chessora.app.data.remote.dto.PlayerAuthResponseDto
import org.chessora.app.data.remote.dto.PlayerLoginRequestDto
import org.chessora.app.data.remote.dto.RegisterResponseDto
import org.chessora.app.data.remote.dto.RegisterWithFideRequestDto
import org.chessora.app.data.remote.dto.ResendConfirmationRequestDto
import org.chessora.app.data.remote.dto.NetworkNewsItem
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.PerformanceHistoryDto
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.remote.dto.PlayerSearchResult
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.SendMessageRequest
import org.chessora.app.data.remote.dto.StartClubConversationRequest
import org.chessora.app.data.remote.dto.SetMessagingPlayerSettingsRequest
import org.chessora.app.data.remote.dto.StartConversationResult
import org.chessora.app.data.remote.dto.StartDirectConversationRequest
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ReportDeliveryRequest
import org.chessora.app.data.remote.dto.ResolveClubResponse
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.remote.dto.TournamentPreRegistrationResult
import org.chessora.app.data.remote.dto.RegisteredPlayer
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
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Specchio 1:1 di docs/android-app-spec.md §6 (repository server Chessora,
 * https://api.chessora.org). La maggior parte degli endpoint qui sotto resta
 * pubblica/senza autenticazione, MA l'app ora HA un vero login (Google/ID FIDE/
 * email+password, vedi ui/auth/) - gli endpoint sotto "api/auth" e "api/me" (vedi
 * sezione dedicata) richiedono un Bearer token, allegato automaticamente
 * dall'interceptor in NetworkModule quando presente (vedi data/local/AuthSession.kt).
 * Se un endpoint nuovo viene aggiunto lato server, questa è la prima cosa da
 * estendere - dopodiché il DTO corrispondente in data/remote/dto/ e infine il
 * punto di chiamata in data/repository/ChessoraRepository.kt.
 *
 * NON aggiungere qui endpoint che richiedono autenticazione ADMIN (tutto ciò che
 * sta sotto un percorso "…/admin" oppure "api/platform/…") - quelli restano fuori
 * scope, distinti dal login giocatori sopra (vedi PlayerAuthService lato server).
 */
interface ChessoraApi {

    // ---------- Autenticazione (ui/auth/) ----------

    /** Login con email o ID FIDE (numerico, come stringa) + password. */
    @POST("api/auth/player-login")
    suspend fun playerLogin(@Body request: PlayerLoginRequestDto): PlayerAuthResponseDto

    /** Verifica il Google ID token (non più uno scambio con Firebase) e
     * recupera/crea SUBITO l'account - auth.profileComplete=false segnala di
     * proseguire con completeProfile prima di considerarsi loggato del tutto. */
    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequestDto): GoogleLoginResponseDto

    /** Secondo passo dopo un login Google con profilo incompleto - stesso account
     * già autenticato dal passo precedente (Bearer già valido). */
    @POST("api/me/complete-profile")
    suspend fun completeProfile(@Body request: CompletePlayerProfileRequestDto): PlayerAuthResponseDto

    /** ID FIDE valido ma non ancora associato a nessun utente - crea l'account,
     * richiede conferma email prima di poter accedere. */
    @POST("api/auth/register-fide")
    suspend fun registerWithFide(@Body request: RegisterWithFideRequestDto): RegisterResponseDto

    /** Null implicito (404) se idFide non esiste affatto in anagrafica FIDE - vedi
     * ChessoraRepository.checkFide, che lo traduce in Result<CheckFideResultDto?>. */
    @GET("api/auth/check-fide")
    suspend fun checkFide(@Query("idFide") idFide: Int): CheckFideResultDto

    /** Un solo campo di ricerca su anag.FidePlayers.Name - mai auto-selezione, il
     * client mostra sempre l'elenco per conferma esplicita. */
    @GET("api/auth/search-fide")
    suspend fun searchFide(@Query("q") query: String): List<FidePlayerSearchResultDto>

    /** Elenco soci di un circolo (selezione manuale del proprio nominativo quando
     * il club non è auto-risolvibile da orga.PlayerCard). */
    @GET("api/auth/club-roster")
    suspend fun getClubRoster(@Query("idClub") idClub: Int): List<ClubRosterEntryDto>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequestDto)

    @POST("api/auth/resend-confirmation")
    suspend fun resendConfirmation(@Body request: ResendConfirmationRequestDto)

    /** Foto dell'utente autenticato (sostituisce le vecchie {idPlayer}/photo
     * scrivibili da chiunque) - la sola LETTURA di una foto (anche la propria)
     * resta pubblica su GET api/players/{idPlayer}/photo, vedi sotto. */
    @Multipart
    @POST("api/me/photo")
    suspend fun setMyPhoto(@Part photo: MultipartBody.Part)

    @DELETE("api/me/photo")
    suspend fun deleteMyPhoto()

    // ---------- Circoli ----------

    @GET("api/clubs/directory")
    suspend fun getClubDirectory(): List<ClubDirectoryItem>

    @GET("api/clubs/resolve")
    suspend fun resolveClubByCode(@Query("code") code: String): ResolveClubResponse

    // ---------- News ----------

    @GET("api/news")
    suspend fun getNews(@Query("club") club: String, @Query("limit") limit: Int = 30, @Query("search") search: String? = null): List<NewsArticle>

    /** Ultime [limit] news su tutti i circoli attivi - scheda News in "modalità
     * piattaforma" (nessun circolo scelto, vedi ui/onboarding/MembershipQuestionScreen.kt). */
    @GET("api/news/all-clubs")
    suspend fun getNewsAllClubs(@Query("limit") limit: Int = 30, @Query("search") search: String? = null): List<NewsArticle>

    /** Notizie FIDE/globali (filtro "Mondo") - separate dalle news di circolo sopra. */
    @GET("api/network-news")
    suspend fun getNetworkNews(@Query("limit") limit: Int = 30, @Query("search") search: String? = null): List<NetworkNewsItem>

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

    // ---------- Dati pubblici di un giocatore (per idPlayer) ----------

    /** Storico Elo per il grafico "Le mie performance" (ui/performance/). */
    @GET("api/players/{idPlayer}/performance")
    suspend fun getPlayerPerformance(@Path("idPlayer") idPlayer: Int): PerformanceHistoryDto

    // Nota: GET api/players/{idPlayer}/photo (lettura pubblica) non compare qui: è
    // consumata direttamente come URL immagine da Coil (vedi NetworkModule.resolveAssetUrl),
    // mai tramite una chiamata Retrofit - solo la SCRITTURA (api/me/photo sopra) passa da qui.

    /** Ruoli organizzativi pubblici del socio in questo circolo (es. "Responsabile dei
     * tornei") - usato per mostrare l'icona "Gestione tornei" in ui/session/SessionViewModel.kt,
     * nessun login richiesto (stesso livello di fiducia di /identify). */
    @GET("api/players/{idPlayer}/roles")
    suspend fun getPlayerRoles(@Path("idPlayer") idPlayer: Int, @Query("club") club: String): List<String>

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

    /** Sola lettura (nessuna creazione) - idConversation è null se non esiste ancora
     * nessuna conversazione diretta tra i due soci. */
    @GET("api/messaging/conversations/direct")
    suspend fun findDirectConversation(@Query("fromIdPlayer") fromIdPlayer: Int, @Query("toIdPlayer") toIdPlayer: Int): StartConversationResult

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

    @GET("api/messaging/settings")
    suspend fun getMessagingSettings(@Query("idPlayer") idPlayer: Int): MessagingPlayerSettings

    @PUT("api/messaging/settings")
    suspend fun setMessagingSettings(@Body request: SetMessagingPlayerSettingsRequest)

    // ---------- Tornei (dettaglio/iscrizione dalla Home, ui/tournaments/) ----------
    // Pubblici come tutto il resto: orga.TournamentRegistrations non ha una colonna
    // IdClub, quindi l'elenco è per costruzione già "tutti i circoli" - lo stesso
    // endpoint usato da tourn.chessora.org.

    @GET("api/tornei")
    suspend fun getTornei(): List<TournamentSummary>

    /** PREISCRIZIONE (non conferma di partecipazione) - richiede login (vedi ui/auth/),
     * l'identità è quella dell'utente autenticato (Bearer), mai passata nel corpo. Il
     * corpo vuoto è comunque necessario: IIS in produzione rifiuta con 411 "Length
     * Required" una POST senza alcun body/Content-Length (vedi EmptyRequestBody). */
    @POST("api/tornei/{id}/preiscrivi")
    suspend fun preRegisterForTournament(@Path("id") id: Int, @Body body: EmptyRequestBody = EmptyRequestBody()): TournamentPreRegistrationResult

    /** Tutti i tornei a cui l'utente autenticato risulta preiscritto - schermata
     * Iscrizioni e segno di spunta in Home. */
    @GET("api/tornei/mie-preiscrizioni")
    suspend fun getMyPreRegistrations(): List<TournamentSummary>

    /** Ritira una preiscrizione - stessa identità (Bearer) di preRegisterForTournament.
     * Serve anche per poter scegliere un altro torneo "fratello" dello stesso evento,
     * dato che se ne può scegliere uno solo per volta. */
    @DELETE("api/tornei/{id}/preiscrivi")
    suspend fun cancelPreRegistration(@Path("id") id: Int): TournamentPreRegistrationResult

    /** Elenco pubblico dei preiscritti a un torneo (nessuna autenticazione richiesta) -
     * stesso endpoint usato dalla lista iscritti su tourn.chessora.org. */
    @GET("api/tornei/{id}/iscritti")
    suspend fun getRegisteredPlayers(@Path("id") id: Int): List<RegisteredPlayer>

    // ---------- Notifiche push ----------

    @POST("api/devices/register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest)

    @POST("api/devices/messages/{idMessage}/delivered")
    suspend fun reportNotificationDelivered(@Path("idMessage") idMessage: Int, @Body request: ReportDeliveryRequest)

    @POST("api/devices/messages/{idMessage}/read")
    suspend fun reportNotificationRead(@Path("idMessage") idMessage: Int, @Body request: ReportDeliveryRequest)
}
