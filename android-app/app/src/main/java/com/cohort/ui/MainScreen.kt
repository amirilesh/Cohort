package com.cohort.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cohort.data.model.RecentStudyCard
import com.cohort.ui.history.HistoryScreen
import com.cohort.ui.search.SearchScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem("search",  "Search",  Icons.Filled.Search,   Icons.Outlined.Search),
    BottomNavItem("history", "History", Icons.Filled.History,  Icons.Outlined.History),
    BottomNavItem("saved",   "Saved",   Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
)

@Composable
fun MainScreen(
    onGenerateCard: (String) -> Unit,
    onOpenStudyCard: (RecentStudyCard) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination
                        ?.hierarchy
                        ?.any { it.route == item.route } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "search",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("search") {
                SearchScreen(onGenerateCard = onGenerateCard)
            }
            composable("history") {
                HistoryScreen(onCardClick = onOpenStudyCard)
            }
            composable("saved") {
                ComingSoonScreen(
                    title = "Saved",
                    subtitle = "Save study cards to read later",
                )
            }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = "Coming soon",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
