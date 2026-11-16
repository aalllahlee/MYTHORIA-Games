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
    private val darkMetal = Rgba.fromHex(0x2B2D42)
    private val redStripe = Rgba.fromHex(0xFF3B5C)
    private val greenCheck = Rgba.fromHex(0x2EE58D)

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
        GLES30.glUniform3f(cartoonShader.uSunDirLoc, 0.55f, 0.85f, 0.42f)
        GLES30.glUniform3f(cartoonShader.uCamPosLoc, world.camX, world.camY, world.camZ)
        GLES30.glUniform3f(cartoonShader.uFogColorLoc, theme.skyBottom.r, theme.skyBottom.g, theme.skyBottom.b)
        GLES30.glUniform1f(cartoonShader.uTimeLoc, world.worldTime)

        if (world.isMenuShowcase) {
            drawMenuShowcaseScene(theme)
        } else {
            drawGameplayScene(theme)
        }
    }

    private fun drawSky(theme: WorldTheme) {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glUseProgram(skyShader.programId)
        GLES30.glUniform3f(skyShader.uTopColorLoc, theme.skyTop.r, theme.skyTop.g, theme.skyTop.b)
        GLES30.glUniform3f(skyShader.uBottomColorLoc, theme.skyBottom.r, theme.skyBottom.g, theme.skyBottom.b)
        GLES30.glUniform3f(skyShader.uSunColorLoc, theme.sunColor.r, theme.sunColor.g, theme.sunColor.b)
        quadMesh.draw()
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
    }

    private fun drawMenuShowcaseScene(theme: WorldTheme) {
        val t = world.worldTime
        val marble = Rgba.fromHex(0xF1F5F9)
        val sapphire = Rgba.fromHex(0x00E5FF)
        val flameOrange = Rgba.fromHex(0xFF9F1C)

        // Central Ancient Greek Marble & Royal Gold Pedestal Platform
        drawCylinder(0f, -0.55f, 0f, 6.4f, 1.1f, 6.4f, gold)
        drawCylinder(0f, -0.10f, 0f, 5.4f, 0.45f, 5.4f, marble)
        drawCylinder(0f, 0.14f, 0f, 4.4f, 0.12f, 4.4f, sapphire, pattern = 4)

        // Twin Foreground Golden Fire Braziers
        for (side in listOf(-3.2f, 3.2f)) {
            drawCylinder(side, 0.35f, 1.2f, 0.75f, 0.7f, 0.75f, gold)
            val flicker = 0.55f + 0.12f * sin(t * 7.5f + side)
            drawCone(side, 0.95f, 1.2f, flicker, 1.1f, flameOrange, pattern = 4)
        }

        // Floating Temple Side Islands & Colonnade
        drawBlock(-4.8f, 0.2f + sin(t * 1.4f) * 0.35f, -4.5f, 3.6f, 0.9f, 3.6f, marble, gold, pattern = 2)
        drawBlock(4.8f, 0.4f + cos(t * 1.2f) * 0.35f, -5.2f, 3.6f, 0.9f, 3.6f, marble, sapphire, pattern = 2)
        drawBlock(0f, 0.3f, -10.5f, 8.8f, 1.2f, 7.5f, gold, marble, pattern = 2)

        // Rotating Temple Hammer & Glowing Sapphire Portal Finish Arch in Background
        drawRotatingHammer(0f, 0.9f, -10.5f, t * 1.4f, 1.0f, sapphire, gold)
        drawFinishArch(0f, 0.9f, -13.0f)

        // Distant Mythological Sky Temple, Moon & Floating Waterfall Islands
        drawSceneryBackdrop(theme, t, baseZ = -24f)

        // Hero Character in Center performing idle/victory vibe
        charRenderer.drawCharacter(
            cartoonShader, vpMatrix,
            0f, 0.15f, 0.2f,
            15f + sin(t * 0.7f) * 18f,
            CharAnimState.IDLE, t,
            world.selectedSkin, world.selectedHatId, world.selectedShoeId
        )

        // Companion Olympian skins cheering on side temple islands
        if (world.graphicsQuality != GraphicsQuality.LOW) {
            charRenderer.drawCharacter(
                cartoonShader, vpMatrix,
                -4.8f, 0.65f + sin(t * 1.4f) * 0.35f, -4.5f,
                25f, CharAnimState.VICTORY_DANCE, t,
                GameCatalog.skins[2], "hat_crown", "shoe_gold"
            )
            charRenderer.drawCharacter(
                cartoonShader, vpMatrix,
                4.8f, 0.85f + cos(t * 1.2f) * 0.35f, -5.2f,
                -25f, CharAnimState.VICTORY_DANCE, t + 1.1f,
                GameCatalog.skins[1], "hat_none", "shoe_default"
            )
        }
    }

    private fun drawGameplayScene(theme: WorldTheme) {
        val t = world.worldTime
        val obsT = world.obstacleTime

        // Scenery backdrop ahead of player
        val backdropZ = if (world.currentMode == GameMode.ENDLESS) world.pz - 65f else world.currentLevel.finishZ - 26f
        drawSceneryBackdrop(theme, t, baseZ = backdropZ)

        // Platforms
        for (i in 0 until world.platforms.size) {
            val rp = world.platforms[i]
            if (!rp.active) continue
            // Frustum cull far behind camera
            if (rp.curZ > world.camZ + 22f || rp.curZ < world.camZ - 135f) continue

            val baseCol = rp.def.color ?: theme.platformPrimary
            val col = if (rp.warningPulse > 0.5f) redStripe else baseCol
            val pattern = when (rp.def.type) {
                PlatformType.SPEED_BOOST, PlatformType.CONVEYOR -> 3
                PlatformType.FINISH -> 2
                else -> 0
            }
            val alt = when (rp.def.type) {
                PlatformType.SPEED_BOOST -> gold
                PlatformType.CONVEYOR -> darkMetal
                PlatformType.FINISH -> white
                else -> theme.platformAccent
            }
            drawBlock(rp.curX, rp.curY, rp.curZ, rp.def.w, rp.def.h, rp.def.d, col, alt, pattern)

            if (rp.def.type == PlatformType.BOUNCE_PAD) {
                drawCylinder(rp.curX, rp.curY + rp.def.h * 0.5f + 0.14f, rp.curZ, rp.def.w * 0.65f, 0.25f, rp.def.d * 0.65f, gold)
            }
            if (rp.def.type == PlatformType.FINISH) {
                drawFinishArch(rp.curX, rp.curY + rp.def.h * 0.5f, rp.curZ)
            }
        }

        // MYTHORIA 3D Checkpoints (Ancient Magical Stone Pillars, Golden Runes, Glowing Sapphire Crystal Core & Crown Crest)
        val marbleStone = Rgba.fromHex(0xE2E8F0)
        val sapphireGlow = Rgba.fromHex(0x00E5FF)
        val deepSapphire = Rgba.fromHex(0x0077FF)
        val checkpoints = world.activeCheckpoints
        for (i in 0 until checkpoints.size) {
            val cp = checkpoints[i]
            if (abs(cp.z - world.pz) > 110f) continue
            val reached = world.highestCheckpointIndex >= cp.index
            val pulseRem = world.checkpointPulseTimers[cp.index] ?: 0f
            val pulseBoost = if (pulseRem > 0f) sin(pulseRem * 9f) * 0.25f + 0.35f else 0f

            // Glowing Activation Ring on the Sanctuary Platform
            val ringCol = if (reached) sapphireGlow else gold
            val ringScale = 3.2f + (if (reached) 0.15f * sin(t * 4f + cp.index) else 0f) + pulseBoost * 0.8f
            drawCylinder(cp.x, cp.y + 0.06f, cp.z, ringScale, 0.08f, ringScale, ringCol, pattern = 4)

            // Twin Ancient Marble/Stone Columns with Golden Rune Rings
            for (side in listOf(-2.25f, 2.25f)) {
                drawBlock(cp.x + side, cp.y + 0.22f, cp.z, 0.82f, 0.42f, 0.82f, gold)
                drawCylinder(cp.x + side, cp.y + 1.55f, cp.z, 0.44f, 2.6f, 0.44f, marbleStone)
                // Golden Rune Bands on Pillar
                drawCylinder(cp.x + side, cp.y + 1.05f + pulseBoost * 0.2f, cp.z, 0.52f, 0.22f, 0.52f, gold, pattern = 4)
                drawCylinder(cp.x + side, cp.y + 2.05f + pulseBoost * 0.2f, cp.z, 0.52f, 0.22f, 0.52f, if (reached) sapphireGlow else gold, pattern = 4)
                // Pillar Capital & Floating Sapphire Crystal Orb
                drawBlock(cp.x + side, cp.y + 2.95f, cp.z, 0.86f, 0.36f, 0.86f, gold)
                val crystalY = cp.y + 3.45f + sin(t * 3.2f + side) * 0.14f + pulseBoost * 0.35f
                drawSphere(cp.x + side, crystalY, cp.z, 0.48f + pulseBoost * 0.18f, if (reached) sapphireGlow else deepSapphire, white, pattern = 4)
            }

            // Overhead Mythoria Golden Rune Arch & Central Rotating Crown / Sapphire Gem
            val archCol = if (reached) sapphireGlow else gold
            val archAlt = if (reached) gold else white
            drawBlock(cp.x, cp.y + 3.15f + pulseBoost * 0.15f, cp.z, 5.2f, 0.48f, 0.48f, gold, archCol, pattern = 2)
            val gemY = cp.y + 3.82f + sin(t * 3.5f + cp.index) * 0.12f + pulseBoost * 0.3f
            drawCone(cp.x, gemY + 0.32f, cp.z, 0.85f, 0.65f, gold, pattern = 4)
            drawSphere(cp.x, gemY, cp.z, 0.58f + pulseBoost * 0.2f, archCol, archAlt, pattern = 4)
        }

        // Coins
        for (i in 0 until world.coins.size) {
            val rc = world.coins[i]
            if (rc.collected) continue
            if (abs(rc.def.z - world.pz) > 85f) continue
            val bob = sin(t * 4f + rc.def.z) * 0.18f
            drawSpinningCoin(rc.def.x, rc.def.y + bob, rc.def.z, t * 180f + rc.def.z * 20f)
        }

        // Obstacles
        val obstacles = world.activeObstacles
        for (i in 0 until obstacles.size) {
            val obs = obstacles[i]
            if (abs(obs.z - world.pz) > 95f) continue
            drawObstacle(obs, theme, obsT)
        }

        // Magical Blue & Gold Portal Effect for Local Player when Respawning at Checkpoint
        if (world.playerCheckpointState.isRespawning) {
            val pScale = world.playerCheckpointState.portalScale.coerceIn(0.15f, 1f)
            drawCylinder(world.px, world.py + 0.08f, world.pz, 2.4f * pScale, 0.12f, 2.4f * pScale, sapphireGlow, pattern = 4)
            drawCylinder(world.px, world.py + 0.95f * (1f - pScale * 0.4f), world.pz, 1.85f * pScale, 0.10f, 1.85f * pScale, gold, pattern = 4)
        }

        // Player Character
        charRenderer.drawCharacter(
            cartoonShader, vpMatrix,
            world.px, world.py, world.pz,
            world.playerYaw, world.animState, world.animTime,
            world.selectedSkin, world.selectedHatId, world.selectedShoeId,
            world.squashY,
            hitFlash = (world.hitStunTimer > 0f || world.playerCheckpointState.arrivalAnimTimerSec > 0.35f) && ((t * 20f).toInt() % 2 == 0),
            showLeaderCrown = world.currentMode == GameMode.RIVAL_RACE && world.computePlayerRaceRank() == 1
        )

        // AI Rival Racers (with independent portal rings if a specific rival is respawning)
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

        // Particles
        for (i in 0 until world.particles.size) {
            val p = world.particles[i]
            drawSphere(p.x, p.y, p.z, p.size, p.color, pattern = 4)
        }

        // Blob shadows beneath grounded characters (HIGH/MEDIUM quality)
        if (world.graphicsQuality != GraphicsQuality.LOW) {
            GLES30.glUseProgram(shadowShader.programId)
            if (world.grounded) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, world.px, world.py + 0.03f, world.pz)
                Matrix.scaleM(modelMatrix, 0, 1.05f, 1f, 1.05f)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(shadowShader.uMvpLoc, 1, false, mvpMatrix, 0)
                GLES30.glUniform1f(shadowShader.uAlphaLoc, 0.48f)
                shadowMesh.draw()
            }
            for (i in 0 until rivals.size) {
                val r = rivals[i]
                if (!r.grounded || abs(r.z - world.pz) > 80f) continue
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, r.x, r.y + 0.03f, r.z)
                Matrix.scaleM(modelMatrix, 0, 1.0f, 1f, 1.0f)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES30.glUniformMatrix4fv(shadowShader.uMvpLoc, 1, false, mvpMatrix, 0)
                GLES30.glUniform1f(shadowShader.uAlphaLoc, 0.42f)
                shadowMesh.draw()
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
                drawCylinder(obs.x, obs.y + 0.7f, obs.z, 0.6f, 1.5f, 0.6f, darkMetal)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, obs.x, obs.y + 0.75f, obs.z)
                Matrix.rotateM(modelMatrix, 0, Math.toDegrees(-phaseT.toDouble()).toFloat(), 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, obs.range * 1.9f, 0.45f * obs.scale, 0.45f * obs.scale)
                drawMeshWithModel(boxMesh, theme.obstaclePrimary, theme.obstacleSecondary, pattern = 1)
            }
            ObstacleType.SPINNING_BLADE -> {
                drawCylinder(obs.x, obs.y + 0.7f, obs.z, 0.7f, 1.4f, 0.7f, darkMetal)
                for (a in listOf(0f, 90f)) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, obs.x, obs.y + 0.75f, obs.z)
                    Matrix.rotateM(modelMatrix, 0, Math.toDegrees(-phaseT.toDouble()).toFloat() + a, 0f, 1f, 0f)
                    Matrix.scaleM(modelMatrix, 0, obs.range * 1.9f, 0.25f, 0.55f)
                    drawMeshWithModel(boxMesh, Rgba.fromHex(0xE2E8F0), redStripe, pattern = 1)
                }
            }
            ObstacleType.ROLLING_BALL -> {
                val ox = obs.x + sin(phaseT) * obs.range
                drawSphere(ox, obs.y, obs.z, 2.0f * obs.scale, darkMetal)
                drawCylinder(ox, obs.y, obs.z, 2.05f * obs.scale, 0.42f * obs.scale, 2.05f * obs.scale, redStripe)
            }
            ObstacleType.SWINGING_PENDULUM, ObstacleType.GIANT_SWINGER -> {
                val swing = sin(phaseT) * 1.1f
                val bx = obs.x + sin(swing) * 3.4f * obs.scale
                val by = obs.y + 4.2f - cos(swing) * 3.4f * obs.scale
                drawBlock(obs.x, obs.y + 4.4f, obs.z, 0.6f, 0.6f, 0.6f, darkMetal)
                // Rod + wrecking sphere
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, obs.x, obs.y + 4.2f, obs.z)
                Matrix.rotateM(modelMatrix, 0, Math.toDegrees(swing.toDouble()).toFloat(), 0f, 0f, 1f)
                Matrix.translateM(modelMatrix, 0, 0f, -1.7f * obs.scale, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.18f, 3.4f * obs.scale, 0.18f)
                drawMeshWithModel(cylMesh, darkMetal)
                drawSphere(bx, by, obs.z, 1.9f * obs.scale, theme.obstaclePrimary, theme.obstacleSecondary, pattern = 1)
            }
            ObstacleType.MOVING_WALL, ObstacleType.SLIDING_OBSTACLE, ObstacleType.PUSHER -> {
                val ox = obs.x + sin(phaseT) * obs.range
                drawBlock(ox, obs.y + 0.9f, obs.z, 1.9f * obs.scale, 1.8f * obs.scale, 1.3f * obs.scale, theme.obstaclePrimary, theme.obstacleSecondary, pattern = 1)
            }
            ObstacleType.JUMPING_BLOCK -> {
                val oy = obs.y + 0.7f + abs(sin(phaseT)) * 2.4f
                drawBlock(obs.x, oy, obs.z, 1.8f * obs.scale, 1.4f * obs.scale, 1.4f * obs.scale, theme.obstacleSecondary, theme.obstaclePrimary, pattern = 1)
            }
            ObstacleType.FIRE_JET -> {
                drawCylinder(obs.x, obs.y + 0.15f, obs.z, 1.3f, 0.35f, 1.3f, darkMetal)
                if (sin(phaseT) > 0.1f) {
                    drawCone(obs.x, obs.y + 1.3f, obs.z, 1.1f * obs.scale, 2.2f * obs.scale, Rgba.fromHex(0xFF5400), pattern = 4)
                }
            }
            ObstacleType.LASER_BEAM -> {
                drawCylinder(obs.x - obs.range, obs.y + 0.8f, obs.z, 0.35f, 1.6f, 0.35f, darkMetal)
                drawCylinder(obs.x + obs.range, obs.y + 0.8f, obs.z, 0.35f, 1.6f, 0.35f, darkMetal)
                if (sin(phaseT) > -0.25f) {
                    drawCylinder(obs.x, obs.y + 0.8f, obs.z, 0.22f, obs.range * 2f, 0.22f, Rgba.fromHex(0xFF1E56), rotZ = 90f, pattern = 4)
                }
            }
        }
    }

    private fun drawRotatingHammer(x: Float, y: Float, z: Float, angleRad: Float, scale: Float, c1: Rgba, c2: Rgba) {
        drawCylinder(x, y + 1.1f, z, 0.55f * scale, 2.2f * scale, 0.55f * scale, darkMetal)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y + 0.85f, z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(-angleRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, 1.4f * scale, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, 0.22f * scale, 2.6f * scale, 0.22f * scale)
        drawMeshWithModel(cylMesh, c2)

        val headX = x + cos(angleRad) * 2.55f * scale
        val headZ = z + sin(angleRad) * 2.55f * scale
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, headX, y + 0.85f, headZ)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(-angleRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 1.05f * scale, 1.9f * scale, 1.05f * scale)
        drawMeshWithModel(cylMesh, c1, c2, pattern = 1)
    }

    private fun drawFinishArch(x: Float, y: Float, z: Float) {
        val marble = Rgba.fromHex(0xF8FAFC)
        val sapphire = Rgba.fromHex(0x00E5FF)
        // Left & Right Marble Temple Columns with Golden Capitals
        drawCylinder(x - 3.6f, y + 2.4f, z, 0.62f, 4.8f, 0.62f, marble)
        drawCylinder(x + 3.6f, y + 2.4f, z, 0.62f, 4.8f, 0.62f, marble)
        drawBlock(x - 3.6f, y + 4.9f, z, 1.3f, 0.45f, 1.3f, gold)
        drawBlock(x + 3.6f, y + 4.9f, z, 1.3f, 0.45f, 1.3f, gold)
        // Top Golden Temple Architrave & Glowing Blue Sapphire Portal Core
        drawBlock(x, y + 5.3f, z, 8.6f, 1.1f, 0.85f, gold, sapphire, pattern = 2)
        drawBlock(x, y + 2.4f, z, 6.2f, 4.2f, 0.16f, sapphire, white, pattern = 4)
        // Royal Crown Peak on Top of Portal Arch
        drawCone(x, y + 6.4f, z, 1.25f, 1.1f, gold, pattern = 4)
    }

    private fun drawSpinningCoin(x: Float, y: Float, z: Float, rotY: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.72f, 0.16f, 0.72f)
        drawMeshWithModel(cylMesh, gold, white, pattern = 4)
    }

    private fun drawSceneryBackdrop(theme: WorldTheme, t: Float, baseZ: Float) {
        val marble = Rgba.fromHex(0xF1F5F9)
        val sapphire = Rgba.fromHex(0x00E5FF)

        // Distant Ancient Greek Colonnade Sanctuary & Glowing Portal
        drawBlock(14f, 6f, baseZ - 14f, 10f, 2.2f, 8f, marble)
        drawCylinder(10.5f, 10.2f, baseZ - 14f, 1.2f, 6.2f, 1.2f, marble)
        drawCylinder(14.0f, 10.2f, baseZ - 14f, 1.2f, 6.2f, 1.2f, marble)
        drawCylinder(17.5f, 10.2f, baseZ - 14f, 1.2f, 6.2f, 1.2f, marble)
        drawCone(14.0f, 14.8f, baseZ - 14f, 9.5f, 3.2f, gold)
        // Cascading Sapphire Waterfall beneath Temple Island
        drawBlock(14f, 1.2f, baseZ - 10f, 2.6f, 8.5f, 0.45f, sapphire, white, pattern = 4)

        // Luminous Blue Moon & Floating Sanctuary Orbs
        val moonY = 14.5f + sin(t * 0.5f) * 0.6f
        drawSphere(-14f, moonY, baseZ - 8f, 4.6f, sapphire, white, pattern = 4)

        val orb2Y = 11.5f + cos(t * 0.7f) * 0.9f
        drawSphere(9f, orb2Y, baseZ + 10f, 2.4f, gold, sapphire, pattern = 1)
        drawBlock(9f, orb2Y - 2.0f, baseZ + 10f, 0.8f, 0.7f, 0.8f, gold)

        // Fluffy sky temple clouds
        if (world.graphicsQuality != GraphicsQuality.LOW) {
            for (i in 0..3) {
                val cx = -18f + i * 12f + sin(t * 0.25f + i) * 1.5f
                val cy = 5f + (i % 2) * 3.5f
                val cz = baseZ + 12f - i * 4f
                drawSphere(cx, cy, cz, 2.8f, white)
                drawSphere(cx + 1.6f, cy - 0.3f, cz, 2.2f, white)
                drawSphere(cx - 1.6f, cy - 0.3f, cz, 2.2f, white)
            }
        }
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

    private fun drawMeshWithModel(mesh: GlMesh, col: Rgba, alt: Rgba = col, pattern: Int = 0) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(cartoonShader.uModelLoc, 1, false, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(cartoonShader.uMvpLoc, 1, false, mvpMatrix, 0)
        GLES30.glUniform4f(cartoonShader.uBaseColorLoc, col.r, col.g, col.b, col.a)
        GLES30.glUniform4f(cartoonShader.uAltColorLoc, alt.r, alt.g, alt.b, alt.a)
        GLES30.glUniform1i(cartoonShader.uPatternModeLoc, pattern)
        mesh.draw()
    }
}
