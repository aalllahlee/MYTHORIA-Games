package com.example.game.engine3d

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GlMesh(
    val vaoId: Int,
    val vboId: Int,
    val vertexCount: Int
) {
    fun draw(mode: Int = GLES30.GL_TRIANGLES) {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(mode, 0, vertexCount)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
    }
}

object MeshBuilder {

    // Interleaved layout: pos(3), normal(3), uv(2) => stride 8 floats (32 bytes)
    private fun uploadMesh(data: FloatArray): GlMesh {
        val vao = IntArray(1)
        val vbo = IntArray(1)
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)

        val fb: FloatBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
        fb.position(0)

        GLES30.glBindVertexArray(vao[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, fb, GLES30.GL_STATIC_DRAW)

        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)
        return GlMesh(vao[0], vbo[0], data.size / 8)
    }

    fun createBox(): GlMesh {
        val v = ArrayList<Float>(36 * 8)
        fun quad(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            fun add(x: Float, y: Float, z: Float, u: Float, w: Float) {
                v.add(x); v.add(y); v.add(z)
                v.add(nx); v.add(ny); v.add(nz)
                v.add(u); v.add(w)
            }
            add(x0, y0, z0, 0f, 0f)
            add(x1, y1, z1, 1f, 0f)
            add(x2, y2, z2, 1f, 1f)
            add(x0, y0, z0, 0f, 0f)
            add(x2, y2, z2, 1f, 1f)
            add(x3, y3, z3, 0f, 1f)
        }
        val h = 0.5f
        // Front (+Z)
        quad(-h, -h, h, h, -h, h, h, h, h, -h, h, h, 0f, 0f, 1f)
        // Back (-Z)
        quad(h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0f, 0f, -1f)
        // Top (+Y)
        quad(-h, h, h, h, h, h, h, h, -h, -h, h, -h, 0f, 1f, 0f)
        // Bottom (-Y)
        quad(-h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0f, -1f, 0f)
        // Right (+X)
        quad(h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1f, 0f, 0f)
        // Left (-X)
        quad(-h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1f, 0f, 0f)
        return uploadMesh(v.toFloatArray())
    }

    fun createSphere(stacks: Int = 12, slices: Int = 18): GlMesh {
        val v = ArrayList<Float>(stacks * slices * 6 * 8)
        for (i in 0 until stacks) {
            val phi0 = PI * (-0.5 + i.toDouble() / stacks)
            val phi1 = PI * (-0.5 + (i + 1).toDouble() / stacks)
            val y0 = sin(phi0).toFloat() * 0.5f
            val r0 = cos(phi0).toFloat() * 0.5f
            val y1 = sin(phi1).toFloat() * 0.5f
            val r1 = cos(phi1).toFloat() * 0.5f
            val v0 = i.toFloat() / stacks
            val v1 = (i + 1).toFloat() / stacks

            for (j in 0 until slices) {
                val th0 = 2.0 * PI * j.toDouble() / slices
                val th1 = 2.0 * PI * (j + 1).toDouble() / slices
                val c0 = cos(th0).toFloat()
                val s0 = sin(th0).toFloat()
                val c1 = cos(th1).toFloat()
                val s1 = sin(th1).toFloat()
                val u0 = j.toFloat() / slices
                val u1 = (j + 1).toFloat() / slices

                fun addP(px: Float, py: Float, pz: Float, u: Float, w: Float) {
                    val len = kotlin.math.sqrt(px * px + py * py + pz * pz).coerceAtLeast(1e-5f)
                    v.add(px); v.add(py); v.add(pz)
                    v.add(px / len); v.add(py / len); v.add(pz / len)
                    v.add(u); v.add(w)
                }

                addP(r0 * c0, y0, r0 * s0, u0, v0)
                addP(r0 * c1, y0, r0 * s1, u1, v0)
                addP(r1 * c1, y1, r1 * s1, u1, v1)

                addP(r0 * c0, y0, r0 * s0, u0, v0)
                addP(r1 * c1, y1, r1 * s1, u1, v1)
                addP(r1 * c0, y1, r1 * s0, u0, v1)
            }
        }
        return uploadMesh(v.toFloatArray())
    }

    fun createCylinder(segments: Int = 18): GlMesh {
        val v = ArrayList<Float>(segments * 12 * 8)
        val r = 0.5f
        val h = 0.5f
        for (i in 0 until segments) {
            val a0 = 2.0 * PI * i / segments
            val a1 = 2.0 * PI * (i + 1) / segments
            val c0 = cos(a0).toFloat()
            val s0 = sin(a0).toFloat()
            val c1 = cos(a1).toFloat()
            val s1 = sin(a1).toFloat()
            val u0 = i.toFloat() / segments
            val u1 = (i + 1).toFloat() / segments

            fun add(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, w: Float) {
                v.add(x); v.add(y); v.add(z)
                v.add(nx); v.add(ny); v.add(nz)
                v.add(u); v.add(w)
            }
            // Side
            add(r * c0, -h, r * s0, c0, 0f, s0, u0, 0f)
            add(r * c1, -h, r * s1, c1, 0f, s1, u1, 0f)
            add(r * c1, h, r * s1, c1, 0f, s1, u1, 1f)

            add(r * c0, -h, r * s0, c0, 0f, s0, u0, 0f)
            add(r * c1, h, r * s1, c1, 0f, s1, u1, 1f)
            add(r * c0, h, r * s0, c0, 0f, s0, u0, 1f)

            // Top cap
            add(0f, h, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            add(r * c0, h, r * s0, 0f, 1f, 0f, u0, 1f)
            add(r * c1, h, r * s1, 0f, 1f, 0f, u1, 1f)

            // Bottom cap
            add(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            add(r * c1, -h, r * s1, 0f, -1f, 0f, u1, 0f)
            add(r * c0, -h, r * s0, 0f, -1f, 0f, u0, 0f)
        }
        return uploadMesh(v.toFloatArray())
    }

    fun createCone(segments: Int = 14): GlMesh {
        val v = ArrayList<Float>(segments * 6 * 8)
        val r = 0.5f
        val h = 0.5f
        for (i in 0 until segments) {
            val a0 = 2.0 * PI * i / segments
            val a1 = 2.0 * PI * (i + 1) / segments
            val c0 = cos(a0).toFloat()
            val s0 = sin(a0).toFloat()
            val c1 = cos(a1).toFloat()
            val s1 = sin(a1).toFloat()
            val nx = cos((a0 + a1) * 0.5).toFloat()
            val nz = sin((a0 + a1) * 0.5).toFloat()

            fun add(x: Float, y: Float, z: Float, nxv: Float, nyv: Float, nzv: Float, u: Float, w: Float) {
                v.add(x); v.add(y); v.add(z)
                v.add(nxv); v.add(nyv); v.add(nzv)
                v.add(u); v.add(w)
            }
            add(0f, h, 0f, nx, 0.5f, nz, 0.5f, 1f)
            add(r * c0, -h, r * s0, c0, 0.35f, s0, 0f, 0f)
            add(r * c1, -h, r * s1, c1, 0.35f, s1, 1f, 0f)

            add(0f, -h, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            add(r * c1, -h, r * s1, 0f, -1f, 0f, 1f, 0f)
            add(r * c0, -h, r * s0, 0f, -1f, 0f, 0f, 0f)
        }
        return uploadMesh(v.toFloatArray())
    }

    fun createQuad(): GlMesh {
        val data = floatArrayOf(
            -1f, -1f, 0f, 0f, 0f, 1f, 0f, 0f,
            1f, -1f, 0f, 0f, 0f, 1f, 1f, 0f,
            1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f,
            -1f, -1f, 0f, 0f, 0f, 1f, 0f, 0f,
            1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f,
            -1f, 1f, 0f, 0f, 0f, 1f, 0f, 1f
        )
        return uploadMesh(data)
    }

    fun createShadowDisc(segments: Int = 20): GlMesh {
        val v = ArrayList<Float>(segments * 3 * 8)
        for (i in 0 until segments) {
            val a0 = 2.0 * PI * i / segments
            val a1 = 2.0 * PI * (i + 1) / segments
            v.add(0f); v.add(0f); v.add(0f); v.add(0f); v.add(1f); v.add(0f); v.add(0.5f); v.add(0.5f)
            v.add(0.5f * cos(a0).toFloat()); v.add(0f); v.add(0.5f * sin(a0).toFloat())
            v.add(0f); v.add(1f); v.add(0f); v.add(0.5f + 0.5f * cos(a0).toFloat()); v.add(0.5f + 0.5f * sin(a0).toFloat())
            v.add(0.5f * cos(a1).toFloat()); v.add(0f); v.add(0.5f * sin(a1).toFloat())
            v.add(0f); v.add(1f); v.add(0f); v.add(0.5f + 0.5f * cos(a1).toFloat()); v.add(0.5f + 0.5f * sin(a1).toFloat())
        }
        return uploadMesh(v.toFloatArray())
    }
}
