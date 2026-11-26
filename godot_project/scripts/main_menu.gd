extends Node3D

## MYTHORIA games — By ahmed
## 3D Interactive Main Menu Showcase with the 3 Exclusive Heroes:
##   0: Ahmed (البطل أحمد)
##   1: Salma (البطلة سلمى)
##   2: Ahmed (أحمد الصغير)
## All 3 Heroes stand together on the 3D Royal Podiums & swap live when selected!

const CVB := preload("res://scripts/character_visual_builder.gd")

@onready var showcase_world: Node3D = $ShowcaseWorld
@onready var hero_pedestal_pivot: Node3D = $ShowcaseWorld/HeroPedestalPivot
@onready var top_left_profile: Panel = $UI/Root/TopLeftProfile
@onready var profile_label: Label = $UI/Root/TopLeftProfile/ProfileLabel
@onready var top_center_logo: Panel = $UI/Root/TopCenterLogo
@onready var coins_badge: Button = $UI/Root/TopRightBar/CoinsBadge
@onready var stars_badge: Button = $UI/Root/TopRightBar/StarsBadge
@onready var mode_button: Button = $UI/Root/TopRightBar/ModeButton
@onready var lang_button: Button = $UI/Root/TopRightBar/LangButton
@onready var missions_quick_btn: Button = $UI/Root/LeftQuickDock/MissionsQuickBtn
@onready var daily_gift_quick_btn: Button = $UI/Root/LeftQuickDock/DailyGiftQuickBtn
@onready var world_graph_quick_btn: Button = $UI/Root/LeftQuickDock/WorldGraphQuickBtn
@onready var big_play_btn: Button = $UI/Root/RightMainDock/BigPlayButton
@onready var levels_btn: Button = $UI/Root/RightMainDock/LevelsButton
@onready var characters_btn: Button = $UI/Root/RightMainDock/CharactersButton
@onready var shop_btn: Button = $UI/Root/RightMainDock/ShopButton
@onready var settings_btn: Button = $UI/Root/RightMainDock/SettingsButton
@onready var prev_skin_btn: Button = $UI/Root/HeroPedestalControls/PrevSkinBtn
@onready var next_skin_btn: Button = $UI/Root/HeroPedestalControls/NextSkinBtn
@onready var pedestal_badge: Panel = $UI/Root/HeroPedestalControls/PedestalBadge
@onready var pedestal_label: Label = $UI/Root/HeroPedestalControls/PedestalBadge/PedestalLabel

@onready var drawer_modal: Panel = $UI/Root/DrawerModal
@onready var drawer_title: Label = $UI/Root/DrawerModal/DrawerHeader/DrawerTitle
@onready var close_drawer_btn: Button = $UI/Root/DrawerModal/DrawerHeader/CloseDrawerBtn
@onready var world_scroll: ScrollContainer = $UI/Root/DrawerModal/WorldScroll
@onready var world_list: HBoxContainer = $UI/Root/DrawerModal/WorldScroll/WorldList
@onready var world_rules_label: Label = $UI/Root/DrawerModal/WorldRulesLabel
@onready var stage_grid: HBoxContainer = $UI/Root/DrawerModal/ContentScroll/StageGrid

var selected_world_id: int = 1
var preview_skin_id: int = 0
var current_tab: String = "WORLDS"
var showcase_time: float = 0.0
var hero_visual: Node3D = null
var companion_left_pivot: Node3D = null
var companion_right_pivot: Node3D = null
var companion_left_visual: Node3D = null
var companion_right_visual: Node3D = null
var hero_yaw_deg: float = 0.0
var bg_hammer_pivot: Node3D = null
var bg_runners: Array = []
var floating_clouds: Array = []
var sky_crystals: Array = []
var world_tex_cache: Dictionary = {}
var event_tex_cache: Dictionary = {}
var skin_tex_cache: Dictionary = {}

func _ready() -> void:
	DisplayServer.screen_set_orientation(DisplayServer.SCREEN_LANDSCAPE)
	AudioSynth.set_bgm_mode("menu")
	selected_world_id = clampi(int((GameState.current_level_number - 1) / 5) + 1, 1, GameState.TOTAL_WORLDS)
	preview_skin_id = clampi(GameState.selected_skin_id, 0, 2)
	_connect_all_buttons()
	_apply_ui_theme()
	_build_3d_showcase_world()
	_rebuild_hero_character(preview_skin_id)
	refresh_header()

func _connect_all_buttons() -> void:
	if not coins_badge.pressed.is_connected(_on_shop_tab_pressed):
		coins_badge.pressed.connect(_on_shop_tab_pressed)
	if not stars_badge.pressed.is_connected(_on_worlds_tab_pressed):
		stars_badge.pressed.connect(_on_worlds_tab_pressed)
	if not mode_button.pressed.is_connected(_on_mode_button_pressed):
		mode_button.pressed.connect(_on_mode_button_pressed)
	if is_instance_valid(lang_button) and not lang_button.pressed.is_connected(_on_lang_button_pressed):
		lang_button.pressed.connect(_on_lang_button_pressed)
	if not missions_quick_btn.pressed.is_connected(_on_missions_tab_pressed):
		missions_quick_btn.pressed.connect(_on_missions_tab_pressed)
	if not daily_gift_quick_btn.pressed.is_connected(_on_daily_gift_pressed):
		daily_gift_quick_btn.pressed.connect(_on_daily_gift_pressed)
	if not world_graph_quick_btn.pressed.is_connected(_on_worlds_tab_pressed):
		world_graph_quick_btn.pressed.connect(_on_worlds_tab_pressed)
	if not big_play_btn.pressed.is_connected(_on_play_button_pressed):
		big_play_btn.pressed.connect(_on_play_button_pressed)
	if not levels_btn.pressed.is_connected(_on_worlds_tab_pressed):
		levels_btn.pressed.connect(_on_worlds_tab_pressed)
	if not characters_btn.pressed.is_connected(_on_characters_tab_pressed):
		characters_btn.pressed.connect(_on_characters_tab_pressed)
	if not shop_btn.pressed.is_connected(_on_shop_tab_pressed):
		shop_btn.pressed.connect(_on_shop_tab_pressed)
	if not settings_btn.pressed.is_connected(_on_settings_tab_pressed):
		settings_btn.pressed.connect(_on_settings_tab_pressed)
	if not prev_skin_btn.pressed.is_connected(_on_prev_skin_pressed):
		prev_skin_btn.pressed.connect(_on_prev_skin_pressed)
	if not next_skin_btn.pressed.is_connected(_on_next_skin_pressed):
		next_skin_btn.pressed.connect(_on_next_skin_pressed)
	if not close_drawer_btn.pressed.is_connected(_on_close_drawer_pressed):
		close_drawer_btn.pressed.connect(_on_close_drawer_pressed)

func _get_world_banner_tex(w_id: int) -> Texture2D:
	var cid: int = clampi(w_id, 1, GameState.TOTAL_WORLDS)
	if not world_tex_cache.has(cid):
		var wt: Dictionary = GameState.get_world_theme(cid)
		var fallback := CVB.make_world_banner_texture(wt["sky_top"], wt["sky_bottom"], wt["primary"], wt["accent"])
		var path: String = "res://assets/art_world_%d.png" % cid
		world_tex_cache[cid] = CVB.load_asset_texture(path, fallback)
	return world_tex_cache[cid]

func _get_event_art_tex(key: String) -> Texture2D:
	if not event_tex_cache.has(key):
		var path_map := {
			"chest": "res://assets/art_event_chest.png",
			"missions": "res://assets/art_event_missions.png",
			"coins": "res://assets/art_event_coins.png",
			"speed": "res://assets/art_event_speed.png",
			"trail": "res://assets/art_shop_trail.png",
			"heroes": "res://assets/art_heroes_banner.png"
		}
		var path: String = str(path_map.get(key, "res://assets/art_event_missions.png"))
		event_tex_cache[key] = CVB.load_asset_texture(path, _get_world_banner_tex(1))
	return event_tex_cache[key]

func _get_skin_portrait_tex(s_id: int) -> ImageTexture:
	var cid: int = clampi(s_id % 3, 0, 2)
	if not skin_tex_cache.has(cid):
		skin_tex_cache[cid] = CVB.make_character_portrait_texture(cid)
	return skin_tex_cache[cid]

func _apply_ui_theme() -> void:
	top_left_profile.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.06, 0.14, 0.36, 0.90), Color(1.0, 0.82, 0.18, 1.0), 18, 3, 6)
	)
	top_center_logo.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.05, 0.11, 0.28, 0.88), Color(1.0, 0.82, 0.18, 1.0), 22, 4, 8)
	)
	pedestal_badge.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.05, 0.11, 0.30, 0.92), Color(1.0, 0.82, 0.18, 1.0), 18, 3, 6)
	)
	drawer_modal.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.04, 0.09, 0.24, 0.96), Color(1.0, 0.82, 0.18, 1.0), 22, 4, 12)
	)

	CVB.style_button(coins_badge, Color(0.10, 0.22, 0.52, 0.95), Color(1.0, 0.82, 0.18), Color(1.0, 0.92, 0.35), 16, 18, 3)
	CVB.style_button(stars_badge, Color(0.10, 0.22, 0.52, 0.95), Color(0.0, 0.86, 1.0), Color(0.75, 0.96, 1.0), 16, 18, 3)
	CVB.style_button(mode_button, Color(0.16, 0.30, 0.68, 0.95), Color(1.0, 0.82, 0.18), Color.WHITE, 15, 18, 3)
	if is_instance_valid(lang_button):
		CVB.style_button(lang_button, Color(0.12, 0.52, 0.38, 0.95), Color(0.45, 1.0, 0.78), Color.WHITE, 16, 18, 3)

	CVB.apply_art_card_button(missions_quick_btn, _get_event_art_tex("missions"), GameState.tr_text("🏆 EVENTS & MISSIONS", "🏆 المهام والأحداث"), Color(1.0, 0.84, 0.22), 16, 18, 0.34)
	CVB.apply_art_card_button(daily_gift_quick_btn, _get_event_art_tex("chest"), GameState.tr_text("🎁 DAILY TREASURE", "🎁 الكنز اليومي"), Color(1.0, 0.78, 0.28), 16, 18, 0.32)
	CVB.apply_art_card_button(world_graph_quick_btn, _get_world_banner_tex(3), GameState.tr_text("⚡ 10 WORLDS MAP", "⚡ خريطة العوالم 10"), Color(0.0, 0.88, 1.0), 16, 18, 0.34)

	CVB.apply_art_card_button(big_play_btn, _get_world_banner_tex(selected_world_id), GameState.tr_text("▶ PLAY (LV.1)", "▶ العب الآن"), Color(1.0, 0.92, 0.32), 22, 20, 0.26)
	CVB.apply_art_card_button(levels_btn, _get_world_banner_tex(1), GameState.tr_text("🌍 LEVELS & WORLDS", "🌍 المراحل والعوالم"), Color(0.42, 0.88, 1.0), 17, 16, 0.38)
	CVB.apply_art_card_button(characters_btn, _get_event_art_tex("heroes"), GameState.tr_text("👑 3 HEROES: SALMA & AHMED", "👑 الأبطال 3: سلمى وأحمد"), Color(1.0, 0.84, 0.22), 16, 16, 0.36)
	CVB.apply_art_card_button(shop_btn, _get_event_art_tex("speed"), GameState.tr_text("🛒 MYTHORIA SHOP", "🛒 متجر ميثوريا"), Color(1.0, 0.82, 0.24), 17, 16, 0.36)
	CVB.apply_art_card_button(settings_btn, _get_world_banner_tex(5), GameState.tr_text("⚙ SETTINGS & AUDIO", "⚙ الإعدادات والصوت"), Color(0.65, 0.86, 1.0), 16, 16, 0.44)

	CVB.style_button(prev_skin_btn, Color(1.0, 0.70, 0.08, 0.96), Color(1.0, 0.92, 0.48), Color.WHITE, 22, 16, 3)
	CVB.style_button(next_skin_btn, Color(1.0, 0.70, 0.08, 0.96), Color(1.0, 0.92, 0.48), Color.WHITE, 22, 16, 3)
	CVB.style_button(close_drawer_btn, Color(0.82, 0.18, 0.24, 0.96), Color(1.0, 0.75, 0.75), Color.WHITE, 17, 14, 3)

func _build_3d_showcase_world() -> void:
	var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.18, 0.88, Color(1.0, 0.72, 0.10), 0.28)
	var mat_royal_blue := CVB.make_mat(Color(0.10, 0.36, 0.82), 0.26, 0.28)
	var mat_cyan_glow := CVB.make_mat(Color(0.0, 0.88, 1.0), 0.14, 0.30, Color(0.0, 0.78, 1.0), 0.55)
	var mat_marble := CVB.make_textured_platform_mat(Color(0.92, 0.95, 1.0), Color(1.0, 0.84, 0.22), false)
	var mat_grass := CVB.make_mat(Color(0.22, 0.80, 0.36), 0.44, 0.04)
	var mat_rock := CVB.make_mat(Color(0.54, 0.42, 0.34), 0.65, 0.04)
	var mat_flame := CVB.make_mat(Color(1.0, 0.56, 0.08), 0.15, 0.10, Color(1.0, 0.62, 0.10), 0.58)

	# 1. Center Hero Pedestal (Multi-tiered Royal Blue, Cyan & Gold Podium)
	_add_cyl(showcase_world, Vector3(0.0, -0.35, 0.0), 2.35, 2.55, 0.42, mat_royal_blue)
	_add_cyl(showcase_world, Vector3(0.0, -0.08, 0.0), 1.92, 2.08, 0.18, mat_gold)
	_add_cyl(showcase_world, Vector3(0.0, 0.04, 0.0), 1.58, 1.66, 0.12, mat_cyan_glow)
	_add_cyl(showcase_world, Vector3(0.0, 0.12, 0.0), 1.42, 1.48, 0.08, mat_marble)

	# 1b. Left & Right Companion Podiums for the other 2 Heroes!
	for side_dir in [-1, 1]:
		var px: float = float(side_dir) * 2.22
		var pz: float = -0.82
		_add_cyl(showcase_world, Vector3(px, -0.32, pz), 1.02, 1.15, 0.32, mat_royal_blue)
		_add_cyl(showcase_world, Vector3(px, -0.10, pz), 0.86, 0.94, 0.12, mat_gold)
		_add_cyl(showcase_world, Vector3(px, -0.02, pz), 0.76, 0.80, 0.06, mat_marble)

	companion_left_pivot = Node3D.new()
	companion_left_pivot.position = Vector3(-2.22, 0.02, -0.82)
	showcase_world.add_child(companion_left_pivot)

	companion_right_pivot = Node3D.new()
	companion_right_pivot.position = Vector3(2.22, 0.02, -0.82)
	showcase_world.add_child(companion_right_pivot)

	# Twin Golden Fire Braziers flanking the Hero Pedestal
	for side_dir in [-1, 1]:
		var bx: float = float(side_dir) * 2.85
		_add_box(showcase_world, Vector3(bx, -0.05, 0.45), Vector3(0.54, 0.52, 0.54), mat_marble)
		_add_cyl(showcase_world, Vector3(bx, 0.30, 0.45), 0.36, 0.22, 0.22, mat_gold)
		_add_sphere(showcase_world, Vector3(bx, 0.52, 0.45), 0.22, 0.42, mat_flame)

	# 2. Winding Background Obstacle Bridge & Distant Finish Arch
	var bridge_colors := [
		Color(0.14, 0.56, 0.98),
		Color(1.0, 0.76, 0.12),
		Color(0.92, 0.22, 0.58),
		Color(0.18, 0.82, 0.52)
	]
	for seg_i in range(6):
		var col_mat := CVB.make_textured_platform_mat(bridge_colors[seg_i % bridge_colors.size()], Color(1.0, 0.84, 0.22), seg_i == 5)
		var bz: float = -3.8 - float(seg_i) * 3.1
		var bx: float = sin(float(seg_i) * 0.8) * 1.1
		_add_box(showcase_world, Vector3(bx, -0.35, bz), Vector3(3.6, 0.48, 2.7), col_mat)

	bg_hammer_pivot = Node3D.new()
	bg_hammer_pivot.position = Vector3(0.6, 0.45, -8.2)
	showcase_world.add_child(bg_hammer_pivot)
	_add_cyl(bg_hammer_pivot, Vector3(0.0, 0.0, 0.0), 0.28, 0.34, 1.2, mat_gold)
	_add_box(bg_hammer_pivot, Vector3(0.0, 0.25, 0.0), Vector3(4.0, 0.32, 0.32), CVB.make_mat(Color(0.92, 0.20, 0.22), 0.28, 0.20))

	_add_cyl(showcase_world, Vector3(-1.45, 0.9, -18.5), 0.24, 0.24, 2.8, mat_marble)
	_add_cyl(showcase_world, Vector3(1.45, 0.9, -18.5), 0.24, 0.24, 2.8, mat_marble)
	_add_box(showcase_world, Vector3(0.0, 2.4, -18.5), Vector3(3.6, 0.55, 0.55), mat_gold)
	_add_box(showcase_world, Vector3(0.0, 1.0, -18.5), Vector3(2.5, 2.2, 0.12), mat_cyan_glow)

	# 3. Left & Right Floating Temple Sky Islands with Waterfalls & Sky Crystals
	for side_dir in [-1, 1]:
		var side_f: float = float(side_dir)
		var ix: float = side_f * 6.2
		var iz: float = -9.5
		_add_cyl(showcase_world, Vector3(ix, -0.4, iz), 3.2, 1.6, 2.4, mat_rock)
		_add_cyl(showcase_world, Vector3(ix, 0.95, iz), 3.4, 3.2, 0.45, mat_grass)
		_add_box(showcase_world, Vector3(ix - side_f * 1.6, -0.8, iz + 1.8), Vector3(0.85, 3.2, 0.22), mat_cyan_glow)
		for col_i in range(3):
			var cx: float = ix + float(col_i - 1) * 1.05
			_add_cyl(showcase_world, Vector3(cx, 2.15, iz), 0.22, 0.25, 2.0, mat_marble)
		_add_box(showcase_world, Vector3(ix, 3.3, iz), Vector3(3.1, 0.45, 1.8), mat_marble)
		_add_cyl(showcase_world, Vector3(ix, 4.1, iz), 0.05, 1.45, 1.2, mat_gold)

		var crystal := _add_cyl(showcase_world, Vector3(ix - side_f * 2.4, 3.2, iz + 2.5), 0.02, 0.45, 1.1, mat_cyan_glow)
		sky_crystals.append(crystal)

	var cloud_mat := CVB.make_mat(Color(0.98, 0.99, 1.0, 0.92), 0.65, 0.0)
	var cloud_coords := [
		Vector3(-8.5, 4.6, -14.0),
		Vector3(8.2, 5.1, -15.0),
		Vector3(-3.8, 6.0, -19.0),
		Vector3(4.5, 5.6, -17.5)
	]
	for cc in cloud_coords:
		var c_node := Node3D.new()
		c_node.position = cc
		_add_sphere(c_node, Vector3(0.0, 0.0, 0.0), 1.1, 1.4, cloud_mat)
		_add_sphere(c_node, Vector3(0.95, -0.15, 0.1), 0.85, 1.1, cloud_mat)
		_add_sphere(c_node, Vector3(-0.90, -0.18, -0.1), 0.80, 1.05, cloud_mat)
		showcase_world.add_child(c_node)
		floating_clouds.append(c_node)

func _add_box(parent: Node3D, pos: Vector3, sz: Vector3, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var bm := BoxMesh.new()
	bm.size = sz
	bm.material = mat
	mi.mesh = bm
	mi.position = pos
	parent.add_child(mi)
	return mi

func _add_cyl(parent: Node3D, pos: Vector3, top_r: float, bot_r: float, h: float, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var cm := CylinderMesh.new()
	cm.top_radius = top_r
	cm.bottom_radius = bot_r
	cm.height = h
	cm.material = mat
	mi.mesh = cm
	mi.position = pos
	parent.add_child(mi)
	return mi

func _add_sphere(parent: Node3D, pos: Vector3, r: float, h: float, mat: Material) -> MeshInstance3D:
	var mi := MeshInstance3D.new()
	var sm := SphereMesh.new()
	sm.radius = r
	sm.height = h
	sm.material = mat
	mi.mesh = sm
	mi.position = pos
	parent.add_child(mi)
	return mi

func _rebuild_hero_character(skin_id_to_show: int = -1) -> void:
	for child in hero_pedestal_pivot.get_children():
		child.queue_free()
	var sid: int = clampi((GameState.selected_skin_id if skin_id_to_show < 0 else skin_id_to_show) % 3, 0, 2)
	preview_skin_id = sid
	hero_visual = CVB.build_character_visual(sid)
	hero_visual.position = Vector3(0.0, 0.16, 0.0)
	var base_sc: float = 1.15 if sid == 2 else 1.24
	hero_visual.scale = Vector3(base_sc, base_sc, base_sc)
	hero_pedestal_pivot.add_child(hero_visual)

	# Place the other 2 heroes on the Left & Right Companion Podiums so all 3 heroes are always visible!
	var left_id: int = (sid + 1) % 3
	var right_id: int = (sid + 2) % 3
	if is_instance_valid(companion_left_pivot):
		for ch in companion_left_pivot.get_children():
			ch.queue_free()
		companion_left_visual = CVB.build_character_visual(left_id)
		var l_sc: float = 0.88 if left_id == 2 else 0.94
		companion_left_visual.scale = Vector3(l_sc, l_sc, l_sc)
		companion_left_visual.rotation.y = deg_to_rad(24.0)
		companion_left_pivot.add_child(companion_left_visual)

	if is_instance_valid(companion_right_pivot):
		for ch in companion_right_pivot.get_children():
			ch.queue_free()
		companion_right_visual = CVB.build_character_visual(right_id)
		var r_sc: float = 0.88 if right_id == 2 else 0.94
		companion_right_visual.scale = Vector3(r_sc, r_sc, r_sc)
		companion_right_visual.rotation.y = deg_to_rad(-24.0)
		companion_right_pivot.add_child(companion_right_visual)

func _input(event: InputEvent) -> void:
	if event is InputEventScreenDrag:
		if not drawer_modal.visible or event.position.y < 335.0:
			hero_yaw_deg += event.relative.x * 0.45

func _process(delta: float) -> void:
	showcase_time += delta

	if is_instance_valid(hero_visual):
		hero_visual.rotation.y = deg_to_rad(hero_yaw_deg) + sin(showcase_time * 1.1) * 0.28
		CVB.animate_character(hero_visual, showcase_time, Vector3.ZERO, true, 0.0, false)

	if is_instance_valid(companion_left_visual):
		companion_left_visual.rotation.y = deg_to_rad(24.0) + sin(showcase_time * 1.4 + 1.0) * 0.22
		CVB.animate_character(companion_left_visual, showcase_time + 0.6, Vector3.ZERO, true, 0.0, true)

	if is_instance_valid(companion_right_visual):
		companion_right_visual.rotation.y = deg_to_rad(-24.0) + cos(showcase_time * 1.3) * 0.22
		CVB.animate_character(companion_right_visual, showcase_time + 1.2, Vector3.ZERO, true, 0.0, false)

	if is_instance_valid(bg_hammer_pivot):
		bg_hammer_pivot.rotation.y += delta * 2.2

	for i in range(floating_clouds.size()):
		var cl: Node3D = floating_clouds[i]
		if is_instance_valid(cl):
			cl.position.x += sin(showcase_time * 0.4 + float(i)) * delta * 0.25

	for i in range(sky_crystals.size()):
		var cr: Node3D = sky_crystals[i]
		if is_instance_valid(cr):
			cr.rotation.y += delta * 1.8
			cr.position.y = 3.2 + sin(showcase_time * 2.0 + float(i)) * 0.18

func refresh_header() -> void:
	var skin_name: String = GameState.get_skin_display_name(preview_skin_id)
	var equipped_name: String = GameState.get_skin_display_name(GameState.selected_skin_id)
	var unlocked_worlds: int = GameState.get_unlocked_worlds_count()
	var total_stars: int = GameState.get_total_stars()
	var lvl: int = GameState.current_level_number
	var w_id: int = int((lvl - 1) / 5) + 1
	var goal_sec: int = int(GameState.goal_time_ms_for_level(lvl) / 1000)
	var diff_prof: Dictionary = GameState.get_stage_difficulty_profile(lvl)
	var tier_txt: String = str(diff_prof["tier_name_ar"]) if GameState.is_arabic() else str(diff_prof["tier_name"])

	if GameState.is_arabic():
		profile_label.text = "👑 مستوى %d  |  %s\n🌍 العوالم المفتوحة: %d / %d" % [
			GameState.player_level,
			equipped_name,
			unlocked_worlds,
			GameState.TOTAL_WORLDS
		]
	else:
		profile_label.text = "👑 Lv.%d  |  %s\n🌍 Worlds Unlocked: %d / %d" % [
			GameState.player_level,
			equipped_name,
			unlocked_worlds,
			GameState.TOTAL_WORLDS
		]

	coins_badge.text = "🪙 %d +" % GameState.coins
	stars_badge.text = "★ %d / 150" % total_stars
	mode_button.text = GameState.tr_text("MODE: %s" % GameState.selected_game_mode, "الطور: %s" % GameState.selected_game_mode)
	if is_instance_valid(lang_button):
		lang_button.text = "🌐 EN" if GameState.is_arabic() else "🌐 عربي"
	close_drawer_btn.text = GameState.tr_text("✖ CLOSE", "✖ إغلاق")

	CVB.set_art_card_text(missions_quick_btn, GameState.tr_text("🏆 EVENTS & MISSIONS", "🏆 المهام والأحداث"))
	CVB.set_art_card_text(daily_gift_quick_btn, GameState.tr_text("🎁 DAILY TREASURE", "🎁 الكنز اليومي"))
	CVB.set_art_card_text(world_graph_quick_btn, GameState.tr_text("⚡ 10 WORLDS MAP", "⚡ خريطة العوالم 10"))
	CVB.set_art_card_text(big_play_btn, GameState.tr_text("▶ PLAY (LV.%d)\n%s" % [lvl, tier_txt], "▶ العب المرحلة %d\n%s" % [lvl, tier_txt]))
	CVB.set_art_card_text(levels_btn, GameState.tr_text("🌍 LEVELS & WORLDS", "🌍 المراحل والعوالم"))
	CVB.set_art_card_text(characters_btn, GameState.tr_text("👑 3 HEROES: SALMA & AHMED", "👑 الأبطال 3: سلمى وأحمد"))
	CVB.set_art_card_text(shop_btn, GameState.tr_text("🛒 MYTHORIA SHOP", "🛒 متجر ميثوريا"))
	CVB.set_art_card_text(settings_btn, GameState.tr_text("⚙ SETTINGS & AUDIO", "⚙ الإعدادات والصوت"))

	pedestal_label.text = "♛ MYTHORIA games — By ahmed\n%s • W%d S%d (%s • <=%ds)" % [
		skin_name,
		w_id,
		lvl,
		tier_txt,
		goal_sec
	]

func _set_drawer_layout(is_character_wardrobe: bool) -> void:
	if is_character_wardrobe:
		drawer_modal.offset_top = 325.0
		drawer_modal.offset_bottom = -14.0
		world_rules_label.offset_top = 62.0
		world_rules_label.offset_bottom = 90.0
		$UI/Root/DrawerModal/ContentScroll.offset_top = 94.0
	else:
		drawer_modal.offset_top = 82.0
		drawer_modal.offset_bottom = -18.0
		if world_scroll.visible:
			world_rules_label.offset_top = 186.0
			world_rules_label.offset_bottom = 216.0
			$UI/Root/DrawerModal/ContentScroll.offset_top = 222.0
		else:
			world_rules_label.offset_top = 74.0
			world_rules_label.offset_bottom = 108.0
			$UI/Root/DrawerModal/ContentScroll.offset_top = 114.0

func _open_drawer(tab_name: String) -> void:
	current_tab = tab_name
	drawer_modal.visible = true

	if tab_name == "WORLDS":
		drawer_title.text = GameState.tr_text("🌍 10 WORLDS — UNIQUE TRACK SHAPES & OBSTACLES", "🌍 العوالم الـ 10 — مسارات وعقبات بأشكال مختلفة")
		world_scroll.visible = true
		world_scroll.offset_bottom = 180.0
		_set_drawer_layout(false)
		rebuild_worlds_and_stages()
	elif tab_name == "CHARACTERS":
		drawer_title.text = GameState.tr_text("👑 CHOOSE YOUR HERO: AHMED • SALMA • LITTLE AHMED", "👑 اختر بطلك: البطل أحمد • البطلة سلمى • أحمد الصغير")
		world_scroll.visible = false
		_set_drawer_layout(true)
		rebuild_characters_tab()
	elif tab_name == "MISSIONS":
		drawer_title.text = GameState.tr_text("🏆 MYTHORIA EVENTS, MISSIONS & TREASURE CHEST", "🏆 مهام وأحداث ميثوريا وصندوق الكنز اليومي")
		world_scroll.visible = false
		_set_drawer_layout(false)
		rebuild_missions_tab()
	elif tab_name == "SHOP":
		drawer_title.text = GameState.tr_text("🛒 MYTHORIA ROYAL ARMORY & ABILITY UPGRADES", "🛒 متجر ميثوريا الملكي وترقيات القدرات")
		world_scroll.visible = false
		_set_drawer_layout(false)
		rebuild_shop_tab()
	elif tab_name == "SETTINGS":
		drawer_title.text = GameState.tr_text("⚙ SETTINGS, MUSIC, SOUND & LANGUAGE (AR / EN)", "⚙ الإعدادات والموسيقى والصوت واللغة (عربي / EN)")
		world_scroll.visible = false
		_set_drawer_layout(false)
		rebuild_settings_tab()
	refresh_header()

func rebuild_worlds_and_stages() -> void:
	for child in world_list.get_children():
		child.queue_free()
	for child in stage_grid.get_children():
		child.queue_free()

	for w in range(1, GameState.TOTAL_WORLDS + 1):
		var w_theme: Dictionary = GameState.get_world_theme(w)
		var unlocked: bool = GameState.is_world_unlocked(w)
		var btn := Button.new()
		btn.custom_minimum_size = Vector2(242, 94)
		var prefix: String = "🌍 " if unlocked else "🔒 "
		var prim_col: Color = w_theme["primary"]
		var border_col: Color = Color(1.0, 0.88, 0.22) if (w == selected_world_id) else (prim_col if unlocked else Color(0.35, 0.40, 0.54))
		var w_title: String = GameState.get_world_display_name(w)
		var track_info: String = str(w_theme.get("track_desc_ar", "")) if GameState.is_arabic() else str(w_theme.get("track_desc", ""))
		CVB.apply_art_card_button(
			btn,
			_get_world_banner_tex(w),
			"%sW%d: %s\n[%s]" % [prefix, w, w_title, track_info],
			border_col,
			14,
			16,
			0.28 if (w == selected_world_id) else (0.42 if unlocked else 0.68)
		)

		var captured_w: int = w
		btn.pressed.connect(func():
			AudioSynth.play_sfx("click")
			selected_world_id = captured_w
			rebuild_worlds_and_stages()
		)
		world_list.add_child(btn)

	var w_unlocked: bool = GameState.is_world_unlocked(selected_world_id)
	var gate_lvl: int = selected_world_id * 5
	var gate_goal_sec: int = int(GameState.goal_time_ms_for_level(gate_lvl) / 1000)
	var w_cur_theme: Dictionary = GameState.get_world_theme(selected_world_id)
	var cur_track_desc: String = str(w_cur_theme.get("track_desc_ar", "")) if GameState.is_arabic() else str(w_cur_theme.get("track_desc", ""))
	if w_unlocked:
		world_rules_label.text = GameState.tr_text(
			"✅ W%d (%s) — Clear Gate Stage %d within <= %ds to unlock Next World!" % [selected_world_id, cur_track_desc, gate_lvl, gate_goal_sec],
			"✅ العالم %d (%s) — أنهِ مرحلة البوابة %d خلال %d ثانية لفتح العالم التالي!" % [selected_world_id, cur_track_desc, gate_lvl, gate_goal_sec]
		)
		world_rules_label.modulate = Color(0.35, 0.98, 0.72)
	else:
		var prev_gate: int = (selected_world_id - 1) * 5
		var prev_goal_sec: int = int(GameState.goal_time_ms_for_level(prev_gate) / 1000)
		world_rules_label.text = GameState.tr_text(
			"🔒 WORLD %d LOCKED — Complete World %d Gateway Stage %d within <= %ds first!" % [selected_world_id, selected_world_id - 1, prev_gate, prev_goal_sec],
			"🔒 العالم %d مغلق — أنهِ مرحلة البوابة %d في العالم %d خلال %d ثانية أولاً!" % [selected_world_id, prev_gate, selected_world_id - 1, prev_goal_sec]
		)
		world_rules_label.modulate = Color(1.0, 0.50, 0.56)

	var first_lvl: int = (selected_world_id - 1) * 5 + 1
	var world_prim: Color = w_cur_theme["primary"]
	var banner_tex: Texture2D = _get_world_banner_tex(selected_world_id)

	for offset in range(5):
		var lvl: int = first_lvl + offset
		var lvl_unlocked: bool = GameState.is_level_unlocked(lvl)
		var stars: int = GameState.get_level_stars(lvl)
		var best_ms: int = GameState.get_level_best_time_ms(lvl)
		var goal_sec: int = int(GameState.goal_time_ms_for_level(lvl) / 1000)
		var is_gate: bool = ((offset + 1) == 5)
		var diff_prof: Dictionary = GameState.get_stage_difficulty_profile(lvl)
		var tier_col: Color = diff_prof["tier_color"]
		var tier_txt: String = str(diff_prof["tier_name_ar"]) if GameState.is_arabic() else str(diff_prof["tier_name"])
		var obs_txt: String = str(diff_prof["new_obstacle_ar"]) if GameState.is_arabic() else str(diff_prof["new_obstacle"])

		var card_panel := PanelContainer.new()
		card_panel.custom_minimum_size = Vector2(226, 290)
		var border_c: Color = Color(1.0, 0.84, 0.18) if is_gate else (tier_col if lvl_unlocked else Color(0.30, 0.35, 0.48))
		var bg_c: Color = (world_prim.darkened(0.50) if stars == 0 else Color(0.08, 0.36, 0.30, 0.96)) if lvl_unlocked else Color(0.08, 0.11, 0.20, 0.92)
		card_panel.add_theme_stylebox_override("panel", CVB.make_box_style(bg_c, border_c, 18, 3, 6))

		var vbox := VBoxContainer.new()
		vbox.add_theme_constant_override("separation", 5)
		card_panel.add_child(vbox)

		var thumb := TextureRect.new()
		thumb.texture = banner_tex
		thumb.custom_minimum_size = Vector2(196, 94)
		thumb.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		thumb.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		if not lvl_unlocked:
			thumb.modulate = Color(0.42, 0.48, 0.60, 0.85)
		vbox.add_child(thumb)

		var title_lbl := Label.new()
		if GameState.is_arabic():
			title_lbl.text = ("⚡ المرحلة %d [البوابة]" % lvl) if is_gate else ("المرحلة %d" % lvl)
		else:
			title_lbl.text = ("⚡ STAGE %d [GATE]" % lvl) if is_gate else ("STAGE %d" % lvl)
		title_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		title_lbl.add_theme_font_size_override("font_size", 18)
		title_lbl.add_theme_color_override("font_color", Color(1.0, 0.88, 0.28) if is_gate else Color.WHITE)
		vbox.add_child(title_lbl)

		var diff_lbl := Label.new()
		diff_lbl.text = "%s\n%s" % [tier_txt, obs_txt]
		diff_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		diff_lbl.add_theme_font_size_override("font_size", 12)
		diff_lbl.add_theme_color_override("font_color", tier_col)
		vbox.add_child(diff_lbl)

		var star_lbl := Label.new()
		var star_str: String = "★".repeat(stars) + "☆".repeat(3 - stars) if stars > 0 else "☆☆☆"
		var best_str: String = (" • %.1fs" % (float(best_ms) / 1000.0)) if best_ms > 0 else ""
		star_lbl.text = "%s  (⏱ <= %ds)%s" % [star_str, goal_sec, best_str]
		star_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		star_lbl.add_theme_font_size_override("font_size", 13)
		star_lbl.add_theme_color_override("font_color", Color(1.0, 0.92, 0.42) if stars > 0 else Color(0.75, 0.84, 0.98))
		vbox.add_child(star_lbl)

		var play_stage_btn := Button.new()
		play_stage_btn.custom_minimum_size = Vector2(190, 48)
		play_stage_btn.text = GameState.tr_text("▶ PLAY STAGE", "▶ العب المرحلة") if lvl_unlocked else GameState.tr_text("🔒 LOCKED", "🔒 مغلقة")
		play_stage_btn.disabled = not lvl_unlocked
		if lvl_unlocked:
			CVB.style_button(play_stage_btn, Color(1.0, 0.68, 0.04), Color(1.0, 0.94, 0.52), Color.WHITE, 16, 14, 3)
		else:
			CVB.style_button(play_stage_btn, Color(0.12, 0.15, 0.24), Color(0.32, 0.36, 0.48), Color(0.60, 0.65, 0.75), 15, 14, 2)

		var captured_lvl: int = lvl
		play_stage_btn.pressed.connect(func():
			AudioSynth.play_sfx("click")
			GameState.current_level_number = captured_lvl
			get_tree().change_scene_to_file("res://scenes/gameplay_3d.tscn")
		)
		vbox.add_child(play_stage_btn)
		stage_grid.add_child(card_panel)

func rebuild_characters_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = GameState.tr_text(
		"3 Exclusive Heroes (Ahmed • Salma • Little Ahmed) — All Unlocked & Ready for Battle!",
		"الأبطال الثلاثة الحصريون (البطل أحمد • البطلة سلمى • أحمد الصغير) — اختر بطلك المفضل!"
	)
	world_rules_label.modulate = Color(1.0, 0.88, 0.32)

	for s_def in GameState.SKIN_CATALOG:
		var sid: int = int(s_def["id"])
		var selected: bool = (GameState.selected_skin_id == sid)
		var prim_col: Color = s_def["primary"]

		var card_panel := PanelContainer.new()
		card_panel.custom_minimum_size = Vector2(310, 232)
		var border_col: Color = Color(0.25, 1.0, 0.55) if selected else Color(1.0, 0.84, 0.22)
		card_panel.add_theme_stylebox_override("panel", CVB.make_box_style(prim_col.darkened(0.46), border_col, 20, 4 if selected else 3, 8))

		var vbox := VBoxContainer.new()
		vbox.add_theme_constant_override("separation", 6)
		card_panel.add_child(vbox)

		var portrait := TextureRect.new()
		portrait.texture = _get_skin_portrait_tex(sid)
		portrait.custom_minimum_size = Vector2(88, 88)
		portrait.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		portrait.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_CENTERED
		vbox.add_child(portrait)

		var name_lbl := Label.new()
		var disp_name: String = GameState.get_skin_display_name(sid)
		var role_str: String = str(s_def.get("rarity_ar", s_def["rarity"])) if GameState.is_arabic() else str(s_def["rarity"])
		var desc_str: String = str(s_def.get("desc_ar", "")) if GameState.is_arabic() else str(s_def.get("desc", ""))
		name_lbl.text = "%s  [%s]\n%s" % [disp_name, role_str, desc_str]
		name_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		name_lbl.add_theme_font_size_override("font_size", 14)
		name_lbl.add_theme_color_override("font_color", Color.WHITE)
		vbox.add_child(name_lbl)

		var action_btn := Button.new()
		action_btn.custom_minimum_size = Vector2(250, 48)
		if selected:
			action_btn.text = GameState.tr_text("✅ SELECTED HERO", "✅ البطل المختار حالياً")
			CVB.style_button(action_btn, Color(0.12, 0.65, 0.36), Color(0.55, 1.0, 0.72), Color.WHITE, 16, 14, 3)
		else:
			action_btn.text = GameState.tr_text("👑 PLAY AS %s" % disp_name.to_upper(), "👑 اختر %s" % disp_name)
			CVB.style_button(action_btn, Color(1.0, 0.68, 0.04), Color(1.0, 0.92, 0.48), Color.WHITE, 16, 14, 3)

		var captured_sid: int = sid
		action_btn.pressed.connect(func():
			AudioSynth.play_sfx("victory" if GameState.selected_skin_id != captured_sid else "click")
			GameState.select_skin(captured_sid)
			_rebuild_hero_character(captured_sid)
			refresh_header()
			rebuild_characters_tab()
		)
		vbox.add_child(action_btn)
		stage_grid.add_child(card_panel)

func rebuild_missions_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = GameState.tr_text(
		"Complete Mythoria Events & Daily Challenges to earn Golden Rewards & Coins!",
		"أكمل مهام وأحداث ميثوريا اليومية لربح العملات الذهبية والمكافآت الملكية!"
	)
	world_rules_label.modulate = Color(0.45, 0.94, 1.0)

	var gift_panel := PanelContainer.new()
	gift_panel.custom_minimum_size = Vector2(252, 300)
	gift_panel.add_theme_stylebox_override("panel", CVB.make_box_style(Color(0.46, 0.10, 0.28, 0.96), Color(1.0, 0.84, 0.22), 20, 3, 8))
	var g_vbox := VBoxContainer.new()
	g_vbox.add_theme_constant_override("separation", 8)
	gift_panel.add_child(g_vbox)

	var g_banner := TextureRect.new()
	g_banner.texture = _get_event_art_tex("chest")
	g_banner.custom_minimum_size = Vector2(226, 128)
	g_banner.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
	g_banner.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
	g_vbox.add_child(g_banner)

	var g_lbl := Label.new()
	g_lbl.text = GameState.tr_text("🎁 DAILY ROYAL CHEST\nInstant +150 Mythoria Coins", "🎁 الصندوق الملكي اليومي\nمكافأة فورية +150 عملة")
	g_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	g_lbl.add_theme_font_size_override("font_size", 15)
	g_vbox.add_child(g_lbl)

	var gift_btn := Button.new()
	gift_btn.custom_minimum_size = Vector2(214, 50)
	gift_btn.text = GameState.tr_text("✅ CLAIMED TODAY", "✅ تم الاستلام اليوم") if GameState.daily_gift_claimed else GameState.tr_text("🎁 CLAIM +150 🪙", "🎁 استلم +150 🪙")
	gift_btn.disabled = GameState.daily_gift_claimed
	CVB.style_button(gift_btn, Color(1.0, 0.68, 0.04), Color(1.0, 0.94, 0.52), Color.WHITE, 16, 14, 3)
	gift_btn.pressed.connect(func():
		if GameState.claim_daily_gift() > 0:
			AudioSynth.play_sfx("victory")
			refresh_header()
			rebuild_missions_tab()
	)
	g_vbox.add_child(gift_btn)
	stage_grid.add_child(gift_panel)

	var missions := [
		{
			"id": 0,
			"title": GameState.tr_text("🏁 OLYMPUS CUP EVENT\nReach Finish Line (3 Runs)", "🏁 كأس الأولمب\nأنهِ خط النهاية 3 مرات"),
			"cur": GameState.mission_runs_completed,
			"target": 3,
			"reward": 120,
			"art": "missions"
		},
		{
			"id": 1,
			"title": GameState.tr_text("🪙 GOLDEN RUSH EVENT\nCollect 50 Mythoria Coins", "🪙 حمى الذهب\nاجمع 50 عملة ذهبية"),
			"cur": GameState.mission_coins_collected,
			"target": 50,
			"reward": 150,
			"art": "coins"
		},
		{
			"id": 2,
			"title": GameState.tr_text("⚡ ZEUS SPEED TRIAL\nClear Any Stage Goal Time", "⚡ تحدي سرعة زيوس\nفُز بأي مرحلة ضمن الوقت"),
			"cur": GameState.mission_perfect_runs,
			"target": 1,
			"reward": 200,
			"art": "speed"
		},
		{
			"id": 3,
			"title": GameState.tr_text("🦅 SKY JUMPER CHALLENGE\nPerform 25 Hero Jumps", "🦅 تحدي القفز السماوي\nاقفز 25 قفزة في المراحل"),
			"cur": GameState.mission_jumps_done,
			"target": 25,
			"reward": 140,
			"art": "heroes"
		},
		{
			"id": 4,
			"title": GameState.tr_text("🏛 SANCTUARY EXPLORER\nActivate 8 Checkpoints", "🏛 مستكشف المعابد\nفعّل 8 بوابات حفظ Checkpoint"),
			"cur": GameState.mission_checkpoints_hit,
			"target": 8,
			"reward": 180,
			"art": "trail"
		},
		{
			"id": 5,
			"title": GameState.tr_text("★ OLYMPUS STAR MASTER\nEarn 10 Stage Stars", "★ سيد نجوم الأولمب\nاحصل على 10 نجوم في المراحل"),
			"cur": GameState.get_total_stars(),
			"target": 10,
			"reward": 250,
			"art": "missions"
		}
	]
	for m in missions:
		var mid: int = int(m["id"])
		var cur: int = int(m["cur"])
		var target: int = int(m["target"])
		var reward: int = int(m["reward"])
		var art_key: String = str(m["art"])
		var claimed: bool = GameState.mission_claimed.has(mid)
		var ready_to_claim: bool = (cur >= target) and not claimed

		var m_panel := PanelContainer.new()
		m_panel.custom_minimum_size = Vector2(252, 300)
		var border_c: Color = Color(0.25, 1.0, 0.58) if ready_to_claim else Color(1.0, 0.82, 0.18)
		m_panel.add_theme_stylebox_override("panel", CVB.make_box_style(Color(0.08, 0.22, 0.50, 0.96), border_c, 20, 3, 6))

		var m_vbox := VBoxContainer.new()
		m_vbox.add_theme_constant_override("separation", 8)
		m_panel.add_child(m_vbox)

		var m_thumb := TextureRect.new()
		m_thumb.texture = _get_event_art_tex(art_key)
		m_thumb.custom_minimum_size = Vector2(226, 128)
		m_thumb.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		m_thumb.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		m_vbox.add_child(m_thumb)

		var m_lbl := Label.new()
		m_lbl.text = "%s\n%s: %d / %d  •  +%d 🪙" % [
			str(m["title"]),
			GameState.tr_text("Progress", "التقدم"),
			min(cur, target),
			target,
			reward
		]
		m_lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		m_lbl.add_theme_font_size_override("font_size", 14)
		m_vbox.add_child(m_lbl)

		var claim_btn := Button.new()
		claim_btn.custom_minimum_size = Vector2(214, 50)
		var state_txt: String = ""
		if claimed:
			state_txt = GameState.tr_text("✅ CLAIMED", "✅ تم الاستلام")
		elif ready_to_claim:
			state_txt = GameState.tr_text("CLAIM +%d 🪙" % reward, "استلم +%d 🪙" % reward)
		else:
			state_txt = GameState.tr_text("PROGRESS (%d/%d)" % [min(cur, target), target], "التقدم (%d/%d)" % [min(cur, target), target])
		claim_btn.text = state_txt
		claim_btn.disabled = not ready_to_claim
		CVB.style_button(claim_btn, Color(0.14, 0.68, 0.36) if ready_to_claim else Color(0.12, 0.28, 0.58), border_c, Color.WHITE, 15, 14, 3)
		claim_btn.pressed.connect(func():
			if GameState.claim_mission(mid, reward):
				AudioSynth.play_sfx("victory")
				refresh_header()
				rebuild_missions_tab()
		)
		m_vbox.add_child(claim_btn)
		stage_grid.add_child(m_panel)

func rebuild_shop_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = GameState.tr_text(
		"Upgrade your Mythoria powers and unlock lightning abilities & golden trails!",
		"طوّر قدرات أبطالك في ميثوريا وافتح اندفاع البرق والمسارات الذهبية!"
	)
	world_rules_label.modulate = Color(1.0, 0.85, 0.25)

	var shop_items := [
		{
			"title": GameState.tr_text("⚡ ZEUS DASH BOOST", "⚡ اندفاع برق زيوس (DASH)"),
			"desc": GameState.tr_text("Unlock Mid-Air Lightning Burst\nfor fast obstacle dodges!", "افتح قدرة الاندفاع الهوائي السريع\nلتجاوز العقبات الصعبة!"),
			"art": _get_event_art_tex("speed"),
			"owned": GameState.dash_unlocked,
			"price": 150,
			"bg": Color(0.08, 0.28, 0.58, 0.96),
			"border": Color(0.0, 0.90, 1.0),
			"key": "dash"
		},
		{
			"title": GameState.tr_text("✨ GOLDEN COMET TRAIL", "✨ مسار المذنب الذهبي"),
			"desc": GameState.tr_text("Leave a sparkling Mythoria\nstardust trail behind you!", "اترك مساراً ذهبياً متألقاً\nخلف بطلك أثناء الجري والقفز!"),
			"art": _get_event_art_tex("trail"),
			"owned": GameState.unlocked_trails.size() > 1,
			"price": 200,
			"bg": Color(0.32, 0.14, 0.56, 0.96),
			"border": Color(1.0, 0.84, 0.20),
			"key": "trail"
		},
		{
			"title": GameState.tr_text("👑 3 HEROES: SALMA & AHMED", "👑 الأبطال الثلاثة: سلمى وأحمد"),
			"desc": GameState.tr_text("Switch between Ahmed, Salma\nand Little Ahmed anytime!", "بدّل بين البطل أحمد والبطلة سلمى\nوأحمد الصغير مجاناً في أي وقت!"),
			"art": _get_event_art_tex("heroes"),
			"owned": false,
			"price": 0,
			"bg": Color(0.10, 0.24, 0.54, 0.96),
			"border": Color(1.0, 0.88, 0.28),
			"key": "wardrobe"
		}
	]

	for item in shop_items:
		var card := PanelContainer.new()
		card.custom_minimum_size = Vector2(280, 305)
		card.add_theme_stylebox_override("panel", CVB.make_box_style(item["bg"], item["border"], 20, 3, 8))

		var vb := VBoxContainer.new()
		vb.add_theme_constant_override("separation", 8)
		card.add_child(vb)

		var img := TextureRect.new()
		img.texture = item["art"]
		img.custom_minimum_size = Vector2(252, 134)
		img.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		img.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		vb.add_child(img)

		var lbl := Label.new()
		lbl.text = "%s\n%s" % [str(item["title"]), str(item["desc"])]
		lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		lbl.add_theme_font_size_override("font_size", 15)
		vb.add_child(lbl)

		var buy_btn := Button.new()
		buy_btn.custom_minimum_size = Vector2(236, 52)
		var item_key: String = str(item["key"])
		var is_owned: bool = bool(item["owned"])
		var price: int = int(item["price"])
		if item_key == "wardrobe":
			buy_btn.text = GameState.tr_text("👑 CHOOSE HERO", "👑 اختر البطل")
			CVB.style_button(buy_btn, Color(1.0, 0.68, 0.04), Color(1.0, 0.94, 0.52), Color.WHITE, 16, 14, 3)
			buy_btn.pressed.connect(func():
				AudioSynth.play_sfx("click")
				_open_drawer("CHARACTERS")
			)
		else:
			buy_btn.text = GameState.tr_text("✅ UNLOCKED", "✅ مفتوح") if is_owned else GameState.tr_text("BUY (🪙 %d COINS)" % price, "شراء (🪙 %d عملة)" % price)
			buy_btn.disabled = is_owned
			CVB.style_button(buy_btn, Color(0.14, 0.68, 0.36) if is_owned else Color(1.0, 0.68, 0.04), item["border"], Color.WHITE, 16, 14, 3)
			buy_btn.pressed.connect(func():
				if item_key == "dash" and not GameState.dash_unlocked and GameState.coins >= 150:
					GameState.coins -= 150
					GameState.dash_unlocked = true
					GameState.save_progress()
					AudioSynth.play_sfx("victory")
					refresh_header()
					rebuild_shop_tab()
				elif item_key == "trail" and GameState.unlocked_trails.size() <= 1 and GameState.coins >= 200:
					GameState.coins -= 200
					GameState.unlocked_trails.append(1)
					GameState.selected_trail_id = 1
					GameState.save_progress()
					AudioSynth.play_sfx("victory")
					refresh_header()
					rebuild_shop_tab()
			)
		vb.add_child(buy_btn)
		stage_grid.add_child(card)

func rebuild_settings_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = GameState.tr_text(
		"Configure Background Music, Sound Effects, 3D Graphics Quality, and Language (Arabic / English).",
		"تحكم بالموسيقى الخلفية، والمؤثرات الصوتية، وجودة الجرافيك 3D، واللغة (العربية / الإنجليزية)."
	)
	world_rules_label.modulate = Color(0.65, 0.92, 1.0)

	var q_names_en := ["LOW", "MEDIUM", "HIGH"]
	var q_names_ar := ["منخفضة", "متوسطة", "عالية جداً"]
	var q_label: String = q_names_ar[GameState.graphics_quality % 3] if GameState.is_arabic() else q_names_en[GameState.graphics_quality % 3]

	var settings_cards := [
		{
			"title": GameState.tr_text("🎵 BACKGROUND MUSIC", "🎵 الموسيقى الخلفية"),
			"val": GameState.tr_text("ON" if GameState.music_enabled else "OFF", "مفعّلة" if GameState.music_enabled else "متوقفة"),
			"art": _get_world_banner_tex(1),
			"border": Color(1.0, 0.78, 0.24),
			"key": "bgm"
		},
		{
			"title": GameState.tr_text("🔊 SOUND EFFECTS", "🔊 المؤثرات الصوتية"),
			"val": GameState.tr_text("ON" if GameState.sound_enabled else "OFF", "مفعّلة" if GameState.sound_enabled else "متوقفة"),
			"art": _get_world_banner_tex(2),
			"border": Color(0.45, 0.88, 1.0),
			"key": "sfx"
		},
		{
			"title": GameState.tr_text("✨ 3D GRAPHICS QUALITY", "✨ جودة الجرافيك 3D"),
			"val": q_label,
			"art": _get_world_banner_tex(4),
			"border": Color(1.0, 0.84, 0.22),
			"key": "gfx"
		},
		{
			"title": "🌐 LANGUAGE / اللغة",
			"val": "العربية (AR)" if GameState.is_arabic() else "English (EN)",
			"art": _get_world_banner_tex(7),
			"border": Color(0.35, 0.96, 0.72),
			"key": "lang"
		}
	]

	for sc in settings_cards:
		var card := PanelContainer.new()
		card.custom_minimum_size = Vector2(246, 280)
		card.add_theme_stylebox_override("panel", CVB.make_box_style(Color(0.08, 0.20, 0.46, 0.96), sc["border"], 20, 3, 6))
		var vb := VBoxContainer.new()
		vb.add_theme_constant_override("separation", 10)
		card.add_child(vb)

		var thumb := TextureRect.new()
		thumb.texture = sc["art"]
		thumb.custom_minimum_size = Vector2(220, 120)
		thumb.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
		thumb.stretch_mode = TextureRect.STRETCH_KEEP_ASPECT_COVERED
		vb.add_child(thumb)

		var lbl := Label.new()
		lbl.text = str(sc["title"])
		lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
		lbl.add_theme_font_size_override("font_size", 16)
		vb.add_child(lbl)

		var toggle_btn := Button.new()
		toggle_btn.custom_minimum_size = Vector2(210, 50)
		toggle_btn.text = str(sc["val"])
		CVB.style_button(toggle_btn, Color(0.14, 0.40, 0.82), sc["border"], Color.WHITE, 17, 14, 3)
		var skey: String = str(sc["key"])
		toggle_btn.pressed.connect(func():
			if skey == "bgm":
				GameState.music_enabled = not GameState.music_enabled
				if not GameState.music_enabled and is_instance_valid(AudioSynth.bgm_player):
					AudioSynth.bgm_player.stop()
			elif skey == "sfx":
				GameState.sound_enabled = not GameState.sound_enabled
			elif skey == "gfx":
				GameState.graphics_quality = (GameState.graphics_quality + 1) % 3
			elif skey == "lang":
				GameState.language_code = "en" if GameState.is_arabic() else "ar"
			AudioSynth.play_sfx("click")
			GameState.save_progress()
			refresh_header()
			_open_drawer("SETTINGS")
		)
		vb.add_child(toggle_btn)
		stage_grid.add_child(card)

func _on_lang_button_pressed() -> void:
	GameState.language_code = "en" if GameState.is_arabic() else "ar"
	GameState.save_progress()
	AudioSynth.play_sfx("click")
	refresh_header()
	if drawer_modal.visible:
		_open_drawer(current_tab)

func _on_play_button_pressed() -> void:
	AudioSynth.play_sfx("click")
	get_tree().change_scene_to_file("res://scenes/gameplay_3d.tscn")

func _on_mode_button_pressed() -> void:
	AudioSynth.play_sfx("click")
	var modes := ["CAREER", "SURVIVAL", "TIME_TRIAL", "CHALLENGE"]
	var idx: int = modes.find(GameState.selected_game_mode)
	GameState.selected_game_mode = modes[(idx + 1) % modes.size()]
	refresh_header()

func _on_prev_skin_pressed() -> void:
	AudioSynth.play_sfx("click")
	var total_skins: int = GameState.SKIN_CATALOG.size()
	preview_skin_id = (preview_skin_id - 1 + total_skins) % total_skins
	GameState.select_skin(preview_skin_id)
	_rebuild_hero_character(preview_skin_id)
	refresh_header()

func _on_next_skin_pressed() -> void:
	AudioSynth.play_sfx("click")
	var total_skins: int = GameState.SKIN_CATALOG.size()
	preview_skin_id = (preview_skin_id + 1) % total_skins
	GameState.select_skin(preview_skin_id)
	_rebuild_hero_character(preview_skin_id)
	refresh_header()

func _on_daily_gift_pressed() -> void:
	AudioSynth.play_sfx("click")
	_open_drawer("MISSIONS")

func _on_worlds_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	_open_drawer("WORLDS")

func _on_characters_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	_open_drawer("CHARACTERS")

func _on_missions_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	_open_drawer("MISSIONS")

func _on_shop_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	_open_drawer("SHOP")

func _on_settings_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	_open_drawer("SETTINGS")

func _on_close_drawer_pressed() -> void:
	AudioSynth.play_sfx("click")
	drawer_modal.visible = false
	_rebuild_hero_character(GameState.selected_skin_id)
	refresh_header()
