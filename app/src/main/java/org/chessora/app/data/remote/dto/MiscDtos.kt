package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Specchio di Chessora.Contracts.Stats.ClubStatsDto (GET /api/stats). */
@Serializable
data class ClubStats(
    val membersCount: Int,
    val tournamentCount: Int,
)

/** Specchio di Chessora.Contracts.GoogleReviews.GoogleReviewDto. */
@Serializable
data class GoogleReview(
    val author: String,
    val rating: Int,
    val date: String,
    val text: String,
)

/** Specchio di Chessora.Contracts.GoogleReviews.GoogleReviewsResponseDto (GET /api/google-reviews). */
@Serializable
data class GoogleReviewsResponse(
    val averageRating: Double? = null,
    val reviewCount: Int = 0,
    val profileUrl: String? = null,
    val reviews: List<GoogleReview> = emptyList(),
)

/** Specchio di Chessora.Contracts.Video.VideoItemDto (un video dentro una VideoRowDto). */
@Serializable
data class VideoItem(
    val id: Int,
    val title: String,
    val description: String,
    val publishedAt: String? = null,
    val createdAt: String,
    val link: String,
)

/** Specchio di Chessora.Contracts.Video.VideoRowDto (GET /api/video-rows). */
@Serializable
data class VideoRow(
    val id: String,
    val items: List<VideoItem> = emptyList(),
)

/** Specchio di Chessora.Contracts.Video.VideoFeedItemDto (GET /api/video-feed) - un video
 * già filtrato per i canali selezionati in Impostazioni (o tutti, di default) più gli
 * eventuali video locali del circolo. channelDescription è null per un video locale (nessun
 * canale collegato), altrimenti il nome del canale - usato per raggruppare in "cartelle"
 * sotto il campo di ricerca in VideoScreen. */
@Serializable
data class VideoFeedItem(
    val id: Int,
    val title: String,
    val channelDescription: String? = null,
    val publishedAt: String? = null,
    val createdAt: String,
    val link: String,
)

/** Specchio di Chessora.Contracts.Video.VideoChannelPreferenceDto (GET
 * /api/video-channel-preferences) - un canale spuntabile in Impostazioni > Video. */
@Serializable
data class VideoChannelPreference(
    val id: Int,
    val description: String,
    val selected: Boolean,
)

/** Specchio di Chessora.Contracts.Video.SetVideoChannelPreferencesRequest (PUT
 * /api/video-channel-preferences). */
@Serializable
data class SetVideoChannelPreferencesRequest(
    val idPlayer: Int,
    val selectedChannelIds: List<Int>,
)

/** Specchio di Chessora.Contracts.Lichess.LichessSettingsDto (GET
 * /api/lichess-settings) - lichessUsername null se il socio non ha ancora configurato
 * nulla in Impostazioni. */
@Serializable
data class LichessSettings(
    val lichessUsername: String? = null,
)

/** Specchio di Chessora.Contracts.Lichess.SetLichessSettingsRequest (PUT
 * /api/lichess-settings). */
@Serializable
data class SetLichessSettingsRequest(
    val idPlayer: Int,
    val lichessUsername: String?,
)

/** Specchio di Chessora.Contracts.VideoPublishing.VideoNewsItemDto (GET /api/video-news). */
@Serializable
data class VideoNewsItem(
    val id: Int,
    val title: String,
    val description: String,
    val link: String? = null,
    val publishedAt: String? = null,
    val createdAt: String,
)
