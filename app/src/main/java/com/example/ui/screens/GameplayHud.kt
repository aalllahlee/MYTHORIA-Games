package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.MissionEntity
import com.example.data.db.PlayerProfileEntity
import com.example.game.engine3d.HudSnapshot
import com.example.game.model.*
import com.example.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun GameplayHud(
    hud: HudSnapshot,
    profile: PlayerProfileEntity,
    missions: List<MissionEntity> = emptyList(),
    language: AppLanguage = AppLanguage.EN,
    strings: LocalizedStrings,
    onJoystickMove: (Float, Float) -> Unit,
    onCameraDrag: (Float, Float) -> Unit,
    onJumpPress: () -> Unit,
    onJumpRelease: () -> Unit,
    onDive: () -> Unit,
    onDash: () -> Unit,
    onPause: () -> Unit
) {
    BackHandler { onPause() }

    val controlScale = profile.controlSize.coerceIn(0.75f, 1.35f)
    val joyRadiusDp = (64f * controlScale).dp
    val actionBtnDp = (74f * controlScale).dp

    Box(modifier = Modifier.fillMaxSize().testTag("gameplay_hud")) {
        // Full-screen upper/center camera drag layer (never blocks joystick or action buttons)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onCameraDrag(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level & Progress pill
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SurfaceCardDark,
                modifier = Modifier.border(1.5.dp, OutlineLight, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    val speedStr = String.format("%.2fx", hud.speedMultiplier)
                    val label = when (hud.mode) {
                        GameMode.ENDLESS -> "TIER ${hud.survivalTier} (${hud.complexityLabel}) • $speedStr • ${hud.distanceM.toInt()}m"
                        GameMode.RIVAL_RACE -> "AI RACE LV.${hud.levelNumber} • #${hud.raceRank}/${hud.totalRacers}"
                        GameMode.DAILY_CHALLENGE -> "DAILY • TIER ${hud.survivalTier} ($speedStr)"
                        GameMode.PRACTICE -> "PRACTICE ARENA"
                        else -> "LV.${hud.levelNumber} • ${hud.levelTitle} • $speedStr"
                    }
                    Text(label, fontFamily = FredokaFontFamily, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { hud.progressFraction },
                        modifier = Modifier.width(210.dp).height(7.dp).clip(RoundedCornerShape(6.dp)),
                        color = ChampionGold,
                        trackColor = Color(0x44FFFFFF)
                    )
                }
            }

            // Center Timer, Race Rank, & Coins
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (hud.mode == GameMode.RIVAL_RACE) {
                    val rankColor = when (hud.raceRank) {
                        1 -> ChampionGold
                        2 -> SkyCyan
                        3 -> IslandMint
                        else -> Color.White
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SurfaceCardDark,
                        modifier = Modifier
                            .border(2.dp, rankColor, RoundedCornerShape(50))
                            .testTag("race_rank_badge")
                    ) {
                        Text(
                            text = "${hud.raceRank}/${hud.totalRacers}",
                            fontFamily = FredokaFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
                val goalActive = hud.goalTimeMs > 0L
                val withinGoal = !goalActive || hud.elapsedMs <= hud.goalTimeMs
                val timerColor = when {
                    !goalActive -> SkyCyan
                    withinGoal -> IslandMint
                    else -> HeroCrimson
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceCardDark,
                    modifier = Modifier
                        .border(1.5.dp, timerColor.copy(alpha = 0.8f), RoundedCornerShape(50))
                        .testTag("hud_timer_pill")
                ) {
                    val sec = hud.elapsedMs / 1000
                    val ms = (hud.elapsedMs % 1000) / 10
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = String.format("%02d.%02ds", sec, ms),
                            fontFamily = FredokaFontFamily,
                            fontSize = 15.sp,
                            color = timerColor
                        )
                        if (goalActive) {
                            val goalSec = hud.goalTimeMs / 1000
                            Text(
                                text = "/ ${strings.goalTargetPrefix} ≤ ${goalSec}s",
                                fontFamily = FredokaFontFamily,
                                fontSize = 11.sp,
                                color = if (withinGoal) ChampionGold else HeroCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceCardDark,
                    modifier = Modifier
                        .border(1.5.dp, if (hud.checkpointReached > 0) SkyCyan else ChampionGold.copy(alpha = 0.75f), RoundedCornerShape(50))
                        .testTag("hud_checkpoint_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Flag,
                            contentDescription = null,
                            tint = if (hud.checkpointReached > 0) SkyCyan else ChampionGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        val cpText = if (hud.mode == GameMode.ENDLESS) {
                            "CHECKPOINT ${hud.checkpointReached}"
                        } else {
                            "CHECKPOINT ${hud.checkpointReached} / ${hud.totalCheckpoints}"
                        }
                        Text(
                            text = cpText,
                            fontFamily = FredokaFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hud.checkpointReached > 0) SkyCyan else ChampionGold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SurfaceCardDark,
                    modifier = Modifier.border(1.5.dp, ChampionGold, RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("${hud.coinsCollected}", fontFamily = FredokaFontFamily, fontSize = 15.sp, color = ChampionGold)
                    }
                }
                if (profile.showFps) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xAA000000)) {
                        Text("${hud.fps} FPS", color = IslandMint, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
            }

            // Pause button
            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SurfaceCardDark)
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(Icons.Filled.Pause, contentDescription = strings.paused, tint = Color.White)
            }
        }

        // Compact Active Mission pill on Top-Left (below main level bar)
        val trackedMission = GameCatalog.missions.firstOrNull()
        if (trackedMission != null && !hud.isVictory && !hud.isFailed) {
            val mMap = missions.associateBy { it.missionId }
            val jumpDef = GameCatalog.missions.find { it.id == "mis_jumps" } ?: trackedMission
            val showDef = if (hud.mode == GameMode.ENDLESS) jumpDef else trackedMission
            val baseProg = mMap[showDef.id]?.progress ?: 0
            val liveProg = if (showDef.type == MissionType.JUMP_COUNT) baseProg + hud.jumpsInRun else baseProg
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xBA0C1A42),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(start = 16.dp, top = 66.dp)
                    .border(1.dp, ChampionGold.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                    .testTag("hud_mission_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "${showDef.localizedTitle(language)} • ${liveProg.coerceAtMost(showDef.target)}/${showDef.target}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // MYTHORIA Checkpoint Activated / Portal Respawn Banner OR Survival Tier-Up Banner OR Level Start Banner
        if (hud.checkpointBannerText.isNotEmpty() && !hud.isVictory && !hud.isFailed) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xEE0A183D),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 74.dp)
                    .border(2.dp, SkyCyan, RoundedCornerShape(22.dp))
                    .testTag("checkpoint_activated_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = hud.checkpointBannerText,
                        fontFamily = FredokaFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChampionGold
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = SkyCyan, modifier = Modifier.size(18.dp))
                }
            }
        } else if (hud.tierBannerText.isNotEmpty() && !hud.isVictory && !hud.isFailed) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE8132D6A),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 74.dp)
                    .border(2.dp, ChampionGold, RoundedCornerShape(20.dp))
                    .testTag("tier_up_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${hud.tierBannerText} • ${String.format("%.2fx", hud.speedMultiplier)} SPEED!",
                        fontFamily = FredokaFontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChampionGold
                    )
                }
            }
        } else if (hud.elapsedMs < 2400L && !hud.isVictory && !hud.isFailed) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .safeDrawingPadding()
                    .padding(top = 74.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 6.dp
                ) {
                    Text(
                        text = hud.levelTitle.uppercase(),
                        fontFamily = FredokaFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNavyDeep,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SkyCyan,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    Text(
                        text = when (hud.mode) {
                            GameMode.ENDLESS -> "SURVIVE AS LONG AS YOU CAN!"
                            GameMode.RIVAL_RACE -> "BEAT ALL 4 AI RIVALS TO FINISH!"
                            else -> "RACE TO FINISH!"
                        },
                        fontFamily = FredokaFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalNavyDeep,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Bottom Controls Row (supports Left-Handed flip!)
        val joyAlignment = if (profile.leftHanded) Alignment.BottomEnd else Alignment.BottomStart
        val actionsAlignment = if (profile.leftHanded) Alignment.BottomStart else Alignment.BottomEnd

        Box(
            modifier = Modifier
                .align(joyAlignment)
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            VirtualJoystick(
                radius = joyRadiusDp,
                onMove = onJoystickMove
            )
        }

        Row(
            modifier = Modifier
                .align(actionsAlignment)
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (profile.dashUnlocked) {
                ActionButton(
                    label = strings.dash,
                    icon = Icons.Filled.Bolt,
                    size = (actionBtnDp * 0.84f),
                    gradient = listOf(SkyCyan, SkyCyanDark),
                    cooldownFraction = hud.dashCooldownFraction,
                    tag = "dash_button",
                    onPress = onDash
                )
            }
            ActionButton(
                label = strings.dive,
                icon = Icons.Filled.FastForward,
                size = (actionBtnDp * 0.86f),
                gradient = listOf(CandyMagenta, Color(0xFF7B2CBF)),
                tag = "dive_button",
                onPress = onDive
            )
            ActionButton(
                label = strings.jump,
                icon = Icons.Filled.ArrowUpward,
                size = actionBtnDp,
                gradient = listOf(Color(0xFF4ADE80), IslandMintDark),
                tag = "jump_button",
                onPress = onJumpPress,
                onRelease = onJumpRelease
            )
        }
    }
}

@Composable
private fun VirtualJoystick(
    radius: Dp,
    onMove: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val diameter = radius * 2

    Box(
        modifier = Modifier
            .size(diameter)
            .testTag("virtual_joystick")
            .pointerInput(radius) {
                val maxR = size.width * 0.42f
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val delta = change.position - center
                            val dist = hypot(delta.x, delta.y)
                            val clampedDist = dist.coerceAtMost(maxR)
                            val angle = atan2(delta.y, delta.x)
                            val nx = if (dist < maxR * 0.12f) 0f else (cos(angle) * (clampedDist / maxR))
                            val ny = if (dist < maxR * 0.12f) 0f else (sin(angle) * (clampedDist / maxR))
                            knobOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                            onMove(nx, ny)
                        } else {
                            knobOffset = Offset.Zero
                            onMove(0f, 0f)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0x660B1538), radius = size.minDimension * 0.48f, center = center)
            drawCircle(color = Color(0xAAFFFFFF), radius = size.minDimension * 0.48f, center = center, style = Stroke(width = 3.5f))
            val knobCenter = center + knobOffset
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, SkyCyan), center = knobCenter, radius = size.minDimension * 0.24f),
                radius = size.minDimension * 0.22f,
                center = knobCenter
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Dp,
    gradient: List<Color>,
    cooldownFraction: Float = 0f,
    tag: String,
    onPress: () -> Unit,
    onRelease: (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "btn_scale")

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.verticalGradient(gradient))
            .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .testTag(tag)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.pressed }
                        if (anyPressed && !pressed) {
                            pressed = true
                            onPress()
                        } else if (!anyPressed && pressed) {
                            pressed = false
                            onRelease?.invoke()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (cooldownFraction > 0.01f) {
            CircularProgressIndicator(
                progress = { 1f - cooldownFraction },
                modifier = Modifier.fillMaxSize().padding(4.dp),
                color = ChampionGold,
                strokeWidth = 4.dp
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
            Text(label, fontFamily = FredokaFontFamily, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
