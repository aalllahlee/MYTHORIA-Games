package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
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
import com.example.data.db.AchievementEntity
import com.example.data.db.PlayerProfileEntity
import com.example.game.model.AppLanguage
import com.example.game.model.GameCatalog
import com.example.game.model.GraphicsQuality
import com.example.game.model.LocalizedStrings
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    profile: PlayerProfileEntity,
    strings: LocalizedStrings,
    onUpdateProfile: ((PlayerProfileEntity) -> PlayerProfileEntity) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, RoyalNavyMid)))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceCardDark)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(strings.settings, fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
                Spacer(Modifier.weight(1f))
                Text("MYTHORIA games — By ahmed", fontSize = 11.sp, color = ChampionGold, fontWeight = FontWeight.Bold)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SettingCard(title = strings.language) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = profile.language == lang.name,
                                    onClick = { onUpdateProfile { it.copy(language = lang.name) } },
                                    label = { Text(lang.displayName, fontFamily = FredokaFontFamily) }
                                )
                            }
                        }
                    }
                }
                item {
                    SettingCard(title = strings.graphicsQuality) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GraphicsQuality.entries.forEach { q ->
                                FilterChip(
                                    selected = profile.graphicsQuality == q.name,
                                    onClick = { onUpdateProfile { it.copy(graphicsQuality = q.name) } },
                                    label = { Text(q.label, fontFamily = FredokaFontFamily) }
                                )
                            }
                        }
                    }
                }
                item {
                    SettingToggleCard(strings.audioMuted, profile.audioMuted) { v ->
                        onUpdateProfile { it.copy(audioMuted = v) }
                    }
                }
                item {
                    SettingSliderCard(strings.musicVolume, profile.musicVolume, 0f..1f) { v ->
                        onUpdateProfile { it.copy(musicVolume = v) }
                    }
                }
                item {
                    SettingSliderCard(strings.sfxVolume, profile.sfxVolume, 0f..1f) { v ->
                        onUpdateProfile { it.copy(sfxVolume = v) }
                    }
                }
                item {
                    SettingSliderCard(strings.uiVolume, profile.uiVolume, 0f..1f) { v ->
                        onUpdateProfile { it.copy(uiVolume = v) }
                    }
                }
                item {
                    SettingSliderCard(strings.cameraSens, profile.cameraSensitivity, 0.5f..2.0f) { v ->
                        onUpdateProfile { it.copy(cameraSensitivity = v) }
                    }
                }
                item {
                    SettingSliderCard(strings.joystickSens, profile.joystickSensitivity, 0.6f..1.6f) { v ->
                        onUpdateProfile { it.copy(joystickSensitivity = v) }
                    }
                }
                item {
                    SettingSliderCard(strings.controlSize, profile.controlSize, 0.8f..1.3f) { v ->
                        onUpdateProfile { it.copy(controlSize = v) }
                    }
                }
                item {
                    SettingToggleCard(strings.cameraShake, profile.cameraShakeEnabled) { v ->
                        onUpdateProfile { it.copy(cameraShakeEnabled = v) }
                    }
                }
                item {
                    SettingToggleCard(strings.leftHanded, profile.leftHanded) { v ->
                        onUpdateProfile { it.copy(leftHanded = v) }
                    }
                }
                item {
                    SettingToggleCard(strings.haptics, profile.hapticsEnabled) { v ->
                        onUpdateProfile { it.copy(hapticsEnabled = v) }
                    }
                }
                item {
                    SettingToggleCard(strings.showFps, profile.showFps) { v ->
                        onUpdateProfile { it.copy(showFps = v) }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(
    achievements: List<AchievementEntity>,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val stateMap = achievements.associateBy { it.achievementId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, RoyalNavyMid)))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("achievements_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceCardDark)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(strings.achievements, fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(GameCatalog.achievements) { def ->
                    val st = stateMap[def.id]
                    val unlocked = st?.unlocked ?: false
                    val progress = st?.progress ?: 0
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SurfaceCardGlass,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, if (unlocked) ChampionGold else OutlineLight, RoundedCornerShape(18.dp))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = if (unlocked) ChampionGold else TextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(def.title, fontFamily = FredokaFontFamily, fontSize = 15.sp, color = Color.White)
                                Text(def.description, fontSize = 12.sp, color = TextSecondary)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (progress.toFloat() / def.target).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                                    color = if (unlocked) IslandMint else SkyCyan
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (unlocked) "UNLOCKED!" else "+${def.rewardCoins}",
                                fontFamily = FredokaFontFamily,
                                fontSize = 13.sp,
                                color = if (unlocked) IslandMint else ChampionGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutCreditsScreen(
    profile: PlayerProfileEntity,
    strings: LocalizedStrings,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, RoyalNavyMid)))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("about_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceCardDark)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Text(strings.about, fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
            }

            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceCardGlass,
                    modifier = Modifier.weight(1f).fillMaxHeight().border(2.dp, ChampionGold, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MythoriaLogoEmblem(compact = true)
                        Spacer(Modifier.height(4.dp))
                        Text("DON'T FALL!", fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
                        Text("MYTHORIA games", fontFamily = CinzelFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("By ahmed", fontSize = 13.sp, color = SapphireGlow, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "A colorful 3D mythological obstacle survival adventure built for smooth 100% offline Android gameplay.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceCardGlass,
                    modifier = Modifier.weight(1f).fillMaxHeight().border(1.5.dp, OutlineLight, RoundedCornerShape(24.dp))
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("HOW TO PLAY", fontFamily = FredokaFontFamily, fontSize = 18.sp, color = SkyCyan)
                        Text("• Virtual Joystick: Move relative to camera.", fontSize = 12.sp, color = Color.White)
                        Text("• Drag Screen: Orbit 3rd-person 3D camera.", fontSize = 12.sp, color = Color.White)
                        Text("• JUMP / DIVE / DASH: Clear hammers, beams, rolling balls, & collapsing bridges.", fontSize = 12.sp, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text("PLAYER RECORDS", fontFamily = FredokaFontFamily, fontSize = 16.sp, color = ChampionGold)
                        Text("• Player Level: ${com.example.game.model.XpProgression.levelFromXp(profile.playerXp)} (${profile.playerXp} XP)", fontSize = 12.sp, color = SkyCyan)
                        Text("• Total Wins: ${profile.totalWins}  (AI Races: ${profile.rivalRaceWins})", fontSize = 12.sp, color = TextSecondary)
                        Text("• Endless Best Distance: ${profile.endlessBestDistance.toInt()}m", fontSize = 12.sp, color = TextSecondary)
                        Text("• Total Coins Earned: ${profile.totalCoinsEarned}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceCardGlass, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontFamily = FredokaFontFamily, fontSize = 15.sp, color = Color.White)
            content()
        }
    }
}

@Composable
private fun SettingSliderCard(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceCardGlass, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontFamily = FredokaFontFamily, fontSize = 14.sp, color = Color.White, modifier = Modifier.width(180.dp))
            Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
            Text("${(value * 100).toInt()}%", fontFamily = FredokaFontFamily, fontSize = 13.sp, color = ChampionGold, modifier = Modifier.width(52.dp))
        }
    }
}

@Composable
private fun SettingToggleCard(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceCardGlass, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontFamily = FredokaFontFamily, fontSize = 15.sp, color = Color.White)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
