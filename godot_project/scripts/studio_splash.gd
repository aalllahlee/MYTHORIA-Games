extends Control

## MYTHORIA games — By ahmed
## 8-second Mythological Greek Temple & Golden Crown Studio Intro + Interactive Loading Bar

const CVB := preload("res://scripts/character_visual_builder.gd")
const TOTAL_DURATION: float = 8.0

var elapsed: float = 0.0
var chime_played: bool = false
var sweep_played: bool = false
var particle_seeds: Array[Vector3] = []

@onready var backdrop_art: TextureRect = $BackdropArt
@onready var light_sweep: ColorRect = $LightSweep
@onready var loading_frame: Panel = $LoadingBarFrame
@onready var loading_fill: Panel = $LoadingBarFrame/LoadingBarFill
@onready var loading_label: Label = $LoadingBarFrame/LoadingLabel

func _ready() -> void:
	DisplayServer.screen_set_orientation(DisplayServer.SCREEN_LANDSCAPE)
	for i in range(42):
		particle_seeds.append(Vector3(
			fmod(float(i * 73 + 19), 100.0) / 100.0,
			fmod(float(i * 41 + 7), 100.0) / 100.0,
			1.5 + float(i % 4) * 1.1
		))

	var frame_style := CVB.make_box_style(
		Color(0.04, 0.08, 0.18, 0.92),
		Color(1.0, 0.82, 0.18, 1.0),
		18,
		4,
		8
	)
	loading_frame.add_theme_stylebox_override("panel", frame_style)

	var fill_style := CVB.make_box_style(
		Color(0.0, 0.85, 1.0, 0.96),
		Color(0.65, 0.96, 1.0, 1.0),
		14,
		2,
		4
	)
	loading_fill.add_theme_stylebox_override("panel", fill_style)

func _input(event: InputEvent) -> void:
	if (event is InputEventScreenTouch and event.pressed) or (event is InputEventMouseButton and event.pressed):
		if elapsed > 1.2:
			_transition_to_main_menu()

func _process(delta: float) -> void:
	elapsed += delta
	queue_redraw()

	var reveal_alpha: float = clampf(elapsed / 1.1, 0.0, 1.0)
	if elapsed > 7.4:
		reveal_alpha = clampf((TOTAL_DURATION - elapsed) / 0.6, 0.0, 1.0)
	backdrop_art.modulate = Color(1.0, 1.0, 1.0, reveal_alpha)

	if elapsed >= 1.8 and not sweep_played:
		sweep_played = true
		AudioSynth.play_sfx("sweep")

	var sweep_t: float = clampf((elapsed - 1.8) / 2.4, 0.0, 1.0)
	light_sweep.position.x = lerpf(-280.0, 1480.0, sweep_t)
	light_sweep.modulate.a = sin(sweep_t * PI) * 0.75

	if elapsed >= 5.2 and not chime_played:
		chime_played = true
		AudioSynth.play_sfx("victory")

	var pct: int = clampi(int((elapsed / 7.3) * 100.0), 0, 100)
	var max_fill_w: float = loading_frame.size.x - 12.0
	loading_fill.size.x = maxf(18.0, max_fill_w * (float(pct) / 100.0))
	loading_label.text = "Loading... %d%%" % pct

	if elapsed >= TOTAL_DURATION:
		_transition_to_main_menu()

func _transition_to_main_menu() -> void:
	set_process(false)
	get_tree().change_scene_to_file("res://scenes/main_menu.tscn")

func _draw() -> void:
	var vp: Vector2 = get_viewport_rect().size
	for i in range(particle_seeds.size()):
		var p: Vector3 = particle_seeds[i]
		var px: float = p.x * vp.x + sin(elapsed * 1.4 + float(i)) * 22.0
		var py: float = fmod((p.y * vp.y - elapsed * 28.0) + vp.y, vp.y)
		var pulse: float = 0.35 + 0.65 * absf(sin(elapsed * 2.8 + float(i)))
		var col: Color = Color(1.0, 0.85, 0.25, pulse * 0.75) if (i % 3 != 0) else Color(0.0, 0.88, 1.0, pulse * 0.75)
		draw_circle(Vector2(px, py), p.z, col)
