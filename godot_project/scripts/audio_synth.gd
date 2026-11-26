extends Node

## MYTHORIA games — By ahmed
## Polyphonic Procedural Background Music Sequencer & Multi-Note SFX Synthesizer
## Features:
## - Continuous upbeat Mythological Adventure BGM melody + bassline loop (`music_player`)
## - Multi-channel polyphonic SFX (`jump`, `double_jump`, `dive`, `dash`, `coin`, `checkpoint`, `fall`, `death`, `hit`, `bounce`, `victory`, `click`, `sweep`)
## - 100% offline & self-contained (zero external audio files needed)

var music_player: AudioStreamPlayer
var music_playback: AudioStreamGeneratorPlayback = null
var sfx_pool: Array[AudioStreamPlayer] = []
var sfx_idx: int = 0
var mix_rate: float = 22050.0

var bgm_phase_lead: float = 0.0
var bgm_phase_bass: float = 0.0
var bgm_time: float = 0.0
var bgm_mode: String = "menu" # "menu" or "gameplay"

# Heroic Mythoria C-Major / A-Minor Pentatonic Adventure Melody (Hz)
const MENU_MELODY := [
	523.25, 659.25, 783.99, 659.25, 880.00, 783.99, 659.25, 587.33,
	523.25, 659.25, 783.99, 1046.50, 987.77, 783.99, 659.25, 587.33
]
const GAME_MELODY := [
	659.25, 783.99, 880.00, 783.99, 1046.50, 880.00, 783.99, 659.25,
	587.33, 659.25, 783.99, 880.00, 987.77, 880.00, 783.99, 659.25
]
const BASS_NOTES := [
	261.63, 261.63, 220.00, 220.00, 174.61, 174.61, 196.00, 196.00
]

func _ready() -> void:
	# 4-voice SFX pool so coin pickups, jumps, and hits never cut each other off!
	for i in range(4):
		var sp := AudioStreamPlayer.new()
		add_child(sp)
		sfx_pool.append(sp)

	music_player = AudioStreamPlayer.new()
	var m_gen := AudioStreamGenerator.new()
	m_gen.mix_rate = mix_rate
	m_gen.buffer_length = 0.25
	music_player.stream = m_gen
	add_child(music_player)
	_ensure_music_playing()

func set_bgm_mode(mode_name: String) -> void:
	bgm_mode = mode_name
	_ensure_music_playing()

func _ensure_music_playing() -> void:
	if not is_instance_valid(music_player):
		return
	var music_ok: bool = GameState.sound_enabled and GameState.music_enabled and GameState.music_volume > 0.01
	if not music_ok:
		if music_player.playing:
			music_player.stop()
		return
	music_player.volume_db = linear_to_db(clampf(GameState.music_volume * 0.48, 0.02, 1.0))
	if not music_player.playing:
		music_player.play()
	music_playback = music_player.get_stream_playback() as AudioStreamGeneratorPlayback

func _process(_delta: float) -> void:
	var music_ok: bool = GameState.sound_enabled and GameState.music_enabled and GameState.music_volume > 0.01
	if not music_ok:
		if is_instance_valid(music_player) and music_player.playing:
			music_player.stop()
		return

	if not music_player.playing:
		_ensure_music_playing()
	if music_playback == null:
		music_playback = music_player.get_stream_playback() as AudioStreamGeneratorPlayback
		if music_playback == null:
			return

	var avail: int = music_playback.get_frames_available()
	if avail <= 0:
		return

	var step_dur: float = 0.20 if bgm_mode == "gameplay" else 0.26
	var melody: Array = GAME_MELODY if bgm_mode == "gameplay" else MENU_MELODY
	var dt: float = 1.0 / mix_rate

	for _i in range(avail):
		bgm_time += dt
		var step_idx: int = int(bgm_time / step_dur)
		var step_frac: float = fmod(bgm_time, step_dur) / step_dur

		var lead_freq: float = float(melody[step_idx % melody.size()])
		var harm_freq: float = lead_freq * 1.25
		var bass_freq: float = float(BASS_NOTES[int(step_idx / 2) % BASS_NOTES.size()])

		# Plucked harp/marimba envelope for lead + warm harmonic chord + rhythmic beat
		var lead_env: float = exp(-step_frac * 3.8) * sin(minf(1.0, step_frac * 24.0) * PI * 0.5)
		var bass_env: float = (1.0 - step_frac * 0.65) * 0.58

		bgm_phase_lead = fmod(bgm_phase_lead + lead_freq * dt, 1.0)
		bgm_phase_bass = fmod(bgm_phase_bass + bass_freq * dt, 1.0)

		var lead_sig: float = (sin(bgm_phase_lead * TAU) * 0.72 + sin(bgm_phase_lead * TAU * 2.0) * 0.28) * lead_env * 0.16
		var harm_sig: float = sin(bgm_phase_lead * 1.25 * TAU) * lead_env * 0.07
		var bass_sig: float = sin(bgm_phase_bass * TAU) * bass_env * 0.13
		var perc_sig: float = (exp(-step_frac * 22.0) * sin(step_frac * 180.0) * 0.07) if (bgm_mode == "gameplay" and step_idx % 2 == 0) else 0.0
		var sample: float = clampf(lead_sig + harm_sig + bass_sig + perc_sig, -0.88, 0.88)
		music_playback.push_frame(Vector2(sample, sample))

func play_sfx(sfx_name: String) -> void:
	if not GameState.sound_enabled or GameState.sfx_volume <= 0.01:
		return
	if sfx_pool.is_empty():
		return

	var sp: AudioStreamPlayer = sfx_pool[sfx_idx % sfx_pool.size()]
	sfx_idx += 1

	var duration: float = 0.22
	match sfx_name:
		"victory", "studio_complete":
			duration = 0.56
		"fall", "death", "fail":
			duration = 0.46
		"checkpoint", "sweep", "studio_sweep", "studio_intro":
			duration = 0.36
		"coin", "jump", "double_jump", "bounce", "dash", "dive", "hit":
			duration = 0.22
		_:
			duration = 0.10

	var gen := AudioStreamGenerator.new()
	gen.mix_rate = mix_rate
	gen.buffer_length = duration + 0.08
	sp.stream = gen
	sp.volume_db = linear_to_db(clampf(GameState.sfx_volume, 0.05, 1.0))
	sp.play()
	var pb := sp.get_stream_playback() as AudioStreamGeneratorPlayback
	if pb == null:
		return

	var frames: int = min(int(duration * mix_rate), pb.get_frames_available())
	var phase1: float = 0.0
	var phase2: float = 0.0

	for i in range(frames):
		var t: float = float(i) / float(max(frames, 1))
		var freq1: float = 520.0
		var freq2: float = 0.0
		var env: float = sin(t * PI) * 0.36
		var noise_amt: float = 0.0

		match sfx_name:
			"jump":
				freq1 = lerpf(340.0, 780.0, pow(t, 0.65))
				freq2 = freq1 * 1.5
			"double_jump", "bounce":
				freq1 = lerpf(520.0, 1120.0, pow(t, 0.55))
				freq2 = freq1 * 1.25
			"dive":
				freq1 = lerpf(680.0, 240.0, t)
				noise_amt = 0.08 * (1.0 - t)
			"dash":
				freq1 = lerpf(480.0, 1240.0, t)
				freq2 = lerpf(720.0, 1680.0, t)
				noise_amt = 0.10 * sin(t * PI)
			"coin":
				# Two-tone crisp arcade coin chime (B5 -> E6)
				freq1 = 987.77 if t < 0.38 else 1318.51
				freq2 = freq1 * 2.0
				env = (1.0 - t) * 0.42
			"checkpoint":
				# Triumphant 3-note arpeggio (D5 -> F#5 -> A5 -> D6)
				if t < 0.25:
					freq1 = 587.33
				elif t < 0.50:
					freq1 = 739.99
				elif t < 0.75:
					freq1 = 880.00
				else:
					freq1 = 1174.66
				freq2 = freq1 * 1.5
				env = (1.0 - t * 0.5) * 0.38
			"victory", "studio_complete":
				# 4-note Royal Fanfare (C5 -> E5 -> G5 -> C6)
				if t < 0.22:
					freq1 = 523.25
				elif t < 0.44:
					freq1 = 659.25
				elif t < 0.66:
					freq1 = 783.99
				else:
					freq1 = 1046.50
				freq2 = freq1 * 1.5
				env = (1.0 - t * 0.35) * 0.44
			"fall":
				# Cartoon falling whistle + low thud
				freq1 = lerpf(820.0, 150.0, pow(t, 0.85))
				freq2 = freq1 * 0.5
				env = sin(t * PI) * 0.42
			"death", "fail":
				# Dramatic descending brass/chime (G4 -> Eb4 -> C4 -> G3)
				if t < 0.28:
					freq1 = 392.00
				elif t < 0.56:
					freq1 = 311.13
				elif t < 0.80:
					freq1 = 261.63
				else:
					freq1 = 196.00
				freq2 = freq1 * 0.5
				env = (1.0 - t * 0.4) * 0.42
			"hit":
				# Obstacle knock / bumper thump
				freq1 = lerpf(280.0, 95.0, t)
				noise_amt = 0.18 * (1.0 - t)
				env = (1.0 - t) * 0.45
			"sweep", "studio_sweep", "studio_intro":
				freq1 = lerpf(220.0, 880.0, t)
				freq2 = freq1 * 1.498
			_:
				freq1 = lerpf(660.0, 880.0, t)
				env = (1.0 - t) * 0.30

		phase1 += freq1 / mix_rate
		var sig: float = sin(phase1 * TAU)
		if freq2 > 0.0:
			phase2 += freq2 / mix_rate
			sig = sig * 0.72 + sin(phase2 * TAU) * 0.28
		if noise_amt > 0.0:
			var pseudo_noise: float = sin(float(i * 127) * 0.43) * cos(float(i * 311) * 0.17)
			sig = sig * (1.0 - noise_amt) + pseudo_noise * noise_amt

		var sample: float = clampf(sig * env, -0.9, 0.9)
		pb.push_frame(Vector2(sample, sample))
