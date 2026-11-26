package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.GameRepository
import com.example.data.db.MissionEntity
import com.example.data.db.PlayerProfileEntity
import com.example.game.model.*
import com.example.ui.components.ArcadeButtonArtwork
import com.example.ui.components.ChestTierBadgeArtwork
import com.example.ui.components.MenuButtonArtType
import com.example.ui.components.MissionEmblemArtwork
import com.example.ui.theme.*

@Composable
fun MissionsScreen(
    profile: PlayerProfileEntity,
    missions: List<MissionEntity>,
    language: AppLanguage,
    strings: LocalizedStrings,
    onClaimMission: (String) -> Unit,
    onClaimLoginReward: () -> Unit = {},
    onPlaySeasonalEvent: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedCadenceFilter by remember { mutableStateOf<MissionCadence?>(null) }

    val playerLevel = XpProgression.levelFromXp(profile.playerXp)
    val xpInLevel = XpProgression.xpIntoCurrentLevel(profile.playerXp)
    val xpFraction = XpProgression.progressFraction(profile.playerXp)
    val nextMilestone = XpProgression.nextMilestone(playerLevel)
    val missionStateMap = missions.associateBy { it.missionId }

    val todayEpochDay = remember { System.currentTimeMillis() / 86_400_000L }
    val streakTracker = missionStateMap[GameRepository.LOGIN_STREAK_TRACKER_ID]
    val claimedToday = (streakTracker?.completedCount?.toLong() == todayEpochDay)
    val currentStreakDay = ((streakTracker?.progress ?: 0).coerceAtLeast(0) % 7) + if (claimedToday) 0 else 1
    val activeStreakDay = currentStreakDay.coerceIn(1, 7)

    val activeEvent = remember(todayEpochDay) { GameCatalog.getActiveSeasonalEvent(todayEpochDay) }
    val eventDaysRemaining = remember(todayEpochDay) { GameCatalog.getSeasonalEventDaysRemaining(todayEpochDay) }

    val filteredMissions = remember(selectedCadenceFilter) {
        if (selectedCadenceFilter == null) {
            GameCatalog.missions
        } else {
            GameCatalog.missions.filter { it.cadence == selectedCadenceFilter }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, RoyalNavyMid)))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("missions_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .border(1.5.dp, ChampionGold, CircleShape)
                            .testTag("missions_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampionGold)
                    }
                    Spacer(Modifier.width(12.dp))
                    ArcadeButtonArtwork(type = MenuButtonArtType.MISSIONS, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(strings.missions, fontFamily = FredokaFontFamily, fontSize = 22.sp, color = ChampionGold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Cadence Filter Chips (All / Daily / Weekly / Seasonal Event)
                    FilterChip(
                        selected = selectedCadenceFilter == null,
                        onClick = { selectedCadenceFilter = null },
                        label = {
                            Text(
                                if (language == AppLanguage.AR) "الكل" else "ALL",
                                fontFamily = FredokaFontFamily,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("missions_tab_all")
                    )
                    MissionCadence.entries.forEach { cadence ->
                        FilterChip(
                            selected = selectedCadenceFilter == cadence,
                            onClick = { selectedCadenceFilter = cadence },
                            label = {
                                Text(
                                    cadence.localizedLabel(language),
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.testTag("missions_tab_${cadence.name.lowercase()}")
                        )
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
            }

            // Two-column landscape layout:
            // Left = XP Level + 7-Day Login Streak Rewards + Active Seasonal Event Banner
            // Right = Filtered Daily / Weekly / Event Missions with Status & Chest Badges
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Panel
                Column(
                    modifier = Modifier.weight(0.95f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Compact Level & XP + Next Milestone Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCardGlass,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.8.dp, SkyCyan, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ArcadeButtonArtwork(type = MenuButtonArtType.PLAY_CROWN, modifier = Modifier.size(30.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "${strings.levelLabel} $playerLevel  •  ${nextMilestone.title} (Lv.${nextMilestone.levelReq})",
                                            fontFamily = FredokaFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChampionGold
                                        )
                                        Text(
                                            "${xpInLevel} / ${XpProgression.XP_PER_LEVEL} XP",
                                            fontSize = 10.sp,
                                            color = SkyCyan
                                        )
                                    }
                                }
                                Text(
                                    strings.goldenCrownReward,
                                    fontSize = 10.sp,
                                    color = MythicGoldLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { xpFraction },
                                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(6.dp)),
                                color = ChampionGold,
                                trackColor = Color(0x33FFFFFF)
                            )
                        }
                    }

                    // 2. Forgiving 7-Day Login Streak Reward Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCardDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.8.dp, ChampionGold, RoundedCornerShape(18.dp))
                            .testTag("login_streak_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (language == AppLanguage.AR) "مكافآت الدخول اليومي (7 أيام)" else "7-DAY LOGIN STREAK REWARDS",
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChampionGold
                                    )
                                    Text(
                                        text = if (language == AppLanguage.AR) "سلسلة متوازنة لا تُصفّر التقدم عند تفويت يوم" else "Forgiving streak — never resets if you miss a day",
                                        fontSize = 9.sp,
                                        color = TextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (!claimedToday) {
                                                Brush.verticalGradient(listOf(IslandMint, IslandMintDark))
                                            } else {
                                                Brush.verticalGradient(listOf(Color(0x441E293B), Color(0x440F172A)))
                                            }
                                        )
                                        .border(
                                            1.2.dp,
                                            if (!claimedToday) ChampionGold else Color(0x33FFFFFF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !claimedToday) { onClaimLoginReward() }
                                        .padding(horizontal = 10.dp)
                                        .testTag("claim_login_reward_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (claimedToday) {
                                            if (language == AppLanguage.AR) "تم الاستلام اليوم ✓" else "CLAIMED TODAY ✓"
                                        } else {
                                            if (language == AppLanguage.AR) "استلم اليوم $activeStreakDay" else "CLAIM DAY $activeStreakDay"
                                        },
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!claimedToday) RoyalNavyDeep else TextMuted
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(GameCatalog.loginStreakRewards) { dayDef ->
                                    val isPast = if (claimedToday) dayDef.dayNumber <= activeStreakDay else dayDef.dayNumber < activeStreakDay
                                    val isCurrent = !claimedToday && dayDef.dayNumber == activeStreakDay
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            isCurrent -> Color(0x552EE58D)
                                            isPast -> Color(0x3310B981)
                                            else -> Color(0x660B132B)
                                        },
                                        modifier = Modifier.border(
                                            1.2.dp,
                                            when {
                                                isCurrent -> ChampionGold
                                                isPast -> IslandMint
                                                else -> Color(0x33FFFFFF)
                                            },
                                            RoundedCornerShape(12.dp)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            ChestTierBadgeArtwork(tier = dayDef.chestTier, modifier = Modifier.size(24.dp))
                                            Text(
                                                text = "D${dayDef.dayNumber}",
                                                fontFamily = FredokaFontFamily,
                                                fontSize = 10.sp,
                                                color = if (isCurrent) ChampionGold else Color.White
                                            )
                                            Text(
                                                text = "+${dayDef.rewardCoins}",
                                                fontFamily = FredokaFontFamily,
                                                fontSize = 9.sp,
                                                color = if (isPast) IslandMint else MythicGoldLight
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Seasonal Limited-Time Event Card (Offline-Local Event with 3D Arena Render Banner, Countdown & Event Chest)
                    val eventBannerCol = Color(activeEvent.bannerColorHex)
                    val eventAccentCol = Color(activeEvent.accentColorHex)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.8.dp, eventAccentCol, RoundedCornerShape(18.dp))
                            .testTag("seasonal_event_card")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_event_mythoria_arena),
                                contentDescription = activeEvent.localizedTitle(language),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xEE09122C),
                                                eventBannerCol.copy(alpha = 0.45f),
                                                Color(0xDD09122C)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    ChestTierBadgeArtwork(tier = ChestTier.EVENT_BOSS, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = eventBannerCol.copy(alpha = 0.28f),
                                            modifier = Modifier.border(1.dp, ChampionGold, RoundedCornerShape(6.dp))
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.AR) "فعالية موسمية • متبقي $eventDaysRemaining أيام" else "SEASONAL EVENT • ${eventDaysRemaining}D LEFT",
                                                fontFamily = FredokaFontFamily,
                                                fontSize = 9.sp,
                                                color = ChampionGold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = activeEvent.localizedTitle(language),
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = activeEvent.localizedSubtitle(language),
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.verticalGradient(listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                                    .clickable { onPlaySeasonalEvent() }
                                    .padding(horizontal = 10.dp)
                                    .testTag("play_seasonal_event_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = RoyalNavyDeep, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (language == AppLanguage.AR) "العب الفعالية" else "PLAY EVENT",
                                            fontFamily = FredokaFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoyalNavyDeep
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Panel: Current Missions List with Bespoke Royal Quest Medallions & Status Badges
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SurfaceCardGlass,
                    modifier = Modifier
                        .weight(1.22f)
                        .fillMaxHeight()
                        .border(1.8.dp, ChampionGold.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.currentMissions, fontFamily = FredokaFontFamily, fontSize = 18.sp, color = ChampionGold)
                            Text(
                                text = "${filteredMissions.size} Royal Quests",
                                fontFamily = FredokaFontFamily,
                                fontSize = 12.sp,
                                color = SapphireGlow
                            )
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            items(filteredMissions) { def ->
                                val st = missionStateMap[def.id]
                                val rawProg = st?.progress ?: 0
                                val claimedCount = st?.completedCount ?: 0
                                val ready = rawProg >= def.target
                                val shownProg = rawProg.coerceAtMost(def.target)
                                val progFrac = (shownProg.toFloat() / def.target).coerceIn(0f, 1f)

                                val missionStatus = when {
                                    ready -> MissionStatus.COMPLETED
                                    rawProg > 0 -> MissionStatus.IN_PROGRESS
                                    claimedCount > 0 -> MissionStatus.CLAIMED
                                    else -> MissionStatus.AVAILABLE
                                }
                                val statusColor = when (missionStatus) {
                                    MissionStatus.COMPLETED -> IslandMint
                                    MissionStatus.IN_PROGRESS -> ChampionGold
                                    MissionStatus.CLAIMED -> SapphireGlow
                                    MissionStatus.AVAILABLE -> TextSecondary
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(18.dp))
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                if (ready) {
                                                    listOf(Color(0xFF064E3B), Color(0xFF0F2942))
                                                } else {
                                                    listOf(Color(0xFF132352), Color(0xFF09122C))
                                                }
                                            )
                                        )
                                        .border(
                                            width = if (ready) 2.dp else 1.5.dp,
                                            brush = if (ready) {
                                                Brush.horizontalGradient(listOf(IslandMint, ChampionGold, IslandMint))
                                            } else {
                                                Brush.horizontalGradient(listOf(ChampionGold.copy(alpha = 0.7f), SapphireGlow.copy(alpha = 0.5f)))
                                            },
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Bespoke Illustrated 58.dp Royal Quest Medallion
                                        MissionEmblemArtwork(
                                            type = def.type,
                                            ready = ready,
                                            modifier = Modifier.size(58.dp)
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xAA07102E),
                                                    modifier = Modifier.border(1.dp, SapphireGlow.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                                ) {
                                                    Text(
                                                        text = def.cadence.localizedLabel(language),
                                                        fontFamily = FredokaFontFamily,
                                                        fontSize = 9.sp,
                                                        color = SapphireGlow,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = statusColor.copy(alpha = 0.18f),
                                                    modifier = Modifier.border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                                ) {
                                                    Text(
                                                        text = missionStatus.localizedLabel(language),
                                                        fontFamily = FredokaFontFamily,
                                                        fontSize = 9.sp,
                                                        color = statusColor,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = def.localizedTitle(language),
                                                fontFamily = FredokaFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2
                                            )

                                            // Illustrated Reward Badges (+Coins & +XP) + Progress Counter
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xAA07102E),
                                                    modifier = Modifier.border(1.dp, ChampionGold.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(3.dp))
                                                        Text("+${def.rewardCoins}", fontFamily = FredokaFontFamily, fontSize = 10.sp, color = ChampionGold)
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xAA07102E),
                                                    modifier = Modifier.border(1.dp, SapphireGlow.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SapphireGlow, modifier = Modifier.size(12.dp))
                                                        Spacer(Modifier.width(3.dp))
                                                        Text("+${def.rewardXp} XP", fontFamily = FredokaFontFamily, fontSize = 10.sp, color = SapphireGlow)
                                                    }
                                                }
                                                Spacer(Modifier.weight(1f))
                                                Text(
                                                    text = "$shownProg / ${def.target}",
                                                    fontFamily = FredokaFontFamily,
                                                    fontSize = 11.sp,
                                                    color = if (ready) IslandMint else MythicGoldLight,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            LinearProgressIndicator(
                                                progress = { progFrac },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(7.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = if (ready) IslandMint else ChampionGold,
                                                trackColor = Color(0x33FFFFFF)
                                            )
                                        }

                                        Spacer(Modifier.width(10.dp))

                                        // 3D Royal Metallic Claim Button with Embedded Emblem
                                        Box(
                                            modifier = Modifier
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (ready) {
                                                        Brush.verticalGradient(listOf(IslandMint, IslandMintDark))
                                                    } else {
                                                        Brush.verticalGradient(listOf(Color(0x441E293B), Color(0x440F172A)))
                                                    }
                                                )
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (ready) ChampionGold else Color(0x33FFFFFF),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable(enabled = ready) { onClaimMission(def.id) }
                                                .padding(horizontal = 10.dp)
                                                .testTag("claim_mission_${def.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                ArcadeButtonArtwork(
                                                    type = if (ready) MenuButtonArtType.PLAY_CROWN else MenuButtonArtType.SHOP,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                                Text(
                                                    text = strings.claim,
                                                    fontFamily = FredokaFontFamily,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ready) RoyalNavyDeep else TextMuted
                                                )
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
}
