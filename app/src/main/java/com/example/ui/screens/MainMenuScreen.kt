package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.LevelProgressEntity
import com.example.data.db.MissionEntity
import com.example.data.db.PlayerProfileEntity
import com.example.game.model.GameCatalog
import com.example.game.model.GameMode
import com.example.game.model.LocalizedStrings
import com.example.game.model.XpProgression
import com.example.ui.components.ArcadeButtonArtwork
import com.example.ui.components.GameModeArtworkBadge
import com.example.ui.components.MenuButtonArtType
import com.example.ui.theme.*
import com.example.viewmodel.GameScreen

@Composable
fun MainMenuScreen(
    profile: PlayerProfileEntity,
    levelsProgress: List<LevelProgressEntity>,
    missions: List<MissionEntity>,
    strings: LocalizedStrings,
    showModeDialog: Boolean,
    onShowModeDialog: (Boolean) -> Unit,
    onPlayMode: (GameMode, Int) -> Unit,
    onNavigate: (GameScreen) -> Unit
) {
    val selectedSkin = GameCatalog.skins.find { it.id == profile.selectedSkinId } ?: GameCatalog.skins[0]
    val highestUnlocked = levelsProgress.filter { it.unlocked }.maxOfOrNull { it.levelNumber } ?: 1
    val totalStars = levelsProgress.sumOf { it.stars }
    val playerLevel = XpProgression.levelFromXp(profile.playerXp)
    val xpInLevel = XpProgression.xpIntoCurrentLevel(profile.playerXp)
    val missionMap = missions.associateBy { it.missionId }
    val hasClaimableMission = GameCatalog.missions.any { (missionMap[it.id]?.progress ?: 0) >= it.target }

    val pulse = rememberInfiniteTransition(label = "menu_pulse")
    val playScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(950, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "play_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("main_menu_screen")
    ) {
        // Top Status Bar (Royal Gold & Glowing Sapphire Borders)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Level XP + Stats badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceCardDark,
                    modifier = Modifier
                        .border(1.5.dp, ChampionGold, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigate(GameScreen.Missions) }
                        .testTag("xp_level_badge")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("LV. $playerLevel", fontFamily = FredokaFontFamily, fontSize = 13.sp, color = ChampionGold, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Text("${xpInLevel}/${XpProgression.XP_PER_LEVEL} XP", fontSize = 10.sp, color = SapphireGlow, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { XpProgression.progressFraction(profile.playerXp) },
                            modifier = Modifier.width(142.dp).height(5.dp).clip(RoundedCornerShape(4.dp)),
                            color = SapphireGlow,
                            trackColor = Color(0x33FFFFFF)
                        )
                    }
                }
                MenuBadge(
                    icon = Icons.Filled.MonetizationOn,
                    iconTint = ChampionGold,
                    text = "${profile.coins}",
                    tag = "coins_badge"
                )
                MenuBadge(
                    icon = Icons.Filled.Star,
                    iconTint = StarYellow,
                    text = "Stage $highestUnlocked  •  $totalStars★",
                    tag = "level_badge"
                )
                MenuBadge(
                    icon = Icons.Filled.Face,
                    iconTint = SapphireGlow,
                    text = selectedSkin.name,
                    tag = "character_badge"
                )
            }

            // Right utility buttons (Offline Ready + Missions + Achievements)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xCC0A183D),
                    modifier = Modifier.border(1.dp, ChampionGold.copy(alpha = 0.7f), RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(strings.offlineReady, color = MythicGoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box {
                    IconButton(
                        onClick = { onNavigate(GameScreen.Missions) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardDark)
                            .border(1.5.dp, SapphireGlow, CircleShape)
                            .testTag("missions_button")
                    ) {
                        ArcadeButtonArtwork(type = MenuButtonArtType.MISSIONS, modifier = Modifier.size(34.dp))
                    }
                    if (hasClaimableMission) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(HeroCrimson)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    }
                }
                IconButton(
                    onClick = { onNavigate(GameScreen.Achievements) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SurfaceCardDark)
                        .border(1.5.dp, ChampionGold, CircleShape)
                        .testTag("achievements_button")
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = strings.achievements, tint = ChampionGold)
                }
            }
        }

        // Left Column: Rich Illustrated Mythological Navigation Buttons (WORLDS, CHARACTERS, MISSIONS, SHOP, DAILY CHALLENGE)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 42.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ArcadeMenuButton(
                label = strings.levels,
                subtitle = "10 Worlds • 50 Stages",
                artType = MenuButtonArtType.WORLDS,
                gradient = listOf(Color(0xFF0369A1), Color(0xFF0C4A6E), Color(0xFF082F49)),
                borderColor = SapphireGlow,
                modifier = Modifier.testTag("levels_button"),
                onClick = { onNavigate(GameScreen.LevelSelect) }
            )
            ArcadeMenuButton(
                label = strings.characters,
                subtitle = "Ahmed • Salma • Ahmed",
                artType = MenuButtonArtType.CHARACTERS,
                gradient = listOf(Color(0xFF4338CA), Color(0xFF312E81), Color(0xFF1E1B4B)),
                borderColor = ChampionGold,
                modifier = Modifier.testTag("characters_button"),
                onClick = { onNavigate(GameScreen.Characters) }
            )
            ArcadeMenuButton(
                label = strings.missions,
                subtitle = "Royal Quests & Crown",
                artType = MenuButtonArtType.MISSIONS,
                gradient = listOf(Color(0xFF0284C7), Color(0xFF075985), Color(0xFF0C4A6E)),
                borderColor = if (hasClaimableMission) IslandMint else SapphireGlow,
                hasBadge = hasClaimableMission,
                modifier = Modifier.testTag("menu_missions_button"),
                onClick = { onNavigate(GameScreen.Missions) }
            )
            ArcadeMenuButton(
                label = strings.shop,
                subtitle = "Crowns, Boots & Trails",
                artType = MenuButtonArtType.SHOP,
                gradient = listOf(Color(0xFF7E22CE), Color(0xFF581C87), Color(0xFF3B0764)),
                borderColor = ChampionGold,
                modifier = Modifier.testTag("shop_button"),
                onClick = { onNavigate(GameScreen.Shop) }
            )
            ArcadeMenuButton(
                label = strings.dailyChallenge,
                subtitle = "2x Gold & XP Bonus",
                artType = MenuButtonArtType.DAILY_CHALLENGE,
                gradient = listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF78350F)),
                borderColor = MythicGoldLight,
                modifier = Modifier.testTag("daily_button"),
                onClick = { onPlayMode(GameMode.DAILY_CHALLENGE, 100) }
            )
        }

        // Bottom Center Studio Platform Plaque (under the 3D Hero showcase)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xDD09122C),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .border(1.5.dp, ChampionGold, RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SapphireDiamondIcon()
                Text(
                    text = "MYTHORIA games",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = ChampionGold,
                    letterSpacing = 1.2.sp
                )
                Text("•", color = SapphireGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "By ahmed",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MythicGoldLight
                )
                SapphireDiamondIcon()
            }
        }

        // Right Column: Official DON'T FALL + MYTHORIA games Crest + Royal Gold PLAY CTA + Settings/About
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Game Title + Official MYTHORIA games — By ahmed Royal Crest
            Column(
                modifier = Modifier
                    .shadow(14.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF214265C), Color(0xF5081029))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            listOf(ChampionGoldDark, MythicGoldLight, ChampionGold, MythicGoldLight, ChampionGoldDark)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DON'T FALL!",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = ChampionGold
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SapphireDiamondIcon()
                    Text(
                        text = "MYTHORIA games",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MythicGoldLight,
                        letterSpacing = 1.2.sp
                    )
                    SapphireDiamondIcon()
                }
                Text(
                    text = "By ahmed",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SapphireGlow
                )
            }

            // Giant Royal Gold & Sapphire PLAY Button with Embedded Illustrated Crown Medallion
            Box(
                modifier = Modifier
                    .scale(playScale)
                    .width(226.dp)
                    .height(64.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)
                        )
                    )
                    .border(2.5.dp, SapphireGlow, RoundedCornerShape(32.dp))
                    .clickable { onShowModeDialog(true) }
                    .padding(horizontal = 12.dp)
                    .testTag("play_button"),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ArcadeButtonArtwork(
                        type = MenuButtonArtType.PLAY_CROWN,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = strings.play,
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        color = RoyalNavyDeep
                    )
                    Surface(
                        shape = CircleShape,
                        color = RoyalNavyDeep,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallMenuButton(
                    artType = MenuButtonArtType.SETTINGS,
                    label = strings.settings,
                    tag = "settings_button",
                    onClick = { onNavigate(GameScreen.Settings) }
                )
                SmallMenuButton(
                    artType = MenuButtonArtType.ABOUT,
                    label = strings.about,
                    tag = "about_button",
                    onClick = { onNavigate(GameScreen.About) }
                )
            }
        }
    }

    if (showModeDialog) {
        GameModeDialog(
            strings = strings,
            highestUnlocked = highestUnlocked,
            onDismiss = { onShowModeDialog(false) },
            onSelectMode = { mode -> onPlayMode(mode, highestUnlocked) }
        )
    }
}

@Composable
private fun SapphireDiamondIcon() {
    Canvas(modifier = Modifier.size(10.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val path = Path().apply {
            moveTo(cx, 0f)
            lineTo(size.width, cy)
            lineTo(cx, size.height)
            lineTo(0f, cy)
            close()
        }
        drawPath(path, color = SapphireGlow)
    }
}

@Composable
private fun GameModeDialog(
    strings: LocalizedStrings,
    highestUnlocked: Int,
    onDismiss: () -> Unit,
    onSelectMode: (GameMode) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = RoyalNavyMid,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, ChampionGold, RoundedCornerShape(26.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SELECT MYTHORIA GAME MODE", fontFamily = FredokaFontFamily, fontSize = 20.sp, color = ChampionGold, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeCard(
                        title = strings.careerMode,
                        subtitle = "Stage $highestUnlocked / 50",
                        mode = GameMode.CAREER,
                        color = SapphireGlow,
                        modifier = Modifier.weight(1f).testTag("mode_career"),
                        onClick = { onSelectMode(GameMode.CAREER) }
                    )
                    ModeCard(
                        title = strings.rivalRaceMode,
                        subtitle = strings.reachFinishFirst,
                        mode = GameMode.RIVAL_RACE,
                        color = ChampionGold,
                        modifier = Modifier.weight(1f).testTag("mode_rival_race"),
                        onClick = { onSelectMode(GameMode.RIVAL_RACE) }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeCard(
                        title = strings.endlessMode,
                        subtitle = "Infinite Sky Temple Survival",
                        mode = GameMode.ENDLESS,
                        color = SkyCyanDark,
                        modifier = Modifier.weight(1f).testTag("mode_endless"),
                        onClick = { onSelectMode(GameMode.ENDLESS) }
                    )
                    ModeCard(
                        title = strings.timeAttackMode,
                        subtitle = "Beat Temple Par Medals",
                        mode = GameMode.TIME_ATTACK,
                        color = HeroCrimson,
                        modifier = Modifier.weight(1f).testTag("mode_time_attack"),
                        onClick = { onSelectMode(GameMode.TIME_ATTACK) }
                    )
                }
                ModeCard(
                    title = strings.practiceMode,
                    subtitle = "Master Jump, Dive & Thunder Dash",
                    mode = GameMode.PRACTICE,
                    color = CandyMagenta,
                    modifier = Modifier.fillMaxWidth().testTag("mode_practice"),
                    onClick = { onSelectMode(GameMode.PRACTICE) }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    subtitle: String,
    mode: GameMode,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.20f),
        modifier = modifier
            .height(76.dp)
            .border(1.8.dp, color, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameModeArtworkBadge(mode = mode, accent = color, modifier = Modifier.size(50.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontFamily = FredokaFontFamily, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun MenuBadge(icon: ImageVector, iconTint: Color, text: String, tag: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = SurfaceCardDark,
        modifier = Modifier
            .border(1.5.dp, ChampionGold.copy(alpha = 0.65f), RoundedCornerShape(50))
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontFamily = FredokaFontFamily, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArcadeMenuButton(
    label: String,
    subtitle: String,
    artType: MenuButtonArtType,
    gradient: List<Color>,
    borderColor: Color,
    hasBadge: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "arcade_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(pressScale)
            .width(214.dp)
            .height(50.dp)
            .shadow(10.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradient))
            .border(
                width = 1.8.dp,
                brush = Brush.horizontalGradient(listOf(ChampionGold, borderColor, ChampionGold)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                ArcadeButtonArtwork(type = artType, modifier = Modifier.size(38.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        fontFamily = FredokaFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        fontSize = 9.sp,
                        color = MythicGoldLight.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(IslandMint)
                        .border(1.dp, Color.White, CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = ChampionGold.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallMenuButton(
    artType: MenuButtonArtType,
    label: String,
    tag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "small_btn_scale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCardDark,
        modifier = Modifier
            .scale(pressScale)
            .height(48.dp)
            .border(1.5.dp, ChampionGold.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArcadeButtonArtwork(type = artType, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontFamily = FredokaFontFamily, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
