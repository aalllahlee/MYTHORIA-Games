package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameMode
import com.example.game.model.LocalizedStrings
import com.example.ui.theme.*
import com.example.viewmodel.VictorySummary

@Composable
fun PauseOverlay(
    strings: LocalizedStrings,
    hasCheckpoint: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onCheckpoint: () -> Unit,
    onSettings: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA050A1C))
            .testTag("pause_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = RoyalNavyMid,
            modifier = Modifier
                .width(340.dp)
                .border(2.dp, SkyCyan, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.paused, fontFamily = FredokaFontFamily, fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = IslandMintDark)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.resume, fontFamily = FredokaFontFamily, fontSize = 16.sp)
                }
                if (hasCheckpoint) {
                    Button(
                        onClick = onCheckpoint,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyCyanDark)
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.checkpoint, fontFamily = FredokaFontFamily, fontSize = 15.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(strings.replay, fontFamily = FredokaFontFamily, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onSettings,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(strings.settings, fontFamily = FredokaFontFamily, color = Color.White)
                    }
                }
                TextButton(onClick = onMainMenu, modifier = Modifier.height(48.dp)) {
                    Text(strings.mainMenu, fontFamily = FredokaFontFamily, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun VictoryOverlay(
    summary: VictorySummary,
    strings: LocalizedStrings,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA050A1C))
            .testTag("victory_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = RoyalNavyMid,
            modifier = Modifier
                .width(420.dp)
                .border(2.5.dp, ChampionGold, RoundedCornerShape(30.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (summary.mode == GameMode.RIVAL_RACE) "PLACE #${summary.racePlacement} / ${summary.totalRacers}!" else strings.levelCompleted,
                    fontFamily = FredokaFontFamily,
                    fontSize = 27.sp,
                    color = ChampionGold,
                    fontWeight = FontWeight.Bold
                )
                Text(summary.levelTitle, fontSize = 14.sp, color = TextSecondary)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (i <= summary.stars) StarYellow else Color(0x44FFFFFF),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // 3-Star Replayability Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "★1: Stage Clear ✓",
                        fontSize = 10.sp,
                        color = IslandMint,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (summary.stars >= 2) "★2: Goal / Secret ✓" else "★2: Beat Goal / Secret",
                        fontSize = 10.sp,
                        color = if (summary.stars >= 2) StarYellow else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (summary.stars >= 3) "★3: Pro Challenge ✓" else "★3: 0 Falls & 3+ Perfect",
                        fontSize = 10.sp,
                        color = if (summary.stars >= 3) StarYellow else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardDark, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("TIME", formatMs(summary.timeMs), SkyCyan)
                    StatColumn("BEST", formatMs(summary.bestTimeMs), IslandMint)
                    StatColumn("COINS", "+${summary.coinsEarned}", ChampionGold)
                    StatColumn("XP", "+${summary.xpEarned}", Color(0xFFFF88E8))
                }

                if (summary.goalTimeMs > 0L) {
                    val bannerColor = when {
                        summary.newlyUnlockedWorldId != null -> ChampionGold
                        summary.goalMet -> IslandMint
                        else -> HeroCrimson
                    }
                    val bannerBg = when {
                        summary.newlyUnlockedWorldId != null -> Color(0x44FFD22B)
                        summary.goalMet -> Color(0x332EE58D)
                        else -> Color(0x44FF3B5C)
                    }
                    val bannerText = when {
                        summary.newlyUnlockedWorldId != null ->
                            "${strings.worldUnlockedPrefix}: W${summary.newlyUnlockedWorldId} ${summary.newlyUnlockedWorldName ?: ""}!"
                        summary.goalMet ->
                            "${strings.goalMetBanner} (${formatMs(summary.timeMs)} ≤ ${formatMs(summary.goalTimeMs)})"
                        else ->
                            "${strings.goalMissedBanner} (${formatMs(summary.goalTimeMs)})"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bannerBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, bannerColor, RoundedCornerShape(12.dp))
                            .testTag("victory_goal_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (summary.goalMet) Icons.Filled.CheckCircle else Icons.Filled.Timer,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = bannerText,
                                fontFamily = FredokaFontFamily,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if ((summary.mode == GameMode.CAREER || summary.mode == GameMode.RIVAL_RACE) &&
                        summary.levelNumber < 50 &&
                        summary.canAdvanceNextLevel
                    ) {
                        Button(
                            onClick = onNextLevel,
                            modifier = Modifier.weight(1f).height(50.dp).testTag("next_level_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = IslandMintDark)
                        ) {
                            Text(strings.nextLevel, fontFamily = FredokaFontFamily, fontSize = 15.sp)
                        }
                    }
                    Button(
                        onClick = onReplay,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyCyanDark)
                    ) {
                        Text(strings.replay, fontFamily = FredokaFontFamily, fontSize = 15.sp)
                    }
                }

                TextButton(onClick = onMainMenu, modifier = Modifier.height(48.dp)) {
                    Text(strings.mainMenu, fontFamily = FredokaFontFamily, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun FailureOverlay(
    strings: LocalizedStrings,
    hasCheckpoint: Boolean,
    mode: GameMode,
    distanceM: Float,
    onRetry: () -> Unit,
    onCheckpoint: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB8090B1A))
            .testTag("failure_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = RoyalNavyMid,
            modifier = Modifier
                .width(370.dp)
                .border(2.5.dp, HeroCrimson, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(strings.dontGiveUp, fontFamily = FredokaFontFamily, fontSize = 28.sp, color = HeroCrimson, fontWeight = FontWeight.Bold)
                if (mode == GameMode.ENDLESS) {
                    Text("Distance Reached: ${distanceM.toInt()}m", fontSize = 14.sp, color = ChampionGold, fontWeight = FontWeight.Bold)
                }
                if (hasCheckpoint && mode != GameMode.ENDLESS) {
                    Button(
                        onClick = onCheckpoint,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("checkpoint_respawn_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IslandMintDark)
                    ) {
                        Icon(Icons.Filled.Flag, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.checkpoint, fontFamily = FredokaFontFamily, fontSize = 16.sp)
                    }
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("retry_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = HeroCrimson)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.retry, fontFamily = FredokaFontFamily, fontSize = 16.sp)
                }
                TextButton(onClick = onMainMenu, modifier = Modifier.height(48.dp)) {
                    Text(strings.mainMenu, fontFamily = FredokaFontFamily, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
        Text(value, fontFamily = FredokaFontFamily, fontSize = 18.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000
    val cs = (ms % 1000) / 10
    return String.format("%02d.%02ds", sec, cs)
}
