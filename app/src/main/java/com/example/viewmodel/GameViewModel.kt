package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.db.*
import com.example.game.engine3d.GamePhysicsWorld
import com.example.game.engine3d.GameWorldListener
import com.example.game.engine3d.HudSnapshot
import com.example.game.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface GameScreen {
    data object Splash : GameScreen
    data object GameLoading : GameScreen
    data object MainMenu : GameScreen
    data object LevelSelect : GameScreen
    data object Characters : GameScreen
    data object Shop : GameScreen
    data object Settings : GameScreen
    data object Achievements : GameScreen
    data object Missions : GameScreen
    data object About : GameScreen
    data object Playing : GameScreen
}

data class VictorySummary(
    val levelNumber: Int,
    val levelTitle: String,
    val mode: GameMode,
    val stars: Int,
    val timeMs: Long,
    val bestTimeMs: Long,
    val coinsEarned: Int,
    val distanceM: Float,
    val obstaclesPassed: Int,
    val racePlacement: Int = 1,
    val totalRacers: Int = 1,
    val xpEarned: Int = 0,
    val goalTimeMs: Long = 0L,
    val goalMet: Boolean = true,
    val newlyUnlockedWorldId: Int? = null,
    val newlyUnlockedWorldName: String? = null,
    val canAdvanceNextLevel: Boolean = true
)

class GameViewModel(application: Application) : AndroidViewModel(application), GameWorldListener {

    private val db = AppDatabase.getInstance(application)
    private val repo = GameRepository(db.gameDao())
    val soundEngine = SoundEngine()
    val physicsWorld = GamePhysicsWorld(this)

    private val _screen = MutableStateFlow<GameScreen>(GameScreen.Splash)
    val screen: StateFlow<GameScreen> = _screen.asStateFlow()

    private val _splashElapsedSec = MutableStateFlow(0f)
    val splashElapsedSec: StateFlow<Float> = _splashElapsedSec.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _hud = MutableStateFlow(HudSnapshot())
    val hud: StateFlow<HudSnapshot> = _hud.asStateFlow()

    private val _isPausedMenuOpen = MutableStateFlow(false)
    val isPausedMenuOpen: StateFlow<Boolean> = _isPausedMenuOpen.asStateFlow()

    private val _victorySummary = MutableStateFlow<VictorySummary?>(null)
    val victorySummary: StateFlow<VictorySummary?> = _victorySummary.asStateFlow()

    private val _showFailOverlay = MutableStateFlow(false)
    val showFailOverlay: StateFlow<Boolean> = _showFailOverlay.asStateFlow()

    private val _showModeDialog = MutableStateFlow(false)
    val showModeDialog: StateFlow<Boolean> = _showModeDialog.asStateFlow()

    val profile: StateFlow<PlayerProfileEntity> = repo.profileFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerProfileEntity())

    val levelsProgress: StateFlow<List<LevelProgressEntity>> = repo.levelsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val worldsProgress: StateFlow<List<WorldProgressEntity>> = repo.worldsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val ownedItems: StateFlow<Set<String>> = repo.ownedItemIdsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, setOf("red_hero", "hat_none", "shoe_default", "trail_dust", "victory_dance"))

    val achievements: StateFlow<List<AchievementEntity>> = repo.achievementsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val missions: StateFlow<List<MissionEntity>> = repo.missionsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var activeTimeAttack = false
    private var activeLevelRef: LevelDef = LevelCatalog.getLevel(1)
    private var reportedJumpsForRun = 0
    private var reportedCoinsForRun = 0

    init {
        startSplashInitialization()
        observeProfileSync()
    }

    private fun observeProfileSync() {
        viewModelScope.launch {
            profile.collect { p ->
                soundEngine.musicVolume = p.musicVolume
                soundEngine.sfxVolume = p.sfxVolume
                physicsWorld.cameraSensitivity = p.cameraSensitivity
                physicsWorld.joystickSensitivity = p.joystickSensitivity
                physicsWorld.dashUnlocked = p.dashUnlocked
                physicsWorld.selectedSkin = GameCatalog.skins.find { it.id == p.selectedSkinId } ?: GameCatalog.skins[0]
                physicsWorld.selectedHatId = p.selectedHatId
                physicsWorld.selectedShoeId = p.selectedShoeId
                physicsWorld.selectedTrailId = p.selectedTrailId
                physicsWorld.graphicsQuality = runCatching { GraphicsQuality.valueOf(p.graphicsQuality) }.getOrDefault(GraphicsQuality.HIGH)
            }
        }
    }

    private fun startSplashInitialization() {
        viewModelScope.launch {
            var assetsReady = false
            val loaderJob = launch(Dispatchers.IO) {
                repo.ensureInitialized()
                LevelCatalog.allLevels // warm lazy catalog
                soundEngine.initBuffers()
                assetsReady = true
            }

            // 8.0-second cinematic studio intro (60Hz smooth timeline, non-blocking)
            val startNs = System.nanoTime()
            var sweepPlayed = false
            var chordPlayed = false
            var completePlayed = false

            while (true) {
                val sec = (System.nanoTime() - startNs) / 1_000_000_000f
                _splashElapsedSec.value = sec
                if (!sweepPlayed && sec >= 2.0f && assetsReady) {
                    sweepPlayed = true
                    soundEngine.playSfx("studio_sweep")
                }
                if (!chordPlayed && sec >= 3.6f && assetsReady) {
                    chordPlayed = true
                    soundEngine.playSfx("studio_intro")
                }
                if (!completePlayed && sec >= 6.0f && assetsReady) {
                    completePlayed = true
                    soundEngine.playSfx("studio_complete")
                }
                if (sec >= 8.0f && assetsReady) break
                delay(16L)
            }
            loaderJob.join()

            // Smooth transition into DON'T FALL game loading screen while 3D showcase prepares
            physicsWorld.startMenuShowcase()
            _screen.value = GameScreen.GameLoading

            val loadStartNs = System.nanoTime()
            while (true) {
                val frac = ((System.nanoTime() - loadStartNs) / 1_500_000_000f).coerceIn(0f, 1f)
                _loadingProgress.value = frac
                if (frac >= 1f) break
                delay(16L)
            }

            soundEngine.setMusicTheme(SoundEngine.MusicTheme.MENU)
            _screen.value = GameScreen.MainMenu
        }
    }

    fun updateHudSnapshot(snapshot: HudSnapshot) {
        _hud.value = snapshot
    }

    fun openModeDialog(show: Boolean) {
        soundEngine.playSfx("click")
        _showModeDialog.value = show
    }

    fun navigateTo(target: GameScreen) {
        soundEngine.playSfx("click")
        _showModeDialog.value = false
        if (target == GameScreen.MainMenu) {
            _isPausedMenuOpen.value = false
            _victorySummary.value = null
            _showFailOverlay.value = false
            physicsWorld.startMenuShowcase()
            soundEngine.setMusicTheme(SoundEngine.MusicTheme.MENU)
        }
        _screen.value = target
    }

    fun playNextUnlockedCareerLevel() {
        val unlockedLevels = computeCurrentUnlockedLevels()
        val highestUnlocked = unlockedLevels.maxOrNull() ?: 1
        startLevel(highestUnlocked, GameMode.CAREER)
    }

    private fun computeCurrentUnlockedLevels(): Set<Int> {
        val goalMap = levelsProgress.value.associate { it.levelNumber to it.goalMet }
        return WorldProgressionGraph.computeUnlockedLevels(goalMap)
    }

    private fun isLevelAccessibleInGraph(levelNumber: Int): Boolean {
        val clamped = levelNumber.coerceIn(1, 50)
        val unlockedLevels = computeCurrentUnlockedLevels()
        val levelRowUnlocked = levelsProgress.value.find { it.levelNumber == clamped }?.unlocked
        val worldId = LevelCatalog.getWorldForLevel(clamped).id
        val worldRowUnlocked = worldsProgress.value.find { it.worldId == worldId }?.unlocked
        return (clamped in unlockedLevels) && (levelRowUnlocked ?: (clamped == 1)) && (worldRowUnlocked ?: (worldId == 1))
    }

    fun startLevel(levelNumber: Int, mode: GameMode) {
        if ((mode == GameMode.CAREER || mode == GameMode.TIME_ATTACK || mode == GameMode.RIVAL_RACE) &&
            !isLevelAccessibleInGraph(levelNumber)
        ) {
            soundEngine.playSfx("fail")
            onHaptic(true)
            return
        }

        soundEngine.playSfx("click")
        _showModeDialog.value = false
        _isPausedMenuOpen.value = false
        _victorySummary.value = null
        _showFailOverlay.value = false
        activeTimeAttack = (mode == GameMode.TIME_ATTACK)
        reportedJumpsForRun = 0
        reportedCoinsForRun = 0

        val levelDef = when (mode) {
            GameMode.ENDLESS -> LevelCatalog.generateEndlessLevel(System.currentTimeMillis())
            GameMode.DAILY_CHALLENGE -> LevelCatalog.generateDailyChallengeLevel(System.currentTimeMillis() / 86_400_000L)
            GameMode.PRACTICE -> LevelCatalog.generatePracticeLevel()
            else -> LevelCatalog.getLevel(levelNumber)
        }
        activeLevelRef = levelDef
        physicsWorld.loadLevel(levelDef, mode)
        soundEngine.setMusicTheme(SoundEngine.MusicTheme.GAMEPLAY)
        _screen.value = GameScreen.Playing
    }

    fun claimMission(missionId: String) {
        viewModelScope.launch {
            val ok = repo.claimMission(missionId)
            soundEngine.playSfx(if (ok) "victory" else "click")
            onHaptic(ok)
        }
    }

    fun togglePause(paused: Boolean) {
        soundEngine.playSfx("click")
        physicsWorld.isPaused = paused
        _isPausedMenuOpen.value = paused
    }

    fun restartCurrentLevel() {
        startLevel(activeLevelRef.number.coerceAtLeast(1), physicsWorld.currentMode)
    }

    fun respawnAtCheckpoint() {
        soundEngine.playSfx("click")
        _showFailOverlay.value = false
        _isPausedMenuOpen.value = false
        physicsWorld.respawnAtCheckpoint()
    }

    fun selectOrBuySkin(skinId: String) {
        viewModelScope.launch {
            if (ownedItems.value.contains(skinId)) {
                repo.selectSkin(skinId)
                soundEngine.playSfx("click")
            } else {
                val ok = repo.purchaseSkin(skinId)
                soundEngine.playSfx(if (ok) "coin" else "fail")
                onHaptic(ok)
            }
        }
    }

    fun buyOrEquipShopItem(itemId: String, category: ShopCategory) {
        viewModelScope.launch {
            if (ownedItems.value.contains(itemId)) {
                repo.equipShopItem(itemId, category)
                soundEngine.playSfx("click")
            } else {
                val ok = repo.purchaseShopItem(itemId)
                soundEngine.playSfx(if (ok) "coin" else "fail")
                onHaptic(ok)
            }
        }
    }

    fun updateSettings(transform: (PlayerProfileEntity) -> PlayerProfileEntity) {
        viewModelScope.launch {
            repo.updateProfile(transform)
        }
    }

    override fun onSfx(name: String) {
        soundEngine.playSfx(name)
    }

    override fun onHaptic(strong: Boolean) {
        if (!profile.value.hapticsEnabled) return
        val ctx = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 45L else 18L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (strong) 45L else 18L)
        }
    }

    override fun onLevelWon(
        levelNumber: Int,
        timeMs: Long,
        coinsCollected: Int,
        distanceM: Float,
        obstaclesPassed: Int,
        raceRank: Int,
        totalRacers: Int,
        jumpsInRun: Int
    ) {
        soundEngine.setMusicTheme(SoundEngine.MusicTheme.VICTORY)
        viewModelScope.launch {
            val mode = physicsWorld.currentMode
            val deltaJumps = (jumpsInRun - reportedJumpsForRun).coerceAtLeast(0)
            val deltaCoins = (coinsCollected - reportedCoinsForRun).coerceAtLeast(0)
            reportedJumpsForRun = jumpsInRun
            reportedCoinsForRun = coinsCollected

            val stars: Int
            val totalCoins: Int
            val xpEarned: Int
            var bestTime = timeMs
            var goalTimeMs = 0L
            var goalMet = true
            var newlyUnlockedWorldId: Int? = null
            var canAdvance = false

            when (mode) {
                GameMode.CAREER, GameMode.TIME_ATTACK -> {
                    val outcome = repo.recordCareerLevelWin(
                        levelNumber,
                        timeMs,
                        activeLevelRef.parTimeSec,
                        deltaCoins,
                        activeLevelRef.rewardCoins,
                        deltaJumps
                    )
                    stars = outcome.stars
                    totalCoins = outcome.coinsEarned
                    xpEarned = outcome.xpEarned
                    goalTimeMs = outcome.goalTimeMs
                    goalMet = outcome.goalMetThisRun
                    newlyUnlockedWorldId = outcome.newlyUnlockedWorldId
                    canAdvance = levelNumber < 50 && outcome.goalMetStored && repo.isLevelUnlocked(levelNumber + 1)
                    val updatedLvl = levelsProgress.value.find { it.levelNumber == levelNumber }
                    bestTime = if (updatedLvl != null && updatedLvl.bestTimeMs > 0L) minOf(updatedLvl.bestTimeMs, timeMs) else timeMs
                }
                GameMode.RIVAL_RACE -> {
                    val outcome = repo.recordRivalRaceResult(
                        levelNumber = levelNumber,
                        placement = raceRank,
                        timeMs = timeMs,
                        parTimeSec = activeLevelRef.parTimeSec,
                        coinsCollected = deltaCoins,
                        jumpsInRun = deltaJumps
                    )
                    stars = outcome.stars
                    totalCoins = outcome.coinsEarned
                    xpEarned = outcome.xpEarned
                    goalTimeMs = outcome.goalTimeMs
                    goalMet = outcome.goalMetThisRun
                    newlyUnlockedWorldId = outcome.newlyUnlockedWorldId
                    canAdvance = levelNumber < 50 && outcome.goalMetStored && repo.isLevelUnlocked(levelNumber + 1)
                }
                GameMode.DAILY_CHALLENGE -> {
                    val day = System.currentTimeMillis() / 86_400_000L
                    repo.recordDailyChallengeWin(day, deltaCoins, deltaJumps)
                    stars = 3
                    totalCoins = coinsCollected + 180
                    xpEarned = 450
                }
                GameMode.ENDLESS -> {
                    repo.recordEndlessRun(distanceM, timeMs, obstaclesPassed, deltaCoins, deltaJumps)
                    stars = 3
                    totalCoins = coinsCollected + (distanceM / 10f).toInt()
                    xpEarned = (distanceM * 4f).toInt().coerceAtLeast(30)
                }
                GameMode.PRACTICE -> {
                    repo.recordRunJumpsAndCoins(deltaCoins + 25, deltaJumps)
                    stars = 3
                    totalCoins = coinsCollected + 25
                    xpEarned = 120
                }
            }

            val unlockedWorldName = newlyUnlockedWorldId?.let { wId ->
                GameCatalog.worlds.find { it.id == wId }?.name
            }

            _victorySummary.value = VictorySummary(
                levelNumber = levelNumber,
                levelTitle = activeLevelRef.title,
                mode = mode,
                stars = stars,
                timeMs = timeMs,
                bestTimeMs = bestTime,
                coinsEarned = totalCoins,
                distanceM = distanceM,
                obstaclesPassed = obstaclesPassed,
                racePlacement = raceRank,
                totalRacers = totalRacers,
                xpEarned = xpEarned,
                goalTimeMs = goalTimeMs,
                goalMet = goalMet,
                newlyUnlockedWorldId = newlyUnlockedWorldId,
                newlyUnlockedWorldName = unlockedWorldName,
                canAdvanceNextLevel = canAdvance
            )
        }
    }

    override fun onPlayerFell() {
        val snap = _hud.value
        val deltaJumps = (snap.jumpsInRun - reportedJumpsForRun).coerceAtLeast(0)
        val deltaCoins = (snap.coinsCollected - reportedCoinsForRun).coerceAtLeast(0)
        reportedJumpsForRun = snap.jumpsInRun
        reportedCoinsForRun = snap.coinsCollected
        viewModelScope.launch {
            if (physicsWorld.currentMode == GameMode.ENDLESS) {
                repo.recordEndlessRun(snap.distanceM, snap.elapsedMs, snap.obstaclesPassed, deltaCoins, deltaJumps)
            } else {
                repo.recordRunJumpsAndCoins(deltaCoins, deltaJumps)
            }
        }
        _showFailOverlay.value = true
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
