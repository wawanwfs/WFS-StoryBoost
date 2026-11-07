#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" "$ROOT/app/src/main/java/com/wfs/storyboost/AspectRatioProfile.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/FrameMath.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/VideoBitrate.java" \
  "$ROOT/tests/AspectRatioProfileTest.java"
java -cp "$TMP" com.wfs.storyboost.AspectRatioProfileTest
