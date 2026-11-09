#!/usr/bin/env bash
# Real Android process restart test using a synthetic app-private JPEG source.
# DocumentsUI is NOT exercised by this check; it has a separate acceptance gate.
set -euo pipefail
APP=com.wfs.storyboost.studio
TEST="$APP.test/androidx.test.runner.AndroidJUnitRunner"
CLASS=com.wfs.storyboost.ProcessDeathLocalTest
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk >/dev/null
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null
# Do not inherit long-lived TFLite allocations from the preceding connected suite.
adb shell am force-stop "$APP" || true

run() {
  local method="$1" phase="$2" log
  log="$(adb shell am instrument -w -r -e class "$CLASS#$method" \
    -e wfs.phase "$phase" "$TEST" 2>&1)" || {
    printf '%s\n' "$log"
    exit 1
  }
  printf '%s\n' "$log"
  echo "$log" | grep -q 'OK (1 test)' || {
    echo "Two-process instrumentation phase $phase was not successful" >&2
    exit 1
  }
  if echo "$log" | grep -E 'FAILURES!!!|INSTRUMENTATION_FAILED'; then exit 1; fi
}
run stageDraftBeforeForceStop stage
adb shell am force-stop "$APP"
run verifyReopenAfterExternalForceStop verify
echo "PASS real adb force-stop, new process, Recent state and source SHA256"
