package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.data.db.PlayerProfileEntity
import com.example.game.model.GameCatalog
import com.example.game.model.LocalizedStrings
import com.example.ui.theme.*

@Composable
fun CharacterSelectScreen(
    profile: PlayerProfileEntity,
    ownedIds: Set<String>,
    strings: LocalizedStrings,
    onSelectOrBuySkin: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, Color(0xFF14275E))))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("character_select_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(SurfaceCardDark)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("${strings.characters} (${GameCatalog.skins.size})", fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
                }
                Surface(shape = RoundedCornerShape(50), color = SurfaceCardDark, modifier = Modifier.border(1.5.dp, ChampionGold, RoundedCornerShape(50))) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${profile.coins}", fontFamily = FredokaFontFamily, fontSize = 15.sp, color = ChampionGold)
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 195.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(GameCatalog.skins) { skin ->
                    val owned = ownedIds.contains(skin.id)
                    val equipped = profile.selectedSkinId == skin.id
                    val hoodieCol = Color(skin.hoodieColor.r, skin.hoodieColor.g, skin.hoodieColor.b)
                    val emblemCol = Color(skin.emblemColor.r, skin.emblemColor.g, skin.emblemColor.b)

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (equipped) hoodieCol.copy(alpha = 0.25f) else SurfaceCardGlass,
                        modifier = Modifier
                            .height(145.dp)
                            .border(2.dp, if (equipped) ChampionGold else if (owned) SkyCyan else OutlineLight, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(hoodieCol)
                                        .border(2.dp, emblemCol, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Face, contentDescription = null, tint = emblemCol, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(skin.name, fontFamily = FredokaFontFamily, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(skin.description, fontSize = 10.sp, color = TextMuted, maxLines = 2, lineHeight = 13.sp)
                                }
                            }

                            Button(
                                onClick = { onSelectOrBuySkin(skin.id) },
                                enabled = owned || profile.coins >= skin.costCoins,
                                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("skin_btn_${skin.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        equipped -> ChampionGold
                                        owned -> IslandMintDark
                                        else -> SkyCyanDark
                                    },
                                    contentColor = if (equipped) RoyalNavyDeep else Color.White
                                )
                            ) {
                                Text(
                                    text = when {
                                        equipped -> strings.equipped
                                        owned -> strings.equip
                                        else -> "${strings.buy} • ${skin.costCoins}"
                                    },
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
