#!/bin/bash
# macOS: Build and package the Kiosk Manager into a .dmg
# Run this on a Mac after running setup-tools-mac.sh
set -e
cd "$(dirname "$0")"

APP_NAME="CU-Denver-Equity-Kiosk-Manager"
DMG_NAME="CU-Denver-Equity-Kiosk-Manager"

echo "============================================"
echo " Building $APP_NAME.dmg for macOS"
echo "============================================"

# Step 0: Check prerequisites
if [ ! -f "platform-tools/adb" ]; then
    echo "ERROR: platform-tools not found. Run setup-tools-mac.sh first."
    exit 1
fi

if ! command -v python3 &>/dev/null; then
    echo "ERROR: python3 not found."
    exit 1
fi

# Step 1: Build the APK
echo "[1/4] Building kiosk APK..."
bash build-apk-mac.sh
mkdir -p dist/build
cp build/CU-Denver-Equity-Kiosk.apk dist/build/

# Step 2: Install PyInstaller if needed
if ! python3 -c "import PyInstaller" 2>/dev/null; then
    echo "[2/4] Installing PyInstaller..."
    pip3 install pyinstaller
else
    echo "[2/4] PyInstaller already installed."
fi

# Step 3: Build macOS .app bundle
echo "[3/4] Building macOS application bundle..."
rm -rf dist/"$APP_NAME" dist/"$APP_NAME".app build/"$APP_NAME" "$APP_NAME".spec 2>/dev/null || true

pyinstaller \
    --onefile \
    --windowed \
    --name "$APP_NAME" \
    --add-data "platform-tools:platform-tools" \
    --osx-bundle-identifier "edu.ucdenver.equity-kiosk-manager" \
    kiosk-manager.py

# Step 4: Create .dmg
echo "[4/4] Creating .dmg disk image..."
rm -f "$DMG_NAME.dmg"

# Create a temporary folder for dmg contents
DMG_DIR="/tmp/kiosk-dmg"
rm -rf "$DMG_DIR"
mkdir -p "$DMG_DIR"

# Copy the .app bundle
cp -R "dist/$APP_NAME.app" "$DMG_DIR/"

# Copy the APK
mkdir -p "$DMG_DIR/Kiosk-APK"
cp dist/build/CU-Denver-Equity-Kiosk.apk "$DMG_DIR/Kiosk-APK/"

# Create a README for the dmg
cat > "$DMG_DIR/README.txt" << 'EOF'
CU Denver Equity — Kiosk Manager
==================================

Double-click the Kiosk Manager app to launch.

The Kiosk-APK folder contains the latest APK for manual
installation if needed.

Requirements:
- USB cable to connect the Lenovo tablet
- USB Debugging enabled on the tablet
- Test DPC installed on the tablet (from Google Play)

For support, refer to the Android Kiosk Lock Task Mode project folder.
EOF

# Build the dmg
hdiutil create \
    -volname "CU Denver Equity Kiosk Manager" \
    -srcfolder "$DMG_DIR" \
    -ov -format UDZO \
    "$DMG_NAME.dmg"

# Cleanup
rm -rf build/"$APP_NAME" "$APP_NAME".spec "$DMG_DIR"

echo ""
echo "============================================"
echo " SUCCESS"
echo "============================================"
echo "DMG created: $DMG_NAME.dmg"
ls -lh "$DMG_NAME.dmg"
echo ""
echo "Distribute this .dmg to end users."
echo "They just need to:"
echo "  1. Open the .dmg"
echo "  2. Drag the app to Applications"
echo "  3. Connect the tablet via USB"
echo "  4. Launch the app"
