extends Node3D

## MYTHORIA games — By ahmed
## DON'T FALL 3D Gameplay Scene:
## - Strictly 3 Playable Heroes (Ahmed, Salma, Little Ahmed)
## - Varied 3D Track Shapes per World & Stage:
##   * Gentle Curve ("gentle_curve")
##   * Winding S-Curve ("s_curve")
##   * Split Twin-Bridge Paths ("twin_paths")
##   * Ascending Sky Stairway ("ascending")
##   * Zig-Zag Stepping Islands ("zigzag")
##   * Grand Coliseum Multi-Path Mix ("coliseum_mix")
## - Distinct 3D World Architectures per World (Greek Temples, Lightning Obelisks,
##   Golden Bridge Towers, Mystic Portal Rings, Crystal Spires, Castle Fortresses)
## - Sculpted Multi-Part 3D Obstacles (Rotating War-Hammers, Crescent Battle-Axe Pendulums,
##   Armored Shield Push-Blocks, Spiked Magma Meteor Boulders, Spring Bounce Pads)
## - 3D Footstep/Dash Particle Trail & Dynamic Speed FOV Camera!

const CVB := preload("res://scripts/character_visual_builder.gd")
const CheckpointScene := preload("res://scenes/checkpoint_3d.tscn")
const PlayerScript := preload("res://scripts/player_controller.gd")

@onready var world_env: WorldEnvironment = $WorldEnvironment
@onready var player: CharacterBody3D = $Player
@onready var camera_rig: Node3D = $CameraRig
@onready var camera_3d: Camera3D = $CameraRig/Camera3D
@onready var track_root: Node3D = $TrackRoot
@onready var top_bar_panel: Panel = $HUD/TopBarPanel
@onready var menu_btn: Button = $HUD/TopBar/MenuButton
@onready var hud_level_label: Label = $HUD/TopBar/LevelLabel
@onready var hud_timer_label: Label = $HUD/TopBar/TimerLabel
@onready var hud_coins_label: Label = $HUD/TopBar/CoinsLabel
@onready var race_progress_frame: Panel = $HUD/RaceProgressFrame
@onready var race_progress_fill: Panel = $HUD/RaceProgressFrame/RaceProgressFill
@onready var race_progress_label: Label = $HUD/RaceProgressFrame/RaceProgressLabel
@onready var hud_goal_banner: Label = $HUD/GoalBanner
@onready var joystick_base: Panel = $HUD/JoystickBase
@onready var joystick_knob: Panel = $HUD/JoystickBase/JoystickKnob
@onready var jump_btn: Button = $HUD/TouchControls/JumpButton
@onready var dive_btn: Button = $HUD/TouchControls/DiveButton
@onready var dash_btn: Button = $HUD/TouchControls/DashButton
@onready var result_panel: Panel = $HUD/ResultPanel
@onready var result_title: Label = $HUD/ResultPanel/VBox/ResultTitle
@onready var result_details: Label = $HUD/ResultPanel/VBox/ResultDetails
@onready var retry_btn: Button = $HUD/ResultPanel/VBox/ButtonsRow/RetryButton
@onready var next_btn: Button = $HUD/ResultPanel/VBox/ButtonsRow/NextButton
@onready var menu_result_btn: Button = $HUD/ResultPanel/VBox/ButtonsRow/MenuResultButton

var level_number: int = 1
var world_id: int = 1
var stage_profile: Dictionary = {}
var track_shape_mode: String = "gentle_curve"
var finish_z: float = -75.0
var finish_pos: Vector3 = Vector3(0.0, 0.0, -75.0)
var void_floor_y: float = -8.5
var elapsed_sec: float = 0.0
var coins_collected: int = 0
var run_active: bool = true
var survival_tier: int = 1
var speed_multiplier: float = 1.0
var total_checkpoints: int = 4
var checkpoint_banner_timer: float = 0.0
var checkpoint_banner_text: String = ""

var hammers: Array = []
var pendulums: Array = []
var moving_platforms: Array = []
var falling_tiles: Array = []
var push_blocks: Array = []
var rolling_boulders: Array = []
var bounce_pads: Array = []
var speed_pads: Array = []
var checkpoints: Array = []
var rivals: Array = []
var coin_nodes: Array = []
var fx_bursts: Array = []
var sky_crystals: Array = []
var track_waypoints: Array[Vector3] = []

var joy_touch_id: int = -1
var cam_touch_id: int = -1
var joy_center: Vector2 = Vector2(133.0, 587.0)
var last_jump_ms: int = 0
var last_dive_ms: int = 0
var last_dash_ms: int = 0
var last_hit_sfx_ms: int = 0
var last_speed_sfx_ms: int = 0
var trail_spawn_timer: float = 0.0
var speed_fov_boost: float = 0.0
var can_advance_next: bool = false

func _ready() -> void:
	DisplayServer.screen_set_orientation(DisplayServer.SCREEN_LANDSCAPE)
	AudioSynth.set_bgm_mode("gameplay")
	level_number = GameState.current_level_number
	world_id = int((level_number - 1) / 5) + 1
	stage_profile = GameState.get_stage_difficulty_profile(level_number)
	track_shape_mode = str(stage_profile.get("track_shape", "gentle_curve"))
	result_panel.visible = false
	_apply_world_atmosphere()
	_apply_hud_styles()
	_connect_action_buttons()
	player.player_id = "player_0"
	player.is_ai_racer = false
	player.character_skin_id = clampi(GameState.selected_skin_id, 0, 2)
	player._ensure_character_visual()
	player.initialize_spawn(Vector3(0.0, 1.8, 1.5), 180.0)
	player.checkpoint_reached.connect(_on_racer_checkpoint_reached)
	player.individual_respawn_started.connect(_on_racer_respawn_started)
	player.player_jumped.connect(_on_player_jumped)
	build_3d_course()
	spawn_ai_rivals()

	var tier_txt: String = str(stage_profile["tier_name_ar"]) if GameState.is_arabic() else str(stage_profile["tier_name"])
	var obs_txt: String = str(stage_profile["new_obstacle_ar"]) if GameState.is_arabic() else str(stage_profile["new_obstacle"])
	checkpoint_banner_timer = 3.2
	checkpoint_banner_text = GameState.tr_text(
		"✨ STAGE %d [%s] — %s" % [level_number, tier_txt, obs_txt],
		"✨ المرحلة %d [%s] — %s" % [level_number, tier_txt, obs_txt]
	)

func _apply_world_atmosphere() -> void:
	if not is_instance_valid(world_env) or world_env.environment == null:
		return
	var wt: Dictionary = GameState.get_world_theme(world_id)
	var env: Environment = world_env.environment
	var sky_top: Color = wt["sky_top"]
	var sky_bot: Color = wt["sky_bottom"]
	# Linear tonemapping + glow_enabled = false guarantees zero white-screen bloom across 50+ stages!
	env.tonemap_mode = Environment.TONEMAP_LINEAR
	env.tonemap_exposure = 1.0
	env.glow_enabled = false
	env.fog_enabled = true
	env.fog_light_color = sky_bot.lerp(Color.WHITE, 0.22)
	env.fog_density = 0.0045
	if env.sky != null and env.sky.sky_material is ProceduralSkyMaterial:
		var psm: ProceduralSkyMaterial = env.sky.sky_material
		psm.sky_top_color = sky_top
		psm.sky_horizon_color = sky_bot
		psm.ground_horizon_color = sky_bot
		psm.ground_bottom_color = sky_top.darkened(0.25)

func _connect_action_buttons() -> void:
	jump_btn.action_mode = BaseButton.ACTION_MODE_BUTTON_PRESS
	dive_btn.action_mode = BaseButton.ACTION_MODE_BUTTON_PRESS
	dash_btn.action_mode = BaseButton.ACTION_MODE_BUTTON_PRESS
	if not jump_btn.button_down.is_connected(_on_jump_button_pressed):
		jump_btn.button_down.connect(_on_jump_button_pressed)
	if not dive_btn.button_down.is_connected(_on_dive_button_pressed):
		dive_btn.button_down.connect(_on_dive_button_pressed)
	if not dash_btn.button_down.is_connected(_on_dash_button_pressed):
		dash_btn.button_down.connect(_on_dash_button_pressed)
	if not menu_btn.pressed.is_connected(_on_menu_button_pressed):
		menu_btn.pressed.connect(_on_menu_button_pressed)
	if not retry_btn.pressed.is_connected(_on_retry_button_pressed):
		retry_btn.pressed.connect(_on_retry_button_pressed)
	if not next_btn.pressed.is_connected(_on_next_button_pressed):
		next_btn.pressed.connect(_on_next_button_pressed)
	if not menu_result_btn.pressed.is_connected(_on_menu_button_pressed):
		menu_result_btn.pressed.connect(_on_menu_button_pressed)

func _apply_hud_styles() -> void:
	top_bar_panel.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.05, 0.11, 0.28, 0.88), Color(1.0, 0.82, 0.18), 16, 3, 6)
	)
	race_progress_frame.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.04, 0.09, 0.22, 0.90), Color(1.0, 0.82, 0.18), 13, 2, 4)
	)
	race_progress_fill.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.18, 0.92, 0.45, 0.95), Color(0.65, 1.0, 0.78), 10, 1, 2)
	)
	joystick_base.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.08, 0.20, 0.48, 0.58), Color(1.0, 0.84, 0.22, 0.92), 85, 4, 6)
	)
	joystick_knob.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.18, 0.62, 1.0, 0.96), Color(1.0, 0.92, 0.48, 1.0), 38, 3, 4)
	)
	result_panel.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.04, 0.09, 0.25, 0.96), Color(1.0, 0.82, 0.18), 22, 4, 12)
	)

	menu_btn.text = GameState.tr_text("⏸ MENU", "⏸ القائمة")
	jump_btn.text = GameState.tr_text("🏃\nJUMP", "🏃\nقفز")
	dive_btn.text = GameState.tr_text("🤸\nDIVE", "🤸\nغوص")
	dash_btn.text = GameState.tr_text("⚡\nDASH", "⚡\nاندفاع")
	if is_instance_valid(race_progress_label):
		race_progress_label.text = GameState.tr_text("👑 START ─────────── 🏁 FINISH", "👑 البداية ─────────── 🏁 النهاية")
	retry_btn.text = GameState.tr_text("🔄 RETRY", "🔄 إعادة")
	next_btn.text = GameState.tr_text("▶ NEXT STAGE", "▶ المرحلة التالية")
	menu_result_btn.text = GameState.tr_text("🏠 MENU", "🏠 القائمة")

	CVB.style_button(menu_btn, Color(0.12, 0.28, 0.65, 0.95), Color(0.55, 0.85, 1.0), Color.WHITE, 16, 14, 3)
	CVB.style_button(jump_btn, Color(0.14, 0.72, 0.36, 0.95), Color(0.68, 1.0, 0.78), Color.WHITE, 18, 52, 4)
	CVB.style_button(dive_btn, Color(0.72, 0.18, 0.58, 0.95), Color(1.0, 0.72, 0.94), Color.WHITE, 18, 52, 4)
	CVB.style_button(dash_btn, Color(0.08, 0.48, 0.88, 0.95), Color(1.0, 0.84, 0.22), Color(1.0, 0.94, 0.42), 18, 52, 4)

	CVB.style_button(retry_btn, Color(0.14, 0.34, 0.72, 0.96), Color(0.55, 0.85, 1.0), Color.WHITE, 18, 16, 3)
	CVB.style_button(next_btn, Color(1.0, 0.68, 0.04, 0.98), Color(1.0, 0.94, 0.52), Color.WHITE, 18, 16, 3)
	CVB.style_button(menu_result_btn, Color(0.22, 0.28, 0.48, 0.96), Color(0.65, 0.78, 0.96), Color.WHITE, 18, 16, 3)

	if not GameState.dash_unlocked:
		dash_btn.text = GameState.tr_text("🔒\nDASH", "🔒\nاندفاع")
		dash_btn.modulate = Color(0.75, 0.80, 0.90, 0.75)

func _play_hit_sfx() -> void:
	var now: int = Time.get_ticks_msec()
	if now - last_hit_sfx_ms > 360:
		last_hit_sfx_ms = now
		AudioSynth.play_sfx("hit")

func _compute_segment_offset(seg: int) -> Vector2:
	# Returns Vector2(seg_x, seg_y) so each world/stage has a unique 3D track shape!
	var sf: float = float(seg + 1)
	if track_shape_mode == "gentle_curve":
		return Vector2(sin(sf * 0.75) * 1.4, 0.0)
	elif track_shape_mode == "s_curve":
		return Vector2(sin(sf * 0.92) * 3.5, sin(sf * 0.6) * 0.35)
	elif track_shape_mode == "twin_paths":
		return Vector2(sin(sf * 0.65) * 1.6, 0.0)
	elif track_shape_mode == "ascending":
		return Vector2(sin(sf * 0.80) * 2.1, float(seg + 1) * 0.55)
	elif track_shape_mode == "zigzag":
		var dir_x: float = -1.0 if (seg % 2 == 0) else 1.0
		return Vector2(dir_x * 2.55, float(seg % 3) * 0.30)
	else: # coliseum_mix
		return Vector2(sin(sf * 1.05) * 3.2, float(seg % 4) * 0.42)

func build_3d_course() -> void:
	var w_theme: Dictionary = GameState.get_world_theme(world_id)
	var prim_col: Color = w_theme["primary"]
	var sec_col: Color = w_theme["secondary"]
	var acc_col: Color = w_theme["accent"]
	var arch_style: String = str(w_theme.get("arch_style", "greek_temples"))

	var plat_w: float = float(stage_profile["platform_width"])
	var gap_len: float = float(stage_profile["gap_len"])
	var obs_spd: float = float(stage_profile["obstacle_speed"])
	var seg_count: int = int(stage_profile["seg_count"])
	var allow_moving: bool = bool(stage_profile["allow_moving"])
	var allow_pendulum: bool = bool(stage_profile["allow_pendulum"])
	var allow_falling: bool = bool(stage_profile["allow_falling"])
	var allow_push: bool = bool(stage_profile["allow_push_block"])
	var allow_boulder: bool = bool(stage_profile["allow_boulder"])
	var allow_bounce: bool = bool(stage_profile["allow_bounce"])
	var hammer_mod: int = int(stage_profile["hammer_count_mod"])

	track_waypoints.clear()
	track_waypoints.append(Vector3(0.0, 0.0, 0.0))

	# Start Sanctuary Platform
	spawn_platform(Vector3(0.0, 0.0, 0.0), Vector3(maxf(plat_w, 8.8), 1.4, 8.4), prim_col, acc_col, false)
	spawn_brazier_pillar(Vector3(-3.8, 0.7, -2.6))
	spawn_brazier_pillar(Vector3(3.8, 0.7, -2.6))

	var cur_z: float = -4.2 - gap_len
	var cp_spawned: int = 0

	var block_palette := [
		prim_col,
		sec_col,
		Color(1.0, 0.76, 0.12),
		Color(0.92, 0.20, 0.58),
		Color(0.14, 0.82, 0.52)
	]

	for seg in range(seg_count):
		var col: Color = block_palette[seg % block_palette.size()]
		var seg_len: float = 9.2 if level_number == 1 else 8.8
		var center_z: float = cur_z - seg_len * 0.5
		var xy_off: Vector2 = _compute_segment_offset(seg)
		var seg_x: float = xy_off.x
		var seg_y: float = xy_off.y
		var seg_center := Vector3(seg_x, seg_y, center_z)
		track_waypoints.append(seg_center)

		var is_split_segment: bool = (track_shape_mode == "twin_paths" or (track_shape_mode == "coliseum_mix" and seg % 3 == 1)) and (seg % 2 == 1)

		if is_split_segment:
			# SPLIT TWIN-BRIDGE PATHS! Two parallel bridges Left & Right with a choice of route!
			var branch_w: float = maxf(3.8, plat_w * 0.54)
			var left_pos := Vector3(seg_x - 3.0, seg_y, center_z)
			var right_pos := Vector3(seg_x + 3.0, seg_y, center_z)
			spawn_platform(left_pos, Vector3(branch_w, 1.2, seg_len), col, acc_col, false)
			spawn_platform(right_pos, Vector3(branch_w, 1.2, seg_len), sec_col, acc_col, false)
			spawn_coin(left_pos + Vector3(0.0, 1.55, -1.5))
			spawn_coin(left_pos + Vector3(0.0, 1.55, 1.5))
			spawn_coin(right_pos + Vector3(0.0, 1.55, -1.5))
			spawn_coin(right_pos + Vector3(0.0, 1.55, 1.5))
			spawn_speed_pad(left_pos + Vector3(0.0, 0.68, 0.0))
			spawn_rotating_hammer(right_pos + Vector3(0.0, 0.65, 0.0), acc_col, 1.35 * obs_spd)
		elif allow_falling and (seg % 4 == 2):
			spawn_falling_tile_bridge(seg_center, plat_w, seg_len, acc_col)
		elif allow_moving and (seg % 3 == 1):
			var move_amp: float = clampf(1.6 + float(level_number) * 0.04, 1.6, 3.0)
			spawn_moving_platform(seg_center, Vector3(maxf(5.6, plat_w - 0.8), 1.2, seg_len), col, acc_col, move_amp, (0.95 + float(seg) * 0.06) * obs_spd)
		else:
			spawn_platform(seg_center, Vector3(plat_w, 1.2, seg_len), col, acc_col, false)
			if seg % 2 == 0:
				spawn_temple_side_pillars(seg_center + Vector3(0.0, 0.6, 0.0), plat_w, acc_col)

		if not is_split_segment:
			# Coin Trails following the curved/zigzag segment center
			spawn_coin(seg_center + Vector3(0.0, 1.55, 0.0))
			spawn_coin(seg_center + Vector3(-1.6, 1.55, -1.8))
			spawn_coin(seg_center + Vector3(1.6, 1.55, 1.8))

			# Progressive Sculpted 3D Obstacles:
			if level_number == 1:
				if seg == 3:
					spawn_rotating_hammer(seg_center + Vector3(0.0, 0.65, 0.0), acc_col, 0.92)
				else:
					spawn_speed_pad(seg_center + Vector3(0.0, 0.68, 0.0))
			else:
				if allow_boulder and (seg == seg_count - 1 or seg % 5 == 4):
					spawn_rolling_boulder(seg_center + Vector3(-1.4 if (seg % 2 == 0) else 1.4, 1.75, -seg_len * 0.42), center_z + seg_len * 0.48, center_z - seg_len * 0.45, 5.0 * obs_spd, seg_x, seg_y + 1.75)
				elif allow_push and (seg % 4 == 3):
					spawn_push_block(seg_center + Vector3(0.0, 1.35, 0.0), plat_w * 0.40, 1.62 * obs_spd, acc_col)
				elif allow_pendulum and (seg % 3 == 1):
					spawn_swinging_pendulum(seg_center + Vector3(0.0, 5.2, 0.0), acc_col, 1.72 * obs_spd)
				elif seg % hammer_mod == 0:
					spawn_rotating_hammer(seg_center + Vector3(0.0, 0.65, 0.0), acc_col, 1.50 * obs_spd)
				else:
					if allow_bounce or track_shape_mode == "ascending":
						spawn_bounce_pad(seg_center + Vector3(-1.6 if (seg % 2 == 0) else 1.6, 0.68, 1.4))
					spawn_speed_pad(seg_center + Vector3(0.0, 0.68, -1.4))

		# Distinct 3D World Scenery Architecture flanking the track!
		if seg % 2 == 0:
			var side_dir: float = -1.0 if (seg % 4 == 0) else 1.0
			spawn_world_architecture(Vector3(seg_x + side_dir * 13.5, seg_y + 0.5, center_z), prim_col, sec_col, acc_col, arch_style)
			spawn_sky_cloud_and_crystal(Vector3(seg_x - side_dir * 11.5, seg_y + 4.8, center_z - 2.0), acc_col)

		cur_z -= (seg_len + gap_len)

		var target_cp: int = int(((seg + 1) * 4) / seg_count)
		var prev_cp: int = int((seg * 4) / seg_count)
		if target_cp > prev_cp and cp_spawned < 4:
			cp_spawned += 1
			var cp_platform_pos := Vector3(seg_x * 0.5, seg_y, cur_z - 3.4)
			track_waypoints.append(cp_platform_pos)
			spawn_platform(cp_platform_pos, Vector3(maxf(plat_w, 8.2), 1.35, 6.8), acc_col, Color.WHITE, false)
			spawn_checkpoint_gate(cp_platform_pos + Vector3(0.0, 0.68, 0.0), cp_spawned)
			spawn_coin(cp_platform_pos + Vector3(-2.1, 1.55, 0.0))
			spawn_coin(cp_platform_pos + Vector3(2.1, 1.55, 0.0))
			cur_z -= (6.8 + gap_len)

	total_checkpoints = max(1, cp_spawned)
	finish_z = cur_z - 4.6
	var last_wp: Vector3 = track_waypoints[track_waypoints.size() - 1]
	finish_pos = Vector3(last_wp.x * 0.35, last_wp.y, finish_z)
	track_waypoints.append(finish_pos)
	spawn_platform(finish_pos, Vector3(10.8, 1.5, 9.4), acc_col, acc_col, true)
	spawn_finish_arch(finish_pos + Vector3(0.0, 0.75, 0.0), acc_col)

	var goal_sec: int = int(GameState.goal_time_ms_for_level(level_number) / 1000)
	var tier_txt: String = str(stage_profile["tier_name_ar"]) if GameState.is_arabic() else str(stage_profile["tier_name"])
	var obs_txt: String = str(stage_profile["new_obstacle_ar"]) if GameState.is_arabic() else str(stage_profile["new_obstacle"])
	hud_level_label.text = "👑 W%d S%d [%s] | CP 0/%d" % [world_id, level_number, tier_txt, total_checkpoints]
	hud_goal_banner.text = GameState.tr_text("GOAL <= %ds • %s" % [goal_sec, obs_txt], "الهدف <= %d ث • %s" % [goal_sec, obs_txt])

func spawn_world_architecture(pos: Vector3, prim_col: Color, sec_col: Color, acc_col: Color, arch_style: String) -> void:
	# Each World Theme spawns its own signature 3D Monumental Scenery!
	if arch_style in ["thunder_obelisks", "castle_fortress"]:
		# Giant Floating Obelisk Fortress with Rotating Energy Rings!
		var fort := Node3D.new()
		fort.position = pos
		track_root.add_child(fort)
		CVB._add_part(fort, CVB._make_cyl(4.0, 2.0, 2.8, CVB.make_mat(Color(0.28, 0.32, 0.44), 0.55, 0.15)), Vector3(0.0, -1.0, 0.0))
		CVB._add_part(fort, CVB._make_cyl(4.2, 4.0, 0.5, CVB.make_mat(prim_col, 0.35, 0.25)), Vector3(0.0, 0.65, 0.0))
		# Twin Obelisk Towers
		for side in [-1.6, 1.6]:
			CVB._add_part(fort, CVB._make_box(Vector3(1.1, 3.8, 1.1), CVB.make_mat(sec_col.darkened(0.2), 0.30, 0.35)), Vector3(side, 2.6, 0.0))
			CVB._add_part(fort, CVB._make_cyl(0.04, 0.85, 1.4, CVB.make_mat(acc_col, 0.18, 0.85, acc_col, 0.45)), Vector3(side, 5.2, 0.0))
		var energy_ring := CVB._add_part(fort, CVB._make_cyl(2.2, 2.2, 0.16, CVB.make_mat(acc_col, 0.15, 0.50, acc_col, 0.50)), Vector3(0.0, 3.4, 0.0), Vector3(90.0, 0.0, 0.0))
		sky_crystals.append(energy_ring)
	elif arch_style in ["portal_rings", "crystal_spires"]:
		# Giant Crystalline Sanctuary Island with Floating Portal Halo & Crystal Clusters!
		var cry_island := Node3D.new()
		cry_island.position = pos
		track_root.add_child(cry_island)
		CVB._add_part(cry_island, CVB._make_cyl(3.8, 1.4, 3.0, CVB.make_mat(prim_col.darkened(0.35), 0.40, 0.25)), Vector3(0.0, -1.1, 0.0))
		CVB._add_part(cry_island, CVB._make_cyl(4.0, 3.8, 0.5, CVB.make_mat(sec_col, 0.25, 0.35)), Vector3(0.0, 0.65, 0.0))
		var mat_cryst := CVB.make_mat(acc_col, 0.14, 0.45, acc_col, 0.52)
		var mat_cryst2 := CVB.make_mat(sec_col.lightened(0.25), 0.14, 0.45, sec_col, 0.48)
		CVB._add_part(cry_island, CVB._make_cyl(0.05, 1.1, 4.2, mat_cryst), Vector3(0.0, 2.8, 0.0))
		CVB._add_part(cry_island, CVB._make_cyl(0.04, 0.75, 2.8, mat_cryst2), Vector3(-1.4, 2.0, 0.6), Vector3(0.0, 0.0, 22.0))
		CVB._add_part(cry_island, CVB._make_cyl(0.04, 0.75, 2.8, mat_cryst2), Vector3(1.4, 2.0, -0.4), Vector3(0.0, 0.0, -22.0))
	elif arch_style in ["golden_towers", "olympus_pantheon"]:
		# Grand Golden Pantheon Dome & Twin Waterfall Sanctuary!
		var pan := Node3D.new()
		pan.position = pos
		track_root.add_child(pan)
		var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.18, 0.86, Color(1.0, 0.75, 0.12), 0.35)
		var mat_marble := CVB.make_mat(Color(0.96, 0.97, 1.0), 0.28, 0.10)
		CVB._add_part(pan, CVB._make_cyl(4.4, 2.0, 3.0, CVB.make_mat(Color(0.52, 0.40, 0.32), 0.60, 0.05)), Vector3(0.0, -1.1, 0.0))
		CVB._add_part(pan, CVB._make_cyl(4.6, 4.4, 0.55, mat_gold), Vector3(0.0, 0.65, 0.0))
		for c_i in range(4):
			var ang: float = float(c_i) * PI * 0.5 + PI * 0.25
			CVB._add_part(pan, CVB._make_cyl(0.28, 0.32, 2.8, mat_marble), Vector3(cos(ang) * 2.1, 2.3, sin(ang) * 2.1))
		CVB._add_part(pan, CVB._make_cyl(2.5, 2.7, 0.45, mat_marble), Vector3(0.0, 3.8, 0.0))
		CVB._add_part(pan, CVB._make_sphere(2.1, 2.4, mat_gold), Vector3(0.0, 4.5, 0.0))
	else:
		spawn_scenic_floating_island(pos, prim_col, acc_col)

func spawn_temple_side_pillars(center_pos: Vector3, plat_w: float, acc_col: Color) -> void:
	var mat_marble := CVB.make_mat(Color(0.95, 0.96, 1.0), 0.32, 0.10)
	var mat_gold := CVB.make_mat(acc_col, 0.22, 0.78)
	for side_dir in [-1, 1]:
		var px: float = float(side_dir) * (plat_w * 0.5 - 0.32)
		var col := MeshInstance3D.new()
		var cm := CylinderMesh.new()
		cm.top_radius = 0.22
		cm.bottom_radius = 0.26
		cm.height = 1.65
		cm.material = mat_marble
		col.mesh = cm
		col.position = center_pos + Vector3(px, 0.82, 0.0)
		track_root.add_child(col)

		var cap := MeshInstance3D.new()
		var sp := SphereMesh.new()
		sp.radius = 0.24
		sp.height = 0.48
		sp.material = mat_gold
		cap.mesh = sp
		cap.position = center_pos + Vector3(px, 1.75, 0.0)
		track_root.add_child(cap)

func spawn_sky_cloud_and_crystal(pos: Vector3, acc_col: Color) -> void:
	var cloud_mat := CVB.make_mat(Color(0.98, 0.99, 1.0), 0.65, 0.0)
	var c_node := Node3D.new()
	c_node.position = pos
	track_root.add_child(c_node)
	for offset in [Vector3.ZERO, Vector3(1.1, -0.18, 0.1), Vector3(-1.05, -0.20, -0.1)]:
		var puff := MeshInstance3D.new()
		var sm := SphereMesh.new()
		sm.radius = 0.95 if offset == Vector3.ZERO else 0.72
		sm.height = 1.25 if offset == Vector3.ZERO else 0.95
		sm.material = cloud_mat
		puff.mesh = sm
		puff.position = offset
		c_node.add_child(puff)

	var crystal := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = 0.03
	cm.bottom_radius = 0.55
	cm.height = 1.35
	cm.material = CVB.make_mat(acc_col, 0.16, 0.55, acc_col, 0.45)
	crystal.mesh = cm
	crystal.position = pos + Vector3(0.0, -1.9, 1.5)
	track_root.add_child(crystal)
	sky_crystals.append(crystal)

func spawn_fx_burst(pos: Vector3, col: Color, max_life: float = 0.42, start_r: float = 0.35) -> void:
	var ring := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = start_r
	cyl.bottom_radius = start_r * 1.45
	cyl.height = 0.12
	cyl.material = CVB.make_mat(col, 0.15, 0.20, col, 0.55)
	ring.mesh = cyl
	ring.position = pos
	track_root.add_child(ring)
	fx_bursts.append({"node": ring, "life": max_life, "max_life": max_life})

func spawn_brazier_pillar(pos: Vector3) -> void:
	var pillar := MeshInstance3D.new()
	var pm := BoxMesh.new()
	pm.size = Vector3(0.85, 1.1, 0.85)
	pm.material = CVB.make_mat(Color(0.95, 0.96, 1.0), 0.35, 0.08)
	pillar.mesh = pm
	pillar.position = pos
	track_root.add_child(pillar)

	var flame := MeshInstance3D.new()
	var fm := SphereMesh.new()
	fm.radius = 0.34
	fm.height = 0.68
	fm.material = CVB.make_mat(Color(1.0, 0.62, 0.08), 0.15, 0.10, Color(1.0, 0.58, 0.08), 0.55)
	flame.mesh = fm
	flame.position = pos + Vector3(0.0, 0.82, 0.0)
	track_root.add_child(flame)

func spawn_scenic_floating_island(pos: Vector3, prim_col: Color, acc_col: Color) -> void:
	var island := Node3D.new()
	island.position = pos
	track_root.add_child(island)

	var rock := MeshInstance3D.new()
	var rm := CylinderMesh.new()
	rm.top_radius = 4.2
	rm.bottom_radius = 1.8
	rm.height = 3.2
	rm.material = CVB.make_mat(Color(0.55, 0.43, 0.34), 0.65, 0.04)
	rock.mesh = rm
	rock.position = Vector3(0.0, -1.2, 0.0)
	island.add_child(rock)

	var top_grass := MeshInstance3D.new()
	var gm := CylinderMesh.new()
	gm.top_radius = 4.4
	gm.bottom_radius = 4.2
	gm.height = 0.55
	gm.material = CVB.make_mat(prim_col.lightened(0.12), 0.45, 0.05)
	top_grass.mesh = gm
	top_grass.position = Vector3(0.0, 0.6, 0.0)
	island.add_child(top_grass)

	var waterfall := MeshInstance3D.new()
	var wm := BoxMesh.new()
	wm.size = Vector3(1.1, 3.6, 0.24)
	wm.material = CVB.make_mat(Color(0.0, 0.88, 1.0), 0.16, 0.25, Color(0.0, 0.75, 1.0), 0.45)
	waterfall.mesh = wm
	waterfall.position = Vector3(-2.2 if pos.x > 0.0 else 2.2, -0.8, 1.6)
	island.add_child(waterfall)

	var tower := MeshInstance3D.new()
	var tm := CylinderMesh.new()
	tm.top_radius = 1.2
	tm.bottom_radius = 1.35
	tm.height = 2.8
	tm.material = CVB.make_mat(Color(0.95, 0.96, 1.0), 0.35, 0.08)
	tower.mesh = tm
	tower.position = Vector3(0.0, 2.2, 0.0)
	island.add_child(tower)

	var roof := MeshInstance3D.new()
	var rfm := CylinderMesh.new()
	rfm.top_radius = 0.05
	rfm.bottom_radius = 1.65
	rfm.height = 1.6
	rfm.material = CVB.make_mat(acc_col, 0.25, 0.78)
	roof.mesh = rfm
	roof.position = Vector3(0.0, 4.3, 0.0)
	island.add_child(roof)

func spawn_checkpoint_gate(pos: Vector3, cp_index: int) -> void:
	var cp_node: Area3D = CheckpointScene.instantiate()
	cp_node.position = pos
	cp_node.set("checkpoint_index", cp_index)
	cp_node.set("safe_yaw_deg", 180.0)
	track_root.add_child(cp_node)
	checkpoints.append(cp_node)

func spawn_ai_rivals() -> void:
	var offsets := [-1.8, 1.8]
	var base_scale: float = clampf(0.76 + float(level_number - 1) * 0.014, 0.76, 1.04)
	var speeds := [6.0 * base_scale, 5.65 * base_scale]
	# Spawn the other 2 exclusive heroes as your friendly rivals (so Ahmed, Salma & Little Ahmed race together!)
	var player_skin: int = clampi(GameState.selected_skin_id, 0, 2)
	var rival_skins := [(player_skin + 1) % 3, (player_skin + 2) % 3]
	for i in range(2):
		var rival := CharacterBody3D.new()
		rival.set_script(PlayerScript)
		rival.name = "Rival_%d" % (i + 1)
		rival.player_id = "rival_%d" % (i + 1)
		rival.is_ai_racer = true
		rival.character_skin_id = rival_skins[i]
		rival.move_speed = speeds[i]
		rival.lane_bias_x = offsets[i] * 0.55

		var col := CollisionShape3D.new()
		var cap := CapsuleShape3D.new()
		cap.radius = 0.36
		cap.height = 1.3
		col.shape = cap
		col.position = Vector3(0.0, 0.65, 0.0)
		rival.add_child(col)

		var start_pos := Vector3(offsets[i], 1.8, 1.5)
		add_child(rival)
		rival.initialize_spawn(start_pos, 180.0)
		rivals.append(rival)

func spawn_platform(pos: Vector3, sz: Vector3, col: Color, acc_col: Color, is_finish: bool = false) -> void:
	var body := StaticBody3D.new()
	body.position = pos
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = sz
	shape.shape = box
	body.add_child(shape)

	var mesh_inst := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = sz
	mesh.material = CVB.make_mat(col.darkened(0.18), 0.42, 0.12)
	mesh_inst.mesh = mesh
	body.add_child(mesh_inst)

	var top_trim := MeshInstance3D.new()
	var top_mesh := BoxMesh.new()
	top_mesh.size = Vector3(sz.x - 0.28, 0.18, sz.z - 0.28)
	top_mesh.material = CVB.make_textured_platform_mat(col, acc_col, is_finish)
	top_trim.mesh = top_mesh
	top_trim.position = Vector3(0.0, sz.y * 0.5 + 0.04, 0.0)
	body.add_child(top_trim)

	var gold_rail_mat := CVB.make_mat(Color(1.0, 0.84, 0.20), 0.20, 0.82)
	for side_dir in [-1, 1]:
		var rail := MeshInstance3D.new()
		var r_box := BoxMesh.new()
		r_box.size = Vector3(0.18, 0.24, sz.z - 0.16)
		r_box.material = gold_rail_mat
		rail.mesh = r_box
		rail.position = Vector3(float(side_dir) * (sz.x * 0.5 - 0.11), sz.y * 0.5 + 0.09, 0.0)
		body.add_child(rail)

	track_root.add_child(body)

func spawn_moving_platform(pos: Vector3, sz: Vector3, col: Color, acc_col: Color, amp_x: float, spd: float) -> void:
	var body := AnimatableBody3D.new()
	body.position = pos
	var shape := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = sz
	shape.shape = box
	body.add_child(shape)

	var mesh_inst := MeshInstance3D.new()
	var mesh := BoxMesh.new()
	mesh.size = sz
	mesh.material = CVB.make_mat(col.darkened(0.16), 0.36, 0.18)
	mesh_inst.mesh = mesh
	body.add_child(mesh_inst)

	var top_trim := MeshInstance3D.new()
	var top_mesh := BoxMesh.new()
	top_mesh.size = Vector3(sz.x - 0.28, 0.18, sz.z - 0.28)
	top_mesh.material = CVB.make_textured_platform_mat(col.lightened(0.12), acc_col, false)
	top_trim.mesh = top_mesh
	top_trim.position = Vector3(0.0, sz.y * 0.5 + 0.04, 0.0)
	body.add_child(top_trim)

	track_root.add_child(body)
	moving_platforms.append({"node": body, "base_x": pos.x, "amp_x": amp_x, "speed": spd})

func spawn_falling_tile_bridge(center_pos: Vector3, total_w: float, total_len: float, acc_col: Color) -> void:
	var cols: int = 3
	var rows: int = 3
	var tile_w: float = (total_w - 0.4) / float(cols)
	var tile_l: float = (total_len - 0.4) / float(rows)
	var gold_frame_mat := CVB.make_mat(Color(1.0, 0.84, 0.20), 0.20, 0.82)
	for r in range(rows):
		for c in range(cols):
			var tx: float = center_pos.x + (float(c) - 1.0) * (tile_w + 0.12)
			var tz: float = center_pos.z + (float(r) - 1.0) * (tile_l + 0.12)
			var body := StaticBody3D.new()
			body.position = Vector3(tx, center_pos.y, tz)

			var col_shape := CollisionShape3D.new()
			var b_shape := BoxShape3D.new()
			b_shape.size = Vector3(tile_w, 0.85, tile_l)
			col_shape.shape = b_shape
			body.add_child(col_shape)

			var mat := CVB.make_mat(Color(1.0, 0.72, 0.12), 0.25, 0.45, acc_col, 0.25)
			var mi := MeshInstance3D.new()
			var bm := BoxMesh.new()
			bm.size = Vector3(tile_w - 0.10, 0.85, tile_l - 0.10)
			bm.material = mat
			mi.mesh = bm
			body.add_child(mi)

			CVB._add_part(body, CVB._make_box(Vector3(tile_w, 0.14, tile_l), gold_frame_mat), Vector3(0.0, 0.38, 0.0))

			track_root.add_child(body)
			falling_tiles.append({
				"body": body,
				"col": col_shape,
				"mesh": mi,
				"mat": mat,
				"base_pos": body.position,
				"half_x": tile_w * 0.52,
				"half_z": tile_l * 0.52,
				"state": 0,
				"timer": 0.0
			})

func spawn_push_block(center_pos: Vector3, amp_x: float, spd: float, acc_col: Color) -> void:
	# UPGRADED 3D OBSTACLE: Mythoria Armored Guardian Shield Ram with Golden Pillars & Glowing Core!
	var block := Node3D.new()
	block.position = center_pos
	var mat_armor := CVB.make_mat(Color(0.92, 0.18, 0.24), 0.22, 0.38, acc_col, 0.28)
	var mat_gold := CVB.make_mat(Color(1.0, 0.84, 0.18), 0.18, 0.88)
	var mat_glow := CVB.make_mat(Color(0.0, 0.90, 1.0), 0.14, 0.35, Color(0.0, 0.80, 1.0), 0.52)

	CVB._add_part(block, CVB._make_box(Vector3(2.2, 1.30, 1.50), mat_armor), Vector3.ZERO)
	CVB._add_part(block, CVB._make_box(Vector3(2.32, 0.18, 1.62), mat_gold), Vector3(0.0, 0.62, 0.0))
	CVB._add_part(block, CVB._make_box(Vector3(2.32, 0.18, 1.62), mat_gold), Vector3(0.0, -0.62, 0.0))
	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		CVB._add_part(block, CVB._make_cyl(0.24, 0.24, 1.42, mat_gold), Vector3(sf * 1.12, 0.0, 0.0))
		CVB._add_part(block, CVB._make_sphere(0.26, 0.48, mat_glow), Vector3(0.0, 0.0, sf * 0.78))

	track_root.add_child(block)
	push_blocks.append({"node": block, "base_x": center_pos.x, "amp_x": amp_x, "speed": spd, "phase": center_pos.z * 0.15})

func spawn_rolling_boulder(start_pos: Vector3, max_z: float, min_z: float, spd: float, track_x: float = 0.0, track_y: float = 1.75) -> void:
	# UPGRADED 3D OBSTACLE: Spiked Magma Meteor Sphere with Intersecting Golden Bands & Crystal Spikes!
	var boulder := Node3D.new()
	boulder.position = Vector3(track_x + start_pos.x, track_y, start_pos.z)
	var mat_rock := CVB.make_mat(Color(0.32, 0.24, 0.28), 0.44, 0.25)
	var mat_lava := CVB.make_mat(Color(1.0, 0.46, 0.08), 0.18, 0.40, Color(1.0, 0.38, 0.06), 0.52)
	var mat_gold := CVB.make_mat(Color(1.0, 0.84, 0.18), 0.18, 0.85)

	CVB._add_part(boulder, CVB._make_sphere(1.12, 2.24, mat_rock), Vector3.ZERO)
	CVB._add_part(boulder, CVB._make_cyl(1.16, 1.16, 0.22, mat_gold), Vector3.ZERO)
	CVB._add_part(boulder, CVB._make_cyl(1.16, 1.16, 0.22, mat_lava), Vector3.ZERO, Vector3(90.0, 0.0, 0.0))
	for s_i in range(6):
		var ang: float = float(s_i) * TAU / 6.0
		CVB._add_part(boulder, CVB._make_sphere(0.24, 0.48, mat_lava), Vector3(cos(ang) * 1.05, sin(ang) * 1.05, 0.0))

	track_root.add_child(boulder)
	rolling_boulders.append({
		"node": boulder,
		"min_z": min_z,
		"max_z": max_z,
		"speed": spd,
		"track_x": track_x,
		"offset_x": start_pos.x
	})

func spawn_rotating_hammer(pos: Vector3, col: Color, spd: float) -> void:
	# UPGRADED 3D OBSTACLE: Ornate Greek Gear Hub + Crystal Crossbeam + Giant Studded War-Mallets!
	var pivot := Node3D.new()
	pivot.position = pos

	var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.18, 0.88)
	var mat_sapphire := CVB.make_mat(Color(0.0, 0.88, 1.0), 0.14, 0.35, Color(0.0, 0.78, 1.0), 0.52)
	var mat_red := CVB.make_mat(Color(0.94, 0.18, 0.24), 0.24, 0.20)
	var mat_white := CVB.make_mat(Color(0.98, 0.99, 1.0), 0.22, 0.08)

	CVB._add_part(pivot, CVB._make_cyl(0.62, 0.74, 0.32, mat_gold), Vector3(0.0, 0.16, 0.0))
	CVB._add_part(pivot, CVB._make_cyl(0.44, 0.54, 1.35, CVB.make_mat(col, 0.25, 0.55)), Vector3(0.0, 0.65, 0.0))
	CVB._add_part(pivot, CVB._make_sphere(0.46, 0.82, mat_sapphire), Vector3(0.0, 1.38, 0.0))
	CVB._add_part(pivot, CVB._make_box(Vector3(5.6, 0.30, 0.30), mat_gold), Vector3(0.0, 0.65, 0.0))

	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		CVB._add_part(pivot, CVB._make_cyl(0.50, 0.50, 1.30, mat_red), Vector3(sf * 2.65, 0.65, 0.0), Vector3(90.0, 0.0, 0.0))
		CVB._add_part(pivot, CVB._make_cyl(0.53, 0.53, 0.36, mat_white), Vector3(sf * 2.65, 0.65, 0.0), Vector3(90.0, 0.0, 0.0))
		CVB._add_part(pivot, CVB._make_cyl(0.55, 0.55, 0.14, mat_gold), Vector3(sf * 2.65, 0.65, 0.52), Vector3(90.0, 0.0, 0.0))
		CVB._add_part(pivot, CVB._make_cyl(0.55, 0.55, 0.14, mat_gold), Vector3(sf * 2.65, 0.65, -0.52), Vector3(90.0, 0.0, 0.0))

	track_root.add_child(pivot)
	hammers.append({"node": pivot, "speed": spd})

func spawn_swinging_pendulum(pos: Vector3, _col: Color, spd: float) -> void:
	# UPGRADED 3D OBSTACLE: Twin Temple Archway + Crescent Double-Bladed Battle-Axe Pendulum!
	var frame := Node3D.new()
	frame.position = pos
	track_root.add_child(frame)

	var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.18, 0.88)
	var mat_marble := CVB.make_mat(Color(0.95, 0.96, 1.0), 0.30, 0.10)
	var mat_blade := CVB.make_mat(Color(0.0, 0.88, 1.0), 0.16, 0.48, Color(0.0, 0.78, 1.0), 0.50)
	var mat_ruby := CVB.make_mat(Color(0.96, 0.18, 0.32), 0.18, 0.35, Color(0.95, 0.15, 0.28), 0.45)

	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		CVB._add_part(frame, CVB._make_cyl(0.24, 0.30, 4.8, mat_marble), Vector3(sf * 3.25, -2.2, 0.0))
		CVB._add_part(frame, CVB._make_box(Vector3(0.75, 0.35, 0.75), mat_gold), Vector3(sf * 3.25, 0.15, 0.0))

	CVB._add_part(frame, CVB._make_box(Vector3(7.1, 0.42, 0.52), mat_gold), Vector3(0.0, 0.38, 0.0))
	CVB._add_part(frame, CVB._make_sphere(0.38, 0.72, mat_ruby), Vector3(0.0, 0.38, 0.0))

	var swing_pivot := Node3D.new()
	frame.add_child(swing_pivot)

	CVB._add_part(swing_pivot, CVB._make_cyl(0.11, 0.11, 3.6, mat_gold), Vector3(0.0, -1.8, 0.0))

	var hammer_head := Node3D.new()
	hammer_head.position = Vector3(0.0, -3.6, 0.0)
	swing_pivot.add_child(hammer_head)

	# Central Golden Hub + Twin Glowing Crescent Battle-Axe Blades!
	CVB._add_part(hammer_head, CVB._make_cyl(0.46, 0.46, 1.25, mat_gold), Vector3.ZERO, Vector3(90.0, 0.0, 0.0))
	CVB._add_part(hammer_head, CVB._make_box(Vector3(1.85, 0.95, 0.22), mat_blade), Vector3(0.0, 0.0, 0.0))
	CVB._add_part(hammer_head, CVB._make_sphere(0.35, 0.68, mat_ruby), Vector3(0.0, 0.0, 0.65))
	CVB._add_part(hammer_head, CVB._make_sphere(0.35, 0.68, mat_ruby), Vector3(0.0, 0.0, -0.65))

	pendulums.append({"pivot": swing_pivot, "head": hammer_head, "speed": spd, "phase": pos.z * 0.2})

func spawn_bounce_pad(pos: Vector3) -> void:
	var pad := Node3D.new()
	pad.position = pos
	var mat_gold := CVB.make_mat(Color(1.0, 0.84, 0.18), 0.20, 0.85)
	var mat_pink := CVB.make_mat(Color(1.0, 0.22, 0.58), 0.18, 0.35, Color(1.0, 0.18, 0.55), 0.52)
	var mat_white := CVB.make_mat(Color(0.98, 0.99, 1.0), 0.18, 0.10)
	CVB._add_part(pad, CVB._make_cyl(1.12, 1.22, 0.14, mat_gold), Vector3(0.0, -0.04, 0.0))
	CVB._add_part(pad, CVB._make_cyl(0.94, 1.02, 0.22, mat_pink), Vector3(0.0, 0.06, 0.0))
	CVB._add_part(pad, CVB._make_cyl(0.52, 0.56, 0.26, mat_white), Vector3(0.0, 0.08, 0.0))
	track_root.add_child(pad)
	bounce_pads.append(pad)

func spawn_speed_pad(pos: Vector3) -> void:
	var pad := Node3D.new()
	pad.position = pos
	var mat_base := CVB.make_mat(Color(0.06, 0.22, 0.52), 0.25, 0.40)
	var mat_cyan := CVB.make_mat(Color(0.0, 0.92, 1.0), 0.15, 0.40, Color(0.0, 0.85, 1.0), 0.55)
	var mat_gold := CVB.make_mat(Color(1.0, 0.86, 0.22), 0.18, 0.80, Color(1.0, 0.80, 0.15), 0.45)
	CVB._add_part(pad, CVB._make_box(Vector3(2.4, 0.10, 2.8), mat_base), Vector3.ZERO)
	for ch_i in range(-1, 2):
		var cz: float = float(ch_i) * 0.78
		CVB._add_part(pad, CVB._make_box(Vector3(1.8, 0.14, 0.34), mat_cyan if ch_i != 0 else mat_gold), Vector3(0.0, 0.03, cz))
	track_root.add_child(pad)
	speed_pads.append(pad)

func spawn_coin(pos: Vector3) -> void:
	var c := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.44
	cyl.bottom_radius = 0.44
	cyl.height = 0.16
	var mat := CVB.make_mat(Color(1.0, 0.84, 0.16), 0.18, 0.88, Color(1.0, 0.76, 0.10), 0.35)
	cyl.material = mat
	c.mesh = cyl
	c.position = pos
	c.rotation.x = deg_to_rad(90.0)
	track_root.add_child(c)
	coin_nodes.append(c)

func spawn_finish_arch(pos: Vector3, col: Color) -> void:
	for side in [-3.4, 3.4]:
		var pillar := MeshInstance3D.new()
		var p_mesh := CylinderMesh.new()
		p_mesh.top_radius = 0.42
		p_mesh.bottom_radius = 0.46
		p_mesh.height = 4.8
		p_mesh.material = CVB.make_mat(Color(0.95, 0.97, 1.0), 0.32, 0.10)
		pillar.mesh = p_mesh
		pillar.position = pos + Vector3(side, 2.4, 0.0)
		track_root.add_child(pillar)

	var beam := MeshInstance3D.new()
	var b_mesh := BoxMesh.new()
	b_mesh.size = Vector3(8.2, 0.90, 0.90)
	b_mesh.material = CVB.make_mat(col, 0.22, 0.82, Color(1.0, 0.78, 0.15), 0.30)
	beam.mesh = b_mesh
	beam.position = pos + Vector3(0.0, 5.0, 0.0)
	track_root.add_child(beam)

	var crown := MeshInstance3D.new()
	var cr_mesh := CylinderMesh.new()
	cr_mesh.top_radius = 0.85
	cr_mesh.bottom_radius = 0.68
	cr_mesh.height = 0.75
	cr_mesh.material = CVB.make_mat(Color(1.0, 0.84, 0.16), 0.20, 0.88, Color(1.0, 0.78, 0.12), 0.40)
	crown.mesh = cr_mesh
	crown.position = pos + Vector3(0.0, 5.8, 0.0)
	track_root.add_child(crown)

	var portal := MeshInstance3D.new()
	var port_mesh := BoxMesh.new()
	port_mesh.size = Vector3(5.8, 4.2, 0.14)
	port_mesh.material = CVB.make_mat(Color(0.0, 0.88, 1.0, 0.55), 0.15, 0.20, Color(0.0, 0.75, 1.0), 0.55)
	portal.mesh = port_mesh
	portal.position = pos + Vector3(0.0, 2.3, 0.0)
	track_root.add_child(portal)

func _on_player_jumped(pid: String, jump_pos: Vector3, is_double_jump: bool) -> void:
	if pid == "player_0":
		spawn_fx_burst(jump_pos + Vector3(0.0, 0.15, 0.0), Color(0.0, 0.92, 1.0) if is_double_jump else Color(1.0, 0.88, 0.32))

func _on_racer_checkpoint_reached(pid: String, cp_index: int) -> void:
	if pid == "player_0":
		checkpoint_banner_timer = 2.4
		checkpoint_banner_text = GameState.tr_text(
			"✨ CHECKPOINT %d / %d ACTIVATED! ✨" % [cp_index, total_checkpoints],
			"✨ تم تفعيل نقطة الحفظ %d / %d! ✨" % [cp_index, total_checkpoints]
		)
		spawn_fx_burst(player.global_position + Vector3(0.0, 0.5, 0.0), Color(0.0, 0.94, 1.0))

func _on_racer_respawn_started(pid: String, cp_index: int, _respawn_pos: Vector3) -> void:
	if pid == "player_0":
		checkpoint_banner_timer = 1.6
		if cp_index > 0:
			checkpoint_banner_text = GameState.tr_text("⚡ RESPAWNED AT CHECKPOINT %d" % cp_index, "⚡ العودة إلى نقطة الحفظ %d" % cp_index)
		else:
			checkpoint_banner_text = GameState.tr_text("⚡ RESPAWNED AT START SANCTUARY", "⚡ العودة إلى منصة البداية")

func _pulse_action_button(btn: Button) -> void:
	if is_instance_valid(btn):
		btn.pivot_offset = btn.size * 0.5
		btn.scale = Vector2(0.88, 0.88)

func _trigger_jump() -> void:
	if not run_active:
		return
	var now: int = Time.get_ticks_msec()
	if now - last_jump_ms < 85:
		return
	last_jump_ms = now
	_pulse_action_button(jump_btn)
	player.request_jump()

func _trigger_dive() -> void:
	if not run_active:
		return
	var now: int = Time.get_ticks_msec()
	if now - last_dive_ms < 85:
		return
	last_dive_ms = now
	_pulse_action_button(dive_btn)
	speed_fov_boost = 5.5
	player.request_dive()

func _trigger_dash() -> void:
	if not run_active:
		return
	var now: int = Time.get_ticks_msec()
	if now - last_dash_ms < 85:
		return
	last_dash_ms = now
	_pulse_action_button(dash_btn)
	speed_fov_boost = 7.5
	player.request_dash()

func _input(event: InputEvent) -> void:
	if not run_active:
		return

	var vp_size: Vector2 = get_viewport().get_visible_rect().size
	var half_w: float = maxf(480.0, vp_size.x * 0.45)

	if event is InputEventScreenTouch:
		if event.pressed:
			if menu_btn.get_global_rect().grow(10.0).has_point(event.position):
				return

			if jump_btn.get_global_rect().grow(18.0).has_point(event.position):
				_trigger_jump()
				return
			if dive_btn.get_global_rect().grow(18.0).has_point(event.position):
				_trigger_dive()
				return
			if dash_btn.get_global_rect().grow(18.0).has_point(event.position):
				_trigger_dash()
				return

			if event.position.x < half_w and event.position.y > 105.0 and joy_touch_id == -1:
				joy_touch_id = event.index
				var base_center: Vector2 = joystick_base.global_position + joystick_base.size * 0.5
				if event.position.distance_to(base_center) < 165.0:
					joy_center = base_center
					var init_delta: Vector2 = event.position - joy_center
					player.joy_input = (init_delta / 78.0).limit_length(1.0)
					if is_instance_valid(joystick_knob):
						joystick_knob.position = Vector2(47.0, 47.0) + player.joy_input * 42.0
				else:
					joy_center = event.position
					player.joy_input = Vector2.ZERO
			elif event.position.x >= half_w and event.position.y > 90.0 and event.position.y < vp_size.y - 165.0 and cam_touch_id == -1:
				cam_touch_id = event.index
		else:
			if event.index == joy_touch_id:
				joy_touch_id = -1
				player.joy_input = Vector2.ZERO
				if is_instance_valid(joystick_knob):
					joystick_knob.position = Vector2(47.0, 47.0)
			elif event.index == cam_touch_id:
				cam_touch_id = -1

	elif event is InputEventScreenDrag:
		if event.index == joy_touch_id:
			var delta_v: Vector2 = event.position - joy_center
			player.joy_input = (delta_v / 78.0).limit_length(1.0)
			if is_instance_valid(joystick_knob):
				joystick_knob.position = Vector2(47.0, 47.0) + player.joy_input * 42.0
		elif event.index == cam_touch_id:
			player.camera_yaw_deg = clampf(player.camera_yaw_deg - event.relative.x * 0.24, -120.0, 120.0)

func _process(delta: float) -> void:
	for ab in [jump_btn, dive_btn, dash_btn]:
		if is_instance_valid(ab):
			ab.scale = ab.scale.lerp(Vector2.ONE, delta * 14.0)

	for cr in sky_crystals:
		if is_instance_valid(cr):
			cr.rotation.y += delta * 1.8

	for i in range(fx_bursts.size() - 1, -1, -1):
		var fx: Dictionary = fx_bursts[i]
		fx["life"] = float(fx["life"]) - delta
		var nd: Node3D = fx["node"]
		if float(fx["life"]) <= 0.0 or not is_instance_valid(nd):
			if is_instance_valid(nd):
				nd.queue_free()
			fx_bursts.remove_at(i)
		else:
			var prog: float = 1.0 - (float(fx["life"]) / float(fx["max_life"]))
			nd.scale = Vector3(1.0 + prog * 2.4, maxf(0.1, 1.0 - prog), 1.0 + prog * 2.4)
			nd.position.y += delta * 1.4

	if not run_active:
		return

	elapsed_sec += delta
	survival_tier = 1 + int(elapsed_sec / 20.0)
	speed_multiplier = clampf(1.0 + elapsed_sec * 0.010 + float(survival_tier - 1) * 0.05, 1.0, 1.8)

	# 3D Footstep / Stardust Trail behind the Hero while running or dashing!
	trail_spawn_timer -= delta
	var horiz_spd: float = Vector2(player.velocity.x, player.velocity.z).length()
	if horiz_spd > 2.4 and trail_spawn_timer <= 0.0 and fx_bursts.size() < 18:
		trail_spawn_timer = 0.14 if player.dive_timer <= 0.0 else 0.07
		var trail_col: Color = Color(1.0, 0.84, 0.22) if GameState.selected_trail_id > 0 else (Color(1.0, 0.45, 0.78) if player.character_skin_id == 1 else Color(0.0, 0.90, 1.0))
		spawn_fx_burst(player.global_position + Vector3(0.0, 0.08, 0.25), trail_col, 0.26, 0.16)

	# Smooth Dynamic Speed FOV Camera Boost
	speed_fov_boost = move_toward(speed_fov_boost, 0.0, delta * 9.0)
	camera_3d.fov = lerpf(camera_3d.fov, 60.0 + speed_fov_boost, delta * 10.0)

	if checkpoint_banner_timer > 0.0:
		checkpoint_banner_timer = maxf(0.0, checkpoint_banner_timer - delta)

	for mp in moving_platforms:
		var body: AnimatableBody3D = mp["node"]
		body.position.x = float(mp["base_x"]) + sin(elapsed_sec * float(mp["speed"])) * float(mp["amp_x"])

	for ft in falling_tiles:
		var body: StaticBody3D = ft["body"]
		var col_shape: CollisionShape3D = ft["col"]
		var mat: StandardMaterial3D = ft["mat"]
		var st: int = int(ft["state"])
		var bpos: Vector3 = ft["base_pos"]
		if st == 0:
			var dx: float = absf(player.global_position.x - bpos.x)
			var dz: float = absf(player.global_position.z - bpos.z)
			if dx < float(ft["half_x"]) and dz < float(ft["half_z"]) and absf(player.global_position.y - (bpos.y + 0.5)) < 1.1:
				ft["state"] = 1
				ft["timer"] = 0.68
		elif st == 1:
			ft["timer"] = float(ft["timer"]) - delta
			body.position.x = bpos.x + sin(elapsed_sec * 42.0) * 0.09
			mat.albedo_color = Color(1.0, 0.25, 0.18)
			if float(ft["timer"]) <= 0.0:
				ft["state"] = 2
				ft["timer"] = 2.6
				body.visible = false
				col_shape.disabled = true
		elif st == 2:
			ft["timer"] = float(ft["timer"]) - delta
			if float(ft["timer"]) <= 0.0:
				ft["state"] = 0
				body.position = bpos
				body.visible = true
				col_shape.disabled = false
				mat.albedo_color = Color(1.0, 0.72, 0.12)

	for pb in push_blocks:
		var node: Node3D = pb["node"]
		var prev_x: float = node.position.x
		node.position.x = float(pb["base_x"]) + sin(elapsed_sec * float(pb["speed"]) + float(pb["phase"])) * float(pb["amp_x"])
		var vx: float = (node.position.x - prev_x) / maxf(delta, 0.001)
		if player.global_position.distance_to(node.global_position) < 1.55:
			player.velocity.x = signf(vx) * 8.5
			player.velocity.y = 4.4
			_play_hit_sfx()

	for rb in rolling_boulders:
		var bnode: Node3D = rb["node"]
		bnode.position.z += float(rb["speed"]) * delta
		bnode.rotation.x += float(rb["speed"]) * delta * 0.95
		if bnode.position.z > float(rb["max_z"]):
			bnode.position.z = float(rb["min_z"])
			rb["offset_x"] = -float(rb["offset_x"])
			bnode.position.x = float(rb["track_x"]) + float(rb["offset_x"])
		if player.global_position.distance_to(bnode.global_position) < 1.55:
			player.velocity.z = 8.2
			player.velocity.y = 5.8
			_play_hit_sfx()
			spawn_fx_burst(bnode.global_position, Color(1.0, 0.45, 0.12))

	for h in hammers:
		var node: Node3D = h["node"]
		node.rotation.y += float(h["speed"]) * speed_multiplier * delta
		if player.global_position.distance_to(node.global_position) < 2.2 and absf(player.global_position.y - node.global_position.y) < 1.2:
			player.velocity.z = 7.2
			player.velocity.y = 5.2
			_play_hit_sfx()
		for r in rivals:
			if r.global_position.distance_to(node.global_position) < 2.2 and absf(r.global_position.y - node.global_position.y) < 1.2:
				r.velocity.z = 6.8
				r.velocity.y = 4.8

	for p in pendulums:
		var piv: Node3D = p["pivot"]
		var head_mesh: Node3D = p["head"]
		var spd: float = float(p["speed"])
		var ph: float = float(p["phase"])
		piv.rotation.z = sin(elapsed_sec * spd + ph) * deg_to_rad(52.0)
		if player.global_position.distance_to(head_mesh.global_position) < 1.45:
			player.velocity.x = signf(sin(elapsed_sec * spd + ph)) * 7.8
			player.velocity.y = 5.4
			_play_hit_sfx()

	for bp in bounce_pads:
		if is_instance_valid(bp) and player.global_position.distance_to(bp.global_position) < 1.35:
			player.velocity.y = player.jump_velocity * 1.38
			speed_fov_boost = 4.5
			AudioSynth.play_sfx("bounce")
			spawn_fx_burst(bp.global_position + Vector3(0.0, 0.2, 0.0), Color(1.0, 0.22, 0.58))

	for sp in speed_pads:
		if is_instance_valid(sp) and player.global_position.distance_to(sp.global_position) < 1.55:
			player.velocity.z = minf(player.velocity.z, -player.move_speed * 1.45)
			speed_fov_boost = 6.5
			var now_sp: int = Time.get_ticks_msec()
			if now_sp - last_speed_sfx_ms > 500:
				last_speed_sfx_ms = now_sp
				AudioSynth.play_sfx("dash")

	_update_ai_rivals(delta)

	for c in coin_nodes:
		if is_instance_valid(c) and c.visible:
			c.rotation.y += delta * 3.2
			if player.global_position.distance_to(c.global_position) < 1.45:
				c.visible = false
				coins_collected += 1
				AudioSynth.play_sfx("coin")
				spawn_fx_burst(c.global_position, Color(1.0, 0.88, 0.22))

	var target_cam_pos: Vector3 = player.global_position + Vector3(0.0, 4.4, 7.6).rotated(Vector3.UP, deg_to_rad(player.camera_yaw_deg))
	camera_rig.global_position = camera_rig.global_position.lerp(target_cam_pos, delta * 8.0)
	camera_3d.look_at(player.global_position + Vector3(0.0, 1.15, -1.8), Vector3.UP)

	var prog_ratio: float = clampf((1.5 - player.global_position.z) / maxf(10.0, 1.5 - finish_z), 0.0, 1.0)
	if is_instance_valid(race_progress_fill):
		race_progress_fill.size.x = maxf(16.0, 412.0 * prog_ratio)

	var elapsed_ms: int = int(elapsed_sec * 1000.0)
	var goal_ms: int = GameState.goal_time_ms_for_level(level_number)
	var tier_txt: String = str(stage_profile["tier_name_ar"]) if GameState.is_arabic() else str(stage_profile["tier_name"])
	var obs_txt: String = str(stage_profile["new_obstacle_ar"]) if GameState.is_arabic() else str(stage_profile["new_obstacle"])

	hud_level_label.text = "👑 W%d S%d [%s] | CP %d/%d" % [world_id, level_number, tier_txt, player.last_activated_checkpoint, total_checkpoints]
	hud_timer_label.text = GameState.tr_text(
		"⏱ %02d.%02ds / Goal <= %ds" % [int(elapsed_sec), int(fmod(elapsed_sec * 100.0, 100.0)), int(goal_ms / 1000)],
		"⏱ %02d.%02d ث / الهدف <= %d ث" % [int(elapsed_sec), int(fmod(elapsed_sec * 100.0, 100.0)), int(goal_ms / 1000)]
	)
	hud_timer_label.modulate = Color(0.25, 0.98, 0.62) if elapsed_ms <= goal_ms else Color(1.0, 0.40, 0.48)
	hud_coins_label.text = "🪙 %d  |  ⚡ %.2fx" % [coins_collected, speed_multiplier]

	if checkpoint_banner_timer > 0.0:
		hud_goal_banner.text = checkpoint_banner_text
		hud_goal_banner.modulate = Color(0.0, 0.94, 1.0)
	else:
		var goal_sec: int = int(goal_ms / 1000)
		hud_goal_banner.text = GameState.tr_text("GOAL <= %ds • %s" % [goal_sec, obs_txt], "الهدف <= %d ث • %s" % [goal_sec, obs_txt])
		hud_goal_banner.modulate = Color(1.0, 0.86, 0.22)

	if player.global_position.y < void_floor_y or absf(player.global_position.x) > 45.0:
		player.respawn_at_checkpoint()

	if player.global_position.z <= finish_z + 2.2:
		complete_level(elapsed_ms)

func _get_track_center_x_at_z(z_val: float) -> float:
	if track_waypoints.is_empty():
		return 0.0
	var best_x: float = 0.0
	var best_dist: float = 99999.0
	for wp in track_waypoints:
		var d: float = absf(wp.z - z_val)
		if d < best_dist:
			best_dist = d
			best_x = wp.x
	return best_x

func _update_ai_rivals(delta: float) -> void:
	for r in rivals:
		if r.finished:
			continue
		var center_x: float = _get_track_center_x_at_z(r.global_position.z - 2.5)
		var target_x: float = center_x + r.lane_bias_x * sin(elapsed_sec * 0.9 + float(r.get_index()))
		var dx: float = target_x - r.global_position.x
		r.velocity.x = move_toward(r.velocity.x, clampf(dx * 3.5, -4.2, 4.2), 16.0 * delta)
		r.velocity.z = move_toward(r.velocity.z, -r.move_speed, 16.0 * delta)

		if r.is_on_floor() and fmod(elapsed_sec + float(r.get_index()) * 0.47, 1.55) < 0.09:
			r.request_jump()

		if r.global_position.y < void_floor_y or absf(r.global_position.x) > 45.0:
			r.respawn_at_checkpoint()

		if r.global_position.z <= finish_z + 2.2:
			r.finished = true
			r.velocity = Vector3.ZERO

func complete_level(elapsed_ms: int) -> void:
	run_active = false
	player.finished = true
	AudioSynth.play_sfx("victory")
	spawn_fx_burst(player.global_position + Vector3(0.0, 0.8, 0.0), Color(1.0, 0.86, 0.20))
	var outcome: Dictionary = GameState.record_level_win(level_number, elapsed_ms, coins_collected, 1)
	can_advance_next = bool(outcome["can_advance"])
	result_panel.visible = true
	next_btn.visible = can_advance_next

	var goal_sec: float = float(outcome["goal_ms"]) / 1000.0
	var run_sec: float = float(elapsed_ms) / 1000.0
	if int(outcome["newly_unlocked_world"]) > 0:
		result_title.text = GameState.tr_text(
			"🌍 NEW WORLD %d UNLOCKED!" % int(outcome["newly_unlocked_world"]),
			"🌍 تم فتح العالم الجديد %d بنجاح!" % int(outcome["newly_unlocked_world"])
		)
	elif bool(outcome["goal_met"]):
		result_title.text = GameState.tr_text("🏆 STAGE COMPLETED — GOAL MET!", "🏆 اكتملت المرحلة بنجاح — حققت الهدف!")
	else:
		result_title.text = GameState.tr_text("🏁 FINISHED (GOAL TIME MISSED)", "🏁 وصلت للنهاية (تجاوزت الوقت المطلوب)")

	result_details.text = GameState.tr_text(
		"Time: %.2fs (Goal <= %.0fs)  |  Stars: ★%d  |  +%d Coins" % [run_sec, goal_sec, int(outcome["stars"]), int(outcome["reward_coins"])],
		"الوقت: %.2f ث (الهدف <= %.0f ث)  |  النجوم: ★%d  |  +%d عملة" % [run_sec, goal_sec, int(outcome["stars"]), int(outcome["reward_coins"])]
	)

func _on_jump_button_pressed() -> void:
	_trigger_jump()

func _on_dive_button_pressed() -> void:
	_trigger_dive()

func _on_dash_button_pressed() -> void:
	_trigger_dash()

func _on_retry_button_pressed() -> void:
	AudioSynth.play_sfx("click")
	get_tree().reload_current_scene()

func _on_next_button_pressed() -> void:
	if can_advance_next and level_number < 50:
		AudioSynth.play_sfx("click")
		GameState.current_level_number = level_number + 1
		get_tree().reload_current_scene()

func _on_menu_button_pressed() -> void:
	AudioSynth.play_sfx("click")
	get_tree().change_scene_to_file("res://scenes/main_menu.tscn")
