package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.GameRepository
import com.example.game.model.LevelCatalog
import com.example.game.model.WorldProgressionGraph
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DON'T FALL", appName)
  }

  @Test
  fun `world unlocking progression graph persists goal and unlock state in Room`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.gameDao()
      val repo = GameRepository(dao)
      repo.ensureInitialized()

      assertTrue(repo.isWorldUnlocked(1))
      assertFalse(repo.isWorldUnlocked(2))
      assertTrue(repo.isLevelUnlocked(1))
      assertFalse(repo.isLevelUnlocked(6))

      // Clear stages 1..4 within their goal time
      for (lvlNum in 1..4) {
        val lvl = LevelCatalog.getLevel(lvlNum)
        val fastTimeMs = WorldProgressionGraph.goalTimeMsForLevel(lvlNum) - 2000L
        val outcome = repo.recordCareerLevelWin(lvlNum, fastTimeMs, lvl.parTimeSec, 5, lvl.rewardCoins)
        assertTrue("Level $lvlNum goal should be met", outcome.goalMetThisRun)
        assertNull(outcome.newlyUnlockedWorldId)
      }
      assertTrue(repo.isLevelUnlocked(5))
      assertFalse(repo.isWorldUnlocked(2))

      // Complete World 1 gateway (Level 5) SLOWER than target goal time
      val lvl5 = LevelCatalog.getLevel(5)
      val slowTimeMs = WorldProgressionGraph.goalTimeMsForLevel(5) + 8000L
      val missedOutcome = repo.recordCareerLevelWin(5, slowTimeMs, lvl5.parTimeSec, 5, lvl5.rewardCoins)
      assertFalse("Slow run should not satisfy World 1 gateway goal", missedOutcome.goalMetThisRun)
      assertNull(missedOutcome.newlyUnlockedWorldId)
      assertFalse("World 2 must remain locked when gateway goal missed", repo.isWorldUnlocked(2))
      assertFalse("Level 6 must remain locked when World 2 is locked", repo.isLevelUnlocked(6))

      // Beat Level 5 within goal time -> unlocks World 2 and Level 6 in Room
      val winTimeMs = WorldProgressionGraph.goalTimeMsForLevel(5) - 3000L
      val unlockOutcome = repo.recordCareerLevelWin(5, winTimeMs, lvl5.parTimeSec, 5, lvl5.rewardCoins)
      assertTrue(unlockOutcome.goalMetThisRun)
      assertEquals(2, unlockOutcome.newlyUnlockedWorldId)
      assertEquals(6, unlockOutcome.newlyUnlockedLevelNumber)
      assertTrue("World 2 must now be unlocked in Room", repo.isWorldUnlocked(2))
      assertTrue("Level 6 must now be unlocked in Room", repo.isLevelUnlocked(6))

      val savedWorld1 = dao.getWorldOnce(1)!!
      val savedWorld2 = dao.getWorldOnce(2)!!
      assertTrue(savedWorld1.gatewayGoalMet)
      assertTrue(savedWorld2.unlocked)
      assertTrue(savedWorld2.unlockedAtMs > 0L)
    } finally {
      db.close()
    }
  }
}
