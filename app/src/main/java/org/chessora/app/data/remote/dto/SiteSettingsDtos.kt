package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * GET /api/site-settings?club= restituisce un blob JSON libero, non tipizzato
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
    val contact: ClubContactInfo = ClubContactInfo(),
    val statuto: ClubStatutoInfo = ClubStatutoInfo(),
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

/** Sezione "contact" del blob - vedi ui/club/ClubContactsScreen.kt/ClubLocationScreen.kt.
 * mapsUrl è un link Maps già pronto impostato dal circolo nel wizard (può restare vuoto:
 * in quel caso la posizione si ricava da address, vedi ClubLocationScreen). */
@Serializable
data class ClubContactInfo(
    val address: String = "",
    val cityLine: String = "",
    val mapsUrl: String = "",
    val phone: String = "",
    val email: String = "",
    val social: ClubSocialLinks = ClubSocialLinks(),
)

@Serializable
data class ClubSocialLinks(
    val facebook: String = "",
    val instagram: String = "",
    val youtube: String = "",
)

/** Sezione "statuto" del blob - path vuoto/nullo finché il circolo non ne carica uno dal
 * wizard (vedi ui/club/ClubScreen.kt, che in quel caso mostra un messaggio invece di
 * aprire il visualizzatore PDF). */
@Serializable
data class ClubStatutoInfo(
    val path: String? = null,
    val nomeFile: String? = null,
)
