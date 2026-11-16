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
    val goalTimeMs: Long = 0L
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
    var collected: Boolean = false
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
        jumpsInRun: Int
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
    val activeObstacles = ArrayList<ObstacleDef>()
    val activeCheckpoints = ArrayList<CheckpointDef>()
    val rivals = ArrayList<RuntimeRival>()
    val particles = ArrayList<Particle>(160)

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
        activeObstacles.clear()
        activeCheckpoints.clear()
        checkpointPulseTimers.clear()
        rivals.clear()
        particles.clear()
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
        activeObstacles.clear()
        activeObstacles.addAll(level.obstacles)
        activeCheckpoints.clear()
        activeCheckpoints.addAll(level.checkpoints)
        checkpointPulseTimers.clear()
        rivals.clear()
        particles.clear()

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
        squashY = 1.26f
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
            val maxSpeed = 8.4f
            val targetVx = inputX * maxSpeed
            val targetVz = inputZ * maxSpeed
            val accel = if (grounded) 38f * surfaceFriction else 16f
            val decel = if (grounded) 24f * surfaceFriction else 5f

            if (mag > 0.08f) {
                vx += (targetVx - vx) * minOf(1f, accel * dt)
                vz += (targetVz - vz) * minOf(1f, accel * dt)
                val targetYaw = Math.toDegrees(atan2(inputX.toDouble(), inputZ.toDouble())).toFloat()
                playerYaw = lerpAngle(playerYaw, targetYaw, minOf(1f, dt * 13f))
            } else {
                vx *= (1f - minOf(1f, decel * dt))
                vz *= (1f - minOf(1f, decel * dt))
            }
        }

        // Jump execution (coyote + buffer)
        if (jumpBufferTimer > 0f && (grounded || coyoteTimer > 0f) && hitStunTimer <= 0f) {
            vy = 11.6f
            grounded = false
            coyoteTimer = 0f
            jumpBufferTimer = 0f
            jumpsInRun++
            squashY = 1.24f
            listener?.onSfx("jump")
            listener?.onHaptic(false)
            spawnBurst(px, py + 0.1f, pz, 7, Rgba(1f, 1f, 1f, 0.9f), 2.2f)
        }

        // Gravity
        val gravity = 28.5f * currentWorld.gravityMultiplier
        vy -= gravity * dt

        val prevY = py
        px += vx * dt
        py += vy * dt
        pz += vz * dt

        // Guard against NaN/infinite physics anomalies
        if (px.isNaN() || py.isNaN() || pz.isNaN() || !px.isFinite() || !py.isFinite() || !pz.isFinite()) {
            triggerIndividualRespawnForPlayer()
        }

        // Platform collision resolution (generous rounded-character cylinder/AABB + ledge step-up)
        val wasGrounded = grounded
        grounded = false
        val playerRadius = 0.36f
        for (rp in platforms) {
            if (!rp.active) continue
            val halfW = rp.def.w * 0.5f + playerRadius
            val halfD = rp.def.d * 0.5f + playerRadius
            val topY = rp.curY + rp.def.h * 0.5f
            val botY = rp.curY - rp.def.h * 0.5f

            if (abs(px - rp.curX) <= halfW && abs(pz - rp.curZ) <= halfD) {
                // Landing on top or smooth ledge step-up within 0.18m
                if ((vy <= 0.6f && prevY >= topY - 0.35f && py <= topY + 0.12f) ||
                    (py >= topY - 0.18f && py < topY && vy > -2.5f)
                ) {
                    py = topY
                    vy = 0f
                    grounded = true
                    coyoteTimer = 0.14f
                    standingPlatform = rp

                    // Carry player on moving platform
                    px += (rp.curX - rp.prevX)
                    py += (rp.curY - rp.prevY)
                    pz += (rp.curZ - rp.prevZ)

                    if (!wasGrounded) {
                        landTimer = 0.12f
                        squashY = 0.78f
                        listener?.onSfx("land")
                        spawnBurst(px, py + 0.05f, pz, 5, Rgba(0.9f, 0.95f, 1f, 0.8f), 1.6f)
                    }

                    // Special platform effects
                    when (rp.def.type) {
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
                            if (rp.fallTimer < 0f) rp.fallTimer = 0f
                        }
                        PlatformType.FINISH -> {
                            if (!runFinished) {
                                runFinished = true
                                playerCheckpointState.finished = true
                                playerFinishOrder = nextFinishOrder++
                                val rank = if (currentMode == GameMode.RIVAL_RACE) playerFinishOrder else 1
                                val totalR = if (currentMode == GameMode.RIVAL_RACE) rivals.size + 1 else 1
                                listener?.onSfx("victory")
                                listener?.onHaptic(true)
                                spawnBurst(px, py + 1.8f, pz, 30, Rgba.fromHex(0xFFD22B), 5.5f)
                                spawnBurst(px, py + 2.1f, pz, 25, Rgba.fromHex(0x00D4FF), 5.5f)
                                listener?.onLevelWon(
                                    currentLevel.number,
                                    (elapsedSec * 1000f).toLong(),
                                    coinsCollected,
                                    maxDistanceM,
                                    obstaclesPassed,
                                    rank,
                                    totalR,
                                    jumpsInRun
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

        // Coins collection
        for (i in 0 until coins.size) {
            val rc = coins[i]
            if (rc.collected) continue
            val dx = px - rc.def.x
            val dy = (py + 0.7f) - rc.def.y
            val dz = pz - rc.def.z
            if (dx * dx + dy * dy + dz * dz < 1.85f) {
                rc.collected = true
                coinsCollected++
                listener?.onSfx("coin")
                listener?.onHaptic(false)
                spawnBurst(rc.def.x, rc.def.y, rc.def.z, 10, Rgba.fromHex(0xFFD22B), 3.0f)
            }
        }

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

        // Running dust & equipped trail particles
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
            if (coins[i].def.z > cullZ) coins.removeAt(i)
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
        val t = obstacleTime * obs.speed + obs.phase
        var ox = obs.x
        var oy = obs.y
        var oz = obs.z
        var radius = 0.85f * obs.scale

        when (obs.type) {
            ObstacleType.ROTATING_HAMMER, ObstacleType.ROTATING_BEAM, ObstacleType.SPINNING_BLADE -> {
                val angle = t
                val armLen = obs.range * 0.82f
                val hx = obs.x + cos(angle) * armLen * 0.65f
                val hz = obs.z + sin(angle) * armLen * 0.65f
                val hy = obs.y + 0.75f
                // Check distance along rotating arm
                for (frac in listOf(0.35f, 0.68f, 0.95f)) {
                    val cx = obs.x + cos(angle) * armLen * frac
                    val cz = obs.z + sin(angle) * armLen * frac
                    val dx = px - cx
                    val dz = pz - cz
                    val dy = (py + 0.65f) - hy
                    if (dx * dx + dz * dz < 0.95f * obs.scale && abs(dy) < 1.05f) {
                        applyKnockback(dx, dz, 11.5f, 5.5f)
                        return
                    }
                }
                return
            }
            ObstacleType.ROLLING_BALL -> {
                ox = obs.x + sin(t) * obs.range
                oy = obs.y
                oz = obs.z
                radius = 1.15f * obs.scale
            }
            ObstacleType.SWINGING_PENDULUM, ObstacleType.GIANT_SWINGER -> {
                val swing = sin(t) * 1.1f
                ox = obs.x + sin(swing) * 3.4f * obs.scale
                oy = obs.y + 4.2f - cos(swing) * 3.4f * obs.scale
                radius = 1.1f * obs.scale
            }
            ObstacleType.MOVING_WALL, ObstacleType.SLIDING_OBSTACLE, ObstacleType.PUSHER -> {
                ox = obs.x + sin(t) * obs.range
                oy = obs.y + 0.85f
                radius = 1.1f * obs.scale
            }
            ObstacleType.JUMPING_BLOCK -> {
                oy = obs.y + 0.7f + abs(sin(t)) * 2.4f
                radius = 1.0f * obs.scale
            }
            ObstacleType.FIRE_JET -> {
                val jetActive = sin(t) > 0.1f
                if (!jetActive) return
                oy = obs.y + 1.2f
                radius = 0.85f * obs.scale
            }
            ObstacleType.LASER_BEAM -> {
                val pulse = sin(t) > -0.25f
                if (!pulse) return
                oy = obs.y + 0.75f
                if (abs(pz - obs.z) < 0.55f && abs((py + 0.65f) - oy) < 0.65f && abs(px - obs.x) < obs.range) {
                    applyKnockback(0f, 1f, 9.5f, 5.0f)
                }
                return
            }
        }

        val dx = px - ox
        val dy = (py + 0.65f) - oy
        val dz = pz - oz
        if (dx * dx + dy * dy + dz * dz < radius * radius) {
            applyKnockback(dx, dz, 11.0f, 5.8f)
        }
    }

    private fun applyKnockback(dx: Float, dz: Float, force: Float, lift: Float) {
        val len = sqrt(dx * dx + dz * dz).coerceAtLeast(0.01f)
        vx = (dx / len) * force
        vz = (dz / len) * force + 2.5f
        vy = lift
        grounded = false
        hitStunTimer = 0.42f
        squashY = 0.72f
        listener?.onSfx("hit")
        listener?.onHaptic(true)
        spawnBurst(px, py + 0.8f, pz, 12, Rgba.fromHex(0xFF3B5C), 4.0f)
    }

    private fun updateFollowCamera(dt: Float) {
        val dist = 8.4f
        val yawRad = Math.toRadians(cameraYawOffset.toDouble())
        val pitchRad = Math.toRadians(cameraPitchDeg.toDouble())
        val horizDist = dist * cos(pitchRad).toFloat()
        val vertDist = dist * sin(pitchRad).toFloat() + 1.2f

        val targetCamX = px + sin(yawRad).toFloat() * horizDist
        val targetCamZ = pz + cos(yawRad).toFloat() * horizDist
        var targetCamY = maxOf(py + vertDist, prevFloorMinY + 4.5f)

        // Camera collision avoidance: never place camera inside any platform block
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

        val smooth = minOf(1f, dt * 8.5f)
        camX += (targetCamX - camX) * smooth
        camY += (targetCamY - camY) * (smooth * 0.75f)
        camZ += (targetCamZ - camZ) * smooth

        camLookX += (px - camLookX) * minOf(1f, dt * 12f)
        camLookY += (py + 1.05f - camLookY) * minOf(1f, dt * 10f)
        camLookZ += (pz - 1.6f - camLookZ) * minOf(1f, dt * 12f)
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, count: Int, col: Rgba, speed: Float) {
        val cap = when (graphicsQuality) {
            GraphicsQuality.LOW -> count / 2
            GraphicsQuality.MEDIUM -> (count * 3) / 4
            GraphicsQuality.HIGH -> count
        }.coerceAtLeast(2)
        for (i in 0 until cap) {
            if (particles.size >= 150) break
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
            }
        )
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = ((b - a + 540f) % 360f) - 180f
        return a + diff * t
    }
}
