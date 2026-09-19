package org.chessora.app.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.chessora.app.data.remote.ChessoraApi
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
import org.chessora.app.data.remote.dto.NetworkNewsItem
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.remote.dto.PerformanceHistoryDto
import org.chessora.app.data.remote.dto.PlayerSearchResult
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ReportDeliveryRequest
import org.chessora.app.data.remote.dto.SendMessageRequest
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.StartClubConversationRequest
import org.chessora.app.data.remote.dto.StartDirectConversationRequest
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.remote.dto.PreRegistrationRequest
import org.chessora.app.data.remote.dto.TournamentPreRegistrationResult
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

    suspend fun preRegisterForTournament(id: Int, request: PreRegistrationRequest): Result<TournamentPreRegistrationResult> =
        safeCall { api.preRegisterForTournament(id, request) }

    suspend fun getMyPreRegistrations(
        contactId: Int? = null,
        idPlayer: Int? = null,
        email: String? = null,
        phoneNumber: String? = null,
    ): Result<List<TournamentSummary>> =
        safeCall { api.getMyPreRegistrations(contactId, idPlayer, email, phoneNumber) }

    suspend fun cancelPreRegistration(
        id: Int,
        idPlayer: Int? = null,
        email: String? = null,
        phoneNumber: String? = null,
    ): Result<TournamentPreRegistrationResult> =
        safeCall { api.cancelPreRegistration(id, idPlayer, email, phoneNumber) }

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

    // ---------- Identificazione socio ----------

    suspend fun identifyPlayer(
        idClub: Int,
        email: String?,
        phoneNumber: String?,
        displayName: String? = null,
    ): Result<IdentifyResultDto> =
        safeCall { api.identifyPlayer(IdentifyRequestDto(idClub, email, phoneNumber, displayName)) }

    /** Per chi ha dichiarato di non essere socio di alcun circolo (vedi
     * ui/onboarding/MembershipQuestionScreen.kt). */
    suspend fun identifyPlayerNational(
        email: String?,
        phoneNumber: String?,
        idFide: String?,
        displayName: String? = null,
    ): Result<IdentifyResultDto> =
        safeCall { api.identifyPlayerNational(IdentifyNationalRequestDto(email, phoneNumber, idFide, displayName)) }

    suspend fun getPlayerPerformance(idPlayer: Int): Result<PerformanceHistoryDto> =
        safeCall { api.getPlayerPerformance(idPlayer) }

    suspend fun getPlayerRoles(idPlayer: Int, club: String): Result<List<String>> =
        safeCall { api.getPlayerRoles(idPlayer, club) }

    /** [jpegBytes] è già compresso/ridimensionato lato client (ui/profile/) prima di
     * arrivare qui - sempre JPEG, cosi' il repository non deve occuparsi di formati. */
    suspend fun setPlayerPhoto(idPlayer: Int, jpegBytes: ByteArray): Result<Unit> = safeCall {
        val body = jpegBytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("photo", "avatar.jpg", body)
        api.setPlayerPhoto(idPlayer, part)
    }

    suspend fun deletePlayerPhoto(idPlayer: Int): Result<Unit> = safeCall { api.deletePlayerPhoto(idPlayer) }

    // ---------- Messaggi ----------

    suspend fun getConversations(idPlayer: Int): Result<List<ConversationSummary>> =
        safeCall { api.getConversations(idPlayer) }

    suspend fun getChessoraThread(idPlayer: Int): Result<List<ChatMessage>> =
        safeCall { api.getChessoraThread(idPlayer) }

    suspend fun getConversationMessages(idConversation: Int, idPlayer: Int): Result<List<ChatMessage>> =
        safeCall { api.getConversationMessages(idConversation, idPlayer) }

    suspend fun startDirectConversation(fromIdPlayer: Int, toIdPlayer: Int, body: String): Result<Int?> =
        safeCall { api.startDirectConversation(StartDirectConversationRequest(fromIdPlayer, toIdPlayer, body)).idConversation }

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
