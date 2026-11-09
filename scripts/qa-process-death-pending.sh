#!/usr/bin/env bash
# Force-stop while the real Editor is still RESUMED. The stage test explicitly
# does not finish the Activity or call onPause; a normal test completion is FAIL.
set -euo pipefail
APP=com.wfs.storyboost.studio
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
CLASS=com.wfs.storyboost.ProcessDeathLocalTest
mkdir -p qa-autosave
adb logcat -c >/dev/null
stage_log=qa-autosave/live-stage.txt
adb shell am instrument -w -r \
  -e class "$CLASS#stageForegroundEditAwaitAbruptForceStop" \
  -e wfs.phase stageLive "$RUNNER" > "$stage_log" 2>&1 &
stage_job=$!
ready=0
for _ in $(seq 1 80); do
  if adb logcat -d -s WfsAutosaveCutQA:I 2>/dev/null | grep -q 'READY pid='; then
    ready=1
    break
  fi
  if ! kill -0 "$stage_job" 2>/dev/null; then
    cat "$stage_log" >&2
    echo "Foreground autosave stage exited without an external kill" >&2
    exit 1
  fi
  sleep 0.5
done
if [[ "$ready" != 1 ]]; then
  cat "$stage_log" >&2
  echo "Foreground editor never confirmed durable draft while RESUMED" >&2
  adb shell am force-stop "$APP" || true
  wait "$stage_job" || true
  exit 1
fi
adb logcat -d -s WfsAutosaveCutQA:I > qa-autosave/live-stage-ready.txt || true
adb shell pidof "$APP" | tee qa-autosave/live-before-kill-pid.txt
test -s qa-autosave/live-before-kill-pid.txt
adb shell am force-stop "$APP"
# Android aborts the instrumentation: a successful completed stage is invalid.
wait "$stage_job" || true
if grep -q 'OK (1 test)' "$stage_log"; then
  echo "Stage completed normally: process-death test invalid" >&2
  exit 1
fi
if [[ -n "$(adb shell pidof "$APP" 2>/dev/null || true)" ]]; then
  echo "App process survived external force-stop" >&2
  exit 1
fi
echo "Checking completely new Android process and Recent with real export"
adb shell am instrument -w -r \
  -e class "$CLASS#verifyReopenAfterExternalForceStop" \
  -e wfs.phase verify "$RUNNER" > qa-autosave/live-verify.txt 2>&1
cat qa-autosave/live-verify.txt
grep -q 'OK (1 test)' qa-autosave/live-verify.txt
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' qa-autosave/live-verify.txt
echo "PASS: foreground completed edits survive abrupt force-stop without Activity.onPause"
