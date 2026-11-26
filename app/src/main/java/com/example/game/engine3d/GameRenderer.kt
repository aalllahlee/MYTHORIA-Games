package com.example.game.engine3d

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.model.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(
    private val world: GamePhysicsWorld,
    private val onHudTick: (HudSnapshot) -> Unit
) : GLSurfaceView.Renderer {

    private lateinit var cartoonShader: CartoonShader
    private lateinit var skyShader: SkyShader
    private lateinit var shadowShader: ShadowShader

    private lateinit var boxMesh: GlMesh
    private lateinit var sphereMesh: GlMesh
    private lateinit var cylMesh: GlMesh
    private lateinit var coneMesh: GlMesh
    private lateinit var quadMesh: GlMesh
    private lateinit var shadowMesh: GlMesh
    private lateinit var charRenderer: CharacterRenderer

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastFrameNs = System.nanoTime()
    private var fpsFrames = 0
    private var fpsAccumSec = 0f
    private var currentFps = 60
    private var hudAccumSec = 0f

    private val white = Rgba(1f, 1f, 1f, 1f)
    private val gold = Rgba.fromHex(0xFFD22B)
    private val goldDark = Rgba.fromHex(0xD97706)
    private val darkMetal = Rgba.fromHex(0x2B2D42)
    private val redStripe = Rgba.fromHex(0xFF3B5C)
    private val greenCheck = Rgba.fromHex(0x2EE58D)
    private val sapphireGlow = Rgba.fromHex(0x00E5FF)
    private val amethystGlow = Rgba.fromHex(0x9D4EDD)
    private val earthRock = Rgba.fromHex(0x64432E)
    private val marbleStone = Rgba.fromHex(0xF1F5F9)
    private val magmaOrange = Rgba.fromHex(0xFF5400)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        cartoonShader = CartoonShader()
        skyShader = SkyShader()
        shadowShader = ShadowShader()

        boxMesh = MeshBuilder.createBox()
        sphereMesh = MeshBuilder.createSphere()
        cylMesh = MeshBuilder.createCylinder()
        coneMesh = MeshBuilder.createCone()
        quadMesh = MeshBuilder.createQuad()
        shadowMesh = MeshBuilder.createShadowDisc()
        charRenderer = CharacterRenderer(boxMesh, sphereMesh, cylMesh, coneMesh)

        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1)
        Matrix.perspectiveM(projMatrix, 0, 56f, aspect, 0.3f, 220f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNs = now

        fpsFrames++
        fpsAccumSec += dt
        if (fpsAccumSec >= 0.5f) {
            currentFps = (fpsFrames / fpsAccumSec).toInt().coerceIn(1, 120)
            fpsFrames = 0
            fpsAccumSec = 0f
        }

        world.step(dt)

        hudAccumSec += dt
        if (hudAccumSec >= 0.05f) {
            hudAccumSec = 0f
            onHudTick(world.snapshot(currentFps))
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val theme = world.currentWorld
        drawSky(theme)

        Matrix.setLookAtM(
            viewMatrix, 0,
            world.camX, world.camY, world.camZ,
            world.camLookX, world.camLookY, world.camLookZ,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES30.glUseProgram(cartoonShader.programId)
        val sunX = 0.52f + 0.14f * sin(world.worldTime * 0.25f)
        val sunY = 0.85f + 0.06f * cos(world.worldTime * 0.18f)
        val sunZ = 0.42f + 0.10f * cos(world.worldTime * 0.22f)
        GLES30.glUniform3f(cartoonShader.uSunDirLoc, sunX, sunY, sunZ)
        GLES30.glUniform3f(cartoonShader.uCamPosLoc, world.camX, world.camY, world.camZ)
        GLES30.glUniform3f(cartoonShader.uFogColorLoc, theme.skyBottom.r, theme.skyBottom.g, theme.skyBottom.b)
        GLES30.glUniform1f(cartoonShader.uTimeLoc, world.worldTime)
        GLES30.glUniform1f(cartoonShader.uRoughnessLoc, 0.50f)
        GLES30.glUniform1f(cartoonShader.uMetallicLoc, 0.08f)

        if (world.isMenuShowcase) {
            drawMenuShowcaseScene(theme)
        } else {
            drawGameplayScene(theme)
        }

        if (world.graphicsQuality == GraphicsQuality.LOW) {
            val frameElapsedMs = (System.nanoTime() - now) / 1_000_000L
            if (frameElapsedMs < 32L) {
                try {
                    Thread.sleep((32L - frameElapsedMs).coerceAtMost(16L))
                } catch (_: InterruptedException) {}
            }
        }
    }

    private fun drawSky(theme: WorldTheme) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glUseProgram(skyShader.programId)
        GLES30.glUniform3f(skyShader.uTopColorLoc, theme.skyTop.r, theme.skyTop.g, theme.skyTop.b)
        GLES30.glUniform3f(skyShader.uBottomColorLoc, theme.skyBottom.r, theme.skyBottom.g, theme.skyBottom.b)
        GLES30.glUniform3f(skyShader.uSunColorLoc, theme.sunColor.r, theme.sunColor.g, theme.sunColor.b)
        GLES30.glUniform1f(skyShader.uTimeLoc, world.worldTime)
        quadMesh.draw()
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun drawMenuShowcaseScene(theme: WorldTheme) {
        val t = world.worldTime
        val flameOrange = Rgba.fromHex(0xFF9F1C)

        // Central Ancient Greek Marble & Royal Gold Pedestal Platform
        drawCone(0f, -1.9f, 0f, 6.2f, -2.5f, earthRock)
        drawCylinder(0f, -0.55f, 0f, 6.6f, 1.1f, 6.6f, gold)
        drawCylinder(0f, -0.10f, 0f, 5.6f, 0.45f, 5.6f, marbleStone)
        drawCylinder(0f, 0.14f, 0f, 4.6f, 0.12f, 4.6f, sapphireGlow, pattern = 4)

        // Twin Foreground Golden Fire Braziers
        for (side in listOf(-3.2f, 3.2f)) {
            drawBlock(side, 0.15f, 1.2f, 0.95f, 0.55f, 0.95f, marbleStone)
            drawCylinder(side, 0.52f, 1.2f, 0.78f, 0.35f, 0.78f, gold)
            val flicker = 0.55f + 0.12f * sin(t * 7.5f + side)
            drawSphere(side, 0.88f, 1.2f, flicker, flameOrange, gold, pattern = 4)
        }

        // Left & Right Floating Sanctuary Islands with Mini Greek Temples & Waterfalls
        val leftY = 0.25f + sin(t * 1.4f) * 0.28f
        val rightY = 0.35f + cos(t * 1.2f) * 0.28f
        for (side in listOf(-1f, 1f)) {
            val ix = side * 4.9f
            val iy = if (side < 0f) leftY else rightY
            val iz = if (side < 0f) -4.6f else -5.1f
            drawCone(ix, iy - 1.3f, iz, 4.4f, -2.6f, earthRock)
            drawCylinder(ix, iy, iz, 4.6f, 0.55f, 4.6f, greenCheck)
            drawCylinder(ix, iy + 0.30f, iz, 3.4f, 0.18f, 3.4f, gold)
            drawBlock(ix - side * 1.1f, iy - 1.2f, iz + 1.6f, 1.1f, 2.6f, 0.25f, sapphireGlow, white, pattern = 4)
        }

        // Ascending Golden-Trimmed Stepping Blocks leading toward the Sky Castle (matching cover art!)
        for (step in 0..3) {
            val sz = -4.2f - step * 2.2f
            val sy = 0.15f + step * 0.25f
            drawBlock(0f, sy, sz, 3.8f, 0.75f, 1.8f, darkMetal, gold, pattern = 5)
            drawBlock(0f, sy - 0.50f, sz, 3.1f, 0.35f, 1.4f, goldDark, gold, pattern = 2)
        }
        drawBlock(0f, 1.1f, -13.5f, 8.8f, 1.2f, 6.5f, gold, marbleStone, pattern = 5)

        // Rotating Temple Hammer & Glowing Sapphire Portal Finish Arch in Background
        drawRotatingHammer(0f, 1.7f, -12.5f, t * 1.4f, 0.95f, redStripe, gold)
        drawFinishArch(0f, 1.7f, -15.2f)

        // Distant Mythological Sky Castle, Moon & Floating Waterfall Islands
        drawSceneryBackdrop(theme, t, baseZ = -25f)

        // Hero Character in Center performing idle/victory vibe
        charRenderer.drawCharacter(
            cartoonShader, vpMatrix,
            0f, 0.20f, 0.2f,
            12f + sin(t * 0.7f) * 16f,
            CharAnimState.IDLE, t,
            world.selectedSkin, world.selectedHatId, world.selectedShoeId
        )

        // The other 2 Exclusive Heroes cheering on Left & Right Companion Podiums
        val companions = GameCatalog.skins.filter { it.id != world.selectedSkin.id }
        val leftCompanion = companions.getOrElse(0) { GameCatalog.skins[1] }
        val rightCompanion = companions.getOrElse(1) { GameCatalog.skins.last() }

        charRenderer.drawCharacter(
            cartoonShader, vpMatrix,
            -4.9f, leftY + 0.40f, -4.6f,
            25f, CharAnimState.VICTORY_DANCE, t,
            leftCompanion, "hat_none", "shoe_default"
        )
        charRenderer.drawCharacter(
            cartoonShader, vpMatrix,
            4.9f, rightY + 0.40f, -5.1f,
            -25f, CharAnimState.VICTORY_DANCE, t + 1.1f,
            rightCompanion, "hat_none", "shoe_default"
        )
    }

    private fun drawGameplayScene(theme: WorldTheme) {
        val t = world.worldTime
        val obsT = world.obstacleTime
        val quality = when {
            world.graphicsQuality == GraphicsQuality.HIGH && currentFps < 34 -> GraphicsQuality.MEDIUM
            world.graphicsQuality == GraphicsQuality.MEDIUM && currentFps < 26 -> GraphicsQuality.LOW
            else -> world.graphicsQuality
        }

        val maxDrawDist = when (quality) {
            GraphicsQuality.LOW -> 72f
            GraphicsQuality.MEDIUM -> 108f
            GraphicsQuality.HIGH -> 148f
        }

        // 1. Grand Horizon Backdrop ahead of the finish line
        val backdropZ = if (world.currentMode == GameMode.ENDLESS) world.pz - 68f else world.currentLevel.finishZ - 24f
        drawSceneryBackdrop(theme, t, baseZ = backdropZ)

        // 2. Continuous 3D World Corridor Flanking the Entire Course (Left & Right Floating Islands, Castles, Trees, Crystals, Statues!)
        drawWorldCorridorScenery(theme, t, maxDrawDist, quality)

        // 3. Animated Ocean / Cloud Sea / Lava Abyss below the platforms
        if (quality != GraphicsQuality.LOW) {
            val waterCol = when (theme.id) {
                10 -> magmaOrange
                8 -> Rgba.fromHex(0x3A0CA3)
                6 -> Rgba.fromHex(0x240046)
                else -> theme.hazardFloorColor ?: Rgba.fromHex(0x0096C7)
            }
            val waterAlt = when (theme.id) {
                10 -> gold
                8 -> sapphireGlow
                else -> white
            }
            val waterPattern = if (theme.id == 10) 9 else 4
            val waveY = -7.6f + sin(t * 1.6f) * 0.24f
            drawBlock(world.px, waveY, world.pz - 36f, 82f, 0.28f, 115f, waterCol, waterAlt, waterPattern)
        }

        val worldPlatformPattern = when (theme.id) {
            4 -> 10 // Golden Bridges: carved sandstone ripples & warm golden trim
            5 -> 11 // Ancient Marble Platforms: frosted glacier marble & crystalline sheen
            7 -> 6  // Mythological Gardens: organic wood & roots
            8 -> 8  // Crystal Caves: faceted glowing crystal
            3, 9 -> 7 // Thunder Arena & Floating Castles: brushed metal & rivets
            10 -> 9 // Ancient Ruins: cracked obsidian & pulsing lava veins
            else -> 5 // Carved ancient stone, cracks, golden frame & glowing chevron crest
        }

        // 4. Multi-Layered 3D Platforms with Under-Structure Foundations, Golden Bumpers & World Architecture
        for (i in 0 until world.platforms.size) {
            val rp = world.platforms[i]
            if (!rp.active) continue
            if (rp.curZ > world.camZ + 18f || rp.curZ < world.camZ - maxDrawDist) continue
            val distFromPlayer = abs(rp.curZ - world.pz)

            val baseCol = rp.def.color ?: theme.platformPrimary
            val isCollapsingWarn = rp.warningPulse > 0.05f
            val col = when {
                isCollapsingWarn -> if (rp.fallTimer > 0.32f || rp.warningPulse > 0.65f) redStripe else magmaOrange
                rp.def.type == PlatformType.SECRET_PATH -> gold
                else -> baseCol
            }
            val pattern = when {
                isCollapsingWarn -> 12 // Graduated glowing fracture web + hazard border stripes
                rp.def.type == PlatformType.SPEED_BOOST || rp.def.type == PlatformType.CONVEYOR -> 3
                rp.def.type == PlatformType.SLIPPERY -> 11 // Frosted low-friction ice surface
                rp.def.type == PlatformType.SECRET_PATH -> 4
                rp.def.type == PlatformType.FINISH -> 5
                else -> if (distFromPlayer > 72f && quality == GraphicsQuality.LOW) 2 else worldPlatformPattern
            }
            val alt = when {
                isCollapsingWarn -> gold
                rp.def.type == PlatformType.SPEED_BOOST -> sapphireGlow
                rp.def.type == PlatformType.CONVEYOR -> darkMetal
                rp.def.type == PlatformType.SLIPPERY -> white
                rp.def.type == PlatformType.SECRET_PATH -> sapphireGlow
                rp.def.type == PlatformType.FINISH -> gold
                else -> theme.platformAccent
            }

            val jitterX = if (rp.warningPulse > 0.05f) sin(t * 58f + i) * 0.075f * rp.warningPulse else 0f
            val jitterZ = if (rp.warningPulse > 0.05f) cos(t * 52f + i) * 0.055f * rp.warningPulse else 0f
            val px = rp.curX + jitterX
            val py = rp.curY
            val pz = rp.curZ + jitterZ

            // Main Top Platform Deck
            drawBlock(px, py, pz, rp.def.w, rp.def.h, rp.def.d, col, alt, pattern)

            // 3D Sculpted Under-Hull / Floating Pedestal Foundation (so platforms never look like thin floating boards!)
            if (distFromPlayer < 64f) {
                drawPlatformUnderStructure(theme, px, py, pz, rp.def.w, rp.def.h, rp.def.d, i, distFromPlayer < 38f && quality != GraphicsQuality.LOW)
            }

            // Near-Player 3D Golden Side Rails, Corner Finials & World-Specific Platform Props
            if (distFromPlayer < 42f && quality != GraphicsQuality.LOW) {
                if (rp.def.w >= 3.2f && rp.def.type != PlatformType.FINISH) {
                    val railY = py + rp.def.h * 0.5f + 0.06f
                    val railCol = if (theme.id == 8) sapphireGlow else gold
                    drawBlock(px - rp.def.w * 0.47f, railY, pz, 0.24f, 0.14f, rp.def.d * 0.96f, railCol)
                    drawBlock(px + rp.def.w * 0.47f, railY, pz, 0.24f, 0.14f, rp.def.d * 0.96f, railCol)

                    // 4 Corner Golden Bollards / Glowing Crystal Caps on major platforms
                    if (rp.def.w >= 4.4f && rp.def.d >= 5.5f && i % 2 == 0) {
                        val cxOff = rp.def.w * 0.46f
                        val czOff = rp.def.d * 0.44f
                        for (sx in listOf(-1f, 1f)) {
                            for (sz in listOf(-1f, 1f)) {
                                drawBlock(px + sx * cxOff, railY + 0.20f, pz + sz * czOff, 0.38f, 0.36f, 0.38f, gold)
                                drawSphere(px + sx * cxOff, railY + 0.48f, pz + sz * czOff, 0.26f, sapphireGlow, white, pattern = 4)
                            }
                        }
                    }
                }

                // World-Specific On-Platform Architectural Details
                val topY = py + rp.def.h * 0.5f
                if (i % 3 == 0 && rp.def.w >= 4.3f && rp.def.type == PlatformType.NORMAL) {
                    val propX = px + (if (i % 2 == 0) -rp.def.w * 0.43f else rp.def.w * 0.43f)
                    when (theme.id) {
                        1, 2, 5 -> {
                            // Greek Marble Column & Golden Flame Brazier
                            drawCylinder(propX, topY + 0.85f, pz, 0.38f, 1.7f, 0.38f, marbleStone)
                            drawBlock(propX, topY + 1.75f, pz, 0.56f, 0.22f, 0.56f, gold)
                            drawSphere(propX, topY + 2.02f, pz, 0.34f, gold, sapphireGlow, pattern = 4)
                        }
                        3, 4 -> {
                            // Zeus Golden Lightning Pylon
                            drawCylinder(propX, topY + 0.90f, pz, 0.34f, 1.8f, 0.34f, darkMetal, pattern = 7)
                            drawCone(propX, topY + 2.05f, pz, 0.54f, 0.7f, gold)
                            drawSphere(propX, topY + 2.50f, pz, 0.36f, sapphireGlow, white, pattern = 4)
                        }
                        6 -> {
                            // Floating Arcane Rune Obelisk
                            val bob = sin(t * 3.0f + i) * 0.18f
                            drawBlock(propX, topY + 1.15f + bob, pz, 0.42f, 1.5f, 0.42f, amethystGlow, sapphireGlow, pattern = 8)
                            drawSphere(propX, topY + 2.15f + bob, pz, 0.36f, sapphireGlow, white, pattern = 4)
                        }
                        7 -> {
                            // Swaying Mythological Garden Tree & Golden Fruit Canopy
                            val sway = sin(t * 2.4f + i) * 0.20f
                            drawCylinder(propX, topY + 0.85f, pz, 0.34f, 1.7f, 0.34f, earthRock, pattern = 6)
                            drawSphere(propX + sway, topY + 1.90f, pz, 1.20f, greenCheck, gold, pattern = 4)
                        }
                        8 -> {
                            // Twin Glowing Crystal Clusters
                            drawCone(propX, topY + 0.75f, pz, 0.54f, 1.5f, sapphireGlow, pattern = 8)
                            drawCone(propX + 0.25f, topY + 0.48f, pz + 0.2f, 0.38f, 0.95f, amethystGlow, pattern = 8)
                        }
                        9 -> {
                            // Royal Floating Castle Turret with Golden Roof
                            drawCylinder(propX, topY + 1.10f, pz, 0.52f, 2.2f, 0.52f, marbleStone, pattern = 7)
                            drawCone(propX, topY + 2.50f, pz, 0.80f, 0.95f, gold)
                        }
                        10 -> {
                            // Ancient Ruin Obsidian Pillar & Magma Brazier
                            drawCylinder(propX, topY + 0.80f, pz, 0.46f, 1.6f, 0.46f, darkMetal, pattern = 9)
                            drawSphere(propX, topY + 1.75f, pz, 0.44f, magmaOrange, gold, pattern = 4)
                        }
                    }
                }
            }

            if (rp.def.type == PlatformType.BOUNCE_PAD) {
                drawCylinder(px, py + rp.def.h * 0.5f + 0.14f, pz, rp.def.w * 0.65f, 0.25f, rp.def.d * 0.65f, gold)
                drawCylinder(px, py + rp.def.h * 0.5f + 0.28f, pz, rp.def.w * 0.45f, 0.10f, rp.def.d * 0.45f, sapphireGlow, pattern = 4)
            }
            if (rp.def.type == PlatformType.FINISH) {
                drawFinishArch(px, py + rp.def.h * 0.5f, pz)
            }
        }

        // 5. MYTHORIA 3D Checkpoints
        val deepSapphire = Rgba.fromHex(0x0077FF)
        val checkpoints = world.activeCheckpoints
        for (i in 0 until checkpoints.size) {
            val cp = checkpoints[i]
            if (abs(cp.z - world.pz) > 115f) continue
            val reached = world.highestCheckpointIndex >= cp.index
            val pulseRem = world.checkpointPulseTimers[cp.index] ?: 0f
            val pulseBoost = if (pulseRem > 0f) sin(pulseRem * 9f) * 0.25f + 0.35f else 0f

            val ringCol = if (reached) sapphireGlow else gold
            val ringScale = 3.2f + (if (reached) 0.15f * sin(t * 4f + cp.index) else 0f) + pulseBoost * 0.8f
            drawCylinder(cp.x, cp.y + 0.06f, cp.z, ringScale, 0.08f, ringScale, ringCol, pattern = 4)

            for (side in listOf(-2.25f, 2.25f)) {
                drawBlock(cp.x + side, cp.y + 0.22f, cp.z, 0.82f, 0.42f, 0.82f, gold)
                drawCylinder(cp.x + side, cp.y + 1.55f, cp.z, 0.44f, 2.6f, 0.44f, marbleStone)
                drawCylinder(cp.x + side, cp.y + 1.05f + pulseBoost * 0.2f, cp.z, 0.52f, 0.22f, 0.52f, gold, pattern = 4)
                drawCylinder(cp.x + side, cp.y + 2.05f + pulseBoost * 0.2f, cp.z, 0.52f, 0.22f, 0.52f, if (reached) sapphireGlow else gold, pattern = 4)
                drawBlock(cp.x + side, cp.y + 2.95f, cp.z, 0.86f, 0.36f, 0.86f, gold)
                val crystalY = cp.y + 3.45f + sin(t * 3.2f + side) * 0.14f + pulseBoost * 0.35f
                drawSphere(cp.x + side, crystalY, cp.z, 0.48f + pulseBoost * 0.18f, if (reached) sapphireGlow else deepSapphire, white, pattern = 4)
            }

            val archCol = if (reached) sapphireGlow else gold
            val archAlt = if (reached) gold else white
            drawBlock(cp.x, cp.y + 3.15f + pulseBoost * 0.15f, cp.z, 5.2f, 0.48f, 0.48f, gold, archCol, pattern = 2)
            val flagWave = sin(t * 6.5f + cp.index) * 0.18f
            drawBlock(cp.x - 2.65f + flagWave * 0.3f, cp.y + 2.45f, cp.z + flagWave, 0.72f, 0.38f, 0.06f, if (reached) sapphireGlow else redStripe, gold, pattern = 4)
            drawBlock(cp.x + 2.65f + flagWave * 0.3f, cp.y + 2.45f, cp.z - flagWave, 0.72f, 0.38f, 0.06f, if (reached) sapphireGlow else redStripe, gold, pattern = 4)
            val gemY = cp.y + 3.82f + sin(t * 3.5f + cp.index) * 0.12f + pulseBoost * 0.3f
            drawCone(cp.x, gemY + 0.32f, cp.z, 0.85f, 0.65f, gold, pattern = 4)
            drawSphere(cp.x, gemY, cp.z, 0.58f + pulseBoost * 0.2f, archCol, archAlt, pattern = 4)
        }

        // 6. Coins (using dynamic curX/curY/curZ so magnetized coins visibly fly into the player)
        for (i in 0 until world.coins.size) {
            val rc = world.coins[i]
            if (rc.collected) continue
            if (abs(rc.curZ - world.pz) > 90f) continue
            val bob = sin(t * 4f + rc.def.z) * 0.18f
            drawSpinningCoin(rc.curX, rc.curY + bob, rc.curZ, t * 180f + rc.def.z * 20f)
        }

        // 6B. 3D Floating Power-Ups (Aegis Shield, Coin Magnet, Hermes Glide Wings)
        for (i in 0 until world.powerUps.size) {
            val pu = world.powerUps[i]
            if (pu.collected) continue
            if (abs(pu.def.z - world.pz) > 95f) continue
            drawPowerUp(pu.def, t)
        }

        // 6C. Hidden Royal Treasure Chests & Stage-5 World Gate Boss Sentinel Statues
        for (i in 0 until world.treasureChests.size) {
            val tc = world.treasureChests[i]
            if (abs(tc.def.z - world.pz) > 105f) continue
            drawTreasureChestAndBossSentinel(tc, theme, t)
        }

        // 7. Obstacles
        val obstacles = world.activeObstacles
        for (i in 0 until obstacles.size) {
            val obs = obstacles[i]
            if (abs(obs.z - world.pz) > 100f) continue
            drawObstacle(obs, theme, obsT)
        }

        // 8. Respawn Portal
        if (world.playerCheckpointState.isRespawning) {
            val pScale = world.playerCheckpointState.portalScale.coerceIn(0.15f, 1f)
            drawCylinder(world.px, world.py + 0.08f, world.pz, 2.4f * pScale, 0.12f, 2.4f * pScale, sapphireGlow, pattern = 4)
            drawCylinder(world.px, world.py + 0.95f * (1f - pScale * 0.4f), world.pz, 1.85f * pScale, 0.10f, 1.85f * pScale, gold, pattern = 4)
        }

        // 8B. Active Player 3D Power-Up Auras (Aegis Shield Ring, Coin Magnet Field, Hermes Golden Wings)
        if (world.shieldTimer > 0f) {
            val shieldPulse = 1.65f + sin(t * 6.5f) * 0.12f
            drawCylinder(world.px, world.py + 0.72f, world.pz, shieldPulse, 0.10f, shieldPulse, sapphireGlow, pattern = 4)
            drawCylinder(world.px, world.py + 1.15f, world.pz, shieldPulse * 0.78f, 0.08f, shieldPulse * 0.78f, gold, pattern = 4)
        }
        if (world.magnetTimer > 0f) {
            val magPulse = 2.05f + sin(t * 8.0f) * 0.22f
            drawCylinder(world.px, world.py + 0.14f, world.pz, magPulse, 0.07f, magPulse, gold, pattern = 4)
        }
        if (world.wingsTimer > 0f) {
            val flap = sin(t * 11f) * 0.16f
            drawBlock(world.px - 0.65f, world.py + 1.05f + flap, world.pz + 0.22f, 0.75f, 0.28f, 0.12f, gold, white, pattern = 4)
            drawBlock(world.px + 0.65f, world.py + 1.05f + flap, world.pz + 0.22f, 0.75f, 0.28f, 0.12f, gold, white, pattern = 4)
        }
        // Short non-obstructive waist-level aura ring on reward/ability activation (never hides the feet!)
        if (world.pickupAuraTimer > 0f) {
            val prog = 1f - (world.pickupAuraTimer / 0.65f).coerceIn(0f, 1f)
            val auraRadius = 1.05f + prog * 1.15f
            drawCylinder(world.px, world.py + 0.78f, world.pz, auraRadius, 0.06f * (1f - prog * 0.5f), auraRadius, world.pickupAuraColor, pattern = 4)
        }

        // 8C. Personal Best / Time Attack Holographic Sapphire Ghost Racer
        if (world.ghostActive && abs(world.ghostZ - world.pz) < 95f) {
            val ghostSkin = world.selectedSkin.copy(
                skinColor = sapphireGlow,
                hairOrAccentColor = white,
                hoodieColor = sapphireGlow,
                pantsColor = Rgba.fromHex(0x0284C7),
                shoeColor = gold,
                emblemColor = white
            )
            drawCylinder(world.ghostX, world.ghostY + 0.05f, world.ghostZ, 1.15f, 0.06f, 1.15f, sapphireGlow, pattern = 4)
            charRenderer.drawCharacter(
                cartoonShader, vpMatrix,
                world.ghostX, world.ghostY, world.ghostZ,
                world.ghostYaw, CharAnimState.RUN, t * 1.15f,
                ghostSkin, "hat_crown", "shoe_winged"
            )
        }

        // 9. Player Character
        charRenderer.drawCharacter(
            cartoonShader, vpMatrix,
            world.px, world.py, world.pz,
            world.playerYaw, world.animState, world.animTime,
            world.selectedSkin, world.selectedHatId, world.selectedShoeId,
            world.squashY,
            hitFlash = (world.hitStunTimer > 0f || world.playerCheckpointState.arrivalAnimTimerSec > 0.35f) && ((t * 20f).toInt() % 2 == 0),
            showLeaderCrown = world.currentMode == GameMode.RIVAL_RACE && world.computePlayerRaceRank() == 1,
            turnLeanDeg = world.playerTurnLeanDeg,
            accelPitchDeg = world.playerAccelPitchDeg
        )

        // 10. AI Rival Racers
        val rivals = world.rivals
        for (i in 0 until rivals.size) {
            val r = rivals[i]
            if (abs(r.z - world.pz) > 105f) continue
            if (r.checkpointState.isRespawning) {
                val rScale = r.checkpointState.portalScale.coerceIn(0.15f, 1f)
                drawCylinder(r.x, r.y + 0.08f, r.z, 2.2f * rScale, 0.11f, 2.2f * rScale, sapphireGlow, pattern = 4)
                drawCylinder(r.x, r.y + 0.85f * (1f - rScale * 0.4f), r.z, 1.65f * rScale, 0.09f, 1.65f * rScale, gold, pattern = 4)
            }
            charRenderer.drawCharacter(
                cartoonShader, vpMatrix,
                r.x, r.y, r.z,
                r.yaw, r.animState, r.animTime,
                r.skin, r.hatId, r.shoeId,
                r.squashY,
                hitFlash = (r.hitStunTimer > 0f || r.checkpointState.arrivalAnimTimerSec > 0.35f) && ((t * 20f).toInt() % 2 == 0),
                showLeaderCrown = r.finishOrder == 1
            )
        }

        // 11. Particles
        for (i in 0 until world.particles.size) {
            val p = world.particles[i]
            drawSphere(p.x, p.y, p.z, p.size, p.color, pattern = 4)
        }

        // 12. Soft Contact Shadows (Ground & Airborne Landing Guide Shadow + Chest/Rival Contact Shadows)
        GLES30.glUseProgram(shadowShader.programId)
        val hasProjectedFloor = !world.shadowFloorY.isNaN() && world.shadowHeightGap < 6.2f
        if (world.grounded || hasProjectedFloor) {
            val shadowY = if (world.grounded) world.py + 0.03f else world.shadowFloorY + 0.03f
            val gapFactor = (world.shadowHeightGap / 6.2f).coerceIn(0f, 1f)
            val shadowScale = 1.14f * (1f - gapFactor * 0.42f)
            val shadowAlpha = 0.56f * (1f - gapFactor * 0.72f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, world.px, shadowY, world.pz)
            Matrix.scaleM(modelMatrix, 0, shadowScale, 1f, shadowScale)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES30.glUniformMatrix4fv(shadowShader.uMvpLoc, 1, false, mvpMatrix, 0)
            GLES30.glUniform1f(shadowShader.uAlphaLoc, shadowAlpha)
            shadowMesh.draw()
        }
        if (quality != GraphicsQuality.LOW) {
            for (i in 0 until world.treasureChests.size) {
                val tc = world.treasureChests[i]
                if (abs(tc.def.z - world.pz) > 48f) continue
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, tc.def.x, tc.def.y + 0.02f, tc.def.z)
                Matrix.scaleM(modelMatrix, 0, 1.55f, 1f, 1.25f)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(shadowShader.uMvpLoc, 1, false, mvpMatrix, 0)
                GLES30.glUniform1f(shadowShader.uAlphaLoc, 0.42f)
                shadowMesh.draw()
            }
            if (quality == GraphicsQuality.HIGH) {
                for (i in 0 until rivals.size) {
                    val r = rivals[i]
                    if (!r.grounded || abs(r.z - world.pz) > 65f) continue
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, r.x, r.y + 0.03f, r.z)
                    Matrix.scaleM(modelMatrix, 0, 1.0f, 1f, 1.0f)
                    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                    GLES30.glUniformMatrix4fv(shadowShader.uMvpLoc, 1, false, mvpMatrix, 0)
                    GLES30.glUniform1f(shadowShader.uAlphaLoc, 0.44f)
                    shadowMesh.draw()
                }
            }
        }
    }

    /**
     * Renders a sculpted 3D under-structure beneath each platform so platforms look like solid
     * temple blocks, floating islands, crystal formations, or royal bridges rather than flat slabs.
     */
    private fun drawPlatformUnderStructure(
        theme: WorldTheme,
        px: Float,
        py: Float,
        pz: Float,
        w: Float,
        h: Float,
        d: Float,
        index: Int,
        highDetail: Boolean
    ) {
        val bottomY = py - h * 0.5f
        // Golden / Metallic beveled lower collar directly under the slab
        val collarCol = when (theme.id) {
            8 -> amethystGlow
            10 -> darkMetal
            else -> goldDark
        }
        drawBlock(px, bottomY - 0.18f, pz, w * 0.88f, 0.34f, d * 0.90f, collarCol, gold, pattern = 2)

        if (!highDetail || w < 2.2f) return

        // Deep world-themed floating foundation beneath larger platforms
        when (theme.id) {
            1, 2, 7 -> {
                // Floating Island Tapered Rock Root Base
                val coneW = (w * 0.72f).coerceAtMost(5.5f)
                drawCone(px, bottomY - 1.15f, pz, coneW, -1.75f, earthRock)
                if (index % 2 == 0) {
                    drawCylinder(px, bottomY - 0.45f, pz, coneW * 0.58f, 0.32f, coneW * 0.58f, gold)
                }
            }
            3, 4, 9 -> {
                // Royal Castle / Thunder Bridge Under-Arch & Glowing Levitation Core
                drawBlock(px, bottomY - 0.52f, pz, w * 0.62f, 0.38f, d * 0.74f, darkMetal, gold, pattern = 7)
                if (index % 2 == 0) {
                    drawSphere(px, bottomY - 0.85f, pz, 0.68f, sapphireGlow, white, pattern = 4)
                }
            }
            5, 6 -> {
                // Marble Pedestal / Arcane Levitation Ring
                drawCylinder(px, bottomY - 0.62f, pz, (w * 0.55f).coerceAtMost(4.2f), 0.75f, (w * 0.55f).coerceAtMost(4.2f), marbleStone)
                drawCylinder(px, bottomY - 1.05f, pz, (w * 0.68f).coerceAtMost(4.8f), 0.14f, (w * 0.68f).coerceAtMost(4.8f), sapphireGlow, pattern = 4)
            }
            8 -> {
                // Glowing Inverted Crystal Stalactite Base
                val crysW = (w * 0.65f).coerceAtMost(4.8f)
                drawCone(px, bottomY - 1.25f, pz, crysW, -2.0f, sapphireGlow, pattern = 8)
            }
            10 -> {
                // Ancient Ruin Obsidian Under-Pillar & Magma Core
                drawBlock(px, bottomY - 0.65f, pz, w * 0.66f, 0.65f, d * 0.76f, darkMetal, magmaOrange, pattern = 9)
            }
        }
    }

    /**
     * Renders a continuous 3D flanking environment along the entire track (left and right sides every 24m)
     * so every single world feels immersive, alive, and distinct from start to finish!
     */
    private fun drawWorldCorridorScenery(
        theme: WorldTheme,
        t: Float,
        maxDrawDist: Float,
        quality: GraphicsQuality
    ) {
        val spacing = if (quality == GraphicsQuality.LOW) 34f else 24f
        val startIdx = ((world.camZ + 12f) / spacing).toInt()
        val endIdx = ((world.camZ - maxDrawDist) / spacing).toInt()

        for (idx in startIdx downTo endIdx) {
            if (idx > 0) continue
            val sz = idx * spacing - 10f
            // Don't overlap the finish backdrop
            if (world.currentMode != GameMode.ENDLESS && sz < world.currentLevel.finishZ - 14f) continue

            val bobL = sin(t * 1.15f + idx * 1.7f) * 0.45f
            val bobR = cos(t * 1.05f + idx * 2.1f) * 0.45f
            val leftX = -14.5f - (abs(idx) % 3) * 2.2f
            val rightX = 14.5f + ((abs(idx) + 1) % 3) * 2.2f
            val baseY = -0.8f + (abs(idx) % 2) * 1.3f

            drawWorldSideLandmark(theme, leftX, baseY + bobL, sz, -1f, idx, t, quality)
            drawWorldSideLandmark(theme, rightX, baseY + bobR, sz - 8f, 1f, idx + 7, t, quality)

            // Drifting volumetric 3D cloud clusters & floating golden sky lanterns along the corridor
            if (quality != GraphicsQuality.LOW) {
                val cloudSide = if (idx % 2 == 0) -1f else 1f
                val cx = cloudSide * (10.5f + (abs(idx) % 3) * 2.5f) + sin(t * 0.35f + idx) * 1.2f
                val cy = 4.8f + (abs(idx) % 3) * 1.6f
                val cz = sz - 5f
                drawSphere(cx, cy, cz, 2.1f, white)
                drawSphere(cx + 1.25f, cy - 0.25f, cz, 1.65f, white)
                drawSphere(cx - 1.25f, cy - 0.25f, cz, 1.65f, white)
            }
        }
    }

    private fun drawWorldSideLandmark(
        theme: WorldTheme,
        x: Float,
        y: Float,
        z: Float,
        sideSign: Float,
        seed: Int,
        t: Float,
        quality: GraphicsQuality
    ) {
        when (theme.id) {
            1 -> {
                // World 1 (Ancient Greek Islands): Floating Sanctuary Island + Marble Colonnade + Cascading Waterfall
                drawCone(x, y - 2.2f, z, 6.8f, -4.4f, earthRock)
                drawCylinder(x, y, z, 7.0f, 0.9f, 7.0f, greenCheck)
                drawBlock(x, y + 0.65f, z, 4.6f, 0.45f, 3.8f, marbleStone, gold, pattern = 2)
                for (cx in listOf(-1.5f, 0f, 1.5f)) {
                    drawCylinder(x + cx, y + 2.1f, z, 0.55f, 2.5f, 0.55f, marbleStone)
                }
                drawCone(x, y + 3.9f, z, 5.2f, 1.6f, gold)
                // Waterfall pouring toward the track
                val wfX = x - sideSign * 2.7f
                drawBlock(wfX, y - 2.2f, z, 0.35f, 4.8f, 1.5f, sapphireGlow, white, pattern = 4)
                if (quality != GraphicsQuality.LOW) {
                    val mistScale = 0.95f + 0.22f * sin(t * 5.5f + seed)
                    drawSphere(wfX, y - 4.5f, z, mistScale, white)
                }
            }
            2 -> {
                // World 2 (Floating Temples): Multi-Tiered Golden Sky Sanctum & Orbiting Halo Ring
                drawCone(x, y - 2.0f, z, 6.2f, -3.8f, earthRock)
                drawBlock(x, y + 0.2f, z, 6.2f, 1.0f, 6.2f, marbleStone, gold, pattern = 5)
                drawCylinder(x, y + 1.8f, z, 3.6f, 2.4f, 3.6f, marbleStone)
                drawSphere(x, y + 3.2f, z, 3.5f, gold, white, pattern = 4)
                drawCylinder(x, y + 2.4f + sin(t * 2.2f + seed) * 0.4f, z, 5.4f, 0.22f, 5.4f, sapphireGlow, pattern = 4)
            }
            3 -> {
                // World 3 (Zeus Thunder Arena): Giant Tesla Storm Obelisk & Rotating Electric Plasma Rings
                drawBlock(x, y, z, 5.2f, 1.2f, 5.2f, darkMetal, gold, pattern = 7)
                drawCylinder(x, y + 3.2f, z, 1.5f, 5.4f, 1.5f, darkMetal, pattern = 7)
                drawCone(x, y + 6.6f, z, 2.2f, 2.0f, gold)
                val ringY = y + 3.4f + sin(t * 3.2f + seed) * 1.1f
                drawCylinder(x, ringY, z, 3.8f, 0.26f, 3.8f, sapphireGlow, pattern = 4)
                drawSphere(x, y + 7.9f, z, 0.95f, sapphireGlow, white, pattern = 4)
            }
            4 -> {
                // World 4 (Golden Bridges): Towering Royal Golden Suspension Pylon & Solar Orb
                drawBlock(x, y - 0.5f, z, 5.6f, 1.4f, 4.4f, goldDark, gold, pattern = 2)
                for (pxOff in listOf(-1.6f, 1.6f)) {
                    drawBlock(x + pxOff, y + 2.8f, z, 1.1f, 5.4f, 1.1f, gold, white, pattern = 7)
                }
                drawBlock(x, y + 4.8f, z, 4.8f, 0.75f, 1.3f, gold, sapphireGlow, pattern = 2)
                drawSphere(x, y + 6.2f + sin(t * 2.0f + seed) * 0.35f, z, 1.35f, sapphireGlow, white, pattern = 4)
            }
            5 -> {
                // World 5 (Ancient Marble Platforms): Frosted Marble Rotunda & Aquamarine Spires
                drawCylinder(x, y, z, 6.4f, 1.1f, 6.4f, marbleStone, pattern = 5)
                drawCylinder(x, y + 2.1f, z, 4.2f, 3.0f, 4.2f, Rgba.fromHex(0xBAE6FD))
                drawCone(x, y + 4.6f, z, 5.2f, 2.2f, gold)
                drawCone(x - sideSign * 2.2f, y + 2.2f, z + 1.8f, 1.2f, 3.4f, sapphireGlow, pattern = 8)
            }
            6 -> {
                // World 6 (Magical Portals): Giant Levitating Arcane Portal Ring & Rune Pillars
                drawBlock(x, y - 0.4f, z, 5.8f, 1.2f, 5.8f, Rgba.fromHex(0x240046), gold, pattern = 5)
                drawCylinder(x, y + 3.2f, z, 4.8f, 0.48f, 4.8f, amethystGlow, rotZ = 90f, pattern = 8)
                drawSphere(x, y + 3.2f + sin(t * 2.6f + seed) * 0.4f, z, 2.1f, sapphireGlow, white, pattern = 4)
                drawCone(x, y + 6.2f, z, 1.5f, 1.6f, gold, pattern = 4)
            }
            7 -> {
                // World 7 (Mythological Gardens): Giant Yggdrasil World-Tree Island, Swaying Canopy & Waterfall
                drawCone(x, y - 2.3f, z, 7.2f, -4.5f, earthRock)
                drawCylinder(x, y, z, 7.4f, 1.0f, 7.4f, greenCheck)
                drawCylinder(x, y + 2.8f, z, 1.85f, 4.8f, 1.85f, earthRock, pattern = 6)
                val sway = sin(t * 1.8f + seed) * 0.35f
                drawSphere(x + sway, y + 6.2f, z, 4.4f, greenCheck, gold, pattern = 4)
                drawSphere(x - 1.8f + sway, y + 5.1f, z + 1.0f, 3.0f, Rgba.fromHex(0x10B981))
                drawSphere(x + 1.8f + sway, y + 5.2f, z - 0.8f, 3.0f, greenCheck)
                drawBlock(x - sideSign * 2.9f, y - 2.2f, z, 0.38f, 4.6f, 1.4f, sapphireGlow, white, pattern = 4)
            }
            8 -> {
                // World 8 (Crystal Caves): Colossal Bioluminescent Gemstone Cluster Island
                drawCone(x, y - 2.0f, z, 6.4f, -3.8f, Rgba.fromHex(0x1E1B4B))
                drawBlock(x, y, z, 6.2f, 1.1f, 6.2f, amethystGlow, sapphireGlow, pattern = 8)
                drawCone(x, y + 3.8f, z, 2.4f, 6.8f, sapphireGlow, pattern = 8)
                drawCone(x - 1.6f, y + 2.6f, z + 1.1f, 1.7f, 4.6f, Rgba.fromHex(0xF72585), pattern = 8)
                drawCone(x + 1.6f, y + 2.4f, z - 1.0f, 1.6f, 4.2f, gold, pattern = 8)
            }
            9 -> {
                // World 9 (Floating Castles): Multi-Turret Royal Floating Citadel
                drawCone(x, y - 2.1f, z, 6.8f, -4.2f, earthRock)
                drawBlock(x, y + 0.4f, z, 6.4f, 1.4f, 5.4f, marbleStone, gold, pattern = 7)
                drawCylinder(x, y + 3.4f, z, 2.1f, 4.8f, 2.1f, marbleStone, pattern = 7)
                drawCone(x, y + 6.6f, z, 3.0f, 2.4f, gold)
                for (tx in listOf(-2.2f, 2.2f)) {
                    drawCylinder(x + tx, y + 2.4f, z + 0.8f, 1.25f, 3.2f, 1.25f, marbleStone, pattern = 7)
                    drawCone(x + tx, y + 4.5f, z + 0.8f, 1.8f, 1.6f, sapphireGlow, pattern = 4)
                }
            }
            10 -> {
                // World 10 (Ancient Ruins): Colossal Crowned Titan Guardian Statue & Lava Brazier
                drawBlock(x, y, z, 6.2f, 1.4f, 5.6f, darkMetal, magmaOrange, pattern = 9)
                drawBlock(x, y + 2.8f, z, 3.0f, 4.2f, 2.0f, darkMetal, gold, pattern = 9)
                drawBlock(x, y + 4.6f, z, 4.6f, 1.1f, 2.2f, gold)
                drawSphere(x, y + 6.1f, z, 1.65f, darkMetal, gold, pattern = 9)
                drawCone(x, y + 7.4f, z, 1.85f, 1.4f, gold, pattern = 4)
                // Glowing magma eyes facing the track
                val eyeX = x - sideSign * 0.75f
                drawSphere(eyeX, y + 6.2f, z - 0.38f, 0.28f, magmaOrange, gold, pattern = 4)
                drawSphere(eyeX, y + 6.2f, z + 0.38f, 0.28f, magmaOrange, gold, pattern = 4)
            }
        }
    }

    private fun drawObstacle(obs: ObstacleDef, theme: WorldTheme, t: Float) {
        val phaseT = t * obs.speed + obs.phase
        when (obs.type) {
            ObstacleType.ROTATING_HAMMER -> {
                drawRotatingHammer(obs.x, obs.y, obs.z, phaseT, obs.scale, theme.obstaclePrimary, theme.obstacleSecondary)
            }
            ObstacleType.ROTATING_BEAM -> {
                drawCylinder(obs.x, obs.y + 0.25f, obs.z, 1.3f, 0.45f, 1.3f, gold)
                drawCylinder(obs.x, obs.y + 0.85f, obs.z, 0.75f, 1.5f, 0.75f, darkMetal)
                drawSphere(obs.x, obs.y + 1.65f, obs.z, 0.72f, sapphireGlow, white, pattern = 4)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, obs.x, obs.y + 0.75f, obs.z)
                Matrix.rotateM(modelMatrix, 0, Math.toDegrees(-phaseT.toDouble()).toFloat(), 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, obs.range * 1.9f, 0.45f * obs.scale, 0.45f * obs.scale)
                drawMeshWithModel(boxMesh, theme.obstaclePrimary, gold, pattern = 1)
            }
            ObstacleType.SPINNING_BLADE -> {
                drawCylinder(obs.x, obs.y + 0.25f, obs.z, 1.3f, 0.45f, 1.3f, gold)
                drawCylinder(obs.x, obs.y + 0.8f, obs.z, 0.75f, 1.4f, 0.75f, darkMetal)
                drawSphere(obs.x, obs.y + 1.55f, obs.z, 0.65f, redStripe, gold, pattern = 4)
                for (a in listOf(0f, 90f)) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obs.x, obs.y + 0.75f, obs.z)
                    Matrix.rotateM(modelMatrix, 0, Math.toDegrees(-phaseT.toDouble()).toFloat() + a, 0f, 1f, 0f)
                    Matrix.scaleM(modelMatrix, 0, obs.range * 1.95f, 0.24f, 0.58f)
                    drawMeshWithModel(boxMesh, marbleStone, redStripe, pattern = 1)
                }
            }
            ObstacleType.ROLLING_BALL -> {
                val ox = obs.x + sin(phaseT) * obs.range
                val s = 1.95f * obs.scale
                drawSphere(ox, obs.y, obs.z, s, darkMetal, theme.obstaclePrimary, pattern = 1)
                drawCylinder(ox, obs.y, obs.z, s * 1.06f, 0.36f * obs.scale, s * 1.06f, gold)
                drawCylinder(ox, obs.y, obs.z, s * 1.06f, 0.36f * obs.scale, s * 1.06f, redStripe, rotZ = 90f, pattern = 4)
                drawCone(ox, obs.y + s * 0.62f, obs.z, 0.55f * obs.scale, 0.70f * obs.scale, gold, pattern = 4)
                drawCone(ox - s * 0.55f, obs.y + 0.2f, obs.z, 0.45f * obs.scale, 0.55f * obs.scale, redStripe, pattern = 4)
                drawCone(ox + s * 0.55f, obs.y + 0.2f, obs.z, 0.45f * obs.scale, 0.55f * obs.scale, redStripe, pattern = 4)
            }
            ObstacleType.SWINGING_PENDULUM, ObstacleType.GIANT_SWINGER -> {
                val swing = sin(phaseT) * 1.1f
                val bx = obs.x + sin(swing) * 3.4f * obs.scale
                val by = obs.y + 4.2f - cos(swing) * 3.4f * obs.scale
                for (side in listOf(-3.2f, 3.2f)) {
                    drawBlock(obs.x + side, obs.y + 0.2f, obs.z, 0.9f, 0.4f, 0.9f, gold)
                    drawCylinder(obs.x + side, obs.y + 2.2f, obs.z, 0.48f, 4.2f, 0.48f, marbleStone)
                    drawBlock(obs.x + side, obs.y + 4.35f, obs.z, 0.95f, 0.4f, 0.95f, gold)
                }
                drawBlock(obs.x, obs.y + 4.5f, obs.z, 7.2f, 0.55f, 0.65f, gold, sapphireGlow, pattern = 2)
                drawSphere(obs.x, obs.y + 4.5f, obs.z, 0.85f, redStripe, gold, pattern = 4)

                val swingDeg = Math.toDegrees(swing.toDouble()).toFloat()
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, obs.x, obs.y + 4.2f, obs.z)
                Matrix.rotateM(modelMatrix, 0, swingDeg, 0f, 0f, 1f)
                Matrix.translateM(modelMatrix, 0, 0f, -1.7f * obs.scale, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.22f, 3.4f * obs.scale, 0.22f)
                drawMeshWithModel(cylMesh, gold)

                drawSphere(bx, by, obs.z, 1.25f * obs.scale, gold, darkMetal, pattern = 2)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, bx, by, obs.z)
                Matrix.rotateM(modelMatrix, 0, swingDeg, 0f, 0f, 1f)
                Matrix.scaleM(modelMatrix, 0, 2.65f * obs.scale, 1.35f * obs.scale, 0.32f * obs.scale)
                drawMeshWithModel(boxMesh, redStripe, gold, pattern = 1)
            }
            ObstacleType.MOVING_WALL, ObstacleType.SLIDING_OBSTACLE, ObstacleType.PUSHER -> {
                val ox = obs.x + sin(phaseT) * obs.range
                val w = 1.95f * obs.scale
                val h = 1.85f * obs.scale
                val d = 1.35f * obs.scale
                drawBlock(ox, obs.y + 0.92f, obs.z, w, h, d, theme.obstaclePrimary, gold, pattern = 2)
                drawCylinder(ox - w * 0.46f, obs.y + 0.92f, obs.z, 0.34f, h * 1.06f, 0.34f, gold)
                drawCylinder(ox + w * 0.46f, obs.y + 0.92f, obs.z, 0.34f, h * 1.06f, 0.34f, gold)
                drawSphere(ox, obs.y + 0.92f, obs.z + d * 0.48f, 0.65f * obs.scale, sapphireGlow, white, pattern = 4)
                drawSphere(ox, obs.y + 0.92f, obs.z - d * 0.48f, 0.65f * obs.scale, sapphireGlow, white, pattern = 4)
            }
            ObstacleType.JUMPING_BLOCK -> {
                val oy = obs.y + 0.7f + abs(sin(phaseT)) * 2.4f
                drawBlock(obs.x, oy, obs.z, 1.85f * obs.scale, 1.45f * obs.scale, 1.45f * obs.scale, theme.obstacleSecondary, gold, pattern = 2)
                drawSphere(obs.x, oy + 0.8f * obs.scale, obs.z, 0.55f * obs.scale, sapphireGlow, white, pattern = 4)
            }
            ObstacleType.FIRE_JET -> {
                drawCylinder(obs.x, obs.y + 0.18f, obs.z, 1.35f, 0.38f, 1.35f, gold)
                drawCylinder(obs.x, obs.y + 0.38f, obs.z, 0.95f, 0.22f, 0.95f, darkMetal)
                if (sin(phaseT) > 0.1f) {
                    drawCone(obs.x, obs.y + 1.35f, obs.z, 1.15f * obs.scale, 2.3f * obs.scale, magmaOrange, pattern = 4)
                    drawCone(obs.x, obs.y + 1.15f, obs.z, 0.68f * obs.scale, 1.6f * obs.scale, gold, pattern = 4)
                }
            }
            ObstacleType.LASER_BEAM -> {
                for (side in listOf(-obs.range, obs.range)) {
                    drawBlock(obs.x + side, obs.y + 0.2f, obs.z, 0.65f, 0.4f, 0.65f, gold)
                    drawCylinder(obs.x + side, obs.y + 0.9f, obs.z, 0.38f, 1.6f, 0.38f, darkMetal)
                    drawSphere(obs.x + side, obs.y + 1.75f, obs.z, 0.48f, redStripe, white, pattern = 4)
                }
                if (sin(phaseT) > -0.25f) {
                    drawCylinder(obs.x, obs.y + 0.85f, obs.z, 0.24f, obs.range * 2f, 0.24f, Rgba.fromHex(0xFF1E56), rotZ = 90f, pattern = 4)
                }
            }
            ObstacleType.FALLING_STALACTITE -> {
                val cycle = (phaseT % 2.6f + 2.6f) % 2.6f
                val dropFrac = (cycle / 1.85f).coerceIn(0f, 1f)
                val oy = obs.y + 5.2f * (1f - dropFrac * dropFrac)
                drawSphere(obs.x, obs.y + 5.6f, obs.z, 1.1f * obs.scale, amethystGlow, sapphireGlow, pattern = 8)
                drawCone(obs.x, oy, obs.z, 1.05f * obs.scale, -2.2f * obs.scale, sapphireGlow, pattern = 8)
                val warnScale = (0.7f + dropFrac * 1.1f) * obs.scale
                drawCylinder(obs.x, obs.y + 0.06f, obs.z, warnScale, 0.06f, warnScale, redStripe, pattern = 4)
            }
        }
    }

    private fun drawRotatingHammer(x: Float, y: Float, z: Float, angleRad: Float, scale: Float, c1: Rgba, c2: Rgba) {
        val rotDeg = Math.toDegrees(-angleRad.toDouble()).toFloat()
        drawCylinder(x, y + 0.20f, z, 1.35f * scale, 0.40f * scale, 1.35f * scale, gold)
        drawCylinder(x, y + 1.05f, z, 0.62f * scale, 1.85f * scale, 0.62f * scale, darkMetal)
        drawCylinder(x, y + 1.75f, z, 0.82f * scale, 0.26f * scale, 0.82f * scale, gold)
        drawSphere(x, y + 2.15f * scale, z, 0.62f * scale, sapphireGlow, white, pattern = 4)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y + 0.85f, z)
        Matrix.rotateM(modelMatrix, 0, rotDeg, 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, 1.35f * scale, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, 0.26f * scale, 2.65f * scale, 0.26f * scale)
        drawMeshWithModel(cylMesh, gold)

        val headX = x + cos(angleRad) * 2.55f * scale
        val headZ = z + sin(angleRad) * 2.55f * scale
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, headX, y + 0.85f, headZ)
        Matrix.rotateM(modelMatrix, 0, rotDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 1.12f * scale, 1.95f * scale, 1.12f * scale)
        drawMeshWithModel(cylMesh, c1, gold, pattern = 1)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, headX, y + 0.85f, headZ)
        Matrix.rotateM(modelMatrix, 0, rotDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 1.24f * scale, 0.45f * scale, 1.24f * scale)
        drawMeshWithModel(cylMesh, gold, c2, pattern = 4)
    }

    private fun drawFinishArch(x: Float, y: Float, z: Float) {
        val marble = Rgba.fromHex(0xF8FAFC)
        for (side in listOf(-3.6f, 3.6f)) {
            drawBlock(x + side, y + 0.25f, z, 1.35f, 0.5f, 1.35f, gold)
            drawCylinder(x + side, y + 2.4f, z, 0.66f, 4.8f, 0.66f, marble)
            drawBlock(x + side, y + 4.9f, z, 1.35f, 0.48f, 1.35f, gold)
        }
        drawBlock(x, y + 5.35f, z, 8.8f, 1.15f, 0.90f, gold, sapphireGlow, pattern = 2)
        drawBlock(x, y + 2.4f, z, 6.2f, 4.3f, 0.18f, sapphireGlow, white, pattern = 4)
        drawCone(x - 1.3f, y + 6.3f, z, 0.95f, 0.85f, gold, pattern = 4)
        drawCone(x, y + 6.55f, z, 1.30f, 1.20f, gold, pattern = 4)
        drawCone(x + 1.3f, y + 6.3f, z, 0.95f, 0.85f, gold, pattern = 4)
    }

    private fun drawSpinningCoin(x: Float, y: Float, z: Float, rotY: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.76f, 0.18f, 0.76f)
        drawMeshWithModel(cylMesh, gold, white, pattern = 4)
    }

    private fun drawPowerUp(pu: PowerUpDef, t: Float) {
        val bob = sin(t * 3.8f + pu.z) * 0.22f
        val py = pu.y + bob
        val rotY = t * 110f + pu.z * 14f

        // Glowing ground halo ring
        val haloCol = when (pu.type) {
            PowerUpType.SHIELD -> sapphireGlow
            PowerUpType.MAGNET -> gold
            PowerUpType.WINGS -> greenCheck
        }
        drawCylinder(pu.x, pu.y - 0.85f, pu.z, 1.35f, 0.06f, 1.35f, haloCol, pattern = 4)

        when (pu.type) {
            PowerUpType.SHIELD -> {
                // Golden & Sapphire Aegis Shield Emblem
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, pu.x, py, pu.z)
                Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.92f, 1.05f, 0.26f)
                drawMeshWithModel(sphereMesh, gold, sapphireGlow, pattern = 4)
                drawSphere(pu.x, py, pu.z, 0.56f, sapphireGlow, white, pattern = 4)
            }
            PowerUpType.MAGNET -> {
                // Royal Crimson & Gold Magnet Orb
                drawSphere(pu.x, py, pu.z, 0.72f, redStripe, gold, pattern = 4)
                drawCylinder(pu.x, py, pu.z, 1.15f, 0.16f, 1.15f, gold, rotZ = 45f, pattern = 4)
            }
            PowerUpType.WINGS -> {
                // Hermes Golden Winged Core
                drawSphere(pu.x, py, pu.z, 0.62f, greenCheck, white, pattern = 4)
                val wingFlap = sin(t * 8.5f) * 0.14f
                drawBlock(pu.x - 0.58f, py + 0.12f + wingFlap, pu.z, 0.68f, 0.26f, 0.14f, gold, white, pattern = 4)
                drawBlock(pu.x + 0.58f, py + 0.12f + wingFlap, pu.z, 0.68f, 0.26f, 0.14f, gold, white, pattern = 4)
            }
        }
    }

    private fun drawTreasureChestAndBossSentinel(tc: RuntimeTreasureChest, theme: WorldTheme, t: Float) {
        val x = tc.def.x
        val y = tc.def.y
        val z = tc.def.z
        val tier = tc.def.tier

        val (bodyCol, trimCol, lidCol, chestPattern, ringCol) = when (tier) {
            ChestTier.WOODEN -> Quintuple(
                Rgba.fromHex(0x8D5524),
                goldDark,
                Rgba.fromHex(0xA66A38),
                6, // wood grain shader pattern
                goldDark
            )
            ChestTier.METALLIC -> Quintuple(
                Rgba.fromHex(0x64748B),
                marbleStone,
                Rgba.fromHex(0x94A3B8),
                7, // brushed metal & rivets shader pattern
                sapphireGlow
            )
            ChestTier.MAGIC_RARE -> Quintuple(
                amethystGlow,
                sapphireGlow,
                Rgba.fromHex(0x7209B7),
                8, // faceted glowing crystal shader pattern
                sapphireGlow
            )
            ChestTier.EVENT_BOSS -> Quintuple(
                darkMetal,
                gold,
                redStripe,
                9, // royal rune shader pattern
                redStripe
            )
        }

        // Pedestal ring beneath chest
        drawCylinder(x, y + 0.05f, z, 2.1f, 0.08f, 2.1f, ringCol, pattern = 4)

        // Chest lower body with tier-specific material & shader pattern
        drawBlock(x, y + 0.36f, z, 1.45f, 0.62f, 0.96f, bodyCol, trimCol, pattern = chestPattern)
        // Side reinforcing straps
        drawBlock(x - 0.55f, y + 0.36f, z, 0.18f, 0.66f, 1.02f, trimCol)
        drawBlock(x + 0.55f, y + 0.36f, z, 0.18f, 0.66f, 1.02f, trimCol)
        if (tier == ChestTier.METALLIC || tier == ChestTier.EVENT_BOSS) {
            // Center heavy reinforcement rib for Metallic & Royal Boss chests
            drawBlock(x, y + 0.36f, z, 0.20f, 0.65f, 1.01f, trimCol)
        }
        // Rear cylindrical hinges & front corner rivets/studs
        for (hx in listOf(-0.48f, 0.48f)) {
            drawCylinder(x + hx, y + 0.68f, z - 0.48f, 0.14f, 0.22f, 0.14f, trimCol, rotZ = 90f)
            drawSphere(x + hx, y + 0.54f, z + 0.49f, 0.11f, trimCol)
            drawSphere(x + hx, y + 0.20f, z + 0.49f, 0.11f, trimCol)
        }
        // Front lock clasp & engraved keyhole/rune plate
        drawBlock(x, y + 0.42f, z + 0.50f, 0.32f, 0.36f, 0.12f, trimCol)
        drawSphere(x, y + 0.42f, z + 0.55f, 0.16f, if (tc.opened) sapphireGlow else ringCol, white, pattern = 4)

        // Animated chest lid (swings open smoothly when collected!)
        val lidLift = tc.openAnim * 0.44f
        val lidBack = tc.openAnim * 0.36f
        val lidY = y + 0.78f + lidLift
        val lidZ = z - lidBack
        drawBlock(x, lidY, lidZ, 1.50f, 0.34f, 0.98f, lidCol, trimCol, pattern = chestPattern)
        // Tier-specific 3D lid craftsmanship & silhouette so every chest tier is recognizable by shape!
        when (tier) {
            ChestTier.WOODEN -> {
                // Rounded oak barrel-arch top beam & bronze straps
                drawCylinder(x, lidY + 0.14f, lidZ, 0.52f, 1.46f, 0.52f, bodyCol, rotZ = 90f, pattern = 6)
            }
            ChestTier.METALLIC -> {
                // Reinforced chamfered steel vault ridge & twin sapphire lock nodes
                drawBlock(x, lidY + 0.18f, lidZ, 1.24f, 0.14f, 0.72f, trimCol, sapphireGlow, pattern = 7)
            }
            ChestTier.MAGIC_RARE -> {
                // Floating corner crystal shards around the arcane reliquary lid
                drawCone(x - 0.62f, lidY + 0.26f, lidZ, 0.24f, 0.45f, sapphireGlow, pattern = 8)
                drawCone(x + 0.62f, lidY + 0.26f, lidZ, 0.24f, 0.45f, sapphireGlow, pattern = 8)
            }
            ChestTier.EVENT_BOSS -> {
                // Grand 3-peak Royal Golden Crown mounted directly on the lid
                drawBlock(x, lidY + 0.20f, lidZ, 1.10f, 0.12f, 0.66f, gold)
                drawCone(x - 0.36f, lidY + 0.38f, lidZ + 0.15f, 0.22f, 0.28f, gold, pattern = 4)
                drawCone(x, lidY + 0.44f, lidZ + 0.18f, 0.28f, 0.38f, gold, pattern = 4)
                drawCone(x + 0.36f, lidY + 0.38f, lidZ + 0.15f, 0.22f, 0.28f, gold, pattern = 4)
            }
        }

        // Glowing treasure core inside when opened or tier emblem above when closed
        if (tc.opened) {
            drawSphere(x, y + 0.78f, z, 0.72f, Rgba.fromHex(tier.colorHex), gold, pattern = 4)
        } else {
            val bob = sin(t * 3.5f + z) * 0.14f
            when (tier) {
                ChestTier.MAGIC_RARE -> {
                    drawSphere(x, y + 1.45f + bob, z, 0.48f, sapphireGlow, amethystGlow, pattern = 8)
                }
                ChestTier.EVENT_BOSS -> {
                    drawCone(x, y + 1.54f + bob, z, 0.72f, 0.62f, gold, pattern = 4)
                }
                else -> {
                    drawCone(x, y + 1.38f + bob, z, 0.55f, 0.46f, trimCol, pattern = 4)
                }
            }
        }

        // If this is a Stage-5 World Gate Boss Sentinel Chest, render the towering World Boss Sentinel Statue behind it!
        if (tc.def.isBossChest) {
            val bz = z - 2.3f
            drawBlock(x, y + 0.35f, bz, 3.4f, 0.7f, 2.2f, darkMetal, gold, pattern = 2)
            drawBlock(x, y + 1.95f, bz, 2.2f, 2.6f, 1.35f, theme.obstaclePrimary, gold, pattern = 7)
            // Broad golden Pauldron Shoulders
            drawBlock(x - 1.55f, y + 2.85f, bz, 1.1f, 0.95f, 1.45f, gold, redStripe, pattern = 2)
            drawBlock(x + 1.55f, y + 2.85f, bz, 1.1f, 0.95f, 1.45f, gold, redStripe, pattern = 2)
            // Boss Head & Royal Horned Crown
            drawSphere(x, y + 3.85f, bz, 1.25f, darkMetal, gold, pattern = 9)
            drawCone(x, y + 4.85f, bz, 1.35f, 1.15f, gold, pattern = 4)
            // Glowing Boss Sentinel Eyes
            val eyePulse = 0.24f + 0.05f * sin(t * 6f)
            drawSphere(x - 0.34f, y + 3.92f, bz + 0.56f, eyePulse, redStripe, gold, pattern = 4)
            drawSphere(x + 0.34f, y + 3.92f, bz + 0.56f, eyePulse, redStripe, gold, pattern = 4)
        }
    }

    private data class Quintuple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private fun drawSceneryBackdrop(theme: WorldTheme, t: Float, baseZ: Float) {
        val castleX = 13.5f
        val castleZ = baseZ - 14f
        drawCone(castleX, 2.5f, castleZ, 12f, -7.5f, earthRock)
        drawBlock(castleX, 6.2f, castleZ, 12.5f, 1.8f, 9.5f, greenCheck)
        drawBlock(castleX, 7.4f, castleZ, 10.5f, 1.6f, 7.5f, marbleStone, gold, pattern = 2)
        for (tx in listOf(-3.8f, 0f, 3.8f)) {
            val th = if (tx == 0f) 7.5f else 5.4f
            val tw = if (tx == 0f) 2.2f else 1.5f
            drawCylinder(castleX + tx, 8.2f + th * 0.5f, castleZ, tw, th, tw, marbleStone)
            drawCone(castleX + tx, 8.2f + th + 1.6f, castleZ, tw * 1.45f, 3.2f, gold)
        }
        drawBlock(castleX - 2.6f, 1.0f, castleZ + 4.4f, 2.1f, 9.5f, 0.45f, sapphireGlow, white, pattern = 4)
        drawBlock(castleX + 2.6f, 1.0f, castleZ + 4.4f, 1.8f, 8.8f, 0.45f, sapphireGlow, white, pattern = 4)

        val leftX = -14.5f
        val leftZ = baseZ - 11f
        drawWorldSideLandmark(theme, leftX, 5.2f, leftZ, -1f, 99, t, world.graphicsQuality)

        val moonY = 16.0f + sin(t * 0.5f) * 0.6f
        drawSphere(-16f, moonY, baseZ - 18f, 4.6f, sapphireGlow, white, pattern = 4)

        val orb2Y = 11.5f + cos(t * 0.7f) * 0.9f
        drawSphere(9f, orb2Y, baseZ + 10f, 2.4f, gold, sapphireGlow, pattern = 1)
        drawBlock(9f, orb2Y - 2.0f, baseZ + 10f, 0.8f, 0.7f, 0.8f, gold)
    }

    private fun drawBlock(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, col: Rgba, alt: Rgba = col, pattern: Int = 0) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMeshWithModel(boxMesh, col, alt, pattern)
    }

    private fun drawSphere(x: Float, y: Float, z: Float, s: Float, col: Rgba, alt: Rgba = col, pattern: Int = 0) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, s, s, s)
        drawMeshWithModel(sphereMesh, col, alt, pattern)
    }

    private fun drawCylinder(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, col: Rgba, rotZ: Float = 0f, pattern: Int = 0) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (rotZ != 0f) Matrix.rotateM(modelMatrix, 0, rotZ, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        drawMeshWithModel(cylMesh, col, col, pattern)
    }

    private fun drawCone(x: Float, y: Float, z: Float, w: Float, h: Float, col: Rgba, pattern: Int = 0) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, w, h, w)
        drawMeshWithModel(coneMesh, col, col, pattern)
    }

    private fun drawMeshWithModel(mesh: GlMesh, col: Rgba, alt: Rgba = col, pattern: Int = 0, roughness: Float = 0.48f, metallic: Float = 0.12f) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(cartoonShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(cartoonShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(cartoonShader.uBaseColorLoc, col.r, col.g, col.b, col.a)
        GLES30.glUniform4f(cartoonShader.uAltColorLoc, alt.r, alt.g, alt.b, alt.a)
        GLES30.glUniform1f(cartoonShader.uRoughnessLoc, roughness)
        GLES30.glUniform1f(cartoonShader.uMetallicLoc, metallic)
        GLES30.glUniform1i(cartoonShader.uPatternModeLoc, pattern)
        mesh.draw()
    }
}
