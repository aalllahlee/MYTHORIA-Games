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
    LASER_BEAM
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
    val index: Int
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
    val startX: Float = 0f,
    val startY: Float = 1.5f,
    val startZ: Float = 0f,
    val finishZ: Float
)

enum class HeadAccessoryStyle {
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
    val previewColor: Rgba
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

    val skins: List<SkinDef> = listOf(
        SkinDef("red_hero", "Young Greek Hero", "Heroic crimson & gold tunic bearing the Mythoria crest.", 0, 1, Rgba.fromHex(0xE62E4D), Rgba.fromHex(0x1E2949), Rgba.fromHex(0xFFCAA8), Rgba.fromHex(0x5C2C16), Rgba.fromHex(0xD97706), Rgba.fromHex(0xFFD22B), HeadAccessoryStyle.SPIKY_HAIR),
        SkinDef("blue_ninja", "Shadow Warrior", "Stealthy night-temple guardian infused with sapphire energy.", 150, 1, Rgba.fromHex(0x1D3557), Rgba.fromHex(0x111827), Rgba.fromHex(0xFFCAA8), Rgba.fromHex(0x00E5FF), Rgba.fromHex(0x111827), Rgba.fromHex(0x00D4FF), HeadAccessoryStyle.NINJA_HOOD),
        SkinDef("golden_champ", "Golden Warrior", "Radiant golden plate armor forged in the halls of Olympus.", 350, 3, Rgba.fromHex(0xFFB703), Rgba.fromHex(0x7C4D00), Rgba.fromHex(0xFFD6B0), Rgba.fromHex(0xFFE066), Rgba.fromHex(0xFB8500), Rgba.fromHex(0x00E5FF), HeadAccessoryStyle.CROWN_HELM),
        SkinDef("panda", "Mythical Explorer", "Cheerful sky-temple explorer who leaps across waterfall islands.", 250, 2, Rgba.fromHex(0x1D7AFC), Rgba.fromHex(0x222634), Rgba.fromHex(0xF8FAFC), Rgba.fromHex(0xFFD22B), Rgba.fromHex(0x1E293B), Rgba.fromHex(0xFFD22B), HeadAccessoryStyle.PANDA_EARS),
        SkinDef("robot", "Automaton Sentinel", "Ancient bronze & sapphire temple colossus built by Hephaestus.", 400, 4, Rgba.fromHex(0x64748B), Rgba.fromHex(0x334155), Rgba.fromHex(0xCBD5E1), Rgba.fromHex(0x00E5FF), Rgba.fromHex(0x00B4D8), Rgba.fromHex(0x00E5FF), HeadAccessoryStyle.ROBOT_ANTENNA),
        SkinDef("astronaut", "Starlight Oracle", "Low-gravity astral voyager of the Floating Castles.", 500, 5, Rgba.fromHex(0xF1F5F9), Rgba.fromHex(0xCBD5E1), Rgba.fromHex(0x0F172A), Rgba.fromHex(0xFFD22B), Rgba.fromHex(0x0077FF), Rgba.fromHex(0x00E5FF), HeadAccessoryStyle.ASTRONAUT_HELMET),
        SkinDef("pirate", "Aegean Corsair", "Fearless sky captain sailing between Ancient Greek Islands.", 450, 5, Rgba.fromHex(0x9B2226), Rgba.fromHex(0x2B2D42), Rgba.fromHex(0xF5C29B), Rgba.fromHex(0xD90429), Rgba.fromHex(0x4A2C16), Rgba.fromHex(0xFFD166), HeadAccessoryStyle.PIRATE_BANDANA),
        SkinDef("knight", "Ancient Knight", "Stalwart marble-and-silver protector with gleaming royal crest.", 600, 7, Rgba.fromHex(0x94A3B8), Rgba.fromHex(0x475569), Rgba.fromHex(0x334155), Rgba.fromHex(0x00D4FF), Rgba.fromHex(0x64748B), Rgba.fromHex(0xFFD22B), HeadAccessoryStyle.KNIGHT_VISOR),
        SkinDef("football_star", "Olympian Sprinter", "Hermes-blessed runner with winged footwork.", 300, 2, Rgba.fromHex(0x10B981), Rgba.fromHex(0xFFFFFF), Rgba.fromHex(0xF4BE96), Rgba.fromHex(0xFFD22B), Rgba.fromHex(0x10B981), Rgba.fromHex(0xFFD22B), HeadAccessoryStyle.HEADBAND),
        SkinDef("ice_warrior", "Thunder Champion", "Storm-forged champion wielding the electric blue fury of Zeus.", 700, 10, Rgba.fromHex(0x00B4D8), Rgba.fromHex(0x03045E), Rgba.fromHex(0xCAF0F8), Rgba.fromHex(0x00E5FF), Rgba.fromHex(0x48CAE4), Rgba.fromHex(0xFFD22B), HeadAccessoryStyle.ICE_HORNS),
        SkinDef("fire_hero", "Solar Phoenix", "Blazing sacred flame runner fueled by golden temple braziers.", 750, 15, Rgba.fromHex(0xFF4800), Rgba.fromHex(0x370617), Rgba.fromHex(0xFFBA7A), Rgba.fromHex(0xFFAA00), Rgba.fromHex(0xDC2F02), Rgba.fromHex(0xFFE66D), HeadAccessoryStyle.FLAME_CREST),
        SkinDef("alien", "Portal Mystic", "Arcane guardian from beyond the glowing sapphire gate.", 850, 20, Rgba.fromHex(0x7209B7), Rgba.fromHex(0x3A0CA3), Rgba.fromHex(0x52E588), Rgba.fromHex(0x00F5D4), Rgba.fromHex(0x4CC9F0), Rgba.fromHex(0xF72585), HeadAccessoryStyle.ALIEN_ANTENNAE),
        SkinDef("royal_king", "Royal Guardian", "Majestic sovereign wearing the 5-peak sapphire-studded Golden Crown.", 1000, 25, Rgba.fromHex(0x1E3A8A), Rgba.fromHex(0x0A122C), Rgba.fromHex(0xFFCAA8), Rgba.fromHex(0xFFD22B), Rgba.fromHex(0xFFD22B), Rgba.fromHex(0x00E5FF), HeadAccessoryStyle.ROYAL_CROWN),
        SkinDef("shadow_runner", "Obsidian Titan", "Midnight phantom glowing with amethyst & sapphire runes.", 1200, 35, Rgba.fromHex(0x18181B), Rgba.fromHex(0x09090B), Rgba.fromHex(0x27272A), Rgba.fromHex(0xC026D3), Rgba.fromHex(0xA21CAF), Rgba.fromHex(0x00E5FF), HeadAccessoryStyle.SHADOW_COWL),
        SkinDef("mythoria_legend", "Celestial Hero", "Ultimate MYTHORIA games champion with golden wings & sapphire aura.", 1500, 1, Rgba.fromHex(0xFFD22B), Rgba.fromHex(0x0A2463), Rgba.fromHex(0xFFCAA8), Rgba.fromHex(0x00E5FF), Rgba.fromHex(0xFFD22B), Rgba.fromHex(0x00E5FF), HeadAccessoryStyle.LEGEND_HALO)
    )

    val shopItems: List<ShopItemDef> = listOf(
        // Abilities
        ShopItemDef("ability_dash", ShopCategory.ABILITIES, "Thunder Dash", "Unlocks a mid-air & ground lightning Dash button with cooldown.", 200, Rgba.fromHex(0x00D4FF)),
        // Hats
        ShopItemDef("hat_none", ShopCategory.HATS, "Default Head", "Use your character's natural hairstyle or helmet.", 0, Rgba.fromHex(0x94A3B8)),
        ShopItemDef("hat_crown", ShopCategory.HATS, "Mythoria Royal Crown", "Gleaming 5-peak golden crown studded with blue sapphires.", 300, Rgba.fromHex(0xFFD22B)),
        ShopItemDef("hat_headband", ShopCategory.HATS, "Olympian Laurel Band", "Bold crimson & gold band for temple champions.", 120, Rgba.fromHex(0xFF3B5C)),
        ShopItemDef("hat_halo", ShopCategory.HATS, "Celestial Halo", "Floating golden solar ring above your head.", 450, Rgba.fromHex(0xFFF05A)),
        ShopItemDef("hat_visor", ShopCategory.HATS, "Sapphire Visor", "Glowing arcane sapphire crystal visor.", 260, Rgba.fromHex(0x00E5FF)),
        // Shoes
        ShopItemDef("shoe_default", ShopCategory.SHOES, "Hero Sandals", "Balanced everyday sky-runner footwear.", 0, Rgba.fromHex(0xFF3B5C)),
        ShopItemDef("shoe_gold", ShopCategory.SHOES, "Winged Golden Greaves", "Luxurious Hermes-inspired golden winged boots.", 220, Rgba.fromHex(0xFFD22B)),
        ShopItemDef("shoe_rocket", ShopCategory.SHOES, "Sapphire Storm Boots", "Vibrant electric-blue lightning boots.", 320, Rgba.fromHex(0x00D4FF)),
        ShopItemDef("shoe_shadow", ShopCategory.SHOES, "Amethyst Greaves", "Sleek obsidian-violet temple runner boots.", 360, Rgba.fromHex(0xC83BFF)),
        // Trails
        ShopItemDef("trail_dust", ShopCategory.TRAILS, "Cloud Mist", "Clean soft sky-cloud running puffs.", 0, Rgba.fromHex(0xFFFFFF)),
        ShopItemDef("trail_gold", ShopCategory.TRAILS, "Mythoria Gold Dust", "Shimmering golden stardust behind every step.", 240, Rgba.fromHex(0xFFD22B)),
        ShopItemDef("trail_cyan", ShopCategory.TRAILS, "Zeus Lightning", "Electric sapphire lightning streak trail.", 280, Rgba.fromHex(0x00E5FF)),
        ShopItemDef("trail_flame", ShopCategory.TRAILS, "Brazier Flame", "Sacred golden-orange fire embers while sprinting.", 340, Rgba.fromHex(0xFF5400)),
        ShopItemDef("trail_rainbow", ShopCategory.TRAILS, "Bifrost Prism", "Mythical celestial prism trail across the sky.", 500, Rgba.fromHex(0xFF5CA8)),
        // Victory
        ShopItemDef("victory_dance", ShopCategory.VICTORY, "Heroic Salute", "Cheerful celebratory victory jump & wave.", 0, Rgba.fromHex(0x2EE58D)),
        ShopItemDef("victory_spin", ShopCategory.VICTORY, "Olympian Spin", "360-degree airborne hero spin salute.", 180, Rgba.fromHex(0xFFD22B)),
        ShopItemDef("victory_flip", ShopCategory.VICTORY, "Royal Backflip", "Acrobatic victory flip at the temple portal arch.", 350, Rgba.fromHex(0xC83BFF))
    )

    val achievements: List<AchievementDef> = listOf(
        AchievementDef("ach_first_win", "First Steps Victory", "Complete your first level in Career Mode.", 1, 100),
        AchievementDef("ach_world_1", "Island Conqueror", "Complete all 5 levels of World 1.", 5, 250),
        AchievementDef("ach_coins_500", "Treasure Hunter", "Collect 500 total coins across any modes.", 500, 150),
        AchievementDef("ach_skins_3", "Style Collector", "Unlock 3 different character skins.", 3, 200),
        AchievementDef("ach_dash", "Speed Burst", "Unlock the Turbo Dash ability in the Shop.", 1, 100),
        AchievementDef("ach_endless_100", "Sky Survivor", "Reach 100m distance in Endless Mode.", 100, 250),
        AchievementDef("ach_daily", "Daily Challenger", "Complete a Daily Challenge run.", 1, 200),
        AchievementDef("ach_10_wins", "Obstacle Master", "Win 10 total runs across the game.", 10, 400)
    )

    val missions: List<MissionDef> = listOf(
        MissionDef(
            id = "mis_stages",
            type = MissionType.COMPLETE_STAGES,
            titleEn = "Complete 3 Stages",
            titleAr = "أكمل 3 مراحل",
            titleFr = "Terminer 3 niveaux",
            target = 3,
            rewardCoins = 500,
            rewardXp = 450
        ),
        MissionDef(
            id = "mis_coins",
            type = MissionType.COLLECT_COINS,
            titleEn = "Collect 100 Coins",
            titleAr = "اجمع 100 عملة",
            titleFr = "Collecter 100 pièces",
            target = 100,
            rewardCoins = 400,
            rewardXp = 350
        ),
        MissionDef(
            id = "mis_jumps",
            type = MissionType.JUMP_COUNT,
            titleEn = "Jump 25 Times",
            titleAr = "اقفز 25 مرة",
            titleFr = "Sauter 25 fois",
            target = 25,
            rewardCoins = 300,
            rewardXp = 300
        ),
        MissionDef(
            id = "mis_rival",
            type = MissionType.WIN_RIVAL_RACE,
            titleEn = "Win an AI Rival Race",
            titleAr = "فز في سباق المنافسين",
            titleFr = "Gagner une course IA",
            target = 1,
            rewardCoins = 600,
            rewardXp = 550
        )
    )

    val xpMilestones: List<XpMilestoneDef> = listOf(
        XpMilestoneDef(5, "Champion Band", 250, "hat_headband"),
        XpMilestoneDef(10, "500 Coin Chest", 500, null),
        XpMilestoneDef(15, "Cyber Visor", 600, "hat_visor"),
        XpMilestoneDef(20, "Gold Sparkle Trail", 800, "trail_gold"),
        XpMilestoneDef(25, "Star Halo", 1000, "hat_halo"),
        XpMilestoneDef(30, "Golden Crown", 1500, "hat_crown")
    )
}

enum class MissionType {
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
    val rewardXp: Int
) {
    fun localizedTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.AR -> titleAr
        AppLanguage.FR -> titleFr
        AppLanguage.EN -> titleEn
    }
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
