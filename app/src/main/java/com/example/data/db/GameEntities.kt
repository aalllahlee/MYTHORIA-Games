package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 120,
    val selectedSkinId: String = "red_hero",
    val selectedHatId: String = "hat_none",
    val selectedShoeId: String = "shoe_default",
    val selectedTrailId: String = "trail_dust",
    val selectedVictoryId: String = "victory_dance",
    val dashUnlocked: Boolean = false,
    val musicVolume: Float = 0.7f,
    val sfxVolume: Float = 0.85f,
    val uiVolume: Float = 0.85f,
    val audioMuted: Boolean = false,
    val cameraShakeEnabled: Boolean = true,
    val graphicsQuality: String = "HIGH",
    val cameraSensitivity: Float = 1.0f,
    val joystickSensitivity: Float = 1.0f,
    val controlSize: Float = 1.0f,
    val leftHanded: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val showFps: Boolean = false,
    val language: String = "EN",
    val endlessBestDistance: Float = 0f,
    val endlessBestTimeMs: Long = 0L,
    val endlessBestObstacles: Int = 0,
    val dailyLastCompletedEpochDay: Long = -1L,
    val totalWins: Int = 0,
    val totalCoinsEarned: Int = 120,
    val playerXp: Int = 0,
    val totalJumps: Int = 0,
    val rivalRaceWins: Int = 0
)

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelNumber: Int,
    val unlocked: Boolean = false,
    val completed: Boolean = false,
    val stars: Int = 0,
    val bestTimeMs: Long = 0L,
    val bestCoins: Int = 0,
    val goalMet: Boolean = false,
    val goalTimeMs: Long = 0L
)

@Entity(tableName = "world_progress")
data class WorldProgressEntity(
    @PrimaryKey val worldId: Int,
    val unlocked: Boolean = false,
    val gatewayLevelNumber: Int = worldId * 5,
    val gatewayGoalMet: Boolean = false,
    val unlockedAtMs: Long = 0L
)

@Entity(tableName = "shop_ownership")
data class ShopOwnershipEntity(
    @PrimaryKey val itemId: String,
    val category: String,
    val unlockedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val achievementId: String,
    val unlocked: Boolean = false,
    val progress: Int = 0,
    val rewardClaimed: Boolean = false
)

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val missionId: String,
    val progress: Int = 0,
    val completedCount: Int = 0
)
