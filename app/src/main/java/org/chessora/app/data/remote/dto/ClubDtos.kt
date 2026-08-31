package org.chessora.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Una riga di GET /api/clubs/directory (elenco nazionale di tutti i circoli,
 * usato dalla schermata di onboarding per la ricerca). Specchio 1:1 di
 * Chessora.Contracts.Clubs.ClubDirectoryDto nel repository server.
 *
 * [onChessora] indica se il circolo ha davvero un sito Chessora configurato
 * (JsonSiteConfiguration non vuoto) - i circoli con onChessora=false esistono
 * comunque nell'anagrafica nazionale ma non hanno contenuti da consultare
 * nell'app (niente news/tornei/ecc.): la UI di onboarding dovrebbe evidenziarli
 * o filtrarli, vedi ui/onboarding/OnboardingViewModel.kt.
 */
@Serializable
data class ClubDirectoryItem(
    val idClub: Int,
    val name: String,
    val president: String? = null,
    val address: String? = null,
    val cap: String? = null,
    val cityName: String? = null,
    val provinceName: String? = null,
    val provinceCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val site: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val onChessora: Boolean = false,
    val memberCount: Int = 0,
)

/** Risposta di GET /api/clubs/resolve?code=XXX. */
@Serializable
data class ResolveClubResponse(
    val idClub: Int,
)
