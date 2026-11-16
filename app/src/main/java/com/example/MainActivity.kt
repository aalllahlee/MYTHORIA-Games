package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.engine3d.GameGLSurfaceView
import com.example.game.model.AppLanguage
import com.example.game.model.StringsProvider
import com.example.ui.screens.*
import com.example.ui.theme.DontFallTheme
import com.example.viewmodel.GameScreen
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var glView: GameGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBarsForLandscapeGame()

        setContent {
            DontFallTheme {
                val screen by viewModel.screen.collectAsStateWithLifecycle()
                val splashElapsedSec by viewModel.splashElapsedSec.collectAsStateWithLifecycle()
                val loadingProgress by viewModel.loadingProgress.collectAsStateWithLifecycle()
                val profile by viewModel.profile.collectAsStateWithLifecycle()
                val levelsProgress by viewModel.levelsProgress.collectAsStateWithLifecycle()
                val worldsProgress by viewModel.worldsProgress.collectAsStateWithLifecycle()
                val ownedItems by viewModel.ownedItems.collectAsStateWithLifecycle()
                val achievements by viewModel.achievements.collectAsStateWithLifecycle()
                val missions by viewModel.missions.collectAsStateWithLifecycle()
                val hud by viewModel.hud.collectAsStateWithLifecycle()
                val isPausedMenuOpen by viewModel.isPausedMenuOpen.collectAsStateWithLifecycle()
                val victorySummary by viewModel.victorySummary.collectAsStateWithLifecycle()
                val showFailOverlay by viewModel.showFailOverlay.collectAsStateWithLifecycle()
                val showModeDialog by viewModel.showModeDialog.collectAsStateWithLifecycle()

                val language = runCatching { AppLanguage.valueOf(profile.language) }.getOrDefault(AppLanguage.EN)
                val strings = remember(language) { StringsProvider.forLanguage(language) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Persistent OpenGL ES 3.0 3D Engine behind GameLoading, Menu, & Gameplay
                    if (screen != GameScreen.Splash) {
                        AndroidView(
                            factory = { ctx ->
                                GameGLSurfaceView(ctx, viewModel.physicsWorld) { snap ->
                                    viewModel.updateHudSnapshot(snap)
                                }.also { glView = it }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Crossfade(targetState = screen, label = "screen_crossfade") { current ->
                        when (current) {
                            GameScreen.Splash -> StudioSplashScreen(elapsedSec = splashElapsedSec)

                            GameScreen.GameLoading -> GameLoadingScreen(progress = loadingProgress)

                            GameScreen.MainMenu -> MainMenuScreen(
                                profile = profile,
                                levelsProgress = levelsProgress,
                                missions = missions,
                                strings = strings,
                                showModeDialog = showModeDialog,
                                onShowModeDialog = viewModel::openModeDialog,
                                onPlayMode = { mode, lvl -> viewModel.startLevel(lvl, mode) },
                                onNavigate = viewModel::navigateTo
                            )

                            GameScreen.LevelSelect -> LevelSelectScreen(
                                levelsProgress = levelsProgress,
                                worldsProgress = worldsProgress,
                                strings = strings,
                                onPlayLevel = { lvl, mode -> viewModel.startLevel(lvl, mode) },
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.Characters -> CharacterSelectScreen(
                                profile = profile,
                                ownedIds = ownedItems,
                                strings = strings,
                                onSelectOrBuySkin = viewModel::selectOrBuySkin,
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.Shop -> ShopScreen(
                                profile = profile,
                                ownedIds = ownedItems,
                                strings = strings,
                                onBuyOrEquipItem = viewModel::buyOrEquipShopItem,
                                onBuyOrSelectSkin = viewModel::selectOrBuySkin,
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.Settings -> SettingsScreen(
                                profile = profile,
                                strings = strings,
                                onUpdateProfile = viewModel::updateSettings,
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.Achievements -> AchievementsScreen(
                                achievements = achievements,
                                strings = strings,
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.Missions -> MissionsScreen(
                                profile = profile,
                                missions = missions,
                                language = language,
                                strings = strings,
                                onClaimMission = viewModel::claimMission,
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.About -> AboutCreditsScreen(
                                profile = profile,
                                strings = strings,
                                onBack = { viewModel.navigateTo(GameScreen.MainMenu) }
                            )

                            GameScreen.Playing -> {
                                GameplayHud(
                                    hud = hud,
                                    profile = profile,
                                    missions = missions,
                                    language = language,
                                    strings = strings,
                                    onJoystickMove = { jx, jy ->
                                        viewModel.physicsWorld.joyX = jx
                                        viewModel.physicsWorld.joyY = jy
                                    },
                                    onCameraDrag = { dx, dy -> viewModel.physicsWorld.addCameraDrag(dx, dy) },
                                    onJumpPress = { viewModel.physicsWorld.triggerJump() },
                                    onJumpRelease = { viewModel.physicsWorld.releaseJump() },
                                    onDive = { viewModel.physicsWorld.triggerDive() },
                                    onDash = { viewModel.physicsWorld.triggerDash() },
                                    onPause = { viewModel.togglePause(true) }
                                )

                                if (isPausedMenuOpen) {
                                    PauseOverlay(
                                        strings = strings,
                                        hasCheckpoint = hud.hasCheckpoint,
                                        onResume = { viewModel.togglePause(false) },
                                        onRestart = viewModel::restartCurrentLevel,
                                        onCheckpoint = viewModel::respawnAtCheckpoint,
                                        onSettings = {
                                            viewModel.togglePause(false)
                                            viewModel.navigateTo(GameScreen.Settings)
                                        },
                                        onMainMenu = { viewModel.navigateTo(GameScreen.MainMenu) }
                                    )
                                }

                                victorySummary?.let { summary ->
                                    VictoryOverlay(
                                        summary = summary,
                                        strings = strings,
                                        onNextLevel = { viewModel.startLevel(summary.levelNumber + 1, summary.mode) },
                                        onReplay = viewModel::restartCurrentLevel,
                                        onMainMenu = { viewModel.navigateTo(GameScreen.MainMenu) }
                                    )
                                }

                                if (showFailOverlay && victorySummary == null) {
                                    FailureOverlay(
                                        strings = strings,
                                        hasCheckpoint = hud.hasCheckpoint,
                                        mode = hud.mode,
                                        distanceM = hud.distanceM,
                                        onRetry = viewModel::restartCurrentLevel,
                                        onCheckpoint = viewModel::respawnAtCheckpoint,
                                        onMainMenu = { viewModel.navigateTo(GameScreen.MainMenu) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hideSystemBarsForLandscapeGame() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    override fun onPause() {
        super.onPause()
        glView?.onPause()
        viewModel.soundEngine.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView?.onResume()
        viewModel.soundEngine.onResume()
    }
}
