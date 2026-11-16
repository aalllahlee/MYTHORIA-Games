package com.example.data.db

import com.example.game.model.GameCatalog
import com.example.game.model.LevelCatalog
import com.example.game.model.ShopCategory
import com.example.game.model.WorldProgressionGraph
import com.example.game.model.XpProgression
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LevelWinOutcome(
    val stars: Int,
    val coinsEarned: Int,
    val xpEarned: Int,
    val goalMetThisRun: Boolean,
    val goalMetStored: Boolean,
    val goalTimeMs: Long,
    val newlyUnlockedWorldId: Int? = null,
    val newlyUnlockedLevelNumber: Int? = null
)

class GameRepository(private val dao: GameDao) {

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map { it ?: PlayerProfileEntity() }
    val levelsFlow: Flow<List<LevelProgressEntity>> = dao.observeAllLevels()
    val worldsFlow: Flow<List<WorldProgressEntity>> = dao.observeAllWorlds()
    val ownedItemIdsFlow: Flow<Set<String>> = dao.observeOwnership().map { list -> list.map { it.itemId }.toSet() }
    val achievementsFlow: Flow<List<AchievementEntity>> = dao.observeAchievements()
    val missionsFlow: Flow<List<MissionEntity>> = dao.observeMissions()

    suspend fun ensureInitialized() {
        if (dao.getProfileOnce() == null) {
            dao.upsertProfile(PlayerProfileEntity())
        }
        val initialLevels = (1..50).map { num ->
            LevelProgressEntity(
                levelNumber = num,
                unlocked = num == 1,
                goalTimeMs = WorldProgressionGraph.goalTimeMsForLevel(num)
            )
        }
        dao.insertLevelsIgnore(initialLevels)

        val now = System.currentTimeMillis()
        val initialWorlds = WorldProgressionGraph.worldNodes.map { node ->
            WorldProgressEntity(
                worldId = node.worldId,
                unlocked = node.worldId == 1,
                gatewayLevelNumber = node.gatewayLevelNumber,
                gatewayGoalMet = false,
                unlockedAtMs = if (node.worldId == 1) now else 0L
            )
        }
        dao.insertWorldsIgnore(initialWorlds)

        reconcileProgressionGraph(backfillMigratedRuns = true)

        val starterItems = listOf(
            ShopOwnershipEntity("red_hero", ShopCategory.CHARACTERS.name),
            ShopOwnershipEntity("hat_none", ShopCategory.HATS.name),
            ShopOwnershipEntity("shoe_default", ShopCategory.SHOES.name),
            ShopOwnershipEntity("trail_dust", ShopCategory.TRAILS.name),
            ShopOwnershipEntity("victory_dance", ShopCategory.VICTORY.name)
        )
        dao.insertOwnershipIgnore(starterItems)

        val initialAchievements = GameCatalog.achievements.map {
            AchievementEntity(achievementId = it.id, unlocked = false, progress = 0)
        }
        dao.insertAchievementsIgnore(initialAchievements)

        val initialMissions = GameCatalog.missions.map {
            MissionEntity(missionId = it.id, progress = 0, completedCount = 0)
        }
        dao.insertMissionsIgnore(initialMissions)
    }

    suspend fun updateProfile(transform: (PlayerProfileEntity) -> PlayerProfileEntity) {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(transform(current))
    }

    suspend fun purchaseSkin(skinId: String): Boolean {
        val skin = GameCatalog.skins.find { it.id == skinId } ?: return false
        if (dao.getOwnershipOnce(skinId) != null) return true
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        if (profile.coins < skin.costCoins) return false

        dao.upsertOwnership(ShopOwnershipEntity(skinId, ShopCategory.CHARACTERS.name))
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins - skin.costCoins,
                selectedSkinId = skinId
            )
        )
        checkSkinAchievement()
        return true
    }

    suspend fun selectSkin(skinId: String) {
        if (dao.getOwnershipOnce(skinId) != null) {
            updateProfile { it.copy(selectedSkinId = skinId) }
        }
    }

    suspend fun purchaseShopItem(itemId: String): Boolean {
        val item = GameCatalog.shopItems.find { it.id == itemId } ?: return false
        if (dao.getOwnershipOnce(itemId) != null) {
            equipShopItem(item.id, item.category)
            return true
        }
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        if (profile.coins < item.costCoins) return false

        dao.upsertOwnership(ShopOwnershipEntity(item.id, item.category.name))
        val updated = when (item.category) {
            ShopCategory.HATS -> profile.copy(coins = profile.coins - item.costCoins, selectedHatId = item.id)
            ShopCategory.SHOES -> profile.copy(coins = profile.coins - item.costCoins, selectedShoeId = item.id)
            ShopCategory.TRAILS -> profile.copy(coins = profile.coins - item.costCoins, selectedTrailId = item.id)
            ShopCategory.VICTORY -> profile.copy(coins = profile.coins - item.costCoins, selectedVictoryId = item.id)
            ShopCategory.ABILITIES -> profile.copy(coins = profile.coins - item.costCoins, dashUnlocked = true)
            ShopCategory.CHARACTERS -> profile.copy(coins = profile.coins - item.costCoins, selectedSkinId = item.id)
        }
        dao.upsertProfile(updated)
        if (item.id == "ability_dash") {
            updateAchievement("ach_dash", 1)
        }
        return true
    }

    suspend fun equipShopItem(itemId: String, category: ShopCategory) {
        if (dao.getOwnershipOnce(itemId) == null) return
        updateProfile { profile ->
            when (category) {
                ShopCategory.HATS -> profile.copy(selectedHatId = itemId)
                ShopCategory.SHOES -> profile.copy(selectedShoeId = itemId)
                ShopCategory.TRAILS -> profile.copy(selectedTrailId = itemId)
                ShopCategory.VICTORY -> profile.copy(selectedVictoryId = itemId)
                ShopCategory.ABILITIES -> profile.copy(dashUnlocked = true)
                ShopCategory.CHARACTERS -> profile.copy(selectedSkinId = itemId)
            }
        }
    }

    suspend fun recordCareerLevelWin(
        levelNumber: Int,
        timeMs: Long,
        parTimeSec: Float,
        coinsCollected: Int,
        rewardCoins: Int,
        jumpsInRun: Int = 0
    ): LevelWinOutcome {
        val goalTimeMs = WorldProgressionGraph.goalTimeMsForLevel(levelNumber)
        val existing = dao.getLevelOnce(levelNumber)
            ?: LevelProgressEntity(levelNumber, unlocked = true, goalTimeMs = goalTimeMs)
        val elapsedSec = timeMs / 1000f
        val earnedStars = when {
            elapsedSec <= parTimeSec * 0.85f -> 3
            elapsedSec <= parTimeSec * 1.25f -> 2
            else -> 1
        }
        val goalMetThisRun = WorldProgressionGraph.isGoalMet(levelNumber, timeMs, completed = true)
        val goalMetStored = existing.goalMet || goalMetThisRun

        val firstTimeBonus = if (!existing.completed) rewardCoins else rewardCoins / 3
        val totalEarned = coinsCollected + firstTimeBonus
        val xpEarned = 350 + earnedStars * 75 + coinsCollected * 2

        val bestTime = if (existing.bestTimeMs == 0L || timeMs < existing.bestTimeMs) timeMs else existing.bestTimeMs
        dao.upsertLevel(
            existing.copy(
                unlocked = true,
                completed = true,
                stars = maxOf(existing.stars, earnedStars),
                bestTimeMs = bestTime,
                bestCoins = maxOf(existing.bestCoins, coinsCollected),
                goalMet = goalMetStored,
                goalTimeMs = goalTimeMs
            )
        )

        val (newlyUnlockedWorldId, newlyUnlockedLevel) = reconcileProgressionGraph(backfillMigratedRuns = false)

        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val newTotalCoinsEarned = profile.totalCoinsEarned + totalEarned
        val newTotalWins = profile.totalWins + 1
        val newXp = profile.playerXp + xpEarned
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + totalEarned,
                totalWins = newTotalWins,
                totalCoinsEarned = newTotalCoinsEarned,
                playerXp = newXp,
                totalJumps = profile.totalJumps + jumpsInRun
            )
        )

        checkXpMilestones(profile.playerXp, newXp)
        incrementMission("mis_stages", 1)
        incrementMission("mis_coins", totalEarned)
        if (jumpsInRun > 0) incrementMission("mis_jumps", jumpsInRun)

        updateAchievement("ach_first_win", newTotalWins)
        updateAchievement("ach_10_wins", newTotalWins)
        updateAchievement("ach_coins_500", newTotalCoinsEarned)
        if (levelNumber <= 5) {
            val world1Done = (1..5).count { dao.getLevelOnce(it)?.completed == true }
            updateAchievement("ach_world_1", world1Done)
        }
        return LevelWinOutcome(
            stars = earnedStars,
            coinsEarned = totalEarned,
            xpEarned = xpEarned,
            goalMetThisRun = goalMetThisRun,
            goalMetStored = goalMetStored,
            goalTimeMs = goalTimeMs,
            newlyUnlockedWorldId = newlyUnlockedWorldId,
            newlyUnlockedLevelNumber = newlyUnlockedLevel
        )
    }

    suspend fun recordRivalRaceResult(
        levelNumber: Int,
        placement: Int,
        timeMs: Long,
        parTimeSec: Float,
        coinsCollected: Int,
        jumpsInRun: Int = 0
    ): LevelWinOutcome {
        val goalTimeMs = WorldProgressionGraph.goalTimeMsForLevel(levelNumber)
        val rankBonus = when (placement) {
            1 -> 260
            2 -> 150
            3 -> 90
            else -> 40
        }
        val xpBonus = when (placement) {
            1 -> 520
            2 -> 360
            3 -> 250
            else -> 160
        }
        val totalEarned = coinsCollected + rankBonus
        val stars = when (placement) {
            1 -> 3
            2 -> 2
            else -> 1
        }

        val existing = dao.getLevelOnce(levelNumber)
            ?: LevelProgressEntity(levelNumber, unlocked = true, goalTimeMs = goalTimeMs)
        val goalMetThisRun = placement == 1 && WorldProgressionGraph.isGoalMet(levelNumber, timeMs, completed = true)
        val goalMetStored = existing.goalMet || goalMetThisRun

        if (placement == 1) {
            dao.upsertLevel(
                existing.copy(
                    unlocked = true,
                    completed = true,
                    stars = maxOf(existing.stars, stars),
                    bestTimeMs = if (existing.bestTimeMs == 0L || timeMs < existing.bestTimeMs) timeMs else existing.bestTimeMs,
                    goalMet = goalMetStored,
                    goalTimeMs = goalTimeMs
                )
            )
        }

        val (newlyUnlockedWorldId, newlyUnlockedLevel) = reconcileProgressionGraph(backfillMigratedRuns = false)

        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val newXp = profile.playerXp + xpBonus
        val newWins = profile.totalWins + if (placement == 1) 1 else 0
        val newRivalWins = profile.rivalRaceWins + if (placement == 1) 1 else 0
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + totalEarned,
                totalCoinsEarned = profile.totalCoinsEarned + totalEarned,
                totalWins = newWins,
                rivalRaceWins = newRivalWins,
                playerXp = newXp,
                totalJumps = profile.totalJumps + jumpsInRun
            )
        )

        checkXpMilestones(profile.playerXp, newXp)
        incrementMission("mis_coins", totalEarned)
        if (jumpsInRun > 0) incrementMission("mis_jumps", jumpsInRun)
        if (placement == 1) {
            incrementMission("mis_stages", 1)
            incrementMission("mis_rival", 1)
            updateAchievement("ach_10_wins", newWins)
        }
        updateAchievement("ach_coins_500", profile.totalCoinsEarned + totalEarned)
        return LevelWinOutcome(
            stars = stars,
            coinsEarned = totalEarned,
            xpEarned = xpBonus,
            goalMetThisRun = goalMetThisRun,
            goalMetStored = goalMetStored,
            goalTimeMs = goalTimeMs,
            newlyUnlockedWorldId = newlyUnlockedWorldId,
            newlyUnlockedLevelNumber = newlyUnlockedLevel
        )
    }

    suspend fun recordDailyChallengeWin(epochDay: Long, coinsEarned: Int, jumpsInRun: Int = 0) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val bonus = if (profile.dailyLastCompletedEpochDay != epochDay) 180 else 60
        val total = coinsEarned + bonus
        val xpBonus = 450
        val newXp = profile.playerXp + xpBonus
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + total,
                dailyLastCompletedEpochDay = epochDay,
                totalWins = profile.totalWins + 1,
                totalCoinsEarned = profile.totalCoinsEarned + total,
                playerXp = newXp,
                totalJumps = profile.totalJumps + jumpsInRun
            )
        )
        checkXpMilestones(profile.playerXp, newXp)
        incrementMission("mis_stages", 1)
        incrementMission("mis_coins", total)
        if (jumpsInRun > 0) incrementMission("mis_jumps", jumpsInRun)
        updateAchievement("ach_daily", 1)
        updateAchievement("ach_10_wins", profile.totalWins + 1)
        updateAchievement("ach_coins_500", profile.totalCoinsEarned + total)
    }

    suspend fun recordEndlessRun(
        distanceM: Float,
        timeMs: Long,
        obstaclesPassed: Int,
        coinsCollected: Int,
        jumpsInRun: Int = 0
    ) {
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val earned = coinsCollected + (distanceM / 10f).toInt()
        val xpBonus = (distanceM * 4f).toInt().coerceAtLeast(30)
        val newXp = profile.playerXp + xpBonus
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + earned,
                totalCoinsEarned = profile.totalCoinsEarned + earned,
                endlessBestDistance = maxOf(profile.endlessBestDistance, distanceM),
                endlessBestTimeMs = maxOf(profile.endlessBestTimeMs, timeMs),
                endlessBestObstacles = maxOf(profile.endlessBestObstacles, obstaclesPassed),
                playerXp = newXp,
                totalJumps = profile.totalJumps + jumpsInRun
            )
        )
        checkXpMilestones(profile.playerXp, newXp)
        incrementMission("mis_coins", earned)
        if (jumpsInRun > 0) incrementMission("mis_jumps", jumpsInRun)
        updateAchievement("ach_endless_100", distanceM.toInt())
        updateAchievement("ach_coins_500", profile.totalCoinsEarned + earned)
    }

    suspend fun recordRunJumpsAndCoins(coinsCollected: Int, jumpsInRun: Int) {
        if (coinsCollected <= 0 && jumpsInRun <= 0) return
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val xpGain = coinsCollected * 2 + jumpsInRun * 3
        val newXp = profile.playerXp + xpGain
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + coinsCollected,
                totalCoinsEarned = profile.totalCoinsEarned + coinsCollected,
                totalJumps = profile.totalJumps + jumpsInRun,
                playerXp = newXp
            )
        )
        checkXpMilestones(profile.playerXp, newXp)
        if (coinsCollected > 0) {
            incrementMission("mis_coins", coinsCollected)
            updateAchievement("ach_coins_500", profile.totalCoinsEarned + coinsCollected)
        }
        if (jumpsInRun > 0) incrementMission("mis_jumps", jumpsInRun)
    }

    suspend fun claimMission(missionId: String): Boolean {
        val def = GameCatalog.missions.find { it.id == missionId } ?: return false
        val entity = dao.getMissionOnce(missionId) ?: MissionEntity(missionId)
        if (entity.progress < def.target) return false

        val remainder = (entity.progress - def.target).coerceAtLeast(0)
        dao.upsertMission(
            entity.copy(
                progress = remainder,
                completedCount = entity.completedCount + 1
            )
        )
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val newXp = profile.playerXp + def.rewardXp
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + def.rewardCoins,
                totalCoinsEarned = profile.totalCoinsEarned + def.rewardCoins,
                playerXp = newXp
            )
        )
        checkXpMilestones(profile.playerXp, newXp)
        updateAchievement("ach_coins_500", profile.totalCoinsEarned + def.rewardCoins)
        return true
    }

    suspend fun awardCoins(coins: Int) {
        if (coins <= 0) return
        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + coins,
                totalCoinsEarned = profile.totalCoinsEarned + coins
            )
        )
        updateAchievement("ach_coins_500", profile.totalCoinsEarned + coins)
    }

    private suspend fun incrementMission(missionId: String, delta: Int) {
        if (delta <= 0) return
        val cur = dao.getMissionOnce(missionId) ?: MissionEntity(missionId)
        dao.upsertMission(cur.copy(progress = cur.progress + delta))
    }

    /**
     * Reconciles all 50 LevelProgressEntity and 10 WorldProgressEntity rows against the
     * WorldProgressionGraph. Next worlds and next levels are strictly unlocked only when
     * prerequisite level goals are satisfied.
     *
     * @return Pair(newlyUnlockedWorldId, newlyUnlockedLevelNumber)
     */
    suspend fun reconcileProgressionGraph(backfillMigratedRuns: Boolean = false): Pair<Int?, Int?> {
        val currentLevels = dao.getAllLevelsOnce().associateBy { it.levelNumber }.toMutableMap()
        val currentWorlds = dao.getAllWorldsOnce().associateBy { it.worldId }.toMutableMap()

        // Ensure goalTimeMs is populated and optionally backfill v2->v3 migrated rows
        for (levelNum in 1..50) {
            val targetGoalMs = WorldProgressionGraph.goalTimeMsForLevel(levelNum)
            val row = currentLevels[levelNum] ?: LevelProgressEntity(levelNum, unlocked = levelNum == 1, goalTimeMs = targetGoalMs)
            val upgradedGoalMet = if (backfillMigratedRuns && row.goalTimeMs == 0L && row.completed) {
                WorldProgressionGraph.isGoalMet(levelNum, row.bestTimeMs, completed = true)
            } else {
                row.goalMet
            }
            if (row.goalTimeMs != targetGoalMs || row.goalMet != upgradedGoalMet) {
                val updated = row.copy(goalTimeMs = targetGoalMs, goalMet = upgradedGoalMet)
                dao.upsertLevel(updated)
                currentLevels[levelNum] = updated
            }
        }

        val goalMetByLevel = (1..50).associateWith { lvl -> currentLevels[lvl]?.goalMet == true }
        val unlockedWorlds = WorldProgressionGraph.computeUnlockedWorlds(goalMetByLevel)
        val unlockedLevels = WorldProgressionGraph.computeUnlockedLevels(goalMetByLevel)

        var newlyUnlockedWorldId: Int? = null
        var newlyUnlockedLevel: Int? = null
        val now = System.currentTimeMillis()

        for (node in WorldProgressionGraph.worldNodes) {
            val shouldBeUnlocked = node.worldId in unlockedWorlds
            val gatewayMet = goalMetByLevel[node.gatewayLevelNumber] == true
            val existingWorld = currentWorlds[node.worldId]
            if (existingWorld == null ||
                existingWorld.unlocked != shouldBeUnlocked ||
                existingWorld.gatewayGoalMet != gatewayMet
            ) {
                if (existingWorld != null && !existingWorld.unlocked && shouldBeUnlocked) {
                    newlyUnlockedWorldId = node.worldId
                }
                dao.upsertWorld(
                    WorldProgressEntity(
                        worldId = node.worldId,
                        unlocked = shouldBeUnlocked,
                        gatewayLevelNumber = node.gatewayLevelNumber,
                        gatewayGoalMet = gatewayMet,
                        unlockedAtMs = when {
                            shouldBeUnlocked && (existingWorld?.unlockedAtMs ?: 0L) > 0L -> existingWorld!!.unlockedAtMs
                            shouldBeUnlocked -> now
                            else -> 0L
                        }
                    )
                )
            }
        }

        for (levelNum in 1..50) {
            val shouldBeUnlocked = levelNum in unlockedLevels
            val existingLvl = currentLevels[levelNum]
            if (existingLvl != null && existingLvl.unlocked != shouldBeUnlocked) {
                if (!existingLvl.unlocked && shouldBeUnlocked) {
                    newlyUnlockedLevel = levelNum
                }
                dao.upsertLevel(existingLvl.copy(unlocked = shouldBeUnlocked))
            }
        }

        return newlyUnlockedWorldId to newlyUnlockedLevel
    }

    suspend fun isWorldUnlocked(worldId: Int): Boolean {
        val worldRow = dao.getWorldOnce(worldId)
        return worldRow?.unlocked ?: (worldId == 1)
    }

    suspend fun isLevelUnlocked(levelNumber: Int): Boolean {
        val worldId = LevelCatalog.getWorldForLevel(levelNumber).id
        if (!isWorldUnlocked(worldId)) return false
        return dao.getLevelOnce(levelNumber)?.unlocked ?: (levelNumber == 1)
    }

    private suspend fun checkXpMilestones(oldXp: Int, newXp: Int) {
        val oldLevel = XpProgression.levelFromXp(oldXp)
        val newLevel = XpProgression.levelFromXp(newXp)
        if (newLevel <= oldLevel) return

        var bonusCoins = 0
        for (m in GameCatalog.xpMilestones) {
            if (m.levelReq in (oldLevel + 1)..newLevel) {
                bonusCoins += m.rewardCoins
                m.unlockItemId?.let { itemId ->
                    val shopItem = GameCatalog.shopItems.find { it.id == itemId }
                    if (shopItem != null && dao.getOwnershipOnce(itemId) == null) {
                        dao.upsertOwnership(ShopOwnershipEntity(itemId, shopItem.category.name))
                    }
                }
            }
        }
        if (bonusCoins > 0) {
            val p = dao.getProfileOnce() ?: PlayerProfileEntity()
            dao.upsertProfile(
                p.copy(
                    coins = p.coins + bonusCoins,
                    totalCoinsEarned = p.totalCoinsEarned + bonusCoins
                )
            )
        }
    }

    private suspend fun checkSkinAchievement() {
        val ownedSkins = GameCatalog.skins.count { dao.getOwnershipOnce(it.id) != null }
        updateAchievement("ach_skins_3", ownedSkins)
    }

    private suspend fun updateAchievement(id: String, newProgress: Int) {
        val def = GameCatalog.achievements.find { it.id == id } ?: return
        val cur = dao.getAchievementOnce(id) ?: AchievementEntity(id)
        val clamped = newProgress.coerceAtLeast(cur.progress)
        val nowUnlocked = cur.unlocked || clamped >= def.target
        var claimBonus = 0
        if (nowUnlocked && !cur.rewardClaimed) {
            claimBonus = def.rewardCoins
        }
        dao.upsertAchievement(
            cur.copy(
                progress = clamped.coerceAtMost(def.target),
                unlocked = nowUnlocked,
                rewardClaimed = cur.rewardClaimed || nowUnlocked
            )
        )
        if (claimBonus > 0) {
            val p = dao.getProfileOnce() ?: PlayerProfileEntity()
            dao.upsertProfile(p.copy(coins = p.coins + claimBonus))
        }
    }
}
