#!/bin/bash

# Variables — EDIT THESE
JAR_PATH="out/artifacts/CombiTry1_jar/CombiTry1.jar"
RELEASE_VERSION="v1.0.6"        # Change to your new version (match your code & latest.txt)
RELEASE_TITLE="Version $RELEASE_VERSION"
RELEASE_BODY="Automated release $RELEASE_VERSION with new features and fixes."

# Step 1: Add all changes and commit with message
git add -A
git commit -m "Release $RELEASE_VERSION"

# Step 2: Push changes to remote main branch
git push origin main

# Step 3: Create new GitHub release (use -t for tag name and -n for release notes)
gh release create $RELEASE_VERSION "$JAR_PATH" -t "$RELEASE_TITLE" -n "$RELEASE_BODY"

echo "Release $RELEASE_VERSION created and JAR uploaded successfully."

