package com.example.game.model

import kotlin.math.sin
import kotlin.random.Random

object LevelCatalog {

    private val levelTitles: List<Pair<String, String>> = listOf(
        // World 1 - Ancient Greek Islands (1..5)
        "First Steps of Olympus" to "Warm up your jump across marble sky islands",
        "Gliding Agora Blocks" to "Time your leaps onto floating temple slabs",
        "Bronze Temple Hammers" to "Dodge swinging mallets on the sanctuary bridge",
        "Colonnade Sky Beam" to "Stay balanced across the marble causeway",
        "Aegean Gate Trial" to "Combine jumps, blocks, and hammers to open World 2!",
        // World 2 - Floating Temples (6..10)
        "Sanctuary Steps" to "Climb high above the waterfall clouds",
        "Sweeping Columns" to "Leap clean over spinning golden beams",
        "Titan Boulders" to "Weave past giant rolling marble spheres",
        "Acropolis Barriers" to "Vault over rhythmic temple walls",
        "Temple Portal Gate" to "Prove yourself atop the Floating Temples",
        // World 3 - Zeus Thunder Arena (11..15)
        "Lightning Dash Pads" to "Sprint across electric sapphire speed pads",
        "Thunder Mallets" to "Storm-charged hammers guard the arena!",
        "Vanishing Cloud Tiles" to "Watch out for disappearing storm slabs",
        "Thunderbolt Spans" to "Cross narrow golden bridges under lightning",
        "Champion of Zeus" to "Reach the Glowing Blue Portal Arch!",
        // World 4 - Golden Bridges (16..20)
        "Aurelian Walkways" to "Steady footing over gleaming royal gold",
        "Shifting Bullion" to "Ride gliding golden bullion platforms",
        "Sun Chariot Rollers" to "Dodge giant solar spheres on the bridge",
        "Crumbling Gilded Spans" to "Keep running before golden tiles drop!",
        "Golden Bridge Sovereign" to "Conquer the Royal Golden Gauntlet",
        // World 5 - Ancient Marble Platforms (21..25)
        "Polished Marble Roads" to "Master momentum on slick white marble",
        "Drifting Plinths" to "Hop across floating colonnade blocks",
        "Frost-Marble Mallets" to "Heavy stone mallets guard the ridge",
        "Shattering Friezes" to "Sprint across cracking marble bridges",
        "Marble Pantheon Gate" to "Claim the Crown of the Marble Sanctuary",
        // World 6 - Magical Portals (26..30)
        "Arcane Rift Decks" to "Don't miss a beat above the astral void",
        "Sapphire Flame Jets" to "Time your dash between magical fire braziers",
        "Teleporting Stones" to "Ride shifting amethyst rune rafts",
        "Starlight Causeways" to "Cross crumbling portal walkways",
        "Master of Portals" to "Step through the Grand Sapphire Gate",
        // World 7 - Mythological Gardens (31..35)
        "Hesperides Terraces" to "Navigate synchronized waterfall decks",
        "Golden Windmills" to "Spinning blades & sweeper arms!",
        "Pendulum Vines" to "Dodge dual swinging sanctuary pendulums",
        "Sacred Stream Belts" to "Fight against high-speed aqueduct currents",
        "Garden Guardian" to "Outrun the Mythological Gardens Citadel",
        // World 8 - Crystal Caves (36..40)
        "Sapphire Spans" to "Cross glowing crystal cavern bridges",
        "Swinging Stalactites" to "Duck and dive past giant crystal swingers",
        "Shattering Quartz" to "Cracking gemstone platforms ahead!",
        "Gliding Geodes" to "Ride floating amethyst crystal rafts",
        "Crystal Cavern King" to "Escape the Deep Sapphire Sanctum",
        // World 9 - Floating Castles (41..45)
        "Celestial Low-Gravity" to "Float higher across astral citadel pads!",
        "Sky Fortress Decks" to "Precision leaps in high-altitude orbit",
        "Arcane Beam Hurdles" to "Jump cleanly over sapphire laser beams",
        "Drifting Bastions" to "Hop across floating castle ramparts",
        "Citadel Conqueror" to "Blast through the Royal Sky Castle!",
        // World 10 - Ancient Ruins (46..50)
        "Titan Hammers" to "High-speed royal mallets guard the pantheon",
        "Colossal Sky Rafts" to "Massive marble rafts & bounce launchers",
        "Bridge of the Gods" to "Razor-thin golden bridges & sweeper beams",
        "Olympian Survival" to "Every mythological hazard stands in your way!",
        "MYTHORIA GRAND FINALE" to "Claim the Mythoria Crown of Champions!"
    )

    val allLevels: List<LevelDef> by lazy {
        (1..50).map { num -> buildCareerLevel(num) }
    }

    fun getLevel(number: Int): LevelDef = allLevels[(number - 1).coerceIn(0, 49)]

    fun getWorldForLevel(levelNumber: Int): WorldTheme {
        val worldIdx = ((levelNumber - 1) / 5).coerceIn(0, GameCatalog.worlds.lastIndex)
        return GameCatalog.worlds[worldIdx]
    }

    private fun buildCareerLevel(levelNum: Int): LevelDef {
        val world = getWorldForLevel(levelNum)
        val stageInWorld = ((levelNum - 1) % 5) + 1 // 1..5
        val (title, subtitle) = levelTitles[levelNum - 1]
        val rng = Random(levelNum * 7919 + 42)
        val speedScale = 1.0f + (levelNum - 1) * 0.022f
        val segmentCount = 5 + stageInWorld + (world.id / 3)

        val platforms = mutableListOf<PlatformDef>()
        val obstacles = mutableListOf<ObstacleDef>()
        val checkpoints = mutableListOf<CheckpointDef>()
        val coins = mutableListOf<CoinDef>()

        // Start Island
        platforms += PlatformDef(0f, 0f, 0f, 8f, 1.4f, 8f, PlatformType.NORMAL, world.platformPrimary)
        coins += CoinDef(-1.5f, 1.3f, -2.5f)
        coins += CoinDef(1.5f, 1.3f, -2.5f)

        var curX = 0f
        var curY = 0f
        var curZ = -6.5f
        var cpIndex = 1

        for (seg in 0 until segmentCount) {
            val isCheckpointSeg = seg > 0 && seg % 3 == 0 && seg < segmentCount - 1
            if (isCheckpointSeg) {
                platforms += PlatformDef(curX, curY, curZ - 3.0f, 7.0f, 1.3f, 6.5f, PlatformType.NORMAL, world.platformAccent)
                checkpoints += CheckpointDef(curX, curY + 0.65f, curZ - 3.0f, cpIndex++)
                coins += CoinDef(curX - 2f, curY + 1.5f, curZ - 3.0f)
                coins += CoinDef(curX + 2f, curY + 1.5f, curZ - 3.0f)
                curZ -= 8.0f
                continue
            }

            val pattern = pickPatternForLevel(levelNum, stageInWorld, seg, rng)
            when (pattern) {
                0 -> { // Stepping stones + gentle coins
                    val steps = if (levelNum <= 2) 3 else 4
                    for (s in 0 until steps) {
                        val dx = if (s % 2 == 0) -1.3f else 1.3f
                        val pType = if (world.id == 5) PlatformType.SLIPPERY else PlatformType.NORMAL
                        val col = if (s % 2 == 0) world.platformPrimary else world.platformSecondary
                        platforms += PlatformDef(curX + dx, curY, curZ - 2.2f, 4.4f, 1.2f, 4.4f, pType, col)
                        coins += CoinDef(curX + dx, curY + 1.4f, curZ - 2.2f)
                        curZ -= 5.8f
                    }
                }
                1 -> { // Moving blocks
                    val pLen = 5.0f
                    platforms += PlatformDef(
                        curX, curY, curZ - 3.0f, 4.6f, 1.1f, pLen,
                        PlatformType.MOVING, world.platformSecondary,
                        moveDx = 2.6f, speed = 1.15f * speedScale, phase = seg * 0.9f
                    )
                    coins += CoinDef(curX, curY + 1.5f, curZ - 3.0f)
                    curZ -= 7.2f
                    platforms += PlatformDef(
                        curX, curY, curZ - 3.0f, 4.6f, 1.1f, pLen,
                        PlatformType.MOVING, world.platformAccent,
                        moveDx = -2.6f, speed = 1.25f * speedScale, phase = seg * 0.9f + 1.5f
                    )
                    curZ -= 7.5f
                }
                2 -> { // Hammer Gauntlet Deck
                    val deckLen = 12f
                    val pType = if (world.id == 5) PlatformType.SLIPPERY else PlatformType.NORMAL
                    platforms += PlatformDef(curX, curY, curZ - deckLen * 0.5f, 7.2f, 1.2f, deckLen, pType, world.platformPrimary)
                    obstacles += ObstacleDef(curX, curY + 0.6f, curZ - 4.0f, ObstacleType.ROTATING_HAMMER, 1.0f, 1.6f * speedScale, 3.0f, seg * 0.7f)
                    if (levelNum >= 5) {
                        obstacles += ObstacleDef(curX, curY + 0.6f, curZ - 8.5f, ObstacleType.ROTATING_HAMMER, 1.0f, -1.8f * speedScale, 3.0f, seg * 1.4f)
                    }
                    coins += CoinDef(curX - 2.2f, curY + 1.4f, curZ - 6.2f)
                    coins += CoinDef(curX + 2.2f, curY + 1.4f, curZ - 6.2f)
                    curZ -= (deckLen + 2.2f)
                }
                3 -> { // Narrow Bridge + Side Coins
                    val bridgeW = if (levelNum < 10) 2.6f else 2.1f
                    val bridgeLen = 11f
                    platforms += PlatformDef(curX, curY, curZ - bridgeLen * 0.5f, bridgeW, 1.0f, bridgeLen, PlatformType.NARROW_BRIDGE, world.platformAccent)
                    for (c in 1..3) {
                        coins += CoinDef(curX, curY + 1.4f, curZ - c * 2.8f)
                    }
                    if (levelNum >= 10) {
                        obstacles += ObstacleDef(curX, curY + 1.8f, curZ - bridgeLen * 0.5f, ObstacleType.SWINGING_PENDULUM, 0.95f, 1.6f * speedScale, 2.8f, seg.toFloat())
                    }
                    curZ -= (bridgeLen + 2.2f)
                }
                4 -> { // Rotating Sweeper Beam Arena
                    val deckLen = 10f
                    platforms += PlatformDef(curX, curY, curZ - deckLen * 0.5f, 7.8f, 1.2f, deckLen, PlatformType.NORMAL, world.platformSecondary)
                    obstacles += ObstacleDef(curX, curY + 0.6f, curZ - deckLen * 0.5f, ObstacleType.ROTATING_BEAM, 1.1f, 1.55f * speedScale, 3.6f, seg * 0.8f)
                    coins += CoinDef(curX, curY + 2.3f, curZ - deckLen * 0.5f)
                    curZ -= (deckLen + 2.2f)
                }
                5 -> { // Rolling Balls Slope / Lane
                    val laneLen = 13f
                    platforms += PlatformDef(curX, curY, curZ - laneLen * 0.5f, 7.6f, 1.2f, laneLen, PlatformType.NORMAL, world.platformPrimary)
                    obstacles += ObstacleDef(curX - 1.8f, curY + 1.4f, curZ - 4.5f, ObstacleType.ROLLING_BALL, 1.0f, 1.7f * speedScale, 3.0f, 0f)
                    obstacles += ObstacleDef(curX + 1.8f, curY + 1.4f, curZ - 9.0f, ObstacleType.ROLLING_BALL, 1.0f, 1.9f * speedScale, 3.0f, 2.1f)
                    coins += CoinDef(curX, curY + 1.4f, curZ - 6.5f)
                    curZ -= (laneLen + 2.2f)
                }
                6 -> { // Bounce Pad + Elevated Sky Deck
                    platforms += PlatformDef(curX, curY, curZ - 2.5f, 5.4f, 1.1f, 5.0f, PlatformType.BOUNCE_PAD, world.platformAccent)
                    curY += 2.2f
                    curZ -= 7.8f
                    platforms += PlatformDef(curX, curY, curZ - 3.5f, 6.8f, 1.2f, 7.0f, PlatformType.NORMAL, world.platformPrimary)
                    coins += CoinDef(curX, curY + 1.5f, curZ - 3.5f)
                    curZ -= 9.0f
                }
                7 -> { // Disappearing / Falling Floor Challenge
                    val pType = if (seg % 2 == 0) PlatformType.DISAPPEARING else PlatformType.FALLING
                    for (k in 0..2) {
                        platforms += PlatformDef(
                            curX + (if (k == 1) 1.5f else -1.5f), curY, curZ - 2.2f,
                            4.2f, 1.0f, 4.2f, pType, world.platformSecondary, phase = k * 1.1f
                        )
                        coins += CoinDef(curX + (if (k == 1) 1.5f else -1.5f), curY + 1.3f, curZ - 2.2f)
                        curZ -= 5.8f
                    }
                }
                8 -> { // Speed Boost + Conveyor / Pusher Deck
                    platforms += PlatformDef(curX, curY, curZ - 3.5f, 6.0f, 1.1f, 7.0f, PlatformType.SPEED_BOOST, world.platformAccent, dirZ = -1f)
                    curZ -= 8.8f
                    val nextType = if (world.id == 7) PlatformType.CONVEYOR else PlatformType.NORMAL
                    platforms += PlatformDef(curX, curY, curZ - 5.0f, 7.2f, 1.2f, 10.0f, nextType, world.platformPrimary, dirX = if (seg % 2 == 0) 1f else -1f, dirZ = 0f)
                    obstacles += ObstacleDef(curX, curY + 0.6f, curZ - 5.0f, ObstacleType.PUSHER, 1.0f, 1.8f * speedScale, 2.8f, seg.toFloat())
                    curZ -= 12.0f
                }
                else -> { // Hazard Special (Fire Jets / Lasers / Spinning Blades / Giant Swinger)
                    val deckLen = 12f
                    platforms += PlatformDef(curX, curY, curZ - deckLen * 0.5f, 7.4f, 1.2f, deckLen, PlatformType.NORMAL, world.platformSecondary)
                    val obsType = when (world.id) {
                        6 -> ObstacleType.FIRE_JET
                        7 -> ObstacleType.SPINNING_BLADE
                        9 -> ObstacleType.LASER_BEAM
                        10 -> ObstacleType.GIANT_SWINGER
                        else -> ObstacleType.JUMPING_BLOCK
                    }
                    obstacles += ObstacleDef(curX, curY + 0.6f, curZ - 4.0f, obsType, 1.05f, 1.6f * speedScale, 3.0f, 0f)
                    obstacles += ObstacleDef(curX, curY + 0.6f, curZ - 8.2f, ObstacleType.MOVING_WALL, 1.0f, 1.7f * speedScale, 2.8f, 1.4f)
                    coins += CoinDef(curX, curY + 1.5f, curZ - 6.0f)
                    curZ -= (deckLen + 2.2f)
                }
            }
        }

        // Finish Island & Arch
        val finishZ = curZ - 5.0f
        platforms += PlatformDef(curX, curY, finishZ, 11f, 1.6f, 10f, PlatformType.FINISH, world.platformAccent)
        for (i in -2..2) {
            coins += CoinDef(curX + i * 1.4f, curY + 1.5f, finishZ + 2.2f)
        }

        val parTime = 24f + segmentCount * 4.5f
        val rewardCoins = 45 + levelNum * 6 + stageInWorld * 10

        return LevelDef(
            number = levelNum,
            worldId = world.id,
            title = title,
            subtitle = subtitle,
            parTimeSec = parTime,
            rewardCoins = rewardCoins,
            platforms = platforms,
            obstacles = obstacles,
            checkpoints = checkpoints,
            coins = coins,
            startX = 0f,
            startY = 1.6f,
            startZ = 1.5f,
            finishZ = finishZ
        )
    }

    private fun pickPatternForLevel(levelNum: Int, stage: Int, seg: Int, rng: Random): Int {
        return when (levelNum) {
            1 -> if (seg % 2 == 0) 0 else 6 // First Steps: easy islands + fun bounce pad
            2 -> if (seg % 2 == 0) 1 else 0 // Moving Blocks
            3 -> if (seg % 2 == 0) 2 else 0 // Small Hammers
            4 -> if (seg % 2 == 0) 3 else 1 // Narrow Bridge
            5 -> listOf(0, 1, 2, 3, 4, 6)[seg % 6] // First Final Challenge
            else -> {
                val pool = when ((levelNum - 1) / 5) {
                    1 -> listOf(0, 1, 4, 5, 6, 9) // Colorful Sky
                    2 -> listOf(8, 2, 7, 3, 6)    // Candy Land
                    3 -> listOf(0, 1, 5, 7, 3)    // Desert Danger
                    4 -> listOf(0, 1, 2, 7, 6)    // Frozen World
                    5 -> listOf(9, 7, 1, 5, 2)    // Volcano World
                    6 -> listOf(8, 9, 4, 2, 6)    // Mechanical City
                    7 -> listOf(3, 9, 7, 1, 5)    // Jungle Adventure
                    8 -> listOf(6, 9, 1, 4, 7)    // Space World
                    else -> listOf(2, 3, 4, 5, 6, 7, 8, 9) // Legendary Sky
                }
                pool[(seg + stage + rng.nextInt(pool.size)) % pool.size]
            }
        }
    }

    fun generateDailyChallengeLevel(epochDay: Long): LevelDef {
        val world = GameCatalog.worlds[(epochDay % GameCatalog.worlds.size).toInt().coerceAtLeast(0)]
        val rng = Random(epochDay * 31337L)
        val platforms = mutableListOf<PlatformDef>()
        val obstacles = mutableListOf<ObstacleDef>()
        val checkpoints = mutableListOf<CheckpointDef>()
        val coins = mutableListOf<CoinDef>()

        platforms += PlatformDef(0f, 0f, 0f, 8f, 1.4f, 8f, PlatformType.NORMAL, world.platformPrimary)
        var curY = 0f
        var curZ = -6.5f
        for (seg in 0 until 8) {
            if (seg == 4) {
                platforms += PlatformDef(0f, curY, curZ - 3f, 7f, 1.2f, 6f, PlatformType.NORMAL, world.platformAccent)
                checkpoints += CheckpointDef(0f, curY + 0.6f, curZ - 3f, 1)
                curZ -= 7.8f
                continue
            }
            val p = rng.nextInt(7)
            when (p) {
                0 -> {
                    platforms += PlatformDef(0f, curY, curZ - 5f, 7.2f, 1.2f, 10f, PlatformType.NORMAL, world.platformPrimary)
                    obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 5f, ObstacleType.ROTATING_HAMMER, 1.1f, 2.0f, 3.0f, seg.toFloat())
                    coins += CoinDef(0f, curY + 1.5f, curZ - 5f)
                    curZ -= 12.2f
                }
                1 -> {
                    platforms += PlatformDef(0f, curY, curZ - 3f, 4.6f, 1.1f, 5.2f, PlatformType.MOVING, world.platformSecondary, moveDx = 2.8f, speed = 1.5f)
                    coins += CoinDef(0f, curY + 1.5f, curZ - 3f)
                    curZ -= 7.6f
                }
                2 -> {
                    platforms += PlatformDef(0f, curY, curZ - 5f, 2.4f, 1.0f, 10f, PlatformType.NARROW_BRIDGE, world.platformAccent)
                    coins += CoinDef(0f, curY + 1.4f, curZ - 5f)
                    curZ -= 12f
                }
                3 -> {
                    platforms += PlatformDef(0f, curY, curZ - 2.5f, 5.4f, 1.1f, 5f, PlatformType.BOUNCE_PAD, world.platformAccent)
                    curY += 2.0f
                    curZ -= 7.5f
                    platforms += PlatformDef(0f, curY, curZ - 3.2f, 6.8f, 1.2f, 6.5f, PlatformType.NORMAL, world.platformSecondary)
                    curZ -= 8.5f
                }
                4 -> {
                    platforms += PlatformDef(0f, curY, curZ - 5f, 7.6f, 1.2f, 10f, PlatformType.NORMAL, world.platformPrimary)
                    obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 5f, ObstacleType.ROTATING_BEAM, 1.1f, 1.9f, 3.6f, seg.toFloat())
                    curZ -= 12.2f
                }
                5 -> {
                    platforms += PlatformDef(0f, curY, curZ - 5f, 7.6f, 1.2f, 10f, PlatformType.NORMAL, world.platformSecondary)
                    obstacles += ObstacleDef(0f, curY + 1.4f, curZ - 5f, ObstacleType.ROLLING_BALL, 1.05f, 2.0f, 3.0f, 0f)
                    curZ -= 12.2f
                }
                else -> {
                    platforms += PlatformDef(0f, curY, curZ - 3.5f, 6f, 1.1f, 7f, PlatformType.SPEED_BOOST, world.platformAccent, dirZ = -1f)
                    curZ -= 9f
                }
            }
        }
        val finishZ = curZ - 5f
        platforms += PlatformDef(0f, curY, finishZ, 11f, 1.6f, 10f, PlatformType.FINISH, world.platformAccent)
        return LevelDef(
            number = 100,
            worldId = world.id,
            title = "Daily Challenge #${epochDay % 1000}",
            subtitle = "${world.name} • 2x Coin Bonus!",
            parTimeSec = 55f,
            rewardCoins = 180,
            platforms = platforms,
            obstacles = obstacles,
            checkpoints = checkpoints,
            coins = coins,
            finishZ = finishZ
        )
    }

    data class EndlessSegmentResult(
        val platforms: List<PlatformDef>,
        val obstacles: List<ObstacleDef>,
        val coins: List<CoinDef>,
        val nextY: Float,
        val nextZ: Float
    )

    fun generateEndlessSegment(
        segIndex: Int,
        tier: Int,
        startY: Float,
        startZ: Float,
        rng: Random
    ): EndlessSegmentResult {
        val platforms = mutableListOf<PlatformDef>()
        val obstacles = mutableListOf<ObstacleDef>()
        val coins = mutableListOf<CoinDef>()
        val wTheme = GameCatalog.worlds[(tier - 1 + segIndex / 3) % GameCatalog.worlds.size]
        val localSpeed = 1.0f + (tier - 1) * 0.14f
        var curY = startY
        var curZ = startZ

        // Available obstacle-course patterns unlock progressively by tier
        val maxPattern = when {
            tier <= 1 -> 3   // Basic hammers, stepping stones, rolling balls
            tier == 2 -> 5   // + Moving blocks, rotating beams, narrow bridges
            tier == 3 -> 7   // + Disappearing tiles, spinning X-blades, jumping blocks
            tier == 4 -> 9   // + Fire jets, conveyor pushers, swinging pendulums
            else -> 11       // + Laser beams, slippery combo decks, giant swingers
        }
        val pattern = (segIndex + rng.nextInt(maxPattern)) % maxPattern

        when (pattern) {
            0 -> { // Hammer Deck (Dual counter-rotating hammers at Tier 3+)
                val len = 11f
                platforms += PlatformDef(0f, curY, curZ - len * 0.5f, 7.2f, 1.2f, len, PlatformType.NORMAL, wTheme.platformPrimary)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 3.6f, ObstacleType.ROTATING_HAMMER, 1.0f, 1.55f * localSpeed, 3.0f, segIndex * 0.7f)
                if (tier >= 3) {
                    obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 7.8f, ObstacleType.ROTATING_HAMMER, 1.05f, -1.85f * localSpeed, 3.1f, segIndex * 1.4f)
                }
                coins += CoinDef(0f, curY + 1.5f, curZ - len * 0.5f)
                curZ -= (len + 2.2f)
            }
            1 -> { // Stepping Islands + Coins
                val steps = if (tier >= 4) 3 else 2
                for (s in 0 until steps) {
                    val dx = if ((s + segIndex) % 2 == 0) -1.5f else 1.5f
                    val pType = if (tier >= 5 && s == 1) PlatformType.SLIPPERY else PlatformType.NORMAL
                    platforms += PlatformDef(dx, curY, curZ - 2.3f, 4.3f, 1.1f, 4.3f, pType, wTheme.platformSecondary)
                    coins += CoinDef(dx, curY + 1.4f, curZ - 2.3f)
                    curZ -= 5.8f
                }
            }
            2 -> { // Rolling Boulder Lane (2 boulders at Tier 2+)
                val len = 12f
                platforms += PlatformDef(0f, curY, curZ - len * 0.5f, 7.4f, 1.2f, len, PlatformType.NORMAL, wTheme.platformSecondary)
                obstacles += ObstacleDef(-1.6f, curY + 1.4f, curZ - 4.2f, ObstacleType.ROLLING_BALL, 1.0f, 1.75f * localSpeed, 2.9f, 0f)
                if (tier >= 2) {
                    obstacles += ObstacleDef(1.6f, curY + 1.4f, curZ - 8.2f, ObstacleType.ROLLING_BALL, 1.05f, 1.95f * localSpeed, 2.9f, 2.2f)
                }
                coins += CoinDef(0f, curY + 1.4f, curZ - 6.0f)
                curZ -= (len + 2.2f)
            }
            3 -> { // Moving Gliders
                val amp = (2.2f + tier * 0.25f).coerceAtMost(3.3f)
                platforms += PlatformDef(0f, curY, curZ - 3.0f, 4.6f, 1.1f, 5.0f, PlatformType.MOVING, wTheme.platformAccent, moveDx = amp, speed = 1.25f * localSpeed, phase = segIndex * 0.9f)
                coins += CoinDef(0f, curY + 1.5f, curZ - 3.0f)
                curZ -= 7.4f
                if (tier >= 2) {
                    platforms += PlatformDef(0f, curY, curZ - 3.0f, 4.4f, 1.1f, 4.8f, PlatformType.MOVING, wTheme.platformSecondary, moveDx = -amp, speed = 1.35f * localSpeed, phase = segIndex * 0.9f + 1.6f)
                    curZ -= 7.4f
                }
            }
            4 -> { // Narrow Bridge + Pendulum at higher tiers
                val bw = (2.7f - tier * 0.12f).coerceAtLeast(1.9f)
                val len = 10f
                platforms += PlatformDef(0f, curY, curZ - len * 0.5f, bw, 1.0f, len, PlatformType.NARROW_BRIDGE, wTheme.platformAccent)
                if (tier >= 3) {
                    obstacles += ObstacleDef(0f, curY + 1.7f, curZ - len * 0.5f, ObstacleType.SWINGING_PENDULUM, 1.0f, 1.65f * localSpeed, 2.9f, segIndex.toFloat())
                }
                coins += CoinDef(0f, curY + 1.4f, curZ - len * 0.5f)
                curZ -= (len + 2.2f)
            }
            5 -> { // Disappearing / Falling Floor Sprint
                val pType = if (segIndex % 2 == 0) PlatformType.DISAPPEARING else PlatformType.FALLING
                for (k in 0..2) {
                    val dx = if (k == 1) 1.4f else -1.4f
                    platforms += PlatformDef(dx, curY, curZ - 2.2f, 4.1f, 1.0f, 4.1f, pType, wTheme.platformSecondary, phase = k * 1.15f)
                    coins += CoinDef(dx, curY + 1.35f, curZ - 2.2f)
                    curZ -= 5.8f
                }
            }
            6 -> { // Spinning Blades + Jumping Block Arena
                val len = 11.5f
                platforms += PlatformDef(0f, curY, curZ - len * 0.5f, 7.6f, 1.2f, len, PlatformType.NORMAL, wTheme.platformPrimary)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 4.0f, ObstacleType.SPINNING_BLADE, 1.05f, 1.6f * localSpeed, 3.3f, segIndex.toFloat())
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 8.2f, ObstacleType.JUMPING_BLOCK, 1.0f, 1.85f * localSpeed, 2.5f, 1.1f)
                coins += CoinDef(0f, curY + 2.2f, curZ - 4.0f)
                curZ -= (len + 2.2f)
            }
            7 -> { // Bounce Pad Elevation Shift
                platforms += PlatformDef(0f, curY, curZ - 2.6f, 5.2f, 1.1f, 5.0f, PlatformType.BOUNCE_PAD, wTheme.platformAccent)
                curY = (curY + 2.0f).coerceAtMost(8.0f)
                curZ -= 7.8f
                platforms += PlatformDef(0f, curY, curZ - 3.5f, 6.8f, 1.2f, 7.0f, PlatformType.NORMAL, wTheme.platformPrimary)
                if (tier >= 4) {
                    obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 3.5f, ObstacleType.ROTATING_BEAM, 1.05f, 1.65f * localSpeed, 3.2f, 0f)
                }
                coins += CoinDef(0f, curY + 1.5f, curZ - 3.5f)
                curZ -= 9.0f
            }
            8 -> { // Conveyor + Pusher + Fire Jet Gauntlet
                val len = 11.5f
                platforms += PlatformDef(0f, curY, curZ - len * 0.5f, 7.2f, 1.2f, len, PlatformType.CONVEYOR, wTheme.platformSecondary, dirX = if (segIndex % 2 == 0) 1f else -1f, dirZ = 0f)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 3.8f, ObstacleType.PUSHER, 1.0f, 1.85f * localSpeed, 2.8f, segIndex.toFloat())
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 8.0f, ObstacleType.FIRE_JET, 1.05f, 1.7f * localSpeed, 2.5f, 0.8f)
                coins += CoinDef(0f, curY + 1.5f, curZ - 6.0f)
                curZ -= (len + 2.2f)
            }
            9 -> { // Speed Boost + Laser Hurdles
                platforms += PlatformDef(0f, curY, curZ - 3.5f, 6.0f, 1.1f, 7.0f, PlatformType.SPEED_BOOST, wTheme.platformAccent, dirZ = -1f)
                curZ -= 8.8f
                platforms += PlatformDef(0f, curY, curZ - 5.0f, 7.4f, 1.2f, 10.0f, PlatformType.NORMAL, wTheme.platformPrimary)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 3.5f, ObstacleType.LASER_BEAM, 1.0f, 1.6f * localSpeed, 3.4f, 0f)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 7.5f, ObstacleType.MOVING_WALL, 1.05f, 1.9f * localSpeed, 2.8f, 1.5f)
                coins += CoinDef(0f, curY + 1.6f, curZ - 5.5f)
                curZ -= 12.0f
            }
            else -> { // Tier 5+ Legendary Triple Combo Deck
                val len = 14f
                val pType = if (segIndex % 2 == 0) PlatformType.SLIPPERY else PlatformType.NORMAL
                platforms += PlatformDef(0f, curY, curZ - len * 0.5f, 7.8f, 1.2f, len, pType, wTheme.platformPrimary)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 3.8f, ObstacleType.GIANT_SWINGER, 1.1f, 1.65f * localSpeed, 3.0f, 0f)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 7.5f, ObstacleType.ROTATING_HAMMER, 1.1f, -2.0f * localSpeed, 3.2f, 1.0f)
                obstacles += ObstacleDef(0f, curY + 0.6f, curZ - 11.2f, ObstacleType.LASER_BEAM, 1.05f, 1.75f * localSpeed, 3.6f, 0.5f)
                coins += CoinDef(-2.2f, curY + 1.5f, curZ - 7.5f)
                coins += CoinDef(2.2f, curY + 1.5f, curZ - 7.5f)
                curZ -= (len + 2.4f)
            }
        }
        return EndlessSegmentResult(platforms, obstacles, coins, curY, curZ)
    }

    fun generateEndlessLevel(seed: Long): LevelDef {
        val world = GameCatalog.worlds[1]
        val rng = Random(seed)
        val platforms = mutableListOf<PlatformDef>()
        val obstacles = mutableListOf<ObstacleDef>()
        val coins = mutableListOf<CoinDef>()

        platforms += PlatformDef(0f, 0f, 0f, 8f, 1.4f, 8f, PlatformType.NORMAL, world.platformPrimary)
        var curY = 0f
        var curZ = -6.5f
        for (seg in 0 until 8) {
            val tier = 1 + (seg / 4)
            val res = generateEndlessSegment(seg, tier, curY, curZ, rng)
            platforms += res.platforms
            obstacles += res.obstacles
            coins += res.coins
            curY = res.nextY
            curZ = res.nextZ
        }
        return LevelDef(
            number = 200,
            worldId = 2,
            title = "Endless Survival",
            subtitle = "Survive as long as possible!",
            parTimeSec = 120f,
            rewardCoins = 250,
            platforms = platforms,
            obstacles = obstacles,
            checkpoints = emptyList(),
            coins = coins,
            finishZ = curZ
        )
    }

    fun generatePracticeLevel(): LevelDef {
        val world = GameCatalog.worlds[0]
        val platforms = listOf(
            PlatformDef(0f, 0f, 0f, 9f, 1.4f, 9f, PlatformType.NORMAL, world.platformPrimary),
            PlatformDef(0f, 0f, -10f, 8f, 1.2f, 9f, PlatformType.SPEED_BOOST, world.platformAccent, dirZ = -1f),
            PlatformDef(0f, 0f, -21f, 8f, 1.2f, 10f, PlatformType.NORMAL, world.platformSecondary),
            PlatformDef(0f, 0f, -32f, 5f, 1.1f, 6f, PlatformType.BOUNCE_PAD, world.platformAccent),
            PlatformDef(0f, 2.2f, -41f, 8f, 1.2f, 9f, PlatformType.NORMAL, world.platformPrimary),
            PlatformDef(0f, 2.2f, -52f, 2.8f, 1.0f, 9f, PlatformType.NARROW_BRIDGE, world.platformAccent),
            PlatformDef(0f, 2.2f, -64f, 10f, 1.5f, 10f, PlatformType.FINISH, world.platformAccent)
        )
        val obstacles = listOf(
            ObstacleDef(0f, 0.6f, -21f, ObstacleType.ROTATING_HAMMER, 1.0f, 1.3f, 2.8f, 0f),
            ObstacleDef(0f, 2.8f, -41f, ObstacleType.ROTATING_BEAM, 1.0f, 1.3f, 3.2f, 0f)
        )
        val checkpoints = listOf(CheckpointDef(0f, 2.8f, -39f, 1))
        val coins = listOf(
            CoinDef(0f, 1.4f, -10f),
            CoinDef(-2f, 1.4f, -21f),
            CoinDef(2f, 1.4f, -21f),
            CoinDef(0f, 3.6f, -52f)
        )
        return LevelDef(
            number = 0,
            worldId = 1,
            title = "Practice Arena",
            subtitle = "Master Jump, Dive & Dash freely!",
            parTimeSec = 45f,
            rewardCoins = 30,
            platforms = platforms,
            obstacles = obstacles,
            checkpoints = checkpoints,
            coins = coins,
            finishZ = -64f
        )
    }
}
