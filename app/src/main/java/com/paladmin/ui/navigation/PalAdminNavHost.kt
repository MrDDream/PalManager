package com.paladmin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.paladmin.ui.bans.BansScreen
import com.paladmin.ui.broadcast.BroadcastScreen
import com.paladmin.ui.dashboard.DashboardScreen
import com.paladmin.ui.guilds.GuildsScreen
import com.paladmin.ui.humanpicker.HumanPickerScreen
import com.paladmin.ui.itempicker.ItemPickerScreen
import com.paladmin.ui.livemap.LiveMapScreen
import com.paladmin.ui.logs.LogsScreen
import com.paladmin.ui.palcreator.PalCreatorScreen
import com.paladmin.ui.palpicker.PalPickerScreen
import com.paladmin.ui.sftp.SftpScreen
import com.paladmin.ui.players.PlayersScreen
import com.paladmin.ui.profiles.AddEditProfileScreen
import com.paladmin.ui.profiles.ServerProfileListScreen
import com.paladmin.ui.settings.AppSettingsScreen
import com.paladmin.ui.splash.SplashScreen

@Composable
fun PalAdminNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onOpenDashboard = { profileId ->
                    navController.navigate(NavRoutes.PROFILE_LIST) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                    navController.navigate(NavRoutes.dashboard(profileId))
                },
                onOpenProfileList = {
                    navController.navigate(NavRoutes.PROFILE_LIST) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(NavRoutes.PROFILE_LIST) {
            ServerProfileListScreen(
                onOpenDashboard = { id -> navController.navigate(NavRoutes.dashboard(id)) },
                onAddProfile = { navController.navigate(NavRoutes.addEditProfile()) },
                onEditProfile = { id -> navController.navigate(NavRoutes.addEditProfile(id)) },
                onOpenSettings = { navController.navigate(NavRoutes.APP_SETTINGS) },
            )
        }
        composable(NavRoutes.APP_SETTINGS) {
            AppSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.ADD_EDIT_PROFILE,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType; defaultValue = "-1" }),
        ) {
            AddEditProfileScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.DASHBOARD,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId")).toLong()
            DashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenItemPicker = { navController.navigate(NavRoutes.itemPicker(profileId)) },
                onOpenPalPicker = { navController.navigate(NavRoutes.palPicker(profileId)) },
                onOpenPlayers = { navController.navigate(NavRoutes.players(profileId)) },
                onOpenGuilds = { navController.navigate(NavRoutes.guilds(profileId)) },
                onOpenBans = { navController.navigate(NavRoutes.bans(profileId)) },
                onOpenBroadcast = { navController.navigate(NavRoutes.broadcast(profileId)) },
                onOpenLiveMap = { navController.navigate(NavRoutes.liveMap(profileId)) },
                onOpenHumanPicker = { navController.navigate(NavRoutes.humanPicker(profileId)) },
                onOpenLogs = { navController.navigate(NavRoutes.logs(profileId)) },
                onOpenSftp = { navController.navigate(NavRoutes.sftp(profileId)) },
                onOpenPalCreator = { navController.navigate(NavRoutes.palCreator(profileId)) },
            )
        }
        composable(
            route = NavRoutes.ITEM_PICKER,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            ItemPickerScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.PAL_PICKER,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            PalPickerScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.PLAYERS,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            PlayersScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.GUILDS,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId")).toLong()
            GuildsScreen(
                onBack = { navController.popBackStack() },
                onOpenLiveMap = { points -> navController.navigate(NavRoutes.liveMap(profileId, points)) },
            )
        }
        composable(
            route = NavRoutes.BANS,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            BansScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.BROADCAST,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            BroadcastScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.LIVE_MAP,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("focusPoints") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            LiveMapScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.HUMAN_PICKER,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) {
            HumanPickerScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = NavRoutes.LOGS,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId")).toLong()
            LogsScreen(
                onBack = { navController.popBackStack() },
                onOpenEditProfile = { navController.navigate(NavRoutes.addEditProfile(profileId)) },
            )
        }
        composable(
            route = NavRoutes.SFTP,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId")).toLong()
            SftpScreen(
                onBack = { navController.popBackStack() },
                onOpenEditProfile = { navController.navigate(NavRoutes.addEditProfile(profileId)) },
            )
        }
        composable(
            route = NavRoutes.PAL_CREATOR,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val profileId = checkNotNull(backStackEntry.arguments?.getString("profileId")).toLong()
            PalCreatorScreen(
                onBack = { navController.popBackStack() },
                onOpenEditProfile = { navController.navigate(NavRoutes.addEditProfile(profileId)) },
            )
        }
    }
}
