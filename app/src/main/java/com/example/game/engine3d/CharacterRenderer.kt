package com.example.game.engine3d

import android.opengl.GLES30
import android.opengl.Matrix
import com.example.game.model.HeadAccessoryStyle
import com.example.game.model.Rgba
import com.example.game.model.SkinDef
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

enum class CharAnimState {
    IDLE,
    WALK,
    RUN,
    SPRINT,
    JUMP,
    FALL,
    LAND,
    SLIDE,
    DIVE,
    HIT,
    STUMBLE,
    VICTORY_DANCE,
    DEFEAT,
    FLOAT
}

class CharacterRenderer(
    private val boxMesh: GlMesh,
    private val sphereMesh: GlMesh,
    private val cylMesh: GlMesh,
    private val coneMesh: GlMesh
) {
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val root = FloatArray(16)
    private val tmp = FloatArray(16)

    private val white = Rgba(1f, 1f, 1f, 1f)
    private val pupilDark = Rgba(0.06f, 0.06f, 0.12f, 1f)
    private val gold = Rgba(1f, 0.84f, 0.18f, 1f)
    private val goldLight = Rgba(1f, 0.94f, 0.55f, 1f)
    private val cyan = Rgba(0f, 0.88f, 1f, 1f)
    private val sapphire = Rgba(0.05f, 0.58f, 1f, 1f)
    private val rubyRed = Rgba(0.95f, 0.14f, 0.34f, 1f)
    private val blushPink = Rgba(1f, 0.52f, 0.66f, 1f)
    private val rosePink = Rgba(1f, 0.30f, 0.58f, 1f)
    private val violetIris = Rgba(0.68f, 0.22f, 0.92f, 1f)
    private val emeraldIris = Rgba(0.08f, 0.82f, 0.58f, 1f)
    private val darkSlate = Rgba(0.16f, 0.20f, 0.28f, 1f)

    // Material presets (roughness, metallic) so cloth, skin, hair, leather, gold, and gems never look uniformly plastic
    private val ROUGH_CLOTH = 0.78f
    private val METAL_CLOTH = 0.02f
    private val ROUGH_SKIN = 0.68f
    private val METAL_SKIN = 0.00f
    private val ROUGH_HAIR = 0.44f
    private val METAL_HAIR = 0.08f
    private val ROUGH_LEATHER = 0.52f
    private val METAL_LEATHER = 0.10f
    private val ROUGH_GOLD = 0.20f
    private val METAL_GOLD = 0.88f
    private val ROUGH_GEM = 0.12f
    private val METAL_GEM = 0.38f

    fun drawCharacter(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        x: Float,
        y: Float,
        z: Float,
        yawDeg: Float,
        anim: CharAnimState,
        animTime: Float,
        skin: SkinDef,
        hatId: String,
        shoeId: String,
        squashY: Float = 1f,
        hitFlash: Boolean = false,
        showLeaderCrown: Boolean = false,
        turnLeanDeg: Float = 0f,
        accelPitchDeg: Float = 0f
    ) {
        val shoeColor = when (shoeId) {
            "shoe_gold" -> gold
            "shoe_rocket" -> cyan
            "shoe_shadow" -> Rgba.fromHex(0xC83BFF)
            else -> skin.shoeColor
        }
        val shoeRough = if (shoeId == "shoe_gold" || shoeId == "shoe_rocket") ROUGH_GOLD else ROUGH_LEATHER
        val shoeMetal = if (shoeId == "shoe_gold") METAL_GOLD else METAL_LEATHER

        val isSalma = skin.headStyle == HeadAccessoryStyle.SALMA_STAR_PRINCESS || skin.id == "salma_heroine"
        val isKidAhmed = skin.headStyle == HeadAccessoryStyle.KID_AHMED_TURBO || skin.id == "kid_ahmed"

        // Smooth animation pose parameters
        var bobY = 0f
        var leanFwd = accelPitchDeg.coerceIn(-8f, 10f)
        var rollZ = turnLeanDeg
        var lArmX = 0f
        var rArmX = 0f
        var lLegX = 0f
        var rLegX = 0f
        var armSpread = 16f
        val isAirborne = (anim == CharAnimState.JUMP || anim == CharAnimState.FALL || anim == CharAnimState.FLOAT)

        when (anim) {
            CharAnimState.IDLE -> {
                bobY = sin(animTime * 3.2f) * 0.045f
                lArmX = sin(animTime * 2.5f) * 7f
                rArmX = -lArmX
            }
            CharAnimState.WALK, CharAnimState.RUN, CharAnimState.SPRINT -> {
                val freq = when (anim) {
                    CharAnimState.WALK -> 7.5f
                    CharAnimState.RUN -> 12.0f
                    else -> 15.5f
                }
                val amp = when (anim) {
                    CharAnimState.WALK -> 30f
                    CharAnimState.RUN -> 54f
                    else -> 70f
                }
                bobY = abs(sin(animTime * freq)) * 0.095f
                leanFwd += if (anim == CharAnimState.SPRINT) 17f else 10f
                lLegX = sin(animTime * freq) * amp
                rLegX = -lLegX
                lArmX = -lLegX * 0.85f
                rArmX = lLegX * 0.85f
            }
            CharAnimState.JUMP -> {
                leanFwd = -7f
                lArmX = -125f
                rArmX = -125f
                armSpread = 32f
                lLegX = 24f
                rLegX = -16f
            }
            CharAnimState.FALL -> {
                leanFwd = 9f
                lArmX = -140f + sin(animTime * 16f) * 18f
                rArmX = -140f - sin(animTime * 16f) * 18f
                armSpread = 45f
                lLegX = sin(animTime * 14f) * 18f
                rLegX = -lLegX
            }
            CharAnimState.LAND -> {
                bobY = -0.09f
                leanFwd = 13f
                lArmX = 26f
                rArmX = 26f
                armSpread = 30f
            }
            CharAnimState.SLIDE -> {
                leanFwd = -35f
                bobY = -0.22f
                lLegX = -60f
                rLegX = -50f
                lArmX = 35f
                rArmX = 35f
            }
            CharAnimState.DIVE -> {
                leanFwd = 72f
                bobY = 0.25f
                lArmX = -155f
                rArmX = -155f
                lLegX = 30f
                rLegX = 30f
            }
            CharAnimState.HIT, CharAnimState.STUMBLE -> {
                leanFwd = -22f
                rollZ = sin(animTime * 22f) * 25f
                lArmX = -95f
                rArmX = -95f
                armSpread = 48f
            }
            CharAnimState.VICTORY_DANCE -> {
                bobY = abs(sin(animTime * 8f)) * 0.28f
                rollZ = sin(animTime * 6f) * 12f
                lArmX = -145f + sin(animTime * 12f) * 25f
                rArmX = -145f - sin(animTime * 12f) * 25f
                lLegX = sin(animTime * 8f) * 24f
                rLegX = -lLegX
            }
            CharAnimState.DEFEAT -> {
                leanFwd = 35f
                bobY = -0.14f
                lArmX = 25f
                rArmX = 25f
            }
            CharAnimState.FLOAT -> {
                bobY = sin(animTime * 4f) * 0.14f
                lArmX = -80f + sin(animTime * 5f) * 20f
                rArmX = -80f - sin(animTime * 5f) * 20f
                lLegX = sin(animTime * 4f) * 18f
                rLegX = -lLegX
            }
        }

        val scaleOverall = if (isKidAhmed) 0.93f else 1.0f
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, x, y + bobY, z)
        Matrix.rotateM(root, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(root, 0, leanFwd, 1f, 0f, 0f)
        Matrix.rotateM(root, 0, rollZ, 0f, 0f, 1f)
        val sxz = (1f / sqrt(squashY.coerceIn(0.65f, 1.35f))) * scaleOverall
        Matrix.scaleM(root, 0, sxz, squashY * scaleOverall, sxz)

        val bodyCol = if (hitFlash) white else skin.hoodieColor

        // 1. Sculpted Torso, Belt, & Character-Specific Outfit Details (Cloth vs Gold vs Gem materials)
        if (isSalma) {
            // Salma's Rose-Pink Princess Dress Bodice + Puff Shoulder Sleeves + Two-Tiered Flared Pleated Skirt
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.78f, 0f, 0f, 0.62f, 0.64f, 0.50f, bodyCol, ROUGH_CLOTH, METAL_CLOTH)
            // White & gold sweetheart collar trim
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.98f, 0.08f, 0f, 0.44f, 0.18f, 0.34f, white, ROUGH_CLOTH, METAL_CLOTH)
            drawPart(shader, vpMatrix, sphereMesh, -0.34f, 0.93f, 0f, 0f, 0.24f, 0.24f, 0.24f, rosePink, ROUGH_CLOTH, METAL_CLOTH)
            drawPart(shader, vpMatrix, sphereMesh, 0.34f, 0.93f, 0f, 0f, 0.24f, 0.24f, 0.24f, rosePink, ROUGH_CLOTH, METAL_CLOTH)
            // Golden waist ribbon & ruby heart brooch on chest
            drawPart(shader, vpMatrix, cylMesh, 0f, 0.60f, 0f, 0f, 0.56f, 0.09f, 0.46f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.84f, 0.26f, 0f, 0.14f, 0.14f, 0.08f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.84f, 0.29f, 0f, 0.09f, 0.09f, 0.06f, rubyRed, ROUGH_GEM, METAL_GEM)
            // Two-tiered flared pleated skirt (kept above y=0.35 so feet and landing contact are 100% visible)
            drawPart(shader, vpMatrix, coneMesh, 0f, 0.54f, 0f, 0f, 0.82f, 0.32f, 0.72f, bodyCol, ROUGH_CLOTH, METAL_CLOTH)
            drawPart(shader, vpMatrix, coneMesh, 0f, 0.45f, 0f, 0f, 0.88f, 0.20f, 0.78f, white, ROUGH_CLOTH, METAL_CLOTH)
            drawPart(shader, vpMatrix, cylMesh, 0f, 0.37f, 0f, 0f, 0.86f, 0.05f, 0.76f, gold, ROUGH_GOLD, METAL_GOLD)

            // Shimmering 4-Wing Fairy Butterfly Wings on Salma's Back
            val flap = sin(animTime * (if (isAirborne) 14f else 9f)) * 14f
            drawPartRot(shader, vpMatrix, sphereMesh, -0.28f, 0.90f, -0.24f, -12f, -20f - flap, 24f, 0.46f, 0.30f, 0.06f, goldLight, ROUGH_GEM, METAL_GEM)
            drawPartRot(shader, vpMatrix, sphereMesh, 0.28f, 0.90f, -0.24f, -12f, 20f + flap, -24f, 0.46f, 0.30f, 0.06f, goldLight, ROUGH_GEM, METAL_GEM)
            drawPartRot(shader, vpMatrix, sphereMesh, -0.22f, 0.70f, -0.23f, 10f, -16f - flap, 15f, 0.34f, 0.22f, 0.06f, cyan, ROUGH_GEM, METAL_GEM)
            drawPartRot(shader, vpMatrix, sphereMesh, 0.22f, 0.70f, -0.23f, 10f, 16f + flap, -15f, 0.34f, 0.22f, 0.06f, cyan, ROUGH_GEM, METAL_GEM)
        } else if (isKidAhmed) {
            // Little Ahmed's Sporty Electric-Blue & Emerald Racing Hoodie + Golden Headphones + Twin Rocket Jetpack
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.73f, 0f, 0f, 0.68f, 0.70f, 0.54f, bodyCol, ROUGH_CLOTH, METAL_CLOTH)
            // Center racing stripe & glowing star core
            drawPart(shader, vpMatrix, boxMesh, 0f, 0.74f, 0.25f, 0f, 0.16f, 0.46f, 0.06f, white, ROUGH_CLOTH, METAL_CLOTH)
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.78f, 0.29f, 0f, 0.15f, 0.15f, 0.07f, gold, ROUGH_GOLD, METAL_GOLD)
            // Golden Headphones around neck
            drawPart(shader, vpMatrix, cylMesh, 0f, 1.00f, 0.02f, 0f, 0.52f, 0.09f, 0.50f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, -0.25f, 1.01f, 0.10f, 0f, 0.15f, 0.16f, 0.12f, rubyRed, ROUGH_GEM, METAL_GEM)
            drawPart(shader, vpMatrix, sphereMesh, 0.25f, 1.01f, 0.10f, 0f, 0.15f, 0.16f, 0.12f, rubyRed, ROUGH_GEM, METAL_GEM)
            // Twin-Thruster 3D Rocket Jetpack on Back (kept high on upper back so feet are completely clear)
            drawPart(shader, vpMatrix, boxMesh, 0f, 0.80f, -0.26f, 0f, 0.38f, 0.36f, 0.14f, darkSlate, 0.32f, 0.75f)
            for (side in listOf(-0.14f, 0.14f)) {
                drawPart(shader, vpMatrix, cylMesh, side, 0.80f, -0.33f, 0f, 0.14f, 0.38f, 0.14f, white, 0.28f, 0.65f)
                drawPart(shader, vpMatrix, coneMesh, side, 1.03f, -0.33f, 0f, 0.15f, 0.16f, 0.15f, rubyRed, ROUGH_GOLD, METAL_GOLD)
                val jetLen = 0.16f + abs(sin(animTime * 18f + side)) * 0.10f
                drawPart(shader, vpMatrix, coneMesh, side, 0.55f, -0.33f, 180f, 0.12f, jetLen, 0.12f, cyan, ROUGH_GEM, METAL_GEM)
            }
        } else {
            // Royal Hero Ahmed: Crimson & Gold Armor Jacket + Golden Pauldrons + Flowing Royal Red Cape + 6-Feather Golden Angel Wings
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.73f, 0f, 0f, 0.70f, 0.72f, 0.56f, bodyCol, ROUGH_LEATHER, METAL_LEATHER)
            // Golden shoulder pauldrons
            drawPart(shader, vpMatrix, sphereMesh, -0.36f, 0.94f, 0f, 0f, 0.24f, 0.20f, 0.24f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, 0.36f, 0.94f, 0f, 0f, 0.24f, 0.20f, 0.24f, gold, ROUGH_GOLD, METAL_GOLD)
            // Royal golden belt & sapphire chest medallion
            drawPart(shader, vpMatrix, cylMesh, 0f, 0.55f, 0f, 0f, 0.66f, 0.10f, 0.54f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.80f, 0.28f, 0f, 0.17f, 0.17f, 0.07f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, 0f, 0.80f, 0.31f, 0f, 0.10f, 0.10f, 0.05f, cyan, ROUGH_GEM, METAL_GEM)
            // Flowing Royal Red Cape (billows backward during jumps/runs so it never obscures the feet!)
            val airborneLift = if (isAirborne) 24f else 0f
            val runLift = if (anim == CharAnimState.RUN || anim == CharAnimState.SPRINT) 18f else 0f
            val capeWave = 16f + sin(animTime * 7f) * 7f + runLift + airborneLift
            drawPart(shader, vpMatrix, boxMesh, 0f, 0.72f, -0.30f, capeWave, 0.56f, 0.58f, 0.05f, rubyRed, ROUGH_CLOTH, METAL_CLOTH)
            drawPart(shader, vpMatrix, boxMesh, 0f, 0.44f, -0.37f, capeWave + 5f, 0.62f, 0.09f, 0.06f, gold, ROUGH_GOLD, METAL_GOLD)
            // 6-Feather Golden Angel Wings on Back
            val wingFlap = sin(animTime * (if (isAirborne) 12f else 7.5f)) * 10f
            for (i in 0..2) {
                val wy = 0.92f - i * 0.11f
                val wSpan = 0.44f - i * 0.07f
                drawPartRot(shader, vpMatrix, boxMesh, -0.30f - i * 0.04f, wy, -0.26f, -8f, -18f - wingFlap, 22f - i * 8f, wSpan, 0.10f, 0.05f, gold, ROUGH_GOLD, METAL_GOLD)
                drawPartRot(shader, vpMatrix, boxMesh, 0.30f + i * 0.04f, wy, -0.26f, -8f, 18f + wingFlap, -22f + i * 8f, wSpan, 0.10f, 0.05f, gold, ROUGH_GOLD, METAL_GOLD)
            }
        }

        // 2. Sculpted Head & Expressive Multi-Layer Anime Eyes
        drawPart(shader, vpMatrix, sphereMesh, 0f, 1.28f, 0f, 0f, 0.68f, 0.65f, 0.64f, skin.skinColor, ROUGH_SKIN, METAL_SKIN)
        // Ears
        drawPart(shader, vpMatrix, sphereMesh, -0.33f, 1.27f, -0.02f, 0f, 0.11f, 0.14f, 0.09f, skin.skinColor, ROUGH_SKIN, METAL_SKIN)
        drawPart(shader, vpMatrix, sphereMesh, 0.33f, 1.27f, -0.02f, 0f, 0.11f, 0.14f, 0.09f, skin.skinColor, ROUGH_SKIN, METAL_SKIN)
        if (isSalma) {
            // Pearl & gold earrings for Salma
            drawPart(shader, vpMatrix, sphereMesh, -0.34f, 1.19f, 0.01f, 0f, 0.07f, 0.07f, 0.07f, gold, ROUGH_GOLD, METAL_GOLD)
            drawPart(shader, vpMatrix, sphereMesh, 0.34f, 1.19f, 0.01f, 0f, 0.07f, 0.07f, 0.07f, gold, ROUGH_GOLD, METAL_GOLD)
        }

        val irisCol = when {
            isSalma -> violetIris
            isKidAhmed -> emeraldIris
            else -> sapphire
        }
        // White sclera & glossy iris/catchlights
        drawPart(shader, vpMatrix, sphereMesh, -0.135f, 1.31f, 0.27f, 0f, 0.155f, 0.175f, 0.10f, white, 0.25f, 0.0f)
        drawPart(shader, vpMatrix, sphereMesh, 0.135f, 1.31f, 0.27f, 0f, 0.155f, 0.175f, 0.10f, white, 0.25f, 0.0f)
        drawPart(shader, vpMatrix, sphereMesh, -0.135f, 1.31f, 0.305f, 0f, 0.105f, 0.125f, 0.06f, irisCol, ROUGH_GEM, METAL_GEM)
        drawPart(shader, vpMatrix, sphereMesh, 0.135f, 1.31f, 0.305f, 0f, 0.105f, 0.125f, 0.06f, irisCol, ROUGH_GEM, METAL_GEM)
        drawPart(shader, vpMatrix, sphereMesh, -0.135f, 1.31f, 0.322f, 0f, 0.065f, 0.080f, 0.04f, pupilDark, 0.15f, 0.0f)
        drawPart(shader, vpMatrix, sphereMesh, 0.135f, 1.31f, 0.322f, 0f, 0.065f, 0.080f, 0.04f, pupilDark, 0.15f, 0.0f)
        drawPart(shader, vpMatrix, sphereMesh, -0.155f, 1.34f, 0.335f, 0f, 0.036f, 0.036f, 0.03f, white, 0.08f, 0.0f)
        drawPart(shader, vpMatrix, sphereMesh, 0.115f, 1.34f, 0.335f, 0f, 0.036f, 0.036f, 0.03f, white, 0.08f, 0.0f)
        // Rosy blush cheeks
        drawPart(shader, vpMatrix, sphereMesh, -0.21f, 1.23f, 0.26f, 0f, 0.11f, 0.065f, 0.05f, blushPink, ROUGH_SKIN, METAL_SKIN)
        drawPart(shader, vpMatrix, sphereMesh, 0.21f, 1.23f, 0.26f, 0f, 0.11f, 0.065f, 0.05f, blushPink, ROUGH_SKIN, METAL_SKIN)
        // Cute nose & happy smile
        drawPart(shader, vpMatrix, sphereMesh, 0f, 1.25f, 0.32f, 0f, 0.075f, 0.065f, 0.08f, skin.skinColor, ROUGH_SKIN, METAL_SKIN)
        drawPart(shader, vpMatrix, boxMesh, 0f, 1.18f, 0.305f, 0f, 0.11f, 0.028f, 0.04f, rubyRed, ROUGH_SKIN, METAL_SKIN)
        // Eyebrows & Salma's curved eyelashes
        drawPart(shader, vpMatrix, boxMesh, -0.135f, 1.42f, 0.29f, 0f, 0.12f, 0.032f, 0.04f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
        drawPart(shader, vpMatrix, boxMesh, 0.135f, 1.42f, 0.29f, 0f, 0.12f, 0.032f, 0.04f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
        if (isSalma) {
            drawPartRot(shader, vpMatrix, boxMesh, -0.20f, 1.36f, 0.29f, 0f, 0f, 22f, 0.07f, 0.024f, 0.03f, pupilDark, ROUGH_HAIR, METAL_HAIR)
            drawPartRot(shader, vpMatrix, boxMesh, 0.20f, 1.36f, 0.29f, 0f, 0f, -22f, 0.07f, 0.024f, 0.03f, pupilDark, ROUGH_HAIR, METAL_HAIR)
        }

        // 3. Sculpted Hairstyle & Signature Crown / Tiara / Star Cap
        when {
            isSalma -> {
                // Salma's Sculpted Hair Dome, Front Bangs, Side Locks, 3-Segment Flowing Twin Pigtails & Ribbon Bows
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.41f, -0.05f, 0f, 0.71f, 0.48f, 0.66f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, sphereMesh, -0.12f, 1.49f, 0.24f, 15f, 0.26f, 0.15f, 0.16f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, sphereMesh, 0.12f, 1.49f, 0.24f, 15f, 0.26f, 0.15f, 0.16f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, -0.31f, 1.20f, 0.16f, 180f, 0.11f, 0.24f, 0.11f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, 0.31f, 1.20f, 0.16f, 180f, 0.11f, 0.24f, 0.11f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                val tailSway = sin(animTime * 6f) * 0.05f
                for (dir in listOf(-1f, 1f)) {
                    val bx = dir * 0.36f
                    drawPartRot(shader, vpMatrix, coneMesh, bx - dir * 0.05f, 1.44f, -0.08f, 0f, 0f, dir * 70f, 0.14f, 0.18f, 0.10f, rosePink, ROUGH_CLOTH, METAL_CLOTH)
                    drawPartRot(shader, vpMatrix, coneMesh, bx + dir * 0.07f, 1.44f, -0.08f, 0f, 0f, -dir * 70f, 0.14f, 0.18f, 0.10f, rosePink, ROUGH_CLOTH, METAL_CLOTH)
                    drawPart(shader, vpMatrix, sphereMesh, bx, 1.44f, -0.06f, 0f, 0.11f, 0.11f, 0.11f, gold, ROUGH_GOLD, METAL_GOLD)
                    drawPart(shader, vpMatrix, sphereMesh, bx + dir * 0.08f, 1.32f, -0.10f, 0f, 0.20f, 0.24f, 0.20f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                    drawPart(shader, vpMatrix, sphereMesh, bx + dir * 0.13f + tailSway, 1.14f, -0.12f, 0f, 0.18f, 0.24f, 0.18f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                    drawPart(shader, vpMatrix, coneMesh, bx + dir * 0.16f + tailSway * 1.4f, 0.94f, -0.13f, 180f, 0.16f, 0.24f, 0.16f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                }
                // Salma's Royal Golden Tiara with Ruby Heart Jewel
                drawPart(shader, vpMatrix, cylMesh, 0f, 1.62f, 0.04f, 0f, 0.42f, 0.10f, 0.40f, gold, ROUGH_GOLD, METAL_GOLD)
                drawPart(shader, vpMatrix, coneMesh, 0f, 1.75f, 0.14f, 0f, 0.18f, 0.22f, 0.14f, gold, ROUGH_GOLD, METAL_GOLD)
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.68f, 0.22f, 0f, 0.10f, 0.10f, 0.07f, rubyRed, ROUGH_GEM, METAL_GEM)
            }
            isKidAhmed -> {
                // Little Ahmed's Electric-Blue & Gold Star Baseball Cap + Spiky Hair Tufts Peeking Out
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.44f, -0.03f, 0f, 0.72f, 0.44f, 0.68f, sapphire, ROUGH_CLOTH, METAL_CLOTH)
                drawPart(shader, vpMatrix, boxMesh, 0f, 1.43f, 0.33f, 12f, 0.48f, 0.06f, 0.26f, gold, ROUGH_GOLD, METAL_GOLD)
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.54f, 0.30f, 0f, 0.14f, 0.14f, 0.06f, gold, ROUGH_GOLD, METAL_GOLD)
                drawPart(shader, vpMatrix, coneMesh, -0.22f, 1.38f, 0.26f, 45f, 0.14f, 0.18f, 0.14f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, 0.22f, 1.38f, 0.26f, 45f, 0.14f, 0.18f, 0.14f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, 0f, 1.32f, -0.34f, -55f, 0.20f, 0.22f, 0.18f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
            }
            else -> {
                // Royal Hero Ahmed: Multi-Lock Spiky Anime Hair + 5-Peak Golden Olympus Crown with Sapphire Gems
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.41f, -0.05f, 0f, 0.70f, 0.45f, 0.66f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, 0f, 1.66f, -0.02f, -15f, 0.26f, 0.28f, 0.26f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, -0.20f, 1.60f, 0.04f, -10f, 0.22f, 0.25f, 0.22f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, 0.20f, 1.60f, 0.04f, -10f, 0.22f, 0.25f, 0.22f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, -0.10f, 1.53f, 0.24f, 38f, 0.16f, 0.20f, 0.16f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawPart(shader, vpMatrix, coneMesh, 0.12f, 1.53f, 0.24f, 38f, 0.16f, 0.20f, 0.16f, skin.hairOrAccentColor, ROUGH_HAIR, METAL_HAIR)
                drawCrown(shader, vpMatrix, 0f, 1.65f, 0f, gold)
            }
        }

        // Equipped Shop Hat Override / Addon
        when (hatId) {
            "hat_crown" -> drawCrown(shader, vpMatrix, 0f, 1.72f, 0f, gold)
            "hat_headband" -> drawPart(shader, vpMatrix, cylMesh, 0f, 1.44f, 0f, 0f, 0.70f, 0.10f, 0.68f, Rgba.fromHex(0xFF3B5C), ROUGH_CLOTH, METAL_CLOTH)
            "hat_halo" -> drawPart(shader, vpMatrix, cylMesh, 0f, 1.88f, 0f, 0f, 0.50f, 0.04f, 0.50f, gold, ROUGH_GOLD, METAL_GOLD)
            "hat_visor" -> drawPart(shader, vpMatrix, boxMesh, 0f, 1.32f, 0.32f, 0f, 0.44f, 0.12f, 0.10f, cyan, ROUGH_GEM, METAL_GEM)
        }

        if (showLeaderCrown) {
            drawCrown(shader, vpMatrix, 0f, 1.98f + sin(animTime * 4f) * 0.05f, 0f, gold)
        }

        // 4. Left & Right Arms
        drawLimb(shader, vpMatrix, -0.39f, 0.90f, 0f, lArmX, -armSpread, 0.18f, 0.40f, bodyCol, skin.skinColor)
        drawLimb(shader, vpMatrix, 0.39f, 0.90f, 0f, rArmX, armSpread, 0.18f, 0.40f, bodyCol, skin.skinColor)

        // 5. Left & Right Legs + Winged / Turbo Sneakers (kept crisp and unobstructed for jumping precision)
        drawLeg(shader, vpMatrix, -0.17f, 0.42f, 0f, lLegX, 0.20f, 0.36f, skin.pantsColor, shoeColor, gold, shoeRough, shoeMetal)
        drawLeg(shader, vpMatrix, 0.17f, 0.42f, 0f, rLegX, 0.20f, 0.36f, skin.pantsColor, shoeColor, gold, shoeRough, shoeMetal)
    }

    private fun drawCrown(shader: CartoonShader, vpMatrix: FloatArray, x: Float, y: Float, z: Float, col: Rgba) {
        drawPart(shader, vpMatrix, cylMesh, x, y, z, 0f, 0.44f, 0.12f, 0.44f, col, ROUGH_GOLD, METAL_GOLD)
        drawPart(shader, vpMatrix, coneMesh, x - 0.15f, y + 0.12f, z + 0.04f, 0f, 0.12f, 0.17f, 0.12f, col, ROUGH_GOLD, METAL_GOLD)
        drawPart(shader, vpMatrix, coneMesh, x, y + 0.15f, z + 0.08f, 0f, 0.15f, 0.22f, 0.15f, col, ROUGH_GOLD, METAL_GOLD)
        drawPart(shader, vpMatrix, coneMesh, x + 0.15f, y + 0.12f, z + 0.04f, 0f, 0.12f, 0.17f, 0.12f, col, ROUGH_GOLD, METAL_GOLD)
        // Front sapphire crown gem
        drawPart(shader, vpMatrix, sphereMesh, x, y + 0.02f, z + 0.22f, 0f, 0.09f, 0.09f, 0.06f, cyan, ROUGH_GEM, METAL_GEM)
    }

    private fun drawPart(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        mesh: GlMesh,
        x: Float, y: Float, z: Float,
        rotX: Float,
        sx: Float, sy: Float, sz: Float,
        color: Rgba,
        roughness: Float = 0.5f,
        metallic: Float = 0.0f
    ) {
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, x, y, z)
        if (rotX != 0f) Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.scaleM(tmp, 0, sx, sy, sz)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, color.r, color.g, color.b, color.a)
        GLES30.glUniform1f(shader.uRoughnessLoc, roughness)
        GLES30.glUniform1f(shader.uMetallicLoc, metallic)
        GLES30.glUniform1i(shader.uPatternModeLoc, 0)
        mesh.draw()
    }

    private fun drawPartRot(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        mesh: GlMesh,
        x: Float, y: Float, z: Float,
        rotX: Float, rotY: Float, rotZ: Float,
        sx: Float, sy: Float, sz: Float,
        color: Rgba,
        roughness: Float = 0.5f,
        metallic: Float = 0.0f
    ) {
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, x, y, z)
        if (rotY != 0f) Matrix.rotateM(tmp, 0, rotY, 0f, 1f, 0f)
        if (rotX != 0f) Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        if (rotZ != 0f) Matrix.rotateM(tmp, 0, rotZ, 0f, 0f, 1f)
        Matrix.scaleM(tmp, 0, sx, sy, sz)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, color.r, color.g, color.b, color.a)
        GLES30.glUniform1f(shader.uRoughnessLoc, roughness)
        GLES30.glUniform1f(shader.uMetallicLoc, metallic)
        GLES30.glUniform1i(shader.uPatternModeLoc, 0)
        mesh.draw()
    }

    private fun drawLimb(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        px: Float, py: Float, pz: Float,
        rotX: Float, rotZ: Float,
        w: Float, len: Float,
        sleeveCol: Rgba, handCol: Rgba
    ) {
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px, py, pz)
        Matrix.rotateM(tmp, 0, rotZ, 0f, 0f, 1f)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len * 0.45f, 0f)
        Matrix.scaleM(tmp, 0, w, len, w)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, sleeveCol.r, sleeveCol.g, sleeveCol.b, 1f)
        GLES30.glUniform1f(shader.uRoughnessLoc, ROUGH_CLOTH)
        GLES30.glUniform1f(shader.uMetallicLoc, METAL_CLOTH)
        GLES30.glUniform1i(shader.uPatternModeLoc, 0)
        cylMesh.draw()

        // Hand sphere
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px, py, pz)
        Matrix.rotateM(tmp, 0, rotZ, 0f, 0f, 1f)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len, 0f)
        Matrix.scaleM(tmp, 0, w * 1.08f, w * 1.08f, w * 1.08f)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, handCol.r, handCol.g, handCol.b, 1f)
        GLES30.glUniform1f(shader.uRoughnessLoc, ROUGH_SKIN)
        GLES30.glUniform1f(shader.uMetallicLoc, METAL_SKIN)
        sphereMesh.draw()
    }

    private fun drawLeg(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        px: Float, py: Float, pz: Float,
        rotX: Float,
        w: Float, len: Float,
        pantsCol: Rgba, shoeCol: Rgba, wingAccentCol: Rgba,
        shoeRough: Float, shoeMetal: Float
    ) {
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px, py, pz)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len * 0.45f, 0f)
        Matrix.scaleM(tmp, 0, w, len, w)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, pantsCol.r, pantsCol.g, pantsCol.b, 1f)
        GLES30.glUniform1f(shader.uRoughnessLoc, ROUGH_CLOTH)
        GLES30.glUniform1f(shader.uMetallicLoc, METAL_CLOTH)
        GLES30.glUniform1i(shader.uPatternModeLoc, 0)
        cylMesh.draw()

        // Stylized chunky sneaker + sole trim
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px, py, pz)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len, 0.06f)
        Matrix.scaleM(tmp, 0, w * 1.18f, w * 0.82f, w * 1.65f)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, shoeCol.r, shoeCol.g, shoeCol.b, 1f)
        GLES30.glUniform1f(shader.uRoughnessLoc, shoeRough)
        GLES30.glUniform1f(shader.uMetallicLoc, shoeMetal)
        sphereMesh.draw()

        // Golden mini winglet on outer side of shoe
        val sideSign = if (px < 0f) -1f else 1f
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px + sideSign * 0.11f, py, pz)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len + 0.05f, 0.02f)
        Matrix.scaleM(tmp, 0, 0.04f, 0.10f, 0.16f)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, wingAccentCol.r, wingAccentCol.g, wingAccentCol.b, 1f)
        GLES30.glUniform1f(shader.uRoughnessLoc, ROUGH_GOLD)
        GLES30.glUniform1f(shader.uMetallicLoc, METAL_GOLD)
        boxMesh.draw()
    }
}
