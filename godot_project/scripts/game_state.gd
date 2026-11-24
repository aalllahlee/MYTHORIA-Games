extends Node

## MYTHORIA games — By ahmed
## Global GameState Autoload Singleton:
## Manages 10 Mythological Worlds, 50 Stages, World Unlocking Progression Graph,
## 15 Mythoria 3D Character Skins, Missions, Shop Upgrades, Audio/Graphics Settings, and Save/Load.

const SAVE_PATH := "user://dont_fall_save.cfg"
const TOTAL_WORLDS: int = 10
const TOTAL_LEVELS: int = 50

enum GameMode {
	CAREER,
	RIVAL_RACE,
	ENDLESS,
	TIME_ATTACK,
	PRACTICE,
	DAILY_CHALLENGE
}

var coins: int = 250
var player_xp: int = 0
var player_level: int = 1
var selected_skin_id: int = 0
var unlocked_skins: Array = [0]
var selected_hat_id: String = "hat_none"
var selected_trail_id: int = 0
var unlocked_trails: Array = [0]
var dash_unlocked: bool = false

var sound_enabled: bool = true
var music_volume: float = 0.75
var sfx_volume: float = 0.85
var haptics_enabled: bool = true
var left_handed: bool = false
var graphics_quality: int = 2
var language_code: String = "en"
var language: String = "EN"

var current_level_number: int = 1
var current_mode: int = GameMode.CAREER
var selected_game_mode: String = "CAREER"

var daily_gift_claimed: bool = false
var mission_runs_completed: int = 0
var mission_coins_collected: int = 0
var mission_perfect_runs: int = 0
var mission_claimed: Array = []

# Progression Graph Storage:
# level_progress[level_num] = { "unlocked": bool, "completed": bool, "stars": int, "best_ms": int, "goal_met": bool, "goal_ms": int }
var level_progress: Dictionary = {}
# world_progress[world_id] = { "unlocked": bool, "gateway_level": int, "gateway_goal_met": bool }
var world_progress: Dictionary = {}

const WORLDS := [
	{"id": 1, "name": "Ancient Greek Islands", "sky_top": Color(0.055, 0.231, 0.549), "sky_bottom": Color(0.392, 0.78, 1.0), "primary": Color(0.16, 0.58, 0.98), "secondary": Color(0.0, 0.78, 1.0), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 2, "name": "Floating Temples", "sky_top": Color(0.075, 0.176, 0.416), "sky_bottom": Color(0.498, 0.89, 1.0), "primary": Color(0.22, 0.72, 0.96), "secondary": Color(0.0, 0.467, 1.0), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 3, "name": "Zeus Thunder Arena", "sky_top": Color(0.035, 0.082, 0.22), "sky_bottom": Color(0.114, 0.306, 0.847), "primary": Color(0.24, 0.38, 0.88), "secondary": Color(0.0, 0.831, 1.0), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 4, "name": "Golden Bridges", "sky_top": Color(0.118, 0.106, 0.294), "sky_bottom": Color(0.961, 0.62, 0.043), "primary": Color(1.0, 0.76, 0.14), "secondary": Color(0.851, 0.467, 0.024), "accent": Color(0.0, 0.898, 1.0)},
	{"id": 5, "name": "Ancient Marble Platforms", "sky_top": Color(0.106, 0.286, 0.396), "sky_bottom": Color(0.792, 0.914, 1.0), "primary": Color(0.32, 0.68, 0.92), "secondary": Color(0.384, 0.714, 0.796), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 6, "name": "Magical Portals", "sky_top": Color(0.063, 0.0, 0.169), "sky_bottom": Color(0.235, 0.035, 0.424), "primary": Color(0.58, 0.22, 0.88), "secondary": Color(0.0, 0.706, 0.847), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 7, "name": "Mythological Gardens", "sky_top": Color(0.059, 0.239, 0.243), "sky_bottom": Color(0.392, 0.875, 0.875), "primary": Color(0.18, 0.82, 0.48), "secondary": Color(0.886, 0.91, 0.941), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 8, "name": "Crystal Caves", "sky_top": Color(0.027, 0.063, 0.18), "sky_bottom": Color(0.0, 0.467, 0.714), "primary": Color(0.0, 0.706, 0.847), "secondary": Color(0.447, 0.035, 0.718), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 9, "name": "Floating Castles", "sky_top": Color(0.027, 0.043, 0.118), "sky_bottom": Color(0.176, 0.118, 0.42), "primary": Color(0.36, 0.48, 0.94), "secondary": Color(0.263, 0.38, 0.933), "accent": Color(1.0, 0.824, 0.169)},
	{"id": 10, "name": "Ancient Ruins", "sky_top": Color(0.039, 0.071, 0.173), "sky_bottom": Color(0.118, 0.227, 0.541), "primary": Color(1.0, 0.78, 0.16), "secondary": Color(0.2, 0.255, 0.333), "accent": Color(0.0, 0.898, 1.0)}
]

const SKIN_CATALOG := [
	{"id": 0, "name": "Young Greek Hero", "rarity": "COMMON", "price": 0, "primary": Color(0.92, 0.18, 0.22), "secondary": Color(1.0, 0.82, 0.18)},
	{"id": 1, "name": "Olympian Champion", "rarity": "RARE", "price": 150, "primary": Color(1.0, 0.75, 0.08), "secondary": Color(0.0, 0.86, 1.0)},
	{"id": 2, "name": "Winged Runner", "rarity": "RARE", "price": 200, "primary": Color(0.14, 0.52, 0.98), "secondary": Color(1.0, 0.84, 0.22)},
	{"id": 3, "name": "Crystal Guardian", "rarity": "RARE", "price": 250, "primary": Color(0.0, 0.78, 0.96), "secondary": Color(0.68, 0.94, 1.0)},
	{"id": 4, "name": "Golden Crown King", "rarity": "EPIC", "price": 320, "primary": Color(0.88, 0.16, 0.32), "secondary": Color(1.0, 0.84, 0.16)},
	{"id": 5, "name": "Marble Knight", "rarity": "EPIC", "price": 400, "primary": Color(0.75, 0.82, 0.92), "secondary": Color(1.0, 0.82, 0.18)},
	{"id": 6, "name": "Shadow Assassin", "rarity": "EPIC", "price": 450, "primary": Color(0.16, 0.18, 0.28), "secondary": Color(0.95, 0.22, 0.28)},
	{"id": 7, "name": "Mythoria Champion", "rarity": "LEGENDARY", "price": 500, "primary": Color(0.12, 0.32, 0.78), "secondary": Color(1.0, 0.84, 0.16)},
	{"id": 8, "name": "Thunder Mage", "rarity": "EPIC", "price": 550, "primary": Color(0.18, 0.42, 0.92), "secondary": Color(0.0, 0.92, 1.0)},
	{"id": 9, "name": "Sun Paladin", "rarity": "EPIC", "price": 600, "primary": Color(1.0, 0.58, 0.08), "secondary": Color(1.0, 0.88, 0.28)},
	{"id": 10, "name": "Pegasus Rider", "rarity": "LEGENDARY", "price": 700, "primary": Color(0.38, 0.68, 1.0), "secondary": Color(1.0, 0.86, 0.24)},
	{"id": 11, "name": "Valkyrie Flyer", "rarity": "LEGENDARY", "price": 800, "primary": Color(0.72, 0.22, 0.88), "secondary": Color(1.0, 0.84, 0.18)},
	{"id": 12, "name": "Temple Guardian", "rarity": "LEGENDARY", "price": 900, "primary": Color(0.14, 0.76, 0.52), "secondary": Color(1.0, 0.84, 0.18)},
	{"id": 13, "name": "Supreme Mythoria Legend", "rarity": "MYTHIC", "price": 1000, "primary": Color(1.0, 0.80, 0.12), "secondary": Color(0.0, 0.88, 1.0)},
	{"id": 14, "name": "Astral Immortal", "rarity": "MYTHIC", "price": 1200, "primary": Color(0.45, 0.18, 0.92), "secondary": Color(0.0, 0.92, 1.0)}
]

func _ready() -> void:
	# Strictly lock orientation to Landscape on Android phones & tablets
	DisplayServer.screen_set_orientation(DisplayServer.SCREEN_LANDSCAPE)
	init_progression_graph()
	load_game()

func goal_time_ms_for_level(level_num: int) -> int:
	var world_id: int = int((level_num - 1) / 5) + 1
	var stage_in_world: int = ((level_num - 1) % 5) + 1
	var segment_count: int = 5 + stage_in_world + int(world_id / 3)
	var par_sec: float = 28.0 + float(segment_count) * 5.0
	var raw_sec: float = par_sec if stage_in_world == 5 else par_sec * 1.18
	if level_num == 1:
		raw_sec = maxf(raw_sec, 65.0)
	return int(ceil(raw_sec) * 1000.0)

func get_stage_difficulty_profile(level_num: int) -> Dictionary:
	var w_id: int = clampi(int((level_num - 1) / 5) + 1, 1, TOTAL_WORLDS)
	var s_in_w: int = ((level_num - 1) % 5) + 1
	var diff_norm: float = clampf(float(level_num - 1) / 49.0, 0.0, 1.0)

	if level_num == 1:
		return {
			"tier_name": "VERY EASY",
			"tier_color": Color(0.24, 0.96, 0.54),
			"new_obstacle": "Speed Boost & Coins",
			"platform_width": 9.6,
			"gap_len": 1.25,
			"obstacle_speed": 0.58,
			"seg_count": 5,
			"allow_moving": false,
			"allow_pendulum": false,
			"allow_falling": false,
			"allow_push_block": false,
			"allow_boulder": false,
			"allow_bounce": false,
			"hammer_count_mod": 5
		}
	elif level_num == 2:
		return {
			"tier_name": "EASY",
			"tier_color": Color(0.35, 0.92, 0.98),
			"new_obstacle": "+ Moving Platforms & Bounce",
			"platform_width": 8.8,
			"gap_len": 1.6,
			"obstacle_speed": 0.72,
			"seg_count": 6,
			"allow_moving": true,
			"allow_pendulum": false,
			"allow_falling": false,
			"allow_push_block": false,
			"allow_boulder": false,
			"allow_bounce": true,
			"hammer_count_mod": 4
		}
	elif level_num == 3:
		return {
			"tier_name": "NORMAL",
			"tier_color": Color(1.0, 0.86, 0.24),
			"new_obstacle": "+ Swinging Pendulums",
			"platform_width": 8.2,
			"gap_len": 1.9,
			"obstacle_speed": 0.88,
			"seg_count": 7,
			"allow_moving": true,
			"allow_pendulum": true,
			"allow_falling": false,
			"allow_push_block": false,
			"allow_boulder": false,
			"allow_bounce": true,
			"hammer_count_mod": 3
		}
	elif level_num == 4:
		return {
			"tier_name": "CHALLENGING",
			"tier_color": Color(1.0, 0.56, 0.16),
			"new_obstacle": "+ Falling Tiles & Push Blocks",
			"platform_width": 7.6,
			"gap_len": 2.2,
			"obstacle_speed": 1.04,
			"seg_count": 8,
			"allow_moving": true,
			"allow_pendulum": true,
			"allow_falling": true,
			"allow_push_block": true,
			"allow_boulder": false,
			"allow_bounce": true,
			"hammer_count_mod": 3
		}
	elif level_num == 5:
		return {
			"tier_name": "HARD",
			"tier_color": Color(1.0, 0.28, 0.34),
			"new_obstacle": "+ Rolling Mythoria Boulders!",
			"platform_width": 7.1,
			"gap_len": 2.45,
			"obstacle_speed": 1.16,
			"seg_count": 9,
			"allow_moving": true,
			"allow_pendulum": true,
			"allow_falling": true,
			"allow_push_block": true,
			"allow_boulder": true,
			"allow_bounce": true,
			"hammer_count_mod": 2
		}

	var tier_label: String = "NORMAL"
	var tier_col: Color = Color(1.0, 0.86, 0.24)
	if w_id <= 3:
		tier_label = "CHALLENGING" if s_in_w < 5 else "HARD"
		tier_col = Color(1.0, 0.56, 0.16) if s_in_w < 5 else Color(1.0, 0.28, 0.34)
	elif w_id <= 6:
		tier_label = "HARD" if s_in_w < 5 else "EXPERT"
		tier_col = Color(1.0, 0.32, 0.32) if s_in_w < 5 else Color(0.92, 0.20, 0.68)
	else:
		tier_label = "EXPERT" if s_in_w < 5 else "MYTHIC HARD"
		tier_col = Color(0.92, 0.20, 0.68) if s_in_w < 5 else Color(1.0, 0.18, 0.22)

	var new_tags := [
		"Fast Hammers & Boosters",
		"Moving + Push Gauntlet",
		"Twin Pendulum Run",
		"Blinking Tile Sprint",
		"Full Boulder Temple Trial"
	]
	return {
		"tier_name": tier_label,
		"tier_color": tier_col,
		"new_obstacle": new_tags[(s_in_w - 1) % new_tags.size()],
		"platform_width": clampf(7.6 - diff_norm * 1.9, 5.7, 7.6),
		"gap_len": clampf(2.1 + diff_norm * 1.35, 2.1, 3.45),
		"obstacle_speed": clampf(1.05 + diff_norm * 0.85, 1.05, 1.90),
		"seg_count": 6 + s_in_w + int(w_id / 3),
		"allow_moving": true,
		"allow_pendulum": true,
		"allow_falling": s_in_w >= 2 or w_id >= 3,
		"allow_push_block": s_in_w >= 2 or w_id >= 2,
		"allow_boulder": s_in_w >= 4 or w_id >= 4,
		"allow_bounce": true,
		"hammer_count_mod": 2 if w_id >= 4 else 3
	}

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
	world_progress[1]["unlocked"] = true
	for w in range(1, 10):
		var gw_lvl: int = w * 5
		var gw_met: bool = level_progress.get(gw_lvl, {}).get("goal_met", false)
		world_progress[w]["gateway_goal_met"] = gw_met
		var next_unlocked: bool = world_progress[w]["unlocked"] and gw_met
		world_progress[w + 1]["unlocked"] = next_unlocked

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

func get_unlocked_worlds_count() -> int:
	var count: int = 0
	for w in range(1, TOTAL_WORLDS + 1):
		if is_world_unlocked(w):
			count += 1
	return max(1, count)

func get_total_stars() -> int:
	var total: int = 0
	for lvl in level_progress.keys():
		total += int(level_progress[lvl].get("stars", 0))
	return total

func get_level_stars(lvl: int) -> int:
	return int(level_progress.get(lvl, {}).get("stars", 0))

func get_level_best_time_ms(lvl: int) -> int:
	return int(level_progress.get(lvl, {}).get("best_ms", 0))

func select_skin(skin_id: int) -> void:
	if unlocked_skins.has(skin_id):
		selected_skin_id = skin_id
		save_game()

func unlock_skin(skin_id: int, price: int) -> bool:
	if unlocked_skins.has(skin_id):
		selected_skin_id = skin_id
		save_game()
		return true
	if coins >= price:
		coins -= price
		unlocked_skins.append(skin_id)
		selected_skin_id = skin_id
		save_game()
		return true
	return false

func claim_daily_gift() -> int:
	if daily_gift_claimed:
		return 0
	daily_gift_claimed = true
	coins += 150
	save_game()
	return 150

func claim_mission(mission_id: int, reward: int) -> bool:
	if mission_claimed.has(mission_id):
		return false
	mission_claimed.append(mission_id)
	coins += reward
	save_game()
	return true

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
	player_level = 1 + int(player_xp / 500)
	mission_runs_completed += 1
	mission_coins_collected += coins_collected
	if goal_met_now:
		mission_perfect_runs += 1
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

func get_skin_def(skin_id_any) -> Dictionary:
	var sid: int = int(skin_id_any) if (typeof(skin_id_any) == TYPE_INT or typeof(skin_id_any) == TYPE_FLOAT) else 0
	for s in SKIN_CATALOG:
		if int(s["id"]) == sid:
			return s
	return SKIN_CATALOG[0]

func save_progress() -> void:
	save_game()

func save_game() -> void:
	var cfg := ConfigFile.new()
	cfg.set_value("profile", "coins", coins)
	cfg.set_value("profile", "player_xp", player_xp)
	cfg.set_value("profile", "player_level", player_level)
	cfg.set_value("profile", "selected_skin_id", selected_skin_id)
	cfg.set_value("profile", "unlocked_skins", unlocked_skins)
	cfg.set_value("profile", "selected_trail_id", selected_trail_id)
	cfg.set_value("profile", "unlocked_trails", unlocked_trails)
	cfg.set_value("profile", "dash_unlocked", dash_unlocked)
	cfg.set_value("profile", "sound_enabled", sound_enabled)
	cfg.set_value("profile", "graphics_quality", graphics_quality)
	cfg.set_value("profile", "language_code", language_code)
	cfg.set_value("profile", "daily_gift_claimed", daily_gift_claimed)
	cfg.set_value("profile", "mission_runs_completed", mission_runs_completed)
	cfg.set_value("profile", "mission_coins_collected", mission_coins_collected)
	cfg.set_value("profile", "mission_perfect_runs", mission_perfect_runs)
	cfg.set_value("profile", "mission_claimed", mission_claimed)
	cfg.set_value("progression", "levels", level_progress)
	cfg.set_value("progression", "worlds", world_progress)
	cfg.save(SAVE_PATH)

func load_game() -> void:
	var cfg := ConfigFile.new()
	if cfg.load(SAVE_PATH) != OK:
		return
	coins = int(cfg.get_value("profile", "coins", 250))
	player_xp = int(cfg.get_value("profile", "player_xp", 0))
	player_level = int(cfg.get_value("profile", "player_level", 1))
	selected_skin_id = int(cfg.get_value("profile", "selected_skin_id", 0))
	var loaded_skins = cfg.get_value("profile", "unlocked_skins", [0])
	if typeof(loaded_skins) == TYPE_ARRAY:
		unlocked_skins = loaded_skins
	if not unlocked_skins.has(0):
		unlocked_skins.append(0)
	selected_trail_id = int(cfg.get_value("profile", "selected_trail_id", 0))
	var loaded_trails = cfg.get_value("profile", "unlocked_trails", [0])
	if typeof(loaded_trails) == TYPE_ARRAY:
		unlocked_trails = loaded_trails
	dash_unlocked = bool(cfg.get_value("profile", "dash_unlocked", false))
	sound_enabled = bool(cfg.get_value("profile", "sound_enabled", true))
	graphics_quality = int(cfg.get_value("profile", "graphics_quality", 2))
	language_code = str(cfg.get_value("profile", "language_code", "en"))
	daily_gift_claimed = bool(cfg.get_value("profile", "daily_gift_claimed", false))
	mission_runs_completed = int(cfg.get_value("profile", "mission_runs_completed", 0))
	mission_coins_collected = int(cfg.get_value("profile", "mission_coins_collected", 0))
	mission_perfect_runs = int(cfg.get_value("profile", "mission_perfect_runs", 0))
	var loaded_mc = cfg.get_value("profile", "mission_claimed", [])
	if typeof(loaded_mc) == TYPE_ARRAY:
		mission_claimed = loaded_mc
	var loaded_levels = cfg.get_value("progression", "levels", {})
	if typeof(loaded_levels) == TYPE_DICTIONARY:
		for k in loaded_levels.keys():
			level_progress[int(k)] = loaded_levels[k]
	var loaded_worlds = cfg.get_value("progression", "worlds", {})
	if typeof(loaded_worlds) == TYPE_DICTIONARY:
		for k in loaded_worlds.keys():
			world_progress[int(k)] = loaded_worlds[k]
	reconcile_progression_graph()
