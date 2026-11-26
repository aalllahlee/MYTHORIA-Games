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
    // Pattern modes:
    // 0  = Solid material with physical roughness & metallic control
    // 1  = Hazard stripes + high-contrast warning chevrons
    // 2  = Golden-framed carved stone & runes
    // 3  = Animated speed boost / conveyor chevrons
    // 4  = Magical emissive glow / animated waterfall
    // 5  = Ancient Carved Marble with cracks, golden border & glowing cyan/gold chevron crest
    // 6  = Mythological Gardens wood grain, root ridges & emerald moss runes
    // 7  = Floating Castles brushed metallic armor plates, golden frame & glowing core chevron
    // 8  = Crystal Caves faceted glowing gemstone, crystal veins & prismatic trim
    // 9  = Ancient Ruins cracked obsidian stone, pulsing magma veins & golden frame
    // 10 = Golden Bridges carved sandstone ripples & warm sun-baked trim
    // 11 = Frosted Glacier Ice & Polished Marble with subsurface crystal sheen
    // 12 = Graduated Collapsing Platform fracture cracks & hazard pulse border
    val uPatternModeLoc: Int
    val uTimeLoc: Int
    val uRoughnessLoc: Int
    val uMetallicLoc: Int

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
            uniform float uRoughness;
            uniform float uMetallic;

            out vec4 fragColor;

            float hash21(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

            float noise2D(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float a = hash21(i);
                float b = hash21(i + vec2(1.0, 0.0));
                float c = hash21(i + vec2(0.0, 1.0));
                float d = hash21(i + vec2(1.0, 1.0));
                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            // Cover-art inspired glowing chevron / rune emblem in the center of top platform surfaces
            float centerChevronEmblem(vec2 uv) {
                vec2 p = uv - vec2(0.5, 0.5);
                float chevY = p.y + abs(p.x) * 0.85;
                float band1 = smoothstep(0.09, 0.06, abs(chevY + 0.04)) * step(abs(p.x), 0.24);
                float innerFrame = (1.0 - smoothstep(0.11, 0.135, min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y))))
                                 * smoothstep(0.08, 0.105, min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y)));
                return clamp(band1 + innerFrame * 0.65, 0.0, 1.0);
            }

            void main() {
                vec3 N = normalize(vNormal);
                vec3 L = normalize(uSunDir);
                vec3 V = normalize(uCamPos - vWorldPos);
                float dist = length(uCamPos - vWorldPos);

                // Distance-based procedural detail filter to prevent shimmering/moire on far surfaces
                float detailFade = 1.0 - smoothstep(26.0, 74.0, dist);

                vec3 albedo = uBaseColor.rgb;
                float roughness = clamp(uRoughness, 0.08, 0.96);
                float metallic = clamp(uMetallic, 0.0, 1.0);
                float emissiveBoost = 0.0;

                // Multi-step 3D chamfered golden edge border detector for platforms
                float edgeDist = min(min(vUv.x, 1.0 - vUv.x), min(vUv.y, 1.0 - vUv.y));
                float borderMask = 1.0 - smoothstep(0.055, 0.085, edgeDist);
                float bevelShadow = smoothstep(0.055, 0.085, edgeDist) * (1.0 - smoothstep(0.085, 0.125, edgeDist));

                if (uPatternMode == 1) {
                    // High-contrast hazard stripes + geometric chevron ticks for colorblind clarity
                    float s = step(0.5, fract((vUv.y + abs(vUv.x - 0.5) * 0.35) * 4.0));
                    albedo = mix(uBaseColor.rgb, uAltColor.rgb, s);
                    roughness = 0.38;
                    metallic = 0.25;
                } else if (uPatternMode == 2) {
                    vec2 tileUv = fract(vUv * vec2(4.0, 4.0));
                    float seam = (1.0 - smoothstep(0.03, 0.06, min(tileUv.x, tileUv.y))) * detailFade;
                    albedo = mix(uBaseColor.rgb, uBaseColor.rgb * 0.78, seam * 0.55 + bevelShadow * 0.4);
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.92);
                    roughness = mix(0.62, 0.26, borderMask);
                    metallic = borderMask * 0.72;
                } else if (uPatternMode == 3) {
                    float arrow = step(0.42, fract(vUv.y * 4.0 - abs(vUv.x - 0.5) * 2.2 - uTime * 2.8));
                    albedo = mix(uBaseColor.rgb, uAltColor.rgb, arrow * 0.88);
                    emissiveBoost = arrow * 0.34;
                    roughness = 0.30;
                } else if (uPatternMode == 4) {
                    float flow = sin(vWorldPos.y * 3.8 + uTime * 7.8 + vWorldPos.x * 2.2) * 0.5 + 0.5;
                    albedo = mix(uBaseColor.rgb, uAltColor.rgb, flow * 0.50);
                    emissiveBoost = 0.36 + flow * 0.22;
                    roughness = 0.22;
                } else if (uPatternMode == 5) {
                    // Ancient Carved Stone / Marble with 3D tile seams, golden frame & glowing sapphire/gold chevron crest
                    float n = noise2D(vWorldPos.xz * 1.5);
                    vec2 tileUv = fract(vWorldPos.xz * 0.65);
                    float tileSeam = (1.0 - smoothstep(0.025, 0.055, min(tileUv.x, tileUv.y))) * detailFade;
                    float crack = smoothstep(0.47, 0.50, abs(n - 0.5));
                    albedo = mix(uBaseColor.rgb * (0.91 + 0.14 * n * detailFade), uBaseColor.rgb * 0.52, (1.0 - crack) * 0.50 * detailFade + tileSeam * 0.35 + bevelShadow * 0.45);
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    float pulse = 0.75 + 0.25 * sin(uTime * 3.6 + vWorldPos.z * 0.2);
                    vec3 cyanGlow = vec3(0.0, 0.88, 1.0);
                    albedo = mix(albedo, cyanGlow, emblem * 0.78 * pulse);
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.94);
                    roughness = mix(0.64, 0.24, clamp(borderMask + emblem, 0.0, 1.0));
                    metallic = borderMask * 0.75;
                    emissiveBoost = emblem * 0.36 * pulse + borderMask * 0.10;
                } else if (uPatternMode == 6) {
                    // Mythological Gardens & Wooden Chests: Rich carved wood grain, root ridges & emerald moss runes
                    float grain = sin((vWorldPos.x * 3.2 + noise2D(vWorldPos.xz * 2.4) * 2.6) * 4.0) * 0.5 + 0.5;
                    vec3 woodDark = uBaseColor.rgb * 0.68;
                    albedo = mix(woodDark, uBaseColor.rgb * 1.08, mix(0.5, grain, detailFade)) * (1.0 - bevelShadow * 0.35);
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    albedo = mix(albedo, vec3(0.18, 0.95, 0.62), emblem * 0.72);
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.86);
                    roughness = mix(0.76, 0.32, borderMask);
                    metallic = borderMask * 0.55;
                    emissiveBoost = emblem * 0.26;
                } else if (uPatternMode == 7) {
                    // Floating Castles, Thunder Arena & Metallic Chests: Brushed royal armor plates & golden frame
                    vec2 plate = fract(vUv * 3.0);
                    float groove = (1.0 - smoothstep(0.03, 0.06, min(plate.x, plate.y))) * detailFade;
                    float brush = sin(vUv.y * 44.0) * 0.042 * detailFade;
                    albedo = (uBaseColor.rgb + vec3(brush)) * (1.0 - groove * 0.38 - bevelShadow * 0.40);
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    albedo = mix(albedo, vec3(0.0, 0.90, 1.0), emblem * 0.82);
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.95);
                    roughness = 0.26;
                    metallic = 0.78;
                    emissiveBoost = emblem * 0.38;
                } else if (uPatternMode == 8) {
                    // Crystal Caves & Magic Rare Chests: Faceted glowing gemstone veins & crystalline refraction
                    float facet = noise2D(floor(vWorldPos.xz * 2.6));
                    float vein = smoothstep(0.43, 0.50, noise2D(vWorldPos.xz * 3.4 + uTime * 0.35)) * detailFade;
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    albedo = mix(uBaseColor.rgb * (0.88 + 0.28 * facet * detailFade), uAltColor.rgb, clamp(vein * 0.75 + borderMask * 0.88 + emblem * 0.65, 0.0, 1.0));
                    roughness = 0.16;
                    metallic = 0.45;
                    emissiveBoost = (vein + emblem * 0.7) * 0.34;
                } else if (uPatternMode == 9) {
                    // Ancient Ruins & Royal Boss Chests: Cracked obsidian stone with pulsing magma veins & golden border
                    float lavaNoise = noise2D(vWorldPos.xz * 1.85);
                    float magmaCrack = (1.0 - smoothstep(0.0, 0.085, abs(lavaNoise - 0.5))) * detailFade;
                    float pulse = 0.72 + 0.28 * sin(uTime * 4.2 + vWorldPos.z * 0.25);
                    vec3 magmaCol = vec3(1.0, 0.36, 0.04) * pulse;
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    albedo = mix(uBaseColor.rgb * (0.85 + 0.20 * lavaNoise * detailFade), magmaCol, clamp(magmaCrack * 0.85 + emblem * 0.65, 0.0, 1.0));
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.92);
                    roughness = mix(0.68, 0.24, borderMask);
                    metallic = borderMask * 0.82;
                    emissiveBoost = (magmaCrack + emblem * 0.6) * 0.42;
                } else if (uPatternMode == 10) {
                    // Golden Bridges & Sandstone Dunes: Warm carved sandstone ripples with golden trim
                    float ripple = sin(vWorldPos.x * 2.8 + vWorldPos.z * 1.9 + noise2D(vWorldPos.xz * 1.8) * 2.2) * 0.5 + 0.5;
                    albedo = mix(uBaseColor.rgb * 0.86, uBaseColor.rgb * 1.08, mix(0.5, ripple, detailFade)) * (1.0 - bevelShadow * 0.38);
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    albedo = mix(albedo, vec3(1.0, 0.92, 0.45), emblem * 0.70);
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.92);
                    roughness = mix(0.82, 0.25, borderMask);
                    metallic = borderMask * 0.76;
                    emissiveBoost = emblem * 0.24;
                } else if (uPatternMode == 11) {
                    // Frosted Glacier Ice & Polished Marble: Subsurface ice veins, high specular clarity
                    float frost = noise2D(vWorldPos.xz * 2.8);
                    float crack = (1.0 - smoothstep(0.0, 0.06, abs(frost - 0.5))) * detailFade;
                    float emblem = (N.y > 0.5) ? centerChevronEmblem(vUv) : 0.0;
                    albedo = mix(uBaseColor.rgb, vec3(0.88, 0.97, 1.0), crack * 0.65 + emblem * 0.60);
                    albedo = mix(albedo, uAltColor.rgb, borderMask * 0.90);
                    roughness = 0.14;
                    metallic = 0.30;
                    emissiveBoost = (crack * 0.4 + emblem * 0.6) * 0.28;
                } else if (uPatternMode == 12) {
                    // Graduated Collapsing Platform Warning: High-contrast fracture web + hazard border stripes
                    float n = noise2D(vWorldPos.xz * 3.2);
                    float fracture = 1.0 - smoothstep(0.0, 0.09, abs(n - 0.5));
                    float hazardEdge = step(0.5, fract((vUv.x + vUv.y) * 6.0)) * borderMask;
                    float pulse = 0.65 + 0.35 * sin(uTime * 18.0);
                    albedo = mix(uBaseColor.rgb * 0.72, uAltColor.rgb, clamp(fracture * 0.85 * pulse + hazardEdge * 0.95, 0.0, 1.0));
                    roughness = 0.42;
                    emissiveBoost = (fracture * 0.48 + hazardEdge * 0.40) * pulse;
                }

                // Physically-inspired roughness-to-specular conversion (avoids uniform plastic shine!)
                float smoothness = 1.0 - roughness;
                float specPower = mix(10.0, 96.0, smoothness * smoothness);
                float specStrength = mix(0.06, 0.88, smoothness) * (1.0 + metallic * 0.45);

                // Cinematic hemisphere ambient + warm golden bounce from below
                float hemi = N.y * 0.5 + 0.5;
                vec3 ambient = mix(vec3(0.26, 0.24, 0.44), vec3(0.84, 0.92, 1.0), hemi) * 0.52;

                // Soft directional sun wrap lighting
                float ndl = clamp(dot(N, L) * 0.70 + 0.30, 0.0, 1.0);
                vec3 sunTint = vec3(1.0, 0.97, 0.90);
                vec3 diffuse = sunTint * ndl * (0.76 - metallic * 0.22);

                // Blinn-Phong specular reflection tinted by albedo on metallic surfaces + Fresnel rim glow
                vec3 H = normalize(L + V);
                float specVal = pow(max(dot(N, H), 0.0), specPower) * specStrength;
                vec3 specCol = mix(vec3(1.0, 0.98, 0.94), albedo * 1.25, metallic) * specVal;

                float fresnel = pow(1.0 - max(dot(N, V), 0.0), 2.8);
                vec3 rimColor = mix(vec3(0.55, 0.88, 1.0), uAltColor.rgb, 0.48) * fresnel * (0.24 + smoothness * 0.22);

                vec3 lit = albedo * (ambient + diffuse) + specCol + rimColor + albedo * emissiveBoost;

                // Soft Reinhard-style highlight roll-off so glowing elements never burn out details
                lit = lit / (vec3(1.0) + max(lit - vec3(0.85), vec3(0.0)) * 0.35);

                // Atmospheric depth & vertical abyss fog
                float distFog = clamp((dist - 54.0) / 115.0, 0.0, 0.84);
                float heightFog = clamp((-vWorldPos.y - 3.0) / 16.0, 0.0, 0.62);
                float fog = clamp(distFog + heightFog * 0.45, 0.0, 0.86);
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
        uRoughnessLoc = GLES30.glGetUniformLocation(programId, "uRoughness")
        uMetallicLoc = GLES30.glGetUniformLocation(programId, "uMetallic")
    }
}

class SkyShader {
    val programId: Int
    val uTopColorLoc: Int
    val uBottomColorLoc: Int
    val uSunColorLoc: Int
    val uTimeLoc: Int

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
            uniform float uTime;
            out vec4 fragColor;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                return mix(
                    mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
                    mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
                    f.y
                );
            }

            float fbm(vec2 p) {
                float v = 0.0;
                v += 0.55 * noise(p);
                v += 0.30 * noise(p * 2.1 + vec2(1.7, 3.2));
                v += 0.15 * noise(p * 4.3);
                return v;
            }

            void main() {
                vec3 sky = mix(uBottomColor, uTopColor, smoothstep(0.02, 0.96, vUv.y));

                // Cinematic sun with volumetric god-rays & golden corona
                vec2 sunPos = vec2(0.75 + sin(uTime * 0.10) * 0.05, 0.78 + cos(uTime * 0.08) * 0.03);
                vec2 toSun = vUv - sunPos;
                float d = length(toSun);
                float angle = atan(toSun.y, toSun.x);
                float rays = (0.5 + 0.5 * sin(angle * 12.0 + uTime * 0.4)) * exp(-d * 3.8) * 0.18;
                float core = smoothstep(0.075, 0.018, d) * 0.75;
                float glow = exp(-d * 4.2) * 0.58;

                // Soft volumetric sky cloud layers
                vec2 cloudUv = vec2(vUv.x * 3.5 + uTime * 0.025, vUv.y * 5.0);
                float cloudNoise = fbm(cloudUv);
                float cloudBand = smoothstep(0.22, 0.52, vUv.y) * (1.0 - smoothstep(0.68, 0.92, vUv.y));
                float clouds = smoothstep(0.48, 0.74, cloudNoise) * cloudBand * 0.38;
                vec3 cloudCol = mix(vec3(0.95, 0.97, 1.0), uSunColor, 0.35);

                // Distant floating mountain / archipelago silhouette along the horizon
                float hillWave = 0.17 + 0.035 * sin(vUv.x * 9.0 + 1.2) + 0.02 * cos(vUv.x * 21.0);
                float hillMask = (1.0 - smoothstep(hillWave - 0.015, hillWave + 0.01, vUv.y)) * smoothstep(0.04, 0.14, vUv.y);
                vec3 hillCol = mix(uTopColor * 0.65, uBottomColor * 0.78, 0.5);

                // Golden-blue horizon mist
                float horizonGlow = exp(-abs(vUv.y - 0.17) * 6.0) * 0.28;

                vec3 finalSky = sky + uSunColor * (core + glow + rays + horizonGlow);
                finalSky = mix(finalSky, hillCol, hillMask * 0.45);
                finalSky = mix(finalSky, cloudCol, clouds);

                fragColor = vec4(finalSky, 1.0);
            }
        """.trimIndent()

        programId = ShaderUtils.linkProgram(vs, fs)
        uTopColorLoc = GLES30.glGetUniformLocation(programId, "uTopColor")
        uBottomColorLoc = GLES30.glGetUniformLocation(programId, "uBottomColor")
        uSunColorLoc = GLES30.glGetUniformLocation(programId, "uSunColor")
        uTimeLoc = GLES30.glGetUniformLocation(programId, "uTime")
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
                float umbra = smoothstep(0.75, 0.10, dist) * 0.65;
                float penumbra = smoothstep(1.0, 0.35, dist) * 0.45;
                float a = (umbra + penumbra) * uAlpha;
                fragColor = vec4(0.03, 0.05, 0.14, a);
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
