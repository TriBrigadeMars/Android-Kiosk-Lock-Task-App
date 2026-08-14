# CU Denver Equity — Android Kiosk Lock Task Mode

A complete toolkit for locking a Lenovo Tab K11 Gen 2 (Android 15) tablet into kiosk mode, displaying the [CU Denver Office of Equity](https://www.ucdenver.edu/offices/equity) website with restricted navigation. Includes a portable Windows GUI for non-technical users and macOS build scripts.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Quick Start (Windows)](#quick-start-windows)
3. [Quick Start (macOS)](#quick-start-macos)
4. [Desktop GUI Reference](#desktop-gui-reference)
5. [Android Kiosk App Details](#android-kiosk-app-details)
6. [Building the APK](#building-the-apk)
7. [Remote Management](#remote-management)
8. [Troubleshooting](#troubleshooting)
9. [Project Files](#project-files)
10. [Revision History](#revision-history)

---

## Architecture Overview

```
┌──────────────────────────────────────────────┐
│                  Desktop PC                   │
│  ┌────────────────────────────────────────┐  │
│  │  CU-Denver-Equity-Kiosk-Manager.exe    │  │
│  │  (Self-contained: ADB + APK bundled)   │  │
│  │                                        │  │
│  │  🔒 Setup Kiosk    🔓 Exit Kiosk      │  │
│  │  📱 Restore Apps   🌐 Remote Setup     │  │
│  │  🗖 Fullscreen     🌙 Dark Mode       │  │
│  │  ♿ Accessibility  ⚠ Factory Reset    │  │
│  └──────────────┬─────────────────────────┘  │
│                 │ USB / Wi-Fi ADB             │
└─────────────────┼─────────────────────────────┘
                  │
┌─────────────────┼─────────────────────────────┐
│                 ▼           Lenovo Tablet      │
│  ┌──────────────────────────────────────────┐ │
│  │           Lock Task Mode                  │ │
│  │  ┌────────────────────────────────────┐  │ │
│  │  │  Custom Launcher (Home Screen)    │  │ │
│  │  │  ┌──────────────────────────┐     │  │ │
│  │  │  │        ⏰  10:30 AM      │     │  │ │
│  │  │  │   CU Denver Equity       │     │  │ │
│  │  │  │      Tap to open         │     │  │ │
│  │  │  └──────────────────────────┘     │  │ │
│  │  └────────────────────────────────────┘  │ │
│  │                    ↕                      │ │
│  │  ┌────────────────────────────────────┐  │ │
│  │  │  Kiosk WebView App                │  │ │
│  │  │  ucdenver.edu + cm.maxient.com    │  │ │
│  │  │  Fullscreen immersive mode        │  │ │
│  │  └────────────────────────────────────┘  │ │
│  └──────────────────────────────────────────┘ │
│  Allowlisted apps: com.example.webkiosk only   │
│  All other apps: hidden (pm disable-user)      │
│  Device owner: Test DPC                        │
└────────────────────────────────────────────────┘
```

**Security model:** The tablet runs in Android Lock Task Mode with only `com.example.webkiosk` allowlisted. The custom launcher shows a single button and a clock — no app drawer, no settings access, no way to escape without ADB.

---

## Quick Start (Windows)

### Prerequisites
- Lenovo Tab K11 Gen 2 (Android 15)
- USB cable
- Windows 10/11 PC

### One-time Setup

```bash
# 1. Install developer tools (ADB, JDK 17, Android SDK) — run once
setup-tools.bat

# 2. Build the kiosk APK
bash build-apk.sh

# 3. Package the GUI
python -m PyInstaller --onefile --windowed --name "CU-Denver-Equity-Kiosk-Manager" ^
  --add-data "platform-tools;platform-tools" ^
  --add-data "build/CU-Denver-Equity-Kiosk.apk;." ^
  kiosk-manager.py
```

### On the Tablet

1. **Enable Developer Options:** Settings → About Tablet → tap Build Number 7 times
2. **Enable USB Debugging:** Settings → System → Developer Options → USB Debugging → ON
3. **Remove all Google accounts:** Settings → Passwords & Accounts → remove each
4. **Install Test DPC:** From Google Play Store, install "Test DPC" by Android Enterprise

### Deploy Kiosk

1. Connect tablet via USB, accept the debugging prompt
2. Double-click `dist/CU-Denver-Equity-Kiosk-Manager.exe`
3. Click **🔒 Setup Kiosk** — runs 6 automated steps
4. On the tablet, open Test DPC → Lock Task Mode → verify only `com.example.webkiosk` is allowlisted
5. Tap the **CU Denver Equity** app to enter kiosk mode

---

## Quick Start (macOS)

```bash
# 1. Install tools
chmod +x setup-tools-mac.sh build-apk-mac.sh package-mac.sh
./setup-tools-mac.sh

# 2. Build everything into a .dmg
./package-mac.sh

# 3. Distribute CU-Denver-Equity-Kiosk-Manager.dmg
```

Or use GitHub Actions (`.github/workflows/build-dmg.yml`) to build the .dmg automatically on push.

---

## Desktop GUI Reference

The `dist/CU-Denver-Equity-Kiosk-Manager.exe` is a fully self-contained 27 MB single file — no installer, no dependencies.

### Buttons

| Button | Function |
|--------|----------|
| **🔒 Setup Kiosk** | 6-step automated setup: verify device, set device owner, install APK, set custom launcher as home, hide non-essential apps, verify allowlist |
| **🔓 Exit Kiosk** | Force-stops the kiosk app — tablet returns to launcher |
| **📱 Restore All Apps** | Re-enables all hidden apps (Chrome, Settings, Play Store, etc.) |
| **🌐 Setup Remote Management** | Guided wizard for Android Management API enrollment (optional) |
| **⚠ Factory Reset Tablet** | Wipes tablet to factory state (double confirmation required) |
| **🗖 Fullscreen (F11)** | Toggles fullscreen (Esc to exit) |
| **🌙 Dark Mode** | Toggles dark theme for the GUI itself |
| **♿ Accessibility** | 40% larger text, high-contrast focus indicators, larger buttons |

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| F11 | Toggle fullscreen |
| Esc | Exit fullscreen |

---

## Android Kiosk App Details

Package: `com.example.webkiosk`

### Components

| Component | Class | Purpose |
|-----------|-------|---------|
| Kiosk Activity | `MainActivity` | Fullscreen WebView locked to allowed domains |
| Custom Launcher | `LauncherActivity` | Home screen replacement — shows clock + one app icon |

### Allowed Domains
- `ucdenver.edu` (CU Denver Office of Equity)
- `cm.maxient.com` (Maxient reporting form)

### Features

- **Fullscreen immersive mode** — status bar and nav bar hidden
- **Domain locking** — navigation outside allowed domains is blocked
- **Error resilience** — page load failures show a friendly error page with Retry button
- **Back navigation** — back gesture/button navigates WebView history; at start page, returns to launcher
- **Lock task auto-entry** — automatically enters lock task mode on resume
- **Memory management** — WebView paused on background, properly destroyed on exit
- **Crash prevention** — try/catch guards on lock task calls, lifecycle race prevention

### Custom Launcher

- Navy blue background (#1a3c6d)
- Live clock (HH:MM AM/PM, updates every second)
- Single "CU Denver Equity" button → launches kiosk app
- No app drawer, no settings, no escape hatch

---

## Building the APK

### Windows
```bash
bash build-apk.sh
```

### macOS
```bash
./build-apk-mac.sh
```

### Build Process (manual)

The build script uses the Android SDK command-line tools (no Android Studio needed):

1. **aapt2 compile** — compile resources (strings.xml, etc.)
2. **aapt2 link** — link resources, generate R.java, create base APK
3. **javac** — compile Java sources (MainActivity + LauncherActivity)
4. **d8** — convert .class files to classes.dex
5. **jar** — add classes.dex to APK
6. **zipalign** — align APK for performance
7. **apksigner** — sign with debug keystore

Keystore: `debug.keystore` (auto-generated on first build, persists across rebuilds)

---

## Remote Management

### Option A: ADB over Wi-Fi (local network)

Enable on tablet: Settings → Developer Options → Wireless Debugging → ON

```bash
adb connect <tablet-ip>:<port>
```

Then use the GUI normally over Wi-Fi — no USB needed.

### Option B: Android Management API (cloud)

Click **🌐 Setup Remote Management** in the GUI for a guided wizard. Requires a Google account (can use work email via [accounts.google.com/signup](https://accounts.google.com/signup) → "Use existing email").

### Option C: VPN + ADB (off-site)

Set up Tailscale/WireGuard on both the tablet and PC. ADB connects over the VPN tunnel for remote management from anywhere.

---

## Troubleshooting

### App crashes frequently

1. Verify internet connection on the tablet
2. Check if the website is accessible from the tablet's browser
3. Rebuild APK (`bash build-apk.sh`) and reinstall — the latest version includes crash resilience fixes
4. Clear WebView cache: Settings → Apps → CU Denver Equity → Storage → Clear Cache

### "Device not connected" in GUI

1. Verify USB cable is data-capable (not charge-only)
2. Re-enable USB Debugging in Developer Options
3. Run `adb kill-server && adb devices` to reset ADB

### Cannot set device owner

1. Remove ALL Google accounts from the tablet
2. Verify no other device admin is active (Settings → Security → Device Admin Apps)
3. If a managed profile exists, factory reset the tablet

### Back gesture doesn't work

The app handles both `onKeyDown` (hardware back) and `onBackPressed` (gesture back). If neither works, the navigation bar may be hidden in fullscreen — swipe from the bottom edge to reveal it temporarily.

### Launcher shows app drawer

Run **🔒 Setup Kiosk** again — this re-hides all non-essential apps. The allowlist cleanup (removing Test DPC from allowlist) must be done manually in Test DPC.

---

## Project Files

```
Android Kiosk-Lock Task Mode/
│
├── dist/                                    # Distribution
│   └── CU-Denver-Equity-Kiosk-Manager.exe   # Self-contained Windows GUI (27 MB)
│
├── app/src/main/                            # Android app source
│   ├── AndroidManifest.xml                  # Both activities, HOME intent filter
│   ├── res/values/strings.xml               # App name
│   └── java/com/example/
│       ├── webkiosk/MainActivity.java        # Kiosk WebView (251 lines)
│       └── kiosklauncher/LauncherActivity.java # Custom launcher (112 lines)
│
├── kiosk-manager.py                         # Desktop GUI source (Python/tkinter, 725 lines)
│
├── build-apk.sh                             # Windows APK build script
├── build-apk-mac.sh                         # macOS APK build script
│
├── setup-tools.bat                          # Windows: download ADB, JDK, SDK
├── setup-tools-mac.sh                       # macOS: download ADB, JDK, SDK
├── setup-kiosk.bat                          # Windows: build + deploy (CLI)
├── exit-kiosk.bat                           # Windows: exit kiosk (CLI)
├── package-mac.sh                           # macOS: build .dmg
│
├── .github/workflows/build-dmg.yml          # GitHub Actions: auto-build macOS .dmg
│
├── debug.keystore                           # APK signing key (auto-generated)
├── platform-tools/                          # ADB binary (bundled in .exe)
├── android-sdk/                             # Build tools (setup-tools.bat)
└── jdk/                                     # Java 17 (setup-tools.bat)
```

---

## Revision History

### v1.0 — Initial Implementation
- Basic WebView kiosk app locked to `ucdenver.edu`
- Test DPC device owner setup via ADB
- Custom APK build pipeline using Android SDK command-line tools

### v1.1 — Domain Expansion
- Added `cm.maxient.com` to allowed domains
- Auto-lock task entry via `startLockTask()` in `onResume()`

### v1.2 — Desktop GUI
- Python/tkinter GUI compiled to standalone .exe via PyInstaller
- Setup Kiosk, Exit Kiosk, Factory Reset buttons
- Status checking and log panel
- ADB bundled inside .exe for portability

### v1.3 — GUI Enhancements
- Fullscreen mode (F11 / Esc)
- Dark mode toggle with persistent styling
- Accessibility mode (40% larger text, high contrast, focus indicators)

### v1.4 — Navigation Controls
- Added back/forward/refresh/home toolbar to kiosk app
- Changed to always-visible toolbar (removed auto-hide)
- Ultimately replaced with gesture-based back navigation

### v1.5 — Security Hardening
- Custom launcher replacing ZUI launcher (no app drawer)
- Live clock widget on launcher
- Hiding all non-essential apps via `pm disable-user`
- Removing Test DPC from lock task allowlist (only kiosk app allowlisted)
- Restore All Apps button in GUI

### v1.6 — Portability & Remote
- APK bundled inside .exe (true single-file distribution)
- Platform detection for macOS compatibility
- macOS build scripts (`setup-tools-mac.sh`, `build-apk-mac.sh`, `package-mac.sh`)
- GitHub Actions workflow for macOS .dmg auto-build
- Remote Management Setup wizard (Android Management API enrollment + QR code)
- ADB over Wi-Fi support

### v1.8 — WiFi Recovery
- Added `WifiSettingsActivity` — lets users add WiFi credentials (SSID + password) if the tablet disconn
ects from the network
- Uses the Android 10+ `WifiNetworkSuggestion` API with runtime permission handling (`NEARBY_WIFI_DEVICES` on Android 13+, `ACCESS_FINE_LOCATION` on Android 10-12)
- Entry points: a "WiFi Settings" button on the custom launcher and a small overlay button in the kiosk app
- Updated `AndroidManifest.xml` and both build scripts to include the new activity

### v1.7 — Stability Fixes
- WebView crash resilience: custom error page with Retry
- Lock task lifecycle race prevention (`mInLockTask` guard)
- try/catch on `startLockTask()` and `ActivityOptions` calls
- WebView pause on background, proper destroy on exit
- Memory leak prevention in WebView cleanup
- Mixed content compatibility mode

---

## Device Management Quick Reference

### Commands

```bash
# Set device owner
adb shell dpm set-device-owner com.afwsamples.testdpc/.DeviceAdminReceiver

# Force-exit kiosk mode
adb shell am force-stop com.example.webkiosk

# Hide/restore apps
adb shell pm disable-user --user 0 <package>
adb shell pm enable <package>

# List hidden apps
adb shell pm list packages -d

# Set custom launcher as home
adb shell cmd package set-home-activity com.example.webkiosk/.kiosklauncher.LauncherActivity

# Check lock task allowlist
adb shell dumpsys device_policy | grep LockTaskPolicy

# Factory reset
adb shell recovery --wipe_data
```
