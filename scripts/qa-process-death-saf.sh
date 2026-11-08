#!/usr/bin/env bash
# End-to-end Android DocumentsUI selection with independent instrumentation PIDs.
# Only synthetic MediaStore data is created. Google Photos is isolated on QA
# emulators and restored on exit; this does not certify that provider's picker.
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
  fi
  if [[ "$PHOTOS_DISABLED" == "1" ]]; then
    adb shell pm enable --user 0 com.google.android.apps.photos >/dev/null 2>&1 || true
  fi
  exit "$status"
}
trap cleanup EXIT
if adb shell pm list packages | grep -Fq 'com.google.android.apps.photos'; then
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
