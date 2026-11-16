package com.example.game.engine3d

import android.opengl.GLES30
import android.opengl.Matrix
import com.example.game.model.HeadAccessoryStyle
import com.example.game.model.Rgba
import com.example.game.model.SkinDef
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    private val pupilDark = Rgba(0.08f, 0.08f, 0.14f, 1f)
    private val gold = Rgba(1f, 0.82f, 0.17f, 1f)
    private val cyan = Rgba(0f, 0.85f, 1f, 1f)

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
        showLeaderCrown: Boolean = false
    ) {
        val shoeColor = when (shoeId) {
            "shoe_gold" -> gold
            "shoe_rocket" -> cyan
            "shoe_shadow" -> Rgba.fromHex(0xC83BFF)
            else -> skin.shoeColor
        }

        // Smooth animation pose parameters
        var bobY = 0f
        var leanFwd = 0f
        var rollZ = 0f
        var lArmX = 0f
        var rArmX = 0f
        var lLegX = 0f
        var rLegX = 0f
        var armSpread = 16f

        when (anim) {
            CharAnimState.IDLE -> {
                bobY = sin(animTime * 3.2f) * 0.04f
                lArmX = sin(animTime * 2.5f) * 6f
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
                leanFwd = if (anim == CharAnimState.SPRINT) 19f else 11f
                lLegX = sin(animTime * freq) * amp
                rLegX = -lLegX
                lArmX = -lLegX * 0.85f
                rArmX = lLegX * 0.85f
            }
            CharAnimState.JUMP -> {
                leanFwd = -8f
                lArmX = -125f
                rArmX = -125f
                armSpread = 32f
                lLegX = 26f
                rLegX = -18f
            }
            CharAnimState.FALL -> {
                leanFwd = 10f
                lArmX = -140f + sin(animTime * 16f) * 18f
                rArmX = -140f - sin(animTime * 16f) * 18f
                armSpread = 45f
                lLegX = sin(animTime * 14f) * 20f
                rLegX = -lLegX
            }
            CharAnimState.LAND -> {
                bobY = -0.10f
                leanFwd = 14f
                lArmX = 28f
                rArmX = 28f
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

        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, x, y + bobY, z)
        Matrix.rotateM(root, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(root, 0, leanFwd, 1f, 0f, 0f)
        Matrix.rotateM(root, 0, rollZ, 0f, 0f, 1f)
        val sxz = 1f / kotlin.math.sqrt(squashY.coerceIn(0.65f, 1.35f))
        Matrix.scaleM(root, 0, sxz, squashY, sxz)

        val bodyCol = if (hitFlash) white else skin.hoodieColor

        // Torso (rounded hoodie + folded back hood + dark inner tee strip)
        drawPart(shader, vpMatrix, sphereMesh, 0f, 0.72f, 0f, 0f, 0.70f, 0.72f, 0.56f, bodyCol)
        drawPart(shader, vpMatrix, sphereMesh, 0f, 0.94f, -0.22f, -20f, 0.48f, 0.24f, 0.30f, bodyCol)
        drawPart(shader, vpMatrix, boxMesh, 0f, 0.74f, 0.26f, 0f, 0.14f, 0.44f, 0.06f, skin.pantsColor)
        // Chest crown emblem badge
        drawPart(shader, vpMatrix, boxMesh, 0f, 0.78f, 0.295f, 0f, 0.15f, 0.13f, 0.03f, skin.emblemColor)

        // Head
        drawPart(shader, vpMatrix, sphereMesh, 0f, 1.28f, 0f, 0f, 0.68f, 0.64f, 0.64f, skin.skinColor)

        // Cute expressive eyes, pupils, nose, and eyebrows
        drawPart(shader, vpMatrix, sphereMesh, -0.13f, 1.31f, 0.27f, 0f, 0.15f, 0.17f, 0.10f, white)
        drawPart(shader, vpMatrix, sphereMesh, 0.13f, 1.31f, 0.27f, 0f, 0.15f, 0.17f, 0.10f, white)
        drawPart(shader, vpMatrix, sphereMesh, -0.13f, 1.31f, 0.31f, 0f, 0.08f, 0.10f, 0.06f, pupilDark)
        drawPart(shader, vpMatrix, sphereMesh, 0.13f, 1.31f, 0.31f, 0f, 0.08f, 0.10f, 0.06f, pupilDark)
        drawPart(shader, vpMatrix, sphereMesh, 0f, 1.25f, 0.32f, 0f, 0.09f, 0.07f, 0.09f, skin.skinColor)
        // Eyebrows
        drawPart(shader, vpMatrix, boxMesh, -0.13f, 1.42f, 0.29f, 0f, 0.12f, 0.035f, 0.04f, skin.hairOrAccentColor)
        drawPart(shader, vpMatrix, boxMesh, 0.13f, 1.42f, 0.29f, 0f, 0.12f, 0.035f, 0.04f, skin.hairOrAccentColor)

        // Head Style / Skin Accessory
        when (skin.headStyle) {
            HeadAccessoryStyle.SPIKY_HAIR, HeadAccessoryStyle.FLAME_CREST -> {
                // Full textured hair cap + layered spiky tufts
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.40f, -0.06f, 0f, 0.69f, 0.44f, 0.64f, skin.hairOrAccentColor)
                drawPart(shader, vpMatrix, coneMesh, 0f, 1.67f, -0.02f, -15f, 0.28f, 0.30f, 0.28f, skin.hairOrAccentColor)
                drawPart(shader, vpMatrix, coneMesh, -0.17f, 1.61f, 0.02f, -10f, 0.22f, 0.25f, 0.22f, skin.hairOrAccentColor)
                drawPart(shader, vpMatrix, coneMesh, 0.17f, 1.61f, 0.02f, -10f, 0.22f, 0.25f, 0.22f, skin.hairOrAccentColor)
                drawPart(shader, vpMatrix, coneMesh, 0.05f, 1.56f, 0.20f, 35f, 0.18f, 0.20f, 0.18f, skin.hairOrAccentColor)
            }
            HeadAccessoryStyle.PANDA_EARS -> {
                drawPart(shader, vpMatrix, sphereMesh, -0.24f, 1.56f, 0f, 0f, 0.20f, 0.20f, 0.16f, skin.hairOrAccentColor)
                drawPart(shader, vpMatrix, sphereMesh, 0.24f, 1.56f, 0f, 0f, 0.20f, 0.20f, 0.16f, skin.hairOrAccentColor)
            }
            HeadAccessoryStyle.CROWN_HELM, HeadAccessoryStyle.ROYAL_CROWN -> {
                drawCrown(shader, vpMatrix, 0f, 1.62f, 0f, gold)
            }
            HeadAccessoryStyle.ROBOT_ANTENNA, HeadAccessoryStyle.ALIEN_ANTENNAE -> {
                drawPart(shader, vpMatrix, cylMesh, 0f, 1.66f, 0f, 0f, 0.04f, 0.24f, 0.04f, skin.hairOrAccentColor)
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.80f, 0f, 0f, 0.12f, 0.12f, 0.12f, skin.emblemColor)
            }
            HeadAccessoryStyle.LEGEND_HALO -> {
                drawCrown(shader, vpMatrix, 0f, 1.63f, 0f, gold)
                drawPart(shader, vpMatrix, cylMesh, 0f, 1.84f, 0f, 0f, 0.44f, 0.04f, 0.44f, cyan)
            }
            HeadAccessoryStyle.ASTRONAUT_HELMET, HeadAccessoryStyle.KNIGHT_VISOR -> {
                drawPart(shader, vpMatrix, boxMesh, 0f, 1.32f, 0.25f, 0f, 0.48f, 0.22f, 0.16f, skin.hairOrAccentColor)
            }
            else -> {
                // Hoodie cap / Ninja cowl dome
                drawPart(shader, vpMatrix, sphereMesh, 0f, 1.34f, -0.06f, 0f, 0.72f, 0.62f, 0.68f, skin.hoodieColor)
            }
        }

        // Equipped Shop Hat Override / Addon
        when (hatId) {
            "hat_crown" -> drawCrown(shader, vpMatrix, 0f, 1.68f, 0f, gold)
            "hat_headband" -> drawPart(shader, vpMatrix, cylMesh, 0f, 1.44f, 0f, 0f, 0.70f, 0.10f, 0.68f, Rgba.fromHex(0xFF3B5C))
            "hat_halo" -> drawPart(shader, vpMatrix, cylMesh, 0f, 1.82f, 0f, 0f, 0.50f, 0.04f, 0.50f, gold)
            "hat_visor" -> drawPart(shader, vpMatrix, boxMesh, 0f, 1.32f, 0.30f, 0f, 0.44f, 0.12f, 0.10f, cyan)
        }

        if (showLeaderCrown) {
            drawCrown(shader, vpMatrix, 0f, 1.98f + sin(animTime * 4f) * 0.05f, 0f, gold)
        }

        // Left & Right Arms
        drawLimb(shader, vpMatrix, -0.39f, 0.90f, 0f, lArmX, -armSpread, 0.18f, 0.40f, bodyCol, skin.skinColor)
        drawLimb(shader, vpMatrix, 0.39f, 0.90f, 0f, rArmX, armSpread, 0.18f, 0.40f, bodyCol, skin.skinColor)

        // Left & Right Legs + Shoes
        drawLeg(shader, vpMatrix, -0.17f, 0.42f, 0f, lLegX, 0.20f, 0.36f, skin.pantsColor, shoeColor)
        drawLeg(shader, vpMatrix, 0.17f, 0.42f, 0f, rLegX, 0.20f, 0.36f, skin.pantsColor, shoeColor)
    }

    private fun drawCrown(shader: CartoonShader, vpMatrix: FloatArray, x: Float, y: Float, z: Float, col: Rgba) {
        drawPart(shader, vpMatrix, cylMesh, x, y, z, 0f, 0.42f, 0.12f, 0.42f, col)
        drawPart(shader, vpMatrix, coneMesh, x - 0.14f, y + 0.11f, z, 0f, 0.13f, 0.16f, 0.13f, col)
        drawPart(shader, vpMatrix, coneMesh, x, y + 0.14f, z, 0f, 0.15f, 0.20f, 0.15f, col)
        drawPart(shader, vpMatrix, coneMesh, x + 0.14f, y + 0.11f, z, 0f, 0.13f, 0.16f, 0.13f, col)
    }

    private fun drawPart(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        mesh: GlMesh,
        x: Float, y: Float, z: Float,
        rotX: Float,
        sx: Float, sy: Float, sz: Float,
        color: Rgba
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
        GLES30.glUniform1i(shader.uPatternModeLoc, 0)
        cylMesh.draw()

        // Hand sphere
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px, py, pz)
        Matrix.rotateM(tmp, 0, rotZ, 0f, 0f, 1f)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len, 0f)
        Matrix.scaleM(tmp, 0, w * 1.05f, w * 1.05f, w * 1.05f)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, handCol.r, handCol.g, handCol.b, 1f)
        sphereMesh.draw()
    }

    private fun drawLeg(
        shader: CartoonShader,
        vpMatrix: FloatArray,
        px: Float, py: Float, pz: Float,
        rotX: Float,
        w: Float, len: Float,
        pantsCol: Rgba, shoeCol: Rgba
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
        GLES30.glUniform1i(shader.uPatternModeLoc, 0)
        cylMesh.draw()

        // Stylized chunky sneaker
        Matrix.setIdentityM(tmp, 0)
        Matrix.translateM(tmp, 0, px, py, pz)
        Matrix.rotateM(tmp, 0, rotX, 1f, 0f, 0f)
        Matrix.translateM(tmp, 0, 0f, -len, 0.06f)
        Matrix.scaleM(tmp, 0, w * 1.15f, w * 0.8f, w * 1.6f)
        Matrix.multiplyMM(model, 0, root, 0, tmp, 0)
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0)
        GLES30.glUniformMatrix4fv(shader.uModelLoc, 1, false, model, 0)
        GLES30.glUniformMatrix4fv(shader.uMvpLoc, 1, false, mvp, 0)
        GLES30.glUniform4f(shader.uBaseColorLoc, shoeCol.r, shoeCol.g, shoeCol.b, 1f)
        sphereMesh.draw()
    }
}
