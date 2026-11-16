package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class MythoriaParticle(
    val xFrac: Float,
    val yFrac: Float,
    val radiusDp: Float,
    val speedY: Float,
    val driftX: Float,
    val phase: Float,
    val isSapphire: Boolean
)

/**
 * 8-Second AAA Mythological Studio Intro for:
 * MYTHORIA games
 * By ahmed
 *
 * Timeline:
 * 0–2s: Dark blue starry cinematic atmosphere, golden & sapphire particles appearing, subtle magical intro sound.
 * 2–4s: Ancient Greek sky temple, giant marble columns, Zeus lightning flashes, luminous moon, and royal 5-peak golden crown appearing.
 * 4–6s: Symmetrical golden angelic wings spread open; metallic 3D "MYTHORIA" logo appears with a golden light sweep and glowing blue diamond gemstones.
 * 6–8s: "games" banner and "By ahmed" ornamental golden plaque appear with twin fire braziers and portal glow; complete logo shines.
 * 8.0s: Smooth fade transition into the DON'T FALL loading screen and main menu.
 */
@Composable
fun StudioSplashScreen(
    elapsedSec: Float
) {
    val particles = remember {
        val rng = Random(2026)
        List(68) { idx ->
            MythoriaParticle(
                xFrac = rng.nextFloat(),
                yFrac = rng.nextFloat(),
                radiusDp = 1.6f + rng.nextFloat() * 3.2f,
                speedY = 0.025f + rng.nextFloat() * 0.055f,
                driftX = (rng.nextFloat() - 0.5f) * 0.03f,
                phase = rng.nextFloat() * 6.28f,
                isSapphire = idx % 3 == 0
            )
        }
    }

    // Smooth timeline curves
    val particleAlpha = (elapsedSec / 1.4f).coerceIn(0f, 1f)
    val templeBgAlpha = ((elapsedSec - 1.5f) / 2.2f).coerceIn(0f, 1f)
    val crownReveal = ((elapsedSec - 2.0f) / 1.6f).coerceIn(0f, 1f)
    val wingsSpread = ((elapsedSec - 3.6f) / 1.6f).coerceIn(0f, 1f)
    val mythoriaAlpha = ((elapsedSec - 3.8f) / 1.5f).coerceIn(0f, 1f)
    val sweepProgress = ((elapsedSec - 4.0f) / 1.9f).coerceIn(0f, 1f)
    val gamesSubAlpha = ((elapsedSec - 5.6f) / 1.1f).coerceIn(0f, 1f)
    val byAhmedAlpha = ((elapsedSec - 6.0f) / 1.2f).coerceIn(0f, 1f)
    val endFadeOut = if (elapsedSec > 7.35f) ((8.0f - elapsedSec) / 0.65f).coerceIn(0f, 1f) else 1f

    val cameraScale = 0.94f + 0.08f * ((elapsedSec / 8.0f).coerceIn(0f, 1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040816))
            .testTag("studio_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Official Mythoria Reference Scene Backdrop (fades in during 2-4s with cinematic zoom)
        Image(
            painter = painterResource(id = R.drawable.mythoria_splash_art),
            contentDescription = "MYTHORIA games Official Temple Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(cameraScale)
                .alpha(templeBgAlpha * 0.68f * endFadeOut)
        )

        // Layer 2: Procedural 3D-styled Sky Temple, Moon, Lightning, Braziers & Particles Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(endFadeOut)
        ) {
            val w = size.width
            val h = size.height

            // Deep night sky radial vignette
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xD904091A),
                        Color(0x990A1940),
                        Color(0xCC050B1E)
                    )
                )
            )

            // 2-4s+: Huge Luminous Blue Moon on Upper Right & Sapphire Portal Glow in Center
            if (templeBgAlpha > 0.01f) {
                val moonCenter = Offset(w * 0.76f, h * 0.22f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xAA90E0EF).copy(alpha = templeBgAlpha * 0.55f),
                            Color(0x440077FF).copy(alpha = templeBgAlpha * 0.25f),
                            Color.Transparent
                        ),
                        center = moonCenter,
                        radius = h * 0.28f
                    ),
                    radius = h * 0.28f,
                    center = moonCenter
                )

                // Central Temple Sapphire Portal Glow
                val portalPulse = 0.85f + 0.15f * sin(elapsedSec * 3.4f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SapphireGlow.copy(alpha = templeBgAlpha * 0.48f * portalPulse),
                            SapphireElectric.copy(alpha = templeBgAlpha * 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.34f),
                        radius = h * 0.42f
                    ),
                    radius = h * 0.42f,
                    center = Offset(w * 0.5f, h * 0.34f)
                )

                // Zeus Lightning Bolt Flash on Upper Left (peaks during 2.2s - 3.8s and 5.8s - 6.4s)
                val flashIntensity = when {
                    elapsedSec in 2.2f..2.6f -> sin((elapsedSec - 2.2f) / 0.4f * Math.PI.toFloat())
                    elapsedSec in 3.1f..3.45f -> sin((elapsedSec - 3.1f) / 0.35f * Math.PI.toFloat()) * 0.8f
                    elapsedSec in 6.0f..6.35f -> sin((elapsedSec - 6.0f) / 0.35f * Math.PI.toFloat()) * 0.7f
                    else -> 0f
                }
                if (flashIntensity > 0.02f) {
                    val boltPath = Path().apply {
                        moveTo(w * 0.14f, h * 0.04f)
                        lineTo(w * 0.12f, h * 0.18f)
                        lineTo(w * 0.15f, h * 0.19f)
                        lineTo(w * 0.10f, h * 0.36f)
                        lineTo(w * 0.13f, h * 0.37f)
                        lineTo(w * 0.09f, h * 0.52f)
                    }
                    drawPath(
                        path = boltPath,
                        color = SapphireGlow.copy(alpha = flashIntensity * 0.9f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = boltPath,
                        color = Color.White.copy(alpha = flashIntensity),
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Twin Foreground Golden Fire Braziers (Left & Right)
                drawGoldenFireBrazier(
                    center = Offset(w * 0.11f, h * 0.76f),
                    scale = h / 420f,
                    timeSec = elapsedSec,
                    alpha = templeBgAlpha
                )
                drawGoldenFireBrazier(
                    center = Offset(w * 0.89f, h * 0.76f),
                    scale = h / 420f,
                    timeSec = elapsedSec + 1.3f,
                    alpha = templeBgAlpha
                )
            }

            // 0-8s: Floating Golden & Sapphire Particles
            particles.forEach { p ->
                val yNow = ((p.yFrac - elapsedSec * p.speedY) % 1f + 1f) % 1f
                val xNow = ((p.xFrac + sin(elapsedSec * 1.3f + p.phase) * p.driftX) % 1f + 1f) % 1f
                val twinkle = (0.4f + 0.6f * sin(elapsedSec * 3.6f + p.phase)).coerceIn(0.15f, 1f)
                val alpha = (particleAlpha * twinkle).coerceIn(0f, 1f)
                val center = Offset(xNow * w, yNow * h)
                val rPx = p.radiusDp.dp.toPx()
                val coreColor = if (p.isSapphire) SapphireGlow else ChampionGold

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = alpha),
                            coreColor.copy(alpha = alpha * 0.8f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = rPx * 2.8f
                    ),
                    radius = rPx * 2.8f,
                    center = center
                )
            }

            // 4-6s: Diagonal Golden Light Sweep across the MYTHORIA Crest
            if (elapsedSec in 3.8f..6.4f) {
                val sweepX = (-0.15f + sweepProgress * 1.3f) * w
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            SapphireGlow.copy(alpha = 0.16f),
                            MythicGoldLight.copy(alpha = 0.42f),
                            SapphireGlow.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset(sweepX - w * 0.16f, h * 0.15f),
                        end = Offset(sweepX + w * 0.16f, h * 0.85f)
                    )
                )
            }
        }

        // Layer 3: Reusable Animated MYTHORIA games — By ahmed Emblem
        MythoriaLogoEmblem(
            crownReveal = crownReveal,
            wingsSpread = wingsSpread,
            mythoriaAlpha = mythoriaAlpha,
            gamesSubAlpha = gamesSubAlpha,
            byAhmedAlpha = byAhmedAlpha,
            sweepProgress = sweepProgress,
            timeSec = elapsedSec,
            modifier = Modifier
                .scale(cameraScale)
                .alpha(endFadeOut)
        )
    }
}

/**
 * Reusable Official MYTHORIA games — By ahmed Logo Emblem Component.
 * Reproduces the 5-peak sapphire-studded golden crown, symmetrical angelic golden wings,
 * 3D metallic "MYTHORIA" typography with a blue diamond inside the "O",
 * the "— games —" stone banner, and the ornamental golden "By ahmed" plaque with side gemstones.
 */
@Composable
fun MythoriaLogoEmblem(
    modifier: Modifier = Modifier,
    crownReveal: Float = 1f,
    wingsSpread: Float = 1f,
    mythoriaAlpha: Float = 1f,
    gamesSubAlpha: Float = 1f,
    byAhmedAlpha: Float = 1f,
    sweepProgress: Float = 0.5f,
    timeSec: Float = 4.5f,
    compact: Boolean = false
) {
    val scaleFactor = if (compact) 0.68f else 1.0f
    val shimmerOffset = (sweepProgress * 700f) + sin(timeSec * 2.4f) * 60f

    val goldMetallicBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF9D2),
            Color(0xFFFFD22B),
            Color(0xFFD97706),
            Color(0xFFFFF3B0),
            Color(0xFFB45309)
        ),
        start = Offset(shimmerOffset - 220f, 0f),
        end = Offset(shimmerOffset + 220f, 180f)
    )

    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background Canvas for Wings, 5-Peak Crown, Sapphires, and Ornamental Plaques
        Canvas(
            modifier = Modifier
                .width((540 * scaleFactor).dp)
                .height((290 * scaleFactor).dp)
        ) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.46f

            // 1. Symmetrical Golden Angelic Wings (Left & Right)
            if (wingsSpread > 0.01f) {
                drawAngelWings(
                    cx = cx,
                    cy = cy - size.height * 0.08f,
                    wingSpan = size.width * 0.46f * wingsSpread,
                    wingHeight = size.height * 0.34f,
                    alpha = wingsSpread
                )
            }

            // 2. Royal 5-Peak Golden Crown with Blue Sapphire Diamonds
            if (crownReveal > 0.01f) {
                drawRoyalMythoriaCrown(
                    cx = cx,
                    crownBottomY = cy - size.height * 0.11f,
                    crownWidth = size.width * 0.30f * (0.85f + 0.15f * crownReveal),
                    crownHeight = size.height * 0.25f * crownReveal,
                    alpha = crownReveal,
                    timeSec = timeSec
                )
            }

            // 3. Dark Ancient Stone Backing Shield behind MYTHORIA & "games"
            if (mythoriaAlpha > 0.01f) {
                val shieldPath = Path().apply {
                    moveTo(cx - size.width * 0.42f, cy - size.height * 0.10f)
                    lineTo(cx + size.width * 0.42f, cy - size.height * 0.10f)
                    lineTo(cx + size.width * 0.46f, cy + size.height * 0.12f)
                    lineTo(cx + size.width * 0.28f, cy + size.height * 0.23f)
                    lineTo(cx - size.width * 0.28f, cy + size.height * 0.23f)
                    lineTo(cx - size.width * 0.46f, cy + size.height * 0.12f)
                    close()
                }
                drawPath(
                    path = shieldPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE1E293B).copy(alpha = mythoriaAlpha),
                            Color(0xF5090F24).copy(alpha = mythoriaAlpha)
                        )
                    )
                )
                drawPath(
                    path = shieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MythicGoldLight.copy(alpha = mythoriaAlpha),
                            ChampionGold.copy(alpha = mythoriaAlpha),
                            ChampionGoldDark.copy(alpha = mythoriaAlpha)
                        )
                    ),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Foreground Typography Stack (MYTHORIA + games + By ahmed)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = (44 * scaleFactor).dp)
        ) {
            // "MYTHORIA" with glowing blue diamond inside the "O"
            Box(
                modifier = Modifier.alpha(mythoriaAlpha),
                contentAlignment = Alignment.Center
            ) {
                // Deep 3D bronze/navy extrusion shadow
                Text(
                    text = "MYTHORIA",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = (54 * scaleFactor).sp,
                        color = Color(0xFF2A1205),
                        letterSpacing = (2.5 * scaleFactor).sp,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(0f, 10f),
                            blurRadius = 18f
                        )
                    ),
                    modifier = Modifier.offset(y = (3.5 * scaleFactor).dp)
                )
                // Metallic Gold Primary Face
                Text(
                    text = "MYTHORIA",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Black,
                        fontSize = (54 * scaleFactor).sp,
                        brush = goldMetallicBrush,
                        letterSpacing = (2.5 * scaleFactor).sp,
                        shadow = Shadow(
                            color = Color(0xAAFFB703),
                            offset = Offset(0f, 2f),
                            blurRadius = 12f
                        )
                    )
                )
                // Faceted Blue Sapphire Gemstone inside the letter "O" of MYTHORIA
                Canvas(
                    modifier = Modifier
                        .size((20 * scaleFactor).dp)
                        .offset(x = (17 * scaleFactor).dp, y = (1 * scaleFactor).dp)
                ) {
                    drawFacetedDiamond(
                        center = Offset(size.width / 2f, size.height / 2f),
                        rx = size.width * 0.42f,
                        ry = size.height * 0.48f,
                        outerColor = SapphireElectric,
                        innerColor = SapphireGlow,
                        alpha = mythoriaAlpha
                    )
                }
            }

            // "— games —" Banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .offset(y = (-6 * scaleFactor).dp)
                    .alpha(gamesSubAlpha)
            ) {
                Box(
                    modifier = Modifier
                        .width((46 * scaleFactor).dp)
                        .height((2.5 * scaleFactor).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, ChampionGold, MythicGoldLight)
                            )
                        )
                )
                Spacer(Modifier.width((10 * scaleFactor).dp))
                Text(
                    text = "games",
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = (24 * scaleFactor).sp,
                        brush = goldMetallicBrush,
                        letterSpacing = (3 * scaleFactor).sp,
                        shadow = Shadow(color = Color.Black, offset = Offset(0f, 4f), blurRadius = 8f)
                    )
                )
                Spacer(Modifier.width((10 * scaleFactor).dp))
                Box(
                    modifier = Modifier
                        .width((46 * scaleFactor).dp)
                        .height((2.5 * scaleFactor).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MythicGoldLight, ChampionGold, Color.Transparent)
                            )
                        )
                )
            }

            Spacer(Modifier.height((2 * scaleFactor).dp))

            // Ornamental Golden Plaque reading "By ahmed" with Amethyst/Sapphire side gems
            Surface(
                shape = RoundedCornerShape((18 * scaleFactor).dp),
                color = Color(0xEE0D152C),
                modifier = Modifier
                    .alpha(byAhmedAlpha)
                    .shadow((10 * scaleFactor).dp, RoundedCornerShape((18 * scaleFactor).dp))
                    .border(
                        width = (2 * scaleFactor).dp,
                        brush = Brush.horizontalGradient(
                            listOf(ChampionGoldDark, MythicGoldLight, ChampionGold, MythicGoldLight, ChampionGoldDark)
                        ),
                        shape = RoundedCornerShape((18 * scaleFactor).dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = (20 * scaleFactor).dp,
                        vertical = (5 * scaleFactor).dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((10 * scaleFactor).dp)
                ) {
                    GemstoneBadge(scaleFactor = scaleFactor, isAmethyst = true)
                    Text(
                        text = "By ahmed",
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            fontSize = (20 * scaleFactor).sp,
                            brush = goldMetallicBrush,
                            letterSpacing = (1.2 * scaleFactor).sp,
                            shadow = Shadow(
                                color = Color(0xAAFFB703),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )
                    GemstoneBadge(scaleFactor = scaleFactor, isAmethyst = true)
                }
            }
        }
    }
}

@Composable
private fun GemstoneBadge(scaleFactor: Float, isAmethyst: Boolean) {
    Canvas(modifier = Modifier.size((14 * scaleFactor).dp)) {
        drawFacetedDiamond(
            center = Offset(size.width / 2f, size.height / 2f),
            rx = size.width * 0.45f,
            ry = size.height * 0.45f,
            outerColor = if (isAmethyst) Color(0xFF7209B7) else SapphireElectric,
            innerColor = if (isAmethyst) Color(0xFFE0AAFF) else SapphireGlow,
            alpha = 1f
        )
    }
}

private fun DrawScope.drawAngelWings(
    cx: Float,
    cy: Float,
    wingSpan: Float,
    wingHeight: Float,
    alpha: Float
) {
    for (dir in listOf(-1f, 1f)) {
        // 4 layered golden feathers per side
        for (layer in 0..3) {
            val frac = layer / 3f
            val reachX = cx + dir * wingSpan * (1.0f - frac * 0.16f)
            val tipY = cy - wingHeight * (0.55f - frac * 0.22f)
            val baseY = cy + wingHeight * (0.05f + frac * 0.08f)

            val featherPath = Path().apply {
                moveTo(cx + dir * wingSpan * 0.18f, baseY)
                quadraticTo(
                    cx + dir * wingSpan * 0.62f,
                    tipY - wingHeight * 0.12f,
                    reachX,
                    tipY
                )
                quadraticTo(
                    cx + dir * wingSpan * 0.68f,
                    baseY + wingHeight * 0.12f,
                    cx + dir * wingSpan * 0.20f,
                    baseY + wingHeight * 0.10f
                )
                close()
            }
            drawPath(
                path = featherPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MythicGoldLight.copy(alpha = alpha),
                        ChampionGold.copy(alpha = alpha),
                        ChampionGoldDark.copy(alpha = alpha * 0.9f)
                    ),
                    start = Offset(cx, tipY),
                    end = Offset(reachX, baseY)
                )
            )
            drawPath(
                path = featherPath,
                color = Color(0xFF78350F).copy(alpha = alpha * 0.7f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawRoyalMythoriaCrown(
    cx: Float,
    crownBottomY: Float,
    crownWidth: Float,
    crownHeight: Float,
    alpha: Float,
    timeSec: Float
) {
    val left = cx - crownWidth * 0.5f
    val right = cx + crownWidth * 0.5f
    val topCenterY = crownBottomY - crownHeight

    val crownPath = Path().apply {
        moveTo(left + crownWidth * 0.08f, crownBottomY)
        lineTo(left, crownBottomY - crownHeight * 0.62f)
        lineTo(left + crownWidth * 0.20f, crownBottomY - crownHeight * 0.42f)
        lineTo(left + crownWidth * 0.28f, crownBottomY - crownHeight * 0.82f)
        lineTo(left + crownWidth * 0.40f, crownBottomY - crownHeight * 0.50f)
        lineTo(cx, topCenterY)
        lineTo(right - crownWidth * 0.40f, crownBottomY - crownHeight * 0.50f)
        lineTo(right - crownWidth * 0.28f, crownBottomY - crownHeight * 0.82f)
        lineTo(right - crownWidth * 0.20f, crownBottomY - crownHeight * 0.42f)
        lineTo(right, crownBottomY - crownHeight * 0.62f)
        lineTo(right - crownWidth * 0.08f, crownBottomY)
        close()
    }

    drawPath(
        path = crownPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                MythicGoldLight.copy(alpha = alpha),
                ChampionGold.copy(alpha = alpha),
                ChampionGoldDark.copy(alpha = alpha)
            ),
            startY = topCenterY,
            endY = crownBottomY
        )
    )
    drawPath(
        path = crownPath,
        color = Color.White.copy(alpha = alpha * 0.85f),
        style = Stroke(width = 2.dp.toPx())
    )

    // Crown Base Rim
    drawRoundRect(
        color = ChampionGoldDark.copy(alpha = alpha),
        topLeft = Offset(left + crownWidth * 0.06f, crownBottomY - crownHeight * 0.16f),
        size = Size(crownWidth * 0.88f, crownHeight * 0.16f),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    // Central + Side Blue Sapphire Diamonds on the Crown
    val gemPulse = 0.85f + 0.15f * sin(timeSec * 4f)
    drawFacetedDiamond(
        center = Offset(cx, crownBottomY - crownHeight * 0.36f),
        rx = crownWidth * 0.065f * gemPulse,
        ry = crownHeight * 0.19f * gemPulse,
        outerColor = SapphireElectric,
        innerColor = SapphireGlow,
        alpha = alpha
    )
    drawFacetedDiamond(
        center = Offset(cx - crownWidth * 0.22f, crownBottomY - crownHeight * 0.32f),
        rx = crownWidth * 0.045f,
        ry = crownHeight * 0.14f,
        outerColor = SapphireElectric,
        innerColor = SapphireGlow,
        alpha = alpha
    )
    drawFacetedDiamond(
        center = Offset(cx + crownWidth * 0.22f, crownBottomY - crownHeight * 0.32f),
        rx = crownWidth * 0.045f,
        ry = crownHeight * 0.14f,
        outerColor = SapphireElectric,
        innerColor = SapphireGlow,
        alpha = alpha
    )
}

private fun DrawScope.drawFacetedDiamond(
    center: Offset,
    rx: Float,
    ry: Float,
    outerColor: Color,
    innerColor: Color,
    alpha: Float
) {
    val outer = Path().apply {
        moveTo(center.x, center.y - ry)
        lineTo(center.x + rx, center.y)
        lineTo(center.x, center.y + ry)
        lineTo(center.x - rx, center.y)
        close()
    }
    drawPath(outer, color = outerColor.copy(alpha = alpha))
    drawPath(outer, color = MythicGoldLight.copy(alpha = alpha * 0.9f), style = Stroke(width = 1.5.dp.toPx()))

    val inner = Path().apply {
        moveTo(center.x, center.y - ry * 0.55f)
        lineTo(center.x + rx * 0.55f, center.y)
        lineTo(center.x, center.y + ry * 0.55f)
        lineTo(center.x - rx * 0.55f, center.y)
        close()
    }
    drawPath(inner, color = innerColor.copy(alpha = alpha))
}

private fun DrawScope.drawGoldenFireBrazier(
    center: Offset,
    scale: Float,
    timeSec: Float,
    alpha: Float
) {
    val bowlW = 46f * scale
    val bowlH = 18f * scale
    // Pedestal base
    drawRoundRect(
        color = Color(0xFF475569).copy(alpha = alpha),
        topLeft = Offset(center.x - bowlW * 0.42f, center.y + bowlH * 0.5f),
        size = Size(bowlW * 0.84f, bowlH * 1.4f),
        cornerRadius = CornerRadius(4f * scale, 4f * scale)
    )
    // Golden bowl
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(ChampionGold.copy(alpha = alpha), ChampionGoldDark.copy(alpha = alpha))
        ),
        topLeft = Offset(center.x - bowlW * 0.5f, center.y - bowlH * 0.3f),
        size = Size(bowlW, bowlH),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
    // Flickering Flame
    val flicker = sin(timeSec * 9f) * 4f * scale
    val flameH = (36f + sin(timeSec * 7f) * 6f) * scale
    val flamePath = Path().apply {
        moveTo(center.x - bowlW * 0.34f, center.y - bowlH * 0.25f)
        quadraticTo(
            center.x - bowlW * 0.25f,
            center.y - flameH * 0.6f,
            center.x + flicker,
            center.y - flameH
        )
        quadraticTo(
            center.x + bowlW * 0.25f,
            center.y - flameH * 0.6f,
            center.x + bowlW * 0.34f,
            center.y - bowlH * 0.25f
        )
        close()
    }
    drawPath(
        path = flamePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                MythicGoldLight.copy(alpha = alpha),
                Color(0xFFFF9F1C).copy(alpha = alpha * 0.9f),
                Color(0xFFFF4800).copy(alpha = alpha * 0.8f)
            )
        )
    )
}
