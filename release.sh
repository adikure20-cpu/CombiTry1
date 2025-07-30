#!/bin/bash

# Variables — EDIT THESE
JAR_PATH="out/artifacts/CombiTry1_jar/CombiTry1.jar"   # Adjust if jar path differs
RELEASE_VERSION="v1.0.19"                               # Your new version tag, e.g. v1.0.11
RELEASE_TITLE="Version $RELEASE_VERSION"
RELEASE_BODY="Automated release $RELEASE_VERSION with new features and fixes."

# Extract version number without 'v' for latest.txt (e.g., "1.0.16")
VERSION_NUMBER="${RELEASE_VERSION#v}"

# Construct download URL for GitHub release asset
DOWNLOAD_URL="https://github.com/adikure20-cpu/CombiTry1/releases/download/$RELEASE_VERSION/CombiTry1.jar"

echo "Checking if JAR exists at $JAR_PATH..."

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR file not found at $JAR_PATH"
    echo "Please build your jar manually before running this script."
    exit 1
fi

echo "Updating latest.txt with version $VERSION_NUMBER and download URL $DOWNLOAD_URL"

# Update latest.txt file
echo "$VERSION_NUMBER" > latest.txt
echo "$DOWNLOAD_URL" >> latest.txt

echo "Adding all changes to git and committing..."

git add -A
git commit -m "Release $RELEASE_VERSION"

echo "Pushing changes to origin main branch..."

git push origin main

echo "Creating GitHub release $RELEASE_VERSION and uploading jar..."

if gh release view "$RELEASE_VERSION" &>/dev/null; then
    echo "❌ A release with tag $RELEASE_VERSION already exists. Please bump the version."
    exit 1
fi

gh release create "$RELEASE_VERSION" "$JAR_PATH" -t "$RELEASE_TITLE" -n "$RELEASE_BODY"

echo "Release $RELEASE_VERSION created, latest.txt updated, and jar uploaded successfully."
