extends RefCounted

## MYTHORIA games — By ahmed
## Procedural 3D Stylized Cartoon Character & Environment Visual Builder + UI Theme Helper.
## 100% self-contained with str() conversions and zero external Autoload calls in static functions.

const SKIN_COLORS := [
	{"primary": Color(0.92, 0.18, 0.22), "secondary": Color(1.0, 0.82, 0.18)},
	{"primary": Color(1.0, 0.75, 0.08), "secondary": Color(0.0, 0.86, 1.0)},
	{"primary": Color(0.14, 0.52, 0.98), "secondary": Color(1.0, 0.84, 0.22)},
	{"primary": Color(0.0, 0.78, 0.96), "secondary": Color(0.68, 0.94, 1.0)},
	{"primary": Color(0.88, 0.16, 0.32), "secondary": Color(1.0, 0.84, 0.16)},
	{"primary": Color(0.75, 0.82, 0.92), "secondary": Color(1.0, 0.82, 0.18)},
	{"primary": Color(0.16, 0.18, 0.28), "secondary": Color(0.95, 0.22, 0.28)},
	{"primary": Color(0.12, 0.32, 0.78), "secondary": Color(1.0, 0.84, 0.16)},
	{"primary": Color(0.18, 0.42, 0.92), "secondary": Color(0.0, 0.92, 1.0)},
	{"primary": Color(1.0, 0.58, 0.08), "secondary": Color(1.0, 0.88, 0.28)},
	{"primary": Color(0.38, 0.68, 1.0), "secondary": Color(1.0, 0.86, 0.24)},
	{"primary": Color(0.72, 0.22, 0.88), "secondary": Color(1.0, 0.84, 0.18)},
	{"primary": Color(0.14, 0.76, 0.52), "secondary": Color(1.0, 0.84, 0.18)},
	{"primary": Color(1.0, 0.80, 0.12), "secondary": Color(0.0, 0.88, 1.0)},
	{"primary": Color(0.45, 0.18, 0.92), "secondary": Color(0.0, 0.92, 1.0)}
]

static func make_mat(col: Color, roughness: float = 0.38, metallic: float = 0.08, emission_col: Color = Color(0, 0, 0, 0), emission_energy: float = 0.0) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = roughness
	m.metallic = metallic
	if col.a < 0.99:
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	if emission_energy > 0.0:
		m.emission_enabled = true
		m.emission = emission_col
		m.emission_energy_multiplier = emission_energy
	return m

static func build_character_visual(skin_id: int = 0) -> Node3D:
	var idx: int = clampi(skin_id, 0, SKIN_COLORS.size() - 1)
	var skin: Dictionary = SKIN_COLORS[idx]
	var prim_col: Color = skin["primary"]
	var sec_col: Color = skin["secondary"]

	var root := Node3D.new()
	root.name = "VisualRoot"

	var mat_primary := make_mat(prim_col, 0.35, 0.10)
	var mat_secondary := make_mat(sec_col, 0.30, 0.35)
	var mat_gold := make_mat(Color(1.0, 0.84, 0.18), 0.22, 0.85, Color(1.0, 0.76, 0.12), 0.35)
	var mat_sapphire := make_mat(Color(0.0, 0.88, 1.0), 0.15, 0.30, Color(0.0, 0.78, 1.0), 1.4)
	var mat_skin := make_mat(Color(1.0, 0.83, 0.70), 0.48, 0.02)
	var mat_dark_pants := make_mat(Color(0.14, 0.20, 0.36), 0.50, 0.05)
	var mat_white := make_mat(Color(0.97, 0.98, 1.0), 0.30, 0.05)
	var mat_eye_dark := make_mat(Color(0.08, 0.10, 0.16), 0.15, 0.10)
	var mat_hair := make_mat(Color(0.38, 0.18, 0.08), 0.45, 0.04)

	if idx == 6:
		mat_skin = make_mat(Color(0.16, 0.16, 0.22), 0.40, 0.15)
	elif idx == 3:
		mat_skin = make_mat(Color(0.72, 0.95, 1.0), 0.20, 0.35, Color(0.0, 0.75, 1.0), 0.4)

	# 1. TORSO PIVOT
	var torso_pivot := Node3D.new()
	torso_pivot.name = "TorsoPivot"
	torso_pivot.position = Vector3(0.0, 0.62, 0.0)
	root.add_child(torso_pivot)

	var torso_mesh_inst := MeshInstance3D.new()
	var torso_box := BoxMesh.new()
	torso_box.size = Vector3(0.52, 0.50, 0.34)
	torso_box.material = mat_primary
	torso_mesh_inst.mesh = torso_box
	torso_pivot.add_child(torso_mesh_inst)

	var collar_inst := MeshInstance3D.new()
	var collar_box := BoxMesh.new()
	collar_box.size = Vector3(0.56, 0.11, 0.38)
	collar_box.material = mat_secondary
	collar_inst.mesh = collar_box
	collar_inst.position = Vector3(0.0, 0.22, 0.0)
	torso_pivot.add_child(collar_inst)

	var crest_inst := MeshInstance3D.new()
	var crest_box := BoxMesh.new()
	crest_box.size = Vector3(0.22, 0.16, 0.04)
	crest_box.material = mat_gold
	crest_inst.mesh = crest_box
	crest_inst.position = Vector3(0.0, 0.04, 0.18)
	torso_pivot.add_child(crest_inst)

	var gem_inst := MeshInstance3D.new()
	var gem_sphere := SphereMesh.new()
	gem_sphere.radius = 0.045
	gem_sphere.height = 0.09
	gem_sphere.material = mat_sapphire
	gem_inst.mesh = gem_sphere
	gem_inst.position = Vector3(0.0, 0.04, 0.20)
	torso_pivot.add_child(gem_inst)

	var belt_inst := MeshInstance3D.new()
	var belt_box := BoxMesh.new()
	belt_box.size = Vector3(0.54, 0.08, 0.36)
	belt_box.material = mat_gold
	belt_inst.mesh = belt_box
	belt_inst.position = Vector3(0.0, -0.22, 0.0)
	torso_pivot.add_child(belt_inst)

	if idx in [1, 2, 4, 7, 10, 11, 13]:
		for side_dir in [-1, 1]:
			var s_f: float = float(side_dir)
			var wing_inst := MeshInstance3D.new()
			var wing_box := BoxMesh.new()
			wing_box.size = Vector3(0.38, 0.26, 0.05)
			wing_box.material = mat_gold
			wing_inst.mesh = wing_box
			wing_inst.position = Vector3(s_f * 0.28, 0.10, -0.20)
			wing_inst.rotation.z = deg_to_rad(-s_f * 22.0)
			wing_inst.rotation.y = deg_to_rad(s_f * 18.0)
			torso_pivot.add_child(wing_inst)

	# 2. HEAD PIVOT
	var head_pivot := Node3D.new()
	head_pivot.name = "HeadPivot"
	head_pivot.position = Vector3(0.0, 1.12, 0.0)
	root.add_child(head_pivot)

	var head_inst := MeshInstance3D.new()
	var head_sphere := SphereMesh.new()
	head_sphere.radius = 0.29
	head_sphere.height = 0.56
	head_sphere.material = mat_skin
	head_inst.mesh = head_sphere
	head_pivot.add_child(head_inst)

	for side_dir in [-1, 1]:
		var s_f: float = float(side_dir)
		var eye_white := MeshInstance3D.new()
		var ew_mesh := SphereMesh.new()
		ew_mesh.radius = 0.068
		ew_mesh.height = 0.135
		ew_mesh.material = mat_white
		eye_white.mesh = ew_mesh
		eye_white.position = Vector3(s_f * 0.105, 0.03, 0.245)
		head_pivot.add_child(eye_white)

		var pupil := MeshInstance3D.new()
		var p_mesh := SphereMesh.new()
		p_mesh.radius = 0.038
		p_mesh.height = 0.075
		p_mesh.material = mat_eye_dark
		pupil.mesh = p_mesh
		pupil.position = Vector3(s_f * 0.105, 0.03, 0.288)
		head_pivot.add_child(pupil)

		var highlight := MeshInstance3D.new()
		var h_mesh := SphereMesh.new()
		h_mesh.radius = 0.015
		h_mesh.height = 0.03
		h_mesh.material = mat_white
		highlight.mesh = h_mesh
		highlight.position = Vector3(s_f * 0.118, 0.052, 0.312)
		head_pivot.add_child(highlight)

		var brow := MeshInstance3D.new()
		var b_mesh := BoxMesh.new()
		b_mesh.size = Vector3(0.11, 0.028, 0.03)
		b_mesh.material = mat_hair
		brow.mesh = b_mesh
		brow.position = Vector3(s_f * 0.105, 0.125, 0.255)
		brow.rotation.z = deg_to_rad(-s_f * 8.0)
		head_pivot.add_child(brow)

	if idx in [0, 2, 8, 10]:
		var hair_cap := MeshInstance3D.new()
		var hc_mesh := SphereMesh.new()
		hc_mesh.radius = 0.30
		hc_mesh.height = 0.36
		hc_mesh.material = mat_hair
		hair_cap.mesh = hc_mesh
		hair_cap.position = Vector3(0.0, 0.12, -0.02)
		head_pivot.add_child(hair_cap)

		for tuft_i in range(5):
			var tuft := MeshInstance3D.new()
			var t_mesh := BoxMesh.new()
			t_mesh.size = Vector3(0.13, 0.16, 0.18)
			t_mesh.material = mat_hair
			tuft.mesh = t_mesh
			var angle_offset: float = float(tuft_i - 2) * 0.26
			tuft.position = Vector3(sin(angle_offset) * 0.18, 0.26, 0.12 + cos(angle_offset) * 0.06)
			tuft.rotation = Vector3(deg_to_rad(-22.0), angle_offset, deg_to_rad(float(tuft_i - 2) * -12.0))
			head_pivot.add_child(tuft)

	var crown_band := MeshInstance3D.new()
	var cb_mesh := CylinderMesh.new()
	cb_mesh.top_radius = 0.21
	cb_mesh.bottom_radius = 0.19
	cb_mesh.height = 0.11
	cb_mesh.material = mat_gold
	crown_band.mesh = cb_mesh
	crown_band.position = Vector3(0.0, 0.31, 0.0)
	head_pivot.add_child(crown_band)

	for peak_i in range(5):
		var ang: float = float(peak_i) * TAU / 5.0
		var peak := MeshInstance3D.new()
		var pk_mesh := BoxMesh.new()
		pk_mesh.size = Vector3(0.065, 0.11, 0.04)
		pk_mesh.material = mat_gold
		peak.mesh = pk_mesh
		peak.position = Vector3(sin(ang) * 0.18, 0.39, cos(ang) * 0.18)
		peak.rotation.y = ang
		head_pivot.add_child(peak)

	var crown_gem := MeshInstance3D.new()
	var cg_mesh := SphereMesh.new()
	cg_mesh.radius = 0.042
	cg_mesh.height = 0.084
	cg_mesh.material = mat_sapphire
	crown_gem.mesh = cg_mesh
	crown_gem.position = Vector3(0.0, 0.32, 0.20)
	head_pivot.add_child(crown_gem)

	# 3. LEFT & RIGHT ARMS
	var arm_names := ["LeftArmPivot", "RightArmPivot"]
	var arm_xs := [-0.35, 0.35]
	for a_i in range(2):
		var arm_pivot := Node3D.new()
		arm_pivot.name = arm_names[a_i]
		arm_pivot.position = Vector3(arm_xs[a_i], 0.80, 0.0)
		root.add_child(arm_pivot)

		var sleeve := MeshInstance3D.new()
		var s_mesh := CapsuleMesh.new()
		s_mesh.radius = 0.085
		s_mesh.height = 0.36
		s_mesh.material = mat_primary
		sleeve.mesh = s_mesh
		sleeve.position = Vector3(0.0, -0.14, 0.0)
		arm_pivot.add_child(sleeve)

		var hand := MeshInstance3D.new()
		var h_mesh := SphereMesh.new()
		h_mesh.radius = 0.082
		h_mesh.height = 0.16
		h_mesh.material = mat_skin
		hand.mesh = h_mesh
		hand.position = Vector3(0.0, -0.31, 0.0)
		arm_pivot.add_child(hand)

	# 4. LEFT & RIGHT LEGS + STYLIZED SNEAKERS
	var leg_names := ["LeftLegPivot", "RightLegPivot"]
	var leg_xs := [-0.15, 0.15]
	for l_i in range(2):
		var leg_pivot := Node3D.new()
		leg_pivot.name = leg_names[l_i]
		leg_pivot.position = Vector3(leg_xs[l_i], 0.38, 0.0)
		root.add_child(leg_pivot)

		var pant := MeshInstance3D.new()
		var p_mesh := CapsuleMesh.new()
		p_mesh.radius = 0.095
		p_mesh.height = 0.34
		p_mesh.material = mat_dark_pants
		pant.mesh = p_mesh
		pant.position = Vector3(0.0, -0.14, 0.0)
		leg_pivot.add_child(pant)

		var shoe := MeshInstance3D.new()
		var sh_mesh := BoxMesh.new()
		sh_mesh.size = Vector3(0.19, 0.12, 0.28)
		sh_mesh.material = mat_primary
		shoe.mesh = sh_mesh
		shoe.position = Vector3(0.0, -0.31, 0.04)
		leg_pivot.add_child(shoe)

		var sole := MeshInstance3D.new()
		var so_mesh := BoxMesh.new()
		so_mesh.size = Vector3(0.205, 0.045, 0.30)
		so_mesh.material = mat_white
		sole.mesh = so_mesh
		sole.position = Vector3(0.0, -0.36, 0.04)
		leg_pivot.add_child(sole)

	return root

static func animate_character(
	visual_root: Node3D,
	anim_time: float,
	velocity: Vector3,
	is_on_floor: bool,
	dive_timer: float = 0.0,
	finished: bool = false
) -> void:
	if not is_instance_valid(visual_root):
		return

	var head_pivot: Node3D = visual_root.get_node_or_null("HeadPivot")
	var torso_pivot: Node3D = visual_root.get_node_or_null("TorsoPivot")
	var left_arm: Node3D = visual_root.get_node_or_null("LeftArmPivot")
	var right_arm: Node3D = visual_root.get_node_or_null("RightArmPivot")
	var left_leg: Node3D = visual_root.get_node_or_null("LeftLegPivot")
	var right_leg: Node3D = visual_root.get_node_or_null("RightLegPivot")

	var horiz_speed: float = Vector2(velocity.x, velocity.z).length()

	if finished:
		var bounce: float = absf(sin(anim_time * 7.0)) * 0.12
		if torso_pivot:
			torso_pivot.position.y = 0.62 + bounce
			torso_pivot.rotation.x = 0.0
		if head_pivot:
			head_pivot.position.y = 1.12 + bounce
			head_pivot.rotation.z = sin(anim_time * 5.0) * 0.15
		if left_arm:
			left_arm.rotation.x = deg_to_rad(-145.0 + sin(anim_time * 9.0) * 20.0)
		if right_arm:
			right_arm.rotation.x = deg_to_rad(-145.0 - sin(anim_time * 9.0) * 20.0)
		if left_leg:
			left_leg.rotation.x = sin(anim_time * 7.0) * 0.35
		if right_leg:
			right_leg.rotation.x = -sin(anim_time * 7.0) * 0.35
		return

	if dive_timer > 0.0:
		if torso_pivot:
			torso_pivot.rotation.x = deg_to_rad(55.0)
		if left_arm:
			left_arm.rotation.x = deg_to_rad(-130.0)
		if right_arm:
			right_arm.rotation.x = deg_to_rad(-130.0)
		if left_leg:
			left_leg.rotation.x = deg_to_rad(30.0)
		if right_leg:
			right_leg.rotation.x = deg_to_rad(30.0)
		return

	if not is_on_floor:
		if torso_pivot:
			torso_pivot.rotation.x = deg_to_rad(10.0)
		if left_arm:
			left_arm.rotation.x = deg_to_rad(-110.0)
			left_arm.rotation.z = deg_to_rad(-22.0)
		if right_arm:
			right_arm.rotation.x = deg_to_rad(-110.0)
			right_arm.rotation.z = deg_to_rad(22.0)
		if left_leg:
			left_leg.rotation.x = deg_to_rad(-28.0)
		if right_leg:
			right_leg.rotation.x = deg_to_rad(24.0)
		return

	if horiz_speed > 0.45:
		var run_phase: float = anim_time * (9.5 + clampf(horiz_speed * 0.45, 0.0, 5.0))
		var swing: float = sin(run_phase) * 0.85
		var bob: float = absf(cos(run_phase)) * 0.055
		if torso_pivot:
			torso_pivot.position.y = 0.62 + bob
			torso_pivot.rotation.x = deg_to_rad(11.0)
		if head_pivot:
			head_pivot.position.y = 1.12 + bob
			head_pivot.rotation.y = sin(run_phase * 0.5) * 0.08
		if left_arm:
			left_arm.rotation.x = swing
			left_arm.rotation.z = deg_to_rad(-8.0)
		if right_arm:
			right_arm.rotation.x = -swing
			right_arm.rotation.z = deg_to_rad(8.0)
		if left_leg:
			left_leg.rotation.x = -swing * 0.95
		if right_leg:
			right_leg.rotation.x = swing * 0.95
	else:
		var breath: float = sin(anim_time * 3.2) * 0.025
		if torso_pivot:
			torso_pivot.position.y = 0.62 + breath
			torso_pivot.rotation.x = 0.0
		if head_pivot:
			head_pivot.position.y = 1.12 + breath
			head_pivot.rotation.y = sin(anim_time * 1.4) * 0.14
			head_pivot.rotation.z = 0.0
		if left_arm:
			left_arm.rotation.x = sin(anim_time * 2.5) * 0.10
			left_arm.rotation.z = deg_to_rad(-12.0)
		if right_arm:
			right_arm.rotation.x = -sin(anim_time * 2.5) * 0.10
			right_arm.rotation.z = deg_to_rad(12.0)
		if left_leg:
			left_leg.rotation.x = 0.0
		if right_leg:
			right_leg.rotation.x = 0.0

static func make_box_style(
	bg_col: Color,
	border_col: Color,
	corner_radius: int = 16,
	border_width: int = 3,
	shadow_size: int = 6
) -> StyleBoxFlat:
	var sb := StyleBoxFlat.new()
	sb.bg_color = bg_col
	sb.border_color = border_col
	sb.set_border_width_all(border_width)
	sb.set_corner_radius_all(corner_radius)
	sb.shadow_color = Color(0.0, 0.0, 0.0, 0.48)
	sb.shadow_size = shadow_size
	sb.shadow_offset = Vector2(0, 3)
	sb.content_margin_left = 14
	sb.content_margin_right = 14
	sb.content_margin_top = 8
	sb.content_margin_bottom = 8
	return sb

static func style_button(
	btn: Button,
	bg_col: Color,
	border_col: Color,
	text_col: Color = Color(1, 1, 1, 1),
	font_size: int = 18,
	corner_radius: int = 16,
	border_width: int = 3
) -> void:
	var normal := make_box_style(bg_col, border_col, corner_radius, border_width, 6)
	var hover := make_box_style(bg_col.lightened(0.14), border_col.lightened(0.20), corner_radius, border_width, 8)
	var pressed := make_box_style(bg_col.darkened(0.15), border_col, corner_radius, border_width, 2)
	var disabled := make_box_style(Color(0.12, 0.15, 0.24, 0.80), Color(0.32, 0.36, 0.48, 0.75), corner_radius, 2, 2)
	btn.add_theme_stylebox_override("normal", normal)
	btn.add_theme_stylebox_override("hover", hover)
	btn.add_theme_stylebox_override("pressed", pressed)
	btn.add_theme_stylebox_override("focus", hover)
	btn.add_theme_stylebox_override("disabled", disabled)
	btn.add_theme_color_override("font_color", text_col)
	btn.add_theme_color_override("font_hover_color", text_col)
	btn.add_theme_color_override("font_pressed_color", text_col)
	btn.add_theme_color_override("font_disabled_color", Color(0.58, 0.64, 0.76, 0.85))
	btn.add_theme_color_override("font_outline_color", Color(0.02, 0.04, 0.10, 0.95))
	btn.add_theme_constant_override("outline_size", 4)
	btn.add_theme_font_size_override("font_size", font_size)
