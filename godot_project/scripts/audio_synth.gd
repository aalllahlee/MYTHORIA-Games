extends Node

var sfx_player: AudioStreamPlayer
var mix_rate: float = 22050.0

func _ready() -> void:
	sfx_player = AudioStreamPlayer.new()
	add_child(sfx_player)

func play_sfx(sfx_name: String) -> void:
	if GameState.sfx_volume <= 0.01:
		return
	var gen := AudioStreamGenerator.new()
	gen.mix_rate = mix_rate
	gen.buffer_length = 0.35
	sfx_player.stream = gen
	sfx_player.volume_db = linear_to_db(clampf(GameState.sfx_volume, 0.05, 1.0))
	sfx_player.play()
	var playback := sfx_player.get_stream_playback() as AudioStreamGeneratorPlayback
	if playback == null:
		return

	var duration: float = 0.18
	var freq_start: float = 520.0
	var freq_end: float = 780.0
	match sfx_name:
		"jump":
			duration = 0.16; freq_start = 380.0; freq_end = 760.0
		"dive":
			duration = 0.18; freq_start = 620.0; freq_end = 280.0
		"dash":
			duration = 0.20; freq_start = 540.0; freq_end = 980.0
		"coin":
			duration = 0.14; freq_start = 880.0; freq_end = 1320.0
		"victory":
			duration = 0.32; freq_start = 523.25; freq_end = 1046.5
		"fail":
			duration = 0.28; freq_start = 340.0; freq_end = 140.0
		"studio_intro":
			duration = 0.34; freq_start = 146.83; freq_end = 293.66
		"studio_sweep":
			duration = 0.32; freq_start = 110.0; freq_end = 659.25
		"studio_complete":
			duration = 0.34; freq_start = 293.66; freq_end = 880.0
		_:
			duration = 0.08; freq_start = 640.0; freq_end = 720.0

	var frames: int = min(int(duration * mix_rate), playback.get_frames_available())
	var phase: float = 0.0
	for i in range(frames):
		var t: float = float(i) / float(max(frames, 1))
		var freq: float = lerpf(freq_start, freq_end, t)
		var env: float = sin(t * PI) * 0.35
		phase += freq / mix_rate
		var sample: float = sin(phase * TAU) * env
		playback.push_frame(Vector2(sample, sample))
