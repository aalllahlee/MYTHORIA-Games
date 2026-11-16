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
        assertEquals(15, GameCatalog.skins.size)
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
}
