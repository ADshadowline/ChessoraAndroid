package org.chessora.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import org.chessora.app.R
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.SiteBranding
import org.chessora.app.ui.board.BoardScreen
import org.chessora.app.ui.calendar.CalendarScreen
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.home.HomeScreen
import org.chessora.app.ui.identity.IdentityScreen
import org.chessora.app.ui.messaging.ConversationScreen
import org.chessora.app.ui.messaging.MessagingListScreen
import org.chessora.app.ui.messaging.NewMessageScreen
import org.chessora.app.ui.more.MoreScreen
import org.chessora.app.ui.news.NewsDetailScreen
import org.chessora.app.ui.news.NewsListScreen
import org.chessora.app.ui.onboarding.MembershipQuestionScreen
import org.chessora.app.ui.onboarding.OnboardingScreen
import org.chessora.app.ui.performance.PerformanceScreen
import org.chessora.app.ui.profile.ProfilePhotoScreen
import org.chessora.app.ui.ranking.RankingScreen
import org.chessora.app.ui.registrations.RegistrationsScreen
import org.chessora.app.ui.session.SessionViewModel
import org.chessora.app.ui.settings.SettingsScreen
import org.chessora.app.ui.shop.ShopScreen
import org.chessora.app.ui.splash.SplashScreen
import org.chessora.app.ui.tournaments.TournamentDetailScreen

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(ChessoraDestinations.HOME, R.string.nav_home, Icons.Default.Home),
    BottomTab(ChessoraDestinations.NEWS_LIST, R.string.nav_news, Icons.Default.Newspaper),
    BottomTab(ChessoraDestinations.REGISTRATIONS, R.string.nav_registrations, Icons.Default.HowToReg),
    BottomTab(ChessoraDestinations.MESSAGING, R.string.nav_messaging, Icons.AutoMirrored.Filled.Chat),
    BottomTab(ChessoraDestinations.MORE, R.string.nav_more, Icons.Default.MoreHoriz),
)

/**
 * Radice della navigazione dell'app. [SessionViewModel] è creato UNA VOLTA qui
 * (non dentro ogni schermata) e condiviso da tutto il grafo, cosi' il circolo
 * scelto e il branding sopravvivono al passaggio da una tab all'altra - vedi
 * ui/session/SessionViewModel.kt.
 *
 * onboarding è la prima destinazione se non è mai stato scelto un circolo,
 * altrimenti si parte direttamente da Home - la decisione avviene osservando
 * selectedClub (il publicCode), che diventa non-null non appena DataStore
 * emette il valore persistito (o rimane null per sempre se non è mai stato
 * scelto nulla).
 */
@Composable
fun ChessoraNavHost(pendingConversationId: Int? = null) {
    val navController = rememberNavController()
    var unconsumedConversationId by remember { mutableStateOf(pendingConversationId) }
    val sessionViewModel = chessoraViewModel { app -> SessionViewModel(app.repository, app.clubPreferences) }
    val selectedClub by sessionViewModel.selectedClub.collectAsState()
    val identityResolved by sessionViewModel.identityResolved.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in ChessoraDestinations.BOTTOM_BAR_ROUTES

    // Instradamento automatico oltre lo SPLASH (che decide da solo la prima volta,
    // vedi il composable SPLASH sotto): se da ONBOARDING un circolo risulta già
    // scelto (riavvii a freddo, o back button di sistema durante l'identificazione),
    // salta in avanti fino al punto giusto - HOME solo se anche l'identità è già
    // risolta, altrimenti IDENTITY - senza mai far rivedere l'onboarding.
    LaunchedEffect(selectedClub, identityResolved, currentRoute) {
        if (selectedClub != null && currentRoute == ChessoraDestinations.ONBOARDING) {
            val target = if (identityResolved) ChessoraDestinations.HOME else ChessoraDestinations.IDENTITY
            navController.navigate(target) {
                popUpTo(ChessoraDestinations.ONBOARDING) { inclusive = true }
            }
        }
    }

    // Tocco di una notifica di chat (vedi ChessoraFirebaseMessagingService,
    // MainActivity): appena l'app raggiunge una schermata stabile, apre subito
    // quella conversazione - una volta sola (unconsumedConversationId azzerato
    // subito dopo), cosi' un ritorno successivo a HOME non la riapre da capo.
    LaunchedEffect(currentRoute, unconsumedConversationId) {
        val idConversation = unconsumedConversationId
        if (idConversation != null && currentRoute in ChessoraDestinations.BOTTOM_BAR_ROUTES) {
            unconsumedConversationId = null
            navController.navigate(ChessoraDestinations.conversation(idConversation, isClubConversation = false, recipientId = 0, displayName = ""))
        }
    }

    val branding by sessionViewModel.branding.collectAsState()
    val identifiedPlayerName by sessionViewModel.identifiedPlayerName.collectAsState()
    val membersCount by sessionViewModel.membersCount.collectAsState()

    Scaffold(
        topBar = {
            if (showBottomBar) {
                ClubBrandingTopBar(
                    branding = branding,
                    identifiedPlayerName = identifiedPlayerName,
                    membersCount = membersCount,
                    isPlatformMode = selectedClub == ClubPreferences.PLATFORM_CLUB_CODE,
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // Si parte sempre dallo SPLASH (intro animata, puramente decorativa):
            // decide lui, una volta sola al termine dell'animazione, se andare a
            // ONBOARDING, IDENTITY o HOME in base allo stato già noto - vedi sotto.
            startDestination = ChessoraDestinations.SPLASH,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ChessoraDestinations.SPLASH) {
                val clubLogoUrl = NetworkModule.resolveAssetUrl(branding?.logoImage)
                SplashScreen(clubLogoUrl = clubLogoUrl, onFinished = {
                    val target = when {
                        selectedClub == null -> ChessoraDestinations.MEMBERSHIP_QUESTION
                        !identityResolved -> ChessoraDestinations.IDENTITY
                        else -> ChessoraDestinations.HOME
                    }
                    navController.navigate(target) {
                        popUpTo(ChessoraDestinations.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(ChessoraDestinations.MEMBERSHIP_QUESTION) {
                MembershipQuestionScreen(
                    onHasClub = { navController.navigate(ChessoraDestinations.ONBOARDING) },
                    onNoClub = {
                        sessionViewModel.selectClub(ClubPreferences.PLATFORM_CLUB_CODE)
                        navController.navigate(ChessoraDestinations.IDENTITY) {
                            popUpTo(ChessoraDestinations.MEMBERSHIP_QUESTION) { inclusive = true }
                        }
                    },
                )
            }
            composable(ChessoraDestinations.ONBOARDING) {
                OnboardingScreen(onClubSelected = { club ->
                    sessionViewModel.selectClub(club)
                    navController.navigate(ChessoraDestinations.IDENTITY) {
                        popUpTo(ChessoraDestinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(ChessoraDestinations.IDENTITY) {
                RequireClub(selectedClub) { club ->
                    val isPlatformMode = club == ClubPreferences.PLATFORM_CLUB_CODE
                    val effectiveClub = if (isPlatformMode) null else club
                    val clubName = branding?.let { "${it.namePrefix}${it.nameHighlight}" }?.takeIf { it.isNotBlank() && !isPlatformMode }
                    IdentityScreen(club = effectiveClub, clubName = clubName, onDone = {
                        sessionViewModel.refreshIdentity()
                        navController.navigate(ChessoraDestinations.HOME) {
                            popUpTo(ChessoraDestinations.IDENTITY) { inclusive = true }
                        }
                    })
                }
            }
            composable(ChessoraDestinations.HOME) {
                RequireClub(selectedClub) {
                    HomeScreen(
                        club = selectedClub?.takeIf { it != ClubPreferences.PLATFORM_CLUB_CODE },
                        onOpenTournament = { idTournament -> navController.navigate(ChessoraDestinations.tournamentDetail(idTournament)) },
                    )
                }
            }
            composable(
                ChessoraDestinations.TOURNAMENT_DETAIL,
                arguments = listOf(navArgument("idTournament") { type = NavType.IntType }),
            ) { backStack ->
                val idTournament = backStack.arguments?.getInt("idTournament") ?: return@composable
                TournamentDetailScreen(
                    idTournament = idTournament,
                    onIdentify = { navController.navigate(ChessoraDestinations.IDENTITY) },
                )
            }
            composable(ChessoraDestinations.NEWS_LIST) {
                RequireClub(selectedClub) {
                    NewsListScreen(
                        club = selectedClub?.takeIf { it != ClubPreferences.PLATFORM_CLUB_CODE },
                        onNewsClick = { navController.navigate(ChessoraDestinations.newsDetail(it)) },
                    )
                }
            }
            composable(
                ChessoraDestinations.NEWS_DETAIL,
                arguments = listOf(navArgument("idNews") { type = NavType.IntType }),
            ) { backStack ->
                val idNews = backStack.arguments?.getInt("idNews") ?: return@composable
                RequireClub(selectedClub) {
                    NewsDetailScreen(club = selectedClub?.takeIf { it != ClubPreferences.PLATFORM_CLUB_CODE }, idNews = idNews)
                }
            }
            composable(ChessoraDestinations.CALENDAR) {
                RequireClub(selectedClub) { club -> CalendarScreen(club = club) }
            }
            composable(ChessoraDestinations.PERFORMANCE) {
                PerformanceScreen(onIdentify = { navController.navigate(ChessoraDestinations.IDENTITY) })
            }
            composable(ChessoraDestinations.PROFILE_PHOTO) {
                ProfilePhotoScreen(onIdentify = { navController.navigate(ChessoraDestinations.IDENTITY) })
            }
            composable(ChessoraDestinations.REGISTRATIONS) {
                // Le iscrizioni ai tornei (orga.TournamentPlayerRegistrations) non sono
                // legate a un circolo - a differenza delle altre schermate qui, non
                // serve RequireClub: funziona identica anche in modalità piattaforma.
                RegistrationsScreen(
                    onIdentify = { navController.navigate(ChessoraDestinations.IDENTITY) },
                    onOpenTournament = { idTournament -> navController.navigate(ChessoraDestinations.tournamentDetail(idTournament)) },
                )
            }
            composable(ChessoraDestinations.RANKING) {
                RankingScreen(club = selectedClub?.takeIf { it != ClubPreferences.PLATFORM_CLUB_CODE })
            }
            composable(ChessoraDestinations.MESSAGING) {
                MessagingListScreen(
                    onOpenConversation = { conversation ->
                        navController.navigate(
                            ChessoraDestinations.conversation(conversation.idConversation, conversation.isClubConversation, 0, conversation.displayName),
                        )
                    },
                    onNewMessage = { navController.navigate(ChessoraDestinations.NEW_MESSAGE) },
                    onIdentify = { navController.navigate(ChessoraDestinations.IDENTITY) },
                )
            }
            composable(ChessoraDestinations.NEW_MESSAGE) {
                NewMessageScreen(
                    onSelectPlayer = { idPlayer, displayName ->
                        navController.navigate(ChessoraDestinations.conversation(-1, isClubConversation = false, recipientId = idPlayer, displayName = displayName)) {
                            popUpTo(ChessoraDestinations.MESSAGING)
                        }
                    },
                    onSelectClub = { idClub, displayName ->
                        navController.navigate(ChessoraDestinations.conversation(-1, isClubConversation = true, recipientId = idClub, displayName = displayName)) {
                            popUpTo(ChessoraDestinations.MESSAGING)
                        }
                    },
                )
            }
            composable(
                ChessoraDestinations.CONVERSATION,
                arguments = listOf(
                    navArgument("idConversation") { type = NavType.IntType },
                    navArgument("isClubConversation") { type = NavType.BoolType },
                    navArgument("recipientId") { type = NavType.IntType },
                    navArgument("displayName") { type = NavType.StringType },
                ),
            ) { backStack ->
                val idConversation = backStack.arguments?.getInt("idConversation") ?: return@composable
                val isClubConversation = backStack.arguments?.getBoolean("isClubConversation") ?: false
                val recipientId = backStack.arguments?.getInt("recipientId") ?: 0
                val encodedName = backStack.arguments?.getString("displayName") ?: ""
                val displayName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                ConversationScreen(idConversation = idConversation, isClubConversation = isClubConversation, recipientId = recipientId, displayName = displayName)
            }
            composable(ChessoraDestinations.MORE) {
                MoreScreen(
                    onCalendarClick = { navController.navigate(ChessoraDestinations.CALENDAR) },
                    onRankingClick = { navController.navigate(ChessoraDestinations.RANKING) },
                    onPerformanceClick = { navController.navigate(ChessoraDestinations.PERFORMANCE) },
                    onProfilePhotoClick = { navController.navigate(ChessoraDestinations.PROFILE_PHOTO) },
                    onBoardClick = { navController.navigate(ChessoraDestinations.BOARD) },
                    onShopClick = { navController.navigate(ChessoraDestinations.SHOP) },
                    onSettingsClick = { navController.navigate(ChessoraDestinations.SETTINGS) },
                    isPlatformMode = selectedClub == ClubPreferences.PLATFORM_CLUB_CODE,
                )
            }
            composable(ChessoraDestinations.BOARD) {
                RequireClub(selectedClub) { club -> BoardScreen(club = club) }
            }
            composable(ChessoraDestinations.SHOP) {
                RequireClub(selectedClub) { club -> ShopScreen(club = club) }
            }
            composable(ChessoraDestinations.SETTINGS) {
                SettingsScreen(
                    club = selectedClub,
                    identifiedPlayerName = identifiedPlayerName,
                    onChangeClub = {
                        // Azzera PRIMA il circolo in memoria (SessionViewModel.clearSelectedClub):
                        // se selectedClub fosse ancora quello vecchio quando arriviamo su
                        // ONBOARDING, la guardia più sopra (pensata solo per i riavvii a
                        // freddo con circolo già scelto) rimbalzerebbe subito indietro senza
                        // far vedere il selettore.
                        sessionViewModel.clearSelectedClub()
                        navController.navigate(ChessoraDestinations.ONBOARDING) {
                            popUpTo(0) // svuota tutto il back stack: si riparte da zero col nuovo circolo
                        }
                    },
                    onIdentify = {
                        // Niente popUpTo qui (a differenza degli altri usi di IDENTITY): il
                        // back button deve tornare a Impostazioni, non a Home.
                        navController.navigate(ChessoraDestinations.IDENTITY)
                    },
                )
            }
        }
    }
}

/**
 * Barra in alto con nome e logo del circolo scelto (docs/android-app-spec.md
 * §4: "Aspetto dell'app adattato al tema/branding del circolo scelto"). Mostra
 * solo il nome del brand Chessora finché [branding] non è ancora stato
 * caricato da GET /api/site-settings (vedi SessionViewModel.loadBranding).
 *
 * Sotto la barra, una riga con il numero di soci del circolo (a sinistra) e -
 * se [identifiedPlayerName] è valorizzato (schermata di identificazione, vedi
 * ui/identity/) - il nome riconosciuto (a destra): è il punto dell'app sempre
 * visibile (ogni schermata con bottom bar) dove l'identificazione "si vede",
 * oltre alle Impostazioni.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ClubBrandingTopBar(branding: SiteBranding?, identifiedPlayerName: String? = null, membersCount: Int? = null, isPlatformMode: Boolean = false) {
    Column {
        TopAppBar(
            title = {
                val name = branding?.let { "${it.namePrefix}${it.nameHighlight}" }?.takeIf { it.isNotBlank() }
                Text(name ?: stringResource(R.string.app_name))
            },
            navigationIcon = {
                val logoUrl = NetworkModule.resolveAssetUrl(branding?.logoImage)
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp).size(32.dp),
                    )
                } else if (isPlatformMode) {
                    // Nessun circolo scelto (vedi MembershipQuestionScreen): mostra il
                    // logo di Chessora stessa invece di quello di un circolo.
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp).size(32.dp),
                    )
                }
            },
        )
        if (membersCount != null || identifiedPlayerName != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (membersCount != null) {
                    Text(
                        stringResource(R.string.topbar_members_count, membersCount),
                        style = MaterialTheme.typography.labelMedium,
                    )
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }
                if (identifiedPlayerName != null) {
                    IdentifiedBadge(name = identifiedPlayerName)
                }
            }
        }
    }
}

@Composable
private fun IdentifiedBadge(name: String) {
    val firstName = name.trim().substringBefore(' ')
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 12.dp)
            .background(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = firstName,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/**
 * Piccola guardia usata da ogni schermata che richiede un circolo scelto:
 * se per qualche motivo si arriva qui con club ancora null (es. onboarding
 * saltato con back button di sistema), mostra semplicemente nulla invece di
 * far crashare la schermata - in pratica non dovrebbe mai succedere perché la
 * navigazione verso queste route parte sempre da un punto che ha già un
 * circolo scelto, ma è una rete di sicurezza a basso costo.
 */
@Composable
private fun RequireClub(club: String?, content: @Composable (String) -> Unit) {
    if (club != null) content(club)
}
