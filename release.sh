#!/bin/bash

# --- USAGE CHECK ---
if [ -z "$1" ]; then
  echo "❌ Usage: ./release.sh <version>   e.g., ./release.sh 1.0.20"
  exit 1
fi

# --- CONFIGURATION ---
VERSION_NUMBER="$1"
RELEASE_VERSION="v$VERSION_NUMBER"
JAR_PATH="out/artifacts/CombiTry1_jar/CombiTry1.jar"
LATEST_TXT="latest.txt"
REPO="adikure20-cpu/CombiTry1"
RELEASE_TITLE="Version $RELEASE_VERSION"
RELEASE_BODY="Automated release $RELEASE_VERSION with updated UI and logic."
DOWNLOAD_URL="https://github.com/$REPO/releases/download/$RELEASE_VERSION/CombiTry1.jar"

# --- CHECK IF JAR EXISTS ---
echo "🔍 Checking for JAR at: $JAR_PATH"
if [ ! -f "$JAR_PATH" ]; then
  echo "❌ JAR file not found. Please build the JAR first."
  exit 1
fi

# --- UPDATE latest.txt ---
echo "📝 Updating $LATEST_TXT..."
echo "$VERSION_NUMBER" > $LATEST_TXT
echo "$DOWNLOAD_URL" >> $LATEST_TXT

# --- GIT COMMIT & PUSH ---
echo "📤 Committing and pushing changes to GitHub..."
git add .
git commit -m "Release $RELEASE_VERSION"
git push origin main

# --- CREATE GITHUB RELEASE ---
echo "🚀 Creating GitHub release $RELEASE_VERSION..."
gh release create "$RELEASE_VERSION" "$JAR_PATH" --title "$RELEASE_TITLE" --notes "$RELEASE_BODY"

echo "✅ Done! Version $RELEASE_VERSION released and uploaded."
