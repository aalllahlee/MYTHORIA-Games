# DON'T FALL — Godot 4 Android Export Guide
**Studio:** MYTHORIA games — By ahmed  
**Application Name:** `DON'T FALL`  
**Android Package Name:** `com.mythoriagames.dontfall`  
**Orientation:** Landscape (`window/handheld/orientation=6` / `sensorLandscape`)  
**Target Architecture:** `arm64-v8a`  
**Renderer:** `mobile` (Vulkan Mobile / OpenGL ES 3.0 fallback with ETC2/ASTC compression)

---

## 1. Verified Project Structure (`godot_project/`)
- `project.godot` — Main Godot 4 configuration (`run/main_scene="res://scenes/studio_splash.tscn"`, autoloads `GameState` and `AudioSynth`, mobile renderer, landscape orientation).
- `export_presets.cfg` — Pre-configured Android export preset (`package/unique_name="com.mythoriagames.dontfall"`, `package/name="DON'T FALL"`, `architectures/arm64-v8a=true`, `min_sdk=24`, `target_sdk=34`, `permissions/vibrate=true` only, 100% offline).
- `icon.svg` & `icon_bg.svg` — Custom adaptive/main launcher icon (`512x512` vector emblem with 5-peak golden crown, angelic wings, blue sapphire diamonds, `MYTHORIA games / By ahmed`, and `DON'T FALL!` crest).
- `assets/mythoria_splash_art.png` — Official high-resolution Mythoria ancient Greek sky temple & golden crest reference artwork.
- `scenes/studio_splash.tscn` & `scripts/studio_splash.gd` — 8-second **MYTHORIA games / By ahmed** cinematic intro sequence with threaded scene preloading.
- `scenes/main_menu.tscn` & `scripts/main_menu.gd` — Mythological Main Menu (`PLAY`, `WORLDS`, `CHARACTERS`, `MISSIONS`, `SHOP`, `SETTINGS`) with the 10-World Progression Graph and stage cards.
- `scenes/gameplay_3d.tscn`, `scripts/gameplay_3d.gd`, & `scripts/player_controller.gd` — 3D obstacle course, ancient Greek marble temple columns, glowing blue sapphire portal finish arch, coyote-time/jump-buffer physics, dive/dash, rotating hammers, coins, and time-goal progression evaluation.
- `scripts/game_state.gd` & `scripts/audio_synth.gd` — Local `user://dont_fall_save.cfg` persistence (`ConfigFile`), World Unlocking progression graph (`level_progress` & `world_progress`), 10 mythological worlds, 8 Olympian hero skins, and procedural zero-dependency audio synthesizer.

---

## 2. Exporting the Android APK in Godot 4 (GUI)
1. Extract `DONT_FALL_Godot4_Project.zip` and open `project.godot` in **Godot Engine 4.2+**.
2. Ensure Godot's Android Export Templates are installed:
   - Go to **Editor -> Manage Export Templates...** and click **Download and Install** if not already installed.
3. Verify Android SDK & Keystore settings in **Editor -> Editor Settings -> Export -> Android**:
   - **Android SDK Path:** Point to your local Android SDK directory (must contain `platform-tools`, `build-tools`, and `platforms;android-34`).
   - **Java SDK Path:** Point to JDK 17.
   - **Debug Keystore / Release Keystore:** Configure your keystore path, user, and password.
4. Open **Project -> Export...**:
   - Select the pre-configured **Android** preset (`com.mythoriagames.dontfall`).
   - If building a Release APK, enter your Release Keystore path, alias, and password under **Keystore**.
   - Click **Export Project...**, uncheck **Export With Debug** for a release build (or keep checked for direct testing), and save as `DONT_FALL_MythoriaGames.apk`.

---

## 3. Headless Command-Line APK Build (CLI)
To build the APK directly from a terminal where `godot` (Godot 4.2+ binary) and Android Export Templates are installed:

```bash
# Release APK build:
godot --headless --path ./godot_project --export-release "Android" ./DONT_FALL_MythoriaGames_Release.apk

# Debug APK build (for quick device testing):
godot --headless --path ./godot_project --export-debug "Android" ./DONT_FALL_MythoriaGames_Debug.apk
```
