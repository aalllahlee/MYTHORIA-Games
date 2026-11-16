extends Control

var selected_world_id: int = 1
var selected_mode: int = GameState.GameMode.CAREER
var active_tab: String = "WORLDS"

@onready var stats_label: Label = $TopBar/StatsLabel
@onready var world_row: HBoxContainer = $WorldScroll/WorldRow
@onready var gate_banner: Label = $GateBanner
@onready var stages_row: HBoxContainer = $StagePanel/StagesRow
@onready var mode_button: Button = $TopBar/ModeButton

func _ready() -> void:
	refresh_ui()

func refresh_ui() -> void:
	var unlocked_worlds_count: int = 0
	for w in range(1, 11):
		if GameState.is_world_unlocked(w):
			unlocked_worlds_count += 1
	var lv: int = 1 + int(GameState.player_xp / 1000)
	var skin_def: Dictionary = GameState.get_skin_def(GameState.selected_skin_id)
	stats_label.text = "LV.%d  |  COINS: %d  |  HERO: %s  |  WORLDS: %d/10" % [
		lv, GameState.coins, str(skin_def["name"]), unlocked_worlds_count
	]

	var mode_names := ["MODE: CAREER", "MODE: AI RIVAL RACE", "MODE: ENDLESS", "MODE: TIME ATTACK", "MODE: PRACTICE", "MODE: DAILY CHALLENGE"]
	mode_button.text = mode_names[selected_mode]

	# Populate 10 Mythological World Graph Nodes
	for child in world_row.get_children():
		child.queue_free()

	for w_def in GameState.WORLDS:
		var w_id: int = int(w_def["id"])
		var unlocked: bool = GameState.is_world_unlocked(w_id)
		var btn := Button.new()
		btn.custom_minimum_size = Vector2(184, 68)
		var lock_prefix: String = "" if unlocked else "[LOCKED] "
		btn.text = "%sW%d: %s" % [lock_prefix, w_id, str(w_def["name"])]
		btn.modulate = Color(1.0, 0.85, 0.25) if w_id == selected_world_id else (Color(0.0, 0.88, 1.0) if unlocked else Color(0.55, 0.58, 0.68))
		btn.pressed.connect(func() -> void:
			AudioSynth.play_sfx("click")
			selected_world_id = w_id
			active_tab = "WORLDS"
			refresh_ui()
		)
		world_row.add_child(btn)

	for child in stages_row.get_children():
		child.queue_free()

	if active_tab == "CHARACTERS" or active_tab == "SHOP":
		gate_banner.text = "MYTHORIA HERO SANCTUARY & ROYAL ARMORY — Select or Unlock Mythological Champions!"
		gate_banner.modulate = Color(1.0, 0.84, 0.22)
		for s_def in GameState.SKINS:
			var s_id: String = str(s_def["id"])
			var owned: bool = bool(GameState.owned_items.get(s_id, false))
			var equipped: bool = (GameState.selected_skin_id == s_id)
			var cost: int = int(s_def["cost"])
			var card := Button.new()
			card.custom_minimum_size = Vector2(195, 170)
			var state_txt: String = "EQUIPPED" if equipped else ("OWNED (EQUIP)" if owned else ("BUY %d COINS" % cost))
			card.text = "%s\n\n%s" % [str(s_def["name"]), state_txt]
			card.modulate = Color(1.0, 0.86, 0.25) if equipped else Color.WHITE
			card.pressed.connect(func() -> void:
				AudioSynth.play_sfx("click")
				if owned:
					GameState.selected_skin_id = s_id
					GameState.save_game()
				elif GameState.coins >= cost:
					GameState.coins -= cost
					GameState.owned_items[s_id] = true
					GameState.selected_skin_id = s_id
					GameState.save_game()
				refresh_ui()
			)
			stages_row.add_child(card)
		return

	if active_tab == "MISSIONS":
		gate_banner.text = "OLYMPUS DAILY TRIALS — Complete runs & time goals to earn Royal Gold & XP!"
		gate_banner.modulate = Color(0.0, 0.89, 1.0)
		var trials := [
			{"title": "Clear 3 Temple Stages", "reward": "+500 Coins"},
			{"title": "Collect 100 Golden Drachmas", "reward": "+400 Coins"},
			{"title": "Beat a World Gateway Time Goal", "reward": "+600 Coins"}
		]
		for tr in trials:
			var m_card := Button.new()
			m_card.custom_minimum_size = Vector2(310, 165)
			m_card.text = "%s\nReward: %s\n(Tap to Play)" % [tr["title"], tr["reward"]]
			m_card.pressed.connect(_on_quick_play_pressed)
			stages_row.add_child(m_card)
		return

	if active_tab == "SETTINGS":
		gate_banner.text = "MYTHORIA games — By ahmed  |  100% Offline Android 3D Build"
		gate_banner.modulate = Color(1.0, 0.84, 0.22)
		var sfx_btn := Button.new()
		sfx_btn.custom_minimum_size = Vector2(260, 150)
		sfx_btn.text = "SFX VOLUME: %d%%\n(Tap to Cycle)" % int(GameState.sfx_volume * 100.0)
		sfx_btn.pressed.connect(func() -> void:
			GameState.sfx_volume = 0.0 if GameState.sfx_volume > 0.8 else (GameState.sfx_volume + 0.4)
			GameState.save_game()
			refresh_ui()
		)
		stages_row.add_child(sfx_btn)
		var hand_btn := Button.new()
		hand_btn.custom_minimum_size = Vector2(260, 150)
		hand_btn.text = "LEFT-HANDED HUD: %s\n(Tap to Toggle)" % ("ON" if GameState.left_handed else "OFF")
		hand_btn.pressed.connect(func() -> void:
			GameState.left_handed = not GameState.left_handed
			GameState.save_game()
			refresh_ui()
		)
		stages_row.add_child(hand_btn)
		return

	# Default WORLDS tab: World Unlock Progression Graph Banner + 5 Stage Cards
	if GameState.is_world_unlocked(selected_world_id):
		var gw_lvl: int = selected_world_id * 5
		var gw_sec: int = int(GameState.goal_time_ms_for_level(gw_lvl) / 1000)
		gate_banner.text = "WORLD %d UNLOCKED  —  Clear Gateway Stage %d within <= %ds to unlock World %d!" % [
			selected_world_id, gw_lvl, gw_sec, min(selected_world_id + 1, 10)
		]
		gate_banner.modulate = Color(0.25, 0.95, 0.62)
	else:
		var prev_gw: int = (selected_world_id - 1) * 5
		var prev_goal_sec: int = int(GameState.goal_time_ms_for_level(prev_gw) / 1000)
		gate_banner.text = "LOCKED IN PROGRESSION GRAPH: Beat World %d Gateway Stage %d within <= %ds to unlock!" % [
			selected_world_id - 1, prev_gw, prev_goal_sec
		]
		gate_banner.modulate = Color(1.0, 0.36, 0.48)

	var start_lvl: int = (selected_world_id - 1) * 5 + 1
	for s in range(5):
		var lvl_num: int = start_lvl + s
		var unlocked: bool = GameState.is_level_unlocked(lvl_num)
		var info: Dictionary = GameState.level_progress.get(lvl_num, {})
		var goal_sec: int = int(GameState.goal_time_ms_for_level(lvl_num) / 1000)
		var goal_met: bool = bool(info.get("goal_met", false))
		var stars: int = int(info.get("stars", 0))
		var is_gate: bool = (s == 4) and (lvl_num < 50)

		var stage_btn := Button.new()
		stage_btn.custom_minimum_size = Vector2(215, 175)
		stage_btn.disabled = not unlocked
		var gate_tag: String = "\n[TEMPLE GATE]" if is_gate else ""
		var status_str: String = "GOAL MET (★%d)" % stars if goal_met else ("UNLOCKED" if unlocked else "LOCKED")
		stage_btn.text = "STAGE %d%s\nGoal: Finish <= %ds\n%s" % [lvl_num, gate_tag, goal_sec, status_str]
		stage_btn.pressed.connect(func() -> void:
			if unlocked:
				AudioSynth.play_sfx("click")
				GameState.current_level_number = lvl_num
				GameState.current_mode = selected_mode
				get_tree().change_scene_to_file("res://scenes/gameplay_3d.tscn")
		)
		stages_row.add_child(stage_btn)

func _on_mode_button_pressed() -> void:
	AudioSynth.play_sfx("click")
	selected_mode = (selected_mode + 1) % 6
	refresh_ui()

func _on_quick_play_pressed() -> void:
	AudioSynth.play_sfx("click")
	var best_unlocked: int = 1
	for i in range(1, 51):
		if GameState.is_level_unlocked(i):
			best_unlocked = i
	GameState.current_level_number = best_unlocked
	GameState.current_mode = selected_mode
	get_tree().change_scene_to_file("res://scenes/gameplay_3d.tscn")

func _on_worlds_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	active_tab = "WORLDS"
	refresh_ui()

func _on_characters_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	active_tab = "CHARACTERS"
	refresh_ui()

func _on_missions_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	active_tab = "MISSIONS"
	refresh_ui()

func _on_shop_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	active_tab = "SHOP"
	refresh_ui()

func _on_settings_tab_pressed() -> void:
	AudioSynth.play_sfx("click")
	active_tab = "SETTINGS"
	refresh_ui()
