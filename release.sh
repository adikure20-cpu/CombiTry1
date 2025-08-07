#!/bin/bash
set -e  # Exit on error

# --- USAGE CHECK ---
if [ -z "$1" ]; then
  echo "❌ Usage: ./release.sh <new_version>   e.g., ./release.sh 1.0.29"
  exit 1
fi

# --- CONFIGURATION ---
VERSION_NUMBER="$1"
RELEASE_VERSION="v$VERSION_NUMBER"
JAR_PATH="out/artifacts/CombiTry1_jar/CombiTry1.jar"
DMG_NAME="CombiTry1.dmg"
BUILD_DIR="build_release"
DMG_PATH="$BUILD_DIR/output/$DMG_NAME"

LATEST_TXT="latest.txt"
REPO="adikure20-cpu/CombiTry1"
RELEASE_TITLE="Version $RELEASE_VERSION"
RELEASE_BODY="Automated release $RELEASE_VERSION with updated UI and logic."
DOWNLOAD_URL="https://github.com/$REPO/releases/download/$RELEASE_VERSION/CombiTry1.jar"

UPDATER_FILE="src/Updater.java"
VERSION_FILE="version.txt"

# --- TOOLS CHECK ---
if ! command -v gh &> /dev/null; then
  echo "❌ GitHub CLI (gh) not installed. https://cli.github.com/"
  exit 1
fi

if ! gh auth status &> /dev/null; then
  echo "❌ GitHub CLI not authenticated. Run: gh auth login"
  exit 1
fi

# --- UPDATE version.txt ---
echo "$VERSION_NUMBER" > "$VERSION_FILE"

# --- UPDATE CURRENT_VERSION in Updater.java ---
if grep -q 'CURRENT_VERSION' "$UPDATER_FILE"; then
  echo "🔧 Updating CURRENT_VERSION in $UPDATER_FILE..."
  sed -i.bak "s/private static final String CURRENT_VERSION = \".*\";/private static final String CURRENT_VERSION = \"$VERSION_NUMBER\";/" "$UPDATER_FILE"
  rm "$UPDATER_FILE.bak"
else
  echo "❌ Couldn't find CURRENT_VERSION in $UPDATER_FILE"
  exit 1
fi

# --- BUILD CHECK ---
echo "🔍 Checking for JAR at: $JAR_PATH"
if [ ! -f "$JAR_PATH" ]; then
  echo "❌ JAR not found. Please build the JAR first."
  exit 1
fi

# --- UPDATE latest.txt ---
echo "📝 Writing $LATEST_TXT..."
echo "$VERSION_NUMBER" > "$LATEST_TXT"
echo "$DOWNLOAD_URL" >> "$LATEST_TXT"

# --- BUILD DMG ---
echo "💿 Building DMG using jpackage..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/input"
mkdir -p "$BUILD_DIR/output"
cp "$JAR_PATH" "$BUILD_DIR/input/"

jpackage \
  --input "$BUILD_DIR/input" \
  --main-jar "CombiTry1.jar" \
  --main-class "com.adikuric.Main" \
  --name "CombiTry1" \
  --type dmg \
  --dest "$BUILD_DIR/output" \
  --java-options "-Xmx512m"

# --- SUMMARY ---
echo
echo "📦 Ready to release:"
echo "   Version:         $VERSION_NUMBER"
echo "   JAR Path:        $JAR_PATH"
echo "   DMG Path:        $DMG_PATH"
echo "   Release Title:   $RELEASE_TITLE"
echo "   Download URL:    $DOWNLOAD_URL"
echo

# --- USER CONFIRMATION ---
read -p "⚠️  Proceed with commit, push, and release? (y/N): " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
  echo "❌ Release cancelled."
  exit 1
fi

# --- COMMIT & PUSH CHANGES (excluding DMG) ---
echo "📤 Committing changes (excluding DMG)..."
git add -u
git add "$VERSION_FILE" "$LATEST_TXT" "$UPDATER_FILE"
git commit -m "Release $RELEASE_VERSION"
git push origin main

# --- CREATE RELEASE ON GITHUB ---
echo "🚀 Uploading to GitHub Releases..."
gh release create "$RELEASE_VERSION" \
  "$JAR_PATH" "$DMG_PATH" \
  --title "$RELEASE_TITLE" \
  --notes "$RELEASE_BODY"

echo "✅ Done! Version $VERSION_NUMBER released and uploaded."
