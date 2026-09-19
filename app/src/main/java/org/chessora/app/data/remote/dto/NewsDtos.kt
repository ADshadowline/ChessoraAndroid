package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Specchio di Chessora.Contracts.News.NewsArticleDto (GET /api/news). Le date
 * arrivano dal server come stringhe ISO-8601 (System.Text.Json di default):
 * teniamole come String grezze nel DTO e le formattiamo solo nella UI (vedi
 * ui/common/DateFormatting.kt), invece di aggiungere una dipendenza data/ora
 * solo per il parsing qui.
 */
@Serializable
data class NewsArticle(
    val id: Int,
    val title: String,
    val body: String,
    val excerpt: String,
    val image: String? = null,
    val author: String,
    val publishedAt: String? = null,
    val createdAt: String,
    val commentsCount: Int = 0,
)

/** Specchio di Chessora.Contracts.News.NewsCommentDto (GET /api/news/{id}/comments). */
@Serializable
data class NewsComment(
    val id: Int,
    val author: String,
    val text: String,
    val createdAt: String,
)

/** Specchio di Chessora.Contracts.NetworkNews.NetworkNewsItemDto (GET
 * /api/network-news) - notizie FIDE/globali, non di un circolo specifico (filtro
 * "Mondo" della schermata News). A differenza di [NewsArticle] non ha un corpo articolo
 * proprio: [link] punta al contenuto originale esterno, aperto fuori dall'app. */
@Serializable
data class NetworkNewsItem(
    val id: Int,
    val title: String,
    val createdAt: String,
    val link: String? = null,
    val source: String,
)
