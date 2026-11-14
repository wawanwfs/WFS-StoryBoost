#!/usr/bin/env bash
# Dedicated emulator-only instrumentation. Restore display state before other QA.
set -euo pipefail
APP=com.wfs.storyboost.studio
runner="$APP.test/androidx.test.runner.AndroidJUnitRunner"
mkdir -p qa-editor-viewports
cleanup(){
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}
trap cleanup EXIT
for item in '320x640 160' '1080x2400 440'; do
  read -r size density <<< "$item"
  adb shell am force-stop "$APP"
  adb shell wm size "$size"
  adb shell wm density "$density"
  report="qa-editor-viewports/${size}-${density}.txt"
  for attempt in 1 2 3 4 5; do
    actual=$(adb shell wm size | tr -d '\r')
    actual_density=$(adb shell wm density | tr -d '\r')
    if [[ "${actual##*: }" == "$size" && "${actual_density##*: }" == "$density" ]]; then
      break
    fi
    sleep 1
  done
  printf '%s\n%s\n' "$actual" "$actual_density" | tee "$report"
  [[ "${actual##*: }" == "$size" && "${actual_density##*: }" == "$density" ]]
  adb logcat -c
  adb shell am instrument -w -r -e class com.wfs.storyboost.PhotoViewportParityQaTest \
    -e wfs.viewport "$size" -e wfs.density "$density" "$runner" | tee -a "$report"
  adb logcat -d -s WfsViewportQA:I | tee -a "$report"
  grep -Fq 'OK (1 test)' "$report"
  grep -Fq "viewport=$size dpi=$density" "$report"
done
