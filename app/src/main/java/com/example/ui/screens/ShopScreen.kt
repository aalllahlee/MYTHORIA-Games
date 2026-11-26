package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.game.model.ShopCategory
import com.example.ui.theme.*

@Composable
fun ShopScreen(
    profile: PlayerProfileEntity,
    ownedIds: Set<String>,
    strings: LocalizedStrings,
    onBuyOrEquipItem: (String, ShopCategory) -> Unit,
    onBuyOrSelectSkin: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf(ShopCategory.ABILITIES) }
    val categories = ShopCategory.entries

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RoyalNavyDeep, Color(0xFF28134D))))
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("shop_screen")
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
                    Text(strings.shop, fontFamily = FredokaFontFamily, fontSize = 24.sp, color = ChampionGold)
                }
                Surface(shape = RoundedCornerShape(50), color = SurfaceCardDark, modifier = Modifier.border(1.5.dp, ChampionGold, RoundedCornerShape(50))) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = ChampionGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${profile.coins} ${strings.coins}", fontFamily = FredokaFontFamily, fontSize = 14.sp, color = ChampionGold)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.name, fontFamily = FredokaFontFamily, fontSize = 12.sp) }
                    )
                }
            }

            if (selectedCategory == ShopCategory.CHARACTERS) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 195.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(GameCatalog.skins) { skin ->
                        val equipped = profile.selectedSkinId == skin.id
                        val col = Color(skin.hoodieColor.r, skin.hoodieColor.g, skin.hoodieColor.b)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceCardGlass,
                            modifier = Modifier
                                .height(165.dp)
                                .border(1.8.dp, if (equipped) ChampionGold else col, RoundedCornerShape(20.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = skin.name,
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 16.sp,
                                    color = ChampionGold,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                com.example.ui.components.FullBodyCharacterArtwork(
                                    skin = skin,
                                    hatId = profile.selectedHatId,
                                    shoeId = profile.selectedShoeId,
                                    trailId = profile.selectedTrailId,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                                Button(
                                    onClick = { onBuyOrSelectSkin(skin.id) },
                                    modifier = Modifier.fillMaxWidth().height(38.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (equipped) ChampionGold else IslandMintDark,
                                        contentColor = if (equipped) RoyalNavyDeep else Color.White
                                    )
                                ) {
                                    Text(
                                        text = if (equipped) strings.equipped else strings.equip,
                                        fontFamily = FredokaFontFamily,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val items = remember(selectedCategory) {
                    GameCatalog.shopItems.filter { it.category == selectedCategory }
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 195.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        val owned = ownedIds.contains(item.id)
                        val equipped = when (item.category) {
                            ShopCategory.HATS -> profile.selectedHatId == item.id
                            ShopCategory.SHOES -> profile.selectedShoeId == item.id
                            ShopCategory.TRAILS -> profile.selectedTrailId == item.id
                            ShopCategory.VICTORY -> profile.selectedVictoryId == item.id
                            ShopCategory.ABILITIES -> profile.dashUnlocked
                            ShopCategory.CHARACTERS -> false
                        }
                        val col = Color(item.previewColor.r, item.previewColor.g, item.previewColor.b)
                        ShopCard(
                            item = item,
                            accent = col,
                            owned = owned,
                            equipped = equipped,
                            canAfford = profile.coins >= item.costCoins,
                            strings = strings,
                            onAction = { onBuyOrEquipItem(item.id, item.category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopCard(
    item: com.example.game.model.ShopItemDef,
    accent: Color,
    owned: Boolean,
    equipped: Boolean,
    canAfford: Boolean,
    strings: LocalizedStrings,
    onAction: () -> Unit
) {
    val rarityCol = Color(item.rarity.colorHex)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceCardGlass,
        modifier = Modifier
            .height(152.dp)
            .border(1.8.dp, if (equipped) ChampionGold else accent, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.example.ui.components.ShopItem3DArtwork(
                    item = item,
                    modifier = Modifier.size(58.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = rarityCol.copy(alpha = 0.22f),
                        modifier = Modifier.border(1.dp, rarityCol, RoundedCornerShape(5.dp))
                    ) {
                        Text(
                            text = item.rarity.labelEn,
                            fontFamily = FredokaFontFamily,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = rarityCol,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(item.name, fontFamily = FredokaFontFamily, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(item.description, fontSize = 10.sp, color = TextMuted, maxLines = 2, lineHeight = 12.sp)
                }
            }
            Button(
                onClick = onAction,
                enabled = owned || canAfford,
                modifier = Modifier.fillMaxWidth().height(38.dp),
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
                        else -> "${strings.buy} • ${item.costCoins}"
                    },
                    fontFamily = FredokaFontFamily,
                    fontSize = 13.sp
                )
            }
        }
    }
}
