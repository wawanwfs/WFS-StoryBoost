#!/usr/bin/env bash
# Actual pixel-dimension check; never treat a failed resize as a successful UI test.
set -euo pipefail
APP=com.wfs.storyboost.studio
HOME_ACTIVITY="$APP/com.wfs.storyboost.StudioHomeActivity"
cleanup() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}
trap cleanup EXIT
capture() {
  local w="$1" h="$2" density="$3" file="storyboost-editor-${1}x${2}.png"
  adb shell wm size "${w}x${h}"
  adb shell wm density "$density"
  for attempt in 1 2 3 4; do
    echo "QA viewport ${w}x${h}; capture attempt ${attempt}/4"
    adb shell wm size
    adb shell wm density
    adb shell am force-stop "$APP"
    adb shell am start -W -n "$HOME_ACTIVITY" --ez qa_demo true
    sleep 6
    adb shell dumpsys activity activities | grep -q 'StudioEditorActivity'
    adb exec-out screencap -p > "$file"
    if python3 - "$file" "$w" "$h" <<'PY'
from pathlib import Path
import struct,sys
p=Path(sys.argv[1])
blob=p.read_bytes()
if blob[:8]!=b'\x89PNG\r\n\x1a\n':
    print('Invalid PNG screenshot',p)
    raise SystemExit(1)
actual=struct.unpack('>II',blob[16:24])
expected=int(sys.argv[2]),int(sys.argv[3])
print('Screenshot:',p,'actual=',actual,'expected=',expected,'bytes=',len(blob))
raise SystemExit(0 if actual==expected else 1)
PY
    then
      echo "PASS viewport screenshot: $file"
      return 0
    fi
    sleep 3
  done
  echo "ERROR: emulator did not render requested screenshot dimensions ${w}x${h}" >&2
  return 1
}
capture 320 640 160
capture 1080 2400 440
