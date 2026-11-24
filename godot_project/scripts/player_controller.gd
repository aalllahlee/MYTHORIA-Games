extends CharacterBody3D

## MYTHORIA games — By ahmed
## DON'T FALL 3D Character Controller with Full 3D Stylized Cartoon Character Mesh,
## Double Jump, Coyote Time, Jump Buffer, Dynamic Ground Contact Shadow,
## Procedural Limb Animation, and Independent Checkpoint & Portal Respawn State.

const CVB := preload("res://scripts/character_visual_builder.gd")

signal checkpoint_reached(player_id: String, checkpoint_index: int)
signal individual_respawn_started(player_id: String, checkpoint_index: int, respawn_pos: Vector3)
signal individual_respawn_completed(player_id: String, checkpoint_index: int)
signal player_jumped(player_id: String, jump_pos: Vector3, is_double_jump: bool)

@export var player_id: String = "player_0"
@export var is_ai_racer: bool = false
@export var character_skin_id: int = 0
@export var move_speed: float = 8.8
@export var jump_velocity: float = 11.8
@export var dive_speed: float = 13.0
@export var dash_speed: float = 17.5
@export var gravity_base: float = 28.0
@export var lane_bias_x: float = 0.0

var joy_input: Vector2 = Vector2.ZERO
var camera_yaw_deg: float = 0.0
var coyote_timer: float = 0.0
var jump_buffer_timer: float = 0.0
var jumps_remaining: int = 2
var dive_timer: float = 0.0
var dive_cooldown: float = 0.0
var dash_cooldown: float = 0.0
var hit_stun_timer: float = 0.0
var anim_clock: float = 0.0

# Independent Per-Player Checkpoint & Respawn State
var last_activated_checkpoint: int = 0
var activated_checkpoints: Dictionary = {0: true}
var safe_respawn_pos: Vector3 = Vector3(0.0, 1.8, 1.5)
var safe_respawn_yaw_deg: float = 180.0
var is_respawning: bool = false
var respawn_timer_sec: float = 0.0
var fall_anim_timer_sec: float = 0.0
var arrival_anim_timer_sec: float = 0.0
var portal_scale: float = 0.0
var race_progress: float = 0.0
var finished: bool = false
var respawn_count: int = 0

var portal_ring_mesh: MeshInstance3D = null
var blob_shadow: MeshInstance3D = null
var visual_root: Node3D = null

func _ready() -> void:
	safe_respawn_pos = global_position
	safe_respawn_yaw_deg = 180.0
	if not is_ai_racer:
		character_skin_id = GameState.selected_skin_id
	_ensure_character_visual()
	_create_portal_ring_visual()
	_create_blob_shadow()

func _ensure_character_visual() -> void:
	var existing: Node3D = get_node_or_null("VisualRoot")
	if is_instance_valid(existing):
		existing.queue_free()
	visual_root = CVB.build_character_visual(character_skin_id)
	visual_root.rotation.y = deg_to_rad(safe_respawn_yaw_deg)
	add_child(visual_root)

func _create_blob_shadow() -> void:
	var existing: Node = get_node_or_null("BlobShadow")
	if is_instance_valid(existing):
		existing.queue_free()
	blob_shadow = CVB.build_blob_shadow()
	blob_shadow.position = Vector3(0.0, 0.04, 0.0)
	add_child(blob_shadow)

func _create_portal_ring_visual() -> void:
	portal_ring_mesh = MeshInstance3D.new()
	var cyl := CylinderMesh.new()
	cyl.top_radius = 1.25
	cyl.bottom_radius = 1.25
	cyl.height = 0.10
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.0, 0.90, 1.0, 0.85)
	mat.emission_enabled = true
	mat.emission = Color(1.0, 0.82, 0.17)
	mat.emission_energy_multiplier = 2.0
	cyl.material = mat
	portal_ring_mesh.mesh = cyl
	portal_ring_mesh.position = Vector3(0.0, 0.1, 0.0)
	portal_ring_mesh.visible = false
	add_child(portal_ring_mesh)

func initialize_spawn(start_pos: Vector3, start_yaw_deg: float = 180.0) -> void:
	global_position = start_pos
	velocity = Vector3.ZERO
	last_activated_checkpoint = 0
	activated_checkpoints = {0: true}
	safe_respawn_pos = start_pos
	safe_respawn_yaw_deg = start_yaw_deg
	is_respawning = false
	respawn_timer_sec = 0.0
	fall_anim_timer_sec = 0.0
	arrival_anim_timer_sec = 0.0
	portal_scale = 0.0
	jumps_remaining = 2
	finished = false
	respawn_count = 0
	if is_instance_valid(visual_root):
		visual_root.rotation.y = deg_to_rad(start_yaw_deg)

func try_activate_checkpoint(cp_index: int, cp_safe_pos: Vector3, cp_safe_yaw_deg: float = 180.0) -> bool:
	if cp_index != last_activated_checkpoint + 1:
		return false
	last_activated_checkpoint = cp_index
	activated_checkpoints[cp_index] = true
	safe_respawn_pos = cp_safe_pos + Vector3(lane_bias_x * 0.35, 0.0, 0.0)
	safe_respawn_yaw_deg = cp_safe_yaw_deg
	if player_id == "player_0":
		AudioSynth.play_sfx("checkpoint")
	checkpoint_reached.emit(player_id, cp_index)
	return true

func request_jump() -> void:
	jump_buffer_timer = 0.18

func request_dive() -> void:
	if dive_timer > 0.0 or dive_cooldown > 0.0 or hit_stun_timer > 0.0:
		return
	var yaw_rad: float = visual_root.rotation.y if is_instance_valid(visual_root) else PI
	var fwd := Vector3(sin(yaw_rad), 0.0, cos(yaw_rad))
	velocity.x = fwd.x * dive_speed
	velocity.z = fwd.z * dive_speed
	velocity.y = 4.6 if is_on_floor() else maxf(velocity.y, 2.8)
	dive_timer = 0.42
	dive_cooldown = 0.65
	if player_id == "player_0":
		AudioSynth.play_sfx("dive")

func request_dash() -> void:
	if not GameState.dash_unlocked or dash_cooldown > 0.0 or hit_stun_timer > 0.0:
		return
	var yaw_rad: float = visual_root.rotation.y if is_instance_valid(visual_root) else PI
	velocity.x = sin(yaw_rad) * dash_speed
	velocity.z = cos(yaw_rad) * dash_speed
	velocity.y = maxf(velocity.y, 1.8)
	dash_cooldown = 2.2
	if player_id == "player_0":
		AudioSynth.play_sfx("dash")

func respawn_at_checkpoint() -> void:
	global_position = safe_respawn_pos
	velocity = Vector3.ZERO
	dive_timer = 0.0
	hit_stun_timer = 0.0
	jumps_remaining = 2
	is_respawning = true
	respawn_timer_sec = 1.05
	fall_anim_timer_sec = 0.25
	arrival_anim_timer_sec = 0.80
	portal_scale = 1.0
	respawn_count += 1
	if is_instance_valid(visual_root):
		visual_root.rotation.y = deg_to_rad(safe_respawn_yaw_deg)
		visual_root.scale = Vector3(0.85, 1.24, 0.85)
	if is_instance_valid(portal_ring_mesh):
		portal_ring_mesh.visible = true
		portal_ring_mesh.scale = Vector3.ONE
	if player_id == "player_0":
		AudioSynth.play_sfx("checkpoint")
	individual_respawn_started.emit(player_id, last_activated_checkpoint, safe_respawn_pos)

func _physics_process(delta: float) -> void:
	anim_clock += delta

	if is_respawning:
		respawn_timer_sec = maxf(0.0, respawn_timer_sec - delta)
		fall_anim_timer_sec = maxf(0.0, fall_anim_timer_sec - delta)
		arrival_anim_timer_sec = maxf(0.0, arrival_anim_timer_sec - delta)
		portal_scale = clampf(respawn_timer_sec / 1.05, 0.0, 1.0)
		if is_instance_valid(portal_ring_mesh):
			portal_ring_mesh.visible = portal_scale > 0.05
			portal_ring_mesh.scale = Vector3(1.0 + (1.0 - portal_scale) * 0.6, 1.0, 1.0 + (1.0 - portal_scale) * 0.6)
			portal_ring_mesh.rotation.y += delta * 6.0
		if respawn_timer_sec <= 0.0:
			is_respawning = false
			portal_scale = 0.0
			if is_instance_valid(portal_ring_mesh):
				portal_ring_mesh.visible = false
			individual_respawn_completed.emit(player_id, last_activated_checkpoint)

	if finished:
		if is_instance_valid(visual_root):
			visual_root.rotation.y += delta * 1.6
			CVB.animate_character(visual_root, anim_clock, Vector3.ZERO, true, 0.0, true)
		return

	if is_on_floor():
		coyote_timer = 0.18
		jumps_remaining = 2
	else:
		coyote_timer = maxf(0.0, coyote_timer - delta)
		velocity.y -= gravity_base * delta

	jump_buffer_timer = maxf(0.0, jump_buffer_timer - delta)
	dive_timer = maxf(0.0, dive_timer - delta)
	dive_cooldown = maxf(0.0, dive_cooldown - delta)
	dash_cooldown = maxf(0.0, dash_cooldown - delta)
	hit_stun_timer = maxf(0.0, hit_stun_timer - delta)

	if jump_buffer_timer > 0.0 and hit_stun_timer <= 0.0:
		if coyote_timer > 0.0:
			velocity.y = jump_velocity
			coyote_timer = 0.0
			jump_buffer_timer = 0.0
			jumps_remaining = 1
			if is_instance_valid(visual_root):
				visual_root.scale = Vector3(0.84, 1.26, 0.84)
			if player_id == "player_0":
				AudioSynth.play_sfx("jump")
			player_jumped.emit(player_id, global_position, false)
		elif not is_ai_racer and jumps_remaining > 0:
			# Crisp mid-air double jump for forgiving, fun mobile platforming!
			velocity.y = jump_velocity * 0.92
			jump_buffer_timer = 0.0
			jumps_remaining = 0
			if is_instance_valid(visual_root):
				visual_root.scale = Vector3(0.80, 1.30, 0.80)
			if player_id == "player_0":
				AudioSynth.play_sfx("jump")
			player_jumped.emit(player_id, global_position, true)

	if not is_ai_racer:
		var kb := Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
		var raw_in: Vector2 = joy_input if joy_input.length() > 0.08 else kb
		var cam_rad: float = deg_to_rad(camera_yaw_deg)
		var fwd := Vector3(-sin(cam_rad), 0.0, -cos(cam_rad))
		var right := Vector3(cos(cam_rad), 0.0, -sin(cam_rad))
		var wish_dir: Vector3 = (right * raw_in.x + fwd * (-raw_in.y)).normalized()

		if dive_timer <= 0.0 and hit_stun_timer <= 0.0:
			if raw_in.length() > 0.08:
				var accel: float = 40.0 if is_on_floor() else 28.0
				velocity.x = move_toward(velocity.x, wish_dir.x * move_speed, accel * delta)
				velocity.z = move_toward(velocity.z, wish_dir.z * move_speed, accel * delta)
				if is_instance_valid(visual_root):
					var target_yaw: float = atan2(wish_dir.x, wish_dir.z)
					visual_root.rotation.y = lerp_angle(visual_root.rotation.y, target_yaw, 13.0 * delta)
			else:
				var decel: float = 26.0 if is_on_floor() else 10.0
				velocity.x = move_toward(velocity.x, 0.0, decel * delta)
				velocity.z = move_toward(velocity.z, 0.0, decel * delta)
	else:
		if is_instance_valid(visual_root) and Vector2(velocity.x, velocity.z).length() > 0.25:
			var ai_yaw: float = atan2(velocity.x, velocity.z)
			visual_root.rotation.y = lerp_angle(visual_root.rotation.y, ai_yaw, 10.0 * delta)

	if is_instance_valid(blob_shadow):
		var height_above: float = maxf(0.0, global_position.y - 0.72)
		blob_shadow.visible = global_position.y > -1.0
		var s_val: float = clampf(1.0 - height_above * 0.12, 0.35, 1.0)
		blob_shadow.scale = Vector3(s_val, 1.0, s_val)
		blob_shadow.position.y = 0.03 if is_on_floor() else -minf(height_above, 3.0) + 0.05

	if is_instance_valid(visual_root):
		visual_root.scale = visual_root.scale.lerp(Vector3.ONE, 12.0 * delta)
		CVB.animate_character(visual_root, anim_clock, velocity, is_on_floor(), dive_timer, finished)

	move_and_slide()
