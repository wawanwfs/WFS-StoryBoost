#!/usr/bin/env bash
# Two independent Android instrumentation processes with actual DocumentsUI.
# The file fixture is generated on the emulator; no user media leaves the device.
set -euo pipefail
# Keep diagnostic evidence strictly limited to the synthetic test fixture UI.
# Never log user-provided media or private gallery content.
on_exit() {
  local code=$?
  if (( code != 0 )); then
    echo "Capturing failed real DocumentsUI picker screenshot / hierarchy"
    adb exec-out screencap -p > qa-picker-failure.png 2>/dev/null || true
    adb shell uiautomator dump /sdcard/qa-picker-failure.xml >/dev/null 2>&1 || true
    adb pull /sdcard/qa-picker-failure.xml qa-picker-failure.xml >/dev/null 2>&1 || true
  fi
  # The emulator is disposable; still restore other picker providers after test.
  if [[ "${PHOTOS_DISABLED:-0}" == "1" ]]; then
    adb shell pm enable --user 0 com.google.android.apps.photos >/dev/null 2>&1 || true
  fi
}
trap on_exit EXIT
APP=com.wfs.storyboost.studio
TEST_PACKAGE="$APP.test"
RUNNER="$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner"
CLASS=com.wfs.storyboost.ProcessDeathSafTest
# Android 15 Google Photos may intercept an image tile and display its viewer,
# which is NOT a DocumentsUI document result. For this specific contract test,
# use the platform DocumentsUI provider rather than a Photos viewer.
# This deliberately does NOT certify the Google Photos integration path.
PHOTOS_DISABLED=0
if adb shell pm list packages | grep -Fq 'com.google.android.apps.photos'; then
  disable_output="$(adb shell pm disable-user --user 0 com.google.android.apps.photos 2>&1 || true)"
  if [[ "$disable_output" == *disabled-user* ]]; then
    PHOTOS_DISABLED=1
    echo "Isolated native DocumentsUI: Google Photos disabled only for emulator QA"
  else
    echo "Could not isolate DocumentsUI from Google Photos: $disable_output" >&2
    exit 1
  fi
fi
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk >/dev/null
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null

phase() {
  local method="$1" phase="$2" log
  echo "Running real picker / process-death QA phase: $phase"
  log="$(adb shell am instrument -w -r -e class "$CLASS#$method" \
      -e wfs.phase "$phase" "$RUNNER" 2>&1)" || {
    printf '%s\n' "$log"
    echo "Instrumentation command failed: $phase" >&2
    return 1
  }
  printf '%s\n' "$log"
  echo "$log" | grep -q 'OK (1 test)' || {
    echo "Instrumentation did not report exactly one successful test: $phase" >&2
    return 1
  }
  if echo "$log" | grep -q -E 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed'; then
    echo "Instrumentation reported failure: $phase" >&2
    return 1
  fi
}
phase stageRealDocumentsUiImportAndDraft stage
# This kill is performed by adb OUTSIDE the instrumentation process. The
# second test checks a new Android PID and persisted SAF document grant.
adb shell am force-stop "$APP"
phase verifyAfterExternalForceStopAndReopenRecent verify
echo "PASS: actual DocumentsUI picker, force-stop, new process, Recent and JPEG"
; then
  if adb shell pm disable-user --user 0 com.google.android.apps.photos \
      | grep -q 'disabled-user'; then
    PHOTOS_DISABLED=1
    echo "Isolated native DocumentsUI: Google Photos disabled only for emulator QA"
  else
    echo "Unable to isolate DocumentsUI from Google Photos" >&2
    exit 1
  fi
fi

adb install -r app/build/outputs/apk/debug/app-universal-debug.apk >/dev/null
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null

phase() {
  local method="$1" phase="$2" log
  echo "Running real picker / process-death QA phase: $phase"
  log="$(adb shell am instrument -w -r -e class "$CLASS#$method" \
      -e wfs.phase "$phase" "$RUNNER" 2>&1)" || {
    printf '%s\n' "$log"
    echo "Instrumentation command failed: $phase" >&2
    return 1
  }
  printf '%s\n' "$log"
  echo "$log" | grep -q 'OK (1 test)' || {
    echo "Instrumentation did not report exactly one successful test: $phase" >&2
    return 1
  }
  if echo "$log" | grep -q -E 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed'; then
    echo "Instrumentation reported failure: $phase" >&2
    return 1
  fi
}
phase stageRealDocumentsUiImportAndDraft stage
# This kill is performed by adb OUTSIDE the instrumentation process. The
# second test checks a new Android PID and persisted SAF document grant.
adb shell am force-stop "$APP"
phase verifyAfterExternalForceStopAndReopenRecent verify
echo "PASS: actual DocumentsUI picker, force-stop, new process, Recent and JPEG"
