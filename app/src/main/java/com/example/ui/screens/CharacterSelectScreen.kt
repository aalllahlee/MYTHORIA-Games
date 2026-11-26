package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.PlayerProfileEntity
import com.example.game.model.GameCatalog
import com.example.game.model.LocalizedStrings
import com.example.game.model.ShopCategory
import com.example.ui.components.FullBodyCharacterArtwork
import com.example.ui.theme.*

@Composable
fun CharacterSelectScreen(
    profile: PlayerProfileEntity,
    ownedIds: Set<String>,
    strings: LocalizedStrings,
    onSelectOrBuySkin: (String) -> Unit,
    onBuyOrEquipItem: (String, ShopCategory) -> Unit = { _, _ -> },
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var rotationDeg by remember { mutableFloatStateOf(0f) }
    var activeWardrobeTab by remember { mutableStateOf(ShopCategory.HATS) }
    val wardrobeCategories = remember {
        listOf(ShopCategory.HATS, ShopCategory.SHOES, ShopCategory.TRAILS, ShopCategory.VICTORY)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, Color(0xFF14275E))))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("character_select_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Header Row
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
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${strings.characters} (${GameCatalog.skins.size})",
                        fontFamily = FredokaFontFamily,
                        fontSize = 22.sp,
                        color = ChampionGold
                    )
                }

                // 360° Rotation Controls + Coin Balance
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SurfaceCardDark,
                        modifier = Modifier
                            .border(1.2.dp, SkyCyan, RoundedCornerShape(50))
                            .clickable { rotationDeg = (rotationDeg - 45f) % 360f }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Rotate Left", tint = SkyCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("360°", fontFamily = FredokaFontFamily, fontSize = 12.sp, color = Color.White)
                        }
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

            // 3 Exclusive Heroes Row — Full-Body Figure + Name ONLY (zero description text!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            rotationDeg = (rotationDeg + dragAmount * 0.55f) % 360f
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameCatalog.skins.forEach { skin ->
                    val equipped = profile.selectedSkinId == skin.id
                    val hoodieCol = Color(skin.hoodieColor.r, skin.hoodieColor.g, skin.hoodieColor.b)

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (equipped) hoodieCol.copy(alpha = 0.28f) else SurfaceCardGlass,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = if (equipped) 2.8.dp else 1.6.dp,
                                brush = if (equipped) {
                                    Brush.verticalGradient(listOf(MythicGoldLight, ChampionGold, SapphireGlow))
                                } else {
                                    Brush.verticalGradient(listOf(SkyCyan.copy(alpha = 0.7f), Color(0xFF1E3A8A)))
                                },
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable { onSelectOrBuySkin(skin.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Character Name ONLY (No description text!)
                            Text(
                                text = skin.name,
                                fontFamily = FredokaFontFamily,
                                fontSize = 18.sp,
                                color = if (equipped) MythicGoldLight else Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )

                            // Full-Body 3D-Shaded Character Figure with live equipped outfit & 360° drag rotation
                            FullBodyCharacterArtwork(
                                skin = skin,
                                hatId = profile.selectedHatId,
                                shoeId = profile.selectedShoeId,
                                trailId = profile.selectedTrailId,
                                rotationDeg = rotationDeg,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )

                            Button(
                                onClick = { onSelectOrBuySkin(skin.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("skin_btn_${skin.id}"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (equipped) ChampionGold else IslandMintDark,
                                    contentColor = if (equipped) RoyalNavyDeep else Color.White
                                )
                            ) {
                                Text(
                                    text = if (equipped) "★ ${strings.equipped} ★" else strings.equip,
                                    fontFamily = FredokaFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Direct Outfit & Wardrobe Control Bar (Hats/Crowns, Boots/Shoes, Trails, Victory Poses)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xD90A1536),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, ChampionGold.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                    .testTag("character_wardrobe_bar")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        wardrobeCategories.forEach { cat ->
                            val selected = activeWardrobeTab == cat
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) ChampionGold else SurfaceCardDark,
                                modifier = Modifier
                                    .border(
                                        1.2.dp,
                                        if (selected) MythicGoldLight else SkyCyan.copy(alpha = 0.5f),
                                        RoundedCornerShape(50)
                                    )
                                    .clickable { activeWardrobeTab = cat }
                                    .testTag("wardrobe_tab_${cat.name}")
                            ) {
                                Text(
                                    text = cat.name,
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) RoyalNavyDeep else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    val categoryItems = remember(activeWardrobeTab) {
                        GameCatalog.shopItems.filter { it.category == activeWardrobeTab }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryItems) { item ->
                            val owned = ownedIds.contains(item.id) || item.costCoins == 0
                            val isEquipped = when (item.category) {
                                ShopCategory.HATS -> profile.selectedHatId == item.id
                                ShopCategory.SHOES -> profile.selectedShoeId == item.id
                                ShopCategory.TRAILS -> profile.selectedTrailId == item.id
                                ShopCategory.VICTORY -> profile.selectedVictoryId == item.id
                                else -> false
                            }
                            val itemCol = Color(item.previewColor.r, item.previewColor.g, item.previewColor.b)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isEquipped) itemCol.copy(alpha = 0.32f) else SurfaceCardDark,
                                modifier = Modifier
                                    .border(
                                        1.5.dp,
                                        if (isEquipped) ChampionGold else itemCol.copy(alpha = 0.7f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onBuyOrEquipItem(item.id, item.category) }
                                    .testTag("wardrobe_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.example.ui.components.ShopItem3DArtwork(
                                        item = item,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEquipped) MythicGoldLight else Color.White
                                    )
                                    Text(
                                        text = when {
                                            isEquipped -> "★"
                                            owned -> strings.equip
                                            else -> "${item.costCoins}"
                                        },
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 11.sp,
                                        color = if (isEquipped) ChampionGold else SkyCyan
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
