package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * GET /api/site-settings?idClub= restituisce un blob JSON libero, non tipizzato
 * lato server (lo stesso usato dal wizard web - vedi
 * docs/android-app-spec.md §4 nel repository server: "non è tipizzato lato
 * server, è un blob JSON libero"). Qui modelliamo SOLO i campi che servono
 * all'app per adattare nome/logo (vedi ui/theme/Theme.kt e
 * data/repository/ChessoraRepository.kt) - il resto della struttura viene
 * ignorato grazie a NetworkModule.kt che configura
 * `ignoreUnknownKeys = true` sul parser Json. Se in futuro serve un altro
 * campo di questo blob, aggiungilo qui senza preoccuparti di modellare
 * l'intero schema.
 */
@Serializable
data class SiteSettings(
    val site: SiteBranding = SiteBranding(),
    val theme: SiteThemeInfo = SiteThemeInfo(),
)

@Serializable
data class SiteBranding(
    val namePrefix: String = "",
    val nameHighlight: String = "",
    val logoImage: String? = null,
)

@Serializable
data class SiteThemeInfo(
    val stylesheet: String? = null,
)
