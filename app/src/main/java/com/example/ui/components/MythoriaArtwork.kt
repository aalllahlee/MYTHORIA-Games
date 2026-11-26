package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.game.model.ChestTier
import com.example.game.model.GameMode
import com.example.game.model.MissionType
import com.example.game.model.ShopCategory
import com.example.game.model.ShopItemDef
import com.example.game.model.SkinDef
import com.example.ui.theme.ChampionGold
import com.example.ui.theme.ChampionGoldDark
import com.example.ui.theme.HeroCrimson
import com.example.ui.theme.IslandMint
import com.example.ui.theme.MythicGoldLight
import com.example.ui.theme.RoyalNavyDeep
import com.example.ui.theme.SapphireGlow
import kotlin.math.cos
import kotlin.math.sin

enum class MenuButtonArtType {
    WORLDS,
    CHARACTERS,
    MISSIONS,
    SHOP,
    DAILY_CHALLENGE,
    PLAY_CROWN,
    SETTINGS,
    ABOUT
}

/**
 * High-detail illustrated button emblem medallion rendered inside Main Menu & Game Mode buttons.
 */
@Composable
fun ArcadeButtonArtwork(
    type: MenuButtonArtType,
    modifier: Modifier = Modifier.size(38.dp)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF1E3A8A), Color(0xFF09122C))
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            // Subtle inner radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SapphireGlow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f
            )

            when (type) {
                MenuButtonArtType.WORLDS -> {
                    // Floating Sky Castle Island + Waterfall + Golden Stepping Path
                    val rock = Path().apply {
                        moveTo(w * 0.20f, h * 0.54f)
                        lineTo(w * 0.80f, h * 0.54f)
                        lineTo(w * 0.58f, h * 0.84f)
                        lineTo(w * 0.42f, h * 0.84f)
                        close()
                    }
                    drawPath(rock, color = Color(0xFF7C4D2B))
                    drawRoundRect(
                        color = IslandMint,
                        topLeft = Offset(w * 0.18f, h * 0.46f),
                        size = Size(w * 0.64f, h * 0.11f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Castle spires
                    drawRect(color = Color.White, topLeft = Offset(w * 0.42f, h * 0.22f), size = Size(w * 0.16f, h * 0.25f))
                    drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(w * 0.27f, h * 0.30f), size = Size(w * 0.12f, h * 0.17f))
                    drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(w * 0.61f, h * 0.30f), size = Size(w * 0.12f, h * 0.17f))
                    drawTriangle(cx, h * 0.09f, w * 0.39f, h * 0.23f, w * 0.61f, h * 0.23f, ChampionGold)
                    drawTriangle(w * 0.33f, h * 0.19f, w * 0.25f, h * 0.31f, w * 0.41f, h * 0.31f, ChampionGold)
                    drawTriangle(w * 0.67f, h * 0.19f, w * 0.59f, h * 0.31f, w * 0.75f, h * 0.31f, ChampionGold)
                    // Cascading sapphire waterfall
                    drawRoundRect(
                        color = SapphireGlow,
                        topLeft = Offset(w * 0.46f, h * 0.52f),
                        size = Size(w * 0.09f, h * 0.36f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                MenuButtonArtType.CHARACTERS -> {
                    // Full-Body Hero Figure inside the Characters Button (Head, Crown, Cape, Tunic, Arms, Legs & Boots)
                    // Flowing Crimson Royal Cape behind body
                    val cape = Path().apply {
                        moveTo(w * 0.34f, h * 0.36f)
                        lineTo(w * 0.66f, h * 0.36f)
                        lineTo(w * 0.78f, h * 0.78f)
                        lineTo(w * 0.22f, h * 0.78f)
                        close()
                    }
                    drawPath(cape, brush = Brush.verticalGradient(listOf(HeroCrimson, Color(0xFF881337))))
                    drawPath(cape, color = ChampionGold, style = Stroke(width = 1.4f))

                    // Left & Right Legs + Golden Boots
                    drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(w * 0.36f, h * 0.62f), size = Size(w * 0.11f, h * 0.19f), cornerRadius = CornerRadius(3f, 3f))
                    drawRoundRect(color = Color(0xFF1E293B), topLeft = Offset(w * 0.53f, h * 0.62f), size = Size(w * 0.11f, h * 0.19f), cornerRadius = CornerRadius(3f, 3f))
                    drawRoundRect(color = ChampionGold, topLeft = Offset(w * 0.34f, h * 0.78f), size = Size(w * 0.14f, h * 0.10f), cornerRadius = CornerRadius(3f, 3f))
                    drawRoundRect(color = ChampionGold, topLeft = Offset(w * 0.52f, h * 0.78f), size = Size(w * 0.14f, h * 0.10f), cornerRadius = CornerRadius(3f, 3f))

                    // Armored Torso Tunic & Golden Belt
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(HeroCrimson, Color(0xFF9F1239))),
                        topLeft = Offset(w * 0.34f, h * 0.38f),
                        size = Size(w * 0.32f, h * 0.26f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    drawRect(color = ChampionGold, topLeft = Offset(w * 0.34f, h * 0.57f), size = Size(w * 0.32f, h * 0.05f))

                    // Arms & Golden Gauntlets
                    drawRoundRect(color = Color(0xFFF3B89A), topLeft = Offset(w * 0.23f, h * 0.40f), size = Size(w * 0.10f, h * 0.20f), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(color = Color(0xFFF3B89A), topLeft = Offset(w * 0.67f, h * 0.40f), size = Size(w * 0.10f, h * 0.20f), cornerRadius = CornerRadius(4f, 4f))
                    drawCircle(color = ChampionGold, radius = w * 0.055f, center = Offset(w * 0.28f, h * 0.58f))
                    drawCircle(color = ChampionGold, radius = w * 0.055f, center = Offset(w * 0.72f, h * 0.58f))

                    // Head, Hair & Golden Crown
                    drawCircle(color = Color(0xFFF3B89A), radius = w * 0.14f, center = Offset(cx, h * 0.26f))
                    drawCircle(color = Color(0xFF1F2937), radius = w * 0.025f, center = Offset(cx - w * 0.05f, h * 0.26f))
                    drawCircle(color = Color(0xFF1F2937), radius = w * 0.025f, center = Offset(cx + w * 0.05f, h * 0.26f))
                    drawCrownShape(cx, h * 0.16f, w * 0.28f, h * 0.11f)
                }

                MenuButtonArtType.MISSIONS -> {
                    // Royal Golden Scroll + Glowing Star Crest + Wax Seal
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFF59E0B))),
                        topLeft = Offset(w * 0.24f, h * 0.18f),
                        size = Size(w * 0.52f, h * 0.64f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Top & bottom golden scroll rollers
                    drawRoundRect(
                        color = ChampionGold,
                        topLeft = Offset(w * 0.17f, h * 0.14f),
                        size = Size(w * 0.66f, h * 0.10f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    drawRoundRect(
                        color = ChampionGold,
                        topLeft = Offset(w * 0.17f, h * 0.76f),
                        size = Size(w * 0.66f, h * 0.10f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    // Crimson wax seal with star
                    drawCircle(color = HeroCrimson, radius = w * 0.15f, center = Offset(cx, cy + h * 0.04f))
                    drawCircle(color = ChampionGold, radius = w * 0.08f, center = Offset(cx, cy + h * 0.04f))
                }

                MenuButtonArtType.SHOP -> {
                    // Royal Golden Treasure Chest overflowing with Sapphires & Gold
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))),
                        topLeft = Offset(w * 0.18f, h * 0.42f),
                        size = Size(w * 0.64f, h * 0.40f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Glowing gems peeking out
                    drawCircle(color = SapphireGlow, radius = w * 0.10f, center = Offset(w * 0.36f, h * 0.41f))
                    drawCircle(color = MythicGoldLight, radius = w * 0.11f, center = Offset(cx, h * 0.39f))
                    drawCircle(color = HeroCrimson, radius = w * 0.09f, center = Offset(w * 0.64f, h * 0.41f))
                    // Chest arched lid & gold bands
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(ChampionGold, ChampionGoldDark)),
                        topLeft = Offset(w * 0.16f, h * 0.22f),
                        size = Size(w * 0.68f, h * 0.17f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRect(color = ChampionGold, topLeft = Offset(w * 0.18f, h * 0.42f), size = Size(w * 0.64f, h * 0.06f))
                    drawRoundRect(
                        color = MythicGoldLight,
                        topLeft = Offset(w * 0.44f, h * 0.43f),
                        size = Size(w * 0.12f, h * 0.15f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }

                MenuButtonArtType.DAILY_CHALLENGE -> {
                    // Electrified Lightning Bolt & Golden Sunburst Medallion
                    drawCircle(
                        brush = Brush.radialGradient(listOf(MythicGoldLight, Color(0xFFD97706))),
                        radius = w * 0.34f,
                        center = Offset(cx, cy)
                    )
                    val bolt = Path().apply {
                        moveTo(w * 0.56f, h * 0.12f)
                        lineTo(w * 0.30f, h * 0.52f)
                        lineTo(w * 0.50f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.88f)
                        lineTo(w * 0.72f, h * 0.44f)
                        lineTo(w * 0.52f, h * 0.44f)
                        close()
                    }
                    drawPath(bolt, color = Color.White)
                    drawPath(bolt, color = SapphireGlow, style = Stroke(width = 2f))
                }

                MenuButtonArtType.PLAY_CROWN -> {
                    drawCrownShape(cx, cy + h * 0.06f, w * 0.64f, h * 0.44f)
                }

                MenuButtonArtType.SETTINGS -> {
                    // Golden Astral Gear & Sapphire Core
                    for (i in 0 until 8) {
                        val a = (i * 45.0) * Math.PI / 180.0
                        val gx = cx + cos(a).toFloat() * w * 0.27f
                        val gy = cy + sin(a).toFloat() * h * 0.27f
                        drawCircle(color = ChampionGold, radius = w * 0.08f, center = Offset(gx, gy))
                    }
                    drawCircle(color = ChampionGold, radius = w * 0.24f, center = Offset(cx, cy))
                    drawCircle(color = RoyalNavyDeep, radius = w * 0.14f, center = Offset(cx, cy))
                    drawCircle(color = SapphireGlow, radius = w * 0.08f, center = Offset(cx, cy))
                }

                MenuButtonArtType.ABOUT -> {
                    // Royal Crest Star & Sapphire Diamond
                    val diamond = Path().apply {
                        moveTo(cx, h * 0.14f)
                        lineTo(w * 0.82f, cy)
                        lineTo(cx, h * 0.86f)
                        lineTo(w * 0.18f, cy)
                        close()
                    }
                    drawPath(diamond, brush = Brush.verticalGradient(listOf(SapphireGlow, Color(0xFF0284C7))))
                    drawPath(diamond, color = ChampionGold, style = Stroke(width = 2.2f))
                    drawCircle(color = MythicGoldLight, radius = w * 0.10f, center = Offset(cx, cy))
                }
            }
        }
    }
}

/**
 * Illustrated Game Mode Banner Emblem for the Mode Selection Dialog.
 */
@Composable
fun GameModeArtworkBadge(
    mode: GameMode,
    accent: Color,
    modifier: Modifier = Modifier.size(48.dp)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.45f), Color(0xFF09122C))
                )
            )
            .border(1.8.dp, ChampionGold, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            when (mode) {
                GameMode.CAREER -> {
                    // Ascending golden stairs leading to a glowing temple portal arch
                    for (step in 0..2) {
                        val sw = w * (0.64f - step * 0.12f)
                        val sy = h * (0.76f - step * 0.12f)
                        drawRoundRect(
                            color = ChampionGold,
                            topLeft = Offset(cx - sw * 0.5f, sy),
                            size = Size(sw, h * 0.09f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                    drawRect(color = Color.White, topLeft = Offset(w * 0.28f, h * 0.22f), size = Size(w * 0.09f, h * 0.32f))
                    drawRect(color = Color.White, topLeft = Offset(w * 0.63f, h * 0.22f), size = Size(w * 0.09f, h * 0.32f))
                    drawRect(color = ChampionGold, topLeft = Offset(w * 0.24f, h * 0.16f), size = Size(w * 0.52f, h * 0.08f))
                    drawCircle(color = SapphireGlow, radius = w * 0.11f, center = Offset(cx, h * 0.37f))
                }
                GameMode.RIVAL_RACE -> {
                    // Twin racing crowns & checkered victory aura
                    drawCrownShape(cx - w * 0.12f, cy + h * 0.10f, w * 0.42f, h * 0.28f)
                    drawCrownShape(cx + w * 0.14f, cy - h * 0.04f, w * 0.46f, h * 0.32f)
                }
                GameMode.ENDLESS -> {
                    // Glowing infinity portal ring & floating islands
                    drawCircle(color = SapphireGlow, radius = w * 0.20f, center = Offset(w * 0.35f, cy), style = Stroke(width = 4.5f))
                    drawCircle(color = ChampionGold, radius = w * 0.20f, center = Offset(w * 0.65f, cy), style = Stroke(width = 4.5f))
                }
                GameMode.TIME_ATTACK -> {
                    // Golden Chronograph Stopwatch with Lightning Hands
                    drawCircle(color = ChampionGold, radius = w * 0.30f, center = Offset(cx, cy + h * 0.04f), style = Stroke(width = 3.5f))
                    drawCircle(color = Color(0xFF1E293B), radius = w * 0.26f, center = Offset(cx, cy + h * 0.04f))
                    drawLine(color = HeroCrimson, start = Offset(cx, cy + h * 0.04f), end = Offset(cx + w * 0.14f, cy - h * 0.10f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                    drawLine(color = SapphireGlow, start = Offset(cx, cy + h * 0.04f), end = Offset(cx, cy - h * 0.14f), strokeWidth = 3f, cap = StrokeCap.Round)
                    drawRect(color = ChampionGold, topLeft = Offset(cx - w * 0.08f, h * 0.12f), size = Size(w * 0.16f, h * 0.08f))
                }
                else -> {
                    // Winged Practice Shield & Star
                    drawCrownShape(cx, cy + h * 0.06f, w * 0.56f, h * 0.36f)
                }
            }
        }
    }
}

/**
 * Rich, bespoke illustrated Royal Quest Medallion for each of the 10 Mission types in MissionsScreen.
 */
@Composable
fun MissionEmblemArtwork(
    type: MissionType,
    ready: Boolean,
    modifier: Modifier = Modifier.size(58.dp)
) {
    val frameBorderBrush = if (ready) {
        Brush.verticalGradient(listOf(IslandMint, ChampionGold, IslandMint))
    } else {
        Brush.verticalGradient(listOf(MythicGoldLight, ChampionGold, ChampionGoldDark))
    }
    val bgTop = if (ready) Color(0xFF065F46) else Color(0xFF1E3A8A)
    val bgBottom = if (ready) Color(0xFF022C22) else Color(0xFF09122C)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
            .border(2.dp, frameBorderBrush, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            // Inner ornamental corner rivets
            val rivetCol = if (ready) IslandMint else ChampionGold.copy(alpha = 0.7f)
            for (rx in listOf(w * 0.13f, w * 0.87f)) {
                for (ry in listOf(h * 0.13f, h * 0.87f)) {
                    drawCircle(color = rivetCol, radius = 2.6f, center = Offset(rx, ry))
                }
            }

            // Radial aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (ready) IslandMint else SapphireGlow).copy(alpha = 0.38f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.44f
                ),
                radius = w * 0.44f
            )

            when (type) {
                MissionType.SPEED_90S -> {
                    // Winged Golden Chronograph Stopwatch with Lightning Bolt
                    val wingL = Path().apply {
                        moveTo(w * 0.24f, cy)
                        lineTo(w * 0.08f, cy - h * 0.18f)
                        lineTo(w * 0.12f, cy + h * 0.08f)
                        close()
                    }
                    val wingR = Path().apply {
                        moveTo(w * 0.76f, cy)
                        lineTo(w * 0.92f, cy - h * 0.18f)
                        lineTo(w * 0.88f, cy + h * 0.08f)
                        close()
                    }
                    drawPath(wingL, color = MythicGoldLight)
                    drawPath(wingR, color = MythicGoldLight)
                    drawCircle(color = ChampionGold, radius = w * 0.27f, center = Offset(cx, cy + h * 0.04f))
                    drawCircle(color = Color(0xFF0F172A), radius = w * 0.22f, center = Offset(cx, cy + h * 0.04f))
                    drawLine(color = SapphireGlow, start = Offset(cx, cy + h * 0.04f), end = Offset(cx + w * 0.12f, cy - h * 0.08f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                    drawLine(color = HeroCrimson, start = Offset(cx, cy + h * 0.04f), end = Offset(cx - w * 0.08f, cy + h * 0.04f), strokeWidth = 2.8f, cap = StrokeCap.Round)
                    drawRoundRect(color = ChampionGold, topLeft = Offset(cx - w * 0.08f, h * 0.14f), size = Size(w * 0.16f, h * 0.08f), cornerRadius = CornerRadius(3f, 3f))
                }

                MissionType.PERFECT_LANDINGS -> {
                    // Glowing Target Ring + Winged Golden Boot + Starburst
                    drawCircle(color = SapphireGlow, radius = w * 0.28f, center = Offset(cx, h * 0.68f), style = Stroke(width = 3f))
                    drawCircle(color = ChampionGold, radius = w * 0.16f, center = Offset(cx, h * 0.68f), style = Stroke(width = 2.5f))
                    // Golden winged boot descending
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(MythicGoldLight, ChampionGoldDark)),
                        topLeft = Offset(w * 0.36f, h * 0.22f),
                        size = Size(w * 0.20f, h * 0.34f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    drawRoundRect(
                        color = ChampionGold,
                        topLeft = Offset(w * 0.34f, h * 0.46f),
                        size = Size(w * 0.34f, h * 0.14f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                MissionType.NO_CHECKPOINT_WIN -> {
                    // Unbroken Royal Aegis Shield + Golden Crown + Diamond Heart
                    val shield = Path().apply {
                        moveTo(cx, h * 0.14f)
                        lineTo(w * 0.78f, h * 0.25f)
                        lineTo(w * 0.72f, h * 0.62f)
                        lineTo(cx, h * 0.86f)
                        lineTo(w * 0.28f, h * 0.62f)
                        lineTo(w * 0.22f, h * 0.25f)
                        close()
                    }
                    drawPath(shield, brush = Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF1E3A8A))))
                    drawPath(shield, color = ChampionGold, style = Stroke(width = 3f))
                    drawCrownShape(cx, h * 0.52f, w * 0.36f, h * 0.22f)
                }

                MissionType.FIND_SECRET_PATH -> {
                    // Glowing Arcane Portal Arch + Golden Skeleton Key
                    drawRoundRect(
                        color = ChampionGold,
                        topLeft = Offset(w * 0.24f, h * 0.18f),
                        size = Size(w * 0.52f, h * 0.64f),
                        cornerRadius = CornerRadius(24f, 24f),
                        style = Stroke(width = 4f)
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(SapphireGlow, Color(0xFF7209B7))),
                        topLeft = Offset(w * 0.30f, h * 0.24f),
                        size = Size(w * 0.40f, h * 0.54f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                    // Golden Key in center
                    drawCircle(color = MythicGoldLight, radius = w * 0.10f, center = Offset(cx, h * 0.38f), style = Stroke(width = 3.2f))
                    drawLine(color = MythicGoldLight, start = Offset(cx, h * 0.48f), end = Offset(cx, h * 0.70f), strokeWidth = 3.5f, cap = StrokeCap.Round)
                    drawLine(color = MythicGoldLight, start = Offset(cx, h * 0.62f), end = Offset(cx + w * 0.09f, h * 0.62f), strokeWidth = 3f, cap = StrokeCap.Round)
                }

                MissionType.CROSS_NARROW_BRIDGES -> {
                    // Twin Castle Towers connected by a glowing Golden Sky Bridge
                    drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(w * 0.14f, h * 0.34f), size = Size(w * 0.18f, h * 0.46f))
                    drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(w * 0.68f, h * 0.34f), size = Size(w * 0.18f, h * 0.46f))
                    drawTriangle(w * 0.23f, h * 0.18f, w * 0.12f, h * 0.34f, w * 0.34f, h * 0.34f, ChampionGold)
                    drawTriangle(w * 0.77f, h * 0.18f, w * 0.66f, h * 0.34f, w * 0.88f, h * 0.34f, ChampionGold)
                    // Golden bridge span
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(ChampionGold, SapphireGlow, ChampionGold)),
                        topLeft = Offset(w * 0.30f, h * 0.50f),
                        size = Size(w * 0.40f, h * 0.10f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                MissionType.NO_DASH_WIN -> {
                    // Pure Mastery Golden Laurel Medallion & Balanced Diamond Core
                    drawCircle(color = ChampionGold, radius = w * 0.28f, center = Offset(cx, cy), style = Stroke(width = 3.5f))
                    val diamond = Path().apply {
                        moveTo(cx, h * 0.24f)
                        lineTo(w * 0.72f, cy)
                        lineTo(cx, h * 0.76f)
                        lineTo(w * 0.28f, cy)
                        close()
                    }
                    drawPath(diamond, brush = Brush.verticalGradient(listOf(MythicGoldLight, ChampionGoldDark)))
                    drawCircle(color = SapphireGlow, radius = w * 0.09f, center = Offset(cx, cy))
                }

                MissionType.COMPLETE_STAGES -> {
                    // Royal Temple Finish Portal with 3 Golden Stars above
                    drawRect(color = Color.White, topLeft = Offset(w * 0.24f, h * 0.36f), size = Size(w * 0.10f, h * 0.44f))
                    drawRect(color = Color.White, topLeft = Offset(w * 0.66f, h * 0.36f), size = Size(w * 0.10f, h * 0.44f))
                    drawRoundRect(color = ChampionGold, topLeft = Offset(w * 0.20f, h * 0.28f), size = Size(w * 0.60f, h * 0.10f), cornerRadius = CornerRadius(4f, 4f))
                    drawRoundRect(color = SapphireGlow.copy(alpha = 0.75f), topLeft = Offset(w * 0.35f, h * 0.38f), size = Size(w * 0.30f, h * 0.40f), cornerRadius = CornerRadius(4f, 4f))
                    drawCircle(color = MythicGoldLight, radius = 4.5f, center = Offset(w * 0.32f, h * 0.18f))
                    drawCircle(color = MythicGoldLight, radius = 5.5f, center = Offset(cx, h * 0.14f))
                    drawCircle(color = MythicGoldLight, radius = 4.5f, center = Offset(w * 0.68f, h * 0.18f))
                }

                MissionType.COLLECT_COINS -> {
                    // Gleaming Royal Gold Coin Stack with Crown Emboss
                    drawCircle(color = ChampionGoldDark, radius = w * 0.24f, center = Offset(cx + 3f, cy + 4f))
                    drawCircle(
                        brush = Brush.radialGradient(listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)),
                        radius = w * 0.26f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(color = MythicGoldLight, radius = w * 0.21f, center = Offset(cx, cy), style = Stroke(width = 2f))
                    drawCrownShape(cx, cy + h * 0.06f, w * 0.26f, h * 0.16f)
                }

                MissionType.JUMP_COUNT -> {
                    // Golden Spring Launch Pad + Soaring Trajectory Arc + Star
                    drawRoundRect(
                        color = ChampionGold,
                        topLeft = Offset(w * 0.22f, h * 0.68f),
                        size = Size(w * 0.56f, h * 0.12f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    val arrow = Path().apply {
                        moveTo(cx, h * 0.16f)
                        lineTo(w * 0.74f, h * 0.42f)
                        lineTo(w * 0.59f, h * 0.42f)
                        lineTo(w * 0.59f, h * 0.64f)
                        lineTo(w * 0.41f, h * 0.64f)
                        lineTo(w * 0.41f, h * 0.42f)
                        lineTo(w * 0.26f, h * 0.42f)
                        close()
                    }
                    drawPath(arrow, brush = Brush.verticalGradient(listOf(SapphireGlow, ChampionGold)))
                }

                MissionType.WIN_RIVAL_RACE -> {
                    // Grand Royal Champion Trophy Cup with Sapphire Jewel
                    val cup = Path().apply {
                        moveTo(w * 0.26f, h * 0.20f)
                        lineTo(w * 0.74f, h * 0.20f)
                        lineTo(w * 0.64f, h * 0.54f)
                        lineTo(w * 0.36f, h * 0.54f)
                        close()
                    }
                    drawPath(cup, brush = Brush.verticalGradient(listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)))
                    drawRect(color = ChampionGold, topLeft = Offset(cx - w * 0.06f, h * 0.54f), size = Size(w * 0.12f, h * 0.18f))
                    drawRoundRect(color = ChampionGoldDark, topLeft = Offset(w * 0.28f, h * 0.70f), size = Size(w * 0.44f, h * 0.12f), cornerRadius = CornerRadius(4f, 4f))
                    drawCircle(color = SapphireGlow, radius = w * 0.07f, center = Offset(cx, h * 0.35f))
                }

                MissionType.OPEN_CHESTS -> {
                    // Royal Golden & Sapphire Treasure Vault Chest
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF7C2D12), Color(0xFF451A03))),
                        topLeft = Offset(w * 0.20f, h * 0.36f),
                        size = Size(w * 0.60f, h * 0.42f),
                        cornerRadius = CornerRadius(7f, 7f)
                    )
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(MythicGoldLight, ChampionGoldDark)),
                        topLeft = Offset(w * 0.18f, h * 0.24f),
                        size = Size(w * 0.64f, h * 0.18f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRect(color = ChampionGold, topLeft = Offset(w * 0.28f, h * 0.36f), size = Size(w * 0.07f, h * 0.42f))
                    drawRect(color = ChampionGold, topLeft = Offset(w * 0.65f, h * 0.36f), size = Size(w * 0.07f, h * 0.42f))
                    drawRoundRect(
                        color = SapphireGlow,
                        topLeft = Offset(cx - w * 0.07f, h * 0.38f),
                        size = Size(w * 0.14f, h * 0.16f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}

/**
 * Illustrated 3D Treasure Chest Tier Badge for Wooden, Metallic, Magic Rare, and Royal Event Boss chests.
 */
@Composable
fun ChestTierBadgeArtwork(
    tier: ChestTier,
    modifier: Modifier = Modifier
) {
    val (bodyTop, bodyBottom, lidTop, trimCol, gemCol) = when (tier) {
        ChestTier.WOODEN -> listOf(
            Color(0xFFB45309),
            Color(0xFF78350F),
            Color(0xFFD97706),
            ChampionGold,
            MythicGoldLight
        )
        ChestTier.METALLIC -> listOf(
            Color(0xFF94A3B8),
            Color(0xFF334155),
            Color(0xFFCBD5E1),
            ChampionGold,
            SapphireGlow
        )
        ChestTier.MAGIC_RARE -> listOf(
            Color(0xFF9D4EDD),
            Color(0xFF3C096C),
            Color(0xFF00B4D8),
            SapphireGlow,
            MythicGoldLight
        )
        ChestTier.EVENT_BOSS -> listOf(
            HeroCrimson,
            Color(0xFF7F1D1D),
            ChampionGold,
            MythicGoldLight,
            SapphireGlow
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // Soft aura behind chest
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(gemCol.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(cx, h * 0.52f),
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = Offset(cx, h * 0.52f)
        )

        // Chest lower box
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(bodyTop, bodyBottom)),
            topLeft = Offset(w * 0.14f, h * 0.42f),
            size = Size(w * 0.72f, h * 0.44f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = trimCol,
            topLeft = Offset(w * 0.14f, h * 0.42f),
            size = Size(w * 0.72f, h * 0.44f),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2f)
        )

        // Vertical straps
        drawRect(color = trimCol, topLeft = Offset(w * 0.25f, h * 0.42f), size = Size(w * 0.08f, h * 0.44f))
        drawRect(color = trimCol, topLeft = Offset(w * 0.67f, h * 0.42f), size = Size(w * 0.08f, h * 0.44f))

        // Chest vaulted lid
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(lidTop, bodyTop)),
            topLeft = Offset(w * 0.11f, h * 0.24f),
            size = Size(w * 0.78f, h * 0.22f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = trimCol,
            topLeft = Offset(w * 0.11f, h * 0.24f),
            size = Size(w * 0.78f, h * 0.22f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 2f)
        )

        // Center jewel lock clasp
        drawRoundRect(
            color = trimCol,
            topLeft = Offset(cx - w * 0.11f, h * 0.38f),
            size = Size(w * 0.22f, h * 0.20f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawCircle(
            color = gemCol,
            radius = w * 0.065f,
            center = Offset(cx, h * 0.48f)
        )

        // Crown topper for EVENT_BOSS or crystal spark for MAGIC_RARE
        if (tier == ChestTier.EVENT_BOSS) {
            drawCrownShape(cx, h * 0.24f, w * 0.40f, h * 0.18f)
        } else if (tier == ChestTier.MAGIC_RARE) {
            drawTriangle(cx, h * 0.06f, cx - w * 0.12f, h * 0.24f, cx + w * 0.12f, h * 0.24f, SapphireGlow)
        }
    }
}

/**
 * Miniature Panoramic Diorama Illustration for each of the 10 Worlds in LevelSelectScreen!
 */
@Composable
fun WorldDioramaBanner(
    worldId: Int,
    skyTop: Color,
    skyBottom: Color,
    primaryCol: Color,
    accentCol: Color,
    unlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sky gradient backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (unlocked) skyTop else Color(0xFF0F172A),
                    if (unlocked) skyBottom else Color(0xFF1E293B)
                )
            )
        )

        // Glowing sun / moon orb with atmospheric halo
        val sunCenter = Offset(w * 0.78f, h * 0.26f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(MythicGoldLight.copy(alpha = if (unlocked) 0.55f else 0.18f), Color.Transparent),
                center = sunCenter,
                radius = w * 0.32f
            ),
            radius = w * 0.32f,
            center = sunCenter
        )
        drawCircle(
            color = if (unlocked) MythicGoldLight else Color(0x5594A3B8),
            radius = h * 0.13f,
            center = sunCenter
        )

        // World-specific iconic 3D-style silhouette & architectural diorama
        when (worldId) {
            1 -> {
                // World 1: Ancient Greek Islands — Floating Island + Marble Parthenon + Twin Waterfalls
                drawFloatingIslandBase(w * 0.50f, h * 0.62f, w * 0.62f, h * 0.30f, Color(0xFF2EE58D), Color(0xFF64432E))
                for (i in 0..3) {
                    drawRect(color = Color.White, topLeft = Offset(w * (0.33f + i * 0.10f), h * 0.34f), size = Size(w * 0.05f, h * 0.24f))
                }
                drawTriangle(w * 0.50f, h * 0.18f, w * 0.28f, h * 0.34f, w * 0.72f, h * 0.34f, ChampionGold)
                drawRect(color = SapphireGlow, topLeft = Offset(w * 0.36f, h * 0.60f), size = Size(w * 0.07f, h * 0.36f))
                drawRect(color = SapphireGlow, topLeft = Offset(w * 0.58f, h * 0.60f), size = Size(w * 0.07f, h * 0.36f))
            }
            2 -> {
                // World 2: Floating Temples — Golden Sanctuaries & Levitating Halo Rings
                drawFloatingIslandBase(w * 0.50f, h * 0.66f, w * 0.65f, h * 0.26f, ChampionGold, Color(0xFF475569))
                drawCircle(color = ChampionGold, radius = h * 0.22f, center = Offset(w * 0.50f, h * 0.46f))
                drawCircle(color = SapphireGlow, radius = h * 0.28f, center = Offset(w * 0.50f, h * 0.42f), style = Stroke(width = 3f))
            }
            3 -> {
                // World 3: Zeus Thunder Arena — Twin Obsidian Storm Towers & Lightning Bolts
                drawRect(color = Color(0xFF1E293B), topLeft = Offset(w * 0.22f, h * 0.25f), size = Size(w * 0.14f, h * 0.55f))
                drawRect(color = Color(0xFF1E293B), topLeft = Offset(w * 0.64f, h * 0.25f), size = Size(w * 0.14f, h * 0.55f))
                drawCircle(color = SapphireGlow, radius = h * 0.09f, center = Offset(w * 0.29f, h * 0.22f))
                drawCircle(color = SapphireGlow, radius = h * 0.09f, center = Offset(w * 0.71f, h * 0.22f))
                drawLine(color = MythicGoldLight, start = Offset(w * 0.35f, h * 0.24f), end = Offset(w * 0.52f, h * 0.44f), strokeWidth = 3.5f)
                drawLine(color = SapphireGlow, start = Offset(w * 0.52f, h * 0.44f), end = Offset(w * 0.65f, h * 0.24f), strokeWidth = 3.5f)
            }
            4 -> {
                // World 4: Golden Bridges — Royal Suspension Arch Bridge & Sunset Clouds
                drawRect(color = ChampionGoldDark, topLeft = Offset(w * 0.20f, h * 0.26f), size = Size(w * 0.10f, h * 0.56f))
                drawRect(color = ChampionGoldDark, topLeft = Offset(w * 0.70f, h * 0.26f), size = Size(w * 0.10f, h * 0.56f))
                drawRect(color = ChampionGold, topLeft = Offset(w * 0.12f, h * 0.56f), size = Size(w * 0.76f, h * 0.10f))
                drawLine(color = MythicGoldLight, start = Offset(w * 0.25f, h * 0.28f), end = Offset(w * 0.50f, h * 0.56f), strokeWidth = 2.5f)
                drawLine(color = MythicGoldLight, start = Offset(w * 0.75f, h * 0.28f), end = Offset(w * 0.50f, h * 0.56f), strokeWidth = 2.5f)
            }
            5 -> {
                // World 5: Ancient Marble Platforms — Ice-Marble Rotunda & Frosted Spires
                drawTriangle(w * 0.25f, h * 0.22f, w * 0.14f, h * 0.72f, w * 0.36f, h * 0.72f, Color(0xFFBAE6FD))
                drawTriangle(w * 0.75f, h * 0.22f, w * 0.64f, h * 0.72f, w * 0.86f, h * 0.72f, Color(0xFFBAE6FD))
                drawRoundRect(color = Color.White, topLeft = Offset(w * 0.32f, h * 0.38f), size = Size(w * 0.36f, h * 0.34f), cornerRadius = CornerRadius(8f, 8f))
                drawTriangle(w * 0.50f, h * 0.18f, w * 0.28f, h * 0.38f, w * 0.72f, h * 0.38f, ChampionGold)
            }
            6 -> {
                // World 6: Magical Portals — Swirling Amethyst & Sapphire Arcane Ring
                drawCircle(color = Color(0xFF9D4EDD), radius = h * 0.30f, center = Offset(w * 0.50f, h * 0.48f), style = Stroke(width = 5f))
                drawCircle(color = SapphireGlow, radius = h * 0.21f, center = Offset(w * 0.50f, h * 0.48f))
                drawCircle(color = ChampionGold, radius = h * 0.10f, center = Offset(w * 0.50f, h * 0.48f))
            }
            7 -> {
                // World 7: Mythological Gardens — Colossal World-Tree Canopy & Waterfall
                drawFloatingIslandBase(w * 0.50f, h * 0.68f, w * 0.66f, h * 0.25f, Color(0xFF2EE58D), Color(0xFF5C3A21))
                drawRect(color = Color(0xFF5C3A21), topLeft = Offset(w * 0.44f, h * 0.38f), size = Size(w * 0.12f, h * 0.30f))
                drawCircle(color = Color(0xFF10B981), radius = h * 0.20f, center = Offset(w * 0.38f, h * 0.34f))
                drawCircle(color = Color(0xFF2EE58D), radius = h * 0.22f, center = Offset(w * 0.60f, h * 0.34f))
                drawCircle(color = MythicGoldLight, radius = h * 0.18f, center = Offset(w * 0.50f, h * 0.24f))
            }
            8 -> {
                // World 8: Crystal Caves — Faceted Giant Cyan & Magenta Gemstone Spires
                drawTriangle(w * 0.32f, h * 0.20f, w * 0.18f, h * 0.76f, w * 0.46f, h * 0.76f, SapphireGlow)
                drawTriangle(w * 0.68f, h * 0.24f, w * 0.54f, h * 0.76f, w * 0.82f, h * 0.76f, ChampionGold)
                drawTriangle(w * 0.50f, h * 0.12f, w * 0.34f, h * 0.76f, w * 0.66f, h * 0.76f, Color(0xFFF72585))
            }
            9 -> {
                // World 9: Floating Castles — Multi-Spire Royal Sky Citadel
                drawFloatingIslandBase(w * 0.50f, h * 0.68f, w * 0.68f, h * 0.25f, Color(0xFFE2E8F0), Color(0xFF334155))
                drawRect(color = Color.White, topLeft = Offset(w * 0.26f, h * 0.36f), size = Size(w * 0.12f, h * 0.32f))
                drawRect(color = Color.White, topLeft = Offset(w * 0.43f, h * 0.24f), size = Size(w * 0.14f, h * 0.44f))
                drawRect(color = Color.White, topLeft = Offset(w * 0.62f, h * 0.36f), size = Size(w * 0.12f, h * 0.32f))
                drawTriangle(w * 0.32f, h * 0.20f, w * 0.23f, h * 0.36f, w * 0.41f, h * 0.36f, ChampionGold)
                drawTriangle(w * 0.50f, h * 0.08f, w * 0.40f, h * 0.24f, w * 0.60f, h * 0.24f, ChampionGold)
                drawTriangle(w * 0.68f, h * 0.20f, w * 0.59f, h * 0.36f, w * 0.77f, h * 0.36f, ChampionGold)
            }
            else -> {
                // World 10: Ancient Ruins — Colossal Crowned Titan Guardian & Lava Glow
                drawRect(color = Color(0xFF334155), topLeft = Offset(w * 0.34f, h * 0.36f), size = Size(w * 0.32f, h * 0.42f))
                drawCircle(color = Color(0xFF1E293B), radius = h * 0.16f, center = Offset(w * 0.50f, h * 0.30f))
                drawCrownShape(w * 0.50f, h * 0.18f, w * 0.30f, h * 0.14f)
                drawCircle(color = Color(0xFFFF5400), radius = 3.5f, center = Offset(w * 0.45f, h * 0.30f))
                drawCircle(color = Color(0xFFFF5400), radius = 3.5f, center = Offset(w * 0.55f, h * 0.30f))
            }
        }

        // Foreground 3D Stepping Platform Path
        val platCol = if (unlocked) primaryCol else Color(0xFF475569)
        val trimCol = if (unlocked) accentCol else Color(0xFF64748B)
        drawRoundRect(
            color = trimCol,
            topLeft = Offset(w * 0.28f, h * 0.78f),
            size = Size(w * 0.44f, h * 0.16f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = platCol,
            topLeft = Offset(w * 0.31f, h * 0.80f),
            size = Size(w * 0.38f, h * 0.11f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

private fun DrawScope.drawFloatingIslandBase(cx: Float, topY: Float, width: Float, depthH: Float, topColor: Color, rockColor: Color) {
    val cone = Path().apply {
        moveTo(cx - width * 0.46f, topY + 4f)
        lineTo(cx + width * 0.46f, topY + 4f)
        lineTo(cx + width * 0.14f, topY + depthH)
        lineTo(cx - width * 0.14f, topY + depthH)
        close()
    }
    drawPath(cone, color = rockColor)
    drawRoundRect(
        color = topColor,
        topLeft = Offset(cx - width * 0.5f, topY),
        size = Size(width, 9f),
        cornerRadius = CornerRadius(5f, 5f)
    )
}

private fun DrawScope.drawTriangle(
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    color: Color
) {
    val p = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
        close()
    }
    drawPath(p, color = color)
}

private fun DrawScope.drawCrownShape(cx: Float, bottomY: Float, width: Float, height: Float) {
    val left = cx - width * 0.5f
    val right = cx + width * 0.5f
    val top = bottomY - height
    val crown = Path().apply {
        moveTo(left + width * 0.08f, bottomY)
        lineTo(left, top + height * 0.20f)
        lineTo(left + width * 0.28f, top + height * 0.52f)
        lineTo(cx, top)
        lineTo(right - width * 0.28f, top + height * 0.52f)
        lineTo(right, top + height * 0.20f)
        lineTo(right - width * 0.08f, bottomY)
        close()
    }
    drawPath(crown, brush = Brush.verticalGradient(listOf(MythicGoldLight, ChampionGold, ChampionGoldDark)))
    drawPath(crown, color = Color.White.copy(alpha = 0.7f), style = Stroke(width = 1.4f, join = StrokeJoin.Round))
    drawCircle(color = SapphireGlow, radius = width * 0.07f, center = Offset(cx, bottomY - height * 0.34f))
}

/**
 * High-Resolution 3D Rendered Full-Body Character Showcase (Head to Toe)
 * Uses actual 3D character render images (`img_char_ahmed_crimson`, `img_char_salma_royal`, `img_char_ahmed_sapphire`)
 * with interactive 360° perspective rotation and live equipped headgear/footwear/trail aura overlays.
 */
@Composable
fun FullBodyCharacterArtwork(
    skin: SkinDef,
    hatId: String = "hat_none",
    shoeId: String = "shoe_default",
    trailId: String = "trail_dust",
    rotationDeg: Float = 0f,
    modifier: Modifier = Modifier
) {
    val characterDrawableRes = when (skin.id) {
        "salma_heroine" -> R.drawable.img_char_salma_royal
        "kid_ahmed" -> R.drawable.img_char_ahmed_sapphire
        else -> R.drawable.img_char_ahmed_crimson
    }
    val accentCol = Color(skin.emblemColor.r, skin.emblemColor.g, skin.emblemColor.b)
    val trailAuraCol = when (trailId) {
        "trail_gold" -> ChampionGold
        "trail_cyan" -> SapphireGlow
        "trail_flame" -> Color(0xFFFF5400)
        "trail_rainbow" -> Color(0xFFF72585)
        else -> accentCol
    }
    val shoeBadgeCol = when (shoeId) {
        "shoe_gold" -> ChampionGold
        "shoe_rocket" -> SapphireGlow
        "shoe_shadow" -> Color(0xFF4D7C0F)
        else -> HeroCrimson
    }

    val rad = Math.toRadians(rotationDeg.toDouble())
    val turnSin = sin(rad).toFloat()
    val turnCos = cos(rad).toFloat()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(trailAuraCol.copy(alpha = 0.30f), Color(0xFF081026))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Real 3D Rendered Full-Body Character Image with 3D Y-axis rotation preview
        Image(
            painter = painterResource(id = characterDrawableRes),
            contentDescription = skin.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = turnSin * 24f
                    scaleX = if (turnCos < -0.25f) -0.96f else 0.98f
                    scaleY = 0.98f
                }
        )

        // Live 3D Aura Pedestal, Equipped Headgear Crest & Footwear Trim Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // Glowing Trail Pedestal Ring at Character's Feet
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(trailAuraCol.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(cx, h * 0.93f),
                    radius = w * 0.45f
                ),
                topLeft = Offset(cx - w * 0.42f, h * 0.86f),
                size = Size(w * 0.84f, h * 0.12f)
            )
            drawOval(
                color = trailAuraCol,
                topLeft = Offset(cx - w * 0.34f, h * 0.89f),
                size = Size(w * 0.68f, h * 0.07f),
                style = Stroke(width = 2.2f)
            )

            // Equipped Hat / Visor / Halo / Crown Accent at top of character frame
            when (hatId) {
                "hat_crown" -> {
                    drawCrownShape(cx + turnSin * w * 0.03f, h * 0.12f, w * 0.24f, h * 0.08f)
                }
                "hat_halo" -> {
                    drawOval(
                        color = MythicGoldLight,
                        topLeft = Offset(cx - w * 0.16f + turnSin * w * 0.03f, h * 0.03f),
                        size = Size(w * 0.32f, h * 0.055f),
                        style = Stroke(width = 3.5f)
                    )
                }
                "hat_visor" -> {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(SapphireGlow, Color.White, SapphireGlow)),
                        topLeft = Offset(cx - w * 0.11f + turnSin * w * 0.03f, h * 0.13f),
                        size = Size(w * 0.22f, h * 0.028f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                "hat_headband" -> {
                    drawRoundRect(
                        color = HeroCrimson,
                        topLeft = Offset(cx - w * 0.12f + turnSin * w * 0.03f, h * 0.10f),
                        size = Size(w * 0.24f, h * 0.025f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // Equipped Shoe Color Rim Glow at Sneakers
            if (shoeId != "shoe_default") {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(shoeBadgeCol.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(cx - w * 0.14f, h * 0.89f),
                        radius = w * 0.14f
                    ),
                    radius = w * 0.14f,
                    center = Offset(cx - w * 0.14f, h * 0.89f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(shoeBadgeCol.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(cx + w * 0.14f, h * 0.89f),
                        radius = w * 0.14f
                    ),
                    radius = w * 0.14f,
                    center = Offset(cx + w * 0.14f, h * 0.89f)
                )
            }
        }
    }
}

/**
 * 3D Rendered Clothing & Gear Thumbnail for Shop & Wardrobe Items.
 */
@Composable
fun ShopItem3DArtwork(
    item: ShopItemDef,
    modifier: Modifier = Modifier.size(56.dp)
) {
    val accent = Color(item.previewColor.r, item.previewColor.g, item.previewColor.b)
    val bannerRes = when (item.category) {
        ShopCategory.HATS -> R.drawable.img_char_ahmed_crimson
        ShopCategory.SHOES -> R.drawable.img_outfit_tracksuits_3d
        ShopCategory.TRAILS -> R.drawable.img_event_mythoria_arena
        ShopCategory.VICTORY -> R.drawable.img_char_salma_royal
        ShopCategory.ABILITIES -> R.drawable.img_char_ahmed_sapphire
        ShopCategory.CHARACTERS -> R.drawable.img_char_ahmed_crimson
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A1433))
            .border(1.8.dp, accent, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = bannerRes),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = 0.42f))
                    )
                )
        )
    }
}


