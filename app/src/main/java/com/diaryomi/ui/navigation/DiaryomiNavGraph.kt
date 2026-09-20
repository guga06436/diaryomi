package com.diaryomi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diaryomi.ui.detail.SearchDetailScreen
import com.diaryomi.ui.library.LibraryScreen

@Composable
fun DiaryomiNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "library"
    ) {
        composable("library") {
            LibraryScreen(
                onNavigateToDetail = { searchId ->
                    navController.navigate("detail/$searchId")
                }
            )
        }

        composable(
            route = "detail/{searchId}",
            arguments = listOf(
                navArgument("searchId") { type = NavType.LongType }
            )
        ) {
            SearchDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
