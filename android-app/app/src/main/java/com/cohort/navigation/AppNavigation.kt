package com.cohort.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cohort.data.model.RecentStudyCard
import com.cohort.ui.MainScreen
import com.cohort.ui.SplashScreen
import com.cohort.ui.WelcomeScreen
import com.cohort.ui.studycard.StudyCardDetailScreen
import com.cohort.ui.studycard.StudyCardScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder

private val navJson = Json { ignoreUnknownKeys = true }

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
                },
                onOpenStudyCard = { card ->
                    val json = URLEncoder.encode(
                        navJson.encodeToString(card),
                        "UTF-8",
                    )
                    navController.navigate("studycard/history/$json")
                },
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

        composable(
            route = "studycard/history/{cardJson}",
            arguments = listOf(navArgument("cardJson") { type = NavType.StringType }),
        ) { backStackEntry ->
            val cardJson = URLDecoder.decode(
                backStackEntry.arguments?.getString("cardJson") ?: "",
                "UTF-8",
            )
            val card = navJson.decodeFromString<RecentStudyCard>(cardJson)
            StudyCardDetailScreen(
                card = card,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
