package com.example.game.engine3d

import android.opengl.GLES30

class CartoonShader {
    val programId: Int
    val uMvpLoc: Int
    val uModelLoc: Int
    val uBaseColorLoc: Int
    val uAltColorLoc: Int
    val uSunDirLoc: Int
    val uCamPosLoc: Int
    val uFogColorLoc: Int
    val uPatternModeLoc: Int // 0=solid, 1=striped, 2=checker, 3=boost/chevron, 4=emissive/glow
    val uTimeLoc: Int

    init {
        val vs = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aUv;

            uniform mat4 uMvp;
            uniform mat4 uModel;

            out vec3 vWorldPos;
            out vec3 vNormal;
            out vec2 vUv;

            void main() {
                vec4 world = uModel * vec4(aPos, 1.0);
                vWorldPos = world.xyz;
                vNormal = normalize(mat3(uModel) * aNormal);
                vUv = aUv;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fs = """#version 300 es
            precision mediump float;

            in vec3 vWorldPos;
            in vec3 vNormal;
            in vec2 vUv;

            uniform vec4 uBaseColor;
            uniform vec4 uAltColor;
            uniform vec3 uSunDir;
            uniform vec3 uCamPos;
            uniform vec3 uFogColor;
            uniform int uPatternMode;
            uniform float uTime;

            out vec4 fragColor;

            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uSunDir);
                vec3 V = normalize(uCamPos - vWorldPos);

                vec3 albedo = uBaseColor.rgb;
                if (uPatternMode == 1) {
                    // Stylized hammer / barrier stripes
                    float s = step(0.5, fract(vUv.y * 4.0));
                    albedo = mix(uBaseColor.rgb, uAltColor.rgb, s);
                } else if (uPatternMode == 2) {
                    // Checkered finish arch / banner
                    float cx = floor(vUv.x * 8.0);
                    float cy = floor(vUv.y * 4.0);
                    float chk = mod(cx + cy, 2.0);
                    albedo = mix(uBaseColor.rgb, uAltColor.rgb, chk);
                } else if (uPatternMode == 3) {
                    // Animated speed boost / conveyor chevrons
                    float arrow = step(0.5, fract(vUv.y * 4.0 - abs(vUv.x - 0.5) * 2.0 - uTime * 2.2));
                    albedo = mix(uBaseColor.rgb, uAltColor.rgb, arrow * 0.75);
                }

                // Hemisphere ambient (bright sky top + warm bounce bottom)
                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = mix(vec3(0.28, 0.30, 0.44), vec3(0.75, 0.88, 1.0), hemi) * 0.52;

                // Soft stylized diffuse wrap lighting
                float ndl = clamp(dot(N, L) * 0.65 + 0.35, 0.0, 1.0);
                vec3 diffuse = vec3(1.0, 0.97, 0.90) * ndl * 0.68;

                // Glossy cartoon specular highlight
                vec3 H = normalize(L + V);
                float spec = pow(max(dot(N, H), 0.0), 28.0) * 0.35;

                // Crisp rim highlight for character & platform separation
                float rim = pow(1.0 - max(dot(N, V), 0.0), 3.2) * 0.25;

                vec3 lit = albedo * (ambient + diffuse) + vec3(spec + rim);
                if (uPatternMode == 4) {
                    lit = albedo * 1.18 + vec3(0.15);
                }

                // Gentle distance atmospheric haze
                float dist = length(uCamPos - vWorldPos);
                float fog = clamp((dist - 55.0) / 110.0, 0.0, 0.85);
                vec3 finalRgb = mix(lit, uFogColor, fog);

                fragColor = vec4(finalRgb, uBaseColor.a);
            }
        """.trimIndent()

        programId = ShaderUtils.linkProgram(vs, fs)
        uMvpLoc = GLES30.glGetUniformLocation(programId, "uMvp")
        uModelLoc = GLES30.glGetUniformLocation(programId, "uModel")
        uBaseColorLoc = GLES30.glGetUniformLocation(programId, "uBaseColor")
        uAltColorLoc = GLES30.glGetUniformLocation(programId, "uAltColor")
        uSunDirLoc = GLES30.glGetUniformLocation(programId, "uSunDir")
        uCamPosLoc = GLES30.glGetUniformLocation(programId, "uCamPos")
        uFogColorLoc = GLES30.glGetUniformLocation(programId, "uFogColor")
        uPatternModeLoc = GLES30.glGetUniformLocation(programId, "uPatternMode")
        uTimeLoc = GLES30.glGetUniformLocation(programId, "uTime")
    }
}

class SkyShader {
    val programId: Int
    val uTopColorLoc: Int
    val uBottomColorLoc: Int
    val uSunColorLoc: Int

    init {
        val vs = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 2) in vec2 aUv;
            out vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = vec4(aPos.xy, 0.999, 1.0);
            }
        """.trimIndent()

        val fs = """#version 300 es
            precision mediump float;
            in vec2 vUv;
            uniform vec3 uTopColor;
            uniform vec3 uBottomColor;
            uniform vec3 uSunColor;
            out vec4 fragColor;
            void main() {
                vec3 sky = mix(uBottomColor, uTopColor, smoothstep(0.05, 0.95, vUv.y));
                vec2 sunPos = vec2(0.75, 0.78);
                float d = length(vUv - sunPos);
                float glow = exp(-d * 5.2) * 0.55;
                fragColor = vec4(sky + uSunColor * glow, 1.0);
            }
        """.trimIndent()

        programId = ShaderUtils.linkProgram(vs, fs)
        uTopColorLoc = GLES30.glGetUniformLocation(programId, "uTopColor")
        uBottomColorLoc = GLES30.glGetUniformLocation(programId, "uBottomColor")
        uSunColorLoc = GLES30.glGetUniformLocation(programId, "uSunColor")
    }
}

class ShadowShader {
    val programId: Int
    val uMvpLoc: Int
    val uAlphaLoc: Int

    init {
        val vs = """#version 300 es
            layout(location = 0) in vec3 aPos;
            layout(location = 2) in vec2 aUv;
            uniform mat4 uMvp;
            out vec2 vUv;
            void main() {
                vUv = aUv;
                gl_Position = uMvp * vec4(aPos, 1.0);
            }
        """.trimIndent()

        val fs = """#version 300 es
            precision mediump float;
            in vec2 vUv;
            uniform float uAlpha;
            out vec4 fragColor;
            void main() {
                float dist = length(vUv - vec2(0.5)) * 2.0;
                float a = smoothstep(1.0, 0.15, dist) * uAlpha;
                fragColor = vec4(0.05, 0.08, 0.18, a);
            }
        """.trimIndent()

        programId = ShaderUtils.linkProgram(vs, fs)
        uMvpLoc = GLES30.glGetUniformLocation(programId, "uMvp")
        uAlphaLoc = GLES30.glGetUniformLocation(programId, "uAlpha")
    }
}

object ShaderUtils {
    fun linkProgram(vsSource: String, fsSource: String): Int {
        val vs = compile(GLES30.GL_VERTEX_SHADER, vsSource)
        val fs = compile(GLES30.GL_FRAGMENT_SHADER, fsSource)
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glLinkProgram(prog)
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return prog
    }

    private fun compile(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        return shader
    }
}
