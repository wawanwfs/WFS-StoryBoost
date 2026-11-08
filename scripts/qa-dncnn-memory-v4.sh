#!/usr/bin/env bash
# Five fresh Android instrumentation processes. Synthetic data only.
# PSS is sampled within each process, not a kernel-level true peak.
set -euo pipefail
APP=com.wfs.storyboost.studio
TEST="$APP.test/androidx.test.runner.AndroidJUnitRunner"
CASE="com.wfs.storyboost.DncnnMemoryV4Test#freshProcessFourTileInferenceMemoryAndCpu"
adb logcat -c || true
for threads in 1 3; do
for trial in 1 2 3 4 5; do
  echo "WFS DnCNN beta4 memory threads=$threads trial=$trial"
  result="$(adb shell am instrument -w -r -e class "$CASE" \
     -e wfs.phase memory-single -e wfs.threads "$threads" "$TEST" 2>&1)" || {
    printf '%s\n' "$result"
    exit 1
  }
  printf '%s\n' "$result" | tail -15
  if ! grep -q 'OK (1 test)' <<< "$result"; then
    echo "Memory trial $trial did not complete successfully" >&2
    exit 1
  fi
  log="$(adb logcat -d -s WfsDnnMemV4:I '*:S' 2>/dev/null || true)"
  record="$(grep 'WFS_DNCNN_MEMORY_V4 ' <<< "$log" | tail -1 || true)"
  if [[ -z "$record" ]]; then
    echo "Missing real TFLite memory telemetry in trial $trial" >&2
    exit 1
  fi
  printf '%s\n' "$record" | tee -a qa-dncnn-memory-v4.txt
  adb logcat -c || true
done
done
test "$(grep -c 'WFS_DNCNN_MEMORY_V4' qa-dncnn-memory-v4.txt)" -eq 10
for threads in 1 3; do
  test "$(grep -c "threads=$threads " qa-dncnn-memory-v4.txt)" -eq 5
done
echo "PASS: 10 fresh-process DnCNN memory snapshots; 5 each at CPU threads=1,3"
