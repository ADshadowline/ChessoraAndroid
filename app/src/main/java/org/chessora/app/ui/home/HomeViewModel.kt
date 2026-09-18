package org.chessora.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.CalendarEvent
import org.chessora.app.data.repository.ChessoraRepository
import org.chessora.app.ui.common.UiState
import org.chessora.app.ui.common.toUiState

/** Un torneo/evento del calendario già raggruppato sui suoi eventuali più giorni di
 * gioco (che restano un dettaglio interno, mai mostrati come voci separate qui - vedi
 * HomeViewModel.groupByEvent): [startDateTime] è la prima occorrenza (usata per
 * ordinamento e per il conto alla rovescia del primo elemento), [endDateTime] l'ultima. */
data class UpcomingEvent(
    val key: String,
    val title: String,
    val startDateTime: String,
    val endDateTime: String,
    val startTime: String,
    /** Id di un torneo rappresentativo del gruppo (un evento con più tornei
     * "fratelli" ne ha comunque uno solo qui) - usato solo per aprire il dettaglio,
     * che risale da sé agli altri fratelli tramite eventGroupId. */
    val idTournament: Int?,
    /** Tutti gli id torneo di questo evento (un solo elemento se non ha fratelli) -
     * usato per il segno di spunta: "preiscritto" vale se lo si è a QUALSIASI
     * torneo del gruppo, non solo a quello rappresentativo. */
    val tournamentIds: Set<Int>,
    val idEvento: Int?,
    val tournamentBandoPath: String?,
)

data class HomeData(
    val upcoming: List<UpcomingEvent>,
    /** idTournament dei tornei a cui il socio identificato risulta già iscritto -
     * mostra il segno di spunta sulla card, vuoto se non identificato. */
    val registeredTournamentIds: Set<Int>,
)

/** Schermata Home: prossimi appuntamenti dal calendario, in ordine cronologico
 * (non più le news, che restano comunque raggiungibili dalla propria scheda). Ogni
 * torneo/evento multi-giorno compare come UNA sola voce (raggruppata per
 * idTournament/idEvento), non una per ciascun giorno di gioco. */
class HomeViewModel(
    private val repository: ChessoraRepository,
    private val clubPreferences: ClubPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    private var loadedForClub: String? = null
    private var hasLoadedOnce = false

    /** [club] null = "modalità piattaforma" (nessun circolo scelto): mostra i tornei
     * di TUTTI i circoli invece che di uno solo. */
    fun load(club: String?) {
        if (hasLoadedOnce && loadedForClub == club && _state.value is UiState.Success) return
        loadedForClub = club
        hasLoadedOnce = true
        fetch(club)
    }

    fun retry(club: String?) = fetch(club)

    /** Id dei tornei a cui il chiamante risulta preiscritto - per il segno di spunta in
     * Home. Funziona sia per un socio riconosciuto (idPlayer) sia per un utente
     * autenticato ma non ancora socio (email/telefono verificati, vedi
     * ClubPreferences.isAuthenticated) - nessuna delle due condizione presente equivale a
     * "non può essersi preiscritto a nulla", niente chiamata di rete. */
    private suspend fun myPreRegisteredTournamentIds(): Set<Int> {
        val contactId = clubPreferences.preRegistrationContactId.first()
        val idPlayer = clubPreferences.identifiedPlayerId.first()
        val email = clubPreferences.authenticatedEmail.first()
        val phone = clubPreferences.authenticatedPhone.first()
        if (contactId == null && idPlayer == null && email == null && phone == null) return emptySet()
        return repository.getMyPreRegistrations(contactId, idPlayer, email, phone)
            .getOrNull()?.map { it.id }?.toSet() ?: emptySet()
    }

    private fun fetch(club: String?) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
            val today = LocalDate.now()
            val from = today.format(isoDate)
            val to = today.plusDays(WINDOW_DAYS).format(isoDate)
            val registeredIds = myPreRegisteredTournamentIds()
            val result = if (club != null) repository.getCalendar(club, from, to) else repository.getCalendarAllClubs(from, to)
            _state.value = result
                .map { events ->
                    val now = LocalDateTime.now()
                    HomeData(
                        upcoming = groupByEvent(events.filter { isUpcoming(it, now) })
                            .sortedBy { it.startDateTime }
                            .take(MAX_ITEMS),
                        registeredTournamentIds = registeredIds,
                    )
                }
                .toUiState()
        }
    }

    /** Una riga di calendario per ogni giorno di gioco di ciascun torneo: qui si
     * raggruppano in una sola voce sia i giorni dello stesso torneo sia, quando un
     * evento ha più tornei "fratelli" (stesso tournamentEventGroupId, es. "Open A"/
     * "Open B" o le categorie di un CIS), le righe di TUTTI i fratelli - altrimenti
     * ciascuno produrrebbe la propria card duplicata con lo stesso titolo (Titolo =
     * EventoNome quando ci sono fratelli, vedi TournamentCalendarPlanner lato
     * server). Gli eventi non-torneo restano raggruppati per idEvento. */
    private fun groupByEvent(events: List<CalendarEvent>): List<UpcomingEvent> =
        events
            .groupBy {
                it.tournamentEventGroupId?.let { g -> "g$g" }
                    ?: it.idTournament?.let { t -> "t$t" }
                    ?: it.idEvento?.let { e -> "e$e" }
                    ?: "c${it.id}"
            }
            .map { (key, rows) ->
                val sorted = rows.sortedBy { it.eventDateTime }
                val first = sorted.first()
                UpcomingEvent(
                    key = key,
                    title = first.title ?: first.eventTypeDescription ?: "Evento",
                    startDateTime = first.eventDateTime,
                    endDateTime = sorted.last().eventDateTime,
                    startTime = first.startTime,
                    idTournament = first.idTournament,
                    tournamentIds = rows.mapNotNull { it.idTournament }.toSet(),
                    idEvento = first.idEvento,
                    tournamentBandoPath = first.tournamentBandoPath,
                )
            }

    private fun isUpcoming(event: CalendarEvent, now: LocalDateTime): Boolean =
        runCatching { LocalDateTime.parse(event.eventDateTime) }.getOrNull()?.isAfter(now) == true

    /** Url assoluto del bando dell'appuntamento, se ne ha uno - solo per gli EVENTI
     * non-torneo (idTournament null): un torneo apre sempre il proprio dettaglio
     * (TournamentDetailScreen), mai direttamente il bando. */
    suspend fun resolveBandoUrl(event: UpcomingEvent, club: String?): String? {
        event.tournamentBandoPath?.let { return NetworkModule.resolveAssetUrl(it) }
        val idEvento = event.idEvento ?: return null
        val bandoPath = club?.let { repository.getEventoBando(idEvento, it).getOrNull()?.bandoPath } ?: return null
        return NetworkModule.resolveAssetUrl(bandoPath)
    }

    private companion object {
        const val WINDOW_DAYS = 60L
        const val MAX_ITEMS = 40
    }
}
