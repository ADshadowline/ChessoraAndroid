package org.chessora.app.data.repository

import org.chessora.app.data.remote.ChessoraApi
import org.chessora.app.data.remote.dto.BoardMember
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.remote.dto.ClubDirectoryItem
import org.chessora.app.data.remote.dto.ClubStats
import org.chessora.app.data.remote.dto.EventType
import org.chessora.app.data.remote.dto.GoogleReviewsResponse
import org.chessora.app.data.remote.dto.NewsArticle
import org.chessora.app.data.remote.dto.NewsComment
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.SiteSettings
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

    suspend fun getNews(club: String, limit: Int = 30): Result<List<NewsArticle>> =
        safeCall { api.getNews(club, limit) }

    suspend fun getNewsComments(idNews: Int): Result<List<NewsComment>> =
        safeCall { api.getNewsComments(idNews) }

    // ---------- Calendario ----------

    suspend fun getEventTypes(): Result<List<EventType>> = safeCall { api.getEventTypes() }

    suspend fun getCalendar(club: String, from: String, to: String): Result<List<CalendarEvent>> =
        safeCall { api.getCalendar(club, from, to) }

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

    // ---------- Notifiche push ----------

    suspend fun registerDevice(token: String, idClub: Int?): Result<Unit> =
        safeCall { api.registerDevice(RegisterDeviceRequest(token = token, idClub = idClub)) }
}
