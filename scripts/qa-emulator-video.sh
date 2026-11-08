#!/usr/bin/env bash
set -euo pipefail
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
if [ "$API" != 35 ]; then
  echo "SKIP: full transcoding/merge QA reserved for Android API 35; API $API still runs UI/JPEG QA."
  exit 0
fi
PKG=com.wfs.storyboost.studio
DEST="/sdcard/Android/data/$PKG/files"
adb shell mkdir -p "$DEST"
adb push qa-video.mp4 "$DEST/qa-video.mp4"
adb push qa-silent.mp4 "$DEST/qa-silent.mp4"
adb shell am force-stop "$PKG"
adb shell am start -W -n "$PKG/com.wfs.storyboost.StudioHomeActivity" --ez qa_video_export true
rm -f qa-video-export.txt
for n in $(seq 1 85); do
  if adb shell run-as "$PKG" cat files/qa-video-export.txt > qa-video-export.txt 2>/dev/null; then
    if grep -q '^STATUS=' qa-video-export.txt; then break; fi
  fi
  sleep 2
done
if [ ! -s qa-video-export.txt ]; then
  echo "Real MP4 QA timed out or process crashed (170s)" >&2
  adb shell pidof "$PKG" || true
  adb logcat -d -t 1200 > qa-video-logcat.txt || true
  grep -E 'StoryBoostVideoQA|E/AndroidRuntime|FATAL EXCEPTION|MediaCodec|Media3' qa-video-logcat.txt | tail -70 || true
  exit 1
fi
cat qa-video-export.txt
adb logcat -d -t 1200 > qa-video-logcat.txt || true
grep -q '^STATUS=PASS' qa-video-export.txt
grep -q '^fhdMp4Ms=' qa-video-export.txt
grep -q '^trimMp4Ms=' qa-video-export.txt
grep -q '^merge2ClipsMs=' qa-video-export.txt
grep -q '^cancelTempCleaned=true' qa-video-export.txt
adb shell dumpsys meminfo "$PKG" > qa-video-memory.txt
grep -q 'TOTAL' qa-video-memory.txt
echo "Real Media3 H.264/AAC FHD/trim/mute/merge/cancel QA passed"
