package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    fun observeAllLevels(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress ORDER BY levelNumber ASC")
    suspend fun getAllLevelsOnce(): List<LevelProgressEntity>

    @Query("SELECT * FROM level_progress WHERE levelNumber = :levelNumber")
    suspend fun getLevelOnce(levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLevel(level: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLevelsIgnore(levels: List<LevelProgressEntity>)

    @Query("SELECT * FROM world_progress ORDER BY worldId ASC")
    fun observeAllWorlds(): Flow<List<WorldProgressEntity>>

    @Query("SELECT * FROM world_progress ORDER BY worldId ASC")
    suspend fun getAllWorldsOnce(): List<WorldProgressEntity>

    @Query("SELECT * FROM world_progress WHERE worldId = :worldId")
    suspend fun getWorldOnce(worldId: Int): WorldProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorld(world: WorldProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorldsIgnore(worlds: List<WorldProgressEntity>)

    @Query("SELECT * FROM shop_ownership")
    fun observeOwnership(): Flow<List<ShopOwnershipEntity>>

    @Query("SELECT * FROM shop_ownership WHERE itemId = :itemId")
    suspend fun getOwnershipOnce(itemId: String): ShopOwnershipEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwnershipIgnore(items: List<ShopOwnershipEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwnership(item: ShopOwnershipEntity)

    @Query("SELECT * FROM achievements")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE achievementId = :id")
    suspend fun getAchievementOnce(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementsIgnore(items: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(item: AchievementEntity)

    @Query("SELECT * FROM missions")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT * FROM missions WHERE missionId = :id")
    suspend fun getMissionOnce(id: String): MissionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissionsIgnore(items: List<MissionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMission(item: MissionEntity)
}
