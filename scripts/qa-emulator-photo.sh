#!/usr/bin/env bash
# Called as ONE command by android-emulator-runner (it runs script lines individually).
set -euo pipefail
PKG=com.wfs.storyboost.studio
REPORT=qa-photo-export.txt
MEMORY=qa-photo-memory.txt
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.wfs.storyboost.StudioHomeActivity" --ez qa_photo_export true
rm -f "$REPORT"
for n in $(seq 1 90); do
  if adb shell run-as "$PKG" cat files/qa-photo-export.txt > "$REPORT" 2>/dev/null; then
    if grep -q '^STATUS=' "$REPORT"; then break; fi
  fi
  sleep 2
done
if [ ! -s "$REPORT" ]; then
  echo "Photo QA report missing after 180 seconds" >&2
  adb logcat -d -t 220 | grep -E 'StoryBoostQA|E/AndroidRuntime|FATAL EXCEPTION' || true
  exit 1
fi
cat "$REPORT"
if ! grep -q '^STATUS=PASS' "$REPORT"; then
  echo "Real media QA reported failure" >&2
  adb logcat -d -t 220 | grep -E 'StoryBoostQA|E/AndroidRuntime|FATAL EXCEPTION' || true
  exit 1
fi
grep -q '^checks=' "$REPORT"
grep -q '^portraitExportMs=' "$REPORT"
grep -q '^noPersonAiMs=' "$REPORT"
adb shell dumpsys meminfo "$PKG" > "$MEMORY"
grep -q 'TOTAL' "$MEMORY"
echo "Verified real JPEG export QA and native heap measurements."
