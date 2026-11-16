extends Node

const SAVE_PATH := "user://dont_fall_save.cfg"

enum GameMode {
	CAREER,
	RIVAL_RACE,
	ENDLESS,
	TIME_ATTACK,
	PRACTICE,
	DAILY_CHALLENGE
}

var coins: int = 120
var player_xp: int = 0
var selected_skin_id: String = "red_hero"
var selected_hat_id: String = "hat_none"
var selected_trail_id: String = "trail_dust"
var dash_unlocked: bool = false
var music_volume: float = 0.75
var sfx_volume: float = 0.85
var haptics_enabled: bool = true
var left_handed: bool = false
var language: String = "EN"

var current_level_number: int = 1
var current_mode: int = GameMode.CAREER

# Progression Graph Storage:
# level_progress[level_num] = { "unlocked": bool, "completed": bool, "stars": int, "best_ms": int, "goal_met": bool, "goal_ms": int }
var level_progress: Dictionary = {}
# world_progress[world_id] = { "unlocked": bool, "gateway_level": int, "gateway_goal_met": bool }
var world_progress: Dictionary = {}
var owned_items: Dictionary = {
	"red_hero": true,
	"hat_none": true,
	"trail_dust": true
}

const WORLDS := [
	{"id": 1, "name": "Ancient Greek Islands", "sky_top": Color(0.055, 0.231, 0.549), "sky_bottom": Color(0.392, 0.78, 1.0), "primary": Color(0.886, 0.91, 0.941), "secondary": Color(0.0, 0.761, 1.0), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 2, "name": "Floating Temples", "sky_top": Color(0.075, 0.176, 0.416), "sky_bottom": Color(0.498, 0.89, 1.0), "primary": Color(0.796, 0.835, 0.882), "secondary": Color(0.0, 0.467, 1.0), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 3, "name": "Zeus Thunder Arena", "sky_top": Color(0.035, 0.082, 0.22), "sky_bottom": Color(0.114, 0.306, 0.847), "primary": Color(0.118, 0.161, 0.231), "secondary": Color(0.0, 0.831, 1.0), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 4, "name": "Golden Bridges", "sky_top": Color(0.118, 0.106, 0.294), "sky_bottom": Color(0.961, 0.62, 0.043), "primary": Color(1.0, 0.824, 0.169), "secondary": Color(0.851, 0.467, 0.024), "accent": Color(0.0, 0.898, 1.0)},
	{"id": 5, "name": "Ancient Marble Platforms", "sky_top": Color(0.106, 0.286, 0.396), "sky_bottom": Color(0.792, 0.914, 1.0), "primary": Color(0.945, 0.961, 0.976), "secondary": Color(0.384, 0.714, 0.796), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 6, "name": "Magical Portals", "sky_top": Color(0.063, 0.0, 0.169), "sky_bottom": Color(0.235, 0.035, 0.424), "primary": Color(0.141, 0.0, 0.275), "secondary": Color(0.0, 0.706, 0.847), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 7, "name": "Mythological Gardens", "sky_top": Color(0.059, 0.239, 0.243), "sky_bottom": Color(0.392, 0.875, 0.875), "primary": Color(0.18, 0.898, 0.553), "secondary": Color(0.886, 0.91, 0.941), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 8, "name": "Crystal Caves", "sky_top": Color(0.027, 0.063, 0.18), "sky_bottom": Color(0.0, 0.467, 0.714), "primary": Color(0.0, 0.706, 0.847), "secondary": Color(0.447, 0.035, 0.718), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 9, "name": "Floating Castles", "sky_top": Color(0.027, 0.043, 0.118), "sky_bottom": Color(0.176, 0.118, 0.42), "primary": Color(0.796, 0.835, 0.882), "secondary": Color(0.263, 0.38, 0.933), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 10, "name": "Ancient Ruins", "sky_top": Color(0.039, 0.071, 0.173), "sky_bottom": Color(0.118, 0.227, 0.541), "primary": Color(1.0, 0.824, 0.169), "secondary": Color(0.2, 0.255, 0.333), "accent": Color(0.0, 0.898, 1.0)}
]

const SKINS := [
	{"id": "red_hero", "name": "Young Greek Hero", "cost": 0, "hoodie": Color(0.902, 0.18, 0.302), "pants": Color(0.118, 0.161, 0.286)},
	{"id": "golden_champ", "name": "Golden Warrior", "cost": 250, "hoodie": Color(1.0, 0.718, 0.012), "pants": Color(0.486, 0.302, 0.0)},
	{"id": "royal_king", "name": "Royal Guardian", "cost": 350, "hoodie": Color(0.118, 0.227, 0.541), "pants": Color(0.039, 0.071, 0.173)},
	{"id": "panda", "name": "Mythical Explorer", "cost": 450, "hoodie": Color(0.114, 0.478, 0.988), "pants": Color(0.133, 0.149, 0.204)},
	{"id": "ice_warrior", "name": "Thunder Champion", "cost": 600, "hoodie": Color(0.0, 0.706, 0.847), "pants": Color(0.012, 0.016, 0.369)},
	{"id": "knight", "name": "Ancient Knight", "cost": 750, "hoodie": Color(0.58, 0.639, 0.722), "pants": Color(0.278, 0.333, 0.412)},
	{"id": "blue_ninja", "name": "Shadow Warrior", "cost": 1000, "hoodie": Color(0.094, 0.094, 0.106), "pants": Color(0.035, 0.035, 0.043)},
	{"id": "mythoria_legend", "name": "Celestial Hero", "cost": 1500, "hoodie": Color(1.0, 0.824, 0.169), "pants": Color(0.039, 0.141, 0.388)}
]

func _ready() -> void:
	init_progression_graph()
	load_game()

func goal_time_ms_for_level(level_num: int) -> int:
	var world_id: int = int((level_num - 1) / 5) + 1
	var stage_in_world: int = ((level_num - 1) % 5) + 1
	var segment_count: int = 5 + stage_in_world + int(world_id / 3)
	var par_sec: float = 24.0 + float(segment_count) * 4.5
	var raw_sec: float = par_sec if stage_in_world == 5 else par_sec * 1.15
	return int(ceil(raw_sec) * 1000.0)

func init_progression_graph() -> void:
	for i in range(1, 51):
		if not level_progress.has(i):
			level_progress[i] = {
				"unlocked": i == 1,
				"completed": false,
				"stars": 0,
				"best_ms": 0,
				"goal_met": false,
				"goal_ms": goal_time_ms_for_level(i)
			}
	for w in range(1, 11):
		if not world_progress.has(w):
			world_progress[w] = {
				"unlocked": w == 1,
				"gateway_level": w * 5,
				"gateway_goal_met": false
			}
	reconcile_progression_graph()

func reconcile_progression_graph() -> void:
	# Traverse directed world graph: World (w + 1) unlocks iff World (w) is unlocked AND gateway level (w * 5) met its goal
	world_progress[1]["unlocked"] = true
	for w in range(1, 10):
		var gw_lvl: int = w * 5
		var gw_met: bool = level_progress.get(gw_lvl, {}).get("goal_met", false)
		world_progress[w]["gateway_goal_met"] = gw_met
		var next_unlocked: bool = world_progress[w]["unlocked"] and gw_met
		world_progress[w + 1]["unlocked"] = next_unlocked

	# Traverse level graph: Level (n + 1) unlocks iff Level (n) is unlocked AND goal_met AND world is unlocked
	level_progress[1]["unlocked"] = true
	for lvl in range(2, 51):
		var w_id: int = int((lvl - 1) / 5) + 1
		var prev_ok: bool = level_progress[lvl - 1]["unlocked"] and level_progress[lvl - 1]["goal_met"]
		var world_ok: bool = world_progress[w_id]["unlocked"]
		level_progress[lvl]["unlocked"] = prev_ok and world_ok

func is_world_unlocked(world_id: int) -> bool:
	return world_progress.get(world_id, {}).get("unlocked", world_id == 1)

func is_level_unlocked(level_num: int) -> bool:
	var w_id: int = int((level_num - 1) / 5) + 1
	return is_world_unlocked(w_id) and level_progress.get(level_num, {}).get("unlocked", level_num == 1)

func record_level_win(level_num: int, elapsed_ms: int, coins_collected: int, placement: int = 1) -> Dictionary:
	var entry: Dictionary = level_progress.get(level_num, {
		"unlocked": true,
		"completed": false,
		"stars": 0,
		"best_ms": 0,
		"goal_met": false,
		"goal_ms": goal_time_ms_for_level(level_num)
	})
	var goal_ms: int = entry["goal_ms"]
	var goal_met_now: bool = (placement == 1) and (elapsed_ms <= goal_ms)
	var stars: int = 3 if elapsed_ms <= int(goal_ms * 0.78) else (2 if elapsed_ms <= goal_ms else 1)

	var was_next_world_unlocked: bool = false
	var w_id: int = int((level_num - 1) / 5) + 1
	if w_id < 10:
		was_next_world_unlocked = is_world_unlocked(w_id + 1)

	entry["completed"] = true
	entry["stars"] = max(int(entry["stars"]), stars)
	if int(entry["best_ms"]) == 0 or elapsed_ms < int(entry["best_ms"]):
		entry["best_ms"] = elapsed_ms
	entry["goal_met"] = bool(entry["goal_met"]) or goal_met_now
	level_progress[level_num] = entry

	reconcile_progression_graph()

	var newly_unlocked_world: int = 0
	if w_id < 10 and not was_next_world_unlocked and is_world_unlocked(w_id + 1):
		newly_unlocked_world = w_id + 1

	var reward_coins: int = 50 + level_num * 6 + coins_collected
	coins += reward_coins
	player_xp += 350 + stars * 75 + coins_collected * 2
	save_game()

	return {
		"stars": stars,
		"reward_coins": reward_coins,
		"goal_met": goal_met_now,
		"goal_ms": goal_ms,
		"newly_unlocked_world": newly_unlocked_world,
		"can_advance": level_num < 50 and is_level_unlocked(level_num + 1)
	}

func get_world_theme(world_id: int) -> Dictionary:
	var idx: int = clampi(world_id - 1, 0, WORLDS.size() - 1)
	return WORLDS[idx]

func get_skin_def(skin_id: String) -> Dictionary:
	for s in SKINS:
		if s["id"] == skin_id:
			return s
	return SKINS[0]

func save_game() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("profile", "coins", coins)
	cfg.set_value("profile", "player_xp", player_xp)
	cfg.set_value("profile", "selected_skin_id", selected_skin_id)
	cfg.set_value("profile", "selected_hat_id", selected_hat_id)
	cfg.set_value("profile", "selected_trail_id", selected_trail_id)
	cfg.set_value("profile", "dash_unlocked", dash_unlocked)
	cfg.set_value("profile", "music_volume", music_volume)
	cfg.set_value("profile", "sfx_volume", sfx_volume)
	cfg.set_value("profile", "haptics_enabled", haptics_enabled)
	cfg.set_value("profile", "left_handed", left_handed)
	cfg.set_value("profile", "language", language)
	cfg.set_value("progression", "levels", level_progress)
	cfg.set_value("progression", "worlds", world_progress)
	cfg.set_value("progression", "owned", owned_items)
	cfg.save(SAVE_PATH)

func load_game() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(SAVE_PATH) != OK:
		return
	coins = int(cfg.get_value("profile", "coins", 120))
	player_xp = int(cfg.get_value("profile", "player_xp", 0))
	selected_skin_id = str(cfg.get_value("profile", "selected_skin_id", "red_hero"))
	selected_hat_id = str(cfg.get_value("profile", "selected_hat_id", "hat_none"))
	selected_trail_id = str(cfg.get_value("profile", "selected_trail_id", "trail_dust"))
	dash_unlocked = bool(cfg.get_value("profile", "dash_unlocked", false))
	music_volume = float(cfg.get_value("profile", "music_volume", 0.75))
	sfx_volume = float(cfg.get_value("profile", "sfx_volume", 0.85))
	haptics_enabled = bool(cfg.get_value("profile", "haptics_enabled", true))
	left_handed = bool(cfg.get_value("profile", "left_handed", false))
	language = str(cfg.get_value("profile", "language", "EN"))
	var loaded_levels = cfg.get_value("progression", "levels", {})
	if typeof(loaded_levels) == TYPE_DICTIONARY:
		for k in loaded_levels.keys():
			level_progress[int(k)] = loaded_levels[k]
	var loaded_worlds = cfg.get_value("progression", "worlds", {})
	if typeof(loaded_worlds) == TYPE_DICTIONARY:
		for k in loaded_worlds.keys():
			world_progress[int(k)] = loaded_worlds[k]
	var loaded_owned = cfg.get_value("progression", "owned", {})
	if typeof(loaded_owned) == TYPE_DICTIONARY:
		owned_items = loaded_owned
	reconcile_progression_graph()
