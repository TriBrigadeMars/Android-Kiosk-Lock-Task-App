#!/bin/bash
# Build script for CU Denver Equity Kiosk APK
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -W)"
SCRIPT_DIR="${SCRIPT_DIR//\\//}"

JDK="$SCRIPT_DIR/jdk/jdk-17.0.20+8"
SDK="$SCRIPT_DIR/android-sdk"
BUILD_TOOLS="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35"
ANDROID_JAR="$PLATFORM/android.jar"

OUT_DIR="$SCRIPT_DIR/build"
GEN_DIR="$OUT_DIR/gen"
OBJ_DIR="$OUT_DIR/obj"
KEYSTORE="$SCRIPT_DIR/debug.keystore"

APP_DIR="$SCRIPT_DIR/app/src/main"
MANIFEST="$APP_DIR/AndroidManifest.xml"
JAVA_SRC="$APP_DIR/java/com/example/webkiosk/MainActivity.java"
WIFI_SRC="$APP_DIR/java/com/example/webkiosk/WifiSettingsActivity.java"
LAUNCHER_SRC="$APP_DIR/java/com/example/kiosklauncher/LauncherActivity.java"
RES_DIR="$APP_DIR/res"

APP_NAME="CU-Denver-Equity-Kiosk"

JAVA_BIN="$JDK/bin"
BUILD_TOOLS_BIN="$BUILD_TOOLS"

echo "=== Cleaning ==="
rm -rf "$OUT_DIR"
mkdir -p "$GEN_DIR" "$OBJ_DIR"

echo "=== Compiling resources ==="
"$BUILD_TOOLS_BIN/aapt2" compile \
  --dir "$RES_DIR" \
  -o "$OBJ_DIR/resources.zip"

echo "=== Linking resources ==="
"$BUILD_TOOLS_BIN/aapt2" link \
  -o "$OUT_DIR/$APP_NAME-unaligned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$MANIFEST" \
  --java "$GEN_DIR" \
  --min-sdk-version 21 \
  --target-sdk-version 35 \
  -R "$OBJ_DIR/resources.zip" \
  --auto-add-overlay

echo "=== Compiling Java ==="
"$JAVA_BIN/javac" \
  -d "$OBJ_DIR" \
  -cp "$ANDROID_JAR" \
  "$GEN_DIR/com/example/webkiosk/R.java" \
  "$JAVA_SRC" \
  "$WIFI_SRC" \
  "$LAUNCHER_SRC"

echo "=== Converting to DEX ==="
"$JAVA_BIN/java" -cp "$BUILD_TOOLS_BIN/lib/d8.jar" com.android.tools.r8.D8 \
  --lib "$ANDROID_JAR" \
  --output "$OBJ_DIR" \
  "$OBJ_DIR/com/example/webkiosk/"*.class \
  "$OBJ_DIR/com/example/kiosklauncher/"*.class

echo "=== Adding DEX to APK ==="
cd "$OBJ_DIR"
"$JAVA_BIN/jar" uf "$OUT_DIR/$APP_NAME-unaligned.apk" classes.dex
cd "$SCRIPT_DIR"

echo "=== Aligning APK ==="
"$BUILD_TOOLS_BIN/zipalign" -f -p 4 \
  "$OUT_DIR/$APP_NAME-unaligned.apk" \
  "$OUT_DIR/$APP_NAME-aligned.apk"

echo "=== Signing APK ==="
# Generate debug keystore if it doesn't exist (persists across builds)
if [ ! -f "$KEYSTORE" ]; then
  echo "=== Generating debug keystore ==="
  "$JAVA_BIN/keytool" -genkey -v \
    -keystore "$KEYSTORE" \
    -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android \
    -keypass android \
    -dname "CN=Android Debug, O=Android, C=US"
fi

"$JAVA_BIN/java" -jar "$BUILD_TOOLS_BIN/lib/apksigner.jar" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out "$OUT_DIR/$APP_NAME.apk" \
  "$OUT_DIR/$APP_NAME-aligned.apk"

echo ""
echo "=== SUCCESS ==="
echo "APK ready: build/$APP_NAME.apk"
ls -lh "$OUT_DIR/$APP_NAME.apk"
