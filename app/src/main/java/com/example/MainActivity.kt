package com.example

import android.os.Bundle
import android.view.Choreographer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import com.example.core.configureAppCheck
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.BatteryScreen
import com.example.ui.screens.BoosterScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SpecsScreen
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.PulseBoostTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.AiAdvisorViewModel
import com.example.ui.viewmodel.OptimizerViewModel

const val ROUTE_OPTIMIZE = "optimize"
const val ROUTE_BOOSTER = "booster"
const val ROUTE_BATTERY = "battery"
const val ROUTE_SPECS = "specs"
const val ROUTE_AI = "ai_advisor"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val Destinations = listOf(
    NavDestinationItem(
        route = ROUTE_OPTIMIZE,
        labelRes = R.string.nav_optimize,
        selectedIcon = Icons.Rounded.RocketLaunch,
        unselectedIcon = Icons.Outlined.RocketLaunch,
        testTag = "nav_optimize"
    ),
    NavDestinationItem(
        route = ROUTE_BOOSTER,
        labelRes = R.string.nav_booster,
        selectedIcon = Icons.Rounded.Speed,
        unselectedIcon = Icons.Outlined.Speed,
        testTag = "nav_booster"
    ),
    NavDestinationItem(
        route = ROUTE_BATTERY,
        labelRes = R.string.nav_battery,
        selectedIcon = Icons.Rounded.BatteryChargingFull,
        unselectedIcon = Icons.Outlined.BatteryChargingFull,
        testTag = "nav_battery"
    ),
    NavDestinationItem(
        route = ROUTE_SPECS,
        labelRes = R.string.nav_specs,
        selectedIcon = Icons.Rounded.DeveloperBoard,
        unselectedIcon = Icons.Outlined.DeveloperBoard,
        testTag = "nav_specs"
    ),
    NavDestinationItem(
        route = ROUTE_AI,
        labelRes = R.string.nav_ai,
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_ai"
    )
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            PulseBoostTheme {
                val optimizerVm: OptimizerViewModel = viewModel()
                val aiVm: AiAdvisorViewModel = viewModel()

                val settings by optimizerVm.settings.collectAsStateWithLifecycle()
                val display by optimizerVm.displayTelemetry.collectAsStateWithLifecycle()

                // Apply preferred refresh rate & cooling brightness smoothly to active Window
                LaunchedEffect(settings.preferredRefreshHz, settings.coolingModeActive, display.supportedModes) {
                    val params = window.attributes
                    if (settings.preferredRefreshHz > 0f) {
                        val targetMode = display.supportedModes.minByOrNull {
                            kotlin.math.abs(it.refreshRateHz - settings.preferredRefreshHz)
                        }
                        if (targetMode != null) {
                            params.preferredDisplayModeId = targetMode.modeId
                        }
                        params.preferredRefreshRate = settings.preferredRefreshHz
                    } else {
                        val maxMode = display.supportedModes.maxByOrNull { it.refreshRateHz }
                        if (maxMode != null) {
                            params.preferredDisplayModeId = maxMode.modeId
                            params.preferredRefreshRate = maxMode.refreshRateHz
                        }
                    }
                    params.screenBrightness = if (settings.coolingModeActive) 0.35f else -1f
                    window.attributes = params
                }

                // Live Choreographer FPS & Frame-Time telemetry callback
                DisposableEffect(Unit) {
                    val choreographer = Choreographer.getInstance()
                    var lastFrameNanos = 0L
                    var frameCount = 0
                    var windowStartNanos = 0L

                    val callback = object : Choreographer.FrameCallback {
                        override fun doFrame(frameTimeNanos: Long) {
                            if (windowStartNanos == 0L) {
                                windowStartNanos = frameTimeNanos
                                lastFrameNanos = frameTimeNanos
                            }
                            frameCount++
                            val elapsedNs = frameTimeNanos - windowStartNanos
                            if (elapsedNs >= 500_000_000L) {
                                val fps = (frameCount * 1_000_000_000f) / elapsedNs.toFloat()
                                val avgFrameMs = (elapsedNs / frameCount.toFloat()) / 1_000_000f
                                optimizerVm.updateLiveFps(fps, avgFrameMs)
                                frameCount = 0
                                windowStartNanos = frameTimeNanos
                            }
                            lastFrameNanos = frameTimeNanos
                            choreographer.postFrameCallback(this)
                        }
                    }
                    choreographer.postFrameCallback(callback)
                    onDispose {
                        choreographer.removeFrameCallback(callback)
                    }
                }

                PulseBoostAppShell(
                    optimizerVm = optimizerVm,
                    aiVm = aiVm
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PulseBoostAppShell(
    optimizerVm: OptimizerViewModel,
    aiVm: AiAdvisorViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_OPTIMIZE
    val snackbarHostState = remember { SnackbarHostState() }

    val performanceScore by optimizerVm.performanceScore.collectAsStateWithLifecycle()
    val ram by optimizerVm.ramTelemetry.collectAsStateWithLifecycle()
    val storage by optimizerVm.storageTelemetry.collectAsStateWithLifecycle()
    val junk by optimizerVm.junkScanResult.collectAsStateWithLifecycle()
    val display by optimizerVm.displayTelemetry.collectAsStateWithLifecycle()
    val battery by optimizerVm.batteryTelemetry.collectAsStateWithLifecycle()
    val specs by optimizerVm.deviceSpecs.collectAsStateWithLifecycle()
    val apps by optimizerVm.launchableApps.collectAsStateWithLifecycle()
    val settings by optimizerVm.settings.collectAsStateWithLifecycle()
    val lastRamBoost by optimizerVm.lastRamBoostResult.collectAsStateWithLifecycle()
    val statusMessage by optimizerVm.statusMessage.collectAsStateWithLifecycle()
    val isMasterBoosting by optimizerVm.isMasterBoosting.collectAsStateWithLifecycle()
    val liveFps by optimizerVm.liveFps.collectAsStateWithLifecycle()
    val liveFrameTimeMs by optimizerVm.liveFrameTimeMs.collectAsStateWithLifecycle()

    val aiMode by aiVm.selectedMode.collectAsStateWithLifecycle()
    val quickPulseState by aiVm.quickPulseState.collectAsStateWithLifecycle()
    val aiChatState by aiVm.chatState.collectAsStateWithLifecycle()
    val aiMessages by aiVm.messages.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            optimizerVm.clearStatusMessage()
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        val activeTitle = when (currentRoute) {
                            ROUTE_OPTIMIZE -> "PulseBoost"
                            ROUTE_BOOSTER -> "Perlicin Layar & Game"
                            ROUTE_BATTERY -> "Monitor Suhu Baterai"
                            ROUTE_SPECS -> "Spesifikasi Perangkat"
                            ROUTE_AI -> "Asisten Performa AI"
                            else -> "PulseBoost"
                        }
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DeepSpace.copy(alpha = 0.96f)
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = DeepSpace,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        Destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
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
                        containerColor = DeepSpace,
                        modifier = Modifier
                            .fillMaxHeight()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Destinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag),
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_OPTIMIZE
                    ) {
                        composable(ROUTE_OPTIMIZE) {
                            DashboardScreen(
                                performanceScore = performanceScore,
                                ram = ram,
                                storage = storage,
                                junk = junk,
                                battery = battery,
                                display = display,
                                liveFps = liveFps,
                                settings = settings,
                                isMasterBoosting = isMasterBoosting,
                                quickPulseState = quickPulseState,
                                onMasterBoost = { optimizerVm.boostAll() },
                                onScanJunk = { optimizerVm.scanJunk() },
                                onCleanJunk = { optimizerVm.cleanJunk() },
                                onToggleJunkCategory = { optimizerVm.toggleJunkCategory(it) },
                                onSelectAutoCleanInterval = { optimizerVm.setAutoCleanInterval(it) },
                                onRunQuickAiPulse = {
                                    aiVm.runQuickPulse(optimizerVm.buildTelemetrySummaryForAi())
                                },
                                onOpenAiAdvisor = { navigateTo(ROUTE_AI) },
                                onOpenSystemStorageCleaner = { optimizerVm.getSystemStorageClearIntent() }
                            )
                        }
                        composable(ROUTE_BOOSTER) {
                            BoosterScreen(
                                display = display,
                                liveFps = liveFps,
                                liveFrameTimeMs = liveFrameTimeMs,
                                ram = ram,
                                lastRamBoost = lastRamBoost,
                                settings = settings,
                                apps = apps,
                                onSelectRefreshRate = { optimizerVm.selectRefreshRate(it) },
                                onSelectGameProfile = { optimizerVm.selectGameProfile(it) },
                                onBoostRam = { optimizerVm.boostRam() },
                                onOpenDisplaySettings = { optimizerVm.getDisplaySettingsIntent() },
                                onToggleAddedGame = { optimizerVm.toggleAddedGame(it) },
                                onRemoveAddedGame = { optimizerVm.removeAddedGame(it) },
                                onBoostForGame = { optimizerVm.boostForGame(it) },
                                onRefreshApps = { optimizerVm.refreshLaunchableApps() }
                            )
                        }
                        composable(ROUTE_BATTERY) {
                            BatteryScreen(
                                battery = battery,
                                settings = settings,
                                onSetThermalAlert = { optimizerVm.setThermalAlertCelsius(it) },
                                onToggleCoolingMode = { optimizerVm.toggleCoolingMode() }
                            )
                        }
                        composable(ROUTE_SPECS) {
                            SpecsScreen(
                                specs = specs,
                                ram = ram,
                                storage = storage,
                                display = display
                            )
                        }
                        composable(ROUTE_AI) {
                            AiAdvisorScreen(
                                selectedMode = aiMode,
                                chatState = aiChatState,
                                messages = aiMessages,
                                onSelectMode = { aiVm.selectMode(it) },
                                onSendPrompt = { prompt ->
                                    aiVm.sendQuery(prompt, optimizerVm.buildTelemetrySummaryForAi())
                                },
                                onClearChat = { aiVm.clearChat() }
                            )
                        }
                    }
                }
            }
        }
    }
}
