package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundEngine {

    enum class MusicTheme {
        NONE,
        MENU,
        GAMEPLAY,
        VICTORY
    }

    @Volatile var musicVolume: Float = 0.7f
    @Volatile var sfxVolume: Float = 0.85f
    @Volatile var uiVolume: Float = 0.85f
    @Volatile var isMuted: Boolean = false
    @Volatile var musicDuckFactor: Float = 1.0f // Ducked to ~0.45f on pause/overlays
    @Volatile var tensionLevel: Float = 0.0f // 0..1 dynamic danger/speed tension layer
    @Volatile private var activeTheme: MusicTheme = MusicTheme.NONE
    @Volatile private var isPaused: Boolean = false
    @Volatile private var isReleased: Boolean = false

    private val sampleRate = 22050
    private val sfxBuffers = mutableMapOf<String, ShortArray>()
    private var musicThread: Thread? = null

    fun initBuffers() {
        if (sfxBuffers.isNotEmpty()) return
        // Epic mythological orchestral fifths & octave swell for MYTHORIA games intro (0–2s)
        sfxBuffers["studio_intro"] = synthChordSweep(floatArrayOf(146.83f, 220.00f, 293.66f, 369.99f, 440.00f, 587.33f), 1.65f)
        // Magical sapphire portal shimmer & golden wing sweep with thunder undertone (2–4s)
        sfxBuffers["studio_sweep"] = synthSweep(110f, 659.25f, 0.95f, 0.36f)
        // Celestial royal fanfare chime for "By ahmed" plaque reveal (6–8s)
        sfxBuffers["studio_complete"] = synthChordSweep(floatArrayOf(293.66f, 369.99f, 440.00f, 587.33f, 880.00f), 0.85f)
        sfxBuffers["jump"] = synthSweep(260f, 590f, 0.14f, 0.52f)
        // Landing sounds graded by impact intensity & accuracy
        sfxBuffers["land_soft"] = synthSweep(140f, 82f, 0.06f, 0.30f)
        sfxBuffers["land"] = synthSweep(155f, 68f, 0.09f, 0.44f)
        sfxBuffers["land_heavy"] = synthSweep(185f, 52f, 0.13f, 0.58f)
        sfxBuffers["land_perfect"] = synthTwoTone(587.33f, 880.00f, 0.16f)
        sfxBuffers["dive"] = synthSweep(420f, 180f, 0.16f, 0.44f)
        sfxBuffers["dash"] = synthSweep(320f, 880f, 0.18f, 0.52f)
        sfxBuffers["slide"] = synthSweep(340f, 220f, 0.11f, 0.28f)
        // Surface-specific footsteps (stone, grass, wood, metal, ice, sand)
        sfxBuffers["step_stone"] = synthSweep(190f, 95f, 0.045f, 0.22f)
        sfxBuffers["step_grass"] = synthSweep(260f, 140f, 0.050f, 0.18f)
        sfxBuffers["step_wood"] = synthSweep(165f, 110f, 0.048f, 0.24f)
        sfxBuffers["step_metal"] = synthTwoTone(520f, 390f, 0.045f)
        sfxBuffers["step_ice"] = synthSweep(780f, 520f, 0.042f, 0.20f)
        sfxBuffers["step_sand"] = synthSweep(230f, 125f, 0.052f, 0.17f)
        // Hazard warning & collapsing platform crack activation cues
        sfxBuffers["hazard_warn"] = synthTwoTone(660f, 880f, 0.12f)
        sfxBuffers["platform_crack"] = synthTwoTone(240f, 155f, 0.14f)
        // Reward & chest sounds differentiated by reward type and chest tier
        sfxBuffers["coin"] = synthTwoTone(987.77f, 1318.51f, 0.18f)
        sfxBuffers["powerup_shield"] = synthArpeggio(floatArrayOf(440f, 554.37f, 659.25f, 880f), 0.34f)
        sfxBuffers["powerup_magnet"] = synthArpeggio(floatArrayOf(392f, 493.88f, 587.33f, 783.99f), 0.34f)
        sfxBuffers["powerup_wings"] = synthArpeggio(floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f), 0.36f)
        sfxBuffers["chest_wooden"] = synthArpeggio(floatArrayOf(329.63f, 392.00f, 523.25f), 0.42f)
        sfxBuffers["chest_metal"] = synthArpeggio(floatArrayOf(392.00f, 493.88f, 587.33f, 783.99f), 0.48f)
        sfxBuffers["chest_magic"] = synthArpeggio(floatArrayOf(440.00f, 554.37f, 659.25f, 880.00f, 1108.73f), 0.56f)
        sfxBuffers["chest_royal"] = synthArpeggio(floatArrayOf(293.66f, 440.00f, 587.33f, 739.99f, 880.00f, 1174.66f), 0.68f)
        sfxBuffers["checkpoint"] = synthTwoTone(523.25f, 783.99f, 0.30f)
        sfxBuffers["bounce"] = synthSweep(200f, 760f, 0.22f, 0.56f)
        sfxBuffers["hit"] = synthSweep(190f, 55f, 0.22f, 0.65f)
        sfxBuffers["click"] = synthSweep(640f, 440f, 0.05f, 0.32f)
        sfxBuffers["victory"] = synthArpeggio(floatArrayOf(293.66f, 369.99f, 440.00f, 587.33f, 739.99f, 880.00f), 0.84f)
        sfxBuffers["fail"] = synthArpeggio(floatArrayOf(392.0f, 349.23f, 311.13f, 261.63f), 0.64f)
        startMusicLoopIfNeeded()
    }

    fun playSfx(name: String) {
        if (isMuted || isPaused || isReleased) return
        val isUiSound = (name == "click")
        val channelVol = if (isUiSound) uiVolume else sfxVolume
        val vol = channelVol.coerceIn(0f, 1f)
        if (vol <= 0.01f) return
        val pcm = sfxBuffers[name] ?: return
        thread(isDaemon = true, name = "sfx-$name") {
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setVolume(vol)
                track.play()
                Thread.sleep((pcm.size * 1000L / sampleRate) + 40L)
                track.stop()
                track.release()
            } catch (_: Throwable) {
            }
        }
    }

    fun setMusicTheme(theme: MusicTheme) {
        activeTheme = theme
    }

    fun onPause() {
        isPaused = true
    }

    fun onResume() {
        isPaused = false
    }

    fun release() {
        isReleased = true
        musicThread?.interrupt()
    }

    private fun startMusicLoopIfNeeded() {
        if (musicThread != null) return
        musicThread = thread(isDaemon = true, name = "dontfall-bgm") {
            val chunkSamples = sampleRate / 4 // 250ms steps
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(chunkSamples * 2)

            var track: AudioTrack? = null
            try {
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track.play()

                val menuNotes = floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f, 392.00f, 329.63f, 293.66f, 392.00f)
                val gameNotes = floatArrayOf(261.63f, 261.63f, 392.00f, 329.63f, 349.23f, 329.63f, 293.66f, 392.00f,
                    523.25f, 440.00f, 392.00f, 329.63f, 349.23f, 392.00f, 440.00f, 392.00f)
                val victoryNotes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f, 783.99f, 1046.50f)

                val buf = ShortArray(chunkSamples)
                var step = 0
                var phase = 0.0
                var harmPhase = 0.0
                var bassPhase = 0.0
                var smoothVol = 0f

                while (!isReleased && !Thread.currentThread().isInterrupted) {
                    val theme = activeTheme
                    val duck = when {
                        theme == MusicTheme.MENU -> 0.85f
                        theme == MusicTheme.VICTORY -> 0.78f
                        else -> musicDuckFactor.coerceIn(0.35f, 1.0f)
                    }
                    val targetVol = if (isPaused || isMuted || theme == MusicTheme.NONE) 0f else (musicVolume * 0.23f * duck)
                    smoothVol += (targetVol - smoothVol) * 0.35f
                    if (smoothVol <= 0.004f) {
                        buf.fill(0)
                        track.write(buf, 0, buf.size)
                        Thread.sleep(60)
                        continue
                    }

                    val notes = when (theme) {
                        MusicTheme.MENU -> menuNotes
                        MusicTheme.GAMEPLAY -> gameNotes
                        MusicTheme.VICTORY -> victoryNotes
                        MusicTheme.NONE -> menuNotes
                    }
                    val tension = if (theme == MusicTheme.GAMEPLAY) tensionLevel.coerceIn(0f, 1f) else 0f
                    val leadFreq = notes[step % notes.size] * (1.0f + tension * 0.03f)
                    val harmFreq = leadFreq * 1.25f // Major third / fifth harmonic warmth
                    val bassFreq = leadFreq * 0.5f

                    for (i in 0 until chunkSamples) {
                        val normT = i.toDouble() / chunkSamples
                        val env = exp(-4.0 * normT)
                        val lead = (sin(phase) + 0.30 * sin(phase * 2.0)) * env
                        val harm = sin(harmPhase) * env * (0.36 + tension * 0.16)
                        val bass = sin(bassPhase) * (0.46 + tension * 0.12)
                        // Subtle rhythmic kick/snare pulse on gameplay steps, intensifying gently near danger
                        val percEnv = exp(-26.0 * normT)
                        val perc = if (theme == MusicTheme.GAMEPLAY && (step % 2 == 0 || tension > 0.45f)) {
                            sin(normT * (210.0 + tension * 55.0)) * percEnv * (0.30 + tension * 0.14)
                        } else 0.0
                        val sample = ((lead + harm + bass + perc) * smoothVol * 14500.0).toInt().coerceIn(-32767, 32767).toShort()
                        buf[i] = sample
                        phase += 2.0 * PI * leadFreq / sampleRate
                        harmPhase += 2.0 * PI * harmFreq / sampleRate
                        bassPhase += 2.0 * PI * bassFreq / sampleRate
                    }
                    track.write(buf, 0, buf.size)
                    step++
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun synthSweep(startFreq: Float, endFreq: Float, durationSec: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * t
            val env = (1f - t) * (1f - t)
            phase += 2.0 * PI * freq / sampleRate
            out[i] = (sin(phase) * env * amp * 32000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthTwoTone(f1: Float, f2: Float, durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val half = count / 2
        var phase = 0.0
        for (i in 0 until count) {
            val freq = if (i < half) f1 else f2
            val localT = (if (i < half) i else i - half).toFloat() / half.coerceAtLeast(1)
            val env = exp(-3.5 * localT).toFloat()
            phase += 2.0 * PI * freq / sampleRate
            out[i] = (sin(phase) * env * 0.55f * 32000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthArpeggio(freqs: FloatArray, durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val noteLen = (count / freqs.size).coerceAtLeast(1)
        var phase = 0.0
        for (i in 0 until count) {
            val idx = (i / noteLen).coerceAtMost(freqs.lastIndex)
            val localT = (i % noteLen).toFloat() / noteLen
            val env = exp(-2.5 * localT).toFloat()
            phase += 2.0 * PI * freqs[idx] / sampleRate
            out[i] = (sin(phase) * env * 0.52f * 32000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun synthChordSweep(freqs: FloatArray, durationSec: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val env = (sin(PI * t) * exp(-1.1 * t)).toFloat()
            var mix = 0.0
            for (k in freqs.indices) {
                val delay = k * 0.08f
                if (t >= delay) {
                    mix += sin(2.0 * PI * freqs[k] * (i.toDouble() / sampleRate))
                }
            }
            out[i] = ((mix / freqs.size) * env * 0.65f * 32000f).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
