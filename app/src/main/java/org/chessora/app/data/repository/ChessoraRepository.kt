package org.chessora.app.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.chessora.app.data.local.OrganizerSession
import org.chessora.app.data.remote.ChessoraApi
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
import org.chessora.app.data.remote.dto.EntrantDto
import org.chessora.app.data.remote.dto.EventType
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
import org.chessora.app.data.remote.dto.RegisteredPlayer
import org.chessora.app.data.remote.dto.ResendConfirmationRequestDto
import retrofit2.HttpException
import org.chessora.app.data.remote.dto.NetworkNewsItem
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.remote.dto.PerformanceHistoryDto
import org.chessora.app.data.remote.dto.PlayerSearchResult
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ReportDeliveryRequest
import org.chessora.app.data.remote.dto.SendMessageRequest
import org.chessora.app.data.remote.dto.SetMessagingPlayerSettingsRequest
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.StartClubConversationRequest
import org.chessora.app.data.remote.dto.StartDirectConversationRequest
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.remote.dto.StandingsRow
import org.chessora.app.data.remote.dto.SubmitResultRequestDto
import org.chessora.app.data.remote.dto.TournamentViewMode
import org.chessora.app.data.remote.dto.TournamentViewStateDto
import org.chessora.app.data.remote.dto.TournamentPreRegistrationResult
import org.chessora.app.data.remote.dto.TournamentRound
import org.chessora.app.data.remote.dto.TournamentSummary
import org.chessora.app.data.remote.dto.VideoNewsItem
import org.chessora.app.data.remote.dto.VideoRow

/**
 * Unico punto di accesso alla rete usato dai ViewModel (vedi i vari
 * ui/…/…ViewModel.kt):
 * incapsula [ChessoraApi] dietro `Result<T>` cosi' i ViewModel non maneggiano mai
 * direttamente eccezioni Retrofit/IOException, solo `.onSuccess { }.onFailure { }`.
 * Nessuna cache locale in questa prima versione (vedi README.md "Cosa NON è
 * stato fatto") - ogni chiamata va sempre in rete.
 */
class ChessoraRepository(private val api: ChessoraApi) {

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }

    // ---------- Autenticazione (ui/auth/) ----------

    suspend fun playerLogin(emailOrIdFide: String, password: String): Result<PlayerAuthResponseDto> =
        safeCall { api.playerLogin(PlayerLoginRequestDto(emailOrIdFide, password)) }

    suspend fun loginWithGoogle(idToken: String): Result<GoogleLoginResponseDto> =
        safeCall { api.loginWithGoogle(GoogleLoginRequestDto(idToken)) }

    suspend fun completeProfile(request: CompletePlayerProfileRequestDto): Result<PlayerAuthResponseDto> =
        safeCall { api.completeProfile(request) }

    suspend fun registerWithFide(request: RegisterWithFideRequestDto): Result<RegisterResponseDto> =
        safeCall { api.registerWithFide(request) }

    /** Null (non un errore) se idFide non esiste affatto in anagrafica FIDE (404
     * dal server) - qualunque altro fallimento resta un Result.failure normale. */
    suspend fun checkFide(idFide: Int): Result<CheckFideResultDto?> =
        try {
            Result.success(api.checkFide(idFide))
        } catch (e: HttpException) {
            if (e.code() == 404) Result.success(null) else Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }

    suspend fun searchFide(query: String): Result<List<FidePlayerSearchResultDto>> =
        safeCall { api.searchFide(query) }

    suspend fun getClubRoster(idClub: Int): Result<List<ClubRosterEntryDto>> =
        safeCall { api.getClubRoster(idClub) }

    suspend fun forgotPassword(email: String): Result<Unit> = safeCall { api.forgotPassword(ForgotPasswordRequestDto(email)) }

    suspend fun resendConfirmation(email: String): Result<Unit> = safeCall { api.resendConfirmation(ResendConfirmationRequestDto(email)) }

    /** [jpegBytes] è già compresso/ridimensionato lato client (ui/profile/) prima di
     * arrivare qui - sempre JPEG, cosi' il repository non deve occuparsi di formati. */
    suspend fun setMyPhoto(jpegBytes: ByteArray): Result<Unit> = safeCall {
        val body = jpegBytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("photo", "avatar.jpg", body)
        api.setMyPhoto(part)
    }

    suspend fun deleteMyPhoto(): Result<Unit> = safeCall { api.deleteMyPhoto() }

    // ---------- Circoli ----------

    suspend fun getClubDirectory(): Result<List<ClubDirectoryItem>> = safeCall { api.getClubDirectory() }

    /** Usato SOLO per POST /api/devices/register, il solo endpoint pubblico che
     * richiede ancora il numero interno (nel body JSON, non in query string -
     * vedi ChessoraRepository.registerDevice e push/DeviceRegistration.kt). Ogni
     * altra chiamata di questo repository usa direttamente il publicCode. */
    suspend fun resolveClubByCode(code: String): Result<Int> = safeCall { api.resolveClubByCode(code).idClub }

    // ---------- News ----------

    suspend fun getNews(club: String, limit: Int = 30, search: String? = null): Result<List<NewsArticle>> =
        safeCall { api.getNews(club, limit, search) }

    /** "Modalità piattaforma" (nessun circolo scelto, vedi
     * ui/onboarding/MembershipQuestionScreen.kt). */
    suspend fun getNewsAllClubs(limit: Int = 30, search: String? = null): Result<List<NewsArticle>> =
        safeCall { api.getNewsAllClubs(limit, search) }

    suspend fun getNewsComments(idNews: Int): Result<List<NewsComment>> =
        safeCall { api.getNewsComments(idNews) }

    /** Filtro "Mondo" della schermata News - notizie FIDE/globali, non di circolo. */
    suspend fun getNetworkNews(limit: Int = 30, search: String? = null): Result<List<NetworkNewsItem>> =
        safeCall { api.getNetworkNews(limit, search) }

    // ---------- Calendario ----------

    suspend fun getEventTypes(): Result<List<EventType>> = safeCall { api.getEventTypes() }

    suspend fun getCalendar(club: String, from: String, to: String): Result<List<CalendarEvent>> =
        safeCall { api.getCalendar(club, from, to) }

    /** Solo tornei, su tutti i circoli attivi - Home in "modalità piattaforma". */
    suspend fun getCalendarAllClubs(from: String, to: String): Result<List<CalendarEvent>> =
        safeCall { api.getCalendarAllClubs(from, to) }

    suspend fun getEventoBando(id: Int, club: String): Result<EventoBandoInfo> =
        safeCall { api.getEventoBando(id, club) }

    // ---------- Tornei (dettaglio/iscrizione) ----------

    suspend fun getTornei(): Result<List<TournamentSummary>> = safeCall { api.getTornei() }

    /** Richiede login (vedi ui/auth/) - l'identità è quella dell'utente autenticato. */
    suspend fun preRegisterForTournament(id: Int): Result<TournamentPreRegistrationResult> =
        safeCall { api.preRegisterForTournament(id) }

    suspend fun getMyPreRegistrations(): Result<List<TournamentSummary>> =
        safeCall { api.getMyPreRegistrations() }

    suspend fun cancelPreRegistration(id: Int): Result<TournamentPreRegistrationResult> =
        safeCall { api.cancelPreRegistration(id) }

    suspend fun getRegisteredPlayers(id: Int): Result<List<RegisteredPlayer>> =
        safeCall { api.getRegisteredPlayers(id) }

    suspend fun getTournamentRounds(id: Int): Result<List<TournamentRound>> =
        safeCall { api.getTournamentRounds(id) }

    suspend fun getTournamentStandings(id: Int): Result<List<StandingsRow>> =
        safeCall { api.getTournamentStandings(id) }

    suspend fun getPollingIntervalMs(id: Int): Result<Int> =
        safeCall { api.getPollingIntervalMs(id) }

    // ---------- Gestione tornei (organizzatore, ui/tournamentmanager/) ----------
    // Vedi OrganizerSession/il commento su ChessoraApi: un secondo token, mai il Bearer
    // giocatore, ottenuto on-demand con la claim self-service (come gestione-tornei.html
    // sul sito) e riusato finché resta valido.

    /** Ottiene il token organizzatore da OrganizerSession, richiedendolo con la claim se
     * assente o se [forceRefresh] (dopo un 401/403 sul token precedente, vedi
     * [withOrganizerAuth]). */
    private suspend fun ensureOrganizerAuth(forceRefresh: Boolean = false): String {
        val cached = OrganizerSession.token
        if (!forceRefresh && cached != null) return "Bearer $cached"
        val claimed = api.claimTournOrganizer().token
        OrganizerSession.token = claimed
        return "Bearer $claimed"
    }

    /** Esegue [block] col token organizzatore corrente; se risponde 401/403 (token
     * scaduto/revocato) ri-effettua la claim UNA sola volta e ritenta, prima di
     * propagare l'errore - mai un logout globale, quello riguarda solo il token
     * giocatore (vedi NetworkModule). */
    private suspend fun <T> withOrganizerAuth(block: suspend (String) -> T): Result<T> = safeCall {
        val auth = ensureOrganizerAuth()
        try {
            block(auth)
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) block(ensureOrganizerAuth(forceRefresh = true)) else throw e
        }
    }

    suspend fun getOrganizedTournaments(): Result<List<TournamentSummary>> =
        withOrganizerAuth { auth -> api.getOrganizedTournaments(auth) }

    suspend fun getOrganizerRounds(id: Int): Result<List<TournamentRound>> =
        withOrganizerAuth { auth -> api.getOrganizerRounds(auth, id) }

    suspend fun getEntrants(id: Int): Result<List<EntrantDto>> =
        withOrganizerAuth { auth -> api.getEntrants(auth, id) }

    suspend fun generateRound(id: Int): Result<TournamentRound> =
        withOrganizerAuth { auth -> api.generateRound(auth, id) }

    suspend fun publishRound(id: Int, roundId: Int): Result<Unit> =
        withOrganizerAuth { auth -> api.publishRound(auth, id, roundId) }

    suspend fun submitOrganizerResult(id: Int, pairingId: Int, result: String?): Result<Unit> =
        withOrganizerAuth { auth -> api.submitOrganizerResult(auth, id, pairingId, SubmitResultRequestDto(result)) }

    suspend fun getTournamentViewState(id: Int): Result<TournamentViewMode> =
        withOrganizerAuth { auth -> TournamentViewMode.fromWire(api.getTournamentViewState(auth, id).view) }

    suspend fun setTournamentViewState(id: Int, mode: TournamentViewMode): Result<Unit> =
        withOrganizerAuth { auth -> api.setTournamentViewState(auth, id, TournamentViewStateDto(mode.wireValue)) }

    // ---------- Classifica ----------

    suspend fun getRankingAssoluta(limit: Int = 20): Result<RankingResponse> =
        safeCall { api.getRankingAssoluta(limit) }

    suspend fun getRankingNazionale(limit: Int = 20): Result<RankingResponse> =
        safeCall { api.getRankingNazionale(limit) }

    suspend fun getRankingCircolo(club: String, limit: Int = 20): Result<RankingResponse> =
        safeCall { api.getRankingCircolo(club, limit) }

    // ---------- Direttivo ----------

    suspend fun getBoardYears(club: String): Result<List<Int>> = safeCall { api.getBoardYears(club) }

    suspend fun getBoard(club: String, year: Int? = null): Result<List<BoardMember>> =
        safeCall { api.getBoard(club, year) }

    // ---------- Negozio ----------

    suspend fun getShopProducts(club: String): Result<List<ShopProduct>> = safeCall { api.getShopProducts(club) }

    // ---------- Altro ----------

    suspend fun getStats(club: String): Result<ClubStats> = safeCall { api.getStats(club) }

    suspend fun getGoogleReviews(club: String): Result<GoogleReviewsResponse> =
        safeCall { api.getGoogleReviews(club) }

    suspend fun getVideoRows(club: String): Result<List<VideoRow>> = safeCall { api.getVideoRows(club) }

    suspend fun getVideoNews(club: String, limit: Int = 20): Result<List<VideoNewsItem>> =
        safeCall { api.getVideoNews(club, limit) }

    suspend fun getSiteSettings(club: String): Result<SiteSettings> = safeCall { api.getSiteSettings(club) }

    // ---------- Dati pubblici di un giocatore ----------

    suspend fun getPlayerPerformance(idPlayer: Int): Result<PerformanceHistoryDto> =
        safeCall { api.getPlayerPerformance(idPlayer) }

    suspend fun getPlayerRoles(idPlayer: Int, club: String): Result<List<String>> =
        safeCall { api.getPlayerRoles(idPlayer, club) }

    // ---------- Messaggi ----------

    suspend fun getConversations(idPlayer: Int): Result<List<ConversationSummary>> =
        safeCall { api.getConversations(idPlayer) }

    suspend fun getChessoraThread(idPlayer: Int): Result<List<ChatMessage>> =
        safeCall { api.getChessoraThread(idPlayer) }

    suspend fun getConversationMessages(idConversation: Int, idPlayer: Int): Result<List<ChatMessage>> =
        safeCall { api.getConversationMessages(idConversation, idPlayer) }

    suspend fun startDirectConversation(fromIdPlayer: Int, toIdPlayer: Int, body: String): Result<Int?> =
        safeCall { api.startDirectConversation(StartDirectConversationRequest(fromIdPlayer, toIdPlayer, body)).idConversation }

    suspend fun findDirectConversation(fromIdPlayer: Int, toIdPlayer: Int): Result<Int?> =
        safeCall { api.findDirectConversation(fromIdPlayer, toIdPlayer).idConversation }

    suspend fun startClubConversation(fromIdPlayer: Int, idClub: Int, body: String): Result<Int?> =
        safeCall { api.startClubConversation(StartClubConversationRequest(fromIdPlayer, idClub, body)).idConversation }

    suspend fun sendMessage(idConversation: Int, fromIdPlayer: Int, body: String): Result<ChatMessage> =
        safeCall { api.sendMessage(idConversation, SendMessageRequest(fromIdPlayer, body)) }

    suspend fun markConversationRead(idConversation: Int, idPlayer: Int): Result<Unit> =
        safeCall { api.markConversationRead(idConversation, MarkReadRequest(idPlayer)) }

    suspend fun searchMessagingPlayers(query: String, idPlayer: Int): Result<List<PlayerSearchResult>> =
        safeCall { api.searchMessagingPlayers(query, idPlayer) }

    suspend fun searchMessagingClubs(query: String): Result<List<ClubSearchResult>> =
        safeCall { api.searchMessagingClubs(query) }

    suspend fun getMessagingSettings(idPlayer: Int): Result<MessagingPlayerSettings> =
        safeCall { api.getMessagingSettings(idPlayer) }

    suspend fun setMessagingSettings(idPlayer: Int, hideDeliveryAndReadStatus: Boolean): Result<Unit> =
        safeCall { api.setMessagingSettings(SetMessagingPlayerSettingsRequest(idPlayer, hideDeliveryAndReadStatus)) }

    // ---------- Notifiche push ----------

    suspend fun registerDevice(
        token: String,
        idClub: Int?,
        idPlayer: Int? = null,
        appVersionName: String? = null,
        appVersionCode: Int? = null,
    ): Result<Unit> =
        safeCall {
            api.registerDevice(
                RegisterDeviceRequest(
                    token = token, idClub = idClub, idPlayer = idPlayer,
                    appVersionName = appVersionName, appVersionCode = appVersionCode,
                ),
            )
        }

    suspend fun reportNotificationDelivered(idMessage: Int, token: String): Result<Unit> =
        safeCall { api.reportNotificationDelivered(idMessage, ReportDeliveryRequest(token)) }

    suspend fun reportNotificationRead(idMessage: Int, token: String): Result<Unit> =
        safeCall { api.reportNotificationRead(idMessage, ReportDeliveryRequest(token)) }
}
