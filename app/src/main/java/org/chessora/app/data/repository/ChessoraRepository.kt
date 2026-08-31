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
import org.chessora.app.data.remote.dto.NextTournament
import org.chessora.app.data.remote.dto.RankingResponse
import org.chessora.app.data.remote.dto.RegisterDeviceRequest
import org.chessora.app.data.remote.dto.ShopProduct
import org.chessora.app.data.remote.dto.SiteSettings
import org.chessora.app.data.remote.dto.Torneo
import org.chessora.app.data.remote.dto.VideoNewsItem
import org.chessora.app.data.remote.dto.VideoRow

/**
 * Unico punto di accesso alla rete usato dai ViewModel (vedi ui/*\/*ViewModel.kt):
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

    suspend fun resolveClubByCode(code: String): Result<Int> = safeCall { api.resolveClubByCode(code).idClub }

    // ---------- News ----------

    suspend fun getNews(idClub: Int, limit: Int = 30): Result<List<NewsArticle>> =
        safeCall { api.getNews(idClub, limit) }

    suspend fun getNewsComments(idNews: Int): Result<List<NewsComment>> =
        safeCall { api.getNewsComments(idNews) }

    // ---------- Calendario ----------

    suspend fun getEventTypes(): Result<List<EventType>> = safeCall { api.getEventTypes() }

    suspend fun getCalendar(idClub: Int, from: String, to: String): Result<List<CalendarEvent>> =
        safeCall { api.getCalendar(idClub, from, to) }

    // ---------- Tornei ----------

    suspend fun getTornei(idClub: Int): Result<List<Torneo>> = safeCall { api.getTornei(idClub) }

    suspend fun getNextUpcomingTournament(idClub: Int): Result<NextTournament?> =
        safeCall { api.getNextUpcomingTournament(idClub) }

    // ---------- Classifica ----------

    suspend fun getRankingAssoluta(limit: Int = 20): Result<RankingResponse> =
        safeCall { api.getRankingAssoluta(limit) }

    suspend fun getRankingNazionale(limit: Int = 20): Result<RankingResponse> =
        safeCall { api.getRankingNazionale(limit) }

    suspend fun getRankingCircolo(idClub: Int, limit: Int = 20): Result<RankingResponse> =
        safeCall { api.getRankingCircolo(idClub, limit) }

    // ---------- Direttivo ----------

    suspend fun getBoardYears(idClub: Int): Result<List<Int>> = safeCall { api.getBoardYears(idClub) }

    suspend fun getBoard(idClub: Int, year: Int? = null): Result<List<BoardMember>> =
        safeCall { api.getBoard(idClub, year) }

    // ---------- Negozio ----------

    suspend fun getShopProducts(idClub: Int): Result<List<ShopProduct>> = safeCall { api.getShopProducts(idClub) }

    // ---------- Altro ----------

    suspend fun getStats(idClub: Int): Result<ClubStats> = safeCall { api.getStats(idClub) }

    suspend fun getGoogleReviews(idClub: Int): Result<GoogleReviewsResponse> =
        safeCall { api.getGoogleReviews(idClub) }

    suspend fun getVideoRows(idClub: Int): Result<List<VideoRow>> = safeCall { api.getVideoRows(idClub) }

    suspend fun getVideoNews(idClub: Int, limit: Int = 20): Result<List<VideoNewsItem>> =
        safeCall { api.getVideoNews(idClub, limit) }

    suspend fun getSiteSettings(idClub: Int): Result<SiteSettings> = safeCall { api.getSiteSettings(idClub) }

    // ---------- Notifiche push ----------

    suspend fun registerDevice(token: String, idClub: Int?): Result<Unit> =
        safeCall { api.registerDevice(RegisterDeviceRequest(token = token, idClub = idClub)) }
}
