#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "Usage: bash scripts/push-to-github.sh https://github.com/USERNAME/Lunara.git"
  exit 1
fi

REPO_URL="$1"
case "$REPO_URL" in
  https://github.com/*/*.git|git@github.com:*/*.git) ;;
  *)
    echo "Pass a complete GitHub repository URL ending in .git"
    exit 1
    ;;
esac

REPO_NAME="$(basename "${REPO_URL%.git}")"
case "$(printf '%s' "$REPO_NAME" | tr '[:upper:]' '[:lower:]')" in
  lunara|lunara-*) ;;
  *)
    echo "Safety stop: this ZIP must be pushed to a dedicated repository named Lunara or Lunara-*."
    echo "Received repository: $REPO_NAME"
    echo "This prevents accidentally building or overwriting an unrelated project."
    exit 1
    ;;
esac

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_DIR"
bash scripts/verify-project.sh

# Always build the Git commit from an isolated staging directory. This prevents
# files from an older project/repository from leaking into the Lunara commit,
# even when the ZIP was extracted inside another Git working tree.
STAGE_DIR="$(mktemp -d "${TMPDIR:-$HOME}/lunara-upload.XXXXXX")"
cleanup() { rm -rf "$STAGE_DIR"; }
trap cleanup EXIT INT TERM

tar \
  --exclude='./.git' \
  --exclude='./.gradle' \
  --exclude='./build' \
  --exclude='./app/build' \
  --exclude='./local.properties' \
  -cf - . | (cd "$STAGE_DIR" && tar -xf -)

cd "$STAGE_DIR"
bash scripts/verify-project.sh

git init
git branch -M main
git config user.name "${GIT_AUTHOR_NAME:-Mohnish Raj}"
git config user.email "${GIT_AUTHOR_EMAIL:-mohnish-ydv@users.noreply.github.com}"
git add -A
# Android shared storage can drop executable bits. Record the intended modes
# directly in Git so the clean GitHub runner receives runnable launchers.
git update-index --chmod=+x gradlew scripts/*.sh
git commit -m "Lunara M7 shared spaces and profile navigation repair"
git remote add origin "$REPO_URL"
git push -u origin main --force

echo "Uploaded isolated Lunara source to $REPO_NAME."
echo "Open GitHub Actions and download Lunara-M7.1-Debug-APK after the workflow is green."
