package com.example.game.model

import kotlin.math.ceil

sealed interface LevelGoalCondition {
    data class FinishWithinTime(val targetTimeSec: Float) : LevelGoalCondition {
        val targetTimeMs: Long
            get() = (targetTimeSec * 1000f).toLong()
    }
}

data class WorldGraphNode(
    val worldId: Int,
    val worldName: String,
    val firstLevelNumber: Int,
    val gatewayLevelNumber: Int,
    val levelRange: IntRange,
    val prerequisiteWorldId: Int?,
    val prerequisiteGatewayLevel: Int?,
    val unlockGoal: LevelGoalCondition.FinishWithinTime?,
    val nextWorldId: Int?
)

data class LevelGraphNode(
    val levelNumber: Int,
    val worldId: Int,
    val prerequisiteLevelNumber: Int?,
    val goal: LevelGoalCondition.FinishWithinTime,
    val isWorldGateway: Boolean,
    val unlocksNextWorldId: Int?
)

data class ProgressionEdge(
    val fromWorldId: Int,
    val toWorldId: Int,
    val viaGatewayLevel: Int,
    val goal: LevelGoalCondition.FinishWithinTime
)

/**
 * Directed Acyclic Progression Graph controlling Stage (1..50) and World (1..10) unlocking.
 * Access to World (w + 1) requires World (w)'s gateway level to satisfy its FinishWithinTime goal.
 */
object WorldProgressionGraph {

    val levelNodes: List<LevelGraphNode> by lazy {
        (1..50).map { levelNum ->
            val worldId = ((levelNum - 1) / 5) + 1
            val stageInWorld = ((levelNum - 1) % 5) + 1
            val isGateway = stageInWorld == 5
            val goalSec = computeGoalTimeSec(levelNum)
            LevelGraphNode(
                levelNumber = levelNum,
                worldId = worldId,
                prerequisiteLevelNumber = if (levelNum == 1) null else levelNum - 1,
                goal = LevelGoalCondition.FinishWithinTime(goalSec),
                isWorldGateway = isGateway,
                unlocksNextWorldId = if (isGateway && worldId < 10) worldId + 1 else null
            )
        }
    }

    val worldNodes: List<WorldGraphNode> by lazy {
        GameCatalog.worlds.map { world ->
            val firstLvl = (world.id - 1) * 5 + 1
            val gatewayLvl = world.id * 5
            val prevWorld = if (world.id > 1) world.id - 1 else null
            val prevGateway = prevWorld?.let { it * 5 }
            val unlockGoal = prevGateway?.let { LevelGoalCondition.FinishWithinTime(computeGoalTimeSec(it)) }
            WorldGraphNode(
                worldId = world.id,
                worldName = world.name,
                firstLevelNumber = firstLvl,
                gatewayLevelNumber = gatewayLvl,
                levelRange = firstLvl..gatewayLvl,
                prerequisiteWorldId = prevWorld,
                prerequisiteGatewayLevel = prevGateway,
                unlockGoal = unlockGoal,
                nextWorldId = if (world.id < GameCatalog.worlds.size) world.id + 1 else null
            )
        }
    }

    val worldEdges: List<ProgressionEdge> by lazy {
        worldNodes.drop(1).mapNotNull { target ->
            val fromId = target.prerequisiteWorldId ?: return@mapNotNull null
            val viaLvl = target.prerequisiteGatewayLevel ?: return@mapNotNull null
            val goal = target.unlockGoal ?: return@mapNotNull null
            ProgressionEdge(
                fromWorldId = fromId,
                toWorldId = target.worldId,
                viaGatewayLevel = viaLvl,
                goal = goal
            )
        }
    }

    private fun computeGoalTimeSec(levelNumber: Int): Float {
        val lvl = LevelCatalog.getLevel(levelNumber)
        val stageInWorld = ((levelNumber - 1) % 5) + 1
        // World Gateway (Stage 5) uses strict par time; stages 1..4 allow a tight 8% margin
        val raw = if (stageInWorld == 5) lvl.parTimeSec else lvl.parTimeSec * 1.08f
        return ceil(raw.toDouble()).toFloat()
    }

    fun getWorldNode(worldId: Int): WorldGraphNode =
        worldNodes[(worldId - 1).coerceIn(0, worldNodes.lastIndex)]

    fun getLevelNode(levelNumber: Int): LevelGraphNode =
        levelNodes[(levelNumber - 1).coerceIn(0, levelNodes.lastIndex)]

    fun getUnlockEdgeForWorld(worldId: Int): ProgressionEdge? =
        worldEdges.find { it.toWorldId == worldId }

    fun goalTimeSecForLevel(levelNumber: Int): Float =
        getLevelNode(levelNumber).goal.targetTimeSec

    fun goalTimeMsForLevel(levelNumber: Int): Long =
        getLevelNode(levelNumber).goal.targetTimeMs

    /**
     * Returns true iff the player completed the level and met the level's FinishWithinTime goal.
     */
    fun isGoalMet(levelNumber: Int, completionTimeMs: Long, completed: Boolean): Boolean {
        if (!completed || completionTimeMs <= 0L) return false
        return completionTimeMs <= goalTimeMsForLevel(levelNumber)
    }

    /**
     * Traverses the progression graph from World 1 using stored level goal statuses.
     * World (w + 1) is only reachable if World (w) is reachable AND World (w)'s gateway level met its goal.
     */
    fun computeUnlockedWorlds(goalMetByLevel: Map<Int, Boolean>): Set<Int> {
        val unlocked = linkedSetOf(1)
        for (edge in worldEdges) {
            if (edge.fromWorldId in unlocked && goalMetByLevel[edge.viaGatewayLevel] == true) {
                unlocked += edge.toWorldId
            }
        }
        return unlocked
    }

    /**
     * Traverses level dependency edges starting from Level 1.
     * Level (n + 1) is only reachable if Level (n) is reachable AND met its goal,
     * AND the target level's world is unlocked in the progression graph.
     */
    fun computeUnlockedLevels(goalMetByLevel: Map<Int, Boolean>): Set<Int> {
        val unlockedWorlds = computeUnlockedWorlds(goalMetByLevel)
        val unlockedLevels = linkedSetOf(1)
        for (node in levelNodes) {
            if (node.levelNumber == 1) continue
            val prereq = node.prerequisiteLevelNumber ?: continue
            if (node.worldId in unlockedWorlds &&
                prereq in unlockedLevels &&
                goalMetByLevel[prereq] == true
            ) {
                unlockedLevels += node.levelNumber
            }
        }
        return unlockedLevels
    }
}
