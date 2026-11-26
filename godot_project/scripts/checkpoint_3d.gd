extends Area3D

## MYTHORIA games — By ahmed
## 3D Mythoria Sanctuary Checkpoint Gate:
## Ancient marble pedestal, twin rune columns, rotating glowing sapphire crystal,
## golden rings, and per-player independent activation trigger.

const CVB := preload("res://scripts/character_visual_builder.gd")

@export var checkpoint_index: int = 1
@export var safe_yaw_deg: float = 180.0

var activated_by_local_player: bool = false
var anim_time: float = 0.0
var crystal_mesh: MeshInstance3D = null
var ring_mesh: MeshInstance3D = null
var beam_mesh: MeshInstance3D = null

func _ready() -> void:
	monitoring = true
	monitorable = true
	_build_checkpoint_3d_visuals()
	body_entered.connect(_on_body_entered)

func _build_checkpoint_3d_visuals() -> void:
	var col := CollisionShape3D.new()
	var box := BoxShape3D.new()
	box.size = Vector3(6.8, 4.2, 3.2)
	col.shape = box
	col.position = Vector3(0.0, 2.0, 0.0)
	add_child(col)

	var mat_marble := CVB.make_mat(Color(0.95, 0.96, 1.0), 0.35, 0.08)
	var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.22, 0.85, Color(1.0, 0.75, 0.12), 0.30)
	var mat_sapphire := CVB.make_mat(Color(0.0, 0.88, 1.0), 0.15, 0.30, Color(0.0, 0.78, 1.0), 0.55)
	var mat_beam := CVB.make_mat(Color(0.0, 0.88, 1.0, 0.28), 0.20, 0.10, Color(0.0, 0.80, 1.0), 0.45)

	ring_mesh = MeshInstance3D.new()
	var r_cyl := CylinderMesh.new()
	r_cyl.top_radius = 1.65
	r_cyl.bottom_radius = 1.75
	r_cyl.height = 0.10
	r_cyl.material = mat_gold
	ring_mesh.mesh = r_cyl
	ring_mesh.position = Vector3(0.0, 0.06, 0.0)
	add_child(ring_mesh)

	for side_dir in [-1, 1]:
		var px: float = float(side_dir) * 2.65
		var base := MeshInstance3D.new()
		var b_box := BoxMesh.new()
		b_box.size = Vector3(0.76, 0.45, 0.76)
		b_box.material = mat_gold
		base.mesh = b_box
		base.position = Vector3(px, 0.22, 0.0)
		add_child(base)

		var pillar := MeshInstance3D.new()
		var p_cyl := CylinderMesh.new()
		p_cyl.top_radius = 0.28
		p_cyl.bottom_radius = 0.32
		p_cyl.height = 3.0
		p_cyl.material = mat_marble
		pillar.mesh = p_cyl
		pillar.position = Vector3(px, 1.85, 0.0)
		add_child(pillar)

		var orb := MeshInstance3D.new()
		var o_sph := SphereMesh.new()
		o_sph.radius = 0.32
		o_sph.height = 0.64
		o_sph.material = mat_sapphire
		orb.mesh = o_sph
		orb.position = Vector3(px, 3.65, 0.0)
		add_child(orb)

	crystal_mesh = MeshInstance3D.new()
	var c_box := BoxMesh.new()
	c_box.size = Vector3(0.58, 0.88, 0.58)
	c_box.material = mat_sapphire
	crystal_mesh.mesh = c_box
	crystal_mesh.position = Vector3(0.0, 2.85, 0.0)
	crystal_mesh.rotation = Vector3(deg_to_rad(35.0), 0.0, deg_to_rad(35.0))
	add_child(crystal_mesh)

	beam_mesh = MeshInstance3D.new()
	var bm_cyl := CylinderMesh.new()
	bm_cyl.top_radius = 1.25
	bm_cyl.bottom_radius = 1.25
	bm_cyl.height = 3.4
	bm_cyl.material = mat_beam
	beam_mesh.mesh = bm_cyl
	beam_mesh.position = Vector3(0.0, 1.75, 0.0)
	add_child(beam_mesh)

func _process(delta: float) -> void:
	anim_time += delta
	if is_instance_valid(crystal_mesh):
		var rot_spd: float = 3.6 if activated_by_local_player else 1.5
		crystal_mesh.rotation.y += delta * rot_spd
		crystal_mesh.position.y = 2.85 + sin(anim_time * 3.0) * 0.18
	if is_instance_valid(ring_mesh):
		var pulse: float = 1.0 + sin(anim_time * 4.0) * 0.06
		ring_mesh.scale = Vector3(pulse, 1.0, pulse)

func _on_body_entered(body: Node3D) -> void:
	if body != null and body.has_method("try_activate_checkpoint"):
		var safe_pos: Vector3 = global_position + Vector3(0.0, 1.15, 0.0)
		var did_activate: bool = bool(body.call("try_activate_checkpoint", checkpoint_index, safe_pos, safe_yaw_deg))
		if did_activate and str(body.get("player_id")) == "player_0":
			activated_by_local_player = true
			if is_instance_valid(beam_mesh):
				beam_mesh.material_override = CVB.make_mat(
					Color(1.0, 0.85, 0.18, 0.38),
					0.15,
					0.30,
					Color(1.0, 0.82, 0.16),
					0.55
				)
