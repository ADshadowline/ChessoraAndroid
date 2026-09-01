package org.chessora.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.chessora.app.data.remote.NetworkModule
import org.chessora.app.data.remote.dto.SiteBranding
import org.chessora.app.ui.board.BoardScreen
import org.chessora.app.ui.calendar.CalendarScreen
import org.chessora.app.ui.common.chessoraViewModel
import org.chessora.app.ui.home.HomeScreen
import org.chessora.app.ui.more.MoreScreen
import org.chessora.app.ui.news.NewsDetailScreen
import org.chessora.app.ui.news.NewsListScreen
import org.chessora.app.ui.onboarding.OnboardingScreen
import org.chessora.app.ui.ranking.RankingScreen
import org.chessora.app.ui.session.SessionViewModel
import org.chessora.app.ui.settings.SettingsScreen
import org.chessora.app.ui.shop.ShopScreen
import org.chessora.app.ui.tornei.TorneiListScreen
import org.chessora.app.ui.tornei.TorneoDetailScreen

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS = listOf(
    BottomTab(ChessoraDestinations.HOME, R.string.nav_home, Icons.Default.Home),
    BottomTab(ChessoraDestinations.NEWS_LIST, R.string.nav_news, Icons.Default.Newspaper),
    BottomTab(ChessoraDestinations.CALENDAR, R.string.nav_calendar, Icons.Default.CalendarMonth),
    BottomTab(ChessoraDestinations.TORNEI_LIST, R.string.nav_tornei, Icons.Default.EmojiEvents),
    BottomTab(ChessoraDestinations.RANKING, R.string.nav_ranking, Icons.Default.Leaderboard),
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
fun ChessoraNavHost() {
    val navController = rememberNavController()
    val sessionViewModel = chessoraViewModel { app -> SessionViewModel(app.repository, app.clubPreferences) }
    val selectedClub by sessionViewModel.selectedClub.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in ChessoraDestinations.BOTTOM_BAR_ROUTES

    // L'app parte sempre dal grafo su ONBOARDING (vedi startDestination sotto),
    // ma se DataStore rivela che un circolo era già stato scelto in una sessione
    // precedente, si salta subito a Home senza far vedere l'onboarding all'utente
    // di ritorno - da qui in poi ONBOARDING resta raggiungibile solo esplicitamente
    // da "Cambia circolo" nelle Impostazioni.
    LaunchedEffect(selectedClub, currentRoute) {
        if (selectedClub != null && currentRoute == ChessoraDestinations.ONBOARDING) {
            navController.navigate(ChessoraDestinations.HOME) {
                popUpTo(ChessoraDestinations.ONBOARDING) { inclusive = true }
            }
        }
    }

    val branding by sessionViewModel.branding.collectAsState()

    Scaffold(
        topBar = {
            if (showBottomBar) {
                ClubBrandingTopBar(branding = branding)
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
            // Se non c'è ancora un circolo scelto (selectedClubId == null) si parte
            // dall'onboarding; il vero valore iniziale arriva asincrono da DataStore,
            // quindi per un istante mostriamo comunque onboarding - non è un problema
            // perché appena arriva il valore vero il grafo naviga a HOME (vedi sotto).
            startDestination = ChessoraDestinations.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ChessoraDestinations.ONBOARDING) {
                OnboardingScreen(onClubSelected = { club ->
                    sessionViewModel.selectClub(club)
                    navController.navigate(ChessoraDestinations.HOME) {
                        popUpTo(ChessoraDestinations.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(ChessoraDestinations.HOME) {
                RequireClub(selectedClub) { club ->
                    HomeScreen(club = club, onNewsClick = { navController.navigate(ChessoraDestinations.newsDetail(it)) })
                }
            }
            composable(ChessoraDestinations.NEWS_LIST) {
                RequireClub(selectedClub) { club ->
                    NewsListScreen(club = club, onNewsClick = { navController.navigate(ChessoraDestinations.newsDetail(it)) })
                }
            }
            composable(
                ChessoraDestinations.NEWS_DETAIL,
                arguments = listOf(navArgument("idNews") { type = NavType.IntType }),
            ) { backStack ->
                val idNews = backStack.arguments?.getInt("idNews") ?: return@composable
                RequireClub(selectedClub) { club -> NewsDetailScreen(club = club, idNews = idNews) }
            }
            composable(ChessoraDestinations.CALENDAR) {
                RequireClub(selectedClub) { club -> CalendarScreen(club = club) }
            }
            composable(ChessoraDestinations.TORNEI_LIST) {
                RequireClub(selectedClub) { club ->
                    TorneiListScreen(club = club, onTorneoClick = { navController.navigate(ChessoraDestinations.torneoDetail(it)) })
                }
            }
            composable(
                ChessoraDestinations.TORNEO_DETAIL,
                arguments = listOf(navArgument("idTorneo") { type = NavType.IntType }),
            ) { backStack ->
                val idTorneo = backStack.arguments?.getInt("idTorneo") ?: return@composable
                RequireClub(selectedClub) { club -> TorneoDetailScreen(club = club, idTorneo = idTorneo) }
            }
            composable(ChessoraDestinations.RANKING) {
                RankingScreen(club = selectedClub)
            }
            composable(ChessoraDestinations.MORE) {
                MoreScreen(
                    onBoardClick = { navController.navigate(ChessoraDestinations.BOARD) },
                    onShopClick = { navController.navigate(ChessoraDestinations.SHOP) },
                    onSettingsClick = { navController.navigate(ChessoraDestinations.SETTINGS) },
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
                    onChangeClub = {
                        navController.navigate(ChessoraDestinations.ONBOARDING) {
                            popUpTo(0) // svuota tutto il back stack: si riparte da zero col nuovo circolo
                        }
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
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ClubBrandingTopBar(branding: SiteBranding?) {
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
            }
        },
    )
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
