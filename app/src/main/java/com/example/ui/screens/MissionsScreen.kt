package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MissionEntity
import com.example.data.db.PlayerProfileEntity
import com.example.game.model.*
import com.example.ui.theme.*

@Composable
fun MissionsScreen(
    profile: PlayerProfileEntity,
    missions: List<MissionEntity>,
    language: AppLanguage,
    strings: LocalizedStrings,
    onClaimMission: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val playerLevel = XpProgression.levelFromXp(profile.playerXp)
    val xpInLevel = XpProgression.xpIntoCurrentLevel(profile.playerXp)
    val xpFraction = XpProgression.progressFraction(profile.playerXp)
    val nextMilestone = XpProgression.nextMilestone(playerLevel)
    val missionStateMap = missions.associateBy { it.missionId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, RoyalNavyMid)))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("missions_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardDark)
                            .testTag("missions_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(strings.missions, fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceCardDark,
                    modifier = Modifier.border(1.5.dp, ChampionGold, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${profile.coins}", fontFamily = FredokaFontFamily, fontSize = 15.sp, color = ChampionGold)
                    }
                }
            }

            // Two-column landscape layout: Left = XP Level + Next Milestone Reward (Golden Crown at Lv 30), Right = Active Missions
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Panel: Level & XP + Golden Crown Reward Card
                Column(
                    modifier = Modifier.weight(0.95f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SurfaceCardGlass,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, SkyCyan, RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${strings.levelLabel} $playerLevel",
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampionGold
                                )
                                Surface(shape = RoundedCornerShape(50), color = SkyCyan.copy(alpha = 0.22f)) {
                                    Text(
                                        "${xpInLevel} / ${XpProgression.XP_PER_LEVEL} XP",
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 12.sp,
                                        color = SkyCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(strings.xpLabel, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { xpFraction },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)),
                                color = ChampionGold,
                                trackColor = Color(0x33FFFFFF)
                            )
                        }
                    }

                    // Next Reward Card (Golden Crown at Level 30 highlighted)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SurfaceCardDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, ChampionGold, RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = ChampionGold.copy(alpha = 0.22f), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(28.dp))
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(strings.nextReward, fontSize = 10.sp, color = SkyCyan, fontWeight = FontWeight.Bold)
                                    Text(nextMilestone.title, fontFamily = FredokaFontFamily, fontSize = 17.sp, color = ChampionGold)
                                    Text("Unlock at Level ${nextMilestone.levelReq} (+${nextMilestone.rewardCoins} Coins)", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            // Highlighted Lv. 30 Golden Crown Progress
                            val crownTarget = 30
                            val crownProg = (playerLevel.toFloat() / crownTarget).coerceIn(0f, 1f)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x441B2D6E), RoundedCornerShape(14.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Golden Crown", fontFamily = FredokaFontFamily, fontSize = 13.sp, color = Color.White)
                                    Text(strings.goldenCrownReward, fontSize = 11.sp, color = ChampionGold, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { crownProg },
                                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp)),
                                    color = if (playerLevel >= 30) IslandMint else ChampionGold,
                                    trackColor = Color(0x33FFFFFF)
                                )
                            }
                        }
                    }

                    // Milestone Road Strip
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(GameCatalog.xpMilestones) { ms ->
                            val reached = playerLevel >= ms.levelReq
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (reached) IslandMintDark.copy(alpha = 0.35f) else SurfaceCardDark,
                                modifier = Modifier.border(
                                    1.2.dp,
                                    if (reached) IslandMint else OutlineLight,
                                    RoundedCornerShape(14.dp)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Lv.${ms.levelReq}", fontFamily = FredokaFontFamily, fontSize = 12.sp, color = if (reached) IslandMint else ChampionGold)
                                    Text(ms.title, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // Right Panel: Current Missions List
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SurfaceCardGlass,
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                        .border(1.5.dp, OutlineLight, RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.currentMissions, fontFamily = FredokaFontFamily, fontSize = 18.sp, color = SkyCyan)
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(GameCatalog.missions) { def ->
                                val st = missionStateMap[def.id]
                                val rawProg = st?.progress ?: 0
                                val ready = rawProg >= def.target
                                val shownProg = rawProg.coerceAtMost(def.target)

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SurfaceCardDark,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, if (ready) IslandMint else OutlineLight, RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val icon = when (def.type) {
                                            MissionType.COMPLETE_STAGES -> Icons.Filled.Flag
                                            MissionType.COLLECT_COINS -> Icons.Filled.MonetizationOn
                                            MissionType.JUMP_COUNT -> Icons.Filled.ArrowUpward
                                            MissionType.WIN_RIVAL_RACE -> Icons.Filled.EmojiEvents
                                        }
                                        Icon(icon, contentDescription = null, tint = if (ready) IslandMint else ChampionGold, modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(def.localizedTitle(language), fontFamily = FredokaFontFamily, fontSize = 14.sp, color = Color.White)
                                            Text(
                                                "$shownProg / ${def.target}   •   +${def.rewardCoins} ${strings.coins} & +${def.rewardXp} XP",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { (shownProg.toFloat() / def.target).coerceIn(0f, 1f) },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                                color = if (ready) IslandMint else SkyCyan,
                                                trackColor = Color(0x33FFFFFF)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Button(
                                            onClick = { onClaimMission(def.id) },
                                            enabled = ready,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (ready) IslandMintDark else Color(0x33FFFFFF),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(40.dp).testTag("claim_mission_${def.id}")
                                        ) {
                                            Text(strings.claim, fontFamily = FredokaFontFamily, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
