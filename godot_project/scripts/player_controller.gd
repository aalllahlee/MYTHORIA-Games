extends CharacterBody3D

const RUN_SPEED: float = 8.4
const JUMP_VELOCITY: float = 11.2
const DIVE_SPEED: float = 13.5
const DASH_SPEED: float = 18.0
const GRAVITY: float = 25.0

var joy_input: Vector2 = Vector2.ZERO
var camera_yaw_deg: float = 0.0
var coyote_timer: float = 0.0
var jump_buffer_timer: float = 0.0
var dive_timer: float = 0.0
var dive_cooldown: float = 0.0
var dash_cooldown: float = 0.0
var spawn_pos: Vector3 = Vector3(0.0, 2.0, 1.5)
var visual_root: Node3D

func _ready() -> void:
	build_stylized_hero_mesh()

func build_stylized_hero_mesh() -> void:
	visual_root = Node3D.new()
	visual_root.name = "VisualRoot"
	add_child(visual_root)

	var skin: Dictionary = GameState.get_skin_def(GameState.selected_skin_id)
	var hoodie_col: Color = skin.get("hoodie", Color(0.9, 0.18, 0.3))
	var pants_col: Color = skin.get("pants", Color(0.12, 0.16, 0.28))

	# Torso (Hoodie)
	var torso := MeshInstance3D.new()
	var torso_mesh := CapsuleMesh.new()
	torso_mesh.radius = 0.34
	torso_mesh.height = 0.92
	var torso_mat := StandardMaterial3D.new()
	torso_mat.albedo_color = hoodie_col
	torso_mat.roughness = 0.45
	torso_mesh.material = torso_mat
	torso.mesh = torso_mesh
	torso.position = Vector3(0.0, 0.72, 0.0)
	visual_root.add_child(torso)

	# Head
	var head := MeshInstance3D.new()
	var head_mesh := SphereMesh.new()
	head_mesh.radius = 0.31
	head_mesh.height = 0.62
	var head_mat := StandardMaterial3D.new()
	head_mat.albedo_color = Color(1.0, 0.8, 0.66)
	head_mesh.material = head_mat
	head.mesh = head_mesh
	head.position = Vector3(0.0, 1.38, 0.0)
	visual_root.add_child(head)

	# Golden Crown Emblem on Top
	var crown := MeshInstance3D.new()
	var crown_mesh := CylinderMesh.new()
	crown_mesh.top_radius = 0.22
	crown_mesh.bottom_radius = 0.18
	crown_mesh.height = 0.16
	var crown_mat := StandardMaterial3D.new()
	crown_mat.albedo_color = Color(1.0, 0.82, 0.16)
	crown_mat.metallic = 0.75
	crown_mat.roughness = 0.22
	crown_mesh.material = crown_mat
	crown.mesh = crown_mesh
	crown.position = Vector3(0.0, 1.72, 0.0)
	visual_root.add_child(crown)

	# Legs
	for side in [-1.0, 1.0]:
		var leg := MeshInstance3D.new()
		var leg_mesh := BoxMesh.new()
		leg_mesh.size = Vector3(0.22, 0.42, 0.24)
		var leg_mat := StandardMaterial3D.new()
		leg_mat.albedo_color = pants_col
		leg_mesh.material = leg_mat
		leg.mesh = leg_mesh
		leg.position = Vector3(side * 0.15, 0.21, 0.0)
		visual_root.add_child(leg)

func request_jump() -> void:
	jump_buffer_timer = 0.15

func request_dive() -> void:
	if dive_timer <= 0.0 and dive_cooldown <= 0.0:
		dive_timer = 0.40
		dive_cooldown = 0.68
		var forward_dir: Vector3 = -global_transform.basis.z.normalized()
		velocity.x = forward_dir.x * DIVE_SPEED
		velocity.z = forward_dir.z * DIVE_SPEED
		velocity.y = maxf(velocity.y, 4.2)
		AudioSynth.play_sfx("dive")

func request_dash() -> void:
	if dash_cooldown <= 0.0:
		dash_cooldown = 2.2
		var forward_dir: Vector3 = -global_transform.basis.z.normalized()
		velocity.x = forward_dir.x * DASH_SPEED
		velocity.z = forward_dir.z * DASH_SPEED
		velocity.y = maxf(velocity.y, 2.0)
		AudioSynth.play_sfx("dash")

func respawn_at_checkpoint() -> void:
	global_position = spawn_pos
	velocity = Vector3.ZERO
	dive_timer = 0.0

func _physics_process(delta: float) -> void:
	if is_on_floor():
		coyote_timer = 0.14
	else:
		coyote_timer = maxf(0.0, coyote_timer - delta)
		velocity.y -= GRAVITY * delta

	if jump_buffer_timer > 0.0:
		jump_buffer_timer = maxf(0.0, jump_buffer_timer - delta)
		if coyote_timer > 0.0:
			velocity.y = JUMP_VELOCITY
			coyote_timer = 0.0
			jump_buffer_timer = 0.0
			AudioSynth.play_sfx("jump")

	if dive_cooldown > 0.0:
		dive_cooldown = maxf(0.0, dive_cooldown - delta)
	if dash_cooldown > 0.0:
		dash_cooldown = maxf(0.0, dash_cooldown - delta)

	if dive_timer > 0.0:
		dive_timer = maxf(0.0, dive_timer - delta)
	else:
		var kb_vec: Vector2 = Input.get_vector("ui_left", "ui_right", "ui_up", "ui_down")
		var combined: Vector2 = joy_input if joy_input.length() > 0.08 else kb_vec
		var yaw_rad: float = deg_to_rad(camera_yaw_deg)
		var move_dir: Vector3 = Vector3(combined.x, 0.0, combined.y).rotated(Vector3.UP, yaw_rad)
		if move_dir.length() > 0.05:
			move_dir = move_dir.normalized()
			velocity.x = lerpf(velocity.x, move_dir.x * RUN_SPEED, delta * 10.0)
			velocity.z = lerpf(velocity.z, move_dir.z * RUN_SPEED, delta * 10.0)
			var target_angle: float = atan2(-move_dir.x, -move_dir.z)
			rotation.y = lerp_angle(rotation.y, target_angle, delta * 12.0)
		else:
			velocity.x = move_toward(velocity.x, 0.0, RUN_SPEED * delta * 8.0)
			velocity.z = move_toward(velocity.z, 0.0, RUN_SPEED * delta * 8.0)

	move_and_slide()
