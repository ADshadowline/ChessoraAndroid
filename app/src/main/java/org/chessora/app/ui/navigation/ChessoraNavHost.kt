package org.chessora.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.DisposableEffect
import org.chessora.app.data.local.ClubPreferences
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.SiteBranding
import org.chessora.app.ui.board.BoardScreen
import org.chessora.app.ui.calendar.BandoViewerScreen
import org.chessora.app.ui.calendar.CalendarScreen
import org.chessora.app.ui.common.ChessoraCountBadge
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.home.DesktopHomeCallbacks
import org.chessora.app.ui.home.EventsListScreen
import org.chessora.app.ui.auth.CompleteProfileScreen
import org.chessora.app.ui.auth.EmailPendingScreen
import org.chessora.app.ui.auth.ForgotPasswordScreen
import org.chessora.app.ui.auth.LoginEmailScreen
import org.chessora.app.ui.auth.LoginFideScreen
import org.chessora.app.ui.auth.LoginScreen
import org.chessora.app.ui.home.HomeScreen
import org.chessora.app.ui.messaging.ConversationScreen
import org.chessora.app.ui.messaging.MessagingListScreen
import org.chessora.app.ui.messaging.NewMessageScreen
import org.chessora.app.ui.more.MoreScreen
import org.chessora.app.ui.news.NewsDetailScreen
import org.chessora.app.ui.news.NewsListScreen
import org.chessora.app.ui.onboarding.MembershipQuestionScreen
import org.chessora.app.ui.onboarding.OnboardingScreen
import org.chessora.app.ui.performance.EloRatingType
import org.chessora.app.ui.performance.PerformanceScreen
import org.chessora.app.ui.profile.ProfilePhotoScreen
import org.chessora.app.ui.ranking.RankingScreen
import org.chessora.app.ui.registrations.RegistrationsScreen
import org.chessora.app.ui.session.SessionViewModel
import org.chessora.app.ui.settings.IconSettingsScreen
import org.chessora.app.ui.settings.SettingsScreen
import org.chessora.app.ui.shop.ShopScreen
import org.chessora.app.ui.splash.SplashScreen
import org.chessora.app.ui.theme.ChessoraGold
import org.chessora.app.ui.tournaments.TournamentDetailScreen
import org.chessora.app.ui.video.VideoScreen

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
    val sessionViewModel = chessoraViewModel { app -> SessionViewModel(app.repository, app.clubPreferences, app.authPreferences) }
    val selectedClub by sessionViewModel.selectedClub.collectAsState()
    val identityResolved by sessionViewModel.identityResolved.collectAsState()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()

    // Il token JWT non ha refresh in questa prima versione (vedi ui/auth/): un 401
    // qualunque invalida subito la sessione e riporta al login, da qualunque schermata
    // ci si trovi - l'interceptor OkHttp (data/remote/NetworkModule.kt) non ha accesso a
    // un NavController, quindi registra qui solo la callback.
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    DisposableEffect(Unit) {
        // L'interceptor OkHttp gira su un thread di rete, mai sul main thread: la
        // navigazione va per forza spostata li' con un Handler, altrimenti
        // navController.navigate lancerebbe (o si comporterebbe in modo indefinito).
        NetworkModule.onUnauthorized = {
            mainHandler.post {
                sessionViewModel.onUnauthorized()
                navController.navigate(ChessoraDestinations.AUTH_LOGIN) { popUpTo(0) }
            }
        }
        onDispose { NetworkModule.onUnauthorized = null }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val displayMode by sessionViewModel.displayMode.collectAsState()
    val showTopBar = currentRoute in ChessoraDestinations.BOTTOM_BAR_ROUTES
    // In visualizzazione desktop la navigazione avviene tramite la griglia di icone della
    // Home (vedi ui/home/HomeScreen.kt), mai dalla bottom bar - nascosta su ogni schermata
    // (la barra superiore con branding/soci/Elo resta invece visibile).
    val showBottomBar = showTopBar && displayMode != ClubPreferences.DISPLAY_MODE_DESKTOP

    // Instradamento automatico oltre lo SPLASH (che decide da solo la prima volta,
    // vedi il composable SPLASH sotto): se da ONBOARDING un circolo risulta già
    // scelto (riavvii a freddo, o back button di sistema durante l'identificazione),
    // salta in avanti fino al punto giusto - HOME solo se anche l'identità è già
    // risolta, altrimenti IDENTITY - senza mai far rivedere l'onboarding.
    LaunchedEffect(selectedClub, currentRoute) {
        if (selectedClub != null && currentRoute == ChessoraDestinations.ONBOARDING) {
            navController.navigate(ChessoraDestinations.HOME) {
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
    val isTournamentManager by sessionViewModel.isTournamentManager.collectAsState()
    val registeredTournamentsCount by sessionViewModel.registeredTournamentsCount.collectAsState()
    val registeredTournamentStartingSoon by sessionViewModel.registeredTournamentStartingSoon.collectAsState()
    val unreadMessagesCount by sessionViewModel.unreadMessagesCount.collectAsState()
    val context = LocalContext.current

    // Un torneo può essere (pre)iscritto/ritirato da TournamentDetailScreen, un
    // ViewModel diverso da questo: senza un refresh esplicito il badge sull'icona
    // "Iscrizioni ai tornei" (sotto e in HomeScreen) resterebbe indietro finché non si
    // riavvia l'app - ricaricarlo a ogni cambio di rotta lo mantiene corretto appena si
    // torna su una qualunque schermata dopo aver toccato una preiscrizione.
    LaunchedEffect(currentRoute) {
        sessionViewModel.refreshRegisteredTournamentsCount()
        sessionViewModel.refreshUnreadMessagesCount()
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                ClubBrandingTopBar(
                    branding = branding,
                    identifiedPlayerName = identifiedPlayerName,
                    membersCount = membersCount,
                    isPlatformMode = selectedClub == ClubPreferences.PLATFORM_CLUB_CODE,
                    isTournamentManager = isTournamentManager,
                    // Nessun login/gestione nativa in app (nessun account con password
                    // esiste qui) - apre il wizard nel browser, dove il gestore usa le sue
                    // credenziali separate (create dall'admin del circolo).
                    onManageTournamentsClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.chessora.org/wizard/")))
                    },
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
                            icon = {
                                val badgeCount = when (tab.route) {
                                    ChessoraDestinations.REGISTRATIONS -> registeredTournamentsCount
                                    ChessoraDestinations.MESSAGING -> unreadMessagesCount
                                    else -> 0
                                }
                                if (badgeCount > 0) {
                                    BadgedBox(badge = { ChessoraCountBadge(badgeCount) }) {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            },
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
                val splashBackgroundUri by sessionViewModel.splashBackgroundUri.collectAsState()
                SplashScreen(clubLogoUrl = clubLogoUrl, backgroundUri = splashBackgroundUri, onFinished = {
                    // Accesso obbligatorio (vedi ui/auth/): AUTH_LOGIN prima di tutto se non
                    // già loggati. selectedClub viene già valorizzato da AuthSessionPersister
                    // quando il login risolve un circolo (socio riconosciuto) - se resta null
                    // dopo un login riuscito, l'utente non è socio di alcun circolo e sceglie
                    // solo quale contenuto sfogliare (MembershipQuestionScreen).
                    val target = when {
                        !isLoggedIn -> ChessoraDestinations.AUTH_LOGIN
                        selectedClub == null -> ChessoraDestinations.MEMBERSHIP_QUESTION
                        else -> ChessoraDestinations.HOME
                    }
                    navController.navigate(target) {
                        popUpTo(ChessoraDestinations.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(ChessoraDestinations.AUTH_LOGIN) {
                LoginScreen(
                    onLoggedIn = {
                        sessionViewModel.onLoggedIn()
                        navController.navigate(ChessoraDestinations.SPLASH) { popUpTo(0) }
                    },
                    onNeedsProfile = { navController.navigate(ChessoraDestinations.AUTH_COMPLETE_PROFILE) },
                    onLoginEmail = { navController.navigate(ChessoraDestinations.AUTH_LOGIN_EMAIL) },
                    onLoginFide = { navController.navigate(ChessoraDestinations.AUTH_LOGIN_FIDE) },
                )
            }
            composable(ChessoraDestinations.AUTH_LOGIN_EMAIL) {
                LoginEmailScreen(
                    onLoggedIn = {
                        sessionViewModel.onLoggedIn()
                        navController.navigate(ChessoraDestinations.SPLASH) { popUpTo(0) }
                    },
                    onForgotPassword = { navController.navigate(ChessoraDestinations.AUTH_FORGOT_PASSWORD) },
                )
            }
            composable(ChessoraDestinations.AUTH_LOGIN_FIDE) {
                LoginFideScreen(
                    onLoggedIn = {
                        sessionViewModel.onLoggedIn()
                        navController.navigate(ChessoraDestinations.SPLASH) { popUpTo(0) }
                    },
                    onRegistered = { email ->
                        navController.navigate(ChessoraDestinations.emailPending(email)) {
                            popUpTo(ChessoraDestinations.AUTH_LOGIN)
                        }
                    },
                )
            }
            composable(ChessoraDestinations.AUTH_FORGOT_PASSWORD) {
                ForgotPasswordScreen()
            }
            composable(ChessoraDestinations.AUTH_COMPLETE_PROFILE) {
                CompleteProfileScreen(onDone = {
                    sessionViewModel.onLoggedIn()
                    navController.navigate(ChessoraDestinations.SPLASH) { popUpTo(0) }
                })
            }
            composable(
                ChessoraDestinations.AUTH_EMAIL_PENDING,
                arguments = listOf(navArgument("email") { type = NavType.StringType }),
            ) { backStack ->
                val encodedEmail = backStack.arguments?.getString("email") ?: return@composable
                EmailPendingScreen(
                    email = java.net.URLDecoder.decode(encodedEmail, "UTF-8"),
                    onBackToLogin = { navController.navigate(ChessoraDestinations.AUTH_LOGIN) { popUpTo(0) } },
                )
            }
            composable(ChessoraDestinations.MEMBERSHIP_QUESTION) {
                MembershipQuestionScreen(
                    onHasClub = { navController.navigate(ChessoraDestinations.ONBOARDING) },
                    onNoClub = {
                        sessionViewModel.selectClub(ClubPreferences.PLATFORM_CLUB_CODE)
                        navController.navigate(ChessoraDestinations.HOME) {
                            popUpTo(ChessoraDestinations.MEMBERSHIP_QUESTION) { inclusive = true }
                        }
                    },
                )
            }
            composable(ChessoraDestinations.ONBOARDING) {
                OnboardingScreen(onClubSelected = { club ->
                    sessionViewModel.selectClub(club)
                    navController.navigate(ChessoraDestinations.HOME) {
                        popUpTo(ChessoraDestinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(ChessoraDestinations.HOME) {
                RequireClub(selectedClub) {
                    HomeScreen(
                        club = selectedClub?.takeIf { it != ClubPreferences.PLATFORM_CLUB_CODE },
                        onOpenTournament = { idTournament -> navController.navigate(ChessoraDestinations.tournamentDetail(idTournament)) },
                        isPlatformMode = selectedClub == ClubPreferences.PLATFORM_CLUB_CODE,
                        registeredTournamentsCount = registeredTournamentsCount,
                        unreadMessagesCount = unreadMessagesCount,
                        registeredTournamentStartingSoon = registeredTournamentStartingSoon,
                        desktop = DesktopHomeCallbacks(
                            onOpenEvents = { navController.navigate(ChessoraDestinations.EVENTS) },
                            onOpenCalendar = { navController.navigate(ChessoraDestinations.CALENDAR) },
                            onOpenNews = { navController.navigate(ChessoraDestinations.NEWS_LIST) },
                            onOpenRegistrations = { navController.navigate(ChessoraDestinations.REGISTRATIONS) },
                            onOpenMessaging = { navController.navigate(ChessoraDestinations.MESSAGING) },
                            onOpenRanking = { navController.navigate(ChessoraDestinations.RANKING) },
                            onOpenPerformance = { navController.navigate(ChessoraDestinations.performance()) },
                            onOpenBoard = { navController.navigate(ChessoraDestinations.BOARD) },
                            onOpenShop = { navController.navigate(ChessoraDestinations.SHOP) },
                            onOpenVideo = { navController.navigate(ChessoraDestinations.VIDEO) },
                            onOpenSettings = { navController.navigate(ChessoraDestinations.SETTINGS) },
                        ),
                    )
                }
            }
            composable(ChessoraDestinations.EVENTS) {
                RequireClub(selectedClub) {
                    EventsListScreen(
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
                    onLogin = { navController.navigate(ChessoraDestinations.AUTH_LOGIN) },
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
                RequireClub(selectedClub) { club ->
                    CalendarScreen(
                        club = club,
                        onOpenBando = { url -> navController.navigate(ChessoraDestinations.bandoViewer(url)) },
                    )
                }
            }
            composable(
                ChessoraDestinations.BANDO_VIEWER,
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) { backStack ->
                val encodedUrl = backStack.arguments?.getString("url") ?: return@composable
                BandoViewerScreen(url = java.net.URLDecoder.decode(encodedUrl, "UTF-8"))
            }
            composable(
                ChessoraDestinations.PERFORMANCE,
                arguments = listOf(navArgument("focus") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { backStack ->
                val focus = EloRatingType.fromRouteValue(backStack.arguments?.getString("focus"))
                PerformanceScreen(onIdentify = { navController.navigate(ChessoraDestinations.AUTH_LOGIN) }, focus = focus)
            }
            composable(ChessoraDestinations.PROFILE_PHOTO) {
                ProfilePhotoScreen(onIdentify = { navController.navigate(ChessoraDestinations.AUTH_LOGIN) })
            }
            composable(ChessoraDestinations.REGISTRATIONS) {
                // Le iscrizioni ai tornei (orga.TournamentPlayerRegistrations) non sono
                // legate a un circolo - a differenza delle altre schermate qui, non
                // serve RequireClub: funziona identica anche in modalità piattaforma.
                RegistrationsScreen(
                    onIdentify = { navController.navigate(ChessoraDestinations.AUTH_LOGIN) },
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
                    onIdentify = { navController.navigate(ChessoraDestinations.AUTH_LOGIN) },
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
                    onPerformanceClick = { navController.navigate(ChessoraDestinations.performance()) },
                    onBoardClick = { navController.navigate(ChessoraDestinations.BOARD) },
                    onShopClick = { navController.navigate(ChessoraDestinations.SHOP) },
                    onVideoClick = { navController.navigate(ChessoraDestinations.VIDEO) },
                    onSettingsClick = { navController.navigate(ChessoraDestinations.SETTINGS) },
                    isPlatformMode = selectedClub == ClubPreferences.PLATFORM_CLUB_CODE,
                )
            }
            composable(ChessoraDestinations.BOARD) {
                RequireClub(selectedClub) { club ->
                    BoardScreen(
                        club = club,
                        onOpenConversation = { idPlayer, displayName ->
                            navController.navigate(ChessoraDestinations.conversation(-1, isClubConversation = false, recipientId = idPlayer, displayName = displayName))
                        },
                    )
                }
            }
            composable(ChessoraDestinations.SHOP) {
                RequireClub(selectedClub) { club -> ShopScreen(club = club) }
            }
            composable(ChessoraDestinations.VIDEO) {
                RequireClub(selectedClub) { club -> VideoScreen(club = club) }
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
                    onLogout = {
                        sessionViewModel.logout()
                        navController.navigate(ChessoraDestinations.AUTH_LOGIN) { popUpTo(0) }
                    },
                    onResetSettings = {
                        sessionViewModel.resetAllSettings()
                        navController.navigate(ChessoraDestinations.AUTH_LOGIN) { popUpTo(0) }
                    },
                    onOpenProfilePhoto = { navController.navigate(ChessoraDestinations.PROFILE_PHOTO) },
                    onOpenIconSettings = { navController.navigate(ChessoraDestinations.ICON_SETTINGS) },
                )
            }
            composable(ChessoraDestinations.ICON_SETTINGS) {
                IconSettingsScreen(isPlatformMode = selectedClub == ClubPreferences.PLATFORM_CLUB_CODE)
            }
        }
    }
}

/**
 * Barra in alto con nome e logo del circolo scelto (docs/android-app-spec.md
 * §4: "Aspetto dell'app adattato al tema/branding del circolo scelto"). Mostra
 * solo il nome del brand Chessora finché [branding] non è ancora stato
 * caricato da GET /api/site-settings (vedi SessionViewModel.loadBranding). Il
 * numero soci è mostrato tra parentesi subito dopo il nome del circolo (es.
 * "Circolo Scacchi Torino (125 soci)"), non più in una riga separata.
 *
 * Sotto la barra, allineato a sinistra: il nome dell'utente loggato (se valorizzato,
 * vedi ui/auth/) - i punteggi Elo non sono più mostrati qui (restano in "Andamento
 * Elo", raggiungibile da Altro/griglia desktop).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ClubBrandingTopBar(
    branding: SiteBranding?,
    identifiedPlayerName: String? = null,
    membersCount: Int? = null,
    isPlatformMode: Boolean = false,
    isTournamentManager: Boolean = false,
    onManageTournamentsClick: () -> Unit = {},
) {
    Column {
        TopAppBar(
            title = {
                val clubName = branding?.let { "${it.namePrefix}${it.nameHighlight}" }?.takeIf { it.isNotBlank() }
                val title = clubName ?: stringResource(R.string.app_name)
                val withCount = if (clubName != null && membersCount != null) {
                    "$title (${stringResource(R.string.topbar_members_count, membersCount)})"
                } else {
                    title
                }
                Text(withCount)
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
        if (identifiedPlayerName != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IdentifiedBadge(name = identifiedPlayerName)
                if (isTournamentManager) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = stringResource(R.string.manage_tournaments),
                        tint = ChessoraGold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clickable(onClick = onManageTournamentsClick)
                            .padding(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentifiedBadge(name: String) {
    // Il nome può arrivare in due formati (vedi AuthSessionPersister): "Cognome, Nome"
    // per un socio risolto da anagrafica FIDE, "Nome Cognome" per un amatoriale
    // (nome/cognome digitati alla registrazione) - senza distinguerli, il primo formato
    // mostrava "Cognome," (con la virgola, tagliato) invece del vero nome proprio.
    val trimmed = name.trim()
    val firstName = if (trimmed.contains(',')) {
        trimmed.substringAfter(',').trim().substringBefore(' ').ifBlank { trimmed.substringBefore(',') }
    } else {
        trimmed.substringBefore(' ')
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
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
