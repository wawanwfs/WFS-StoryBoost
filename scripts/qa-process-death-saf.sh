#!/usr/bin/env bash
# End-to-end Android DocumentsUI selection with independent instrumentation PIDs.
# Only synthetic MediaStore data is created. Default isolated run preserves
# beta3 evidence; WFS_PHOTOS_ENABLED=1 keeps Photos active to check picker
# compatibility (not selection or persistence of a Photos-owned URI).
set -euo pipefail
APP=com.wfs.storyboost.studio
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
CLASS=com.wfs.storyboost.ProcessDeathSafTest
PHOTOS_DISABLED=0
cleanup() {
  local status=$?
  trap - EXIT
  if (( status != 0 )); then
    echo "Picker failed: collecting synthetic test UI only" >&2
    adb exec-out screencap -p > qa-picker-failure.png 2>/dev/null || true
    adb shell uiautomator dump /sdcard/qa-picker-failure.xml >/dev/null 2>&1 || true
    adb pull /sdcard/qa-picker-failure.xml qa-picker-failure.xml >/dev/null 2>&1 || true
    echo "Picker failure: current Android activities" >&2
    adb shell dumpsys activity activities 2>/dev/null |
      grep -Ei 'topResumed|mResumed|documentsui|photopicker|storyboost|Recent' |
      tail -n 35 || true
    echo "Picker failure: focused window" >&2
    adb shell dumpsys window 2>/dev/null |
      grep -Ei 'mCurrentFocus|mFocusedApp' | tail -n 8 || true
    if [[ -s qa-picker-failure.xml ]];then
      echo "Picker failure: initial XML accessibility hierarchy" >&2
      head -c 5500 qa-picker-failure.xml >&2
      echo >&2
    fi
  fi
  if [[ "$PHOTOS_DISABLED" == "1" ]]; then
    adb shell pm enable --user 0 com.google.android.apps.photos >/dev/null 2>&1 || true
  fi
  exit "$status"
}
trap cleanup EXIT
if [[ "${WFS_PHOTOS_ENABLED:-0}" == "1" ]]; then
  echo "Google Photos remains enabled: validating DocumentsUI coexistence, NOT Photos URI grants"
elif adb shell pm list packages | grep -Fq 'com.google.android.apps.photos'; then
  disabled="$(adb shell pm disable-user --user 0 com.google.android.apps.photos 2>&1 || true)"
  if [[ "$disabled" != *disabled-user* ]]; then
    echo "Cannot isolate native DocumentsUI: $disabled" >&2
    exit 1
  fi
  PHOTOS_DISABLED=1
  echo "Google Photos isolated only for native DocumentsUI QA"
fi
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk >/dev/null
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null
phase() {
  local method="$1" scope="$2" result
  echo "Running picker phase $scope"
  if ! result="$(adb shell am instrument -w -r \
      -e class "$CLASS#$method" -e wfs.phase "$scope" "$RUNNER" 2>&1)"; then
    printf '%s\n' "$result"; return 1
  fi
  printf '%s\n' "$result"
  if ! grep -q 'OK (1 test)' <<< "$result"; then
    echo "No successful complete instrumentation result for $scope" >&2
    return 1
  fi
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' <<< "$result"; then
    return 1
  fi
}
phase stageRealDocumentsUiImportAndDraft stage
adb shell am force-stop "$APP"
phase verifyAfterExternalForceStopAndReopenRecent verify
echo "PASS actual DocumentsUI selection, persisted grant, force-stop, Recent and JPEG"
