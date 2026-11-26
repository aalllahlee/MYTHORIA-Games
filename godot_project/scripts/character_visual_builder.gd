extends RefCounted

## MYTHORIA games — By ahmed
## Ultra-Detailed 3D Stylized Cartoon Character & Environment Visual Builder:
## Strictly 3 Exclusive Heroes:
##   0: Ahmed (Royal Winged Hero Boy)
##   1: Salma (Star Princess Heroine Girl)
##   2: Ahmed (Turbo Kid Adventurer with Cap, Headphones & Rocket Jetpack)

const SKIN_COLORS := [
	{"primary": Color(0.92, 0.18, 0.24), "secondary": Color(1.0, 0.84, 0.18), "headgear": "crown_hair", "wings": true, "style": "ahmed_hero"},
	{"primary": Color(0.96, 0.28, 0.64), "secondary": Color(1.0, 0.86, 0.24), "headgear": "girl_pigtails", "wings": true, "style": "salma_girl"},
	{"primary": Color(0.10, 0.78, 0.52), "secondary": Color(0.14, 0.54, 0.98), "headgear": "kid_cap", "wings": false, "style": "ahmed_kid2"}
]

static var _platform_mat_cache: Dictionary = {}

static func make_mat(col: Color, roughness: float = 0.22, metallic: float = 0.14, emission_col: Color = Color(0, 0, 0, 0), emission_energy: float = 0.0) -> StandardMaterial3D:
	var m := StandardMaterial3D.new()
	m.albedo_color = col
	m.roughness = roughness
	m.metallic = metallic
	m.specular_mode = BaseMaterial3D.SPECULAR_SCHLICK_GGX
	m.rim_enabled = true
	m.rim = 0.38
	m.rim_tint = 0.45
	if col.a < 0.99:
		m.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	if emission_energy > 0.0:
		m.emission_enabled = true
		m.emission = emission_col
		m.emission_energy_multiplier = minf(emission_energy, 0.58)
	return m

static func _add_part(parent: Node3D, mesh: Mesh, pos: Vector3, rot_deg: Vector3 = Vector3.ZERO, sc: Vector3 = Vector3.ONE) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	mi.mesh = mesh
	mi.position = pos
	if rot_deg != Vector3.ZERO:
		mi.rotation_degrees = rot_deg
	if sc != Vector3.ONE:
		mi.scale = sc
	parent.add_child(mi)
	return mi

static func _make_capsule(r: float, h: float, mat: Material) -> CapsuleMesh:
	var m := CapsuleMesh.new()
	m.radius = r
	m.height = h
	m.material = mat
	return m

static func _make_sphere(r: float, h: float, mat: Material) -> SphereMesh:
	var m := SphereMesh.new()
	m.radius = r
	m.height = h
	m.material = mat
	return m

static func _make_cyl(top_r: float, bot_r: float, h: float, mat: Material) -> CylinderMesh:
	var m := CylinderMesh.new()
	m.top_radius = top_r
	m.bottom_radius = bot_r
	m.height = h
	m.material = mat
	return m

static func _make_box(sz: Vector3, mat: Material) -> BoxMesh:
	var m := BoxMesh.new()
	m.size = sz
	m.material = mat
	return m

static func build_character_visual(skin_id: int = 0) -> Node3D:
	var idx: int = clampi(skin_id % 3, 0, 2)
	var skin: Dictionary = SKIN_COLORS[idx]
	var prim_col: Color = skin["primary"]
	var sec_col: Color = skin["secondary"]
	var char_style: String = str(skin["style"])

	var root := Node3D.new()
	root.name = "VisualRoot"

	var mat_primary := make_mat(prim_col, 0.22, 0.16)
	var mat_primary_light := make_mat(prim_col.lightened(0.26), 0.20, 0.18)
	var mat_primary_dark := make_mat(prim_col.darkened(0.25), 0.28, 0.12)
	var mat_secondary := make_mat(sec_col, 0.20, 0.42)
	var mat_gold := make_mat(Color(1.0, 0.84, 0.18), 0.16, 0.88, Color(1.0, 0.76, 0.12), 0.35)
	var mat_sapphire := make_mat(Color(0.0, 0.88, 1.0), 0.12, 0.35, Color(0.0, 0.78, 1.0), 0.55)
	var mat_ruby := make_mat(Color(1.0, 0.18, 0.36), 0.14, 0.35, Color(1.0, 0.15, 0.30), 0.50)
	var mat_skin := make_mat(Color(1.0, 0.85, 0.74), 0.34, 0.02)
	var mat_skin_warm := make_mat(Color(0.98, 0.76, 0.64), 0.36, 0.02)
	var mat_blush := make_mat(Color(1.0, 0.48, 0.58), 0.38, 0.0)
	var mat_white := make_mat(Color(0.98, 0.99, 1.0), 0.18, 0.06)
	var mat_eye_dark := make_mat(Color(0.05, 0.08, 0.16), 0.08, 0.15)
	var mat_hair_brown := make_mat(Color(0.30, 0.14, 0.06), 0.30, 0.08)
	var mat_hair_salma := make_mat(Color(0.22, 0.09, 0.14), 0.26, 0.12)
	var mat_navy := make_mat(Color(0.12, 0.20, 0.42), 0.34, 0.10)

	var mat_hair: StandardMaterial3D = mat_hair_salma if char_style == "salma_girl" else mat_hair_brown
	var mat_eye_iris: StandardMaterial3D = make_mat(Color(0.74, 0.22, 0.92), 0.12, 0.25) if char_style == "salma_girl" else (make_mat(Color(0.0, 0.78, 0.96), 0.12, 0.25) if char_style == "ahmed_kid2" else make_mat(Color(0.14, 0.52, 0.98), 0.12, 0.25))

	# =========================================================
	# 1. SCULPTED TORSO PIVOT (Unique Outfit per Hero!)
	# =========================================================
	var torso_pivot := Node3D.new()
	torso_pivot.name = "TorsoPivot"
	torso_pivot.position = Vector3(0.0, 0.62, 0.0)
	root.add_child(torso_pivot)

	# Upper chest & waist base
	_add_part(torso_pivot, _make_capsule(0.255, 0.64, mat_primary), Vector3.ZERO)

	if char_style == "salma_girl":
		# SALMA'S ROYAL PRINCESS DRESS:
		# Fitted bodice + sweetheart gold trim + puff shoulders + golden heart necklace + 2-tier pleated flared skirt + waist ribbon bow + fairy wings!
		_add_part(torso_pivot, _make_cyl(0.265, 0.235, 0.32, mat_primary_light), Vector3(0.0, 0.06, 0.0))
		_add_part(torso_pivot, _make_cyl(0.22, 0.26, 0.07, mat_white), Vector3(0.0, 0.22, 0.0))
		# Golden heart necklace pendant
		_add_part(torso_pivot, _make_sphere(0.055, 0.10, mat_gold), Vector3(0.0, 0.12, 0.255))
		_add_part(torso_pivot, _make_sphere(0.036, 0.07, mat_ruby), Vector3(0.0, 0.12, 0.278))
		# Cute Puff Shoulders
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(torso_pivot, _make_sphere(0.135, 0.25, mat_primary_light), Vector3(sf * 0.28, 0.18, 0.0))
			_add_part(torso_pivot, _make_cyl(0.105, 0.105, 0.035, mat_gold), Vector3(sf * 0.29, 0.10, 0.0), Vector3(0, 0, -sf * 16.0))
		# Golden Waist Sash & Back Butterfly Bow
		_add_part(torso_pivot, _make_cyl(0.248, 0.255, 0.08, mat_gold), Vector3(0.0, -0.09, 0.0))
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(torso_pivot, _make_capsule(0.06, 0.22, mat_gold), Vector3(sf * 0.11, -0.08, -0.25), Vector3(0, 0, sf * 55.0))
		# Two-Tiered Flared Pleated Skirt
		_add_part(torso_pivot, _make_cyl(0.245, 0.36, 0.18, mat_primary), Vector3(0.0, -0.18, 0.0))
		_add_part(torso_pivot, _make_cyl(0.34, 0.43, 0.15, mat_primary_light), Vector3(0.0, -0.27, 0.0))
		_add_part(torso_pivot, _make_cyl(0.425, 0.44, 0.035, mat_gold), Vector3(0.0, -0.33, 0.0))
		# Shimmering Fairy Wings (4 crystalline petals)
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(torso_pivot, _make_capsule(0.075, 0.44, mat_gold), Vector3(sf * 0.26, 0.18, -0.23), Vector3(-12.0, sf * 22.0, -sf * 46.0))
			_add_part(torso_pivot, _make_capsule(0.062, 0.36, mat_sapphire), Vector3(sf * 0.32, 0.05, -0.24), Vector3(-8.0, sf * 18.0, -sf * 68.0))
	elif char_style == "ahmed_kid2":
		# LITTLE AHMED'S TURBO STREET HOODIE + NECK HEADPHONES + ROCKET JETPACK:
		_add_part(torso_pivot, _make_cyl(0.272, 0.262, 0.36, mat_primary_light), Vector3(0.0, 0.03, 0.0))
		# White & Blue Racing Stripes + Kangaroo Pocket
		_add_part(torso_pivot, _make_box(Vector3(0.26, 0.14, 0.06), mat_secondary), Vector3(0.0, -0.11, 0.24))
		_add_part(torso_pivot, _make_cyl(0.095, 0.095, 0.04, mat_gold), Vector3(0.0, 0.07, 0.255), Vector3(90.0, 0.0, 0.0))
		_add_part(torso_pivot, _make_sphere(0.052, 0.10, mat_sapphire), Vector3(0.0, 0.07, 0.275))
		# Golden Headphones resting around his neck!
		_add_part(torso_pivot, _make_cyl(0.245, 0.255, 0.065, mat_navy), Vector3(0.0, 0.22, 0.0))
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(torso_pivot, _make_cyl(0.075, 0.075, 0.06, mat_gold), Vector3(sf * 0.23, 0.23, 0.10), Vector3(0.0, 0.0, 90.0))
			_add_part(torso_pivot, _make_sphere(0.05, 0.09, mat_sapphire), Vector3(sf * 0.26, 0.23, 0.10))
		# 3D Twin-Thruster Rocket Jetpack on Back!
		_add_part(torso_pivot, _make_box(Vector3(0.34, 0.36, 0.16), mat_secondary), Vector3(0.0, 0.04, -0.27))
		_add_part(torso_pivot, _make_box(Vector3(0.26, 0.24, 0.05), mat_gold), Vector3(0.0, 0.05, -0.35))
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(torso_pivot, _make_cyl(0.075, 0.085, 0.34, mat_white), Vector3(sf * 0.15, 0.02, -0.31))
			_add_part(torso_pivot, _make_cyl(0.065, 0.095, 0.10, mat_gold), Vector3(sf * 0.15, -0.17, -0.31))
			_add_part(torso_pivot, _make_sphere(0.058, 0.12, mat_sapphire), Vector3(sf * 0.15, -0.23, -0.31))
		root.scale = Vector3(0.92, 0.92, 0.92)
	else:
		# HERO AHMED'S ROYAL OLYMPUS ARMOR JACKET + PAULDRONS + CAPE + GOLDEN WINGS:
		_add_part(torso_pivot, _make_cyl(0.275, 0.255, 0.35, mat_primary_light), Vector3(0.0, 0.04, 0.0))
		_add_part(torso_pivot, _make_cyl(0.24, 0.28, 0.09, mat_gold), Vector3(0.0, 0.22, 0.0))
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(torso_pivot, _make_sphere(0.14, 0.24, mat_gold), Vector3(sf * 0.29, 0.18, 0.0), Vector3(0.0, 0.0, -sf * 18.0))
			_add_part(torso_pivot, _make_cyl(0.12, 0.13, 0.04, mat_primary_dark), Vector3(sf * 0.29, 0.11, 0.0), Vector3(0.0, 0.0, -sf * 18.0))
		# Royal Medallion & Glowing Core
		_add_part(torso_pivot, _make_cyl(0.11, 0.11, 0.045, mat_gold), Vector3(0.0, 0.05, 0.255), Vector3(90.0, 0.0, 0.0))
		_add_part(torso_pivot, _make_sphere(0.06, 0.115, mat_sapphire), Vector3(0.0, 0.05, 0.28))
		# Golden Champion Belt & Buckle
		_add_part(torso_pivot, _make_cyl(0.268, 0.272, 0.085, mat_gold), Vector3(0.0, -0.17, 0.0))
		_add_part(torso_pivot, _make_box(Vector3(0.12, 0.11, 0.05), mat_sapphire), Vector3(0.0, -0.17, 0.26))
		# Flowing Royal Cape
		_add_part(torso_pivot, _make_box(Vector3(0.46, 0.54, 0.035), mat_primary_dark), Vector3(0.0, -0.05, -0.26), Vector3(14.0, 0.0, 0.0))
		_add_part(torso_pivot, _make_box(Vector3(0.48, 0.06, 0.04), mat_gold), Vector3(0.0, -0.31, -0.32), Vector3(14.0, 0.0, 0.0))
		# 6-Feathered Golden Angel Wings
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			for f_i in range(3):
				var ff: float = float(f_i)
				var w_mat: StandardMaterial3D = mat_gold if f_i < 2 else mat_sapphire
				_add_part(
					torso_pivot,
					_make_capsule(0.056 - ff * 0.008, 0.44 - ff * 0.06, w_mat),
					Vector3(sf * (0.24 + ff * 0.11), 0.16 - ff * 0.07, -0.24),
					Vector3(-10.0, sf * 20.0, -sf * (46.0 + ff * 16.0))
				)

	# =========================================================
	# 2. SCULPTED ANIME/CHIBI HEAD PIVOT
	# =========================================================
	var head_pivot := Node3D.new()
	head_pivot.name = "HeadPivot"
	head_pivot.position = Vector3(0.0, 1.15, 0.0)
	root.add_child(head_pivot)

	# Soft Chibi Head & Neck
	_add_part(head_pivot, _make_cyl(0.11, 0.12, 0.14, mat_skin_warm), Vector3(0.0, -0.27, 0.0))
	_add_part(head_pivot, _make_sphere(0.31, 0.58, mat_skin), Vector3.ZERO)

	# Ears & Rosy Anime Cheeks
	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		_add_part(head_pivot, _make_sphere(0.065, 0.13, mat_skin), Vector3(sf * 0.295, -0.01, 0.02))
		_add_part(head_pivot, _make_sphere(0.055 if char_style == "salma_girl" else 0.048, 0.07, mat_blush), Vector3(sf * 0.18, -0.05, 0.258))
		if char_style == "salma_girl":
			# Salma's Golden Pearl Earrings!
			_add_part(head_pivot, _make_sphere(0.030, 0.06, mat_gold), Vector3(sf * 0.31, -0.075, 0.03))

	# Cute Sculpted Nose & Happy 3D Smile
	_add_part(head_pivot, _make_sphere(0.034, 0.062, mat_skin_warm), Vector3(0.0, -0.01, 0.308))
	_add_part(head_pivot, _make_capsule(0.020, 0.115, mat_white), Vector3(0.0, -0.095, 0.288), Vector3(0.0, 0.0, 90.0))
	_add_part(head_pivot, _make_capsule(0.014, 0.08, mat_ruby), Vector3(0.0, -0.105, 0.284), Vector3(0.0, 0.0, 90.0))

	# Big Expressive Multi-Layered Anime Eyes (Sclera + Colored Iris + Dark Pupil + Dual Catchlights + Upper Eyelid + Eyebrow)
	for side_dir in [-1, 1]:
		var sf: float = float(side_dir)
		_add_part(head_pivot, _make_sphere(0.082, 0.162, mat_white), Vector3(sf * 0.118, 0.038, 0.250))
		_add_part(head_pivot, _make_sphere(0.056, 0.110, mat_eye_iris), Vector3(sf * 0.118, 0.038, 0.286))
		_add_part(head_pivot, _make_sphere(0.034, 0.068, mat_eye_dark), Vector3(sf * 0.118, 0.038, 0.312))
		# Top-outer big starlight catchlight + bottom-inner mini catchlight
		_add_part(head_pivot, _make_sphere(0.020, 0.040, mat_white), Vector3(sf * 0.136, 0.066, 0.332))
		_add_part(head_pivot, _make_sphere(0.011, 0.022, mat_white), Vector3(sf * 0.102, 0.016, 0.330))
		# Upper Eyelash / Eyelid Ridge
		_add_part(head_pivot, _make_capsule(0.016, 0.125, mat_eye_dark), Vector3(sf * 0.118, 0.105, 0.286), Vector3(0.0, 0.0, 90.0 - sf * 6.0))
		if char_style == "salma_girl":
			# Salma's Curved Side Wing Eyelashes!
			_add_part(head_pivot, _make_capsule(0.012, 0.065, mat_eye_dark), Vector3(sf * 0.175, 0.095, 0.272), Vector3(0.0, 0.0, 90.0 - sf * 32.0))
		# Expressive Eyebrow
		_add_part(head_pivot, _make_capsule(0.018, 0.115, mat_hair), Vector3(sf * 0.118, 0.152, 0.268), Vector3(0.0, 0.0, 90.0 - sf * (4.0 if char_style == "salma_girl" else 11.0)))

	# =========================================================
	# 3. DISTINCT SCULPTED HAIRSTYLE & HEADGEAR PER HERO
	# =========================================================
	if char_style == "salma_girl":
		# SALMA'S SCULPTED HAIR + BANGS + LONG WAVY TWIN PIGTAILS + RIBBON BOWS + PRINCESS TIARA:
		_add_part(head_pivot, _make_sphere(0.324, 0.42, mat_hair), Vector3(0.0, 0.12, -0.04))
		# Front Fringe Bangs (5 sculpted locks)
		for b_i in range(-2, 3):
			var bf: float = float(b_i)
			_add_part(
				head_pivot,
				_make_capsule(0.048, 0.22, mat_hair),
				Vector3(bf * 0.065, 0.22, 0.22 - absf(bf) * 0.02),
				Vector3(62.0, bf * 14.0, bf * 10.0)
			)
		# Cute Face-Framing Side Locks
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(head_pivot, _make_capsule(0.045, 0.26, mat_hair), Vector3(sf * 0.26, 0.02, 0.16), Vector3(15.0, 0.0, -sf * 12.0))
			# Big Butterfly Silk Ribbon Bows
			_add_part(head_pivot, _make_sphere(0.055, 0.10, mat_gold), Vector3(sf * 0.32, 0.15, -0.03))
			_add_part(head_pivot, _make_capsule(0.058, 0.18, mat_primary_light), Vector3(sf * 0.34, 0.19, 0.03), Vector3(35.0, 0.0, -sf * 25.0))
			_add_part(head_pivot, _make_capsule(0.058, 0.18, mat_primary_light), Vector3(sf * 0.34, 0.11, -0.09), Vector3(-35.0, 0.0, -sf * 25.0))
			# Flowing 3-Segment Wavy Twin Pigtails!
			_add_part(head_pivot, _make_capsule(0.092, 0.30, mat_hair), Vector3(sf * 0.40, 0.04, -0.06), Vector3(10.0, 0.0, -sf * 34.0))
			_add_part(head_pivot, _make_capsule(0.082, 0.30, mat_hair), Vector3(sf * 0.48, -0.14, -0.08), Vector3(16.0, 0.0, -sf * 18.0))
			_add_part(head_pivot, _make_capsule(0.064, 0.24, mat_hair), Vector3(sf * 0.51, -0.30, -0.05), Vector3(22.0, 0.0, -sf * 6.0))
		# Golden Princess Tiara with Ruby Heart Center & Pearl Studs
		_add_part(head_pivot, _make_cyl(0.22, 0.21, 0.09, mat_gold), Vector3(0.0, 0.31, 0.02))
		_add_part(head_pivot, _make_cyl(0.02, 0.08, 0.16, mat_gold), Vector3(0.0, 0.40, 0.19))
		_add_part(head_pivot, _make_sphere(0.054, 0.105, mat_ruby), Vector3(0.0, 0.35, 0.225))
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(head_pivot, _make_cyl(0.015, 0.05, 0.12, mat_gold), Vector3(sf * 0.13, 0.38, 0.15))
			_add_part(head_pivot, _make_sphere(0.028, 0.055, mat_white), Vector3(sf * 0.13, 0.44, 0.15))

	elif char_style == "ahmed_kid2":
		# LITTLE AHMED'S TWO-TONE STAR BASEBALL CAP + SPIKY HAIR TUFTS PEEKING OUT:
		_add_part(head_pivot, _make_sphere(0.326, 0.38, mat_secondary), Vector3(0.0, 0.15, -0.02))
		_add_part(head_pivot, _make_cyl(0.318, 0.318, 0.07, mat_gold), Vector3(0.0, 0.17, 0.0))
		_add_part(head_pivot, _make_sphere(0.045, 0.08, mat_gold), Vector3(0.0, 0.35, -0.02))
		# Curved Golden Visor Brim + Front Star Badge
		_add_part(head_pivot, _make_box(Vector3(0.40, 0.055, 0.26), mat_gold), Vector3(0.0, 0.19, 0.26), Vector3(12.0, 0.0, 0.0))
		_add_part(head_pivot, _make_sphere(0.052, 0.10, mat_sapphire), Vector3(0.0, 0.26, 0.29))
		# Spiky Chestnut Hair Tufts peeking under the cap front & sides!
		for b_i in range(-1, 2):
			var bf: float = float(b_i)
			_add_part(head_pivot, _make_capsule(0.044, 0.18, mat_hair), Vector3(bf * 0.10, 0.17, 0.25), Vector3(70.0, bf * 22.0, 15.0))
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(head_pivot, _make_capsule(0.048, 0.18, mat_hair), Vector3(sf * 0.28, 0.08, 0.10), Vector3(20.0, 0.0, -sf * 30.0))

	else:
		# HERO AHMED'S LAYERED SPIKY ANIME HAIR + ROYAL 5-PEAK OLYMPUS CROWN:
		_add_part(head_pivot, _make_sphere(0.322, 0.40, mat_hair), Vector3(0.0, 0.13, -0.03))
		# 5 Sculpted Spiky Front & Side Anime Bangs
		for b_i in range(-2, 3):
			var bf: float = float(b_i)
			_add_part(
				head_pivot,
				_make_capsule(0.054, 0.24, mat_hair),
				Vector3(bf * 0.085, 0.22, 0.22 - absf(bf) * 0.025),
				Vector3(64.0, bf * 18.0, 18.0 + bf * 12.0)
			)
		# Back & Side Hero Hair Spikes
		for side_dir in [-1, 1]:
			var sf: float = float(side_dir)
			_add_part(head_pivot, _make_capsule(0.058, 0.24, mat_hair), Vector3(sf * 0.29, 0.12, -0.02), Vector3(0.0, 0.0, -sf * 42.0))
			_add_part(head_pivot, _make_capsule(0.055, 0.22, mat_hair), Vector3(sf * 0.22, 0.10, -0.24), Vector3(-35.0, sf * 25.0, -sf * 28.0))
		# Royal 5-Peak Golden Olympus Crown with Jewels
		_add_part(head_pivot, _make_cyl(0.235, 0.215, 0.12, mat_gold), Vector3(0.0, 0.31, 0.0))
		for peak_i in range(5):
			var ang: float = float(peak_i) * TAU / 5.0
			_add_part(head_pivot, _make_cyl(0.01, 0.05, 0.14, mat_gold), Vector3(sin(ang) * 0.205, 0.40, cos(ang) * 0.205))
			_add_part(
				head_pivot,
				_make_sphere(0.026, 0.052, mat_sapphire if (peak_i % 2 == 0) else mat_ruby),
				Vector3(sin(ang) * 0.205, 0.475, cos(ang) * 0.205)
			)
		_add_part(head_pivot, _make_sphere(0.054, 0.108, mat_sapphire), Vector3(0.0, 0.32, 0.228))

	# =========================================================
	# 4. LEFT & RIGHT ARMS + GAUNTLETS & HANDS
	# =========================================================
	var arm_names := ["LeftArmPivot", "RightArmPivot"]
	var arm_xs := [-0.36, 0.36]
	for a_i in range(2):
		var arm_pivot := Node3D.new()
		arm_pivot.name = arm_names[a_i]
		arm_pivot.position = Vector3(arm_xs[a_i], 0.80, 0.0)
		root.add_child(arm_pivot)

		var sleeve_mat: StandardMaterial3D = mat_skin if char_style == "salma_girl" else mat_primary
		_add_part(arm_pivot, _make_capsule(0.086, 0.35, sleeve_mat), Vector3(0.0, -0.14, 0.0))
		_add_part(arm_pivot, _make_cyl(0.092, 0.086, 0.09, mat_gold), Vector3(0.0, -0.23, 0.0))
		_add_part(arm_pivot, _make_sphere(0.088, 0.172, mat_skin), Vector3(0.0, -0.32, 0.0))

	# =========================================================
	# 5. LEFT & RIGHT LEGS + DETAILED 3D FOOTWEAR
	# =========================================================
	var leg_names := ["LeftLegPivot", "RightLegPivot"]
	var leg_xs := [-0.15, 0.15]
	for l_i in range(2):
		var sf: float = -1.0 if l_i == 0 else 1.0
		var leg_pivot := Node3D.new()
		leg_pivot.name = leg_names[l_i]
		leg_pivot.position = Vector3(leg_xs[l_i], 0.38, 0.0)
		root.add_child(leg_pivot)

		var leg_mat: StandardMaterial3D = mat_white if char_style == "salma_girl" else mat_navy
		_add_part(leg_pivot, _make_capsule(0.098, 0.34, leg_mat), Vector3(0.0, -0.14, 0.0))
		# Golden Knee Guard / Sock Band
		_add_part(leg_pivot, _make_cyl(0.104, 0.102, 0.045, mat_gold), Vector3(0.0, -0.18, 0.0))
		# Sculpted Two-Tone Sneaker / Mary-Jane Shoe
		_add_part(leg_pivot, _make_capsule(0.105, 0.30, mat_primary), Vector3(0.0, -0.30, 0.045), Vector3(90.0, 0.0, 0.0))
		_add_part(leg_pivot, _make_sphere(0.098, 0.18, mat_white if char_style != "salma_girl" else mat_gold), Vector3(0.0, -0.30, 0.135))
		_add_part(leg_pivot, _make_cyl(0.112, 0.118, 0.048, mat_white if char_style != "ahmed_kid2" else mat_sapphire), Vector3(0.0, -0.36, 0.045), Vector3.ZERO, Vector3(1.0, 1.0, 1.38))
		if char_style == "ahmed_hero":
			# Mini Golden Hermes Ankle Wings on Hero Ahmed's boots!
			_add_part(leg_pivot, _make_capsule(0.025, 0.16, mat_gold), Vector3(sf * 0.11, -0.26, -0.02), Vector3(-25.0, 0.0, -sf * 25.0))

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

	if horiz_speed > 0.35:
		var run_phase: float = anim_time * (9.5 + clampf(horiz_speed * 0.50, 0.0, 5.0))
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

			var island_top: int = 54 + int(sin(float(x) * 0.08) * 3.0)
			if x > 24 and x < 136 and y >= island_top:
				var half_w: int = 56 - int(float(y - island_top) * 1.25)
				if abs(x - 80) <= half_w:
					if y < island_top + 7:
						c = prim_col.lightened(0.18)
					else:
						c = prim_col.darkened(0.38)
					if abs(x - 80) <= 6 and y >= island_top + 4:
						c = Color(0.0, 0.90, 1.0)

			if y >= 32 and y < island_top and x >= 56 and x <= 104:
				if y < 39 and abs(x - 80) <= (y - 31) * 3:
					c = acc_col
				elif y >= 39 and ((x - 56) % 14 < 5):
					c = Color(0.96, 0.98, 1.0)

			img.set_pixel(x, y, c)
	return ImageTexture.create_from_image(img)

static func make_character_portrait_texture(skin_id: int) -> ImageTexture:
	var idx: int = clampi(skin_id % 3, 0, 2)
	var skin: Dictionary = SKIN_COLORS[idx]
	var prim_col: Color = skin["primary"]
	var sec_col: Color = skin["secondary"]
	var char_style: String = str(skin["style"])
	var w: int = 128
	var h: int = 128
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	var gold := Color(1.0, 0.84, 0.18)
	var face_col := Color(1.0, 0.85, 0.74)

	for y in range(h):
		var ty: float = float(y) / float(h)
		for x in range(w):
			var dx: float = float(x - 64)
			var dy: float = float(y - 64)
			var dist: float = sqrt(dx * dx + dy * dy)
			var c: Color = prim_col.darkened(0.55).lerp(sec_col.darkened(0.35), ty)
			if dist < 58.0 and dist > 52.0:
				c = gold
			elif dist < 52.0:
				c = c.lerp(prim_col, 0.30)

			# Salma Twin Pigtails & Pink Bows on Portrait
			if char_style == "salma_girl":
				var lp: float = sqrt(float((x - 35) * (x - 35) + (y - 62) * (y - 62)))
				var rp: float = sqrt(float((x - 93) * (x - 93) + (y - 62) * (y - 62)))
				if lp <= 14.0 or rp <= 14.0:
					c = Color(0.24, 0.09, 0.14)
				var lbow: float = sqrt(float((x - 38) * (x - 38) + (y - 46) * (y - 46)))
				var rbow: float = sqrt(float((x - 90) * (x - 90) + (y - 46) * (y - 46)))
				if lbow <= 7.0 or rbow <= 7.0:
					c = Color(1.0, 0.45, 0.76)

			# Hero Torso & Golden Emblem
			if y >= 78 and y <= 116 and abs(x - 64) <= 28:
				c = prim_col
				if y <= 85:
					c = gold
				elif abs(x - 64) <= 7 and abs(y - 96) <= 7:
					c = Color(1.0, 0.18, 0.42) if char_style == "salma_girl" else Color(0.0, 0.88, 1.0)

			# Hero Face Circle
			var h_dist: float = sqrt(float((x - 64) * (x - 64) + (y - 56) * (y - 56)))
			if h_dist <= 22.0:
				c = face_col
				var le: float = sqrt(float((x - 55) * (x - 55) + (y - 55) * (y - 55)))
				var re: float = sqrt(float((x - 73) * (x - 73) + (y - 55) * (y - 55)))
				if le <= 4.8 or re <= 4.8:
					c = Color(0.68, 0.18, 0.88) if char_style == "salma_girl" else Color(0.06, 0.24, 0.56)
				if le <= 2.6 or re <= 2.6:
					c = Color(0.05, 0.08, 0.16)
				var lh: float = sqrt(float((x - 53) * (x - 53) + (y - 53) * (y - 53)))
				var rh: float = sqrt(float((x - 71) * (x - 71) + (y - 53) * (y - 53)))
				if lh <= 1.8 or rh <= 1.8:
					c = Color.WHITE

			# Kid Cap or Golden Crown above Head
			if char_style == "ahmed_kid2":
				if y >= 25 and y <= 40 and abs(x - 64) <= 23:
					c = sec_col if y < 35 else gold
			else:
				if y >= 22 and y <= 37 and abs(x - 64) <= 20:
					var crown_wave: int = 22 + int(abs(((x - 44) % 10) - 5) * 2)
					if y >= crown_wave:
						c = gold
						if abs(x - 64) <= 4 and abs(y - 31) <= 4:
							c = Color(1.0, 0.18, 0.38) if char_style == "salma_girl" else Color(0.0, 0.90, 1.0)

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

static func load_asset_texture(res_path: String, fallback_tex: Texture2D = null) -> Texture2D:
	if ResourceLoader.exists(res_path):
		var loaded = load(res_path)
		if loaded is Texture2D:
			return loaded
	var img := Image.new()
	if img.load(res_path) == OK:
		return ImageTexture.create_from_image(img)
	return fallback_tex

static func apply_art_card_button(
	btn: Button,
	art_tex: Texture2D,
	title_text: String,
	border_col: Color,
	font_size: int = 17,
	corner_radius: int = 16,
	scrim_alpha: float = 0.44
) -> void:
	btn.clip_contents = true
	btn.text = ""
	style_button(btn, Color(0.05, 0.10, 0.24, 0.96), border_col, Color.WHITE, font_size, corner_radius, 3)

	var old_bg: Node = btn.get_node_or_null("CardArtBg")
	if is_instance_valid(old_bg):
		old_bg.queue_free()
	var old_scrim: Node = btn.get_node_or_null("CardScrim")
	if is_instance_valid(old_scrim):
		old_scrim.queue_free()
	var old_lbl: Node = btn.get_node_or_null("CardLabel")
	if is_instance_valid(old_lbl):
		old_lbl.queue_free()

	if art_tex != null:
		var bg := TextureRect.new()
		bg.name = "CardArtBg"
		bg.texture = art_tex
		bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
		bg.offset_left = 3.0
		bg.offset_top = 3.0
		bg.offset_right = -3.0
		bg.offset_bottom = -3.0
		bg.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		bg.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		bg.mouse_filter = Control.MOUSE_FILTER_IGNORE
		btn.add_child(bg)

	var scrim := ColorRect.new()
	scrim.name = "CardScrim"
	scrim.color = Color(0.02, 0.05, 0.16, scrim_alpha)
	scrim.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	scrim.offset_left = 3.0
	scrim.offset_top = 3.0
	scrim.offset_right = -3.0
	scrim.offset_bottom = -3.0
	scrim.mouse_filter = Control.MOUSE_FILTER_IGNORE
	btn.add_child(scrim)

	var lbl := Label.new()
	lbl.name = "CardLabel"
	lbl.text = title_text
	lbl.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	lbl.offset_left = 8.0
	lbl.offset_right = -8.0
	lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	lbl.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
	lbl.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	lbl.mouse_filter = Control.MOUSE_FILTER_IGNORE
	lbl.add_theme_font_size_override("font_size", font_size)
	lbl.add_theme_color_override("font_color", Color.WHITE)
	lbl.add_theme_color_override("font_outline_color", Color(0.02, 0.04, 0.12, 0.98))
	lbl.add_theme_constant_override("outline_size", 5)
	btn.add_child(lbl)

static func set_art_card_text(btn: Button, new_text: String) -> void:
	var lbl: Label = btn.get_node_or_null("CardLabel")
	if is_instance_valid(lbl):
		lbl.text = new_text
	else:
		btn.text = new_text

static func make_platform_tile_texture(base_col: Color, accent_col: Color) -> ImageTexture:
	var s: int = 64
	var img := Image.create(s, s, false, Image.FORMAT_RGBA8)
	var light_c: Color = base_col.lightened(0.26)
	var mid_c: Color = base_col
	var dark_c: Color = base_col.darkened(0.32)
	var gold_c: Color = accent_col.lerp(Color.WHITE, 0.30)

	for y in range(s):
		for x in range(s):
			var gx: int = x % 32
			var gy: int = y % 32
			var tile_alt: bool = ((int(x / 32) + int(y / 32)) % 2 == 0)
			var c: Color = light_c if tile_alt else mid_c
			if gx <= 1 or gy <= 1 or gx == 31 or gy == 31:
				c = dark_c
			elif gx == 2 or gy == 2:
				c = c.lightened(0.24)
			elif gx == 29 or gy == 29:
				c = c.lerp(gold_c, 0.35)
			var dx: int = abs(gx - 16)
			var dy: int = abs(gy - 16)
			if dx + dy <= 7:
				c = c.lerp(gold_c, 0.55 if (dx + dy <= 3) else 0.28)
			img.set_pixel(x, y, c)
	return ImageTexture.create_from_image(img)

static func make_finish_checker_texture(accent_col: Color) -> ImageTexture:
	var s: int = 64
	var img := Image.create(s, s, false, Image.FORMAT_RGBA8)
	var c1 := Color(0.98, 0.99, 1.0)
	var c2 := accent_col
	for y in range(s):
		for x in range(s):
			var is_even: bool = ((int(x / 16) + int(y / 16)) % 2 == 0)
			var c: Color = c1 if is_even else c2
			if x % 16 == 0 or y % 16 == 0:
				c = c.darkened(0.16)
			img.set_pixel(x, y, c)
	return ImageTexture.create_from_image(img)

static func make_textured_platform_mat(col: Color, acc: Color, is_finish: bool = false) -> StandardMaterial3D:
	var cache_key: String = "%s_%s_%s" % [col.to_html(false), acc.to_html(false), "1" if is_finish else "0"]
	if _platform_mat_cache.has(cache_key):
		return _platform_mat_cache[cache_key]
	var m := StandardMaterial3D.new()
	m.albedo_color = Color.WHITE
	m.albedo_texture = make_finish_checker_texture(acc) if is_finish else make_platform_tile_texture(col, acc)
	m.uv1_triplanar = true
	m.uv1_scale = Vector3(0.32, 0.32, 0.32)
	m.roughness = 0.20 if is_finish else 0.25
	m.metallic = 0.28 if is_finish else 0.16
	m.specular_mode = BaseMaterial3D.SPECULAR_SCHLICK_GGX
	if is_finish:
		m.emission_enabled = true
		m.emission = acc
		m.emission_energy_multiplier = 0.16
	_platform_mat_cache[cache_key] = m
	return m

static func build_blob_shadow() -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	mi.name = "BlobShadow"
	var cyl := CylinderMesh.new()
	cyl.top_radius = 0.46
	cyl.bottom_radius = 0.46
	cyl.height = 0.02
	var mat := StandardMaterial3D.new()
	mat.shading_mode = BaseMaterial3D.SHADING_MODE_UNSHADED
	mat.albedo_color = Color(0.02, 0.05, 0.14, 0.42)
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	cyl.material = mat
	mi.mesh = cyl
	mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF
	return mi
