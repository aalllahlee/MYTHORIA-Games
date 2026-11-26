package com.example.game.engine3d

import com.example.game.model.*
import kotlin.math.*
import kotlin.random.Random

data class HudSnapshot(
    val levelNumber: Int = 1,
    val levelTitle: String = "First Steps",
    val worldName: String = "Green Starter Island",
    val mode: GameMode = GameMode.CAREER,
    val progressFraction: Float = 0f,
    val coinsCollected: Int = 0,
    val elapsedMs: Long = 0L,
    val distanceM: Float = 0f,
    val obstaclesPassed: Int = 0,
    val dashCooldownFraction: Float = 0f,
    val checkpointReached: Int = 0,
    val totalCheckpoints: Int = 4,
    val checkpointBannerText: String = "",
    val isPlayerRespawning: Boolean = false,
    val respawnProgress: Float = 0f,
    val fps: Int = 60,
    val isVictory: Boolean = false,
    val isFailed: Boolean = false,
    val hasCheckpoint: Boolean = false,
    val survivalTier: Int = 1,
    val speedMultiplier: Float = 1f,
    val tierProgressFraction: Float = 0f,
    val complexityLabel: String = "WARMUP",
    val tierBannerText: String = "",
    val raceRank: Int = 1,
    val totalRacers: Int = 1,
    val jumpsInRun: Int = 0,
    val goalTimeMs: Long = 0L,
    val hazardWarningActive: Boolean = false,
    val shieldRemainingSec: Float = 0f,
    val magnetRemainingSec: Float = 0f,
    val wingsRemainingSec: Float = 0f
)

data class RuntimeRival(
    val id: Int,
    val name: String,
    val skin: SkinDef,
    val hatId: String,
    val shoeId: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var yaw: Float = 180f,
    var grounded: Boolean = false,
    var coyoteTimer: Float = 0f,
    var animState: CharAnimState = CharAnimState.IDLE,
    var animTime: Float = 0f,
    var squashY: Float = 1f,
    var hitStunTimer: Float = 0f,
    var jumpCooldown: Float = 0f,
    var baseSpeed: Float = 7.8f,
    var laneBiasX: Float = 0f,
    var respawnX: Float = 0f,
    var respawnY: Float = 1.8f,
    var respawnZ: Float = 1.5f,
    var finished: Boolean = false,
    var finishOrder: Int = 0,
    var finishTimeMs: Long = 0L,
    val checkpointState: PlayerCheckpointState = PlayerCheckpointState(playerId = "rival_$id")
)

data class RuntimePlatform(
    val def: PlatformDef,
    var curX: Float = def.x,
    var curY: Float = def.y,
    var curZ: Float = def.z,
    var prevX: Float = def.x,
    var prevY: Float = def.y,
    var prevZ: Float = def.z,
    var active: Boolean = true,
    var warningPulse: Float = 0f,
    var fallTimer: Float = -1f,
    var respawnTimer: Float = 0f
)

data class RuntimeCoin(
    val def: CoinDef,
    var curX: Float = def.x,
    var curY: Float = def.y,
    var curZ: Float = def.z,
    var collected: Boolean = false
)

data class RuntimePowerUp(
    val def: PowerUpDef,
    var collected: Boolean = false
)

data class RuntimeTreasureChest(
    val def: TreasureChestDef,
    var opened: Boolean = false,
    var openAnim: Float = 0f
)

data class Particle(
    var x: Float, var y: Float, var z: Float,
    var vx: Float, var vy: Float, var vz: Float,
    var life: Float, var maxLife: Float,
    var size: Float,
    var color: Rgba
)

interface GameWorldListener {
    fun onSfx(name: String)
    fun onHaptic(strong: Boolean = false)
    fun onLevelWon(
        levelNumber: Int,
        timeMs: Long,
        coinsCollected: Int,
        distanceM: Float,
        obstaclesPassed: Int,
        raceRank: Int,
        totalRacers: Int,
        jumpsInRun: Int,
        perfectLandings: Int = 0,
        secretPathFound: Boolean = false,
        narrowBridgesCrossed: Int = 0,
        dashUsed: Boolean = false,
        respawnCount: Int = 0,
        chestsOpened: Int = 0
    )
    fun onPlayerFell()
}

class GamePhysicsWorld(var listener: GameWorldListener? = null) {

    @Volatile var isMenuShowcase: Boolean = true
    @Volatile var isPaused: Boolean = false

    var currentMode: GameMode = GameMode.CAREER
    var currentLevel: LevelDef = LevelCatalog.getLevel(1)
    var currentWorld: WorldTheme = GameCatalog.worlds[0]

    val platforms = ArrayList<RuntimePlatform>()
    val coins = ArrayList<RuntimeCoin>()
    val powerUps = ArrayList<RuntimePowerUp>()
    val treasureChests = ArrayList<RuntimeTreasureChest>()
    val activeObstacles = ArrayList<ObstacleDef>()
    val activeCheckpoints = ArrayList<CheckpointDef>()
    val rivals = ArrayList<RuntimeRival>()
    val particles = ArrayList<Particle>(160)

    // Active 3D Power-Up Timers
    var shieldTimer = 0f
    var magnetTimer = 0f
    var wingsTimer = 0f

    // Personal Best Ghost Racer Recording & Playback
    private val currentRunGhostFrames = ArrayList<GhostFrame>(512)
    private var activeGhostFrames: List<GhostFrame> = emptyList()
    private var ghostRecordTimer = 0f
    var ghostActive = false
    var ghostX = 0f
    var ghostY = 1.6f
    var ghostZ = 1.5f
    var ghostYaw = 180f

    companion object {
        val bestGhostByLevel = HashMap<Int, List<GhostFrame>>()
    }

    // Per-checkpoint activation animation pulse timers (checkpointIndex -> remainingSec)
    val checkpointPulseTimers = HashMap<Int, Float>()

    // Independent Checkpoint & Respawn State for Local Player ("player_0")
    val playerCheckpointState = PlayerCheckpointState(playerId = "player_0")

    // Player physics state
    var px = 0f
    var py = 1.6f
    var pz = 1.5f
    var vx = 0f
    var vy = 0f
    var vz = 0f
    var playerYaw = 180f // Facing -Z (into course)
    var playerTurnLeanDeg = 0f // Subtle lateral body banking on turns
    var playerAccelPitchDeg = 0f // Subtle forward/backward pitch on acceleration/braking
    var pickupAuraTimer = 0f // Short non-obstructive waist aura on ability/reward pickup
    var pickupAuraColor: Rgba = Rgba.fromHex(0xFFD22B)
    var shadowFloorY = Float.NaN // Underlying platform surface Y for ground & airborne shadow
    var shadowHeightGap = 0f
    var grounded = false
    var coyoteTimer = 0f
    var jumpBufferTimer = 0f
    var diveTimer = 0f
    var diveCooldown = 0f
    var dashCooldown = 0f
    var hitStunTimer = 0f
    var landTimer = 0f
    var squashY = 1f
    var animState = CharAnimState.IDLE
    var animTime = 0f
    var worldTime = 0f
    var obstacleTime = 0f
    var platformTime = 0f
    var cameraShakeTimer = 0f
    var cameraShakeIntensity = 0f
    var hazardWarningActive = false
    private var footstepTimer = 0f
    private var slideSfxTimer = 0f
    private var warnSfxCooldown = 0f

    // Survival Progression & Run Stats
    var spawnX = 0f
    var spawnY = 1.8f
    var spawnZ = 1.5f
    var highestCheckpointIndex = 0
    var checkpointBannerTimer = 0f
    var checkpointBannerText = ""
    var timeAttackPenaltySec = 2.0f
    var endlessSurvivalRespawnsRemaining = 0
    var coinsCollected = 0
    var elapsedSec = 0f
    var maxDistanceM = 0f
    var obstaclesPassed = 0
    var jumpsInRun = 0
    var perfectLandingsInRun = 0
    var secretPathFoundInRun = false
    var chestsOpenedInRun = 0
    var narrowBridgesCrossedWithoutFall = 0
    var dashesUsedInRun = 0
    private var wasOnNarrowBridge = false
    private var lastNarrowBridgeZ = Float.NaN
    private var airbornePeakY = 0f
    private var ambientParticleTimer = 0f
    var survivalTier = 1
    var speedMultiplier = 1.0f
    var tierBannerTimer = 0f
    var tierBannerText = ""
    var runFinished = false
    var runFailed = false
    private var nextFinishOrder = 1
    private var playerFinishOrder = 0

    private var endlessNextY = 0f
    private var endlessNextZ = 0f
    private var endlessSegIndex = 0
    private var endlessRng = Random(1L)

    // Controls & Preferences (volatile for cross-thread UI -> GL)
    @Volatile var joyX = 0f
    @Volatile var joyY = 0f
    @Volatile var cameraYawOffset = 0f
    @Volatile var cameraPitchDeg = 22f
    @Volatile var cameraSensitivity = 1.0f
    @Volatile var joystickSensitivity = 1.0f
    @Volatile var cameraShakeEnabled = true
    @Volatile var dashUnlocked = false
    @Volatile var selectedSkin: SkinDef = GameCatalog.skins[0]
    @Volatile var selectedHatId: String = "hat_none"
    @Volatile var selectedShoeId: String = "shoe_default"
    @Volatile var selectedTrailId: String = "trail_dust"
    @Volatile var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH

    // Smooth camera coordinates
    var camX = 0f
    var camY = 5.5f
    var camZ = 9.5f
    var camLookX = 0f
    var camLookY = 1.5f
    var camLookZ = 0f

    private var trailTimer = 0f
    private var prevFloorMinY = -8.5f

    private val cmdLock = Any()
    private var pendingMenuShowcase = false
    private var pendingLevelLoad: Pair<LevelDef, GameMode>? = null
    private var pendingRespawn = false
    private var pendingJump = false
    private var pendingJumpRelease = false
    private var pendingDive = false
    private var pendingDash = false

    fun startMenuShowcase() {
        synchronized(cmdLock) {
            pendingMenuShowcase = true
            pendingLevelLoad = null
            pendingRespawn = false
        }
    }

    private fun applyMenuShowcase() {
        isMenuShowcase = true
        isPaused = false
        currentLevel = LevelCatalog.getLevel(1)
        currentWorld = GameCatalog.worlds[1]
        platforms.clear()
        coins.clear()
        powerUps.clear()
        treasureChests.clear()
        activeObstacles.clear()
        activeCheckpoints.clear()
        checkpointPulseTimers.clear()
        rivals.clear()
        particles.clear()
        shieldTimer = 0f
        magnetTimer = 0f
        wingsTimer = 0f
        ghostActive = false
        speedMultiplier = 1.0f
        survivalTier = 1
        obstacleTime = 0f
        platformTime = 0f
        px = 0f; py = 1.0f; pz = 0f
        playerYaw = 18f
        animState = CharAnimState.IDLE
        camX = 0f; camY = 3.2f; camZ = 7.2f
        camLookX = 0f; camLookY = 1.4f; camLookZ = -2f
    }

    fun loadLevel(level: LevelDef, mode: GameMode) {
        synchronized(cmdLock) {
            pendingLevelLoad = level to mode
            pendingMenuShowcase = false
            pendingRespawn = false
            pendingJump = false
            pendingJumpRelease = false
            pendingDive = false
            pendingDash = false
        }
    }

    private fun applyLoadLevel(level: LevelDef, mode: GameMode) {
        isMenuShowcase = false
        isPaused = false
        currentMode = mode
        currentLevel = level
        currentWorld = GameCatalog.worlds[(level.worldId - 1).coerceIn(0, GameCatalog.worlds.lastIndex)]

        platforms.clear()
        level.platforms.forEach { platforms += RuntimePlatform(it) }
        coins.clear()
        level.coins.forEach { coins += RuntimeCoin(it) }
        powerUps.clear()
        level.powerUps.forEach { powerUps += RuntimePowerUp(it) }
        treasureChests.clear()
        level.treasureChests.forEach { treasureChests += RuntimeTreasureChest(it) }
        activeObstacles.clear()
        activeObstacles.addAll(level.obstacles)
        activeCheckpoints.clear()
        activeCheckpoints.addAll(level.checkpoints)
        checkpointPulseTimers.clear()
        rivals.clear()
        particles.clear()
        shieldTimer = 0f
        magnetTimer = 0f
        wingsTimer = 0f
        currentRunGhostFrames.clear()
        ghostRecordTimer = 0f
        activeGhostFrames = synchronized(bestGhostByLevel) {
            bestGhostByLevel[level.number] ?: emptyList()
        }
        ghostActive = false

        spawnX = level.startX
        spawnY = level.startY
        spawnZ = level.startZ
        highestCheckpointIndex = 0
        checkpointBannerTimer = 0f
        checkpointBannerText = ""
        endlessSurvivalRespawnsRemaining = 0
        coinsCollected = 0
        elapsedSec = 0f
        maxDistanceM = 0f
        obstaclesPassed = 0
        jumpsInRun = 0
        perfectLandingsInRun = 0
        secretPathFoundInRun = false
        chestsOpenedInRun = 0
        narrowBridgesCrossedWithoutFall = 0
        dashesUsedInRun = 0
        wasOnNarrowBridge = false
        lastNarrowBridgeZ = Float.NaN
        airbornePeakY = level.startY
        survivalTier = 1
        speedMultiplier = 1.0f
        obstacleTime = 0f
        platformTime = 0f
        tierBannerTimer = 0f
        tierBannerText = ""
        runFinished = false
        runFailed = false
        nextFinishOrder = 1
        playerFinishOrder = 0

        // Reset local player's independent checkpoint state to Checkpoint 0 (Start Island)
        playerCheckpointState.apply {
            currentX = level.startX
            currentY = level.startY + 0.6f
            currentZ = level.startZ
            lastActivatedCheckpoint = 0
            activatedCheckpoints.clear()
            activatedCheckpoints.add(0)
            safeRespawnX = level.startX
            safeRespawnY = level.startY + 0.6f
            safeRespawnZ = level.startZ
            safeRespawnYaw = 180f
            isRespawning = false
            respawnTimerSec = 0f
            fallAnimTimerSec = 0f
            arrivalAnimTimerSec = 0f
            portalScale = 0f
            raceProgress = 0f
            finished = false
            respawnCount = 0
        }

        if (mode == GameMode.RIVAL_RACE) {
            spawnAiRivals(level)
        }

        endlessSegIndex = 8
        endlessNextY = platforms.lastOrNull()?.def?.y ?: 0f
        endlessNextZ = (platforms.minOfOrNull { it.def.z - it.def.d * 0.5f } ?: -6.5f) - 2.0f
        endlessRng = Random(System.nanoTime())

        prevFloorMinY = (level.platforms.minOfOrNull { it.y } ?: 0f) - 9.0f
        respawnPlayer(fullReset = true)
    }

    fun spawnAiRivals(level: LevelDef = currentLevel) {
        rivals.clear()
        val candidateSkins = GameCatalog.skins.filter { it.id != selectedSkin.id }
        val offsets = listOf(-2.4f, -0.9f, 0.9f, 2.4f)
        val speeds = listOf(8.25f, 7.85f, 7.50f, 7.15f)
        val hats = listOf("hat_crown", "hat_headband", "hat_visor", "hat_none")
        val shoes = listOf("shoe_gold", "shoe_rocket", "shoe_shadow", "shoe_default")
        for (i in 0 until 4) {
            val skin = candidateSkins[(i * 3) % candidateSkins.size]
            val sx = level.startX + offsets[i]
            val sy = level.startY + 0.5f
            val sz = level.startZ + (i % 2) * 0.6f
            val rival = RuntimeRival(
                id = i + 1,
                name = skin.name,
                skin = skin,
                hatId = hats[i],
                shoeId = shoes[i],
                x = sx,
                y = sy,
                z = sz,
                baseSpeed = speeds[i],
                laneBiasX = offsets[i] * 0.6f,
                respawnX = sx,
                respawnY = sy,
                respawnZ = sz,
                animTime = i * 0.45f
            )
            rival.checkpointState.apply {
                currentX = sx
                currentY = sy
                currentZ = sz
                lastActivatedCheckpoint = 0
                activatedCheckpoints.clear()
                activatedCheckpoints.add(0)
                safeRespawnX = sx
                safeRespawnY = sy
                safeRespawnZ = sz
                safeRespawnYaw = 180f
                isRespawning = false
                respawnTimerSec = 0f
                fallAnimTimerSec = 0f
                arrivalAnimTimerSec = 0f
                portalScale = 0f
                raceProgress = 0f
                finished = false
                respawnCount = 0
            }
            rivals += rival
        }
    }

    /**
     * Returns the independent [PlayerCheckpointState] for `"player_0"` or `"rival_1".."rival_4"`.
     */
    fun getRacerCheckpointState(playerId: String): PlayerCheckpointState? {
        if (playerId == "player_0") return playerCheckpointState
        val rival = rivals.firstOrNull { it.checkpointState.playerId == playerId || "rival_${it.id}" == playerId }
        return rival?.checkpointState
    }

    /**
     * Computes a guaranteed safe respawn position above the checkpoint platform,
     * ensuring the player never spawns inside an obstacle or off the platform edge.
     */
    fun findSafeCheckpointPosition(cp: CheckpointDef, laneOffsetX: Float = 0f): Triple<Float, Float, Float> {
        var safeX = cp.x + laneOffsetX.coerceIn(-1.8f, 1.8f)
        var safeY = cp.y + 0.62f
        val safeZ = cp.z

        // Snap to actual top of underlying checkpoint platform if present
        for (i in 0 until platforms.size) {
            val rp = platforms[i]
            if (!rp.active) continue
            if (abs(cp.x - rp.curX) <= rp.def.w * 0.5f && abs(cp.z - rp.curZ) <= rp.def.d * 0.5f) {
                safeY = maxOf(safeY, rp.curY + rp.def.h * 0.5f + 0.55f)
                val maxLat = (rp.def.w * 0.5f - 0.65f).coerceAtLeast(0f)
                safeX = (cp.x + laneOffsetX).coerceIn(rp.curX - maxLat, rp.curX + maxLat)
                break
            }
        }

        // Verify clearance from any nearby obstacle
        for (i in 0 until activeObstacles.size) {
            val obs = activeObstacles[i]
            if (abs(obs.z - safeZ) < 1.8f && abs(obs.x - safeX) < 1.5f) {
                safeX = if (obs.x >= cp.x) cp.x - 1.6f else cp.x + 1.6f
            }
        }
        return Triple(safeX, safeY, safeZ)
    }

    /**
     * Activates checkpoint [checkpointIndex] for the local player (`"player_0"`) or any rival (`"rival_1"..`),
     * enforcing strict forward checkpoint progression (never regressing on backward movement, and
     * activating sequential checkpoints in order).
     */
    fun activateCheckpointForRacer(playerId: String, checkpointIndex: Int): Boolean {
        val cp = activeCheckpoints.firstOrNull { it.index == checkpointIndex } ?: return false
        if (playerId == "player_0") {
            return tryActivateCheckpointForPlayer(cp)
        }
        val rival = rivals.firstOrNull { it.checkpointState.playerId == playerId || "rival_${it.id}" == playerId }
            ?: return false
        return tryActivateCheckpointForRival(rival, cp)
    }

    private fun tryActivateCheckpointForPlayer(cp: CheckpointDef): Boolean {
        // Strict ordered progression: only activate the next sequential checkpoint (last + 1).
        // Moving backward to an already-activated checkpoint (cp.index <= lastActivatedCheckpoint)
        // or skipping unreached checkpoints (cp.index > lastActivatedCheckpoint + 1) is ignored.
        if (cp.index != playerCheckpointState.lastActivatedCheckpoint + 1) return false

        val (safeX, safeY, safeZ) = findSafeCheckpointPosition(cp, 0f)
        playerCheckpointState.lastActivatedCheckpoint = cp.index
        playerCheckpointState.activatedCheckpoints.add(cp.index)
        playerCheckpointState.safeRespawnX = safeX
        playerCheckpointState.safeRespawnY = safeY
        playerCheckpointState.safeRespawnZ = safeZ
        playerCheckpointState.safeRespawnYaw = cp.safeYaw

        highestCheckpointIndex = cp.index
        spawnX = safeX
        spawnY = safeY
        spawnZ = safeZ
        if (currentMode == GameMode.ENDLESS) {
            endlessSurvivalRespawnsRemaining = 1
        }

        checkpointPulseTimers[cp.index] = 1.25f
        val totalCps = activeCheckpoints.size.coerceAtLeast(1)
        checkpointBannerTimer = 2.4f
        checkpointBannerText = if ( currentMode == GameMode.ENDLESS ) {
            "CHECKPOINT ${cp.index} ACTIVATED!"
        } else {
            "CHECKPOINT ${cp.index} / $totalCps ACTIVATED!"
        }

        listener?.onSfx("checkpoint")
        listener?.onHaptic(false)
        // Golden runes + Glowing sapphire blue magical Mythoria particle burst
        spawnBurst(cp.x, cp.y + 1.4f, cp.z, 12, Rgba.fromHex(0xFFD22B), 3.8f)
        spawnBurst(cp.x, cp.y + 1.7f, cp.z, 10, Rgba.fromHex(0x00E5FF), 3.4f)
        return true
    }

    private fun tryActivateCheckpointForRival(r: RuntimeRival, cp: CheckpointDef): Boolean {
        val state = r.checkpointState
        if (cp.index != state.lastActivatedCheckpoint + 1) return false

        val (safeX, safeY, safeZ) = findSafeCheckpointPosition(cp, r.laneBiasX * 0.45f)
        state.lastActivatedCheckpoint = cp.index
        state.activatedCheckpoints.add(cp.index)
        state.safeRespawnX = safeX
        state.safeRespawnY = safeY
        state.safeRespawnZ = safeZ
        state.safeRespawnYaw = cp.safeYaw

        r.respawnX = safeX
        r.respawnY = safeY
        r.respawnZ = safeZ

        checkpointPulseTimers[cp.index] = maxOf(checkpointPulseTimers[cp.index] ?: 0f, 0.9f)
        spawnBurst(cp.x, cp.y + 1.3f, cp.z, 6, Rgba.fromHex(0xFFD22B), 3.0f)
        spawnBurst(cp.x, cp.y + 1.5f, cp.z, 6, Rgba.fromHex(0x00E5FF), 2.8f)
        return true
    }

    /**
     * Individual non-blocking respawn for ONLY the local player (`"player_0"`).
     * Never resets the match, never resets the timer, and never interrupts any rival or platform/obstacle!
     */
    private fun triggerIndividualRespawnForPlayer() {
        // In Survival/Endless Mode, do not grant unlimited respawns unless a periodic checkpoint was earned
        if (currentMode == GameMode.ENDLESS && endlessSurvivalRespawnsRemaining <= 0) {
            runFailed = true
            listener?.onSfx("fail")
            listener?.onHaptic(true)
            listener?.onPlayerFell()
            return
        }
        if (currentMode == GameMode.ENDLESS && endlessSurvivalRespawnsRemaining > 0) {
            endlessSurvivalRespawnsRemaining--
        }

        // Fall particles at fall location before portal teleport
        spawnBurst(px, py + 0.5f, pz, 10, Rgba.fromHex(0x00E5FF), 3.2f)

        val safeX = playerCheckpointState.safeRespawnX
        val safeY = playerCheckpointState.safeRespawnY
        val safeZ = playerCheckpointState.safeRespawnZ
        val safeYaw = playerCheckpointState.safeRespawnYaw

        px = safeX
        py = safeY
        pz = safeZ
        vx = 0f
        vy = 0f
        vz = 0f
        playerYaw = safeYaw
        grounded = false
        diveTimer = 0f
        hitStunTimer = 0f
        squashY = 1.24f

        playerCheckpointState.currentX = px
        playerCheckpointState.currentY = py
        playerCheckpointState.currentZ = pz
        playerCheckpointState.isRespawning = true
        playerCheckpointState.respawnTimerSec = 1.05f
        playerCheckpointState.fallAnimTimerSec = 0.25f
        playerCheckpointState.arrivalAnimTimerSec = 0.80f
        playerCheckpointState.portalScale = 1.0f
        playerCheckpointState.respawnCount++
        narrowBridgesCrossedWithoutFall = 0
        wasOnNarrowBridge = false

        if (currentMode == GameMode.TIME_ATTACK) {
            elapsedSec += timeAttackPenaltySec
            checkpointBannerTimer = 2.0f
            checkpointBannerText = "PORTAL RESPAWN (+${String.format("%.1f", timeAttackPenaltySec)}s)"
        } else {
            val cpLabel = if (playerCheckpointState.lastActivatedCheckpoint > 0) {
                "CHECKPOINT ${playerCheckpointState.lastActivatedCheckpoint}"
            } else {
                "START SANCTUARY"
            }
            checkpointBannerTimer = 1.6f
            checkpointBannerText = "RESPAWNED AT $cpLabel"
        }

        listener?.onSfx("checkpoint")
        listener?.onHaptic(true)
        // Golden & Sapphire portal arrival burst at the checkpoint
        spawnBurst(px, py + 0.4f, pz, 14, Rgba.fromHex(0x00E5FF), 3.8f)
        spawnBurst(px, py + 0.9f, pz, 12, Rgba.fromHex(0xFFD22B), 3.4f)
    }

    /**
     * Individual non-blocking respawn for ONLY the specified rival (`r`).
     * Never affects the human player or any other rival!
     */
    private fun triggerIndividualRespawnForRival(r: RuntimeRival) {
        val state = r.checkpointState
        spawnBurst(r.x, r.y + 0.4f, r.z, 6, Rgba.fromHex(0x00E5FF), 2.5f)

        r.x = state.safeRespawnX
        r.y = state.safeRespawnY
        r.z = state.safeRespawnZ
        r.vx = 0f
        r.vy = 0f
        r.vz = 0f
        r.yaw = state.safeRespawnYaw
        r.grounded = false
        r.hitStunTimer = 0f
        r.squashY = 1.22f

        state.currentX = r.x
        state.currentY = r.y
        state.currentZ = r.z
        state.isRespawning = true
        state.respawnTimerSec = 1.05f
        state.fallAnimTimerSec = 0.25f
        state.arrivalAnimTimerSec = 0.80f
        state.portalScale = 1.0f
        state.respawnCount++

        spawnBurst(r.x, r.y + 0.5f, r.z, 8, Rgba.fromHex(0x00E5FF), 3.0f)
        spawnBurst(r.x, r.y + 0.8f, r.z, 8, Rgba.fromHex(0xFFD22B), 2.8f)
    }

    fun respawnAtCheckpoint() {
        synchronized(cmdLock) {
            pendingRespawn = true
        }
    }

    private fun applyRespawnAtCheckpoint() {
        runFailed = false
        isPaused = false
        respawnPlayer(fullReset = false)
    }

    private fun respawnPlayer(fullReset: Boolean) {
        if (fullReset) {
            spawnX = currentLevel.startX
            spawnY = currentLevel.startY
            spawnZ = currentLevel.startZ
            playerCheckpointState.safeRespawnX = spawnX
            playerCheckpointState.safeRespawnY = spawnY + 0.6f
            playerCheckpointState.safeRespawnZ = spawnZ
            playerCheckpointState.safeRespawnYaw = 180f
        }
        px = playerCheckpointState.safeRespawnX
        py = playerCheckpointState.safeRespawnY
        pz = playerCheckpointState.safeRespawnZ
        vx = 0f; vy = 0f; vz = 0f
        playerYaw = playerCheckpointState.safeRespawnYaw
        grounded = false
        diveTimer = 0f
        hitStunTimer = 0f
        cameraYawOffset = 0f
        playerCheckpointState.currentX = px
        playerCheckpointState.currentY = py
        playerCheckpointState.currentZ = pz
        camX = px
        camY = py + 4.8f
        camZ = pz + 8.5f
    }

    fun triggerJump() {
        synchronized(cmdLock) { pendingJump = true }
    }

    fun releaseJump() {
        synchronized(cmdLock) { pendingJumpRelease = true }
    }

    fun triggerDive() {
        synchronized(cmdLock) { pendingDive = true }
    }

    fun triggerDash() {
        synchronized(cmdLock) { pendingDash = true }
    }

    private fun aimInputYawOrCurrent(): Float {
        val jx = (joyX * joystickSensitivity).coerceIn(-1f, 1f)
        val jy = (joyY * joystickSensitivity).coerceIn(-1f, 1f)
        if (jx * jx + jy * jy < 0.04f) return playerYaw
        val camYawRad = Math.toRadians(cameraYawOffset.toDouble())
        val camFwdX = -sin(camYawRad).toFloat()
        val camFwdZ = -cos(camYawRad).toFloat()
        val camRightX = cos(camYawRad).toFloat()
        val camRightZ = -sin(camYawRad).toFloat()
        val inputX = camRightX * jx + camFwdX * (-jy)
        val inputZ = camRightZ * jx + camFwdZ * (-jy)
        return Math.toDegrees(atan2(inputX.toDouble(), inputZ.toDouble())).toFloat()
    }

    private fun applyDive() {
        if (isMenuShowcase || isPaused || runFinished || runFailed) return
        if (diveTimer > 0f || diveCooldown > 0f || hitStunTimer > 0f) return
        playerYaw = aimInputYawOrCurrent()
        val rad = Math.toRadians(playerYaw.toDouble())
        val fx = sin(rad).toFloat()
        val fz = cos(rad).toFloat()
        vx = fx * 12.6f
        vz = fz * 12.6f
        vy = if (grounded) 4.4f else maxOf(vy, 2.4f)
        grounded = false
        diveTimer = 0.42f
        diveCooldown = 0.68f
        squashY = 0.74f
        listener?.onSfx("dive")
        listener?.onHaptic(false)
        spawnBurst(px, py + 0.4f, pz, 8, Rgba(1f, 1f, 1f, 1f), 2.5f)
    }

    private fun applyDash() {
        if (!dashUnlocked || isMenuShowcase || isPaused || runFinished || runFailed) return
        if (dashCooldown > 0f || hitStunTimer > 0f) return
        playerYaw = aimInputYawOrCurrent()
        val rad = Math.toRadians(playerYaw.toDouble())
        vx = sin(rad).toFloat() * 17.0f
        vz = cos(rad).toFloat() * 17.0f
        vy = maxOf(vy, 1.6f)
        dashCooldown = 2.4f
        dashesUsedInRun++
        squashY = 1.26f
        pickupAuraTimer = 0.42f
        pickupAuraColor = Rgba.fromHex(0x00E5FF)
        listener?.onSfx("dash")
        listener?.onHaptic(true)
        spawnBurst(px, py + 0.7f, pz, 12, Rgba.fromHex(0x00E5FF), 4.0f)
    }

    fun addCameraDrag(dx: Float, dy: Float) {
        if (isMenuShowcase) {
            cameraYawOffset += dx * 0.25f
            return
        }
        cameraYawOffset = (cameraYawOffset - dx * 0.22f * cameraSensitivity).coerceIn(-120f, 120f)
        cameraPitchDeg = (cameraPitchDeg + dy * 0.14f * cameraSensitivity).coerceIn(10f, 45f)
    }

    private fun drainPendingCommands() {
        var doMenu = false
        var loadPair: Pair<LevelDef, GameMode>? = null
        var doRespawn = false
        var doJump = false
        var doJumpRelease = false
        var doDive = false
        var doDash = false
        synchronized(cmdLock) {
            doMenu = pendingMenuShowcase
            pendingMenuShowcase = false
            loadPair = pendingLevelLoad
            pendingLevelLoad = null
            doRespawn = pendingRespawn
            pendingRespawn = false
            doJump = pendingJump
            pendingJump = false
            doJumpRelease = pendingJumpRelease
            pendingJumpRelease = false
            doDive = pendingDive
            pendingDive = false
            doDash = pendingDash
            pendingDash = false
        }
        if (doMenu) applyMenuShowcase()
        loadPair?.let { (lvl, mode) -> applyLoadLevel(lvl, mode) }
        if (doRespawn) applyRespawnAtCheckpoint()
        if (doJump && !isMenuShowcase && !isPaused && !runFinished && !runFailed) {
            jumpBufferTimer = 0.14f
        }
        if (doJumpRelease && vy > 4.5f && diveTimer <= 0f) {
            vy *= 0.62f
        }
        if (doDive) applyDive()
        if (doDash) applyDash()
    }

    fun step(rawDt: Float) {
        drainPendingCommands()
        val dt = rawDt.coerceIn(0.001f, 0.045f)
        worldTime += dt
        animTime += dt
        updateParticles(dt)

        if (isMenuShowcase) {
            camX = sin(worldTime * 0.22f) * 1.4f
            camY = 3.1f + sin(worldTime * 0.4f) * 0.25f
            camZ = 7.0f
            camLookX = 0f
            camLookY = 1.4f
            camLookZ = -2.5f
            return
        }

        if (isPaused) return

        // Update survival progression & monotonic obstacle/platform clocks
        if (!runFinished && !runFailed) {
            elapsedSec += dt
            val tierDuration = if (currentMode == GameMode.ENDLESS) 14f else 18f
            val newTier = 1 + (elapsedSec / tierDuration).toInt()
            if (newTier > survivalTier) {
                survivalTier = newTier
                tierBannerTimer = 2.4f
                tierBannerText = "TIER $survivalTier • ${complexityLabelFor(survivalTier)}"
                if (currentMode == GameMode.ENDLESS) {
                    currentWorld = GameCatalog.worlds[(survivalTier - 1) % GameCatalog.worlds.size]
                }
                listener?.onSfx("checkpoint")
                listener?.onHaptic(false)
                spawnBurst(px, py + 1.5f, pz, 14, Rgba.fromHex(0xFFD22B), 3.8f)
            }
            val rampRate = if (currentMode == GameMode.ENDLESS) 0.028f else 0.012f
            val maxMult = if (currentMode == GameMode.ENDLESS) 2.4f else 1.45f
            speedMultiplier = (1.0f + elapsedSec * rampRate + (survivalTier - 1) * 0.06f).coerceAtMost(maxMult)
        }
        if (tierBannerTimer > 0f) {
            tierBannerTimer = (tierBannerTimer - dt).coerceAtLeast(0f)
            if (tierBannerTimer == 0f) tierBannerText = ""
        }
        if (checkpointBannerTimer > 0f) {
            checkpointBannerTimer = (checkpointBannerTimer - dt).coerceAtLeast(0f)
            if (checkpointBannerTimer == 0f) checkpointBannerText = ""
        }

        // Decay checkpoint activation visual pulse timers
        val pulseIter = checkpointPulseTimers.entries.iterator()
        while (pulseIter.hasNext()) {
            val entry = pulseIter.next()
            val rem = entry.value - dt
            if (rem <= 0f) pulseIter.remove() else entry.setValue(rem)
        }

        // Decay local player's portal respawn visual timer (non-blocking)
        if (playerCheckpointState.isRespawning) {
            playerCheckpointState.respawnTimerSec = (playerCheckpointState.respawnTimerSec - dt).coerceAtLeast(0f)
            playerCheckpointState.fallAnimTimerSec = (playerCheckpointState.fallAnimTimerSec - dt).coerceAtLeast(0f)
            playerCheckpointState.arrivalAnimTimerSec = (playerCheckpointState.arrivalAnimTimerSec - dt).coerceAtLeast(0f)
            playerCheckpointState.portalScale = (playerCheckpointState.respawnTimerSec / 1.05f).coerceIn(0f, 1f)
            if (playerCheckpointState.respawnTimerSec <= 0f) {
                playerCheckpointState.isRespawning = false
                playerCheckpointState.portalScale = 0f
            }
        }

        obstacleTime += dt * speedMultiplier
        platformTime += dt * (1.0f + (speedMultiplier - 1.0f) * 0.65f)

        // Endless Mode: stream progressive segments ahead & cull far behind
        if (currentMode == GameMode.ENDLESS && !runFailed && !runFinished) {
            extendEndlessTrackIfNeeded()
        }

        // Update animated platforms
        for (i in 0 until platforms.size) {
            val rp = platforms[i]
            rp.prevX = rp.curX
            rp.prevY = rp.curY
            rp.prevZ = rp.curZ
            when (rp.def.type) {
                PlatformType.MOVING, PlatformType.ELEVATOR -> {
                    val wave = sin(platformTime * rp.def.speed + rp.def.phase)
                    rp.curX = rp.def.x + rp.def.moveDx * wave
                    rp.curY = rp.def.y + rp.def.moveDy * wave
                    rp.curZ = rp.def.z + rp.def.moveDz * wave
                }
                PlatformType.DISAPPEARING -> {
                    val cycle = (platformTime + rp.def.phase) % 4.5f
                    rp.active = cycle < 3.0f
                    rp.warningPulse = if (cycle in 2.1f..3.0f) ((sin(platformTime * 22f) + 1f) * 0.5f) else 0f
                    if (cycle in 2.1f..2.3f && abs(rp.curZ - pz) < 7.5f && warnSfxCooldown <= 0f) {
                        warnSfxCooldown = 0.85f
                        listener?.onSfx("hazard_warn")
                    }
                }
                PlatformType.FALLING -> {
                    if (rp.fallTimer >= 0f) {
                        rp.fallTimer += dt * (1f + (speedMultiplier - 1f) * 0.4f)
                        if (rp.fallTimer > 0.65f) {
                            rp.curY -= dt * 11f
                            if (rp.fallTimer > 1.8f) {
                                rp.active = false
                            }
                        } else {
                            rp.warningPulse = (sin(platformTime * 30f) + 1f) * 0.5f
                            if (rp.fallTimer < 0.15f && warnSfxCooldown <= 0f) {
                                warnSfxCooldown = 0.75f
                                listener?.onSfx("hazard_warn")
                            }
                        }
                        if (rp.fallTimer > 4.2f) {
                            rp.fallTimer = -1f
                            rp.curY = rp.def.y
                            rp.active = true
                            rp.warningPulse = 0f
                        }
                    }
                }
                else -> {}
            }
        }

        if (runFinished) {
            animState = CharAnimState.VICTORY_DANCE
            playerYaw += dt * 70f
            squashY += (1f - squashY) * minOf(1f, dt * 10f)
            if (currentMode == GameMode.RIVAL_RACE || rivals.isNotEmpty()) {
                updateRivals(dt)
            }
            updateFollowCamera(dt)
            return
        }

        if (runFailed) {
            animState = CharAnimState.DEFEAT
            vy -= 26f * dt
            py += vy * dt
            if (currentMode == GameMode.RIVAL_RACE || rivals.isNotEmpty()) {
                updateRivals(dt)
            }
            updateFollowCamera(dt)
            return
        }

        if (coyoteTimer > 0f) coyoteTimer -= dt
        if (jumpBufferTimer > 0f) jumpBufferTimer -= dt
        if (diveTimer > 0f) diveTimer -= dt
        if (diveCooldown > 0f) diveCooldown -= dt
        if (dashCooldown > 0f) dashCooldown -= dt
        if (hitStunTimer > 0f) hitStunTimer -= dt
        if (landTimer > 0f) landTimer -= dt
        if (warnSfxCooldown > 0f) warnSfxCooldown -= dt
        if (shieldTimer > 0f) shieldTimer = (shieldTimer - dt).coerceAtLeast(0f)
        if (magnetTimer > 0f) magnetTimer = (magnetTimer - dt).coerceAtLeast(0f)
        if (wingsTimer > 0f) wingsTimer = (wingsTimer - dt).coerceAtLeast(0f)
        if (pickupAuraTimer > 0f) pickupAuraTimer = (pickupAuraTimer - dt).coerceAtLeast(0f)
        if (cameraShakeTimer > 0f) cameraShakeTimer = (cameraShakeTimer - dt).coerceAtLeast(0f)
        squashY += (1f - squashY) * minOf(1f, dt * 12f)

        // Camera-relative movement input
        val jx = (joyX * joystickSensitivity).coerceIn(-1f, 1f)
        val jy = (joyY * joystickSensitivity).coerceIn(-1f, 1f)
        val mag = sqrt(jx * jx + jy * jy).coerceAtMost(1f)
        val camYawRad = Math.toRadians(cameraYawOffset.toDouble())
        val camFwdX = -sin(camYawRad).toFloat()
        val camFwdZ = -cos(camYawRad).toFloat()
        val camRightX = cos(camYawRad).toFloat()
        val camRightZ = -sin(camYawRad).toFloat()

        // Screen joystick: +jx is right, -jy is up/forward
        val inputX = camRightX * jx + camFwdX * (-jy)
        val inputZ = camRightZ * jx + camFwdZ * (-jy)

        var surfaceFriction = currentWorld.frictionMultiplier
        var standingPlatform: RuntimePlatform? = null

        if (hitStunTimer <= 0f && diveTimer <= 0f) {
            val charSpeedScale = when (selectedSkin.id) {
                "kid_ahmed" -> 1.03f
                "salma_heroine" -> 0.99f
                else -> 1.0f
            }
            val maxSpeed = 8.4f * charSpeedScale
            val targetVx = inputX * maxSpeed
            val targetVz = inputZ * maxSpeed
            val accel = if (grounded) 38f * surfaceFriction else 16f
            val decel = if (grounded) 24f * surfaceFriction else 5f
            val prevSpeed = sqrt(vx * vx + vz * vz)

            if (mag > 0.08f) {
                vx += (targetVx - vx) * minOf(1f, accel * dt)
                vz += (targetVz - vz) * minOf(1f, accel * dt)
                val newSpeed = sqrt(vx * vx + vz * vz)
                val speedDelta = (newSpeed - prevSpeed) / dt.coerceAtLeast(0.005f)
                val targetPitch = (speedDelta * 0.22f).coerceIn(-6f, 8f)
                playerAccelPitchDeg += (targetPitch - playerAccelPitchDeg) * minOf(1f, dt * 10f)
                val targetYaw = Math.toDegrees(atan2(inputX.toDouble(), inputZ.toDouble())).toFloat()
                val yawDelta = ((targetYaw - playerYaw + 540f) % 360f) - 180f
                playerTurnLeanDeg += (yawDelta.coerceIn(-25f, 25f) * 0.48f - playerTurnLeanDeg) * minOf(1f, dt * 10f)
                playerYaw = lerpAngle(playerYaw, targetYaw, minOf(1f, dt * 13f))
            } else {
                vx *= (1f - minOf(1f, decel * dt))
                vz *= (1f - minOf(1f, decel * dt))
                val targetPitch = if (prevSpeed > 1.2f) -4.5f else 0f
                playerAccelPitchDeg += (targetPitch - playerAccelPitchDeg) * minOf(1f, dt * 10f)
                playerTurnLeanDeg *= (1f - minOf(1f, dt * 10f))
            }
        } else {
            playerTurnLeanDeg *= (1f - minOf(1f, dt * 8f))
            playerAccelPitchDeg *= (1f - minOf(1f, dt * 8f))
        }

        // Jump execution (coyote + buffer)
        if (jumpBufferTimer > 0f && (grounded || coyoteTimer > 0f) && hitStunTimer <= 0f) {
            val baseJumpVy = when (selectedSkin.id) {
                "salma_heroine" -> 11.82f
                "kid_ahmed" -> 11.52f
                else -> 11.65f
            }
            vy = if (wingsTimer > 0f) 13.4f else baseJumpVy
            grounded = false
            coyoteTimer = 0f
            jumpBufferTimer = 0f
            jumpsInRun++
            squashY = 1.24f
            listener?.onSfx("jump")
            listener?.onHaptic(false)
            spawnBurst(px, py + 0.1f, pz, 7, Rgba(1f, 1f, 1f, 0.9f), 2.2f)
        }

        // Gravity (Hermes Glide Wings softens downward fall gravity by 48% for smooth air-gliding)
        val charFallFactor = if (selectedSkin.id == "salma_heroine" && vy < 0f) 0.96f else 1.0f
        val glideFactor = if (wingsTimer > 0f && vy < 0f) 0.52f else charFallFactor
        val gravity = 28.5f * currentWorld.gravityMultiplier * glideFactor
        vy -= gravity * dt

        val prevY = py
        px += vx * dt
        py += vy * dt
        pz += vz * dt

        // Guard against NaN/infinite physics anomalies
        if (px.isNaN() || py.isNaN() || pz.isNaN() || !px.isFinite() || !py.isFinite() || !pz.isFinite()) {
            triggerIndividualRespawnForPlayer()
        }

        if (!grounded && py > airbornePeakY) {
            airbornePeakY = py
        }

        // Platform collision resolution (generous rounded-character cylinder/AABB + ledge step-up)
        val wasGrounded = grounded
        grounded = false
        val playerRadius = 0.36f
        val prevVyBeforeLanding = vy
        var bestShadowFloorY = Float.NaN
        for (rp in platforms) {
            if (!rp.active) continue
            val halfW = rp.def.w * 0.5f + playerRadius
            val halfD = rp.def.d * 0.5f + playerRadius
            val topY = rp.curY + rp.def.h * 0.5f
            val botY = rp.curY - rp.def.h * 0.5f

            if (abs(px - rp.curX) <= halfW && abs(pz - rp.curZ) <= halfD) {
                if (topY <= py + 0.15f && (bestShadowFloorY.isNaN() || topY > bestShadowFloorY)) {
                    bestShadowFloorY = topY
                }
                // Landing on top or smooth ledge step-up within 0.18m
                if ((vy <= 0.6f && prevY >= topY - 0.35f && py <= topY + 0.12f) ||
                    (py >= topY - 0.18f && py < topY && vy > -2.5f)
                ) {
                    py = topY
                    vy = 0f
                    grounded = true
                    coyoteTimer = 0.14f
                    standingPlatform = rp
                    bestShadowFloorY = topY

                    // Carry player on moving platform
                    px += (rp.curX - rp.prevX)
                    py += (rp.curY - rp.prevY)
                    pz += (rp.curZ - rp.prevZ)

                    if (!wasGrounded) {
                        val dropHeight = (airbornePeakY - topY).coerceAtLeast(0f)
                        val impactSpeed = abs(prevVyBeforeLanding)
                        val isHeavyLanding = dropHeight > 2.2f || impactSpeed > 13.5f
                        landTimer = if (isHeavyLanding) 0.16f else 0.12f
                        squashY = if (isHeavyLanding) 0.72f else 0.78f
                        if (cameraShakeEnabled) {
                            cameraShakeTimer = if (isHeavyLanding) 0.18f else 0.12f
                            cameraShakeIntensity = if (isHeavyLanding) 0.15f else 0.09f
                        }
                        // Surface-matched landing particle color (sand/stone dust, snow/ice, metal sparks, water spray)
                        val surfaceDustCol = when (currentWorld.id) {
                            4 -> Rgba.fromHex(0xFDE68A) // Golden sand dust
                            5, 8 -> Rgba.fromHex(0xBAE6FD) // Ice / frost crystals
                            3, 9 -> Rgba.fromHex(0x38BDF8) // Electric / metallic sparks
                            7 -> Rgba.fromHex(0x6EE7B7) // Garden leaf/moss dust
                            10 -> Rgba.fromHex(0xFB923C) // Ancient ruins ember dust
                            else -> Rgba(0.92f, 0.96f, 1f, 0.85f)
                        }
                        // Perfect Landing check: centered on platform after an actual jump
                        val centerDistX = abs(px - rp.curX)
                        val centerDistZ = abs(pz - rp.curZ)
                        val isRealJump = dropHeight > 0.75f
                        if (isRealJump && centerDistX <= rp.def.w * 0.28f && centerDistZ <= rp.def.d * 0.38f) {
                            perfectLandingsInRun++
                            listener?.onSfx("land_perfect")
                            if (checkpointBannerTimer <= 0.3f) {
                                checkpointBannerTimer = 1.15f
                                checkpointBannerText = "PERFECT LANDING! ★ ($perfectLandingsInRun)"
                            }
                            spawnBurst(px, py + 0.12f, pz, 8, Rgba.fromHex(0xFFD22B), 2.6f)
                            spawnBurst(px, py + 0.06f, pz, 5, surfaceDustCol, 1.9f)
                        } else {
                            val landSfx = when {
                                isHeavyLanding -> "land_heavy"
                                dropHeight < 0.45f -> "land_soft"
                                else -> "land"
                            }
                            listener?.onSfx(landSfx)
                            spawnBurst(px, py + 0.05f, pz, if (isHeavyLanding) 8 else 5, surfaceDustCol, if (isHeavyLanding) 2.2f else 1.5f)
                        }
                        airbornePeakY = topY
                    }

                    // Gentle rhythmic lateral crosswind on high-altitude Narrow Bridges in Worlds 3 & 9 (fair & learnable)
                    if (rp.def.type == PlatformType.NARROW_BRIDGE && (currentWorld.id == 3 || currentWorld.id == 9) && currentLevel.number >= 12) {
                        val windGust = sin(worldTime * 1.8f) * 1.35f
                        px += windGust * dt
                    }

                    // Track Narrow Bridge crossing without falling
                    if (rp.def.type == PlatformType.NARROW_BRIDGE) {
                        wasOnNarrowBridge = true
                        lastNarrowBridgeZ = rp.curZ
                    } else if (wasOnNarrowBridge && rp.curZ < lastNarrowBridgeZ - 2.0f) {
                        wasOnNarrowBridge = false
                        narrowBridgesCrossedWithoutFall++
                    }

                    // Special platform effects
                    when (rp.def.type) {
                        PlatformType.SECRET_PATH -> {
                            if (!secretPathFoundInRun) {
                                secretPathFoundInRun = true
                                checkpointBannerTimer = 2.2f
                                checkpointBannerText = "SECRET PATH DISCOVERED! ★"
                                listener?.onSfx("checkpoint")
                                listener?.onHaptic(true)
                                spawnBurst(px, py + 0.8f, pz, 16, Rgba.fromHex(0xFFD22B), 4.0f)
                                spawnBurst(px, py + 1.1f, pz, 12, Rgba.fromHex(0x00E5FF), 3.6f)
                            }
                        }
                        PlatformType.BOUNCE_PAD -> {
                            vy = 16.5f
                            grounded = false
                            squashY = 1.32f
                            listener?.onSfx("bounce")
                            listener?.onHaptic(true)
                            spawnBurst(px, py + 0.2f, pz, 10, Rgba.fromHex(0xFFD22B), 3.2f)
                        }
                        PlatformType.SPEED_BOOST -> {
                            vx += rp.def.dirX * 14f * dt
                            vz += rp.def.dirZ * 14f * dt
                        }
                        PlatformType.CONVEYOR -> {
                            px += rp.def.dirX * 3.8f * dt
                            pz += rp.def.dirZ * 3.8f * dt
                        }
                        PlatformType.SLIPPERY -> {
                            surfaceFriction = 0.32f
                        }
                        PlatformType.FALLING -> {
                            if (rp.fallTimer < 0f) {
                                rp.fallTimer = 0f
                                listener?.onSfx("platform_crack")
                                listener?.onHaptic(false)
                            }
                        }
                        PlatformType.FINISH -> {
                            if (!runFinished) {
                                runFinished = true
                                playerCheckpointState.finished = true
                                playerFinishOrder = nextFinishOrder++
                                currentRunGhostFrames += GhostFrame(elapsedSec, px, py, pz, playerYaw)
                                if (currentLevel.number in 1..100 && currentRunGhostFrames.size >= 4) {
                                    synchronized(bestGhostByLevel) {
                                        val prev = bestGhostByLevel[currentLevel.number]
                                        val prevTime = prev?.lastOrNull()?.timeSec ?: Float.MAX_VALUE
                                        if (elapsedSec < prevTime) {
                                            bestGhostByLevel[currentLevel.number] = currentRunGhostFrames.toList()
                                        }
                                    }
                                }
                                val rank = if (currentMode == GameMode.RIVAL_RACE) playerFinishOrder else 1
                                val totalR = if (currentMode == GameMode.RIVAL_RACE) rivals.size + 1 else 1
                                listener?.onSfx("victory")
                                listener?.onHaptic(true)
                                spawnBurst(px, py + 1.8f, pz, 30, Rgba.fromHex(0xFFD22B), 5.5f)
                                spawnBurst(px, py + 2.1f, pz, 25, Rgba.fromHex(0x00D4FF), 5.5f)
                                listener?.onLevelWon(
                                    levelNumber = currentLevel.number,
                                    timeMs = (elapsedSec * 1000f).toLong(),
                                    coinsCollected = coinsCollected,
                                    distanceM = maxDistanceM,
                                    obstaclesPassed = obstaclesPassed,
                                    raceRank = rank,
                                    totalRacers = totalR,
                                    jumpsInRun = jumpsInRun,
                                    perfectLandings = perfectLandingsInRun,
                                    secretPathFound = secretPathFoundInRun,
                                    narrowBridgesCrossed = narrowBridgesCrossedWithoutFall,
                                    dashUsed = dashesUsedInRun > 0,
                                    respawnCount = playerCheckpointState.respawnCount,
                                    chestsOpened = chestsOpenedInRun
                                )
                            }
                        }
                        else -> {}
                    }
                    break
                } else if (py < topY - 0.18f && py + 1.4f > botY) {
                    // Side wall push-out so character never clips inside blocks
                    val dx = px - rp.curX
                    val dz = pz - rp.curZ
                    val overlapX = halfW - abs(dx)
                    val overlapZ = halfD - abs(dz)
                    if (overlapX < overlapZ) {
                        px += if (dx >= 0f) overlapX else -overlapX
                        vx = 0f
                    } else {
                        pz += if (dz >= 0f) overlapZ else -overlapZ
                        vz = 0f
                    }
                }
            }
        }
        shadowFloorY = bestShadowFloorY
        shadowHeightGap = if (!bestShadowFloorY.isNaN()) (py - bestShadowFloorY).coerceAtLeast(0f) else 0f

        // Obstacle collisions & passed tracking
        var passedCount = 0
        for (i in 0 until activeObstacles.size) {
            val obs = activeObstacles[i]
            if (pz < obs.z - 1.5f) passedCount++
            checkObstacleCollision(obs)
        }
        obstaclesPassed = maxOf(obstaclesPassed, passedCount)

        // Ordered Checkpoints Activation for Local Player ("player_0")
        for (i in 0 until activeCheckpoints.size) {
            val cp = activeCheckpoints[i]
            val distSq = (px - cp.x) * (px - cp.x) + (pz - cp.z) * (pz - cp.z)
            if (distSq < 10.5f && abs(py - cp.y) < 3.2f) {
                tryActivateCheckpointForPlayer(cp)
            }
        }

        // Coins collection + Active Coin Magnet pull
        for (i in 0 until coins.size) {
            val rc = coins[i]
            if (rc.collected) continue
            var dx = px - rc.curX
            var dy = (py + 0.7f) - rc.curY
            var dz = pz - rc.curZ
            val distSq = dx * dx + dy * dy + dz * dz
            if (magnetTimer > 0f && distSq < 72.25f) { // 8.5m magnet radius
                val dist = sqrt(distSq).coerceAtLeast(0.05f)
                val pullSpeed = 19.5f * dt
                rc.curX += (dx / dist) * minOf(dist, pullSpeed)
                rc.curY += (dy / dist) * minOf(dist, pullSpeed)
                rc.curZ += (dz / dist) * minOf(dist, pullSpeed)
                dx = px - rc.curX
                dy = (py + 0.7f) - rc.curY
                dz = pz - rc.curZ
            }
            if (dx * dx + dy * dy + dz * dz < 1.95f) {
                rc.collected = true
                coinsCollected++
                listener?.onSfx("coin")
                listener?.onHaptic(false)
                spawnBurst(rc.curX, rc.curY, rc.curZ, 10, Rgba.fromHex(0xFFD22B), 3.0f)
            }
        }

        // 3D Power-Ups collection (Aegis Shield, Coin Magnet, Hermes Glide Wings)
        for (i in 0 until powerUps.size) {
            val pu = powerUps[i]
            if (pu.collected) continue
            val dx = px - pu.def.x
            val dy = (py + 0.75f) - pu.def.y
            val dz = pz - pu.def.z
            if (dx * dx + dy * dy + dz * dz < 2.25f) {
                pu.collected = true
                pickupAuraTimer = 0.48f
                when (pu.def.type) {
                    PowerUpType.SHIELD -> {
                        shieldTimer = 12.0f
                        pickupAuraColor = Rgba.fromHex(0x00E5FF)
                        checkpointBannerTimer = 1.8f
                        checkpointBannerText = "AEGIS GOLDEN SHIELD ACTIVATED! 🛡️"
                        listener?.onSfx("powerup_shield")
                        spawnBurst(pu.def.x, pu.def.y, pu.def.z, 16, Rgba.fromHex(0x00E5FF), 4.0f)
                    }
                    PowerUpType.MAGNET -> {
                        magnetTimer = 9.5f
                        pickupAuraColor = Rgba.fromHex(0xFFD22B)
                        checkpointBannerTimer = 1.8f
                        checkpointBannerText = "ROYAL COIN MAGNET ACTIVATED! 🧲"
                        listener?.onSfx("powerup_magnet")
                        spawnBurst(pu.def.x, pu.def.y, pu.def.z, 16, Rgba.fromHex(0xFFD22B), 4.0f)
                    }
                    PowerUpType.WINGS -> {
                        wingsTimer = 8.5f
                        pickupAuraColor = Rgba.fromHex(0x2EE58D)
                        checkpointBannerTimer = 1.8f
                        checkpointBannerText = "HERMES GLIDE WINGS ACTIVATED! 🪽"
                        listener?.onSfx("powerup_wings")
                        spawnBurst(pu.def.x, pu.def.y, pu.def.z, 16, Rgba.fromHex(0x2EE58D), 4.0f)
                    }
                }
                listener?.onHaptic(true)
            }
        }

        // Hidden Royal Treasure Chests & Boss Gate Chests (Wooden, Metallic, Magic Rare, Royal Event Boss)
        for (i in 0 until treasureChests.size) {
            val tc = treasureChests[i]
            if (tc.opened) {
                tc.openAnim = (tc.openAnim + dt * 3.2f).coerceAtMost(1f)
                continue
            }
            val dx = px - tc.def.x
            val dy = (py + 0.65f) - tc.def.y
            val dz = pz - tc.def.z
            if (dx * dx + dy * dy + dz * dz < 2.65f) {
                tc.opened = true
                tc.openAnim = 0.05f
                chestsOpenedInRun++
                coinsCollected += tc.def.bonusCoins
                pickupAuraTimer = 0.65f
                pickupAuraColor = Rgba.fromHex(tc.def.tier.colorHex)
                checkpointBannerTimer = 2.4f
                checkpointBannerText = "${tc.def.tier.titleEn.uppercase()}! +${tc.def.bonusCoins} COINS ★"
                val chestSfx = when (tc.def.tier) {
                    ChestTier.WOODEN -> "chest_wooden"
                    ChestTier.METALLIC -> "chest_metal"
                    ChestTier.MAGIC_RARE -> "chest_magic"
                    ChestTier.EVENT_BOSS -> "chest_royal"
                }
                listener?.onSfx(chestSfx)
                listener?.onHaptic(true)
                spawnBurst(tc.def.x, tc.def.y + 0.8f, tc.def.z, 22, Rgba.fromHex(tc.def.tier.colorHex), 4.5f)
                spawnBurst(tc.def.x, tc.def.y + 1.1f, tc.def.z, 14, Rgba.fromHex(0xFFD22B), 4.0f)
            }
        }

        // Record current run Ghost Frame & update Personal Best Ghost Racer playback
        ghostRecordTimer += dt
        if (ghostRecordTimer >= 0.10f && currentRunGhostFrames.size < 1800) {
            ghostRecordTimer = 0f
            currentRunGhostFrames += GhostFrame(elapsedSec, px, py, pz, playerYaw)
        }
        updateGhostPlayback()

        // Distance & Race Progress tracking
        val distNow = (-pz).coerceAtLeast(0f)
        if (distNow > maxDistanceM) maxDistanceM = distNow
        val totalCourseDist = (-currentLevel.finishZ).coerceAtLeast(10f)
        playerCheckpointState.currentX = px
        playerCheckpointState.currentY = py
        playerCheckpointState.currentZ = pz
        playerCheckpointState.raceProgress = ((-pz) / totalCourseDist).coerceIn(0f, 1f)

        // Individual Fall & Out-of-Bounds Detection for Local Player ("player_0")
        val outOfBounds = abs(px) > 45f || pz > currentLevel.startZ + 25f
        if ((py < prevFloorMinY || outOfBounds) && !runFailed && !runFinished) {
            triggerIndividualRespawnForPlayer()
        }

        // Running dust, equipped trail particles, and surface-specific footsteps / slide sounds
        val horizSpeed = sqrt(vx * vx + vz * vz)
        if (grounded && horizSpeed > 2.2f) {
            trailTimer += dt
            if (trailTimer > 0.085f) {
                trailTimer = 0f
                val trailCol = when (selectedTrailId) {
                    "trail_gold" -> Rgba.fromHex(0xFFD22B)
                    "trail_cyan" -> Rgba.fromHex(0x00E5FF)
                    "trail_flame" -> Rgba.fromHex(0xFF5400)
                    "trail_rainbow" -> Rgba.fromHex(listOf(0xFF3B5CL, 0xFFD22BL, 0x2EE58DL, 0x00D4FFL, 0xC83BFFL)[(worldTime * 8).toInt() % 5])
                    else -> Rgba(1f, 1f, 1f, 0.75f)
                }
                particles += Particle(px, py + 0.12f, pz, -vx * 0.15f, 0.8f, -vz * 0.15f, 0.42f, 0.42f, 0.24f, trailCol)
            }

            if (standingPlatform?.def?.type == PlatformType.SLIPPERY) {
                slideSfxTimer += dt
                if (slideSfxTimer >= 0.38f) {
                    slideSfxTimer = 0f
                    listener?.onSfx("slide")
                }
            } else {
                footstepTimer += dt * (horizSpeed / 6.0f).coerceIn(0.7f, 1.5f)
                if (footstepTimer >= 0.32f) {
                    footstepTimer = 0f
                    val stepSfx = when (currentWorld.id) {
                        4 -> "step_sand"
                        5, 8 -> "step_ice"
                        3, 9 -> "step_metal"
                        7 -> if (standingPlatform?.def?.type == PlatformType.NARROW_BRIDGE) "step_wood" else "step_grass"
                        else -> "step_stone"
                    }
                    listener?.onSfx(stepSfx)
                }
            }
        } else {
            footstepTimer = 0.22f
        }

        // Check if any nearby platform or obstacle is in active hazard warning state
        var nearWarn = standingPlatform != null && standingPlatform.warningPulse > 0.1f
        if (!nearWarn) {
            for (i in 0 until activeObstacles.size) {
                val obs = activeObstacles[i]
                if (abs(obs.z - pz) < 6.5f) {
                    nearWarn = true
                    break
                }
            }
        }
        hazardWarningActive = nearWarn

        // Living world ambient particles (fluttering leaves in Gardens, glowing crystal motes in Caves, magma embers in Ruins)
        val maxParticles = when (graphicsQuality) {
            GraphicsQuality.LOW -> 35
            GraphicsQuality.MEDIUM -> 80
            GraphicsQuality.HIGH -> 150
        }
        ambientParticleTimer += dt
        val ambientInterval = if (graphicsQuality == GraphicsQuality.HIGH) 0.14f else 0.26f
        if (graphicsQuality != GraphicsQuality.LOW && ambientParticleTimer >= ambientInterval && particles.size < maxParticles - 8) {
            ambientParticleTimer = 0f
            val ambCol = when (currentWorld.id) {
                7 -> if (Random.nextBoolean()) Rgba.fromHex(0x2EE58D) else Rgba.fromHex(0xFFD22B) // Fluttering leaves
                8 -> Rgba.fromHex(0x38BDF8) // Crystal cave motes
                10 -> Rgba.fromHex(0xFF5400) // Ancient ruins lava embers
                else -> Rgba.fromHex(0x00E5FF)
            }
            val ax = px + (Random.nextFloat() - 0.5f) * 14f
            val ay = py + 1.5f + Random.nextFloat() * 3.5f
            val az = pz - Random.nextFloat() * 18f
            val avy = if (currentWorld.id == 10) 2.2f else -0.6f
            particles += Particle(ax, ay, az, (Random.nextFloat() - 0.5f) * 1.4f, avy, 0.4f, 0.95f, 0.95f, 0.18f, ambCol)
        }

        // Animation state selection
        animState = when {
            playerCheckpointState.fallAnimTimerSec > 0f -> CharAnimState.FALL
            hitStunTimer > 0f -> CharAnimState.HIT
            diveTimer > 0f -> CharAnimState.DIVE
            !grounded && currentWorld.gravityMultiplier < 0.85f && vy < 0f -> CharAnimState.FLOAT
            !grounded && vy > 1.5f -> CharAnimState.JUMP
            !grounded && vy <= 1.5f -> CharAnimState.FALL
            landTimer > 0f -> CharAnimState.LAND
            standingPlatform?.def?.type == PlatformType.SLIPPERY && horizSpeed > 2.5f -> CharAnimState.SLIDE
            horizSpeed > 7.6f -> CharAnimState.SPRINT
            horizSpeed > 3.2f -> CharAnimState.RUN
            horizSpeed > 0.4f -> CharAnimState.WALK
            else -> CharAnimState.IDLE
        }

        // Update AI Rivals / Other Players independently
        if (currentMode == GameMode.RIVAL_RACE || rivals.isNotEmpty()) {
            updateRivals(dt)
        }

        updateFollowCamera(dt)
    }

    private fun updateRivals(dt: Float) {
        val gravity = 28.5f * currentWorld.gravityMultiplier
        val totalCourseDist = (-currentLevel.finishZ).coerceAtLeast(10f)
        for (i in 0 until rivals.size) {
            val r = rivals[i]
            val state = r.checkpointState
            r.animTime += dt

            if (state.isRespawning) {
                state.respawnTimerSec = (state.respawnTimerSec - dt).coerceAtLeast(0f)
                state.fallAnimTimerSec = (state.fallAnimTimerSec - dt).coerceAtLeast(0f)
                state.arrivalAnimTimerSec = (state.arrivalAnimTimerSec - dt).coerceAtLeast(0f)
                state.portalScale = (state.respawnTimerSec / 1.05f).coerceIn(0f, 1f)
                if (state.respawnTimerSec <= 0f) {
                    state.isRespawning = false
                    state.portalScale = 0f
                }
            }

            if (r.finished) {
                r.animState = CharAnimState.VICTORY_DANCE
                r.yaw += dt * 75f
                r.squashY += (1f - r.squashY) * minOf(1f, dt * 10f)
                continue
            }

            if (r.hitStunTimer > 0f) r.hitStunTimer -= dt
            if (r.jumpCooldown > 0f) r.jumpCooldown -= dt
            if (r.coyoteTimer > 0f) r.coyoteTimer -= dt
            r.squashY += (1f - r.squashY) * minOf(1f, dt * 12f)

            // Choose target platform ahead of rival
            var targetPlat: RuntimePlatform? = null
            var curStanding: RuntimePlatform? = null
            var bestScore = Float.MAX_VALUE
            for (p in platforms) {
                if (!p.active) continue
                val inX = abs(r.x - p.curX) <= p.def.w * 0.5f + 0.35f
                val inZ = abs(r.z - p.curZ) <= p.def.d * 0.5f + 0.35f
                if (inX && inZ && abs(r.y - (p.curY + p.def.h * 0.5f)) < 0.7f) {
                    curStanding = p
                }
                if (p.curZ - p.def.d * 0.5f < r.z - 0.5f) {
                    val distZ = r.z - p.curZ
                    if (distZ in -1f..22f && distZ < bestScore) {
                        bestScore = distZ
                        targetPlat = p
                    }
                }
            }

            if (r.hitStunTimer <= 0f) {
                var tx = targetPlat?.curX ?: r.x
                val tz = (targetPlat?.curZ ?: (r.z - 8f))
                if (targetPlat != null && targetPlat.def.w > 4.5f) {
                    tx += r.laneBiasX * 0.55f
                }

                // Subtle lateral dodge around nearby active obstacles
                var avoidX = 0f
                for (obs in activeObstacles) {
                    val dz = r.z - obs.z
                    if (dz in 0.2f..5.5f) {
                        val obsX = when (obs.type) {
                            ObstacleType.ROLLING_BALL, ObstacleType.MOVING_WALL, ObstacleType.SLIDING_OBSTACLE, ObstacleType.PUSHER ->
                                obs.x + sin(obstacleTime * obs.speed + obs.phase) * obs.range
                            else -> obs.x
                        }
                        val dx = r.x - obsX
                        if (abs(dx) < 2.1f) {
                            avoidX += if (dx >= 0f) 2.2f else -2.2f
                        }
                    }
                }

                val dirX = (tx + avoidX) - r.x
                val dirZ = (tz - 3.0f) - r.z
                val len = sqrt(dirX * dirX + dirZ * dirZ).coerceAtLeast(0.01f)
                val speed = r.baseSpeed * (0.94f + 0.08f * sin(elapsedSec * 0.7f + r.id))
                val desVx = (dirX / len) * speed
                val desVz = (dirZ / len) * speed
                val accel = if (r.grounded) 18f else 9f
                r.vx += (desVx - r.vx) * minOf(1f, accel * dt)
                r.vz += (desVz - r.vz) * minOf(1f, accel * dt)
                val targetYaw = Math.toDegrees(atan2(r.vx.toDouble(), r.vz.toDouble())).toFloat()
                r.yaw = lerpAngle(r.yaw, targetYaw, minOf(1f, dt * 11f))

                // AI Jump decision: approaching platform front edge OR hazard ahead
                var needJump = false
                if (curStanding != null) {
                    val frontEdgeZ = curStanding.curZ - curStanding.def.d * 0.5f
                    if (r.z - frontEdgeZ < 1.35f) needJump = true
                }
                for (obs in activeObstacles) {
                    val dz = r.z - obs.z
                    if (dz in 0.4f..2.6f && abs(r.x - obs.x) < 2.2f &&
                        (obs.type == ObstacleType.ROTATING_BEAM || obs.type == ObstacleType.SPINNING_BLADE || obs.type == ObstacleType.LASER_BEAM)
                    ) {
                        needJump = true
                    }
                }
                if (needJump && (r.grounded || r.coyoteTimer > 0f) && r.jumpCooldown <= 0f) {
                    r.vy = 11.2f
                    r.grounded = false
                    r.coyoteTimer = 0f
                    r.jumpCooldown = 0.55f
                    r.squashY = 1.22f
                }
            }

            r.vy -= gravity * dt
            val prevRy = r.y
            r.x += r.vx * dt
            r.y += r.vy * dt
            r.z += r.vz * dt

            if (r.x.isNaN() || r.y.isNaN() || r.z.isNaN() || !r.x.isFinite() || !r.y.isFinite() || !r.z.isFinite()) {
                triggerIndividualRespawnForRival(r)
            }

            // Rival platform collision
            r.grounded = false
            for (rp in platforms) {
                if (!rp.active) continue
                val halfW = rp.def.w * 0.5f + 0.34f
                val halfD = rp.def.d * 0.5f + 0.34f
                val topY = rp.curY + rp.def.h * 0.5f
                if (abs(r.x - rp.curX) <= halfW && abs(r.z - rp.curZ) <= halfD) {
                    if (r.vy <= 0.6f && prevRy >= topY - 0.35f && r.y <= topY + 0.14f) {
                        r.y = topY
                        r.vy = 0f
                        r.grounded = true
                        r.coyoteTimer = 0.12f
                        r.x += (rp.curX - rp.prevX)
                        r.y += (rp.curY - rp.prevY)
                        r.z += (rp.curZ - rp.prevZ)
                        when (rp.def.type) {
                            PlatformType.BOUNCE_PAD -> {
                                r.vy = 16.2f
                                r.grounded = false
                                r.squashY = 1.3f
                            }
                            PlatformType.SPEED_BOOST -> {
                                r.vx += rp.def.dirX * 13f * dt
                                r.vz += rp.def.dirZ * 13f * dt
                            }
                            PlatformType.FINISH -> {
                                r.finished = true
                                state.finished = true
                                r.finishOrder = nextFinishOrder++
                                r.finishTimeMs = (elapsedSec * 1000f).toLong()
                                spawnBurst(r.x, r.y + 1.5f, r.z, 12, r.skin.hoodieColor, 4.0f)
                            }
                            else -> {}
                        }
                        break
                    }
                }
            }

            // Rival ordered checkpoint activation (independent per rival)
            for (cIdx in 0 until activeCheckpoints.size) {
                val cp = activeCheckpoints[cIdx]
                val distSq = (r.x - cp.x) * (r.x - cp.x) + (r.z - cp.z) * (r.z - cp.z)
                if (distSq < 11.5f && abs(r.y - cp.y) < 3.4f) {
                    tryActivateCheckpointForRival(r, cp)
                }
            }

            // Rival obstacle knockback check
            if (r.hitStunTimer <= 0.1f) {
                for (obs in activeObstacles) {
                    if (abs(r.z - obs.z) > 5f) continue
                    val t = obstacleTime * obs.speed + obs.phase
                    val (ox, oy, oz) = when (obs.type) {
                        ObstacleType.ROLLING_BALL, ObstacleType.MOVING_WALL, ObstacleType.SLIDING_OBSTACLE, ObstacleType.PUSHER ->
                            Triple(obs.x + sin(t) * obs.range, obs.y + 0.9f, obs.z)
                        ObstacleType.ROTATING_HAMMER, ObstacleType.ROTATING_BEAM, ObstacleType.SPINNING_BLADE ->
                            Triple(obs.x + cos(t) * obs.range * 0.65f, obs.y + 0.75f, obs.z + sin(t) * obs.range * 0.65f)
                        else -> Triple(obs.x, obs.y + 0.8f, obs.z)
                    }
                    val dx = r.x - ox
                    val dy = (r.y + 0.6f) - oy
                    val dz = r.z - oz
                    if (dx * dx + dy * dy + dz * dz < 1.15f) {
                        val len = sqrt(dx * dx + dz * dz).coerceAtLeast(0.05f)
                        r.vx = (dx / len) * 9.5f
                        r.vz = (dz / len) * 9.5f + 2f
                        r.vy = 4.8f
                        r.hitStunTimer = 0.4f
                        r.grounded = false
                        break
                    }
                }
            }

            state.currentX = r.x
            state.currentY = r.y
            state.currentZ = r.z
            state.raceProgress = ((-r.z) / totalCourseDist).coerceIn(0f, 1f)

            // Rival individual void/out-of-bounds respawn (ONLY respawns this specific rival)
            val rivalOutOfBounds = abs(r.x) > 45f || r.z > currentLevel.startZ + 25f
            if (r.y < prevFloorMinY || rivalOutOfBounds) {
                triggerIndividualRespawnForRival(r)
            }

            val spd = sqrt(r.vx * r.vx + r.vz * r.vz)
            r.animState = when {
                state.fallAnimTimerSec > 0f -> CharAnimState.FALL
                r.hitStunTimer > 0f -> CharAnimState.HIT
                !r.grounded && r.vy > 1.2f -> CharAnimState.JUMP
                !r.grounded -> CharAnimState.FALL
                spd > 6.5f -> CharAnimState.SPRINT
                spd > 2.5f -> CharAnimState.RUN
                else -> CharAnimState.IDLE
            }
        }
    }

    fun computePlayerRaceRank(): Int {
        if (currentMode != GameMode.RIVAL_RACE) return 1
        if (playerFinishOrder > 0) return playerFinishOrder
        var aheadCount = 0
        for (i in 0 until rivals.size) {
            val r = rivals[i]
            if (r.finished || r.z < pz) aheadCount++
        }
        return aheadCount + 1
    }

    private fun extendEndlessTrackIfNeeded() {
        while (pz < endlessNextZ + 95f) {
            val res = LevelCatalog.generateEndlessSegment(endlessSegIndex++, survivalTier, endlessNextY, endlessNextZ, endlessRng)
            for (p in res.platforms) platforms += RuntimePlatform(p)
            activeObstacles.addAll(res.obstacles)
            activeCheckpoints.addAll(res.checkpoints)
            for (c in res.coins) coins += RuntimeCoin(c)
            for (pu in res.powerUps) powerUps += RuntimePowerUp(pu)
            endlessNextY = res.nextY
            endlessNextZ = res.nextZ
        }
        val minY = platforms.minOfOrNull { it.def.y } ?: 0f
        prevFloorMinY = minY - 9.5f

        val cullZ = pz + 45f
        for (i in platforms.size - 1 downTo 0) {
            if (platforms[i].curZ > cullZ) platforms.removeAt(i)
        }
        for (i in activeObstacles.size - 1 downTo 0) {
            if (activeObstacles[i].z > cullZ) activeObstacles.removeAt(i)
        }
        for (i in coins.size - 1 downTo 0) {
            if (coins[i].curZ > cullZ) coins.removeAt(i)
        }
        for (i in powerUps.size - 1 downTo 0) {
            if (powerUps[i].def.z > cullZ) powerUps.removeAt(i)
        }
    }

    private fun updateGhostPlayback() {
        val frames = activeGhostFrames
        if (frames.size >= 2) {
            val lastFrame = frames.last()
            if (elapsedSec > lastFrame.timeSec + 0.5f) {
                ghostActive = false
                return
            }
            ghostActive = true
            var idx = 0
            while (idx < frames.size - 2 && frames[idx + 1].timeSec < elapsedSec) {
                idx++
            }
            val f0 = frames[idx]
            val f1 = frames[idx + 1]
            val span = (f1.timeSec - f0.timeSec).coerceAtLeast(0.001f)
            val alpha = ((elapsedSec - f0.timeSec) / span).coerceIn(0f, 1f)
            ghostX = f0.x + (f1.x - f0.x) * alpha
            ghostY = f0.y + (f1.y - f0.y) * alpha
            ghostZ = f0.z + (f1.z - f0.z) * alpha
            ghostYaw = lerpAngle(f0.yaw, f1.yaw, alpha)
            return
        }
        // In TIME_ATTACK mode, if no personal best ghost exists yet, show a Target Par-Time Pace Ghost!
        if (currentMode == GameMode.TIME_ATTACK && platforms.isNotEmpty()) {
            val goalSec = (WorldProgressionGraph.goalTimeMsForLevel(currentLevel.number) / 1000f).coerceAtLeast(18f)
            val frac = (elapsedSec / goalSec).coerceIn(0f, 1f)
            if (frac < 1f) {
                ghostActive = true
                ghostZ = currentLevel.startZ + (currentLevel.finishZ - currentLevel.startZ) * frac
                var bestPlat: RuntimePlatform? = null
                var minDz = Float.MAX_VALUE
                for (i in 0 until platforms.size) {
                    val rp = platforms[i]
                    if (rp.def.type == PlatformType.SECRET_PATH) continue
                    val dz = abs(rp.curZ - ghostZ)
                    if (dz < minDz) {
                        minDz = dz
                        bestPlat = rp
                    }
                }
                ghostX = bestPlat?.curX ?: 0f
                ghostY = (bestPlat?.let { it.curY + it.def.h * 0.5f } ?: currentLevel.startY) + abs(sin(elapsedSec * 4.5f)) * 0.45f
                ghostYaw = 180f
            } else {
                ghostActive = false
            }
        } else {
            ghostActive = false
        }
    }

    private fun complexityLabelFor(tier: Int): String = when {
        tier <= 1 -> "WARMUP"
        tier == 2 -> "RISING PACE"
        tier == 3 -> "DOUBLE HAZARD"
        tier == 4 -> "GAUNTLET"
        tier == 5 -> "HYPER ORBIT"
        else -> "OVERDRIVE"
    }

    private fun checkObstacleCollision(obs: ObstacleDef) {
        if (hitStunTimer > 0.15f) return
        // Broad-phase Z cull (skip obstacles more than 9m away)
        if (abs(pz - obs.z) > 9.0f) return

        val t = obstacleTime * obs.speed + obs.phase
        val playerCenterY = py + 0.65f
        val playerRadius = 0.36f

        when (obs.type) {
            ObstacleType.ROTATING_HAMMER -> {
                // 1) Center turret base & pillar
                val turretDx = px - obs.x
                val turretDz = pz - obs.z
                val turretR = 0.62f * obs.scale + playerRadius
                if (turretDx * turretDx + turretDz * turretDz < turretR * turretR &&
                    py < obs.y + 2.15f * obs.scale && py + 1.35f > obs.y
                ) {
                    applyKnockback(turretDx, turretDz, 10.5f, 5.0f)
                    return
                }
                // 2) Full rotating horizontal arm + heavy war-mallet head at 2.55f * obs.scale
                val angle = t
                val dirX = cos(angle)
                val dirZ = sin(angle)
                val hy = obs.y + 0.85f
                if (abs(playerCenterY - hy) < 1.05f * obs.scale) {
                    // Check samples along the shaft from center to tip
                    val shaftSamples = floatArrayOf(0.55f, 1.05f, 1.55f, 2.05f, 2.55f)
                    for (dist in shaftSamples) {
                        val cx = obs.x + dirX * (dist * obs.scale)
                        val cz = obs.z + dirZ * (dist * obs.scale)
                        val hitR = (if (dist >= 2.0f) 0.92f else 0.56f) * obs.scale + playerRadius
                        val dx = px - cx
                        val dz = pz - cz
                        if (dx * dx + dz * dz < hitR * hitR) {
                            applyKnockback(dx, dz, 11.8f, 5.6f)
                            return
                        }
                    }
                }
                return
            }

            ObstacleType.ROTATING_BEAM -> {
                // 1) Center turret pillar
                val turretDx = px - obs.x
                val turretDz = pz - obs.z
                val turretR = 0.60f * obs.scale + playerRadius
                if (turretDx * turretDx + turretDz * turretDz < turretR * turretR &&
                    py < obs.y + 1.75f * obs.scale && py + 1.35f > obs.y
                ) {
                    applyKnockback(turretDx, turretDz, 10.5f, 5.0f)
                    return
                }
                // 2) Full two-sided beam (-halfLen .. +halfLen). Previously only positive half was checked!
                val angle = t
                val halfLen = obs.range * 0.95f
                val hy = obs.y + 0.75f
                if (abs(playerCenterY - hy) < 0.88f * obs.scale) {
                    val dirX = cos(angle)
                    val dirZ = sin(angle)
                    val relX = px - obs.x
                    val relZ = pz - obs.z
                    // Project player onto the rotating beam segment [-halfLen, +halfLen]
                    val proj = (relX * dirX + relZ * dirZ).coerceIn(-halfLen, halfLen)
                    val closestX = obs.x + dirX * proj
                    val closestZ = obs.z + dirZ * proj
                    val dx = px - closestX
                    val dz = pz - closestZ
                    val beamRadius = 0.36f * obs.scale + playerRadius
                    if (dx * dx + dz * dz < beamRadius * beamRadius) {
                        applyKnockback(dx, dz, 11.5f, 5.5f)
                        return
                    }
                }
                return
            }

            ObstacleType.SPINNING_BLADE -> {
                // 1) Center turret pillar
                val turretDx = px - obs.x
                val turretDz = pz - obs.z
                val turretR = 0.60f * obs.scale + playerRadius
                if (turretDx * turretDx + turretDz * turretDz < turretR * turretR &&
                    py < obs.y + 1.65f * obs.scale && py + 1.35f > obs.y
                ) {
                    applyKnockback(turretDx, turretDz, 10.5f, 5.0f)
                    return
                }
                // 2) Both perpendicular cross-blades (0°/180° AND 90°/270°). Previously only 1 half-arm was checked!
                val halfLen = obs.range * 0.97f
                val hy = obs.y + 0.75f
                if (abs(playerCenterY - hy) < 0.82f * obs.scale) {
                    val relX = px - obs.x
                    val relZ = pz - obs.z
                    val bladeRadius = 0.38f * obs.scale + playerRadius
                    for (axisOffset in floatArrayOf(0f, (Math.PI * 0.5).toFloat())) {
                        val a = t + axisOffset
                        val dirX = cos(a)
                        val dirZ = sin(a)
                        val proj = (relX * dirX + relZ * dirZ).coerceIn(-halfLen, halfLen)
                        val closestX = obs.x + dirX * proj
                        val closestZ = obs.z + dirZ * proj
                        val dx = px - closestX
                        val dz = pz - closestZ
                        if (dx * dx + dz * dz < bladeRadius * bladeRadius) {
                            applyKnockback(dx, dz, 11.8f, 5.6f)
                            return
                        }
                    }
                }
                return
            }

            ObstacleType.ROLLING_BALL -> {
                val ox = obs.x + sin(t) * obs.range
                val oy = obs.y
                val oz = obs.z
                val hitR = 1.05f * obs.scale + playerRadius
                val dx = px - ox
                val dy = playerCenterY - oy
                val dz = pz - oz
                if (dx * dx + dy * dy + dz * dz < hitR * hitR) {
                    applyKnockback(dx, dz, 11.5f, 5.8f)
                }
                return
            }

            ObstacleType.SWINGING_PENDULUM, ObstacleType.GIANT_SWINGER -> {
                // 1) Twin side marble columns at obs.x ± 3.2f
                for (side in floatArrayOf(-3.2f, 3.2f)) {
                    val colX = obs.x + side
                    val dx = px - colX
                    val dz = pz - obs.z
                    val colR = 0.48f + playerRadius
                    if (dx * dx + dz * dz < colR * colR && py < obs.y + 4.5f && py + 1.35f > obs.y) {
                        applyKnockback(dx, dz, 9.5f, 4.2f)
                        return
                    }
                }
                // 2) Swinging crescent battle-axe head AND lower pendulum rod
                val swing = sin(t) * 1.1f
                val pivotY = obs.y + 4.2f
                for (rodFrac in floatArrayOf(0.55f, 0.78f, 1.0f)) {
                    val len = 3.4f * obs.scale * rodFrac
                    val bx = obs.x + sin(swing) * len
                    val by = pivotY - cos(swing) * len
                    val hitX = (if (rodFrac >= 0.95f) 1.32f else 0.45f) * obs.scale + playerRadius
                    val hitY = (if (rodFrac >= 0.95f) 0.95f else 0.55f) * obs.scale + 0.55f
                    val hitZ = 0.45f * obs.scale + playerRadius
                    if (abs(px - bx) < hitX && abs(playerCenterY - by) < hitY && abs(pz - obs.z) < hitZ) {
                        val dx = px - bx
                        val dz = if (abs(pz - obs.z) < 0.05f) 0.6f else (pz - obs.z)
                        applyKnockback(dx, dz, 12.0f, 5.8f)
                        return
                    }
                }
                return
            }

            ObstacleType.MOVING_WALL, ObstacleType.SLIDING_OBSTACLE, ObstacleType.PUSHER -> {
                // Exact 3D box bounds matching drawBlock(ox, obs.y + 0.92f, obs.z, 1.95f * scale, 1.85f * scale, 1.35f * scale)
                val ox = obs.x + sin(t) * obs.range
                val oy = obs.y + 0.92f
                val halfW = 0.98f * obs.scale + playerRadius
                val halfH = 0.93f * obs.scale + 0.55f
                val halfD = 0.68f * obs.scale + playerRadius
                if (abs(px - ox) < halfW && abs(playerCenterY - oy) < halfH && abs(pz - obs.z) < halfD) {
                    val dx = px - ox
                    val dz = pz - obs.z
                    applyKnockback(dx, dz, 11.2f, 5.5f)
                }
                return
            }

            ObstacleType.JUMPING_BLOCK -> {
                // Exact 3D box bounds matching drawBlock(obs.x, oy, obs.z, 1.85f * scale, 1.45f * scale, 1.45f * scale)
                val oy = obs.y + 0.7f + abs(sin(t)) * 2.4f
                val halfW = 0.93f * obs.scale + playerRadius
                val halfH = 0.85f * obs.scale + 0.55f
                val halfD = 0.73f * obs.scale + playerRadius
                if (abs(px - obs.x) < halfW && abs(playerCenterY - oy) < halfH && abs(pz - obs.z) < halfD) {
                    applyKnockback(px - obs.x, pz - obs.z, 11.0f, 5.8f)
                }
                return
            }

            ObstacleType.FIRE_JET -> {
                // 1) Solid floor nozzle base
                val dx = px - obs.x
                val dz = pz - obs.z
                val baseR = 0.62f + playerRadius
                if (dx * dx + dz * dz < baseR * baseR && py < obs.y + 0.48f && py + 1.35f > obs.y) {
                    applyKnockback(dx, dz, 9.0f, 4.5f)
                    return
                }
                // 2) Active vertical flame column (from obs.y to obs.y + 2.5f * scale)
                if (sin(t) > 0.1f) {
                    val flameR = 0.65f * obs.scale + playerRadius
                    if (dx * dx + dz * dz < flameR * flameR &&
                        py < obs.y + 2.5f * obs.scale && py + 1.35f > obs.y + 0.2f
                    ) {
                        applyKnockback(dx, dz, 11.2f, 6.2f)
                    }
                }
                return
            }

            ObstacleType.LASER_BEAM -> {
                // 1) Twin side emitter pillars at obs.x ± obs.range
                for (side in floatArrayOf(-obs.range, obs.range)) {
                    val postX = obs.x + side
                    val dx = px - postX
                    val dz = pz - obs.z
                    val postR = 0.36f + playerRadius
                    if (dx * dx + dz * dz < postR * postR && py < obs.y + 1.95f && py + 1.35f > obs.y) {
                        applyKnockback(dx, dz, 9.5f, 4.5f)
                        return
                    }
                }
                // 2) Active horizontal laser beam
                if (sin(t) > -0.25f) {
                    val oy = obs.y + 0.85f
                    if (abs(pz - obs.z) < 0.42f + playerRadius &&
                        abs(playerCenterY - oy) < 0.72f &&
                        abs(px - obs.x) <= obs.range + playerRadius
                    ) {
                        val pushZ = if (pz >= obs.z) 1f else -1f
                        applyKnockback(0f, pushZ, 10.2f, 5.2f)
                    }
                }
                return
            }

            ObstacleType.FALLING_STALACTITE -> {
                val cycle = (t % 2.6f + 2.6f) % 2.6f
                val dropFrac = (cycle / 1.85f).coerceIn(0f, 1f)
                val oy = obs.y + 5.2f * (1f - dropFrac * dropFrac)
                val spikeTop = oy + 0.25f * obs.scale
                val spikeBottom = oy - 2.2f * obs.scale
                val dx = px - obs.x
                val dz = pz - obs.z
                val hitR = 0.62f * obs.scale + playerRadius
                if (dx * dx + dz * dz < hitR * hitR &&
                    py + 1.35f >= spikeBottom && py <= spikeTop
                ) {
                    applyKnockback(dx, dz, 11.0f, 5.5f)
                }
                return
            }
        }
    }

    private fun applyKnockback(dx: Float, dz: Float, force: Float, lift: Float) {
        // If Golden Aegis Shield is active, absorb the collision completely without knockback!
        if (shieldTimer > 0f) {
            shieldTimer = 0f
            hitStunTimer = 0.35f
            checkpointBannerTimer = 1.6f
            checkpointBannerText = "AEGIS SHIELD ABSORBED HIT! 🛡️"
            listener?.onSfx("checkpoint")
            listener?.onHaptic(true)
            spawnBurst(px, py + 0.8f, pz, 18, Rgba.fromHex(0x00E5FF), 4.5f)
            spawnBurst(px, py + 0.8f, pz, 14, Rgba.fromHex(0xFFD22B), 4.0f)
            return
        }
        val len = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01f)
        vx = (dx / len) * force
        vz = (dz / len) * force + 2.5f
        vy = lift
        grounded = false
        hitStunTimer = 0.42f
        squashY = 0.72f
        if (cameraShakeEnabled) {
            cameraShakeTimer = 0.24f
            cameraShakeIntensity = 0.22f
        }
        listener?.onSfx("hit")
        listener?.onHaptic(true)
        // Bright collision sparks (شرارات عند الاصطدام)
        spawnBurst(px, py + 0.8f, pz, 10, Rgba.fromHex(0xFFD22B), 4.6f)
        spawnBurst(px, py + 0.8f, pz, 8, Rgba.fromHex(0xFF3B5C), 3.8f)
    }

    private fun updateFollowCamera(dt: Float) {
        val dist = 8.6f
        val yawRad = Math.toRadians(cameraYawOffset.toDouble())
        val pitchRad = Math.toRadians(cameraPitchDeg.toDouble())
        val horizDist = dist * cos(pitchRad).toFloat()
        val vertDist = dist * sin(pitchRad).toFloat() + 1.25f

        val shakeX = if (cameraShakeEnabled && cameraShakeTimer > 0f) sin(worldTime * 55f) * cameraShakeIntensity * (cameraShakeTimer / 0.24f) else 0f
        val shakeY = if (cameraShakeEnabled && cameraShakeTimer > 0f) cos(worldTime * 48f) * cameraShakeIntensity * (cameraShakeTimer / 0.24f) else 0f

        val targetCamX = px + sin(yawRad).toFloat() * horizDist + shakeX
        val targetCamZ = pz + cos(yawRad).toFloat() * horizDist
        var targetCamY = maxOf(py + vertDist + shakeY, prevFloorMinY + 4.5f)

        // Camera collision avoidance: never place camera inside any platform block or large archway structure
        for (i in 0 until platforms.size) {
            val rp = platforms[i]
            if (!rp.active) continue
            val halfW = rp.def.w * 0.5f + 0.5f
            val halfD = rp.def.d * 0.5f + 0.5f
            if (abs(targetCamX - rp.curX) <= halfW && abs(targetCamZ - rp.curZ) <= halfD) {
                val minCamY = rp.curY + rp.def.h * 0.5f + 1.1f
                if (targetCamY < minCamY) targetCamY = minCamY
            }
        }
        // Prevent camera clipping through checkpoint arches or pendulum crossbeams
        for (i in 0 until activeCheckpoints.size) {
            val cp = activeCheckpoints[i]
            if (abs(targetCamZ - cp.z) < 1.1f && abs(targetCamX - cp.x) < 3.0f) {
                if (targetCamY in (cp.y + 2.3f)..(cp.y + 4.4f)) {
                    targetCamY = cp.y + 4.45f
                }
            }
        }
        for (i in 0 until activeObstacles.size) {
            val obs = activeObstacles[i]
            if (obs.type == ObstacleType.SWINGING_PENDULUM || obs.type == ObstacleType.GIANT_SWINGER) {
                if (abs(targetCamZ - obs.z) < 1.2f && abs(targetCamX - obs.x) < 3.8f) {
                    if (targetCamY in (obs.y + 3.4f)..(obs.y + 5.3f)) {
                        targetCamY = obs.y + 5.35f
                    }
                }
            }
        }

        val smooth = minOf(1f, dt * 8.5f)
        camX += (targetCamX - camX) * smooth
        camY += (targetCamY - camY) * (smooth * 0.75f)
        camZ += (targetCamZ - camZ) * smooth

        // Show generous look-ahead space in front of the player
        camLookX += (px - camLookX) * minOf(1f, dt * 12f)
        camLookY += (py + 1.08f - camLookY) * minOf(1f, dt * 10f)
        camLookZ += (pz - 2.15f - camLookZ) * minOf(1f, dt * 12f)
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, count: Int, col: Rgba, speed: Float) {
        val cap = when (graphicsQuality) {
            GraphicsQuality.LOW -> count / 3
            GraphicsQuality.MEDIUM -> (count * 2) / 3
            GraphicsQuality.HIGH -> count
        }.coerceAtLeast(2)
        val maxParticles = when (graphicsQuality) {
            GraphicsQuality.LOW -> 35
            GraphicsQuality.MEDIUM -> 80
            GraphicsQuality.HIGH -> 150
        }
        for (i in 0 until cap) {
            if (particles.size >= maxParticles) break
            val a = Random.nextFloat() * 6.283f
            val sp = (0.35f + Random.nextFloat() * 0.65f) * speed
            particles += Particle(
                x, y, z,
                cos(a) * sp, 1.8f + Random.nextFloat() * speed, sin(a) * sp,
                0.55f + Random.nextFloat() * 0.35f, 0.85f,
                0.20f + Random.nextFloat() * 0.16f, col
            )
        }
    }

    private fun updateParticles(dt: Float) {
        var i = particles.size - 1
        while (i >= 0) {
            val p = particles[i]
            p.life -= dt
            if (p.life <= 0f) {
                particles.removeAt(i)
            } else {
                p.vy -= 9.5f * dt
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += p.vz * dt
            }
            i--
        }
    }

    fun snapshot(fps: Int): HudSnapshot {
        val totalDist = (-currentLevel.finishZ).coerceAtLeast(10f)
        val tierDuration = if (currentMode == GameMode.ENDLESS) 14f else 18f
        val tierProg = ((elapsedSec % tierDuration) / tierDuration).coerceIn(0f, 1f)
        val prog = if (currentMode == GameMode.ENDLESS) tierProg else ((-pz) / totalDist).coerceIn(0f, 1f)
        val totalCps = activeCheckpoints.size.coerceAtLeast(4)
        return HudSnapshot(
            levelNumber = currentLevel.number,
            levelTitle = currentLevel.title,
            worldName = currentWorld.name,
            mode = currentMode,
            progressFraction = prog,
            coinsCollected = coinsCollected,
            elapsedMs = (elapsedSec * 1000f).toLong(),
            distanceM = maxDistanceM,
            obstaclesPassed = obstaclesPassed,
            dashCooldownFraction = (dashCooldown / 2.4f).coerceIn(0f, 1f),
            checkpointReached = highestCheckpointIndex,
            totalCheckpoints = totalCps,
            checkpointBannerText = checkpointBannerText,
            isPlayerRespawning = playerCheckpointState.isRespawning,
            respawnProgress = playerCheckpointState.portalScale,
            fps = fps,
            isVictory = runFinished,
            isFailed = runFailed,
            hasCheckpoint = highestCheckpointIndex > 0,
            survivalTier = survivalTier,
            speedMultiplier = speedMultiplier,
            tierProgressFraction = tierProg,
            complexityLabel = complexityLabelFor(survivalTier),
            tierBannerText = tierBannerText,
            raceRank = computePlayerRaceRank(),
            totalRacers = if (currentMode == GameMode.RIVAL_RACE) rivals.size + 1 else 1,
            jumpsInRun = jumpsInRun,
            goalTimeMs = if (currentLevel.number in 1..50 &&
                (currentMode == GameMode.CAREER || currentMode == GameMode.TIME_ATTACK || currentMode == GameMode.RIVAL_RACE)
            ) {
                WorldProgressionGraph.goalTimeMsForLevel(currentLevel.number)
            } else {
                0L
            },
            hazardWarningActive = hazardWarningActive,
            shieldRemainingSec = shieldTimer,
            magnetRemainingSec = magnetTimer,
            wingsRemainingSec = wingsTimer
        )
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = ((b - a + 540f) % 360f) - 180f
        return a + diff * t
    }
}
