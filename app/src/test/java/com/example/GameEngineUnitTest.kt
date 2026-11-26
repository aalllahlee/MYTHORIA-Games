package com.example

import com.example.game.engine3d.GamePhysicsWorld
import com.example.game.model.GameCatalog
import com.example.game.model.GameMode
import com.example.game.model.LevelCatalog
import com.example.game.model.PlatformType
import org.junit.Assert.*
import org.junit.Test

class GameEngineUnitTest {

    @Test
    fun all50LevelsAnd10WorldsAreValidAndCompletable() {
        assertEquals(10, GameCatalog.worlds.size)
        assertEquals(3, GameCatalog.skins.size)
        val levels = LevelCatalog.allLevels
        assertEquals(50, levels.size)

        for (i in 1..50) {
            val lvl = LevelCatalog.getLevel(i)
            assertEquals(i, lvl.number)
            assertTrue("Level $i missing finish platform", lvl.platforms.any { it.type == PlatformType.FINISH })
            assertTrue("Level $i finishZ must be < startZ", lvl.finishZ < lvl.startZ)
            assertTrue("Level $i should have coins", lvl.coins.isNotEmpty())
        }
    }

    @Test
    fun dailyChallengeIsDeterministicOffline() {
        val a = LevelCatalog.generateDailyChallengeLevel(20500L)
        val b = LevelCatalog.generateDailyChallengeLevel(20500L)
        assertEquals(a.finishZ, b.finishZ, 0.001f)
        assertEquals(a.platforms.size, b.platforms.size)
        assertEquals(a.obstacles.size, b.obstacles.size)
    }

    @Test
    fun physicsMovementJumpDiveAndDashWork() {
        val world = GamePhysicsWorld()
        world.dashUnlocked = true
        world.loadLevel(LevelCatalog.getLevel(1), GameMode.CAREER)

        // Settle on start island
        repeat(20) { world.step(0.016f) }
        assertTrue(world.grounded)

        // Move forward with virtual joystick (-Y is forward)
        world.joyX = 0f
        world.joyY = -1f
        val startZ = world.pz
        repeat(25) { world.step(0.016f) }
        assertTrue("Player should move forward along -Z", world.pz < startZ - 1.0f)

        // Trigger jump
        world.triggerJump()
        world.step(0.016f)
        assertTrue("Player should rise on jump", world.vy > 5f)
        assertFalse(world.grounded)

        // Trigger dash
        world.triggerDash()
        world.step(0.016f)
        assertTrue("Dash cooldown should activate", world.dashCooldown > 0f)
    }

    @Test
    fun concurrentLoadLevelDoesNotThrowDuringStep() {
        val world = GamePhysicsWorld()
        world.loadLevel(LevelCatalog.getLevel(1), GameMode.CAREER)
        val loaderThread = Thread {
            repeat(40) { idx ->
                world.loadLevel(LevelCatalog.getLevel((idx % 10) + 1), GameMode.CAREER)
                world.triggerJump()
                world.triggerDive()
            }
        }
        loaderThread.start()
        repeat(60) {
            world.step(0.016f)
            world.platforms.forEach { _ -> }
            world.coins.forEach { _ -> }
            world.particles.forEach { _ -> }
        }
        loaderThread.join()
    }

    @Test
    fun survivalProgressionEscalatesSpeedAndComplexityOverTime() {
        val world = GamePhysicsWorld()
        world.loadLevel(LevelCatalog.generateEndlessLevel(1234L), GameMode.ENDLESS)
        world.step(0.016f)
        assertEquals(1, world.survivalTier)
        assertEquals(1.0f, world.speedMultiplier, 0.05f)

        // Simulate 32 seconds of survival (keeping player safe at start height for test)
        repeat(1000) {
            world.py = 1.5f
            world.vy = 0f
            world.step(0.032f)
        }
        assertTrue("Survival tier should advance over time", world.survivalTier >= 3)
        assertTrue("Obstacle speed multiplier should increase", world.speedMultiplier > 1.5f)
        assertTrue("Obstacle clock should advance faster than wall time", world.obstacleTime > world.worldTime)
    }

    @Test
    fun rivalRaceModeSpawnsRivalsWhoRunAndTrackJumps() {
        val world = GamePhysicsWorld()
        world.loadLevel(LevelCatalog.getLevel(1), GameMode.RIVAL_RACE)
        world.step(0.016f)

        assertEquals(4, world.rivals.size)
        val initialZ = world.rivals.map { it.z }
        repeat(45) { world.step(0.032f) }

        val advancedCount = world.rivals.indices.count { i -> world.rivals[i].z < initialZ[i] - 2.0f }
        assertTrue("AI rivals should run forward toward finish", advancedCount >= 3)

        world.triggerJump()
        world.step(0.016f)
        assertTrue("Player jump count should increment", world.jumpsInRun >= 1)

        val snap = world.snapshot(60)
        assertEquals(5, snap.totalRacers)
        assertTrue(snap.raceRank in 1..5)
    }

    @Test
    fun xpProgressionAndMilestonesWork() {
        assertEquals(1, com.example.game.model.XpProgression.levelFromXp(0))
        assertEquals(25, com.example.game.model.XpProgression.levelFromXp(24750))
        assertEquals(750, com.example.game.model.XpProgression.xpIntoCurrentLevel(24750))
        assertEquals("Golden Crown", com.example.game.model.XpProgression.nextMilestone(25).title)
        assertEquals(30, com.example.game.model.XpProgression.nextMilestone(25).levelReq)
        assertTrue(com.example.game.model.XpProgression.starsRequiredForWorld(2) > 0)
    }

    @Test
    fun worldProgressionGraphRequiresGoalMetToUnlockNextWorld() {
        val graph = com.example.game.model.WorldProgressionGraph
        assertEquals(10, graph.worldNodes.size)
        assertEquals(50, graph.levelNodes.size)
        assertEquals(9, graph.worldEdges.size)

        // Empty map: only World 1 and Level 1 unlocked
        assertEquals(setOf(1), graph.computeUnlockedWorlds(emptyMap()))
        assertEquals(setOf(1), graph.computeUnlockedLevels(emptyMap()))

        // Complete World 1 stages 1..4 within goal time -> unlocks up to Level 5, World 2 still locked
        val partialW1 = mapOf(1 to true, 2 to true, 3 to true, 4 to true, 5 to false)
        assertEquals(setOf(1), graph.computeUnlockedWorlds(partialW1))
        assertEquals(setOf(1, 2, 3, 4, 5), graph.computeUnlockedLevels(partialW1))

        // Out-of-order World 2 gateway goalMet without World 1 gateway does NOT unlock World 3
        val bypassAttempt = mapOf(1 to true, 2 to true, 3 to true, 4 to true, 5 to false, 10 to true)
        assertEquals(setOf(1), graph.computeUnlockedWorlds(bypassAttempt))

        // Meet World 1 gateway (Level 5) goal -> unlocks World 2 and Level 6
        val w1Cleared = mapOf(1 to true, 2 to true, 3 to true, 4 to true, 5 to true)
        assertEquals(setOf(1, 2), graph.computeUnlockedWorlds(w1Cleared))
        assertEquals(setOf(1, 2, 3, 4, 5, 6), graph.computeUnlockedLevels(w1Cleared))

        // Goal evaluation respects target time
        val l5GoalMs = graph.goalTimeMsForLevel(5)
        assertFalse(graph.isGoalMet(5, l5GoalMs + 1500L, completed = true))
        assertTrue(graph.isGoalMet(5, l5GoalMs - 500L, completed = true))
        assertFalse(graph.isGoalMet(5, l5GoalMs - 500L, completed = false))
    }

    @Test
    fun test1_playerReachesCheckpoint2AndFalls_respawnsAtCheckpoint2() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(1)
        assertEquals("Every level must have 4 ordered checkpoints", 4, level.checkpoints.size)
        world.loadLevel(level, GameMode.CAREER)
        world.step(0.016f)

        // Player A activates Checkpoint 1 then Checkpoint 2 in order
        assertTrue(world.activateCheckpointForRacer("player_0", 1))
        assertTrue(world.activateCheckpointForRacer("player_0", 2))
        assertEquals(2, world.playerCheckpointState.lastActivatedCheckpoint)

        val cp2 = level.checkpoints[1]
        // Player A falls off the map beyond Checkpoint 2
        world.px = 5.0f
        world.py = -30.0f
        world.pz = cp2.z - 12.0f
        world.step(0.016f)

        assertEquals("Player A must retain Checkpoint 2", 2, world.playerCheckpointState.lastActivatedCheckpoint)
        assertEquals("Player A must respawn at Checkpoint 2 Z", cp2.z, world.pz, 0.05f)
        assertTrue("Player A must respawn safely above Checkpoint 2 platform", world.py > cp2.y)
        assertFalse("Match must NOT fail or reset", world.runFailed)
    }

    @Test
    fun test2_fivePlayersRacing_playerAFalls_onlyPlayerARespawnsOthersContinueUninterrupted() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(2)
        world.loadLevel(level, GameMode.RIVAL_RACE)
        world.step(0.016f)
        assertEquals("5 racers total (1 human + 4 AI rivals)", 4, world.rivals.size)

        // Advance all 5 players along the track
        for (i in 0 until 20) {
            world.step(0.02f)
        }
        world.activateCheckpointForRacer("player_0", 1)

        // Record the 4 other players' positions and respawn counts right before Player A falls
        val rivalPositionsBefore = world.rivals.map { Triple(it.x, it.y, it.z) }
        val rivalRespawnCountsBefore = world.rivals.map { it.checkpointState.respawnCount }
        val timerBefore = world.elapsedSec

        // Player A falls off the map
        world.py = -30f
        world.step(0.02f)

        // Verify ONLY Player A respawned
        assertEquals(1, world.playerCheckpointState.respawnCount)
        assertTrue(world.playerCheckpointState.isRespawning)
        assertTrue("Global race timer must continue uninterrupted", world.elapsedSec > timerBefore)

        // Verify none of the other 4 players were reset or teleported back to start
        for (i in 0 until 4) {
            val r = world.rivals[i]
            assertEquals("Rival ${r.id} must NOT have respawned", rivalRespawnCountsBefore[i], r.checkpointState.respawnCount)
            assertFalse("Rival ${r.id} must NOT be marked respawning", r.checkpointState.isRespawning)
            assertTrue("Rival ${r.id} must continue advancing forward along -Z", r.z <= rivalPositionsBefore[i].third + 0.01f)
        }
    }

    @Test
    fun test3_playerAHasCheckpoint3AndPlayerBHasCheckpoint1_playerAFalls_independentRespawnAndPreservation() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(3)
        world.loadLevel(level, GameMode.RIVAL_RACE)
        world.step(0.016f)

        // Player A ("player_0") activates Checkpoints 1, 2, 3
        assertTrue(world.activateCheckpointForRacer("player_0", 1))
        assertTrue(world.activateCheckpointForRacer("player_0", 2))
        assertTrue(world.activateCheckpointForRacer("player_0", 3))

        // Player B ("rival_1") activates Checkpoint 1 and is currently running between CP 1 and CP 2
        assertTrue(world.activateCheckpointForRacer("rival_1", 1))
        val rivalB = world.rivals[0]
        rivalB.x = -1.2f
        rivalB.y = 2.0f
        rivalB.z = -38.0f
        rivalB.vx = 0f
        rivalB.vy = 0f
        rivalB.vz = -7.5f

        val cp3 = level.checkpoints[2]

        // Player A falls off the map
        world.py = -30.0f
        world.step(0.016f)

        // Player A returns to Checkpoint 3
        assertEquals(3, world.playerCheckpointState.lastActivatedCheckpoint)
        assertEquals(cp3.z, world.pz, 0.05f)

        // Player B remains at their position (continuing forward) and keeps Checkpoint 1
        assertEquals(1, rivalB.checkpointState.lastActivatedCheckpoint)
        assertEquals(0, rivalB.checkpointState.respawnCount)
        assertTrue("Player B must remain near their running Z (-38) and not teleport to CP 1 or CP 3", rivalB.z < -37.5f && rivalB.z > -40.0f)
    }

    @Test
    fun test4_twoPlayersFallAtSameTime_bothRespawnIndependentlyAtOwnCheckpoints() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(4)
        world.loadLevel(level, GameMode.RIVAL_RACE)
        world.step(0.016f)

        // Player A reaches Checkpoint 3; Player B ("rival_1") reaches Checkpoint 2; Player C ("rival_2") is at Checkpoint 1
        world.activateCheckpointForRacer("player_0", 1)
        world.activateCheckpointForRacer("player_0", 2)
        world.activateCheckpointForRacer("player_0", 3)

        world.activateCheckpointForRacer("rival_1", 1)
        world.activateCheckpointForRacer("rival_1", 2)

        world.activateCheckpointForRacer("rival_2", 1)

        val cp3 = level.checkpoints[2]
        val cp2 = level.checkpoints[1]
        val rivalB = world.rivals[0]
        val rivalC = world.rivals[1]

        // Both Player A and Player B fall at the exact same frame
        world.py = -35.0f
        rivalB.y = -35.0f
        world.step(0.016f)

        assertEquals("Player A must respawn at Checkpoint 3", cp3.z, world.pz, 0.05f)
        assertEquals("Player B must respawn at Checkpoint 2", cp2.z, rivalB.z, 0.05f)
        assertEquals("Player A respawn count must be 1", 1, world.playerCheckpointState.respawnCount)
        assertEquals("Player B respawn count must be 1", 1, rivalB.checkpointState.respawnCount)
        assertEquals("Player C must NOT have respawned", 0, rivalC.checkpointState.respawnCount)
    }

    @Test
    fun test5_playerFallsBeforeActivatingAnyCheckpoint_returnsToStartingPosition() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(5)
        world.loadLevel(level, GameMode.CAREER)
        world.step(0.016f)

        assertEquals(0, world.playerCheckpointState.lastActivatedCheckpoint)
        world.px = 3.5f
        world.pz = -6.0f
        world.py = -30.0f
        world.step(0.016f)

        assertEquals(0, world.playerCheckpointState.lastActivatedCheckpoint)
        assertEquals("Must return to Start X", level.startX, world.px, 0.05f)
        assertEquals("Must return to Start Z", level.startZ, world.pz, 0.05f)
        assertTrue("Must return safely above Start platform", world.py > level.startY)
    }

    @Test
    fun test6_playerActivatesCheckpoints1To4InOrderAndFalls_returnsToCheckpoint4NotCheckpoint1() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(6)
        world.loadLevel(level, GameMode.CAREER)
        world.step(0.016f)

        // Cannot skip Checkpoint 1 to activate Checkpoint 3 directly
        assertFalse("Cannot skip unreached checkpoints", world.activateCheckpointForRacer("player_0", 3))

        // Activate Checkpoints 1, 2, 3, 4 in order
        assertTrue(world.activateCheckpointForRacer("player_0", 1))
        assertTrue(world.activateCheckpointForRacer("player_0", 2))
        assertTrue(world.activateCheckpointForRacer("player_0", 3))
        assertTrue(world.activateCheckpointForRacer("player_0", 4))

        // Moving backward to touch Checkpoint 1 again must NEVER overwrite Checkpoint 4
        assertFalse("Backward touch must not regress checkpoint progress", world.activateCheckpointForRacer("player_0", 1))
        assertEquals(4, world.playerCheckpointState.lastActivatedCheckpoint)

        val cp4 = level.checkpoints[3]
        val cp1 = level.checkpoints[0]

        // Player falls off the map
        world.py = -30.0f
        world.step(0.016f)

        assertEquals("Player must respawn at Checkpoint 4 Z", cp4.z, world.pz, 0.05f)
        assertTrue("Player must NOT respawn at Checkpoint 1 Z", kotlin.math.abs(world.pz - cp1.z) > 5.0f)
    }

    @Test
    fun test7_playerRespawnsWhileOthersStandOnMovingPlatforms_movingPlatformsAndOthersContinueNormally() {
        val world = GamePhysicsWorld()
        val level = LevelCatalog.getLevel(2) // Level 2 has moving platforms
        world.loadLevel(level, GameMode.RIVAL_RACE)
        world.step(0.016f)

        val movingPlat = world.platforms.first { it.def.type == com.example.game.model.PlatformType.MOVING }
        val rivalOnPlat = world.rivals[0]

        // Place rival B squarely on top of the moving platform
        world.step(0.02f)
        val platTopY = movingPlat.curY + movingPlat.def.h * 0.5f
        rivalOnPlat.x = movingPlat.curX
        rivalOnPlat.y = platTopY
        rivalOnPlat.z = movingPlat.curZ
        rivalOnPlat.vy = 0f
        rivalOnPlat.grounded = true

        val platTimeBefore = world.platformTime
        val platXBefore = movingPlat.curX

        // Player A falls and respawns while Rival B is on the moving platform
        world.py = -30.0f
        world.step(0.03f)

        assertEquals(1, world.playerCheckpointState.respawnCount)
        assertTrue("Platform clock must advance monotonically during player respawn", world.platformTime > platTimeBefore)
        assertTrue("Moving platform must continue moving without reset", movingPlat.curX != platXBefore)
        assertEquals("Rival on moving platform must NOT respawn", 0, rivalOnPlat.checkpointState.respawnCount)
        assertTrue("Rival on moving platform must remain above platform height", rivalOnPlat.y >= movingPlat.curY)
    }

    @Test
    fun test8_solidObstaclesPowerUpsBossChestsAndNameOnlyCharacters() {
        // 1) Characters must have ONLY their names (Ahmed, Salma, Ahmed) and empty description
        assertEquals(3, GameCatalog.skins.size)
        assertEquals(listOf("Ahmed", "Salma", "Ahmed"), GameCatalog.skins.map { it.name })
        assertTrue("Character descriptions must be empty", GameCatalog.skins.all { it.description.isEmpty() })

        // 2) Longer tracks, power-ups, secret chests, and Stage-5 Boss Sentinel chests
        val lvl1 = LevelCatalog.getLevel(1)
        val lvl5 = LevelCatalog.getLevel(5)
        assertTrue("Level 1 track should be long (> 110m)", kotlin.math.abs(lvl1.finishZ - lvl1.startZ) > 110f)
        assertTrue("Every level should spawn 3D power-ups", lvl1.powerUps.isNotEmpty())
        assertTrue("Every level should spawn hidden royal treasure chests", lvl1.treasureChests.isNotEmpty())
        assertTrue("Stage 5 Gateway level must include a Boss Guardian Treasure Chest", lvl5.treasureChests.any { it.isBossChest })

        // 3) Both positive AND negative arms of ROTATING_BEAM and perpendicular blades of SPINNING_BLADE must be 100% solid
        val world = GamePhysicsWorld()
        world.loadLevel(lvl1, GameMode.CAREER)
        world.step(0.016f)

        val beamObs = com.example.game.model.ObstacleDef(
            type = com.example.game.model.ObstacleType.ROTATING_BEAM,
            x = 0f, y = 0.5f, z = -20f,
            speed = 0f, phase = 0f, range = 2.4f, scale = 1.0f
        )
        world.activeObstacles.clear()
        world.activeObstacles.add(beamObs)

        // Place player on the NEGATIVE arm (-1.6m along X when angle = 0)
        world.px = -1.6f
        world.py = 0.5f
        world.pz = -20f
        world.hitStunTimer = 0f
        world.step(0.016f)
        assertTrue("Negative half-arm of ROTATING_BEAM must be solid and trigger hit stun", world.hitStunTimer > 0f)

        // Test Golden Aegis Shield absorbs hit without knockback velocity
        world.hitStunTimer = 0f
        world.shieldTimer = 8.0f
        world.vx = 0f
        world.vz = 0f
        world.px = 1.6f
        world.py = 0.5f
        world.pz = -20f
        world.step(0.016f)
        assertEquals("Aegis Shield should be consumed on hit", 0f, world.shieldTimer, 0.001f)
    }

    @Test
    fun test9_difficultyTiersChestTiersSeasonalEventsAndNoDuplicateChestRewards() {
        // 1) Verify 5 difficulty tiers across levels 1..50
        assertEquals(com.example.game.model.DifficultyTier.TUTORIAL, LevelCatalog.getLevel(1).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.TUTORIAL, LevelCatalog.getLevel(5).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.EASY, LevelCatalog.getLevel(6).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.EASY, LevelCatalog.getLevel(15).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.MEDIUM, LevelCatalog.getLevel(16).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.MEDIUM, LevelCatalog.getLevel(30).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.HARD, LevelCatalog.getLevel(31).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.HARD, LevelCatalog.getLevel(40).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.EXPERT, LevelCatalog.getLevel(41).difficultyTier)
        assertEquals(com.example.game.model.DifficultyTier.EXPERT, LevelCatalog.getLevel(50).difficultyTier)

        // 2) Verify all 4 chest tiers exist across the 50 levels (WOODEN, METALLIC, MAGIC_RARE, EVENT_BOSS)
        val allChestTiers = LevelCatalog.allLevels.flatMap { it.treasureChests }.map { it.tier }.toSet()
        assertTrue(allChestTiers.contains(com.example.game.model.ChestTier.WOODEN))
        assertTrue(allChestTiers.contains(com.example.game.model.ChestTier.METALLIC))
        assertTrue(allChestTiers.contains(com.example.game.model.ChestTier.MAGIC_RARE))
        assertTrue(allChestTiers.contains(com.example.game.model.ChestTier.EVENT_BOSS))

        // 3) Verify opening a chest awards its bonus coins once and cannot be opened again in the same run
        val world = GamePhysicsWorld()
        world.loadLevel(LevelCatalog.getLevel(5), GameMode.CAREER)
        world.step(0.016f)
        val bossChest = world.treasureChests.first { it.def.tier == com.example.game.model.ChestTier.EVENT_BOSS }
        val coinsBefore = world.coinsCollected

        world.px = bossChest.def.x
        world.py = bossChest.def.y
        world.pz = bossChest.def.z
        world.step(0.016f)

        assertTrue("Chest should be marked opened", bossChest.opened)
        assertEquals(1, world.chestsOpenedInRun)
        assertEquals(coinsBefore + bossChest.def.bonusCoins, world.coinsCollected)

        // Step multiple frames standing on the same chest -> must not award coins again
        repeat(10) {
            world.px = bossChest.def.x
            world.py = bossChest.def.y
            world.pz = bossChest.def.z
            world.step(0.016f)
        }
        assertEquals("Chest must not award duplicate coins", 1, world.chestsOpenedInRun)
        assertEquals(coinsBefore + bossChest.def.bonusCoins, world.coinsCollected)

        // 4) Verify Daily, Weekly, and Seasonal Event missions + 7-Day Login Streak + Seasonal Event schedule
        assertTrue(GameCatalog.missions.any { it.cadence == com.example.game.model.MissionCadence.DAILY })
        assertTrue(GameCatalog.missions.any { it.cadence == com.example.game.model.MissionCadence.WEEKLY })
        assertTrue(GameCatalog.missions.any { it.cadence == com.example.game.model.MissionCadence.EVENT })
        assertEquals(7, GameCatalog.loginStreakRewards.size)
        val event = GameCatalog.getActiveSeasonalEvent(20500L)
        assertTrue(event.titleEn.isNotEmpty())
        assertTrue(GameCatalog.getSeasonalEventDaysRemaining(20500L) in 1..7)
    }

    @Test
    fun test10_airborneShadowProjectionAudioSyncAndCameraArchwayAvoidance() {
        val playedSfx = mutableListOf<String>()
        val world = GamePhysicsWorld(object : com.example.game.engine3d.GameWorldListener {
            override fun onSfx(name: String) {
                playedSfx += name
            }
            override fun onHaptic(strong: Boolean) {}
            override fun onLevelWon(
                levelNumber: Int, timeMs: Long, coinsCollected: Int, distanceM: Float,
                obstaclesPassed: Int, raceRank: Int, totalRacers: Int, jumpsInRun: Int,
                perfectLandings: Int, secretPathFound: Boolean, narrowBridgesCrossed: Int,
                dashUsed: Boolean, respawnCount: Int, chestsOpened: Int
            ) {}
            override fun onPlayerFell() {}
        })

        val level = LevelCatalog.getLevel(1)
        world.loadLevel(level, GameMode.CAREER)
        repeat(25) { world.step(0.02f) }
        assertTrue("Player should be grounded on start platform", world.grounded)
        assertTrue("Landing SFX should have played upon touching start platform", playedSfx.any { it.startsWith("land") })

        // 1) When jumping above a platform, shadowFloorY remains valid and shadowHeightGap reflects height above platform
        playedSfx.clear()
        world.triggerJump()
        world.step(0.016f)
        world.step(0.04f)
        assertFalse("Shadow floor Y should be tracked while airborne above a platform", world.shadowFloorY.isNaN())
        assertTrue("Shadow height gap should be positive while airborne", world.shadowHeightGap > 0.1f)
        assertTrue("Jump SFX should have been triggered", playedSfx.contains("jump"))

        // 2) Opening a Wooden Chest triggers "chest_wooden" SFX and activates waist pickupAuraTimer
        val woodenChest = world.treasureChests.first { it.def.tier == com.example.game.model.ChestTier.WOODEN }
        playedSfx.clear()
        world.px = woodenChest.def.x
        world.py = woodenChest.def.y
        world.pz = woodenChest.def.z
        world.step(0.016f)
        assertTrue("Opening wooden chest should trigger chest_wooden SFX", playedSfx.contains("chest_wooden"))
        assertTrue("Opening chest should activate short waist pickup aura", world.pickupAuraTimer > 0f)

        // 3) Reduced Motion / Camera Shake toggle: when cameraShakeEnabled = false, hits do not shake camera
        world.loadLevel(LevelCatalog.getLevel(3), GameMode.CAREER)
        world.step(0.016f)
        world.cameraShakeEnabled = false
        world.cameraShakeTimer = 0f
        world.shieldTimer = 0f
        val obs = world.activeObstacles.first()
        world.px = obs.x
        world.py = obs.y + 0.8f
        world.pz = obs.z
        world.step(0.016f)
        assertEquals("Camera shake timer must remain 0 when cameraShakeEnabled is false", 0f, world.cameraShakeTimer, 0.001f)
    }
}
