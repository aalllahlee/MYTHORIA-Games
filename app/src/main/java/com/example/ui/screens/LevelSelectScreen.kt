package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.LevelProgressEntity
import com.example.data.db.WorldProgressEntity
import com.example.game.model.*
import com.example.ui.components.ArcadeButtonArtwork
import com.example.ui.components.ChestTierBadgeArtwork
import com.example.ui.components.MenuButtonArtType
import com.example.ui.components.WorldDioramaBanner
import com.example.ui.theme.*

@Composable
fun LevelSelectScreen(
    levelsProgress: List<LevelProgressEntity>,
    worldsProgress: List<WorldProgressEntity>,
    strings: LocalizedStrings,
    language: AppLanguage = AppLanguage.EN,
    onPlayLevel: (Int, GameMode) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedWorldId by remember { mutableIntStateOf(1) }
    var selectedMode by remember { mutableStateOf(GameMode.CAREER) }

    val progressMap = remember(levelsProgress) { levelsProgress.associateBy { it.levelNumber } }
    val worldProgressMap = remember(worldsProgress) { worldsProgress.associateBy { it.worldId } }
    val goalMetByLevel = remember(levelsProgress) { levelsProgress.associate { it.levelNumber to it.goalMet } }
    val graphUnlockedWorlds = remember(goalMetByLevel) { WorldProgressionGraph.computeUnlockedWorlds(goalMetByLevel) }
    val graphUnlockedLevels = remember(goalMetByLevel) { WorldProgressionGraph.computeUnlockedLevels(goalMetByLevel) }

    val totalStars = remember(levelsProgress) { levelsProgress.sumOf { it.stars } }
    val world = GameCatalog.worlds[(selectedWorldId - 1).coerceIn(0, GameCatalog.worlds.lastIndex)]
    val worldLevels = remember(selectedWorldId) {
        val start = (selectedWorldId - 1) * 5 + 1
        (start..(start + 4)).map { LevelCatalog.getLevel(it) }
    }

    fun isWorldUnlocked(worldId: Int): Boolean {
        if (worldId <= 1) return true
        val persistedUnlocked = worldProgressMap[worldId]?.unlocked
        val graphUnlocked = worldId in graphUnlockedWorlds
        return (persistedUnlocked ?: graphUnlocked) && graphUnlocked
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, Color(0xFF102154))))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("level_select_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header + Mode Selector (Career / Time Attack / AI Rival Race)
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
                            .testTag("levels_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChampionGold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(strings.worldMap, fontFamily = FredokaFontFamily, fontSize = 22.sp, color = ChampionGold)
                        Text(
                            "World ${world.id}: ${world.name}  •  ★ $totalStars / 150  •  ${graphUnlockedWorlds.size}/10 Worlds",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = selectedMode == GameMode.CAREER,
                        onClick = { selectedMode = GameMode.CAREER },
                        label = { Text(strings.careerMode, fontFamily = FredokaFontFamily, fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedMode == GameMode.TIME_ATTACK,
                        onClick = { selectedMode = GameMode.TIME_ATTACK },
                        label = { Text(strings.timeAttackMode, fontFamily = FredokaFontFamily, fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedMode == GameMode.RIVAL_RACE,
                        onClick = { selectedMode = GameMode.RIVAL_RACE },
                        label = { Text(strings.rivalRaceMode, fontFamily = FredokaFontFamily, fontSize = 12.sp) },
                        modifier = Modifier.testTag("mode_chip_rival_race")
                    )
                }
            }

            // World Progression Graph (10 Rich Illustrated World Diorama Cards + Directed Gateway Goal Edges)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(GameCatalog.worlds) { idx, w ->
                    val unlocked = isWorldUnlocked(w.id)
                    val active = w.id == selectedWorldId
                    val startNum = (w.id - 1) * 5 + 1
                    val worldStars = (startNum..(startNum + 4)).sumOf { progressMap[it]?.stars ?: 0 }
                    val topCol = Color(w.skyTop.r, w.skyTop.g, w.skyTop.b)
                    val bottomCol = Color(w.skyBottom.r, w.skyBottom.g, w.skyBottom.b)
                    val primaryCol = Color(w.platformPrimary.r, w.platformPrimary.g, w.platformPrimary.b)
                    val accentCol = Color(w.platformAccent.r, w.platformAccent.g, w.platformAccent.b)
                    val unlockEdge = WorldProgressionGraph.getUnlockEdgeForWorld(w.id)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .width(178.dp)
                            .height(118.dp)
                            .border(
                                width = if (active) 2.8.dp else 1.6.dp,
                                color = when {
                                    active -> ChampionGold
                                    unlocked -> primaryCol
                                    else -> Color(0x44FFFFFF)
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedWorldId = w.id }
                            .testTag("world_card_${w.id}")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Full Panoramic Illustrated Diorama of the World
                            WorldDioramaBanner(
                                worldId = w.id,
                                skyTop = topCol,
                                skyBottom = bottomCol,
                                primaryCol = primaryCol,
                                accentCol = accentCol,
                                unlocked = unlocked,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Subtle readability gradient overlay at top and bottom
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xAA060D24),
                                                Color(0x22060D24),
                                                Color(0xDD060D24)
                                            )
                                        )
                                    )
                                    .padding(9.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xCC09122C),
                                            modifier = Modifier.border(1.dp, ChampionGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                        ) {
                                            Text(
                                                "W${w.id} (${startNum}-${startNum + 4})",
                                                fontFamily = FredokaFontFamily,
                                                fontSize = 11.sp,
                                                color = ChampionGold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (!unlocked) {
                                            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        } else if (worldProgressMap[w.id]?.gatewayGoalMet == true || goalMetByLevel[w.id * 5] == true) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Gateway Cleared", tint = IslandMint, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            w.name,
                                            fontFamily = FredokaFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unlocked) Color.White else TextMuted,
                                            maxLines = 1
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xBB07102E), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Star, contentDescription = null, tint = StarYellow, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("$worldStars / 15", fontFamily = FredokaFontFamily, fontSize = 11.sp, color = Color.White)
                                            }
                                            if (!unlocked && unlockEdge != null) {
                                                Text(
                                                    "Lv.${unlockEdge.viaGatewayLevel}≤${unlockEdge.goal.targetTimeSec.toInt()}s",
                                                    fontSize = 10.sp,
                                                    color = ChampionGold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (idx < GameCatalog.worlds.lastIndex) {
                        val nextWorldUnlocked = isWorldUnlocked(w.id + 1)
                        Canvas(modifier = Modifier.width(30.dp).height(24.dp)) {
                            val edgeColor = if (nextWorldUnlocked) Color(0xFFFFD22B) else Color(0x55FFFFFF)
                            val cy = size.height / 2f
                            drawLine(
                                color = edgeColor,
                                start = Offset(2f, cy),
                                end = Offset(size.width - 8f, cy),
                                strokeWidth = 4f,
                                pathEffect = if (nextWorldUnlocked) null else PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                            )
                            val arrow = Path().apply {
                                moveTo(size.width - 2f, cy)
                                lineTo(size.width - 11f, cy - 6f)
                                lineTo(size.width - 11f, cy + 6f)
                                close()
                            }
                            drawPath(arrow, color = edgeColor)
                        }
                    }
                }
            }

            // World Unlock Requirement Banner (when selected world node is locked in the graph)
            val worldUnlocked = isWorldUnlocked(selectedWorldId)
            val selectedEdge = WorldProgressionGraph.getUnlockEdgeForWorld(selectedWorldId)
            if (!worldUnlocked && selectedEdge != null) {
                val gatewayProg = progressMap[selectedEdge.viaGatewayLevel]
                val bestGateStr = if ((gatewayProg?.bestTimeMs ?: 0L) > 0L) {
                    val ms = gatewayProg!!.bestTimeMs
                    "Best: ${ms / 1000}.${(ms % 1000) / 10}s"
                } else {
                    "Not cleared yet"
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x9935142C),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, HeroCrimson, RoundedCornerShape(14.dp))
                        .testTag("world_locked_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${strings.lockedWorldHint}: Beat W${selectedEdge.fromWorldId} Stage ${selectedEdge.viaGatewayLevel} in ≤ ${selectedEdge.goal.targetTimeSec.toInt()}s to unlock ${world.name}.",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(bestGateStr, fontSize = 11.sp, color = ChampionGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Stage Progression Path for Selected World (1 -> 2 -> 3 -> 4 -> 5 [WORLD GATE])
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                itemsIndexed(worldLevels) { sIdx, lvl ->
                    val prog = progressMap[lvl.number]
                    val unlocked = worldUnlocked && (lvl.number in graphUnlockedLevels) && (prog?.unlocked ?: (lvl.number == 1))
                    val stars = prog?.stars ?: 0
                    val bestMs = prog?.bestTimeMs ?: 0L
                    val goalMet = prog?.goalMet == true
                    val goalTimeSec = WorldProgressionGraph.goalTimeSecForLevel(lvl.number).toInt()
                    val isGateway = (lvl.number % 5 == 0) && lvl.number < 50
                    val accent = Color(world.platformAccent.r, world.platformAccent.g, world.platformAccent.b)
                    val topCol = Color(world.skyTop.r, world.skyTop.g, world.skyTop.b)
                    val bottomCol = Color(world.skyBottom.r, world.skyBottom.g, world.skyBottom.b)
                    val primaryCol = Color(world.platformPrimary.r, world.platformPrimary.g, world.platformPrimary.b)

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (unlocked) SurfaceCardGlass else Color(0x660B1126),
                        modifier = Modifier
                            .width(210.dp)
                            .fillMaxHeight(0.94f)
                            .offset(y = if (sIdx % 2 == 0) (-4).dp else 6.dp)
                            .border(
                                width = if (isGateway && unlocked) 2.5.dp else 2.dp,
                                color = when {
                                    !unlocked -> Color(0x33FFFFFF)
                                    isGateway -> ChampionGold
                                    else -> accent
                                },
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(enabled = unlocked) {
                                onPlayLevel(lvl.number, selectedMode)
                            }
                            .testTag("level_card_${lvl.number}")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Top Illustrated World Diorama Preview Header inside each Stage Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(62.dp)
                            ) {
                                WorldDioramaBanner(
                                    worldId = world.id,
                                    skyTop = topCol,
                                    skyBottom = bottomCol,
                                    primaryCol = primaryCol,
                                    accentCol = accent,
                                    unlocked = unlocked,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0x6607102E), Color(0xDD09122C))
                                            )
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (unlocked) accent else Color(0x44FFFFFF),
                                                modifier = Modifier.border(1.5.dp, Color.White, CircleShape)
                                            ) {
                                                Text(
                                                    text = "${lvl.number}",
                                                    fontFamily = FredokaFontFamily,
                                                    fontSize = 14.sp,
                                                    color = RoyalNavyDeep,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                                )
                                            }
                                            val diffTier = lvl.difficultyTier
                                            val diffCol = Color(diffTier.colorHex)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xCC07102E),
                                                modifier = Modifier.border(1.dp, diffCol, RoundedCornerShape(6.dp))
                                            ) {
                                                Text(
                                                    text = diffTier.localizedLabel(language),
                                                    fontFamily = FredokaFontFamily,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = diffCol,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (isGateway) {
                                                Surface(shape = RoundedCornerShape(8.dp), color = ChampionGold) {
                                                    Text(
                                                        text = strings.worldGateBadge,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RoyalNavyDeep,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (!unlocked) {
                                            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = TextMuted, modifier = Modifier.size(18.dp))
                                        } else {
                                            Row {
                                                for (s in 1..3) {
                                                    Icon(
                                                        Icons.Filled.Star,
                                                        contentDescription = null,
                                                        tint = if (s <= stars) StarYellow else Color(0x44FFFFFF),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val topChestTier = lvl.treasureChests.maxByOrNull { it.bonusCoins }?.tier ?: ChestTier.WOODEN
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        lvl.title,
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 13.sp,
                                        color = if (unlocked) Color.White else TextMuted,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    ChestTierBadgeArtwork(tier = topChestTier, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = if (bestMs > 0L) "Best: ${bestMs / 1000}.${(bestMs % 1000) / 10}s" else lvl.subtitle,
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (goalMet) Icons.Filled.CheckCircle else Icons.Filled.Timer,
                                        contentDescription = null,
                                        tint = if (goalMet) IslandMint else ChampionGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${strings.goalTargetPrefix}: Finish ≤ ${goalTimeSec}s",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (goalMet) IslandMint else ChampionGold
                                    )
                                }
                                Text(
                                    text = "★1: Clear • ★2: Goal/Secret • ★3: No Fall/3+ Perfect",
                                    fontSize = 9.sp,
                                    color = if (stars >= 3) StarYellow else TextSecondary,
                                    maxLines = 1
                                )
                            }

                            if (unlocked) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .fillMaxWidth()
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                if (selectedMode == GameMode.RIVAL_RACE) {
                                                    listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)
                                                } else {
                                                    listOf(MythicGoldLight, accent, ChampionGold)
                                                }
                                            )
                                        )
                                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ArcadeButtonArtwork(
                                            type = MenuButtonArtType.PLAY_CROWN,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (selectedMode == GameMode.RIVAL_RACE) strings.rivalRaceMode else strings.play,
                                            fontFamily = FredokaFontFamily,
                                            fontSize = 12.sp,
                                            color = RoyalNavyDeep,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    if (sIdx < worldLevels.lastIndex) {
                        val nextLvlUnlocked = worldUnlocked && ((lvl.number + 1) in graphUnlockedLevels)
                        Canvas(modifier = Modifier.width(28.dp).height(24.dp)) {
                            val edgeCol = if (nextLvlUnlocked) Color(0xFF00D4FF) else Color(0x33FFFFFF)
                            val cy = size.height / 2f
                            drawLine(
                                color = edgeCol,
                                start = Offset(0f, cy),
                                end = Offset(size.width - 7f, cy),
                                strokeWidth = 5f,
                                pathEffect = if (nextLvlUnlocked) null else PathEffect.dashPathEffect(floatArrayOf(8f, 5f), 0f)
                            )
                            val arrow = Path().apply {
                                moveTo(size.width, cy)
                                lineTo(size.width - 9f, cy - 6f)
                                lineTo(size.width - 9f, cy + 6f)
                                close()
                            }
                            drawPath(arrow, color = edgeCol)
                        }
                    }
                }
            }
        }
    }
}
