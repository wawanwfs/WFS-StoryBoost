#!/usr/bin/env bash
set -euo pipefail
PKG=com.wfs.storyboost.studio
# Runs genuine Android Activity instrumentation on-device. Fixture is a
# real MediaStore JPEG; DocumentsUI selection is simulated as an ActivityResult
# and must not be represented as end-to-end picker automation.
adb shell am instrument -w -r \
  "$PKG.test/androidx.test.runner.AndroidJUnitRunner" |
  tee qa-ui-instrumentation.txt
grep -Eq '^OK \([1-9][0-9]* tests?\)' qa-ui-instrumentation.txt
if grep -E '^FAILURES|Process crashed|INSTRUMENTATION_FAILED' qa-ui-instrumentation.txt; then
  exit 1
fi
echo "On-device Activity UI instrumentation PASS"
