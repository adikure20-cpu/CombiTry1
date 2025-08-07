#!/bin/bash
set -e  # Exit immediately if a command exits with a non-zero status

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

# --- CHECK GH CLI ---
if ! command -v gh &> /dev/null; then
  echo "❌ GitHub CLI (gh) not installed. Please install it: https://cli.github.com/"
  exit 1
fi

if ! gh auth status &> /dev/null; then
  echo "❌ GitHub CLI not authenticated. Run: gh auth login"
  exit 1
fi

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

# --- SHOW SUMMARY ---
echo
echo "📦 Preparing to release:"
echo "   Version:         $RELEASE_VERSION"
echo "   JAR Path:        $JAR_PATH"
echo "   GitHub Repo:     $REPO"
echo "   Release Title:   $RELEASE_TITLE"
echo "   Download URL:    $DOWNLOAD_URL"
echo

# --- USER CONFIRMATION ---
read -p "⚠️  Proceed with commit, push, and release? (y/N): " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
  echo "❌ Release aborted by user."
  exit 1
fi

# --- GIT COMMIT & PUSH ---
echo "📤 Committing and pushing changes to GitHub..."
git add .
git commit -m "Release $RELEASE_VERSION"
git push origin main

# --- CREATE GITHUB RELEASE ---
echo "🚀 Creating GitHub release $RELEASE_VERSION..."
gh release create "$RELEASE_VERSION" "$JAR_PATH" --title "$RELEASE_TITLE" --notes "$RELEASE_BODY"

echo "✅ Done! Version $RELEASE_VERSION released and uploaded."
