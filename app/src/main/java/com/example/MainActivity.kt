package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.StudioViewModel
import com.example.ui.screens.BrandsScreen
import com.example.ui.screens.OverlaysScreen
import com.example.ui.screens.StreamsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.AsianPressCrimson
import com.example.ui.theme.AsianPressLiveTheme

const val ROUTE_STUDIO = "studio"
const val ROUTE_BRANDS = "brands"
const val ROUTE_OVERLAYS = "overlays"
const val ROUTE_STREAMS = "streams"

private data class StudioDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val studioDestinations = listOf(
    StudioDestination(ROUTE_STUDIO, R.string.nav_studio, Icons.Filled.Videocam, Icons.Outlined.Videocam),
    StudioDestination(ROUTE_BRANDS, R.string.nav_brands, Icons.Filled.Badge, Icons.Outlined.Badge),
    StudioDestination(ROUTE_OVERLAYS, R.string.nav_overlays, Icons.Filled.Layers, Icons.Outlined.Layers),
    StudioDestination(ROUTE_STREAMS, R.string.nav_streams, Icons.Filled.Radio, Icons.Outlined.Radio)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsianPressLiveTheme(darkTheme = true) {
                AsianPressLiveApp()
            }
        }
    }
}

@Composable
fun AsianPressLiveApp(
    viewModel: StudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_STUDIO
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        studioDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AsianPressCrimson,
                                    indicatorColor = AsianPressCrimson
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        studioDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val label = stringResource(dest.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = AsianPressCrimson,
                                    indicatorColor = AsianPressCrimson
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_STUDIO,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    composable(ROUTE_STUDIO) {
                        StudioScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToStreams = { navigateTo(ROUTE_STREAMS) }
                        )
                    }
                    composable(ROUTE_BRANDS) {
                        BrandsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBackToStudio = { navigateTo(ROUTE_STUDIO) }
                        )
                    }
                    composable(ROUTE_OVERLAYS) {
                        OverlaysScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onLoadToStudio = { navigateTo(ROUTE_STUDIO) }
                        )
                    }
                    composable(ROUTE_STREAMS) {
                        StreamsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBackToStudio = { navigateTo(ROUTE_STUDIO) }
                        )
                    }
                }
            }
        }
    }
}
