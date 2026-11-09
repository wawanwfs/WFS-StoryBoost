#!/usr/bin/env bash
# Repeat the SAME real DnCNN CPU benchmark in separate Android app processes.
# Record native/Java/graphics snapshots and sampled live total PSS. These
# measurements are emulator evidence, not device peak RAM certification.
set -euo pipefail
APP=com.wfs.storyboost.studio
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
TEST=com.wfs.storyboost.DncnnTilingQaTest#colorNoiseTiledDenoiseReportsPairedQualityAndSeamContinuity
mkdir -p qa-memory
prev_pid=""
for attempt in 1 2; do
  adb shell am force-stop "$APP" || true
  adb logcat -c >/dev/null || true
  log="qa-memory/dncnn-process-$attempt.log"
  samples="qa-memory/dncnn-process-$attempt-pss.txt"
  : > "$samples"
  echo "DnCNN isolated process $attempt"
  adb shell am instrument -w -r -e class "$TEST" "$RUNNER" > "$log" 2>&1 &
  instrument_pid=$!
  samples_count=0
  peak_pss=0
  # Different adb host subprocess; monitor active inference without asserting
  # a single instantaneous sample is the true peak RSS or process RAM.
  while kill -0 "$instrument_pid" 2>/dev/null; do
    meminfo="$(adb shell dumpsys meminfo "$APP" 2>/dev/null || true)"
    # Android 10 prints "TOTAL: 12345" under App Summary, while newer
    # releases print "TOTAL PSS: 12345". Both refer to process PSS in KB.
    pss="$(printf '%s\n' "$meminfo" | awk '
      /TOTAL PSS:/ { if ($3 ~ /^[0-9]+$/) {print $3; exit} }
      $1 == "TOTAL:" && $2 ~ /^[0-9]+$/ {print $2; exit}
    ')"
    if [[ "$pss" =~ ^[0-9]+$ && "$pss" -gt 0 ]]; then
      printf '%s sampledPssKB=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$pss" >> "$samples"
      ((samples_count+=1))
      if (( pss > peak_pss )); then peak_pss="$pss"; fi
    fi
    sleep 1
  done
  if ! wait "$instrument_pid"; then
    cat "$log"
    echo "DnCNN instrumentation process $attempt failed" >&2
    exit 1
  fi
  cat "$log"
  grep -q 'OK (1 test)' "$log" || { echo "Missing passed DnCNN run $attempt" >&2; exit 1; }
  # This prints to instrumentation output because the test logs via stdout.
  report="$(grep -E 'DN CNN TILE QA.*pid=' "$log" | tail -1 || true)"
  if [[ -z "$report" ]]; then
    # AndroidJUnitRunner may suppress System.out; retain logcat independently.
    adb logcat -d -s WfsDncnnTileQA:I > "qa-memory/dncnn-process-$attempt-logcat.txt" || true
    report="$(grep 'pid=' "qa-memory/dncnn-process-$attempt-logcat.txt" | tail -1 || true)"
  fi
  if [[ -z "$report" ]]; then
    echo "No instrumented DnCNN process/memory breakdown available" >&2
    exit 1
  fi
  current_pid="$(printf '%s\n' "$report" | sed -n 's/.* pid=\([0-9][0-9]*\).*/\1/p')"
  [[ -n "$current_pid" ]] || { echo "Cannot extract process PID" >&2; exit 1; }
  if [[ -n "$prev_pid" && "$prev_pid" == "$current_pid" ]]; then
    echo "DnCNN runs reused PID; separate process proof failed" >&2
    exit 1
  fi
  prev_pid="$current_pid"
  echo "attempt=$attempt android_pid=$current_pid samples=$samples_count sampled_peak_pss_kb=$peak_pss" |
    tee "qa-memory/dncnn-process-$attempt-summary.txt"
  echo "$report" >> "qa-memory/dncnn-process-$attempt-summary.txt"
  echo "$report"
  # The in-process 150ms Debug sampler is the required metric: ADB polling
  # can miss a short API29 inference entirely. Never report zero as peak PSS.
  if ! grep -Eq 'liveSamples=[1-9][0-9]* sampledPeakPssKB=[1-9][0-9]*' <<< "$report"; then
    echo "ERROR: in-process PSS profiler missing or invalid" >&2
    exit 1
  fi
  if (( samples_count == 0 )); then
    echo "NOTE: external ADB sampler missed this short inference; in-process PSS samples recorded" |
      tee -a "qa-memory/dncnn-process-$attempt-summary.txt"
  fi
done
echo "PASS: two DnCNN inference runs in separate process IDs, with memory category snapshots"
