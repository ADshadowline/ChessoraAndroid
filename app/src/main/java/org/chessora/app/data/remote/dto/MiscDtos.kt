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
