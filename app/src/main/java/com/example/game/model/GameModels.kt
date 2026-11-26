package com.example.game.model

enum class GameMode {
    CAREER,
    RIVAL_RACE,
    ENDLESS,
    TIME_ATTACK,
    PRACTICE,
    DAILY_CHALLENGE
}

enum class GraphicsQuality(val label: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH")
}

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    AR("ar", "العربية"),
    FR("fr", "Français")
}

enum class ShopCategory {
    CHARACTERS,
    HATS,
    SHOES,
    TRAILS,
    VICTORY,
    ABILITIES
}

enum class PowerUpType {
    SHIELD,
    MAGNET,
    WINGS
}

data class PowerUpDef(
    val x: Float,
    val y: Float,
    val z: Float,
    val type: PowerUpType
)

enum class ChestTier(
    val defaultCoins: Int,
    val titleEn: String,
    val titleAr: String,
    val colorHex: Long
) {
    WOODEN(35, "Wooden Chest", "صندوق خشبي", 0xFFD97706),
    METALLIC(60, "Metallic Chest", "صندوق معدني", 0xFF94A3B8),
    MAGIC_RARE(95, "Magic Rare Chest", "صندوق سحري نادر", 0xFF00E5FF),
    EVENT_BOSS(135, "Royal Event Chest", "صندوق ملكي للفعالية", 0xFFFFD22B);

    fun localizedTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) titleAr else titleEn
}

enum class ItemRarity(
    val labelEn: String,
    val labelAr: String,
    val colorHex: Long
) {
    COMMON("COMMON", "شائع", 0xFF94A3B8),
    RARE("RARE", "نادر", 0xFF00E5FF),
    EPIC("EPIC", "ملحمي", 0xFFC83BFF),
    LEGENDARY("LEGENDARY", "أسطوري", 0xFFFFD22B),
    MYTHIC_EVENT("EVENT LIMITED", "حصري للفعالية", 0xFFFF3B5C);

    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) labelAr else labelEn
}

enum class DifficultyTier(
    val labelEn: String,
    val labelAr: String,
    val colorHex: Long
) {
    TUTORIAL("TUTORIAL", "تعليمي", 0xFF2EE58D),
    EASY("EASY", "سهل", 0xFF00E5FF),
    MEDIUM("MEDIUM", "متوسط", 0xFFFFD22B),
    HARD("HARD", "صعب", 0xFFFF8C00),
    EXPERT("EXPERT", "خبير", 0xFFFF3B5C);

    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) labelAr else labelEn
}

data class TreasureChestDef(
    val x: Float,
    val y: Float,
    val z: Float,
    val bonusCoins: Int = 75,
    val isBossChest: Boolean = false,
    val tier: ChestTier = if (isBossChest) ChestTier.EVENT_BOSS else ChestTier.WOODEN
)

data class GhostFrame(
    val timeSec: Float,
    val x: Float,
    val y: Float,
    val z: Float,
    val yaw: Float
)

data class Rgba(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {
    companion object {
        fun fromHex(hex: Long): Rgba {
            val r = ((hex shr 16) and 0xFF) / 255f
            val g = ((hex shr 8) and 0xFF) / 255f
            val b = (hex and 0xFF) / 255f
            return Rgba(r, g, b, 1f)
        }
    }
}

data class WorldTheme(
    val id: Int,
    val name: String,
    val skyTop: Rgba,
    val skyBottom: Rgba,
    val sunColor: Rgba,
    val platformPrimary: Rgba,
    val platformSecondary: Rgba,
    val platformAccent: Rgba,
    val obstaclePrimary: Rgba,
    val obstacleSecondary: Rgba,
    val gravityMultiplier: Float = 1.0f,
    val frictionMultiplier: Float = 1.0f,
    val hazardFloorColor: Rgba? = null
)

enum class PlatformType {
    NORMAL,
    MOVING,
    DISAPPEARING,
    FALLING,
    SLIPPERY,
    CONVEYOR,
    SPEED_BOOST,
    BOUNCE_PAD,
    NARROW_BRIDGE,
    ELEVATOR,
    SECRET_PATH,
    FINISH
}

enum class ObstacleType {
    ROTATING_HAMMER,
    ROLLING_BALL,
    SWINGING_PENDULUM,
    ROTATING_BEAM,
    MOVING_WALL,
    JUMPING_BLOCK,
    SLIDING_OBSTACLE,
    SPINNING_BLADE,
    PUSHER,
    GIANT_SWINGER,
    FIRE_JET,
    LASER_BEAM,
    FALLING_STALACTITE
}

data class PlatformDef(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
    val h: Float,
    val d: Float,
    val type: PlatformType = PlatformType.NORMAL,
    val color: Rgba? = null,
    val moveDx: Float = 0f,
    val moveDy: Float = 0f,
    val moveDz: Float = 0f,
    val speed: Float = 1.4f,
    val phase: Float = 0f,
    val dirX: Float = 0f,
    val dirZ: Float = 1f
)

data class ObstacleDef(
    val x: Float,
    val y: Float,
    val z: Float,
    val type: ObstacleType,
    val scale: Float = 1.0f,
    val speed: Float = 1.8f,
    val range: Float = 3.2f,
    val phase: Float = 0f,
    val axisX: Float = 1f,
    val axisZ: Float = 0f
)

data class CheckpointDef(
    val x: Float,
    val y: Float,
    val z: Float,
    val index: Int,
    val safeYaw: Float = 180f
)

data class PlayerCheckpointState(
    val playerId: String,
    var currentX: Float = 0f,
    var currentY: Float = 1.6f,
    var currentZ: Float = 1.5f,
    var lastActivatedCheckpoint: Int = 0,
    val activatedCheckpoints: MutableSet<Int> = mutableSetOf(0),
    var safeRespawnX: Float = 0f,
    var safeRespawnY: Float = 1.6f,
    var safeRespawnZ: Float = 1.5f,
    var safeRespawnYaw: Float = 180f,
    var isRespawning: Boolean = false,
    var respawnTimerSec: Float = 0f,
    var fallAnimTimerSec: Float = 0f,
    var arrivalAnimTimerSec: Float = 0f,
    var portalScale: Float = 0f,
    var raceProgress: Float = 0f,
    var finished: Boolean = false,
    var respawnCount: Int = 0
)

data class CoinDef(
    val x: Float,
    val y: Float,
    val z: Float
)

data class LevelDef(
    val number: Int,
    val worldId: Int,
    val title: String,
    val subtitle: String,
    val parTimeSec: Float,
    val rewardCoins: Int,
    val platforms: List<PlatformDef>,
    val obstacles: List<ObstacleDef>,
    val checkpoints: List<CheckpointDef>,
    val coins: List<CoinDef>,
    val powerUps: List<PowerUpDef> = emptyList(),
    val treasureChests: List<TreasureChestDef> = emptyList(),
    val startX: Float = 0f,
    val startY: Float = 1.5f,
    val startZ: Float = 0f,
    val finishZ: Float
) {
    val difficultyTier: DifficultyTier
        get() = when {
            number <= 5 -> DifficultyTier.TUTORIAL
            number <= 15 -> DifficultyTier.EASY
            number <= 30 -> DifficultyTier.MEDIUM
            number <= 40 -> DifficultyTier.HARD
            else -> DifficultyTier.EXPERT
        }
}

enum class HeadAccessoryStyle {
    AHMED_ROYAL_WINGED,
    SALMA_STAR_PRINCESS,
    KID_AHMED_TURBO,
    SPIKY_HAIR,
    NINJA_HOOD,
    CROWN_HELM,
    PANDA_EARS,
    ROBOT_ANTENNA,
    ASTRONAUT_HELMET,
    PIRATE_BANDANA,
    KNIGHT_VISOR,
    HEADBAND,
    ICE_HORNS,
    FLAME_CREST,
    ALIEN_ANTENNAE,
    ROYAL_CROWN,
    SHADOW_COWL,
    LEGEND_HALO
}

data class SkinDef(
    val id: String,
    val name: String,
    val description: String,
    val costCoins: Int,
    val unlockLevelReq: Int = 1,
    val hoodieColor: Rgba,
    val pantsColor: Rgba,
    val skinColor: Rgba,
    val hairOrAccentColor: Rgba,
    val shoeColor: Rgba,
    val emblemColor: Rgba,
    val headStyle: HeadAccessoryStyle
)

data class ShopItemDef(
    val id: String,
    val category: ShopCategory,
    val name: String,
    val description: String,
    val costCoins: Int,
    val previewColor: Rgba,
    val rarity: ItemRarity = when {
        costCoins == 0 -> ItemRarity.COMMON
        costCoins < 700 -> ItemRarity.RARE
        costCoins < 1200 -> ItemRarity.EPIC
        costCoins < 1800 -> ItemRarity.LEGENDARY
        else -> ItemRarity.MYTHIC_EVENT
    }
)

data class AchievementDef(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val rewardCoins: Int
)

object GameCatalog {
    val worlds: List<WorldTheme> = listOf(
        WorldTheme(
            id = 1,
            name = "Ancient Greek Islands",
            skyTop = Rgba.fromHex(0x0E3B8C),
            skyBottom = Rgba.fromHex(0x64C7FF),
            sunColor = Rgba.fromHex(0xFFF3B0),
            platformPrimary = Rgba.fromHex(0xE2E8F0),
            platformSecondary = Rgba.fromHex(0x00C2FF),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0xD97706),
            obstacleSecondary = Rgba.fromHex(0xFFD22B)
        ),
        WorldTheme(
            id = 2,
            name = "Floating Temples",
            skyTop = Rgba.fromHex(0x132D6A),
            skyBottom = Rgba.fromHex(0x7FE3FF),
            sunColor = Rgba.fromHex(0xFFE082),
            platformPrimary = Rgba.fromHex(0xCBD5E1),
            platformSecondary = Rgba.fromHex(0x0077FF),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0x00B4D8),
            obstacleSecondary = Rgba.fromHex(0xFFF05A)
        ),
        WorldTheme(
            id = 3,
            name = "Zeus Thunder Arena",
            skyTop = Rgba.fromHex(0x091538),
            skyBottom = Rgba.fromHex(0x1D4ED8),
            sunColor = Rgba.fromHex(0x00E5FF),
            platformPrimary = Rgba.fromHex(0x1E293B),
            platformSecondary = Rgba.fromHex(0x00D4FF),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0x00E5FF),
            obstacleSecondary = Rgba.fromHex(0xFFFFFF)
        ),
        WorldTheme(
            id = 4,
            name = "Golden Bridges",
            skyTop = Rgba.fromHex(0x1E1B4B),
            skyBottom = Rgba.fromHex(0xF59E0B),
            sunColor = Rgba.fromHex(0xFFF8D6),
            platformPrimary = Rgba.fromHex(0xFFD22B),
            platformSecondary = Rgba.fromHex(0xD97706),
            platformAccent = Rgba.fromHex(0x00E5FF),
            obstaclePrimary = Rgba.fromHex(0xB5179E),
            obstacleSecondary = Rgba.fromHex(0xFFD166)
        ),
        WorldTheme(
            id = 5,
            name = "Ancient Marble Platforms",
            skyTop = Rgba.fromHex(0x1B4965),
            skyBottom = Rgba.fromHex(0xCAE9FF),
            sunColor = Rgba.fromHex(0xE0FBFC),
            platformPrimary = Rgba.fromHex(0xF1F5F9),
            platformSecondary = Rgba.fromHex(0x62B6CB),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0x0077FF),
            obstacleSecondary = Rgba.fromHex(0xFFD22B),
            frictionMultiplier = 0.55f
        ),
        WorldTheme(
            id = 6,
            name = "Magical Portals",
            skyTop = Rgba.fromHex(0x10002B),
            skyBottom = Rgba.fromHex(0x3C096C),
            sunColor = Rgba.fromHex(0x00E5FF),
            platformPrimary = Rgba.fromHex(0x240046),
            platformSecondary = Rgba.fromHex(0x00B4D8),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0x9D4EDD),
            obstacleSecondary = Rgba.fromHex(0x00E5FF),
            hazardFloorColor = Rgba.fromHex(0x0077FF)
        ),
        WorldTheme(
            id = 7,
            name = "Mythological Gardens",
            skyTop = Rgba.fromHex(0x0F3D3E),
            skyBottom = Rgba.fromHex(0x64DFDF),
            sunColor = Rgba.fromHex(0xFFE66D),
            platformPrimary = Rgba.fromHex(0x2EE58D),
            platformSecondary = Rgba.fromHex(0xE2E8F0),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0xD97706),
            obstacleSecondary = Rgba.fromHex(0xFFF3B0)
        ),
        WorldTheme(
            id = 8,
            name = "Crystal Caves",
            skyTop = Rgba.fromHex(0x07102E),
            skyBottom = Rgba.fromHex(0x0077B6),
            sunColor = Rgba.fromHex(0x00F5D4),
            platformPrimary = Rgba.fromHex(0x00B4D8),
            platformSecondary = Rgba.fromHex(0x7209B7),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0xF72585),
            obstacleSecondary = Rgba.fromHex(0x90E0EF)
        ),
        WorldTheme(
            id = 9,
            name = "Floating Castles",
            skyTop = Rgba.fromHex(0x070B1E),
            skyBottom = Rgba.fromHex(0x2D1E6B),
            sunColor = Rgba.fromHex(0xFFD22B),
            platformPrimary = Rgba.fromHex(0xCBD5E1),
            platformSecondary = Rgba.fromHex(0x4361EE),
            platformAccent = Rgba.fromHex(0xFFD22B),
            obstaclePrimary = Rgba.fromHex(0x00E5FF),
            obstacleSecondary = Rgba.fromHex(0xFFD22B),
            gravityMultiplier = 0.74f
        ),
        WorldTheme(
            id = 10,
            name = "Ancient Ruins",
            skyTop = Rgba.fromHex(0x0A122C),
            skyBottom = Rgba.fromHex(0x1E3A8A),
            sunColor = Rgba.fromHex(0xFFD22B),
            platformPrimary = Rgba.fromHex(0xFFD22B),
            platformSecondary = Rgba.fromHex(0x334155),
            platformAccent = Rgba.fromHex(0x00E5FF),
            obstaclePrimary = Rgba.fromHex(0x00D4FF),
            obstacleSecondary = Rgba.fromHex(0xFFD22B)
        )
    )

    // Strictly 3 characters with ONLY their names and no description text
    val skins: List<SkinDef> = listOf(
        SkinDef(
            id = "red_hero",
            name = "Ahmed",
            description = "",
            costCoins = 0,
            unlockLevelReq = 1,
            hoodieColor = Rgba.fromHex(0xE62E4D),
            pantsColor = Rgba.fromHex(0x1E2949),
            skinColor = Rgba.fromHex(0xFFCAA8),
            hairOrAccentColor = Rgba.fromHex(0x4A2511),
            shoeColor = Rgba.fromHex(0xFFB703),
            emblemColor = Rgba.fromHex(0xFFD22B),
            headStyle = HeadAccessoryStyle.AHMED_ROYAL_WINGED
        ),
        SkinDef(
            id = "salma_heroine",
            name = "Salma",
            description = "",
            costCoins = 0,
            unlockLevelReq = 1,
            hoodieColor = Rgba.fromHex(0xFF4785),
            pantsColor = Rgba.fromHex(0x5A189A),
            skinColor = Rgba.fromHex(0xFFD6BA),
            hairOrAccentColor = Rgba.fromHex(0x2D1635),
            shoeColor = Rgba.fromHex(0xFF85A1),
            emblemColor = Rgba.fromHex(0xFFD22B),
            headStyle = HeadAccessoryStyle.SALMA_STAR_PRINCESS
        ),
        SkinDef(
            id = "kid_ahmed",
            name = "Ahmed",
            description = "",
            costCoins = 0,
            unlockLevelReq = 1,
            hoodieColor = Rgba.fromHex(0x00B4D8),
            pantsColor = Rgba.fromHex(0x1D3557),
            skinColor = Rgba.fromHex(0xFFCAA8),
            hairOrAccentColor = Rgba.fromHex(0x3B1F0E),
            shoeColor = Rgba.fromHex(0x10B981),
            emblemColor = Rgba.fromHex(0x00F5D4),
            headStyle = HeadAccessoryStyle.KID_AHMED_TURBO
        )
    )

    val shopItems: List<ShopItemDef> = listOf(
        // Abilities
        ShopItemDef("ability_dash", ShopCategory.ABILITIES, "Thunder Dash", "Unlocks a mid-air & ground lightning Dash button with cooldown.", 650, Rgba.fromHex(0x00D4FF), ItemRarity.EPIC),
        // Hats & Realistic Hoods / Headgear
        ShopItemDef("hat_none", ShopCategory.HATS, "Default Athletic Hair", "Clean modern athletic runner hairstyle.", 0, Rgba.fromHex(0x94A3B8), ItemRarity.COMMON),
        ShopItemDef("hat_headband", ShopCategory.HATS, "Crimson Pro Track Hood", "Breathable red & carbon streetwear athletic hood.", 420, Rgba.fromHex(0xFF3B5C), ItemRarity.RARE),
        ShopItemDef("hat_visor", ShopCategory.HATS, "Sapphire Aero Visor", "High-contrast blue competition running visor & tech hood.", 780, Rgba.fromHex(0x00E5FF), ItemRarity.EPIC),
        ShopItemDef("hat_crown", ShopCategory.HATS, "Black & Gold Champion Crown", "Championship gold & obsidian crown emblem.", 1150, Rgba.fromHex(0xFFD22B), ItemRarity.LEGENDARY),
        ShopItemDef("hat_halo", ShopCategory.HATS, "Mythoria Astral Halo", "Limited-edition floating golden solar ring.", 1650, Rgba.fromHex(0xFFF05A), ItemRarity.MYTHIC_EVENT),
        // Shoes & Realistic Athletic Footwear / Tracksuit Bottom Sets
        ShopItemDef("shoe_default", ShopCategory.SHOES, "Crimson Street High-Tops", "Red & carbon high-top parkour sneakers with cargo trim.", 0, Rgba.fromHex(0xFF3B5C), ItemRarity.COMMON),
        ShopItemDef("shoe_rocket", ShopCategory.SHOES, "Cobalt Pro Track Runners", "White & cobalt blue competition marathon sneakers.", 640, Rgba.fromHex(0x00D4FF), ItemRarity.RARE),
        ShopItemDef("shoe_shadow", ShopCategory.SHOES, "Tactical Olive Trail Boots", "Rugged olive & carbon tactical outdoor running shoes.", 980, Rgba.fromHex(0x4D7C0F), ItemRarity.EPIC),
        ShopItemDef("shoe_gold", ShopCategory.SHOES, "Black & Gold Apex Sneakers", "Luxury black & metallic gold championship high-tops.", 1450, Rgba.fromHex(0xFFD22B), ItemRarity.LEGENDARY),
        // Trails & Outfit Auras
        ShopItemDef("trail_dust", ShopCategory.TRAILS, "Clean Air Slipstream", "Subtle aerodynamic white sprint vapor.", 0, Rgba.fromHex(0xFFFFFF), ItemRarity.COMMON),
        ShopItemDef("trail_gold", ShopCategory.TRAILS, "Black & Gold Championship Aura", "Gleaming golden carbon sparks behind every stride.", 720, Rgba.fromHex(0xFFD22B), ItemRarity.RARE),
        ShopItemDef("trail_cyan", ShopCategory.TRAILS, "Cobalt Velocity Streak", "Electric sapphire speed lines for competition runners.", 960, Rgba.fromHex(0x00E5FF), ItemRarity.EPIC),
        ShopItemDef("trail_flame", ShopCategory.TRAILS, "Crimson Streetwear Blaze", "High-energy crimson & amber thermal sprint trail.", 1280, Rgba.fromHex(0xFF5400), ItemRarity.LEGENDARY),
        ShopItemDef("trail_rainbow", ShopCategory.TRAILS, "Mythoria Festival Prism", "Limited-edition seasonal championship aura.", 1850, Rgba.fromHex(0xFF5CA8), ItemRarity.MYTHIC_EVENT),
        // Victory Poses
        ShopItemDef("victory_dance", ShopCategory.VICTORY, "Athlete Fist Pump", "Confident champion podium salute.", 0, Rgba.fromHex(0x2EE58D), ItemRarity.COMMON),
        ShopItemDef("victory_spin", ShopCategory.VICTORY, "360° Parkour Cyclone", "Airborne 360-degree freestyle spin landing.", 580, Rgba.fromHex(0xFFD22B), ItemRarity.RARE),
        ShopItemDef("victory_flip", ShopCategory.VICTORY, "Apex Championship Backflip", "Explosive acrobatic backflip at the finish arch.", 1120, Rgba.fromHex(0xC83BFF), ItemRarity.LEGENDARY)
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef("ach_first_win", "First Steps Victory", "Complete your first level in Career Mode.", 1, 75),
        AchievementDef("ach_world_1", "Island Conqueror", "Complete all 5 levels of World 1.", 5, 180),
        AchievementDef("ach_coins_500", "Treasure Hunter", "Collect 500 total coins across any modes.", 500, 120),
        AchievementDef("ach_skins_3", "Style Collector", "Unlock 3 different character skins.", 3, 150),
        AchievementDef("ach_dash", "Speed Burst", "Unlock the Turbo Dash ability in the Shop.", 1, 90),
        AchievementDef("ach_endless_100", "Sky Survivor", "Reach 100m distance in Endless Mode.", 100, 200),
        AchievementDef("ach_daily", "Daily Challenger", "Complete a Daily Challenge run.", 1, 150),
        AchievementDef("ach_10_wins", "Obstacle Master", "Win 10 total runs across the game.", 10, 320)
    )

    val missions: List<MissionDef> = listOf(
        MissionDef(
            id = "mis_speed_90",
            type = MissionType.SPEED_90S,
            cadence = MissionCadence.DAILY,
            titleEn = "Speed Mission: Finish 2 Stages under 90s",
            titleAr = "مهمة السرعة: أنهِ مرحلتين خلال أقل من 90 ثانية",
            titleFr = "Vitesse : Terminer 2 niveaux en 90s",
            target = 2,
            rewardCoins = 220,
            rewardXp = 260
        ),
        MissionDef(
            id = "mis_perfect_landings",
            type = MissionType.PERFECT_LANDINGS,
            cadence = MissionCadence.DAILY,
            titleEn = "Precision Mission: Perform 8 Perfect Landings",
            titleAr = "مهمة الدقة: نفّذ 8 هبوطات مثالية في منتصف المنصة",
            titleFr = "Précision : Réussir 8 atterrissages parfaits",
            target = 8,
            rewardCoins = 260,
            rewardXp = 300
        ),
        MissionDef(
            id = "mis_no_checkpoint",
            type = MissionType.NO_CHECKPOINT_WIN,
            cadence = MissionCadence.DAILY,
            titleEn = "Zero-Fall Survival: Win 2 Stages without Respawning",
            titleAr = "مهمة البقاء الاحترافية: فز بمرحلتين دون أي سقوط",
            titleFr = "Survie : Gagner 2 niveaux sans chute",
            target = 2,
            rewardCoins = 340,
            rewardXp = 380
        ),
        MissionDef(
            id = "mis_secret_path",
            type = MissionType.FIND_SECRET_PATH,
            cadence = MissionCadence.DAILY,
            titleEn = "Explorer Mastery: Discover 2 Secret Side Paths",
            titleAr = "مهمة الاستكشاف: اعثر على مسارين سريين جانبيين",
            titleFr = "Exploration : Trouver 2 chemins secrets",
            target = 2,
            rewardCoins = 320,
            rewardXp = 350
        ),
        MissionDef(
            id = "mis_open_chest",
            type = MissionType.OPEN_CHESTS,
            cadence = MissionCadence.DAILY,
            titleEn = "Vault Raider: Open 2 Secret or Boss Treasure Chests",
            titleAr = "مهمة الكنز: افتح صندوقي كنز سري أو ملكي",
            titleFr = "Trésor : Ouvrir 2 coffres au trésor",
            target = 2,
            rewardCoins = 290,
            rewardXp = 320
        ),
        MissionDef(
            id = "mis_narrow_bridges",
            type = MissionType.CROSS_NARROW_BRIDGES,
            cadence = MissionCadence.DAILY,
            titleEn = "Balance Master: Cross 5 Narrow Bridges without Falling",
            titleAr = "مهمة التوازن: اعبر 5 جسور ضيقة دون السقوط",
            titleFr = "Défi : Traverser 5 ponts étroits sans tomber",
            target = 5,
            rewardCoins = 280,
            rewardXp = 310
        ),
        MissionDef(
            id = "mis_no_dash",
            type = MissionType.NO_DASH_WIN,
            cadence = MissionCadence.DAILY,
            titleEn = "Pure Footwork: Win 2 Stages without Using Dash",
            titleAr = "مهمة التحكم الخالص: فز بمرحلتين دون استخدام الاندفاع",
            titleFr = "Avancé : Gagner 2 niveaux sans utiliser le Dash",
            target = 2,
            rewardCoins = 360,
            rewardXp = 400
        ),
        MissionDef(
            id = "mis_stages",
            type = MissionType.COMPLETE_STAGES,
            cadence = MissionCadence.DAILY,
            titleEn = "Daily Grind: Complete 5 Stages",
            titleAr = "التحدي اليومي: أكمل 5 مراحل",
            titleFr = "Terminer 5 niveaux",
            target = 5,
            rewardCoins = 280,
            rewardXp = 320
        ),
        MissionDef(
            id = "mis_coins",
            type = MissionType.COLLECT_COINS,
            cadence = MissionCadence.DAILY,
            titleEn = "Gold Rush: Collect 250 Coins",
            titleAr = "جامع الذهب: اجمع 250 عملة ذهبية",
            titleFr = "Collecter 250 pièces",
            target = 250,
            rewardCoins = 240,
            rewardXp = 280
        ),
        MissionDef(
            id = "mis_jumps",
            type = MissionType.JUMP_COUNT,
            cadence = MissionCadence.DAILY,
            titleEn = "Sky Leaper: Perform 45 Jumps",
            titleAr = "قفزات البطولة: اقفز 45 مرة",
            titleFr = "Sauter 45 fois",
            target = 45,
            rewardCoins = 190,
            rewardXp = 230
        ),
        MissionDef(
            id = "mis_rival",
            type = MissionType.WIN_RIVAL_RACE,
            cadence = MissionCadence.DAILY,
            titleEn = "Arena Champion: Win 2 AI Rival Races",
            titleAr = "بطل السباق: فز مرتين في سباق المنافسين",
            titleFr = "Gagner 2 courses IA",
            target = 2,
            rewardCoins = 350,
            rewardXp = 380
        ),
        // Weekly Missions (Harder targets, rewarding multi-session progression)
        MissionDef(
            id = "mis_weekly_stages_10",
            type = MissionType.COMPLETE_STAGES,
            cadence = MissionCadence.WEEKLY,
            titleEn = "Weekly Marathon: Complete 15 Stages",
            titleAr = "ماراثون الأسبوع: أكمل 15 مرحلة",
            titleFr = "Hebdo : Terminer 15 niveaux",
            target = 15,
            rewardCoins = 750,
            rewardXp = 850
        ),
        MissionDef(
            id = "mis_weekly_chests_3",
            type = MissionType.OPEN_CHESTS,
            cadence = MissionCadence.WEEKLY,
            titleEn = "Weekly Vault Hunter: Open 6 Treasure Chests",
            titleAr = "صائد الكنوز الأسبوعي: افتح 6 صناديق كنز",
            titleFr = "Hebdo : Ouvrir 6 coffres au trésor",
            target = 6,
            rewardCoins = 820,
            rewardXp = 920
        ),
        MissionDef(
            id = "mis_weekly_coins_400",
            type = MissionType.COLLECT_COINS,
            cadence = MissionCadence.WEEKLY,
            titleEn = "Weekly Treasury: Collect 900 Coins",
            titleAr = "ثروة الأسبوع: اجمع 900 عملة ذهبية",
            titleFr = "Hebdo : Collecter 900 pièces",
            target = 900,
            rewardCoins = 780,
            rewardXp = 880
        ),
        // Seasonal Event Missions
        MissionDef(
            id = "mis_event_festival_stages",
            type = MissionType.COMPLETE_STAGES,
            cadence = MissionCadence.EVENT,
            titleEn = "Event Odyssey: Complete 5 Festival Stages",
            titleAr = "مهمة الفعالية: أكمل 5 مراحل خلال الموسم",
            titleFr = "Événement : Terminer 5 niveaux",
            target = 5,
            rewardCoins = 620,
            rewardXp = 700
        ),
        MissionDef(
            id = "mis_event_royal_chest",
            type = MissionType.OPEN_CHESTS,
            cadence = MissionCadence.EVENT,
            titleEn = "Event Vault: Open 3 Event or Magic Chests",
            titleAr = "صندوق الفعالية: افتح 3 صناديق سحرية أو ملكية",
            titleFr = "Événement : Ouvrir 3 coffres royaux",
            target = 3,
            rewardCoins = 680,
            rewardXp = 760
        )
    )

    val loginStreakRewards: List<LoginRewardDayDef> = listOf(
        LoginRewardDayDef(1, 50, ChestTier.WOODEN, "Day 1 • +50", "اليوم 1 • +50"),
        LoginRewardDayDef(2, 75, ChestTier.WOODEN, "Day 2 • +75", "اليوم 2 • +75"),
        LoginRewardDayDef(3, 110, ChestTier.METALLIC, "Day 3 • +110", "اليوم 3 • +110"),
        LoginRewardDayDef(4, 150, ChestTier.METALLIC, "Day 4 • +150", "اليوم 4 • +150"),
        LoginRewardDayDef(5, 200, ChestTier.MAGIC_RARE, "Day 5 • +200", "اليوم 5 • +200"),
        LoginRewardDayDef(6, 260, ChestTier.MAGIC_RARE, "Day 6 • +260", "اليوم 6 • +260"),
        LoginRewardDayDef(7, 420, ChestTier.EVENT_BOSS, "Day 7 • +420", "اليوم 7 • +420")
    )

    val seasonalEvents: List<SeasonalEventDef> = listOf(
        SeasonalEventDef(
            id = "event_golden_lights",
            titleEn = "Festival of Golden Lights",
            titleAr = "مهرجان الأضواء الذهبية",
            subtitleEn = "Glowing lanterns, golden bridges & 2x Event Chest rewards (Local Event)",
            subtitleAr = "فوانيس مضيئة وجسور ذهبية وصندوق فعالية ملكي (محلي بدون خادم)",
            featuredWorldId = 4,
            featuredStageNumber = 20,
            bannerColorHex = 0xFFFFB703,
            accentColorHex = 0xFF00E5FF,
            bonusChestCoins = 180
        ),
        SeasonalEventDef(
            id = "event_frost_sanctuary",
            titleEn = "Crystal Frost Sanctuary",
            titleAr = "موسم بلورات الجليد",
            subtitleEn = "Sparkling crystal caverns, low-friction marble & sapphire chests (Local Event)",
            subtitleAr = "كهوف بلورية متلألئة ومنصات رخامية وصناديق ياقوتية (محلي بدون خادم)",
            featuredWorldId = 8,
            featuredStageNumber = 40,
            bannerColorHex = 0xFF00B4D8,
            accentColorHex = 0xFFFFD22B,
            bonusChestCoins = 200
        ),
        SeasonalEventDef(
            id = "event_emerald_gardens",
            titleEn = "Mythological Gardens Odyssey",
            titleAr = "مهرجان الحدائق الأسطورية",
            subtitleEn = "Sky waterfalls, emerald terraces & royal guardian rewards (Local Event)",
            subtitleAr = "شلالات معلقة وحدائق زمردية ومكافآت حارس البوابة (محلي بدون خادم)",
            featuredWorldId = 7,
            featuredStageNumber = 35,
            bannerColorHex = 0xFF2EE58D,
            accentColorHex = 0xFFFFD22B,
            bonusChestCoins = 190
        )
    )

    fun getActiveSeasonalEvent(epochDay: Long): SeasonalEventDef {
        val cycleIndex = ((epochDay.coerceAtLeast(0L) / 7L) % seasonalEvents.size).toInt()
        return seasonalEvents[cycleIndex]
    }

    fun getSeasonalEventDaysRemaining(epochDay: Long): Int {
        val dayInWeek = (epochDay.coerceAtLeast(0L) % 7L).toInt()
        return (7 - dayInWeek).coerceIn(1, 7)
    }

    val xpMilestones: List<XpMilestoneDef> = listOf(
        XpMilestoneDef(5, "Champion Band", 140, "hat_headband"),
        XpMilestoneDef(10, "300 Coin Chest", 300, null),
        XpMilestoneDef(15, "Cyber Visor", 380, "hat_visor"),
        XpMilestoneDef(20, "Gold Sparkle Trail", 500, "trail_gold"),
        XpMilestoneDef(25, "Star Halo", 650, "hat_halo"),
        XpMilestoneDef(30, "Golden Crown", 950, "hat_crown")
    )
}

enum class MissionCadence(val labelEn: String, val labelAr: String) {
    DAILY("DAILY", "يومية"),
    WEEKLY("WEEKLY", "أسبوعية"),
    EVENT("EVENT", "فعالية موسمية");

    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) labelAr else labelEn
}

enum class MissionStatus(val labelEn: String, val labelAr: String) {
    AVAILABLE("AVAILABLE", "متاحة"),
    IN_PROGRESS("IN PROGRESS", "قيد التقدم"),
    COMPLETED("COMPLETED", "مكتملة"),
    CLAIMED("CLAIMED", "مستلمة");

    fun localizedLabel(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) labelAr else labelEn
}

enum class MissionType {
    SPEED_90S,
    PERFECT_LANDINGS,
    NO_CHECKPOINT_WIN,
    FIND_SECRET_PATH,
    OPEN_CHESTS,
    CROSS_NARROW_BRIDGES,
    NO_DASH_WIN,
    COMPLETE_STAGES,
    COLLECT_COINS,
    JUMP_COUNT,
    WIN_RIVAL_RACE
}

data class MissionDef(
    val id: String,
    val type: MissionType,
    val titleEn: String,
    val titleAr: String,
    val titleFr: String,
    val target: Int,
    val rewardCoins: Int,
    val rewardXp: Int,
    val cadence: MissionCadence = MissionCadence.DAILY
) {
    fun localizedTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.AR -> titleAr
        AppLanguage.FR -> titleFr
        AppLanguage.EN -> titleEn
    }
}

data class LoginRewardDayDef(
    val dayNumber: Int,
    val rewardCoins: Int,
    val chestTier: ChestTier,
    val labelEn: String,
    val labelAr: String
)

data class SeasonalEventDef(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val featuredWorldId: Int,
    val featuredStageNumber: Int,
    val bannerColorHex: Long,
    val accentColorHex: Long,
    val bonusChestCoins: Int
) {
    fun localizedTitle(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) titleAr else titleEn

    fun localizedSubtitle(lang: AppLanguage): String =
        if (lang == AppLanguage.AR) subtitleAr else subtitleEn
}

data class XpMilestoneDef(
    val levelReq: Int,
    val title: String,
    val rewardCoins: Int,
    val unlockItemId: String? = null
)

object XpProgression {
    const val XP_PER_LEVEL = 1000

    fun levelFromXp(totalXp: Int): Int = 1 + (totalXp.coerceAtLeast(0) / XP_PER_LEVEL)
    fun xpIntoCurrentLevel(totalXp: Int): Int = totalXp.coerceAtLeast(0) % XP_PER_LEVEL
    fun progressFraction(totalXp: Int): Float = xpIntoCurrentLevel(totalXp).toFloat() / XP_PER_LEVEL

    fun nextMilestone(currentLevel: Int): XpMilestoneDef {
        return GameCatalog.xpMilestones.firstOrNull { it.levelReq > currentLevel }
            ?: GameCatalog.xpMilestones.last()
    }

    fun starsRequiredForWorld(worldId: Int): Int = when {
        worldId <= 1 -> 0
        else -> (worldId - 1) * 4
    }
}
