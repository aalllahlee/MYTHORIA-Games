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
        sfxBuffers["jump"] = synthSweep(260f, 590f, 0.14f, 0.55f)
        sfxBuffers["land"] = synthSweep(150f, 70f, 0.09f, 0.45f)
        sfxBuffers["dive"] = synthSweep(420f, 180f, 0.16f, 0.45f)
        sfxBuffers["dash"] = synthSweep(320f, 880f, 0.18f, 0.55f)
        sfxBuffers["coin"] = synthTwoTone(987.77f, 1318.51f, 0.18f)
        sfxBuffers["checkpoint"] = synthTwoTone(523.25f, 783.99f, 0.30f)
        sfxBuffers["bounce"] = synthSweep(200f, 760f, 0.22f, 0.6f)
        sfxBuffers["hit"] = synthSweep(190f, 55f, 0.22f, 0.7f)
        sfxBuffers["click"] = synthSweep(640f, 440f, 0.05f, 0.35f)
        sfxBuffers["victory"] = synthArpeggio(floatArrayOf(293.66f, 369.99f, 440.00f, 587.33f, 739.99f, 880.00f), 0.84f)
        sfxBuffers["fail"] = synthArpeggio(floatArrayOf(392.0f, 349.23f, 311.13f, 261.63f), 0.64f)
        startMusicLoopIfNeeded()
    }

    fun playSfx(name: String) {
        val vol = sfxVolume
        if (vol <= 0.01f || isPaused || isReleased) return
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
                track.setVolume(vol.coerceIn(0f, 1f))
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
                var bassPhase = 0.0

                while (!isReleased && !Thread.currentThread().isInterrupted) {
                    val theme = activeTheme
                    val vol = if (isPaused || theme == MusicTheme.NONE) 0f else musicVolume * 0.22f
                    if (vol <= 0.005f) {
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
                    val leadFreq = notes[step % notes.size]
                    val bassFreq = leadFreq * 0.5f

                    for (i in 0 until chunkSamples) {
                        val env = exp(-4.5 * (i.toDouble() / chunkSamples))
                        val lead = sin(phase) * env
                        val bass = sin(bassPhase) * 0.45
                        val sample = ((lead + bass) * vol * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
                        buf[i] = sample
                        phase += 2.0 * PI * leadFreq / sampleRate
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
