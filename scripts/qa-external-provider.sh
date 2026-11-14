#!/usr/bin/env bash
# Independent UID DocumentsProvider acceptance, API29/API35 (QA-only).
set -euo pipefail
APP=com.wfs.storyboost.studio
PROVIDER=com.wfs.storyboost.qaprovider
RECEIVER="$PROVIDER/.FixtureControlReceiver"
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
CLASS=com.wfs.storyboost.QaExternalProviderTest
mkdir -p qa-provider-results
phase_number=0
diagnose(){
  local status=$?
  trap - EXIT
  if ((status!=0));then
    adb exec-out screencap -p > qa-provider-results/provider-failure.png 2>/dev/null || true
    adb shell uiautomator dump /sdcard/provider-failure.xml >/dev/null 2>&1 || true
    adb pull /sdcard/provider-failure.xml qa-provider-results/provider-failure.xml >/dev/null 2>&1 || true
    adb logcat -d -t 1000 > qa-provider-results/provider-logcat.txt 2>/dev/null || true
  fi
  adb shell am broadcast -n "$RECEIVER" -a "$PROVIDER.VIDEO_REVOKE" >/dev/null 2>&1 || true
  adb shell pm enable --user 0 "$PROVIDER" >/dev/null 2>&1 || true
  exit "$status"
}
trap diagnose EXIT
gradle --no-daemon :qa-provider:assembleDebug
test -s qa-provider/build/outputs/apk/debug/qa-provider-debug.apk
adb install -r qa-provider/build/outputs/apk/debug/qa-provider-debug.apk
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm enable --user 0 "$PROVIDER" >/dev/null 2>&1 || true
# PackageManager user IDs from public shell output, not queryPackageVisibility
# inside the consumer app. Use exact package match to avoid .test suffix.
adb shell pm list packages -U "$APP" | tr -d '\r' > qa-provider-results/consumer-package-uid.txt
adb shell pm list packages -U "$PROVIDER" | tr -d '\r' > qa-provider-results/provider-package-uid.txt
extract_uid(){
  awk -v p="package:$1" '$1==p {
    for(i=2;i<=NF;i++)if($i ~ /^uid:/){sub(/^uid:/,"",$i);print $i;exit}
  }' "$2"
}
consumer_uid="$(extract_uid "$APP" qa-provider-results/consumer-package-uid.txt)"
provider_uid="$(extract_uid "$PROVIDER" qa-provider-results/provider-package-uid.txt)"
if [[ -z "$consumer_uid" || -z "$provider_uid" || "$consumer_uid" == "$provider_uid" ]];then
  echo "Provider isolation failed: consumer UID $consumer_uid, source UID $provider_uid" >&2
  exit 1
fi
echo "PROVED separate Android UIDs consumer=$consumer_uid provider=$provider_uid" |
  tee qa-provider-results/uid-isolation.txt
control(){
  local action="$1" answer
  answer="$(adb shell am broadcast --receiver-foreground \
    -n "$RECEIVER" -a "$PROVIDER.$action" 2>&1)"
  printf '%s\n' "$answer" | tee -a qa-provider-results/owner-controls.txt
  if [[ "$answer" != *"result=200"* ]];then
    echo "Provider owner control $action was not acknowledged" >&2; exit 1
  fi
}
phase(){
  local method="$1" scope="$2" output
  ((phase_number+=1))
  output="qa-provider-results/$phase_number-$scope.txt"
  adb shell am force-stop "$APP" || true
  adb shell am instrument -w -r -e class "$CLASS#$method" \
    -e wfs.provider.phase "$scope" "$RUNNER" > "$output" 2>&1 || {
      cat "$output"; echo "External provider phase $scope failed" >&2; exit 1;
    }
  cat "$output"
  grep -q 'OK (1 test)' "$output" ||
    { echo "Missing passed $scope instrumentation" >&2; exit 1; }
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "$output";then
    echo "Non-green native provider phase $scope" >&2; exit 1;
  fi
}
control RESTORE
phase stageNativeImportFromSeparateProvider stage
phase verifyNewProcessRecentAndExport verify
control REVOKE
phase verifyOwnerRevokedGrant revoked
phase reimportAfterOwnerRevoke reimport
control DELETE
phase verifyDisabledOrDeletedProvider unavailable
control RESTORE
phase verifyDisabledOrDeletedProvider unavailable
adb shell pm disable-user --user 0 "$PROVIDER"
phase verifyDisabledOrDeletedProvider unavailable
adb shell pm enable --user 0 "$PROVIDER"
control RESTORE
phase reimportAfterOwnerRevoke reimport
phase verifyVideoGrantRevokedDuringOriginalCopy video-copy
if [[ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" == 35 ]]; then
  phase verifyVideoGrantRevokedDuringRealEncoder video-encode
fi
echo "PASS independent UID DocumentsUI grants, owner revocation, deletion, disable and reimport" |
  tee qa-provider-results/provider-acceptance-summary.txt
