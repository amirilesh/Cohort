package com.cohort.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cohort.ui.MainScreen
import com.cohort.ui.SplashScreen
import com.cohort.ui.WelcomeScreen
import com.cohort.ui.studycard.StudyCardScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(onDone = {
                navController.navigate("welcome") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("welcome") {
            WelcomeScreen(onEnter = {
                navController.navigate("main") {
                    popUpTo("welcome") { inclusive = true }
                }
            })
        }

        composable("main") {
            MainScreen(
                onGenerateCard = { doi ->
                    val encoded = URLEncoder.encode(doi, "UTF-8")
                    navController.navigate("studycard/$encoded")
                }
            )
        }

        composable(
            route = "studycard/{doi}",
            arguments = listOf(navArgument("doi") { type = NavType.StringType }),
        ) { backStackEntry ->
            val doi = URLDecoder.decode(
                backStackEntry.arguments?.getString("doi") ?: "",
                "UTF-8",
            )
            StudyCardScreen(
                doi = doi,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
