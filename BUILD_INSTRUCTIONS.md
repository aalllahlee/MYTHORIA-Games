# DON'T FALL — MYTHORIA games (By ahmed) — Build & Release Instructions

## 1. Architecture & Why x86_64 is Required for Android Builds
Official Android SDK `aapt2` (Android Asset Packaging Tool 2) binaries distributed by Google for Linux are compiled for **`x86_64` (Intel/AMD)**.
- Running `./gradlew assembleDebug` on a Linux **ARM64 (`aarch64`)** container without `x86_64` emulation will fail when `aapt2` executes, regardless of `compileSdk`.
- Therefore, always build this project on one of the following:
  1. **Google AI Studio Build** (directly inside this workspace via **Settings ⚙️ -> Generate APK** or `gradle assembleDebug`, where the verified APK is already built at `app/build/outputs/apk/debug/app-debug.apk`).
  2. **GitHub Actions (`ubuntu-24.04` x86_64 runner)** using the included `.github/workflows/android-build.yml` workflow.
  3. **Android Studio** on a Windows, macOS, or Linux `x86_64` machine.

---

## 2. Project Identity & SDK Configuration
- **Application Name:** `DON'T FALL` (`MYTHORIA games — By ahmed`)
- **Application ID (`applicationId`):** `com.allouchgames.dontfall`
- **Namespace (`namespace`):** `com.example`
- **Version Code / Name:** `versionCode = 1`, `versionName = "1.0"`
- **Minimum SDK (`minSdk`):** `24` (Android 7.0+)
- **Target SDK (`targetSdk`):** `36`
- **Compile SDK (`compileSdk`):** `36`
- **Graphics Engine:** Native OpenGL ES 3.0 (`glEsVersion = 0x00030000`) + Jetpack Compose UI + Room SQLite + Procedural PCM SoundPool Audio.

---

## 3. Required Toolchain
- **JDK:** Java 17 (Temurin / OpenJDK 17)
- **Android SDK Packages:**
  - `platform-tools`
  - `platforms;android-36` (and `platforms;android-35`)
  - `build-tools;36.0.0` (or `build-tools;35.0.0`)
- **Gradle Wrapper:** Included (`./gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`).

---

## 4. Building the Debug APK (`app-debug.apk`)
1. Ensure `debug.keystore` exists in the project root (if missing, decode `debug.keystore.base64` or generate one):
   ```bash
   if [ ! -f debug.keystore ] && [ -f debug.keystore.base64 ]; then
     base64 -d debug.keystore.base64 > debug.keystore
   fi
   ```
2. Run the Debug build and Unit Test suite:
   ```bash
   chmod +x ./gradlew
   ./gradlew testDebugUnitTest assembleDebug
   ```
3. **Output Path:**
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 5. Building the Signed Release APK (`app-release.apk`)
Release signing is configured through environment variables in `app/build.gradle.kts` so private keys are never hardcoded:
- `KEYSTORE_PATH`: Absolute path to your `.jks` release keystore (defaults to `${rootDir}/my-upload-key.jks`, key alias `upload`).
- `STORE_PASSWORD`: Keystore password.
- `KEY_PASSWORD`: Key password.

### Commands to Generate a Keystore and Build Release APK:
```bash
keytool -genkeypair -v \
  -keystore my-upload-key.jks \
  -storepass YOUR_STRONG_PASSWORD \
  -alias upload \
  -keypass YOUR_STRONG_PASSWORD \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -dname "CN=Ahmed,OU=MythoriaGames,O=Mythoria,C=SA"

export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
export STORE_PASSWORD="YOUR_STRONG_PASSWORD"
export KEY_PASSWORD="YOUR_STRONG_PASSWORD"

./gradlew assembleRelease
```
- **Output Path:**
  ```text
  app/build/outputs/apk/release/app-release.apk
  ```

---

## 6. Automated GitHub Actions APK Build (`dont-fall-debug-apk`)
1. Push this project to a GitHub repository (or use **Push to GitHub** in Google AI Studio).
2. Open the **Actions** tab in your GitHub repository.
3. The workflow **`Android Build (DON'T FALL — MYTHORIA)`** (`.github/workflows/android-build.yml`) will automatically run on `ubuntu-24.04` (`x86_64`), execute all unit tests, compile the APK, and upload the installable artifact named:
   - **`dont-fall-debug-apk`** (`app-debug.apk`)

---

## 7. Installing the APK on an Android Device
- Transfer `app-debug.apk` (or `app-release.apk`) to your Android phone and tap it to install, or install via ADB from your computer:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
