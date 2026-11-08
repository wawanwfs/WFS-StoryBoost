#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -d "$TMP" "$ROOT/app/src/main/java/com/wfs/storyboost/AspectRatioProfile.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/OutputProfile.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/FrameMath.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/VideoBitrate.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/VideoFramePlan.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/CropFocus.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/ClipWindow.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/ExportOptions.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/ColorLooks.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/SocialExportProfile.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/QualityAdvisor.java" \
  "$ROOT/app/src/main/java/com/wfs/storyboost/VideoPassthrough.java" \
  "$ROOT/tests/AspectRatioProfileTest.java"
java -cp "$TMP" com.wfs.storyboost.AspectRatioProfileTest
