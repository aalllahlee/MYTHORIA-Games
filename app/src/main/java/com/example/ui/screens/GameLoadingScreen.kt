package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Mythoria Loading Screen matching the official reference image:
 * - Ancient Greek Sky Kingdom & Temple Portal backdrop
 * - MYTHORIA games — By ahmed crest with 5-peak golden crown & wings
 * - Glowing sapphire blue progress bar inside an ornate golden frame ("Loading... XX%")
 * - Real progression connected to resource/catalog initialization
 */
@Composable
fun GameLoadingScreen(
    progress: Float
) {
    val pct = (progress * 100f).toInt().coerceIn(0, 100)

    val infinite = rememberInfiniteTransition(label = "loading_pulse")
    val glowSweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "glow_sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalNavyDeep)
            .testTag("game_loading_screen")
    ) {
        // Official Mythoria Reference Scene Art Backdrop
        Image(
            painter = painterResource(id = R.drawable.mythoria_splash_art),
            contentDescription = "MYTHORIA games Loading Backdrop",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.76f)
        )

        // Subtle atmospheric gradient overlay for crisp text legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x99050B1E),
                            Color(0x44081434),
                            Color(0xCC04091A)
                        )
                    )
                )
        )

        // Top-Left Game Title & Tagline Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xD90A1432),
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(start = 20.dp, top = 14.dp)
                .border(1.5.dp, ChampionGold, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SapphireGlow, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DON'T FALL!",
                        fontFamily = FredokaFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ChampionGold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Mythological Sky Obstacle Adventure",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Right-side Feature Pills
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .safeDrawingPadding()
                .padding(end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeaturePill(Icons.Filled.Public, "10 Mythic", "Sky Worlds", ChampionGold)
            FeaturePill(Icons.Filled.EmojiEvents, "50 Stages", "Progressive Tracks", SapphireGlow)
            FeaturePill(Icons.Filled.Shield, "3 Heroes", "Ahmed • Salma • Ahmed", CandyMagenta)
            FeaturePill(Icons.Filled.WifiOff, "Play Offline", "Zero Internet Needed", IslandMint)
        }

        // Centerpiece: Official MYTHORIA games — By ahmed Emblem
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MythoriaLogoEmblem(
                sweepProgress = glowSweep,
                timeSec = 5.0f,
                compact = true
            )
        }

        // Bottom Ornate Golden & Sapphire Loading Bar (matching reference "Loading... 85%")
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(440.dp)
                    .height(36.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xEE080F26))
                    .border(
                        width = 2.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(ChampionGoldDark, MythicGoldLight, ChampionGold, MythicGoldLight, ChampionGoldDark)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0.08f, 1f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    SapphireElectric,
                                    SapphireGlow,
                                    Color(0xFF90E0EF)
                                )
                            )
                        )
                )
                Text(
                    text = "Loading... $pct%",
                    fontFamily = FredokaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "★  MYTHORIA games  •  By ahmed  ★",
                fontFamily = FredokaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = ChampionGold
            )
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, title: String, subtitle: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xD90A1432),
        modifier = Modifier
            .width(178.dp)
            .border(1.5.dp, ChampionGold.copy(alpha = 0.75f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Text(subtitle, fontSize = 10.sp, color = TextSecondary)
            }
        }
    }
}
