extends Control

# 8-Second AAA Mythological Studio Intro for:
# MYTHORIA games
# By ahmed
#
# Timeline:
# 0–2s: Dark blue starry atmosphere, golden & sapphire particles appearing, subtle magical intro sound.
# 2–4s: Ancient Greek sky temple, columns, moon, waterfalls, and lightning flashes; golden 5-peak crown appears.
# 4–6s: Symmetrical golden angelic wings spread open; "MYTHORIA" metallic logo appears with golden light sweep and glowing blue sapphires.
# 6–8s: "— games —" and "By ahmed" appear on the ornamental golden plaque with side gems and fire braziers.
# 8.0s: Smooth transition into main_menu.tscn with real threaded resource loading progress.

const NEXT_SCENE := "res://scenes/main_menu.tscn"

var elapsed: float = 0.0
var intro_played: bool = false
var sweep_played: bool = false
var complete_played: bool = false
var particles: Array = []
var loader_started: bool = false

@onready var backdrop_art: TextureRect = $BackdropArt
@onready var crown_label: Label = $CenterBox/VBox/CrownLabel
@onready var title_label: Label = $CenterBox/VBox/TitleLabel
@onready var games_banner_label: Label = $CenterBox/VBox/GamesBannerLabel
@onready var sub_label: Label = $CenterBox/VBox/SubtitleLabel
@onready var loading_bar: ProgressBar = $BottomBox/LoadingBar
@onready var loading_text: Label = $BottomBox/LoadingText

func _ready() -> void:
	if ResourceLoader.exists(NEXT_SCENE):
		ResourceLoader.load_threaded_request(NEXT_SCENE)
		loader_started = true

	for i in range(56):
		particles.append({
			"pos": Vector2(randf_range(60.0, 1220.0), randf_range(40.0, 680.0)),
			"vel": Vector2(randf_range(-16.0, 16.0), randf_range(-36.0, -12.0)),
			"size": randf_range(2.0, 5.2),
			"phase": randf_range(0.0, TAU),
			"sapphire": (i % 3 == 0)
		})

func _process(delta: float) -> void:
	elapsed += delta
	if not intro_played:
		intro_played = true
		AudioSynth.play_sfx("studio_intro")

	for p in particles:
		p["pos"] += p["vel"] * delta
		if p["pos"].y < 20.0:
			p["pos"].y = 700.0

	var bg_alpha: float = clampf((elapsed - 1.5) / 2.2, 0.0, 0.72)
	var crown_alpha: float = clampf((elapsed - 2.0) / 1.5, 0.0, 1.0)
	var logo_alpha: float = clampf((elapsed - 3.8) / 1.5, 0.0, 1.0)
	var games_alpha: float = clampf((elapsed - 5.5) / 1.1, 0.0, 1.0)
	var sub_alpha: float = clampf((elapsed - 6.0) / 1.2, 0.0, 1.0)

	backdrop_art.modulate = Color(1.0, 1.0, 1.0, bg_alpha)
	crown_label.modulate = Color(1.0, 0.84, 0.18, crown_alpha)
	title_label.modulate = Color(1.0, 0.85, 0.22, logo_alpha)
	games_banner_label.modulate = Color(0.0, 0.9, 1.0, games_alpha)
	sub_label.modulate = Color(1.0, 0.95, 0.75, sub_alpha)

	# Connect loading bar to real ResourceLoader threaded progress + smooth 8s intro pacing
	var real_prog := [0.0]
	if loader_started:
		ResourceLoader.load_threaded_get_status(NEXT_SCENE, real_prog)
	var timed_pct: float = clampf((elapsed / 8.0) * 100.0, 0.0, 100.0)
	var display_pct: int = int(clampf(timed_pct * 0.75 + float(real_prog[0]) * 25.0, 0.0, 100.0))
	loading_bar.value = float(display_pct)
	loading_text.text = "Loading... %d%%  —  MYTHORIA games • By ahmed" % display_pct

	if elapsed >= 2.0 and not sweep_played:
		sweep_played = true
		AudioSynth.play_sfx("studio_sweep")

	if elapsed >= 6.0 and not complete_played:
		complete_played = true
		AudioSynth.play_sfx("studio_complete")

	queue_redraw()

	if elapsed >= 8.0:
		get_tree().change_scene_to_file(NEXT_SCENE)

func _draw() -> void:
	var vp := get_viewport_rect().size
	var cx: float = vp.x * 0.5
	var cy: float = vp.y * 0.46

	# 4-6s: Symmetrical Golden Angelic Wings + Royal Sapphire Gems
	var wing_alpha: float = clampf((elapsed - 3.6) / 1.6, 0.0, 1.0)
	if wing_alpha > 0.01:
		for dir in [-1.0, 1.0]:
			var wing_pts := PackedVector2Array([
				Vector2(cx + dir * 90.0, cy - 20.0),
				Vector2(cx + dir * 290.0 * wing_alpha, cy - 110.0),
				Vector2(cx + dir * 220.0 * wing_alpha, cy - 35.0),
				Vector2(cx + dir * 310.0 * wing_alpha, cy - 20.0),
				Vector2(cx + dir * 215.0 * wing_alpha, cy + 28.0)
			])
			draw_colored_polygon(wing_pts, Color(1.0, 0.82, 0.18, 0.45 * wing_alpha))
		# Central Blue Sapphire Diamond inside the Crown & Logo
		draw_circle(Vector2(cx, cy - 82.0), 10.0, Color(0.0, 0.88, 1.0, wing_alpha))

	# Golden & Sapphire Particles
	var p_alpha: float = clampf(elapsed / 1.4, 0.0, 0.92)
	for p in particles:
		var glow: float = 0.5 + 0.5 * sin(elapsed * 3.4 + float(p["phase"]))
		var col: Color = Color(0.0, 0.89, 1.0, p_alpha * glow) if bool(p["sapphire"]) else Color(1.0, 0.84, 0.22, p_alpha * glow)
		draw_circle(p["pos"], float(p["size"]), col)

	# 4-6s: Golden Light Sweep across the MYTHORIA Crest
	if elapsed >= 3.8 and elapsed <= 6.2:
		var sweep_t: float = clampf((elapsed - 3.8) / 2.2, 0.0, 1.0)
		var sx: float = lerpf(160.0, vp.x - 160.0, sweep_t)
		draw_circle(Vector2(sx, cy), 110.0, Color(1.0, 0.9, 0.35, 0.22))
