extends Node3D

## MYTHORIA games — By ahmed
## 3D Interactive Main Menu Showcase + World Unlocking Progression Graph,
## Character Wardrobe, Daily Missions, Shop & Settings.
## Uses str() conversions — 100% compatible with Godot 4.x GDScript compiler.

const CVB := preload("res://scripts/character_visual_builder.gd")

@onready var showcase_world: Node3D = $ShowcaseWorld
@onready var hero_pedestal_pivot: Node3D = $ShowcaseWorld/HeroPedestalPivot
@onready var top_left_profile: Panel = $UI/Root/TopLeftProfile
@onready var profile_label: Label = $UI/Root/TopLeftProfile/ProfileLabel
@onready var top_center_logo: Panel = $UI/Root/TopCenterLogo
@onready var coins_badge: Button = $UI/Root/TopRightBar/CoinsBadge
@onready var stars_badge: Button = $UI/Root/TopRightBar/StarsBadge
@onready var mode_button: Button = $UI/Root/TopRightBar/ModeButton
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
var current_tab: String = "WORLDS"
var showcase_time: float = 0.0
var hero_visual: Node3D = null
var hero_yaw_deg: float = 0.0
var bg_hammer_pivot: Node3D = null
var bg_runners: Array = []
var floating_clouds: Array = []

func _ready() -> void:
	DisplayServer.screen_set_orientation(DisplayServer.SCREEN_LANDSCAPE)
	selected_world_id = clampi(int((GameState.current_level_number - 1) / 5) + 1, 1, GameState.TOTAL_WORLDS)
	_connect_all_buttons()
	_apply_ui_theme()
	_build_3d_showcase_world()
	_rebuild_hero_character()
	refresh_header()

func _connect_all_buttons() -> void:
	if not coins_badge.pressed.is_connected(_on_shop_tab_pressed):
		coins_badge.pressed.connect(_on_shop_tab_pressed)
	if not stars_badge.pressed.is_connected(_on_worlds_tab_pressed):
		stars_badge.pressed.connect(_on_worlds_tab_pressed)
	if not mode_button.pressed.is_connected(_on_mode_button_pressed):
		mode_button.pressed.connect(_on_mode_button_pressed)
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

func _apply_ui_theme() -> void:
	top_left_profile.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.06, 0.14, 0.36, 0.88), Color(1.0, 0.82, 0.18, 1.0), 18, 3, 6)
	)
	top_center_logo.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.05, 0.11, 0.28, 0.86), Color(1.0, 0.82, 0.18, 1.0), 22, 4, 8)
	)
	pedestal_badge.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.05, 0.11, 0.30, 0.90), Color(1.0, 0.82, 0.18, 1.0), 18, 3, 6)
	)
	drawer_modal.add_theme_stylebox_override(
		"panel",
		CVB.make_box_style(Color(0.04, 0.09, 0.24, 0.94), Color(1.0, 0.82, 0.18, 1.0), 22, 4, 12)
	)

	CVB.style_button(coins_badge, Color(0.10, 0.22, 0.52, 0.95), Color(1.0, 0.82, 0.18), Color(1.0, 0.92, 0.35), 17, 18, 3)
	CVB.style_button(stars_badge, Color(0.10, 0.22, 0.52, 0.95), Color(0.0, 0.86, 1.0), Color(0.75, 0.96, 1.0), 17, 18, 3)
	CVB.style_button(mode_button, Color(0.16, 0.30, 0.68, 0.95), Color(1.0, 0.82, 0.18), Color.WHITE, 16, 18, 3)

	CVB.style_button(missions_quick_btn, Color(0.12, 0.26, 0.62, 0.92), Color(1.0, 0.82, 0.18), Color(1.0, 0.94, 0.68), 17, 16, 3)
	CVB.style_button(daily_gift_quick_btn, Color(0.68, 0.14, 0.36, 0.92), Color(1.0, 0.84, 0.22), Color.WHITE, 17, 16, 3)
	CVB.style_button(world_graph_quick_btn, Color(0.08, 0.38, 0.58, 0.92), Color(0.0, 0.88, 1.0), Color(0.78, 0.98, 1.0), 16, 16, 3)

	CVB.style_button(big_play_btn, Color(1.0, 0.68, 0.04, 1.0), Color(1.0, 0.94, 0.52), Color.WHITE, 28, 20, 4)
	CVB.style_button(levels_btn, Color(0.10, 0.44, 0.88, 0.95), Color(0.52, 0.88, 1.0), Color.WHITE, 19, 16, 3)
	CVB.style_button(characters_btn, Color(0.12, 0.36, 0.78, 0.95), Color(0.45, 0.82, 1.0), Color.WHITE, 19, 16, 3)
	CVB.style_button(shop_btn, Color(0.14, 0.30, 0.72, 0.95), Color(1.0, 0.82, 0.24), Color(1.0, 0.94, 0.68), 19, 16, 3)
	CVB.style_button(settings_btn, Color(0.10, 0.18, 0.38, 0.92), Color(0.48, 0.65, 0.92), Color.WHITE, 17, 16, 2)

	CVB.style_button(prev_skin_btn, Color(1.0, 0.70, 0.08, 0.96), Color(1.0, 0.92, 0.48), Color.WHITE, 22, 16, 3)
	CVB.style_button(next_skin_btn, Color(1.0, 0.70, 0.08, 0.96), Color(1.0, 0.92, 0.48), Color.WHITE, 22, 16, 3)
	CVB.style_button(close_drawer_btn, Color(0.82, 0.18, 0.24, 0.96), Color(1.0, 0.75, 0.75), Color.WHITE, 17, 14, 3)

func _build_3d_showcase_world() -> void:
	var mat_gold := CVB.make_mat(Color(1.0, 0.82, 0.18), 0.22, 0.85, Color(1.0, 0.72, 0.10), 0.25)
	var mat_royal_blue := CVB.make_mat(Color(0.10, 0.36, 0.82), 0.32, 0.20)
	var mat_cyan_glow := CVB.make_mat(Color(0.0, 0.88, 1.0), 0.18, 0.25, Color(0.0, 0.78, 1.0), 1.3)
	var mat_marble := CVB.make_mat(Color(0.95, 0.96, 1.0), 0.35, 0.08)
	var mat_grass := CVB.make_mat(Color(0.24, 0.78, 0.32), 0.48, 0.02)
	var mat_rock := CVB.make_mat(Color(0.56, 0.44, 0.36), 0.68, 0.04)
	var mat_flame := CVB.make_mat(Color(1.0, 0.56, 0.08), 0.15, 0.10, Color(1.0, 0.62, 0.10), 2.4)

	# 1. Center Hero Pedestal (Multi-tiered Royal Blue, Cyan & Gold Podium)
	_add_cyl(showcase_world, Vector3(0.0, -0.35, 0.0), 2.35, 2.55, 0.42, mat_royal_blue)
	_add_cyl(showcase_world, Vector3(0.0, -0.08, 0.0), 1.92, 2.08, 0.18, mat_gold)
	_add_cyl(showcase_world, Vector3(0.0, 0.04, 0.0), 1.58, 1.66, 0.12, mat_cyan_glow)
	_add_cyl(showcase_world, Vector3(0.0, 0.12, 0.0), 1.42, 1.48, 0.08, mat_marble)

	# Twin Golden Fire Braziers flanking the Hero Pedestal
	for side_dir in [-1, 1]:
		var bx: float = float(side_dir) * 2.35
		_add_box(showcase_world, Vector3(bx, -0.05, 0.35), Vector3(0.58, 0.55, 0.58), mat_marble)
		_add_cyl(showcase_world, Vector3(bx, 0.32, 0.35), 0.38, 0.24, 0.24, mat_gold)
		_add_sphere(showcase_world, Vector3(bx, 0.56, 0.35), 0.24, 0.46, mat_flame)

	# 2. Background Obstacle Bridge & Distant Finish Arch
	var bridge_colors := [
		Color(0.14, 0.56, 0.98),
		Color(1.0, 0.76, 0.12),
		Color(0.92, 0.22, 0.58),
		Color(0.18, 0.82, 0.52)
	]
	for seg_i in range(6):
		var col_mat := CVB.make_mat(bridge_colors[seg_i % bridge_colors.size()], 0.35, 0.10)
		var bz: float = -3.8 - float(seg_i) * 3.1
		_add_box(showcase_world, Vector3(0.0, -0.35, bz), Vector3(3.4, 0.48, 2.7), col_mat)

	bg_hammer_pivot = Node3D.new()
	bg_hammer_pivot.position = Vector3(0.0, 0.45, -8.2)
	showcase_world.add_child(bg_hammer_pivot)
	_add_cyl(bg_hammer_pivot, Vector3(0.0, 0.0, 0.0), 0.24, 0.28, 1.1, mat_gold)
	_add_box(bg_hammer_pivot, Vector3(0.0, 0.22, 0.0), Vector3(3.8, 0.32, 0.32), CVB.make_mat(Color(0.92, 0.20, 0.22), 0.30, 0.20))

	_add_cyl(showcase_world, Vector3(-1.45, 0.9, -18.5), 0.24, 0.24, 2.8, mat_marble)
	_add_cyl(showcase_world, Vector3(1.45, 0.9, -18.5), 0.24, 0.24, 2.8, mat_marble)
	_add_box(showcase_world, Vector3(0.0, 2.4, -18.5), Vector3(3.6, 0.55, 0.55), mat_gold)
	_add_box(showcase_world, Vector3(0.0, 1.0, -18.5), Vector3(2.5, 2.2, 0.12), mat_cyan_glow)

	for r_i in range(2):
		var comp_vis: Node3D = CVB.build_character_visual((r_i + 2) % 8)
		comp_vis.position = Vector3(-0.75 + float(r_i) * 1.5, -0.10, -5.8 - float(r_i) * 3.2)
		comp_vis.rotation.y = PI
		comp_vis.scale = Vector3(0.82, 0.82, 0.82)
		showcase_world.add_child(comp_vis)
		bg_runners.append(comp_vis)

	# 3. Left & Right Floating Temple / Castle Sky Islands with Waterfalls
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

func _rebuild_hero_character() -> void:
	for child in hero_pedestal_pivot.get_children():
		child.queue_free()
	hero_visual = CVB.build_character_visual(GameState.selected_skin_id)
	hero_visual.position = Vector3(0.0, 0.16, 0.0)
	hero_visual.scale = Vector3(1.22, 1.22, 1.22)
	hero_pedestal_pivot.add_child(hero_visual)

func _input(event: InputEvent) -> void:
	if not drawer_modal.visible and event is InputEventScreenDrag:
		hero_yaw_deg += event.relative.x * 0.45

func _process(delta: float) -> void:
	showcase_time += delta

	if is_instance_valid(hero_visual):
		hero_visual.rotation.y = deg_to_rad(hero_yaw_deg) + sin(showcase_time * 1.1) * 0.28
		CVB.animate_character(hero_visual, showcase_time, Vector3.ZERO, true, 0.0, false)

	if is_instance_valid(bg_hammer_pivot):
		bg_hammer_pivot.rotation.y += delta * 2.2

	for i in range(bg_runners.size()):
		var runner: Node3D = bg_runners[i]
		if is_instance_valid(runner):
			CVB.animate_character(runner, showcase_time + float(i) * 0.8, Vector3(0, 0, -5.0), true, 0.0, false)

	for i in range(floating_clouds.size()):
		var cl: Node3D = floating_clouds[i]
		if is_instance_valid(cl):
			cl.position.x += sin(showcase_time * 0.4 + float(i)) * delta * 0.25

func refresh_header() -> void:
	var skin: Dictionary = GameState.get_skin_def(GameState.selected_skin_id)
	var skin_name: String = str(skin["name"])
	var unlocked_worlds: int = GameState.get_unlocked_worlds_count()
	var total_stars: int = GameState.get_total_stars()
	var lvl: int = GameState.current_level_number
	var w_id: int = int((lvl - 1) / 5) + 1
	var goal_sec: int = int(GameState.goal_time_ms_for_level(lvl) / 1000)

	profile_label.text = "👑 Lv.%d  |  %s\n🌍 Worlds Unlocked: %d / %d" % [
		GameState.player_level,
		skin_name,
		unlocked_worlds,
		GameState.TOTAL_WORLDS
	]
	coins_badge.text = "🪙 %d +" % GameState.coins
	stars_badge.text = "★ %d / 150" % total_stars
	mode_button.text = "MODE: %s" % GameState.selected_game_mode
	big_play_btn.text = "▶ PLAY (LV.%d)" % lvl
	pedestal_label.text = "♛ MYTHORIA games — By ahmed\n%s  •  W%d STAGE %d (Goal <= %ds)" % [
		skin_name,
		w_id,
		lvl,
		goal_sec
	]

func _open_drawer(tab_name: String) -> void:
	current_tab = tab_name
	drawer_modal.visible = true
	refresh_header()

	if tab_name == "WORLDS":
		drawer_title.text = "🌍 WORLD SELECT & PROGRESSION GRAPH"
		world_scroll.visible = true
		rebuild_worlds_and_stages()
	elif tab_name == "CHARACTERS":
		drawer_title.text = "👑 MYTHORIA HEROES & CHAMPIONS (15 SKINS)"
		world_scroll.visible = false
		rebuild_characters_tab()
	elif tab_name == "MISSIONS":
		drawer_title.text = "🏆 DAILY MISSIONS & REWARDS"
		world_scroll.visible = false
		rebuild_missions_tab()
	elif tab_name == "SHOP":
		drawer_title.text = "🛒 MYTHORIA ROYAL ARMORY & SHOP"
		world_scroll.visible = false
		rebuild_shop_tab()
	elif tab_name == "SETTINGS":
		drawer_title.text = "⚙ GAME SETTINGS & PERFORMANCE"
		world_scroll.visible = false
		rebuild_settings_tab()

func rebuild_worlds_and_stages() -> void:
	for child in world_list.get_children():
		child.queue_free()
	for child in stage_grid.get_children():
		child.queue_free()

	for w in range(1, GameState.TOTAL_WORLDS + 1):
		var w_theme: Dictionary = GameState.get_world_theme(w)
		var unlocked: bool = GameState.is_world_unlocked(w)
		var btn := Button.new()
		btn.custom_minimum_size = Vector2(225, 68)
		var prefix: String = "🌍 " if unlocked else "🔒 "
		btn.text = "%sW%d: %s" % [prefix, w, str(w_theme["name"])]

		var prim_col: Color = w_theme["primary"]
		if w == selected_world_id:
			CVB.style_button(btn, prim_col.darkened(0.25), Color(1.0, 0.86, 0.22), Color.WHITE, 16, 16, 4)
		elif unlocked:
			CVB.style_button(btn, Color(0.10, 0.22, 0.48, 0.95), prim_col, Color.WHITE, 15, 16, 3)
		else:
			CVB.style_button(btn, Color(0.10, 0.13, 0.22, 0.88), Color(0.35, 0.40, 0.52), Color(0.65, 0.70, 0.82), 15, 16, 2)

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
	if w_unlocked:
		world_rules_label.text = "✅ WORLD %d UNLOCKED — Clear Gateway Stage %d within <= %ds to unlock World %d!" % [
			selected_world_id, gate_lvl, gate_goal_sec, min(selected_world_id + 1, GameState.TOTAL_WORLDS)
		]
		world_rules_label.modulate = Color(0.35, 0.98, 0.72)
	else:
		var prev_gate: int = (selected_world_id - 1) * 5
		var prev_goal_sec: int = int(GameState.goal_time_ms_for_level(prev_gate) / 1000)
		world_rules_label.text = "🔒 WORLD %d LOCKED — Complete World %d Gateway Stage %d within <= %ds first!" % [
			selected_world_id, selected_world_id - 1, prev_gate, prev_goal_sec
		]
		world_rules_label.modulate = Color(1.0, 0.50, 0.56)

	var first_lvl: int = (selected_world_id - 1) * 5 + 1
	var w_theme_cur: Dictionary = GameState.get_world_theme(selected_world_id)
	var world_prim: Color = w_theme_cur["primary"]

	for offset in range(5):
		var lvl: int = first_lvl + offset
		var lvl_unlocked: bool = GameState.is_level_unlocked(lvl)
		var stars: int = GameState.get_level_stars(lvl)
		var best_ms: int = GameState.get_level_best_time_ms(lvl)
		var goal_sec: int = int(GameState.goal_time_ms_for_level(lvl) / 1000)
		var is_gate: bool = ((offset + 1) == 5)

		var card := Button.new()
		card.custom_minimum_size = Vector2(210, 220)
		var gate_tag: String = "\n⚡ [TEMPLE GATE]" if is_gate else ""
		var best_text: String = ("Best: %.2fs" % (float(best_ms) / 1000.0)) if best_ms > 0 else ("▶ PLAY" if lvl_unlocked else "🔒 LOCKED")
		var star_str: String = "★".repeat(stars) + "☆".repeat(3 - stars) if stars > 0 else "☆☆☆"
		card.text = "STAGE %d%s\n%s\n⏱ Goal <= %ds\n%s" % [lvl, gate_tag, star_str, goal_sec, best_text]
		card.disabled = not lvl_unlocked

		if lvl_unlocked:
			var border_c: Color = Color(1.0, 0.84, 0.18) if is_gate else world_prim.lightened(0.25)
			var bg_c: Color = world_prim.darkened(0.35) if stars == 0 else Color(0.10, 0.42, 0.34, 0.96)
			CVB.style_button(card, bg_c, border_c, Color.WHITE, 17, 18, 3)
		else:
			CVB.style_button(card, Color(0.10, 0.13, 0.22, 0.85), Color(0.30, 0.35, 0.48), Color(0.60, 0.65, 0.75), 16, 18, 2)

		var captured_lvl: int = lvl
		card.pressed.connect(func():
			AudioSynth.play_sfx("click")
			GameState.current_level_number = captured_lvl
			get_tree().change_scene_to_file("res://scenes/gameplay_3d.tscn")
		)
		stage_grid.add_child(card)

func rebuild_characters_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = "Select or unlock 15 Mythological 3D Heroes! Your chosen hero appears live on the 3D Pedestal."
	world_rules_label.modulate = Color(1.0, 0.86, 0.28)

	for s_def in GameState.SKIN_CATALOG:
		var sid: int = int(s_def["id"])
		var unlocked: bool = GameState.unlocked_skins.has(sid)
		var selected: bool = (GameState.selected_skin_id == sid)
		var price: int = int(s_def["price"])
		var prim_col: Color = s_def["primary"]

		var btn := Button.new()
		btn.custom_minimum_size = Vector2(205, 210)
		var status: String = "✅ EQUIPPED" if selected else ("EQUIP HERO" if unlocked else ("🪙 %d COINS" % price))
		btn.text = "👑 %s\n[%s]\n\n%s" % [str(s_def["name"]), str(s_def["rarity"]), status]

		var border_col: Color = Color(0.25, 1.0, 0.55) if selected else (Color(1.0, 0.82, 0.18) if unlocked else prim_col)
		CVB.style_button(btn, prim_col.darkened(0.38), border_col, Color.WHITE, 16, 18, 3)

		btn.pressed.connect(func():
			if unlocked:
				AudioSynth.play_sfx("click")
				GameState.select_skin(sid)
				_rebuild_hero_character()
				refresh_header()
				rebuild_characters_tab()
			elif GameState.unlock_skin(sid, price):
				AudioSynth.play_sfx("victory")
				_rebuild_hero_character()
				refresh_header()
				rebuild_characters_tab()
		)
		stage_grid.add_child(btn)

func rebuild_missions_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = "Complete Daily Challenges and claim your Daily Mythoria Treasure Chest!"
	world_rules_label.modulate = Color(0.45, 0.94, 1.0)

	var gift_btn := Button.new()
	gift_btn.custom_minimum_size = Vector2(240, 210)
	gift_btn.text = "🎁 DAILY TREASURE\n\n%s" % ("✅ CLAIMED TODAY" if GameState.daily_gift_claimed else "CLAIM +150 COINS!")
	gift_btn.disabled = GameState.daily_gift_claimed
	CVB.style_button(gift_btn, Color(0.68, 0.16, 0.38, 0.95), Color(1.0, 0.84, 0.22), Color.WHITE, 17, 18, 3)
	gift_btn.pressed.connect(func():
		if GameState.claim_daily_gift() > 0:
			AudioSynth.play_sfx("victory")
			refresh_header()
			rebuild_missions_tab()
	)
	stage_grid.add_child(gift_btn)

	var missions := [
		{"id": 0, "title": "🏁 Reach Finish Line", "cur": GameState.mission_runs_completed, "target": 3, "reward": 120},
		{"id": 1, "title": "🪙 Collect 50 Coins", "cur": GameState.mission_coins_collected, "target": 50, "reward": 150},
		{"id": 2, "title": "⚡ Speed Goal Clear", "cur": GameState.mission_perfect_runs, "target": 1, "reward": 200}
	]
	for m in missions:
		var mid: int = int(m["id"])
		var cur: int = int(m["cur"])
		var target: int = int(m["target"])
		var reward: int = int(m["reward"])
		var claimed: bool = GameState.mission_claimed.has(mid)
		var ready_to_claim: bool = (cur >= target) and not claimed

		var card := Button.new()
		card.custom_minimum_size = Vector2(235, 210)
		var state_txt: String = "✅ CLAIMED" if claimed else (("CLAIM +%d 🪙" % reward) if ready_to_claim else "IN PROGRESS")
		card.text = "%s\nProgress: %d / %d\nReward: +%d Coins\n\n%s" % [str(m["title"]), cur, target, reward, state_txt]
		card.disabled = not ready_to_claim
		CVB.style_button(card, Color(0.10, 0.28, 0.62, 0.95), Color(1.0, 0.82, 0.18), Color.WHITE, 16, 18, 3)
		card.pressed.connect(func():
			if GameState.claim_mission(mid, reward):
				AudioSynth.play_sfx("victory")
				refresh_header()
				rebuild_missions_tab()
		)
		stage_grid.add_child(card)

func rebuild_shop_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = "Upgrade your Mythoria abilities and unlock golden trails & victory dances!"
	world_rules_label.modulate = Color(1.0, 0.85, 0.25)

	var dash_btn := Button.new()
	dash_btn.custom_minimum_size = Vector2(250, 210)
	dash_btn.text = "⚡ ZEUS DASH BOOST\nBurst forward in mid-air!\n\n%s" % ("✅ UNLOCKED" if GameState.dash_unlocked else "BUY (🪙 150 COINS)")
	dash_btn.disabled = GameState.dash_unlocked
	CVB.style_button(dash_btn, Color(0.08, 0.38, 0.68, 0.95), Color(0.0, 0.88, 1.0), Color.WHITE, 17, 18, 3)
	dash_btn.pressed.connect(func():
		if not GameState.dash_unlocked and GameState.coins >= 150:
			GameState.coins -= 150
			GameState.dash_unlocked = true
			GameState.save_progress()
			AudioSynth.play_sfx("victory")
			refresh_header()
			rebuild_shop_tab()
	)
	stage_grid.add_child(dash_btn)

	var trail_btn := Button.new()
	trail_btn.custom_minimum_size = Vector2(250, 210)
	trail_btn.text = "✨ GOLDEN COMET TRAIL\nMythoria stardust trail!\n\n%s" % ("✅ UNLOCKED" if GameState.unlocked_trails.size() > 1 else "BUY (🪙 200 COINS)")
	trail_btn.disabled = GameState.unlocked_trails.size() > 1
	CVB.style_button(trail_btn, Color(0.42, 0.22, 0.68, 0.95), Color(1.0, 0.82, 0.18), Color.WHITE, 17, 18, 3)
	trail_btn.pressed.connect(func():
		if GameState.unlocked_trails.size() <= 1 and GameState.coins >= 200:
			GameState.coins -= 200
			GameState.unlocked_trails.append(1)
			GameState.selected_trail_id = 1
			GameState.save_progress()
			AudioSynth.play_sfx("victory")
			refresh_header()
			rebuild_shop_tab()
	)
	stage_grid.add_child(trail_btn)

func rebuild_settings_tab() -> void:
	for child in stage_grid.get_children():
		child.queue_free()
	world_rules_label.text = "Configure audio, graphics quality (LOW / MEDIUM / HIGH), and language (EN / AR)."
	world_rules_label.modulate = Color(0.65, 0.92, 1.0)

	var sfx_btn := Button.new()
	sfx_btn.custom_minimum_size = Vector2(230, 190)
	sfx_btn.text = "🔊 SOUND EFFECTS\n\n%s" % ("ON" if GameState.sound_enabled else "OFF")
	CVB.style_button(sfx_btn, Color(0.12, 0.28, 0.58, 0.95), Color(0.45, 0.85, 1.0), Color.WHITE, 18, 18, 3)
	sfx_btn.pressed.connect(func():
		GameState.sound_enabled = not GameState.sound_enabled
		GameState.save_progress()
		rebuild_settings_tab()
	)
	stage_grid.add_child(sfx_btn)

	var gfx_btn := Button.new()
	gfx_btn.custom_minimum_size = Vector2(230, 190)
	var q_names := ["LOW", "MEDIUM", "HIGH"]
	gfx_btn.text = "✨ GRAPHICS QUALITY\n\n%s" % q_names[GameState.graphics_quality % 3]
	CVB.style_button(gfx_btn, Color(0.14, 0.34, 0.64, 0.95), Color(1.0, 0.82, 0.18), Color.WHITE, 18, 18, 3)
	gfx_btn.pressed.connect(func():
		GameState.graphics_quality = (GameState.graphics_quality + 1) % 3
		GameState.save_progress()
		rebuild_settings_tab()
	)
	stage_grid.add_child(gfx_btn)

	var lang_btn := Button.new()
	lang_btn.custom_minimum_size = Vector2(230, 190)
	lang_btn.text = "🌐 LANGUAGE / اللغة\n\n%s" % ("العربية (AR)" if GameState.language_code == "ar" else "English (EN)")
	CVB.style_button(lang_btn, Color(0.12, 0.28, 0.58, 0.95), Color(0.45, 0.85, 1.0), Color.WHITE, 18, 18, 3)
	lang_btn.pressed.connect(func():
		GameState.language_code = "en" if GameState.language_code == "ar" else "ar"
		GameState.save_progress()
		rebuild_settings_tab()
	)
	stage_grid.add_child(lang_btn)

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
	var count: int = GameState.unlocked_skins.size()
	if count > 0:
		var cur_idx: int = GameState.unlocked_skins.find(GameState.selected_skin_id)
		var next_id: int = int(GameState.unlocked_skins[(cur_idx - 1 + count) % count])
		GameState.select_skin(next_id)
		_rebuild_hero_character()
		refresh_header()

func _on_next_skin_pressed() -> void:
	AudioSynth.play_sfx("click")
	var count: int = GameState.unlocked_skins.size()
	if count > 0:
		var cur_idx: int = GameState.unlocked_skins.find(GameState.selected_skin_id)
		var next_id: int = int(GameState.unlocked_skins[(cur_idx + 1) % count])
		GameState.select_skin(next_id)
		_rebuild_hero_character()
		refresh_header()

func _on_daily_gift_pressed() -> void:
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
