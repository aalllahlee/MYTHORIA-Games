extends Node3D

@onready var player: CharacterBody3D = $Player
@onready var camera_rig: Node3D = $CameraRig
@onready var camera_3d: Camera3D = $CameraRig/Camera3D
@onready var track_root: Node3D = $TrackRoot
@onready var hud_level_label: Label = $HUD/TopBar/LevelLabel
@onready var hud_timer_label: Label = $HUD/TopBar/TimerLabel
@onready var hud_coins_label: Label = $HUD/TopBar/CoinsLabel
@onready var hud_goal_banner: Label = $HUD/GoalBanner
@onready var result_panel: Panel = $HUD/ResultPanel
@onready var result_title: Label = $HUD/ResultPanel/VBox/ResultTitle
@onready var result_details: Label = $HUD/ResultPanel/VBox/ResultDetails
@onready var next_btn: Button = $HUD/ResultPanel/VBox/ButtonsRow/NextButton

var level_number: int = 1
var world_id: int = 1
var finish_z: float = -75.0
var elapsed_sec: float = 0.0
var coins_collected: int = 0
var run_active: bool = true
var survival_tier: int = 1
var speed_multiplier: float = 1.0
var hammers: Array = []
var coin_nodes: Array = []
var joy_touch_id: int = -1
var joy_center: Vector2 = Vector2(160.0, 560.0)
var can_advance_next: bool = false

func _ready() -> void:
	level_number = GameState.current_level_number
	world_id = int((level_number - 1) / 5) + 1
	result_panel.visible = false
	build_3d_course()

func build_3d_course() -> void:
	var w_theme: Dictionary = GameState.get_world_theme(world_id)
	var prim_col: Color = w_theme["primary"]
	var sec_col: Color = w_theme["secondary"]
	var acc_col: Color = w_theme["accent"]

	# Start Island with Twin Golden Fire Braziers
	spawn_platform(Vector3(0.0, 0.0, 0.0), Vector3(8.0, 1.4, 8.0), prim_col)
	spawn_platform(Vector3(-3.2, 1.1, -2.5), Vector3(0.8, 0.9, 0.8), Color(1.0, 0.82, 0.18))
	spawn_platform(Vector3(3.2, 1.1, -2.5), Vector3(0.8, 0.9, 0.8), Color(1.0, 0.82, 0.18))

	var stage_in_world: int = ((level_number - 1) % 5) + 1
	var seg_count: int = 5 + stage_in_world + int(world_id / 3)
	var cur_z: float = -7.5

	for seg in range(seg_count):
		var col: Color = prim_col if (seg % 2 == 0) else sec_col
		spawn_platform(Vector3(0.0, 0.0, cur_z - 4.5), Vector3(7.0, 1.2, 9.0), col)
		spawn_coin(Vector3(0.0, 1.5, cur_z - 4.5))
		if seg % 2 == 1:
			spawn_rotating_hammer(Vector3(0.0, 1.2, cur_z - 4.5), acc_col, 1.8 + float(level_number) * 0.04)
		cur_z -= 11.2

	finish_z = cur_z - 4.5
	spawn_platform(Vector3(0.0, 0.0, finish_z), Vector3(10.0, 1.5, 9.0), acc_col)
	spawn_finish_arch(Vector3(0.0, 0.75, finish_z), acc_col)

	var goal_sec: int = int(GameState.goal_time_ms_for_level(level_number) / 1000)
	hud_level_label.text = "W%d STAGE %d" % [world_id, level_number]
	hud_goal_banner.text = "PROGRESSION GOAL: Reach Finish within <= %ds to unlock next stage/world!" % goal_sec

func spawn_platform(pos: Vector3, sz: Vector3, col: Color) -> void:
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
	var mat := StandardMaterial3D.new()
	mat.albedo_color = col
	mat.roughness = 0.5
	mesh.material = mat
	mesh_inst.mesh = mesh
	body.add_child(mesh_inst)
	track_root.add_child(body)

func spawn_rotating_hammer(pos: Vector3, col: Color, spd: float) -> void:
	var pivot := Node3D.new()
	pivot.position = pos
	var bar := MeshInstance3D.new()
	var bar_mesh := BoxMesh.new()
	bar_mesh.size = Vector3(5.6, 0.55, 0.55)
	var mat := StandardMaterial3D.new()
	mat.albedo_color = col
	bar_mesh.material = mat
	bar.mesh = bar_mesh
	pivot.add_child(bar)
	track_root.add_child(pivot)
	hammers.append({"node": pivot, "speed": spd})

func spawn_coin(pos: Vector3) -> void:
	var c := MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.42
	cyl.bottom_radius = 0.42
	cyl.height = 0.16
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(1.0, 0.84, 0.16)
	mat.metallic = 0.8
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
		p_mesh.top_radius = 0.38
		p_mesh.bottom_radius = 0.38
		p_mesh.height = 4.8
		var mat := StandardMaterial3D.new()
		mat.albedo_color = Color(0.95, 0.97, 1.0)
		p_mesh.material = mat
		pillar.mesh = p_mesh
		pillar.position = pos + Vector3(side, 2.4, 0.0)
		track_root.add_child(pillar)

	# Top Golden Temple Architrave & Glowing Blue Sapphire Portal Core
	var beam := MeshInstance3D.new()
	var b_mesh := BoxMesh.new()
	b_mesh.size = Vector3(8.2, 0.85, 0.85)
	var b_mat := StandardMaterial3D.new()
	b_mat.albedo_color = col
	b_mat.metallic = 0.75
	b_mesh.material = b_mat
	beam.mesh = b_mesh
	beam.position = pos + Vector3(0.0, 5.0, 0.0)
	track_root.add_child(beam)

	var portal := MeshInstance3D.new()
	var port_mesh := BoxMesh.new()
	port_mesh.size = Vector3(5.8, 4.2, 0.14)
	var port_mat := StandardMaterial3D.new()
	port_mat.albedo_color = Color(0.0, 0.88, 1.0, 0.55)
	port_mat.emission_enabled = true
	port_mat.emission = Color(0.0, 0.75, 1.0)
	port_mesh.material = port_mat
	portal.mesh = port_mesh
	portal.position = pos + Vector3(0.0, 2.3, 0.0)
	track_root.add_child(portal)

func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed and event.position.x < 520.0 and event.position.y > 320.0:
			joy_touch_id = event.index
			joy_center = event.position
		elif not event.pressed and event.index == joy_touch_id:
			joy_touch_id = -1
			player.joy_input = Vector2.ZERO
	elif event is InputEventScreenDrag:
		if event.index == joy_touch_id:
			var delta_v: Vector2 = event.position - joy_center
			player.joy_input = (delta_v / 75.0).limit_length(1.0)
		elif event.position.x >= 520.0 and event.position.y < 420.0:
			player.camera_yaw_deg = clampf(player.camera_yaw_deg - event.relative.x * 0.22, -110.0, 110.0)

func _process(delta: float) -> void:
	if not run_active:
		return

	elapsed_sec += delta
	survival_tier = 1 + int(elapsed_sec / 18.0)
	speed_multiplier = clampf(1.0 + elapsed_sec * 0.015 + float(survival_tier - 1) * 0.08, 1.0, 2.2)

	# Rotate obstacle hammers and check knockback
	for h in hammers:
		var node: Node3D = h["node"]
		node.rotation.y += float(h["speed"]) * speed_multiplier * delta
		if player.global_position.distance_to(node.global_position) < 2.2 and absf(player.global_position.y - node.global_position.y) < 1.1:
			player.velocity.z = 7.5
			player.velocity.y = 5.5

	# Collect coins
	for c in coin_nodes:
		if is_instance_valid(c) and c.visible:
			c.rotation.y += delta * 3.2
			if player.global_position.distance_to(c.global_position) < 1.35:
				c.visible = false
				coins_collected += 1
				AudioSynth.play_sfx("coin")

	# Follow camera smoothly
	var target_cam_pos: Vector3 = player.global_position + Vector3(0.0, 4.8, 8.2).rotated(Vector3.UP, deg_to_rad(player.camera_yaw_deg))
	camera_rig.global_position = camera_rig.global_position.lerp(target_cam_pos, delta * 8.0)
	camera_3d.look_at(player.global_position + Vector3(0.0, 1.1, -1.5), Vector3.UP)

	# Update HUD timer & goal pace
	var elapsed_ms: int = int(elapsed_sec * 1000.0)
	var goal_ms: int = GameState.goal_time_ms_for_level(level_number)
	hud_timer_label.text = "%02d.%02ds / Goal <= %ds" % [int(elapsed_sec), int(fmod(elapsed_sec * 100.0, 100.0)), int(goal_ms / 1000)]
	hud_timer_label.modulate = Color(0.25, 0.95, 0.62) if elapsed_ms <= goal_ms else Color(1.0, 0.35, 0.45)
	hud_coins_label.text = "COINS: %d  |  SPEED: %.2fx" % [coins_collected, speed_multiplier]

	# Check fall into void
	if player.global_position.y < -7.5:
		AudioSynth.play_sfx("fail")
		player.respawn_at_checkpoint()

	# Check finish arch
	if player.global_position.z <= finish_z + 2.2:
		complete_level(elapsed_ms)

func complete_level(elapsed_ms: int) -> void:
	run_active = false
	AudioSynth.play_sfx("victory")
	var outcome: Dictionary = GameState.record_level_win(level_number, elapsed_ms, coins_collected, 1)
	can_advance_next = bool(outcome["can_advance"])
	result_panel.visible = true
	next_btn.visible = can_advance_next

	var goal_sec: float = float(outcome["goal_ms"]) / 1000.0
	var run_sec: float = float(elapsed_ms) / 1000.0
	if int(outcome["newly_unlocked_world"]) > 0:
		result_title.text = "NEW WORLD %d UNLOCKED!" % int(outcome["newly_unlocked_world"])
	elif bool(outcome["goal_met"]):
		result_title.text = "STAGE COMPLETED — GOAL MET!"
	else:
		result_title.text = "FINISHED (GOAL TIME MISSED)"

	result_details.text = "Time: %.2fs (Goal <= %.0fs)  |  Stars: ★%d  |  +%d Coins" % [
		run_sec, goal_sec, int(outcome["stars"]), int(outcome["reward_coins"])
	]

func _on_jump_button_pressed() -> void:
	player.request_jump()

func _on_dive_button_pressed() -> void:
	player.request_dive()

func _on_dash_button_pressed() -> void:
	player.request_dash()

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
