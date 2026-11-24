extends RefCounted

## MYTHORIA games — By ahmed
## Procedural 3D Stylized Cartoon Character & Environment Visual Builder + Rich Illustrated Card Textures & UI Theme Helper.
## 100% self-contained with str() conversions and zero external Autoload calls in static functions.

const SKIN_COLORS := [
	{"primary": Color(0.92, 0.18, 0.22), "secondary": Color(1.0, 0.82, 0.18), "headgear": "crown_hair", "wings": false},
	{"primary": Color(1.0, 0.75, 0.08), "secondary": Color(0.0, 0.86, 1.0), "headgear": "laurel_crown", "wings": true},
	{"primary": Color(0.14, 0.52, 0.98), "secondary": Color(1.0, 0.84, 0.22), "headgear": "winged_helm", "wings": true},
	{"primary": Color(0.0, 0.78, 0.96), "secondary": Color(0.68, 0.94, 1.0), "headgear": "crystal_horns", "wings": false},
	{"primary": Color(0.88, 0.16, 0.32), "secondary": Color(1.0, 0.84, 0.16), "headgear": "royal_crown", "wings": true},
	{"primary": Color(0.75, 0.82, 0.92), "secondary": Color(1.0, 0.82, 0.18), "headgear": "spartan_crest", "wings": false},
	{"primary": Color(0.16, 0.18, 0.28), "secondary": Color(0.95, 0.22, 0.28), "headgear": "ninja_hood", "wings": false},
	{"primary": Color(0.12, 0.32, 0.78), "secondary": Color(1.0, 0.84, 0.16), "headgear": "royal_crown", "wings": true},
	{"primary": Color(0.18, 0.42, 0.92), "secondary": Color(0.0, 0.92, 1.0), "headgear": "wizard_halo", "wings": false},
	{"primary": Color(1.0, 0.58, 0.08), "secondary": Color(1.0, 0.88, 0.28), "headgear": "sun_halo", "wings": true},
	{"primary": Color(0.38, 0.68, 1.0), "secondary": Color(1.0, 0.86, 0.24), "headgear": "winged_helm", "wings": true},
	{"primary": Color(0.72, 0.22, 0.88), "secondary": Color(1.0, 0.84, 0.18), "headgear": "valkyrie_helm", "wings": true},
	{"primary": Color(0.14, 0.76, 0.52), "secondary": Color(1.0, 0.84, 0.18), "headgear": "spartan_crest", "wings": false},
	{"primary": Color(1.0, 0.80, 0.12), "secondary": Color(0.0, 0.88, 1.0), "headgear": "supreme_crown", "wings": true},
	{"primary": Color(0.45, 0.18, 0.92), "secondary": Color(0.0, 0.92, 1.0), "headgear": "wizard_halo", "wings": true}
]

static func make_mat(col: Color, roughness: float = 0.32, metallic: float = 0.08, emission_col: Color = Color(0, 0, 0, 0), emission_energy: float = 0.0) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = roughness
	m.metallic = metallic
	m.specular_mode = BaseMaterial3D.SPECULAR_SCHLICK_GGX
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
	var headgear_type: String = str(skin["headgear"])
	var has_wings: bool = bool(skin["wings"])

	var root := Node3D.new()
	root.name = "VisualRoot"

	var mat_primary := make_mat(prim_col, 0.28, 0.12)
	var mat_primary_light := make_mat(prim_col.lightened(0.22), 0.25, 0.15)
	var mat_secondary := make_mat(sec_col, 0.25, 0.42)
	var mat_gold := make_mat(Color(1.0, 0.84, 0.18), 0.18, 0.88, Color(1.0, 0.76, 0.12), 0.42)
	var mat_sapphire := make_mat(Color(0.0, 0.88, 1.0), 0.12, 0.35, Color(0.0, 0.78, 1.0), 1.6)
	var mat_ruby := make_mat(Color(1.0, 0.18, 0.32), 0.15, 0.35, Color(1.0, 0.15, 0.28), 1.2)
	var mat_skin := make_mat(Color(1.0, 0.83, 0.70), 0.42, 0.02)
	var mat_blush := make_mat(Color(1.0, 0.56, 0.58), 0.45, 0.0)
	var mat_dark_pants := make_mat(Color(0.14, 0.22, 0.42), 0.42, 0.08)
	var mat_white := make_mat(Color(0.98, 0.99, 1.0), 0.22, 0.05)
	var mat_eye_dark := make_mat(Color(0.06, 0.09, 0.18), 0.10, 0.15)
	var mat_eye_iris := make_mat(Color(0.12, 0.55, 0.98), 0.15, 0.20)
	var mat_hair := make_mat(Color(0.36, 0.18, 0.07), 0.38, 0.05)

	if idx == 6:
		mat_skin = make_mat(Color(0.18, 0.20, 0.28), 0.35, 0.18)
		mat_eye_iris = mat_ruby
	elif idx == 3:
		mat_skin = make_mat(Color(0.75, 0.96, 1.0), 0.18, 0.38, Color(0.0, 0.75, 1.0), 0.45)
	elif idx in [1, 4, 13]:
		mat_hair = make_mat(Color(0.95, 0.72, 0.12), 0.25, 0.45)

	# 1. ORGANIC ROUNDED TORSO PIVOT (Capsule + Shoulder Pads + Cape + Mythoria Emblem)
	var torso_pivot := Node3D.new()
	torso_pivot.name = "TorsoPivot"
	torso_pivot.position = Vector3(0.0, 0.62, 0.0)
	root.add_child(torso_pivot)

	# Smooth rounded capsule hoodie body instead of a boxy cube!
	var torso_mesh_inst := MeshInstance3D.new()
	var torso_cap := CapsuleMesh.new()
	torso_cap.radius = 0.265
	torso_cap.height = 0.66
	torso_cap.material = mat_primary
	torso_mesh_inst.mesh = torso_cap
	torso_pivot.add_child(torso_mesh_inst)

	# Chest armor / hoodie vest overlay
	var chest_vest := MeshInstance3D.new()
	var vest_cyl := CylinderMesh.new()
	vest_cyl.top_radius = 0.275
	vest_cyl.bottom_radius = 0.255
	vest_cyl.height = 0.34
	vest_cyl.material = mat_primary_light
	chest_vest.mesh = vest_cyl
	chest_vest.position = Vector3(0.0, 0.04, 0.0)
	torso_pivot.add_child(chest_vest)

	# Rounded golden collar ring
	var collar_inst := MeshInstance3D.new()
	var collar_cyl := CylinderMesh.new()
	collar_cyl.top_radius = 0.24
	collar_cyl.bottom_radius = 0.28
	collar_cyl.height = 0.09
	collar_cyl.material = mat_gold
	collar_inst.mesh = collar_cyl
	collar_inst.position = Vector3(0.0, 0.22, 0.0)
	torso_pivot.add_child(collar_inst)

	# Left & Right Pauldrons (Heroic Shoulder Pads)
	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		var pauldron := MeshInstance3D.new()
		var p_sph := SphereMesh.new()
		p_sph.radius = 0.135
		p_sph.height = 0.22
		p_sph.material = mat_secondary
		pauldron.mesh = p_sph
		pauldron.position = Vector3(sf * 0.28, 0.18, 0.0)
		pauldron.rotation.z = deg_to_rad(-sf * 18.0)
		torso_pivot.add_child(pauldron)

	# Golden MYTHORIA Chest Medallion + Glowing Sapphire Core
	var crest_inst := MeshInstance3D.new()
	var crest_cyl := CylinderMesh.new()
	crest_cyl.top_radius = 0.105
	crest_cyl.bottom_radius = 0.105
	crest_cyl.height = 0.045
	crest_cyl.material = mat_gold
	crest_inst.mesh = crest_cyl
	crest_inst.position = Vector3(0.0, 0.05, 0.255)
	crest_inst.rotation.x = deg_to_rad(90.0)
	torso_pivot.add_child(crest_inst)

	var gem_inst := MeshInstance3D.new()
	var gem_sphere := SphereMesh.new()
	gem_sphere.radius = 0.058
	gem_sphere.height = 0.115
	gem_sphere.material = mat_sapphire
	gem_inst.mesh = gem_sphere
	gem_inst.position = Vector3(0.0, 0.05, 0.278)
	torso_pivot.add_child(gem_inst)

	# Royal Belt + Golden Buckle
	var belt_inst := MeshInstance3D.new()
	var belt_cyl := CylinderMesh.new()
	belt_cyl.top_radius = 0.268
	belt_cyl.bottom_radius = 0.272
	belt_cyl.height = 0.085
	belt_cyl.material = mat_gold
	belt_inst.mesh = belt_cyl
	belt_inst.position = Vector3(0.0, -0.17, 0.0)
	torso_pivot.add_child(belt_inst)

	# Heroic Back Cape
	var cape_inst := MeshInstance3D.new()
	var cape_box := BoxMesh.new()
	cape_box.size = Vector3(0.44, 0.52, 0.035)
	cape_box.material = mat_secondary
	cape_inst.mesh = cape_box
	cape_inst.position = Vector3(0.0, -0.05, -0.26)
	cape_inst.rotation.x = deg_to_rad(14.0)
	torso_pivot.add_child(cape_inst)

	# Multi-Feathered Golden Mythoria Wings on Back
	if has_wings:
		for side_dir in [-1, 1]:
			var s_f: float = float(side_dir)
			for feather_i in range(3):
				var f_f: float = float(feather_i)
				var wing_feather := MeshInstance3D.new()
				var wf_cap := CapsuleMesh.new()
				wf_cap.radius = 0.055 - f_f * 0.008
				wf_cap.height = 0.42 - f_f * 0.06
				wf_cap.material = mat_gold if feather_i < 2 else mat_sapphire
				wing_feather.mesh = wf_cap
				wing_feather.position = Vector3(s_f * (0.24 + f_f * 0.11), 0.15 - f_f * 0.07, -0.24)
				wing_feather.rotation = Vector3(deg_to_rad(-10.0), deg_to_rad(s_f * 20.0), deg_to_rad(-s_f * (48.0 + f_f * 16.0)))
				torso_pivot.add_child(wing_feather)

	# 2. EXPRESSIVE CARTOON HEAD PIVOT
	var head_pivot := Node3D.new()
	head_pivot.name = "HeadPivot"
	head_pivot.position = Vector3(0.0, 1.15, 0.0)
	root.add_child(head_pivot)

	var head_inst := MeshInstance3D.new()
	var head_sphere := SphereMesh.new()
	head_sphere.radius = 0.31
	head_sphere.height = 0.58
	head_sphere.material = mat_skin
	head_inst.mesh = head_sphere
	head_pivot.add_child(head_inst)

	# Cute Cartoon Ears & Rosy Cheeks
	for side_dir in [-1, 1]:
		var s_f: float = float(side_dir)
		var ear := MeshInstance3D.new()
		var ear_sph := SphereMesh.new()
		ear_sph.radius = 0.065
		ear_sph.height = 0.13
		ear_sph.material = mat_skin
		ear.mesh = ear_sph
		ear.position = Vector3(s_f * 0.295, 0.0, 0.02)
		head_pivot.add_child(ear)

		var cheek := MeshInstance3D.new()
		var ch_sph := SphereMesh.new()
		ch_sph.radius = 0.048
		ch_sph.height = 0.065
		ch_sph.material = mat_blush
		cheek.mesh = ch_sph
		cheek.position = Vector3(s_f * 0.175, -0.045, 0.255)
		head_pivot.add_child(cheek)

	# Cute Button Nose & Cheerful Smile
	var nose := MeshInstance3D.new()
	var nose_sph := SphereMesh.new()
	nose_sph.radius = 0.036
	nose_sph.height = 0.065
	nose_sph.material = mat_skin
	nose.mesh = nose_sph
	nose.position = Vector3(0.0, -0.01, 0.305)
	head_pivot.add_child(nose)

	var smile := MeshInstance3D.new()
	var sm_cap := CapsuleMesh.new()
	sm_cap.radius = 0.018
	sm_cap.height = 0.11
	sm_cap.material = mat_white
	smile.mesh = sm_cap
	smile.position = Vector3(0.0, -0.095, 0.285)
	smile.rotation.z = deg_to_rad(90.0)
	head_pivot.add_child(smile)

	# Large Glossy Cartoon Eyes (Sclera + Colored Iris + Dark Pupil + Twin Sparkle Highlights)
	for side_dir in [-1, 1]:
		var s_f: float = float(side_dir)
		var eye_white := MeshInstance3D.new()
		var ew_mesh := SphereMesh.new()
		ew_mesh.radius = 0.078
		ew_mesh.height = 0.155
		ew_mesh.material = mat_white
		eye_white.mesh = ew_mesh
		eye_white.position = Vector3(s_f * 0.115, 0.04, 0.252)
		head_pivot.add_child(eye_white)

		var iris := MeshInstance3D.new()
		var ir_mesh := SphereMesh.new()
		ir_mesh.radius = 0.052
		ir_mesh.height = 0.102
		ir_mesh.material = mat_eye_iris
		iris.mesh = ir_mesh
		iris.position = Vector3(s_f * 0.115, 0.04, 0.288)
		head_pivot.add_child(iris)

		var pupil := MeshInstance3D.new()
		var p_mesh := SphereMesh.new()
		p_mesh.radius = 0.032
		p_mesh.height = 0.064
		p_mesh.material = mat_eye_dark
		pupil.mesh = p_mesh
		pupil.position = Vector3(s_f * 0.115, 0.04, 0.312)
		head_pivot.add_child(pupil)

		var highlight := MeshInstance3D.new()
		var h_mesh := SphereMesh.new()
		h_mesh.radius = 0.018
		h_mesh.height = 0.036
		h_mesh.material = mat_white
		highlight.mesh = h_mesh
		highlight.position = Vector3(s_f * 0.132, 0.066, 0.332)
		head_pivot.add_child(highlight)

		var brow := MeshInstance3D.new()
		var b_mesh := CapsuleMesh.new()
		b_mesh.radius = 0.018
		b_mesh.height = 0.11
		b_mesh.material = mat_hair
		brow.mesh = b_mesh
		brow.position = Vector3(s_f * 0.115, 0.145, 0.268)
		brow.rotation.z = deg_to_rad(90.0 - s_f * 10.0)
		head_pivot.add_child(brow)

	# Stylized Smooth Hair & Distinct Headgear per Skin (No brown boxes on the forehead!)
	var hair_cap := MeshInstance3D.new()
	var hc_mesh := SphereMesh.new()
	hc_mesh.radius = 0.318
	hc_mesh.height = 0.38
	hc_mesh.material = mat_primary if headgear_type == "ninja_hood" else mat_hair
	hair_cap.mesh = hc_mesh
	hair_cap.position = Vector3(0.0, 0.13, -0.03)
	head_pivot.add_child(hair_cap)

	# Smooth rounded hair swoop bangs
	for swoop_i in range(3):
		var sw := MeshInstance3D.new()
		var sw_cap := CapsuleMesh.new()
		sw_cap.radius = 0.055
		sw_cap.height = 0.22
		sw_cap.material = mat_primary if headgear_type == "ninja_hood" else mat_hair
		sw.mesh = sw_cap
		var off_x: float = float(swoop_i - 1) * 0.11
		sw.position = Vector3(off_x, 0.23, 0.21)
		sw.rotation = Vector3(deg_to_rad(65.0), deg_to_rad(float(swoop_i - 1) * 18.0), deg_to_rad(25.0))
		head_pivot.add_child(sw)

	# Distinct Heroic Crown / Helmet / Halo
	var crown_band := MeshInstance3D.new()
	var cb_mesh := CylinderMesh.new()
	cb_mesh.top_radius = 0.235
	cb_mesh.bottom_radius = 0.215
	cb_mesh.height = 0.12
	cb_mesh.material = mat_gold
	crown_band.mesh = cb_mesh
	crown_band.position = Vector3(0.0, 0.31, 0.0)
	head_pivot.add_child(crown_band)

	var peak_count: int = 7 if headgear_type == "supreme_crown" else 5
	for peak_i in range(peak_count):
		var ang: float = float(peak_i) * TAU / float(peak_count)
		var peak := MeshInstance3D.new()
		var pk_cone := CylinderMesh.new()
		pk_cone.top_radius = 0.01
		pk_cone.bottom_radius = 0.048
		pk_cone.height = 0.13
		pk_cone.material = mat_gold
		peak.mesh = pk_cone
		peak.position = Vector3(sin(ang) * 0.205, 0.40, cos(ang) * 0.205)
		head_pivot.add_child(peak)

		var tip_gem := MeshInstance3D.new()
		var tg_sph := SphereMesh.new()
		tg_sph.radius = 0.024
		tg_sph.height = 0.048
		tg_sph.material = mat_sapphire if (peak_i % 2 == 0) else mat_ruby
		tip_gem.mesh = tg_sph
		tip_gem.position = Vector3(sin(ang) * 0.205, 0.47, cos(ang) * 0.205)
		head_pivot.add_child(tip_gem)

	var crown_gem := MeshInstance3D.new()
	var cg_mesh := SphereMesh.new()
	cg_mesh.radius = 0.052
	cg_mesh.height = 0.104
	cg_mesh.material = mat_sapphire
	crown_gem.mesh = cg_mesh
	crown_gem.position = Vector3(0.0, 0.32, 0.225)
	head_pivot.add_child(crown_gem)

	if headgear_type in ["spartan_crest", "valkyrie_helm"]:
		var crest_plume := MeshInstance3D.new()
		var cp_cap := CapsuleMesh.new()
		cp_cap.radius = 0.065
		cp_cap.height = 0.44
		cp_cap.material = mat_ruby if headgear_type == "spartan_crest" else mat_sapphire
		crest_plume.mesh = cp_cap
		crest_plume.position = Vector3(0.0, 0.48, -0.02)
		crest_plume.rotation.x = deg_to_rad(82.0)
		head_pivot.add_child(crest_plume)
	elif headgear_type in ["winged_helm", "valkyrie_helm"]:
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			var h_wing := MeshInstance3D.new()
			var hw_cap := CapsuleMesh.new()
			hw_cap.radius = 0.038
			hw_cap.height = 0.28
			hw_cap.material = mat_gold
			h_wing.mesh = hw_cap
			h_wing.position = Vector3(sf * 0.27, 0.38, 0.0)
			h_wing.rotation.z = deg_to_rad(-sf * 28.0)
			head_pivot.add_child(h_wing)
	elif headgear_type in ["wizard_halo", "sun_halo"]:
		var halo := MeshInstance3D.new()
		var hl_cyl := CylinderMesh.new()
		hl_cyl.top_radius = 0.28
		hl_cyl.bottom_radius = 0.28
		hl_cyl.height = 0.035
		hl_cyl.material = mat_sapphire if headgear_type == "wizard_halo" else mat_gold
		halo.mesh = hl_cyl
		halo.position = Vector3(0.0, 0.56, 0.0)
		head_pivot.add_child(halo)

	# 3. LEFT & RIGHT ARMS + GOLDEN WRIST BRACERS
	var arm_names := ["LeftArmPivot", "RightArmPivot"]
	var arm_xs := [-0.36, 0.36]
	for a_i in range(2):
		var arm_pivot := Node3D.new()
		arm_pivot.name = arm_names[a_i]
		arm_pivot.position = Vector3(arm_xs[a_i], 0.80, 0.0)
		root.add_child(arm_pivot)

		var sleeve := MeshInstance3D.new()
		var s_mesh := CapsuleMesh.new()
		s_mesh.radius = 0.088
		s_mesh.height = 0.36
		s_mesh.material = mat_primary
		sleeve.mesh = s_mesh
		sleeve.position = Vector3(0.0, -0.14, 0.0)
		arm_pivot.add_child(sleeve)

		var bracer := MeshInstance3D.new()
		var br_cyl := CylinderMesh.new()
		br_cyl.top_radius = 0.092
		br_cyl.bottom_radius = 0.088
		br_cyl.height = 0.09
		br_cyl.material = mat_gold
		bracer.mesh = br_cyl
		bracer.position = Vector3(0.0, -0.23, 0.0)
		arm_pivot.add_child(bracer)

		var hand := MeshInstance3D.new()
		var h_mesh := SphereMesh.new()
		h_mesh.radius = 0.088
		h_mesh.height = 0.175
		h_mesh.material = mat_skin
		hand.mesh = h_mesh
		hand.position = Vector3(0.0, -0.32, 0.0)
		arm_pivot.add_child(hand)

	# 4. LEFT & RIGHT LEGS + ROUNDED WINGED SNEAKERS
	var leg_names := ["LeftLegPivot", "RightLegPivot"]
	var leg_xs := [-0.15, 0.15]
	for l_i in range(2):
		var leg_pivot := Node3D.new()
		leg_pivot.name = leg_names[l_i]
		leg_pivot.position = Vector3(leg_xs[l_i], 0.38, 0.0)
		root.add_child(leg_pivot)

		var pant := MeshInstance3D.new()
		var p_mesh := CapsuleMesh.new()
		p_mesh.radius = 0.10
		p_mesh.height = 0.34
		p_mesh.material = mat_dark_pants
		pant.mesh = p_mesh
		pant.position = Vector3(0.0, -0.14, 0.0)
		leg_pivot.add_child(pant)

		var shoe := MeshInstance3D.new()
		var sh_mesh := CapsuleMesh.new()
		sh_mesh.radius = 0.105
		sh_mesh.height = 0.30
		sh_mesh.material = mat_primary
		shoe.mesh = sh_mesh
		shoe.position = Vector3(0.0, -0.30, 0.045)
		shoe.rotation.x = deg_to_rad(90.0)
		leg_pivot.add_child(shoe)

		var sole := MeshInstance3D.new()
		var so_mesh := CylinderMesh.new()
		so_mesh.top_radius = 0.11
		so_mesh.bottom_radius = 0.115
		so_mesh.height = 0.045
		so_mesh.material = mat_white
		sole.mesh = so_mesh
		sole.position = Vector3(0.0, -0.36, 0.045)
		sole.scale = Vector3(1.0, 1.0, 1.35)
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
			head_pivot.position.y = 1.15 + bounce
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
			left_arm.rotation.x = deg_to_rad(-115.0)
			left_arm.rotation.z = deg_to_rad(-24.0)
		if right_arm:
			right_arm.rotation.x = deg_to_rad(-115.0)
			right_arm.rotation.z = deg_to_rad(24.0)
		if left_leg:
			left_leg.rotation.x = deg_to_rad(-30.0)
		if right_leg:
			right_leg.rotation.x = deg_to_rad(26.0)
		return

	if horiz_speed > 0.45:
		var run_phase: float = anim_time * (9.5 + clampf(horiz_speed * 0.45, 0.0, 5.0))
		var swing: float = sin(run_phase) * 0.88
		var bob: float = absf(cos(run_phase)) * 0.06
		if torso_pivot:
			torso_pivot.position.y = 0.62 + bob
			torso_pivot.rotation.x = deg_to_rad(11.0)
		if head_pivot:
			head_pivot.position.y = 1.15 + bob
			head_pivot.rotation.y = sin(run_phase * 0.5) * 0.08
		if left_arm:
			left_arm.rotation.x = swing
			left_arm.rotation.z = deg_to_rad(-10.0)
		if right_arm:
			right_arm.rotation.x = -swing
			right_arm.rotation.z = deg_to_rad(10.0)
		if left_leg:
			left_leg.rotation.x = -swing * 0.95
		if right_leg:
			right_leg.rotation.x = swing * 0.95
	else:
		var breath: float = sin(anim_time * 3.2) * 0.028
		if torso_pivot:
			torso_pivot.position.y = 0.62 + breath
			torso_pivot.rotation.x = 0.0
		if head_pivot:
			head_pivot.position.y = 1.15 + breath
			head_pivot.rotation.y = sin(anim_time * 1.4) * 0.15
			head_pivot.rotation.z = 0.0
		if left_arm:
			left_arm.rotation.x = sin(anim_time * 2.5) * 0.12
			left_arm.rotation.z = deg_to_rad(-14.0)
		if right_arm:
			right_arm.rotation.x = -sin(anim_time * 2.5) * 0.12
			right_arm.rotation.z = deg_to_rad(14.0)
		if left_leg:
			left_leg.rotation.x = 0.0
		if right_leg:
			right_leg.rotation.x = 0.0

## PROCEDURAL ILLUSTRATED ARTWORK TEXTURES FOR WORLDS, CHARACTERS & MISSIONS
static func make_world_banner_texture(sky_top: Color, sky_bot: Color, prim_col: Color, acc_col: Color) -> ImageTexture:
	var w: int = 160
	var h: int = 92
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	for y in range(h):
		var ty: float = float(y) / float(h)
		var bg: Color = sky_top.lerp(sky_bot, ty)
		for x in range(w):
			var c: Color = bg
			var dx_sun: float = float(x - 122)
			var dy_sun: float = float(y - 24)
			var d_sun: float = sqrt(dx_sun * dx_sun + dy_sun * dy_sun)
			if d_sun < 15.0:
				c = acc_col.lightened(0.35)
			elif d_sun < 24.0:
				c = c.lerp(acc_col, 0.35)

			# Floating mythological island silhouette
			var island_top: int = 54 + int(sin(float(x) * 0.08) * 3.0)
			if x > 24 and x < 136 and y >= island_top:
				var half_w: int = 56 - int(float(y - island_top) * 1.25)
				if abs(x - 80) <= half_w:
					if y < island_top + 7:
						c = prim_col.lightened(0.18)
					else:
						c = prim_col.darkened(0.38)
					# Glowing waterfall in center
					if abs(x - 80) <= 6 and y >= island_top + 4:
						c = Color(0.0, 0.90, 1.0)

			# Greek temple pillars & golden roof on top of island
			if y >= 32 and y < island_top and x >= 56 and x <= 104:
				if y < 39 and abs(x - 80) <= (y - 31) * 3:
					c = acc_col
				elif y >= 39 and ((x - 56) % 14 < 5):
					c = Color(0.96, 0.98, 1.0)

			img.set_pixel(x, y, c)
	return ImageTexture.create_from_image(img)

static func make_character_portrait_texture(skin_id: int) -> ImageTexture:
	var idx: int = clampi(skin_id, 0, SKIN_COLORS.size() - 1)
	var skin: Dictionary = SKIN_COLORS[idx]
	var prim_col: Color = skin["primary"]
	var sec_col: Color = skin["secondary"]
	var w: int = 128
	var h: int = 128
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	var gold := Color(1.0, 0.84, 0.18)
	var face_col := Color(1.0, 0.83, 0.70) if idx != 6 else Color(0.20, 0.22, 0.30)

	for y in range(h):
		var ty: float = float(y) / float(h)
		for x in range(w):
			var dx: float = float(x - 64)
			var dy: float = float(y - 64)
			var dist: float = sqrt(dx * dx + dy * dy)
			var c: Color = prim_col.darkened(0.55).lerp(sec_col.darkened(0.35), ty)
			if dist < 54.0 and dist > 49.0:
				c = gold
			elif dist < 49.0:
				c = c.lerp(prim_col, 0.25)

			# Hero Torso & Golden Emblem
			if y >= 78 and y <= 116 and abs(x - 64) <= 28:
				c = prim_col
				if y <= 85:
					c = gold
				elif abs(x - 64) <= 7 and abs(y - 96) <= 7:
					c = Color(0.0, 0.88, 1.0)

			# Hero Face Circle
			var h_dist: float = sqrt(float((x - 64) * (x - 64) + (y - 56) * (y - 56)))
			if h_dist <= 22.0:
				c = face_col
				# Expressive Eyes
				var le: float = sqrt(float((x - 55) * (x - 55) + (y - 55) * (y - 55)))
				var re: float = sqrt(float((x - 73) * (x - 73) + (y - 55) * (y - 55)))
				if le <= 4.5 or re <= 4.5:
					c = Color(0.06, 0.10, 0.20)
				if le <= 1.8 or re <= 1.8:
					c = Color.WHITE

			# Golden Crown above Head
			if y >= 22 and y <= 37 and abs(x - 64) <= 20:
				var crown_wave: int = 22 + int(abs(((x - 44) % 10) - 5) * 2)
				if y >= crown_wave:
					c = gold
					if abs(x - 64) <= 4 and abs(y - 31) <= 4:
						c = Color(0.0, 0.90, 1.0)

			img.set_pixel(x, y, c)
	return ImageTexture.create_from_image(img)

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
