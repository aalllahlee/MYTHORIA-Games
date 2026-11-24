extends Node3D

## MYTHORIA games — By ahmed
## DON'T FALL 3D Gameplay Scene:
## - Progressive Difficulty Curve ("Very Easy -> Hard") with New Obstacles Each Stage:
##   Stage 1 (Very Easy): Wide tracks, speed pads, coins, gentle bumper
##   Stage 2 (Easy): + Moving Platforms & Bounce Pads
##   Stage 3 (Normal): + Swinging Pendulums
##   Stage 4 (Challenging): + Falling/Blinking Golden Tiles & Sideways Sliding Push-Blocks
##   Stage 5 (Hard Gate): + Rolling Giant Mythoria Boulders
##   Stages 6–50 (Worlds 2–10): Narrowing paths, faster obstacles & multi-obstacle gauntlets
## - Upgraded 3D Graphics: Dynamic World Sky/Fog, Procedural Tiled Marble & Checkerboard Finish,
##   3D Sparkle/Ring FX Bursts, and Ground Contact Blob Shadows.
## - 100% Working UI Buttons + Simultaneous Multi-Touch Analog Joystick & Jump/Dive/Dash!

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
var finish_z: float = -75.0
var void_floor_y: float = -7.5
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

var joy_touch_id: int = -1
var cam_touch_id: int = -1
var joy_center: Vector2 = Vector2(133.0, 587.0)
var last_jump_ms: int = 0
var last_dive_ms: int = 0
var last_dash_ms: int = 0
var can_advance_next: bool = false

func _ready() -> void:
	DisplayServer.screen_set_orientation(DisplayServer.SCREEN_LANDSCAPE)
	level_number = GameState.current_level_number
	world_id = int((level_number - 1) / 5) + 1
	stage_profile = GameState.get_stage_difficulty_profile(level_number)
	result_panel.visible = false
	_apply_world_atmosphere()
	_apply_hud_styles()
	_connect_action_buttons()
	player.player_id = "player_0"
	player.is_ai_racer = false
	player.character_skin_id = GameState.selected_skin_id
	player._ensure_character_visual()
	player.initialize_spawn(Vector3(0.0, 1.8, 1.5), 180.0)
	player.checkpoint_reached.connect(_on_racer_checkpoint_reached)
	player.individual_respawn_started.connect(_on_racer_respawn_started)
	player.player_jumped.connect(_on_player_jumped)
	build_3d_course()
	spawn_ai_rivals()

	checkpoint_banner_timer = 3.2
	checkpoint_banner_text = "✨ STAGE %d [%s] — %s" % [
		level_number,
		str(stage_profile["tier_name"]),
		str(stage_profile["new_obstacle"])
	]

func _apply_world_atmosphere() -> void:
	if not is_instance_valid(world_env) or world_env.environment == null:
		return
	var wt: Dictionary = GameState.get_world_theme(world_id)
	var env: Environment = world_env.environment.duplicate()
	var sky_top: Color = wt["sky_top"]
	var sky_bot: Color = wt["sky_bottom"]
	env.fog_enabled = true
	env.fog_light_color = sky_bot.lerp(Color.WHITE, 0.25)
	env.fog_density = 0.0055
	if env.sky != null and env.sky.sky_material is ProceduralSkyMaterial:
		var sky_copy: Sky = env.sky.duplicate()
		var psm: ProceduralSkyMaterial = env.sky.sky_material.duplicate()
		psm.sky_top_color = sky_top
		psm.sky_horizon_color = sky_bot
		psm.ground_horizon_color = sky_bot
		psm.ground_bottom_color = sky_top.darkened(0.25)
		sky_copy.sky_material = psm
		env.sky = sky_copy
	world_env.environment = env

func _connect_action_buttons() -> void:
	# Instant press mode for gameplay action buttons + debounced multi-touch in _input()
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

	CVB.style_button(menu_btn, Color(0.12, 0.28, 0.65, 0.95), Color(0.55, 0.85, 1.0), Color.WHITE, 16, 14, 3)
	CVB.style_button(jump_btn, Color(0.14, 0.72, 0.36, 0.95), Color(0.68, 1.0, 0.78), Color.WHITE, 18, 52, 4)
	CVB.style_button(dive_btn, Color(0.72, 0.18, 0.58, 0.95), Color(1.0, 0.72, 0.94), Color.WHITE, 18, 52, 4)
	CVB.style_button(dash_btn, Color(0.08, 0.48, 0.88, 0.95), Color(1.0, 0.84, 0.22), Color(1.0, 0.94, 0.42), 18, 52, 4)

	CVB.style_button(retry_btn, Color(0.14, 0.34, 0.72, 0.96), Color(0.55, 0.85, 1.0), Color.WHITE, 18, 16, 3)
	CVB.style_button(next_btn, Color(1.0, 0.68, 0.04, 0.98), Color(1.0, 0.94, 0.52), Color.WHITE, 18, 16, 3)
	CVB.style_button(menu_result_btn, Color(0.22, 0.28, 0.48, 0.96), Color(0.65, 0.78, 0.96), Color.WHITE, 18, 16, 3)

	if not GameState.dash_unlocked:
		dash_btn.text = "🔒\nDASH"
		dash_btn.modulate = Color(0.75, 0.80, 0.90, 0.75)

func build_3d_course() -> void:
	var w_theme: Dictionary = GameState.get_world_theme(world_id)
	var prim_col: Color = w_theme["primary"]
	var sec_col: Color = w_theme["secondary"]
	var acc_col: Color = w_theme["accent"]

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

		# Progressive Platform Type per Stage Difficulty:
		if allow_falling and (seg % 4 == 2):
			# NEW OBSTACLE (Stage 4+): Falling/Blinking Golden Tile Bridge!
			spawn_falling_tile_bridge(Vector3(0.0, 0.0, center_z), plat_w, seg_len, acc_col)
		elif allow_moving and (seg % 3 == 1):
			# NEW OBSTACLE (Stage 2+): Wide Smooth Moving Platform!
			var move_amp: float = clampf(1.6 + float(level_number) * 0.04, 1.6, 3.0)
			spawn_moving_platform(Vector3(0.0, 0.0, center_z), Vector3(maxf(5.6, plat_w - 0.8), 1.2, seg_len), col, acc_col, move_amp, (0.95 + float(seg) * 0.06) * obs_spd)
		else:
			spawn_platform(Vector3(0.0, 0.0, center_z), Vector3(plat_w, 1.2, seg_len), col, acc_col, false)

		# Generous Coin Trails to guide the player's ideal line
		spawn_coin(Vector3(0.0, 1.55, center_z))
		spawn_coin(Vector3(-1.8, 1.55, center_z - 1.8))
		spawn_coin(Vector3(1.8, 1.55, center_z + 1.8))

		# Progressive Obstacles (Very Easy Stage 1 -> Hard Stage 5+):
		if level_number == 1:
			# VERY EASY STAGE 1: Fun speed pads + only 1 slow center bumper on segment 3!
			if seg == 3:
				spawn_rotating_hammer(Vector3(0.0, 0.65, center_z), acc_col, 0.95)
			else:
				spawn_speed_pad(Vector3(0.0, 0.68, center_z))
		else:
			if allow_boulder and (seg == seg_count - 1 or seg % 5 == 4):
				# NEW OBSTACLE (Stage 5+): Rolling Giant Mythoria Boulders!
				spawn_rolling_boulder(Vector3(-1.5 if (seg % 2 == 0) else 1.5, 1.75, center_z - seg_len * 0.42), center_z + seg_len * 0.48, center_z - seg_len * 0.45, 5.2 * obs_spd)
			elif allow_push and (seg % 4 == 3):
				# NEW OBSTACLE (Stage 4+): Sideways Sliding Mythoria Push-Blocks!
				spawn_push_block(Vector3(0.0, 1.35, center_z), plat_w * 0.42, 1.65 * obs_spd, acc_col)
			elif allow_pendulum and (seg % 3 == 1):
				# NEW OBSTACLE (Stage 3+): Swinging Pendulum!
				spawn_swinging_pendulum(Vector3(0.0, 5.2, center_z), acc_col, 1.75 * obs_spd)
			elif seg % hammer_mod == 0:
				spawn_rotating_hammer(Vector3(0.0, 0.65, center_z), acc_col, 1.55 * obs_spd)
			else:
				if allow_bounce:
					spawn_bounce_pad(Vector3(-1.8 if (seg % 2 == 0) else 1.8, 0.68, center_z + 1.4))
				spawn_speed_pad(Vector3(0.0, 0.68, center_z - 1.4))

		# Scenic Floating Greek Islands flanking the track
		if seg % 2 == 0:
			var side_dir: float = -1.0 if (seg % 4 == 0) else 1.0
			spawn_scenic_floating_island(Vector3(side_dir * 13.5, 0.5, center_z), prim_col, acc_col)

		cur_z -= (seg_len + gap_len)

		var target_cp: int = int(((seg + 1) * 4) / seg_count)
		var prev_cp: int = int((seg * 4) / seg_count)
		if target_cp > prev_cp and cp_spawned < 4:
			cp_spawned += 1
			var cp_platform_pos := Vector3(0.0, 0.0, cur_z - 3.4)
			spawn_platform(cp_platform_pos, Vector3(maxf(plat_w, 8.0), 1.35, 6.8), acc_col, Color.WHITE, false)
			spawn_checkpoint_gate(Vector3(0.0, 0.68, cur_z - 3.4), cp_spawned)
			spawn_coin(Vector3(-2.1, 1.55, cur_z - 3.4))
			spawn_coin(Vector3(2.1, 1.55, cur_z - 3.4))
			cur_z -= (6.8 + gap_len)

	total_checkpoints = max(1, cp_spawned)
	finish_z = cur_z - 4.6
	spawn_platform(Vector3(0.0, 0.0, finish_z), Vector3(10.6, 1.5, 9.4), acc_col, acc_col, true)
	spawn_finish_arch(Vector3(0.0, 0.75, finish_z), acc_col)

	var goal_sec: int = int(GameState.goal_time_ms_for_level(level_number) / 1000)
	hud_level_label.text = "👑 W%d S%d [%s] | CP 0/%d" % [world_id, level_number, str(stage_profile["tier_name"]), total_checkpoints]
	hud_goal_banner.text = "GOAL <= %ds • %s" % [goal_sec, str(stage_profile["new_obstacle"])]

func spawn_fx_burst(pos: Vector3, col: Color) -> void:
	var ring := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.35
	cyl.bottom_radius = 0.55
	cyl.height = 0.14
	cyl.material = CVB.make_mat(col, 0.15, 0.20, col, 2.2)
	ring.mesh = cyl
	ring.position = pos
	track_root.add_child(ring)
	fx_bursts.append({"node": ring, "life": 0.42, "max_life": 0.42})

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
	fm.material = CVB.make_mat(Color(1.0, 0.62, 0.08), 0.15, 0.10, Color(1.0, 0.58, 0.08), 2.5)
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
	var offsets := [-2.4, -0.9, 0.9, 2.4]
	var base_scale: float = clampf(0.76 + float(level_number - 1) * 0.014, 0.76, 1.06)
	var speeds := [8.2 * base_scale, 7.8 * base_scale, 7.45 * base_scale, 7.05 * base_scale]
	var skin_ids := [1, 2, 4, 6]
	for i in range(4):
		var rival := CharacterBody3D.new()
		rival.set_script(PlayerScript)
		rival.name = "Rival_%d" % (i + 1)
		rival.player_id = "rival_%d" % (i + 1)
		rival.is_ai_racer = true
		rival.character_skin_id = skin_ids[i % skin_ids.size()]
		rival.move_speed = speeds[i]
		rival.lane_bias_x = offsets[i] * 0.6

		var col := CollisionShape3D.new()
		var cap := CapsuleShape3D.new()
		cap.radius = 0.36
		cap.height = 1.3
		col.shape = cap
		col.position = Vector3(0.0, 0.65, 0.0)
		rival.add_child(col)

		var start_pos := Vector3(offsets[i], 1.8, 1.5 + float(i % 2) * 0.5)
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

			var mat := CVB.make_mat(Color(1.0, 0.72, 0.12), 0.25, 0.45, acc_col, 0.28)
			var mi := MeshInstance3D.new()
			var bm := BoxMesh.new()
			bm.size = Vector3(tile_w, 0.85, tile_l)
			bm.material = mat
			mi.mesh = bm
			body.add_child(mi)

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
	var block := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = Vector3(2.2, 1.35, 1.6)
	bm.material = CVB.make_mat(Color(0.95, 0.22, 0.26), 0.24, 0.35, acc_col, 0.38)
	block.mesh = bm
	block.position = center_pos
	track_root.add_child(block)
	push_blocks.append({"node": block, "base_x": center_pos.x, "amp_x": amp_x, "speed": spd, "phase": center_pos.z * 0.15})

func spawn_rolling_boulder(start_pos: Vector3, max_z: float, min_z: float, spd: float) -> void:
	var boulder := MeshInstance3D.new()
	var sp := SphereMesh.new()
	sp.radius = 1.15
	sp.height = 2.30
	sp.material = CVB.make_mat(Color(0.45, 0.36, 0.32), 0.48, 0.22, Color(1.0, 0.42, 0.10), 0.35)
	boulder.mesh = sp
	boulder.position = start_pos
	track_root.add_child(boulder)
	rolling_boulders.append({
		"node": boulder,
		"min_z": min_z,
		"max_z": max_z,
		"speed": spd,
		"base_x": start_pos.x
	})

func spawn_rotating_hammer(pos: Vector3, col: Color, spd: float) -> void:
	var pivot := Node3D.new()
	pivot.position = pos

	var hub := MeshInstance3D.new()
	var hub_mesh := CylinderMesh.new()
	hub_mesh.top_radius = 0.45
	hub_mesh.bottom_radius = 0.58
	hub_mesh.height = 1.45
	hub_mesh.material = CVB.make_mat(Color(1.0, 0.82, 0.18), 0.22, 0.82)
	hub.mesh = hub_mesh
	hub.position = Vector3(0.0, 0.45, 0.0)
	pivot.add_child(hub)

	var bar := MeshInstance3D.new()
	var bar_mesh := BoxMesh.new()
	bar_mesh.size = Vector3(5.6, 0.34, 0.34)
	bar_mesh.material = CVB.make_mat(col, 0.28, 0.52)
	bar.mesh = bar_mesh
	bar.position = Vector3(0.0, 0.65, 0.0)
	pivot.add_child(bar)

	var red_mat := CVB.make_mat(Color(0.94, 0.18, 0.22), 0.28, 0.15)
	var white_mat := CVB.make_mat(Color(0.98, 0.98, 1.0), 0.25, 0.08)
	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		var mallet := MeshInstance3D.new()
		var m_cyl := CylinderMesh.new()
		m_cyl.top_radius = 0.48
		m_cyl.bottom_radius = 0.48
		m_cyl.height = 1.25
		m_cyl.material = red_mat
		mallet.mesh = m_cyl
		mallet.position = Vector3(sf * 2.65, 0.65, 0.0)
		mallet.rotation.x = deg_to_rad(90.0)
		pivot.add_child(mallet)

		var stripe := MeshInstance3D.new()
		var s_cyl := CylinderMesh.new()
		s_cyl.top_radius = 0.50
		s_cyl.bottom_radius = 0.50
		s_cyl.height = 0.42
		s_cyl.material = white_mat
		stripe.mesh = s_cyl
		stripe.position = Vector3(sf * 2.65, 0.65, 0.0)
		stripe.rotation.x = deg_to_rad(90.0)
		pivot.add_child(stripe)

	track_root.add_child(pivot)
	hammers.append({"node": pivot, "speed": spd})

func spawn_swinging_pendulum(pos: Vector3, _col: Color, spd: float) -> void:
	var frame := Node3D.new()
	frame.position = pos
	track_root.add_child(frame)

	var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.20, 0.85)
	var mat_blade := CVB.make_mat(Color(0.0, 0.88, 1.0), 0.18, 0.45, Color(0.0, 0.78, 1.0), 1.1)

	for side_dir in [-1, 1]:
		var p := MeshInstance3D.new()
		var pm := CylinderMesh.new()
		pm.top_radius = 0.22
		pm.bottom_radius = 0.26
		pm.height = 4.8
		pm.material = mat_gold
		p.mesh = pm
		p.position = Vector3(float(side_dir) * 3.2, -2.2, 0.0)
		frame.add_child(p)

	var top_bar := MeshInstance3D.new()
	var tb := BoxMesh.new()
	tb.size = Vector3(6.8, 0.35, 0.35)
	tb.material = mat_gold
	top_bar.mesh = tb
	frame.add_child(top_bar)

	var swing_pivot := Node3D.new()
	frame.add_child(swing_pivot)

	var rod := MeshInstance3D.new()
	var rm := CylinderMesh.new()
	rm.top_radius = 0.10
	rm.bottom_radius = 0.10
	rm.height = 3.6
	rm.material = mat_gold
	rod.mesh = rm
	rod.position = Vector3(0.0, -1.8, 0.0)
	swing_pivot.add_child(rod)

	var hammer_head := MeshInstance3D.new()
	var hm := CylinderMesh.new()
	hm.top_radius = 0.75
	hm.bottom_radius = 0.75
	hm.height = 1.4
	hm.material = mat_blade
	hammer_head.mesh = hm
	hammer_head.position = Vector3(0.0, -3.6, 0.0)
	hammer_head.rotation.x = deg_to_rad(90.0)
	swing_pivot.add_child(hammer_head)

	pendulums.append({"pivot": swing_pivot, "head": hammer_head, "speed": spd, "phase": pos.z * 0.2})

func spawn_bounce_pad(pos: Vector3) -> void:
	var pad := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.98
	cyl.bottom_radius = 1.10
	cyl.height = 0.24
	cyl.material = CVB.make_mat(Color(1.0, 0.22, 0.58), 0.18, 0.35, Color(1.0, 0.18, 0.55), 1.4)
	pad.mesh = cyl
	pad.position = pos
	track_root.add_child(pad)
	bounce_pads.append(pad)

func spawn_speed_pad(pos: Vector3) -> void:
	var pad := MeshInstance3D.new()
	var box := BoxMesh.new()
	box.size = Vector3(2.4, 0.12, 2.8)
	box.material = CVB.make_mat(Color(0.0, 0.92, 1.0), 0.15, 0.40, Color(0.0, 0.85, 1.0), 1.8)
	pad.mesh = box
	pad.position = pos
	track_root.add_child(pad)
	speed_pads.append(pad)

func spawn_coin(pos: Vector3) -> void:
	var c := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.44
	cyl.bottom_radius = 0.44
	cyl.height = 0.16
	var mat := CVB.make_mat(Color(1.0, 0.84, 0.16), 0.18, 0.88, Color(1.0, 0.76, 0.10), 0.45)
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
	b_mesh.material = CVB.make_mat(col, 0.22, 0.82, Color(1.0, 0.78, 0.15), 0.35)
	beam.mesh = b_mesh
	beam.position = pos + Vector3(0.0, 5.0, 0.0)
	track_root.add_child(beam)

	var crown := MeshInstance3D.new()
	var cr_mesh := CylinderMesh.new()
	cr_mesh.top_radius = 0.85
	cr_mesh.bottom_radius = 0.68
	cr_mesh.height = 0.75
	cr_mesh.material = CVB.make_mat(Color(1.0, 0.84, 0.16), 0.20, 0.88, Color(1.0, 0.78, 0.12), 0.55)
	crown.mesh = cr_mesh
	crown.position = pos + Vector3(0.0, 5.8, 0.0)
	track_root.add_child(crown)

	var portal := MeshInstance3D.new()
	var port_mesh := BoxMesh.new()
	port_mesh.size = Vector3(5.8, 4.2, 0.14)
	port_mesh.material = CVB.make_mat(Color(0.0, 0.88, 1.0, 0.55), 0.15, 0.20, Color(0.0, 0.75, 1.0), 1.6)
	portal.mesh = port_mesh
	portal.position = pos + Vector3(0.0, 2.3, 0.0)
	track_root.add_child(portal)

func _on_player_jumped(pid: String, jump_pos: Vector3, is_double_jump: bool) -> void:
	if pid == "player_0":
		spawn_fx_burst(jump_pos + Vector3(0.0, 0.15, 0.0), Color(0.0, 0.92, 1.0) if is_double_jump else Color(1.0, 0.88, 0.32))

func _on_racer_checkpoint_reached(pid: String, cp_index: int) -> void:
	if pid == "player_0":
		checkpoint_banner_timer = 2.4
		checkpoint_banner_text = "✨ CHECKPOINT %d / %d ACTIVATED! ✨" % [cp_index, total_checkpoints]
		spawn_fx_burst(player.global_position + Vector3(0.0, 0.5, 0.0), Color(0.0, 0.94, 1.0))

func _on_racer_respawn_started(pid: String, cp_index: int, _respawn_pos: Vector3) -> void:
	if pid == "player_0":
		checkpoint_banner_timer = 1.6
		if cp_index > 0:
			checkpoint_banner_text = "⚡ RESPAWNED AT CHECKPOINT %d" % cp_index
		else:
			checkpoint_banner_text = "⚡ RESPAWNED AT START SANCTUARY"

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
	player.request_dive()

func _trigger_dash() -> void:
	if not run_active:
		return
	var now: int = Time.get_ticks_msec()
	if now - last_dash_ms < 85:
		return
	last_dash_ms = now
	_pulse_action_button(dash_btn)
	player.request_dash()

## BULLETPROOF MULTI-TOUCH INPUT SYSTEM (works with pointing/emulate_mouse_from_touch=true):
## - Menu/Result buttons work 100% natively on Android!
## - Finger 0 can hold/drag the Virtual Analog Joystick while Finger 1 simultaneously taps
##   JUMP, DIVE, or DASH (with 85ms debounce preventing double-fire on single-finger tap)!
func _input(event: InputEvent) -> void:
	if not run_active:
		return

	var vp_size: Vector2 = get_viewport().get_visible_rect().size
	var half_w: float = maxf(480.0, vp_size.x * 0.45)

	if event is InputEventScreenTouch:
		if event.pressed:
			# Let MenuButton receive its native click cleanly
			if menu_btn.get_global_rect().grow(10.0).has_point(event.position):
				return

			# 1. Multi-touch direct hit on JUMP / DIVE / DASH (works on ANY finger index 0, 1, 2!)
			if jump_btn.get_global_rect().grow(18.0).has_point(event.position):
				_trigger_jump()
				return
			if dive_btn.get_global_rect().grow(18.0).has_point(event.position):
				_trigger_dive()
				return
			if dash_btn.get_global_rect().grow(18.0).has_point(event.position):
				_trigger_dash()
				return

			# 2. Left side below top HUD bar -> Analog Joystick Finger
			if event.position.x < half_w and event.position.y > 105.0 and joy_touch_id == -1:
				joy_touch_id = event.index
				var base_center: Vector2 = joystick_base.global_position + joystick_base.size * 0.5
				if event.position.distance_to(base_center) < 165.0:
					joy_center = base_center
					var init_delta: Vector2 = event.position - joy_center
					player.joy_input = (init_delta / 65.0).limit_length(1.0)
					if is_instance_valid(joystick_knob):
						joystick_knob.position = Vector2(47.0, 47.0) + player.joy_input * 42.0
				else:
					joy_center = event.position
					player.joy_input = Vector2.ZERO
			# 3. Right side above action buttons -> Camera Orbit Finger
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
			player.joy_input = (delta_v / 65.0).limit_length(1.0)
			if is_instance_valid(joystick_knob):
				joystick_knob.position = Vector2(47.0, 47.0) + player.joy_input * 42.0
		elif event.index == cam_touch_id:
			player.camera_yaw_deg = clampf(player.camera_yaw_deg - event.relative.x * 0.24, -120.0, 120.0)

func _process(delta: float) -> void:
	# Smooth visual bounce recovery on action buttons
	for ab in [jump_btn, dive_btn, dash_btn]:
		if is_instance_valid(ab):
			ab.scale = ab.scale.lerp(Vector2.ONE, delta * 14.0)

	# Update 3D FX bursts
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
	speed_multiplier = clampf(1.0 + elapsed_sec * 0.012 + float(survival_tier - 1) * 0.06, 1.0, 2.0)

	if checkpoint_banner_timer > 0.0:
		checkpoint_banner_timer = maxf(0.0, checkpoint_banner_timer - delta)

	for mp in moving_platforms:
		var body: AnimatableBody3D = mp["node"]
		body.position.x = float(mp["base_x"]) + sin(elapsed_sec * float(mp["speed"])) * float(mp["amp_x"])

	# Update Falling / Blinking Golden Tiles (Stage 4+)
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

	# Update Sideways Sliding Push-Blocks (Stage 4+)
	for pb in push_blocks:
		var node: Node3D = pb["node"]
		var prev_x: float = node.position.x
		node.position.x = float(pb["base_x"]) + sin(elapsed_sec * float(pb["speed"]) + float(pb["phase"])) * float(pb["amp_x"])
		var vx: float = (node.position.x - prev_x) / maxf(delta, 0.001)
		if player.global_position.distance_to(node.global_position) < 1.55:
			player.velocity.x = signf(vx) * 9.5
			player.velocity.y = 4.6

	# Update Rolling Giant Mythoria Boulders (Stage 5+)
	for rb in rolling_boulders:
		var bnode: Node3D = rb["node"]
		bnode.position.z += float(rb["speed"]) * delta
		bnode.rotation.x += float(rb["speed"]) * delta * 0.95
		if bnode.position.z > float(rb["max_z"]):
			bnode.position.z = float(rb["min_z"])
			bnode.position.x = -float(rb["base_x"])
			rb["base_x"] = bnode.position.x
		if player.global_position.distance_to(bnode.global_position) < 1.55:
			player.velocity.z = 9.0
			player.velocity.y = 6.2
			spawn_fx_burst(bnode.global_position, Color(1.0, 0.45, 0.12))

	for h in hammers:
		var node: Node3D = h["node"]
		node.rotation.y += float(h["speed"]) * speed_multiplier * delta
		if player.global_position.distance_to(node.global_position) < 2.2 and absf(player.global_position.y - node.global_position.y) < 1.2:
			player.velocity.z = 7.5
			player.velocity.y = 5.5
		for r in rivals:
			if r.global_position.distance_to(node.global_position) < 2.2 and absf(r.global_position.y - node.global_position.y) < 1.2:
				r.velocity.z = 7.2
				r.velocity.y = 5.2

	for p in pendulums:
		var piv: Node3D = p["pivot"]
		var head_mesh: Node3D = p["head"]
		var spd: float = float(p["speed"])
		var ph: float = float(p["phase"])
		piv.rotation.z = sin(elapsed_sec * spd + ph) * deg_to_rad(52.0)
		if player.global_position.distance_to(head_mesh.global_position) < 1.45:
			player.velocity.x = signf(sin(elapsed_sec * spd + ph)) * 8.5
			player.velocity.y = 5.8

	for bp in bounce_pads:
		if is_instance_valid(bp) and player.global_position.distance_to(bp.global_position) < 1.35:
			player.velocity.y = player.jump_velocity * 1.42
			AudioSynth.play_sfx("jump")
			spawn_fx_burst(bp.global_position + Vector3(0.0, 0.2, 0.0), Color(1.0, 0.22, 0.58))

	for sp in speed_pads:
		if is_instance_valid(sp) and player.global_position.distance_to(sp.global_position) < 1.55:
			player.velocity.z = minf(player.velocity.z, -player.dash_speed * 0.92)

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
	hud_level_label.text = "👑 W%d S%d [%s] | CP %d/%d" % [world_id, level_number, str(stage_profile["tier_name"]), player.last_activated_checkpoint, total_checkpoints]
	hud_timer_label.text = "⏱ %02d.%02ds / Goal <= %ds" % [int(elapsed_sec), int(fmod(elapsed_sec * 100.0, 100.0)), int(goal_ms / 1000)]
	hud_timer_label.modulate = Color(0.25, 0.98, 0.62) if elapsed_ms <= goal_ms else Color(1.0, 0.40, 0.48)
	hud_coins_label.text = "🪙 %d  |  ⚡ %.2fx" % [coins_collected, speed_multiplier]

	if checkpoint_banner_timer > 0.0:
		hud_goal_banner.text = checkpoint_banner_text
		hud_goal_banner.modulate = Color(0.0, 0.94, 1.0)
	else:
		var goal_sec: int = int(goal_ms / 1000)
		hud_goal_banner.text = "GOAL <= %ds • %s" % [goal_sec, str(stage_profile["new_obstacle"])]
		hud_goal_banner.modulate = Color(1.0, 0.86, 0.22)

	if player.global_position.y < void_floor_y or absf(player.global_position.x) > 45.0:
		player.respawn_at_checkpoint()

	if player.global_position.z <= finish_z + 2.2:
		complete_level(elapsed_ms)

func _update_ai_rivals(delta: float) -> void:
	for r in rivals:
		if r.finished:
			continue
		var target_x: float = r.lane_bias_x * sin(elapsed_sec * 0.9 + float(r.get_index()))
		var dx: float = target_x - r.global_position.x
		r.velocity.x = move_toward(r.velocity.x, clampf(dx * 3.5, -4.0, 4.0), 18.0 * delta)
		r.velocity.z = move_toward(r.velocity.z, -r.move_speed, 18.0 * delta)

		if r.is_on_floor() and fmod(elapsed_sec + float(r.get_index()) * 0.47, 1.8) < 0.08:
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
		result_title.text = "🌍 NEW WORLD %d UNLOCKED!" % int(outcome["newly_unlocked_world"])
	elif bool(outcome["goal_met"]):
		result_title.text = "🏆 STAGE COMPLETED — GOAL MET!"
	else:
		result_title.text = "🏁 FINISHED (GOAL TIME MISSED)"

	result_details.text = "Time: %.2fs (Goal <= %.0fs)  |  Stars: ★%d  |  +%d Coins" % [
		run_sec, goal_sec, int(outcome["stars"]), int(outcome["reward_coins"])
	]

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
