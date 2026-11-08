#!/usr/bin/env bash
# Two independent Android instrumentation processes with actual DocumentsUI.
# The file fixture is generated on the emulator; no user media leaves the device.
set -euo pipefail
APP=com.wfs.storyboost.studio
TEST_PACKAGE="$APP.test"
RUNNER="$TEST_PACKAGE/androidx.test.runner.AndroidJUnitRunner"
CLASS=com.wfs.storyboost.ProcessDeathSafTest

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
