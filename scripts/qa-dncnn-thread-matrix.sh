#!/usr/bin/env bash
# Reproduce DnCNN transient native allocations with 1/2/3 CPU threads.
# Android 10: 5 FULL fresh-process runs per configuration; Android 15: 2.
# Same pinned weights, paired RGB fixture, quality and tile-seam assertions.
set -euo pipefail
APP=com.wfs.storyboost.studio
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
TEST=com.wfs.storyboost.DncnnTilingQaTest#colorNoiseTiledDenoiseReportsPairedQualityAndSeamContinuity
API="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ "$API" == 29 ]];then repeats=5;else repeats=2;fi
mkdir -p qa-thread-matrix
printf '%s\n' 'api,threads,repeat,pid,pssBeforeKB,pssAfterKB,sampledPeakPssKB,inferMs,pipelineMs,quality' > qa-thread-matrix/results.csv
prev_pid=''
for threads in 1 2 3;do
  for attempt in $(seq 1 "$repeats");do
    adb shell am force-stop "$APP" || true
    adb logcat -c >/dev/null || true
    log="qa-thread-matrix/api$API-t$threads-run$attempt.log"
    result="$(adb shell am instrument -w -r -e class "$TEST" \
      -e wfs.ai.threads "$threads" "$RUNNER" 2>&1)" || {
        printf '%s\n' "$result" | tee "$log"
        exit 1
      }
    printf '%s\n' "$result" > "$log"
    grep -q 'OK (1 test)' "$log" || {
      cat "$log";echo "FAILED repeated CPU thread benchmark" >&2;exit 1;
    }
    adb logcat -d -s WfsDncnnTileQA:I > "qa-thread-matrix/api$API-t$threads-run$attempt-logcat.txt" || true
    report="$(grep 'qaCpuThreads=' "qa-thread-matrix/api$API-t$threads-run$attempt-logcat.txt" | tail -1 || true)"
    [[ "$report" == *"qaCpuThreads=$threads "* ]] || {
      echo "Missing thread-specific genuine TFLite inference" >&2;exit 1;
    }
    getval(){ sed -n "s/.* $1=\([0-9][0-9]*\).*/\1/p" <<< "$report"; }
    pid="$(getval pid)"
    pre="$(getval pssBeforeKB)"
    post="$(getval pssAfterKB)"
    peak="$(getval sampledPeakPssKB)"
    infer="$(getval inferMs)"
    pipeline="$(getval pipelineMs)"
    for field in "$pid" "$pre" "$post" "$peak" "$infer" "$pipeline";do
      [[ "$field" =~ ^[0-9]+$ ]] || { echo "Invalid memory/performance metric" >&2;exit 1; }
    done
    [[ "$prev_pid" != "$pid" ]] || { echo "Not a fresh Android process" >&2;exit 1; }
    prev_pid="$pid"
    # QA assertions for output PSNR/SSIM and seams already ran on all processes.
    printf '%s\n' "$API,$threads,$attempt,$pid,$pre,$post,$peak,$infer,$pipeline,paired-PSNR-SSIM-seams-PASS" |
      tee -a qa-thread-matrix/results.csv
  done
done
echo "PASS real pinned TFLite CPU thread matrix with $repeats fresh processes per config" |
  tee qa-thread-matrix/summary.txt
