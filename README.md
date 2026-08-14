# CU Denver Equity — Android Kiosk Lock Task Mode

A complete toolkit for locking a Lenovo Tab K11 Gen 2 (Android 15) tablet into kiosk mode, displaying the [CU Denver Office of Equity](https://www.ucdenver.edu/offices/equity) website. Includes a portable Windows GUI for non-technical users, a custom Android launcher, and macOS build support.

[![Download Windows .exe](https://img.shields.io/badge/Download-Windows%20.exe-2d5a8f?style=for-the-badge&logo=windows)](https://github.com/TriBrigadeMars/Android-Kiosk-Lock-Task-App/releases/latest/download/CU-Denver-Equity-Kiosk-Manager.exe)

**Direct download:** [CU-Denver-Equity-Kiosk-Manager.exe](https://github.com/TriBrigadeMars/Android-Kiosk-Lock-Task-App/releases/latest/download/CU-Denver-Equity-Kiosk-Manager.exe) — a single self-contained file (bundles ADB, the kiosk APK, and Test DPC). No install needed: double-click it, connect the tablet via USB, and click **Setup Kiosk**.

---

## Architecture

```
Desktop PC                          Lenovo Tablet (Android 15)
┌─────────────────────────┐         ┌──────────────────────────────┐
│  Kiosk-Manager.exe      │         │  Lock Task Mode (active)      │
│  (self-contained)       │  USB    │                               │
│  ┌───────────────────┐  │ ◄───── │  ┌─────────────────────────┐  │
│  │ Setup Kiosk       │  │  ADB   │  │ Custom Launcher (Home)  │  │
│  │ Exit Kiosk        │  │        │  │  ⏰ Clock + 1 app icon  │  │
│  │ Restore Apps      │  │        │  │  No app drawer           │  │
│  │ Remote Management │  │        │  └───────────┬─────────────┘  │
│  │ Factory Reset     │  │        │              ↕                │
│  │ Fullscreen/Dark   │  │        │  ┌───────────┴─────────────┐  │
│  │ Accessibility     │  │        │  │ Kiosk WebView App       │  │
│  └───────────────────┘  │        │  │ ucdenver.edu domain     │  │
└─────────────────────────┘        │  │ cm.maxient.com domain   │  │
                                   │  │ Fullscreen immersive    │  │
                                   │  └─────────────────────────┘  │
                                   │                               │
                                   │  Allowlisted: com.example     │
                                   │  .webkiosk only               │
                                   │  Device owner: Test DPC       │
                                   └──────────────────────────────┘
```

---

## Quick Start

### What you need
- Windows 10/11 PC
- USB cable (data-capable, not charge-only)
- Lenovo Tab K11 Gen 2

### On the tablet (one-time)
1. Enable Developer Options: **Settings → About Tablet → tap Build Number 7 times**
2. Enable USB Debugging: **Settings → System → Developer Options → USB Debugging → ON**
3. Remove all Google accounts: **Settings → Passwords & Accounts → remove each one**

### Deploy
1. Connect tablet via USB — accept the debugging prompt
2. Double-click `dist/CU-Denver-Equity-Kiosk-Manager.exe`
3. Click **Setup Kiosk**
4. On the tablet, open **Test DPC → Lock Task Mode** — verify only `com.example.webkiosk` is allowlisted
5. Tap the **CU Denver Equity** app to enter kiosk mode

---

## Desktop GUI Reference

The `.exe` is a single 36 MB file — no installer, no dependencies. It bundles ADB, the kiosk APK, and Test DPC APK inside.

| Button | What it does |
|--------|-------------|
| **🔒 Setup Kiosk** | 7-step automated setup (check device → install Test DPC → set device owner → install kiosk APK → set launcher → hide apps → verify) |
| **🔓 Exit Kiosk** | Force-stops the kiosk app — tablet returns to launcher |
| **📱 Restore All Apps** | Re-enables all hidden apps (Chrome, Settings, Play Store, etc.) |
| **🌐 Setup Remote Management** | Guided wizard for Android Management API enrollment |
| **⚠ Factory Reset Tablet** | Wipes tablet to factory state (double confirmation) |
| **🗖 Fullscreen (F11)** | Toggles fullscreen (Esc to exit) |
| **🌙 Dark Mode** | Toggles dark theme |
| **♿ Accessibility** | 40% larger text, high-contrast focus indicators |

---

## Android Kiosk App

### Kiosk Activity (`MainActivity.java`)
- Fullscreen immersive WebView locked to `ucdenver.edu` and `cm.maxient.com`
- Domain locking — external navigation blocked with toast message
- Error resilience — custom error page with Retry button on load failure
- Back gesture/hardware key — navigates WebView history; returns to launcher at start page
- Auto lock task entry on resume with lifecycle race prevention
- Memory management — WebView paused on background, properly destroyed on exit

### Custom Launcher (`LauncherActivity.java`)
- Navy blue home screen with live clock and single kiosk app icon
- No app drawer — users cannot access any other apps
- Clean Android lifecycle with clock handler cleanup

### WiFi Settings (`WifiSettingsActivity.java`)
- Lets users add WiFi credentials (SSID + password) if the tablet drops off the network
- Uses the Android 10+ `WifiNetworkSuggestion` API; the system prompts the user to approve the network
- Runtime permission handling (`NEARBY_WIFI_DEVICES` on Android 13+, `ACCESS_FINE_LOCATION` on Android 10-12)
- Reachable from a "WiFi Settings" button on the launcher and a small overlay button in the kiosk app

---

## Download the prebuilt .exe

Non-technical users don't need to build anything. A prepackaged Windows `.exe` is built automatically on every push to `main`/`master` by the `.github/workflows/build-exe.yml` workflow and can be downloaded two ways:

- **GitHub Releases** (recommended) — a rolling **Latest build** release is published automatically on every push to `main`/`master`, so `CU-Denver-Equity-Kiosk-Manager.exe` is always downloadable from the repo's **Releases** page — no PR or build required. Versioned releases are also created when a version tag (e.g. `v1.0.0`) is pushed.
- **GitHub Actions artifacts** — open the **Actions** tab, select the latest **Build Windows EXE** run, and download the `Windows-EXE` artifact.

The `.exe` is a single self-contained file (bundles ADB, the kiosk APK, and Test DPC) — no installer or dependencies. Just double-click it, connect the tablet via USB, and click **Setup Kiosk**.

---

## Building from source

### Windows
```bash
setup-tools.bat        # One-time: downloads ADB, JDK 17, Android SDK
bash build-apk.sh      # Compiles the kiosk APK
```

Then package the GUI (or just run the one-step script):
```bash
bash package-win.sh   # Builds the APK and packages the .exe
```

Or manually:
```bash
pip install pyinstaller qrcode pillow
python -m PyInstaller --onefile --windowed --name "CU-Denver-Equity-Kiosk-Manager" \
  --add-data "platform-tools;platform-tools" \
  --add-data "build/CU-Denver-Equity-Kiosk.apk;." \
  --add-data "TestDPC.apk;." \
  kiosk-manager.py
```

### macOS
```bash
chmod +x setup-tools-mac.sh build-apk-mac.sh package-mac.sh
./setup-tools-mac.sh    # One-time: downloads toolchain
./package-mac.sh        # Builds .dmg
```

Or push to GitHub — the `.github/workflows/build-dmg.yml` auto-builds the macOS `.dmg` on every push.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| App crashes | Verify internet on tablet; rebuild APK and reinstall |
| "Device not connected" | Use data-capable USB cable; re-enable USB Debugging; run `adb kill-server && adb devices` |
| Cannot set device owner | Remove ALL Google accounts from tablet; check no other device admin active |
| Back gesture fails | Swipe from bottom edge to reveal nav bar temporarily |
| Launcher shows app drawer | Re-run Setup Kiosk; manually remove extra packages from Test DPC allowlist |

---

## Project Files

```
├── app/src/main/            # Android app source
├── build/                   # Pre-built APK
├── kiosk-manager.py         # Desktop GUI (Python/tkinter)
├── build-apk.sh/mac.sh      # APK compilation scripts
├── setup-tools.bat/mac.sh   # Toolchain downloaders
├── setup-kiosk.bat           # CLI deploy script
├── exit-kiosk.bat            # CLI exit script
├── package-mac.sh            # macOS .dmg builder
├── package-win.sh            # Windows .exe builder
├── .github/workflows/        # CI/CD (builds macOS .dmg + Windows .exe)
├── TestDPC.apk              # Bundled device policy controller
├── debug.keystore           # APK signing key
├── README.md                # This file
└── LICENSE                  # MIT
```

## Quick Command Reference

```bash
# Connect to tablet
adb devices

# Set device owner
adb shell dpm set-device-owner com.afwsamples.testdpc/.DeviceAdminReceiver

# Exit kiosk mode
adb shell am force-stop com.example.webkiosk

# Hide/restore apps
adb shell pm disable-user --user 0 <package>
adb shell pm enable <package>

# List hidden apps
adb shell pm list packages -d

# Check lock task allowlist
adb shell dumpsys device_policy | grep LockTaskPolicy

# Factory reset
adb shell recovery --wipe_data
```
