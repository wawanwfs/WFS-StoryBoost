#!/usr/bin/env bash
# QA ONLY: compare default TFLite 2.17 CPU kernels with explicit XNNPACK
# on/off. Same model, alpha/chroma, image noise seed, tile overlap and quality.
# Cold process, no production backend change, no claim of physical readiness.
set -euo pipefail
APP=com.wfs.storyboost.studio
RUNNER="$APP.test/androidx.test.runner.AndroidJUnitRunner"
TEST=com.wfs.storyboost.DncnnTilingQaTest#colorNoiseTiledDenoiseReportsPairedQualityAndSeamContinuity
API="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
mkdir -p qa-backend-matrix
MODEL=app/src/main/assets/models/dncnn_25.tflite
test -s "$MODEL"
sha256sum "$MODEL" | tee qa-backend-matrix/model-sha256.txt
printf '%s\n' 'api,xnnpack,repeat,pid,threads,pssBeforeKB,pssAfterKB,sampledPeakPssKB,inferMs,pipelineMs,pairedPsnr,pairedSsim' \
  > qa-backend-matrix/results.csv
prev_pid=''
for backend in default off on;do
  for repeat in 1 2;do
    adb shell am force-stop "$APP" || true
    adb logcat -c >/dev/null || true
    log="qa-backend-matrix/api$API-$backend-$repeat.txt"
    if ! adb shell am instrument -w -r -e class "$TEST" \
         -e wfs.ai.threads 2 -e wfs.ai.xnnpack "$backend" "$RUNNER" > "$log" 2>&1;then
      cat "$log";exit 1
    fi
    grep -q 'OK (1 test)' "$log" || { cat "$log";exit 1; }
    adb logcat -d -s WfsDncnnTileQA:I > "qa-backend-matrix/api$API-$backend-$repeat-logcat.txt" || true
    report="$(grep "qaXnnpack=$backend " "qa-backend-matrix/api$API-$backend-$repeat-logcat.txt" | tail -1 || true)"
    [[ -n "$report" ]] || { echo "Backend inference log missing: $backend" >&2;exit 1; }
    readval(){ sed -n "s/.* $1=\([0-9][0-9.]*\).*/\1/p" <<< "$report"; }
    pid="$(readval pid)"
    pre="$(readval pssBeforeKB)"
    post="$(readval pssAfterKB)"
    peak="$(readval sampledPeakPssKB)"
    infer="$(readval inferMs)"
    pipeline="$(readval pipelineMs)"
    psnr="$(readval pairedPsnr)"
    ssim="$(readval pairedSsim)"
    for field in "$pid" "$pre" "$post" "$peak" "$infer" "$pipeline" "$psnr" "$ssim";do
      [[ "$field" =~ ^[0-9]+([.][0-9]+)?$ ]] || { echo "Invalid $backend metric $field" >&2;exit 1; }
    done
    [[ "$prev_pid" != "$pid" ]] || { echo "Process was not restarted" >&2;exit 1; }
    prev_pid="$pid"
    echo "$API,$backend,$repeat,$pid,2,$pre,$post,$peak,$infer,$pipeline,$psnr,$ssim" |
      tee -a qa-backend-matrix/results.csv
  done
done
python3 - <<'PY'
import csv, math, statistics
with open("qa-backend-matrix/results.csv",newline="") as f:
    rows=list(csv.DictReader(f))
assert len(rows)==6, len(rows)
r={}
for mode in ("default","off","on"):
    samples=[x for x in rows if x["xnnpack"]==mode]
    assert len(samples)==2,mode
    pss=[int(x["sampledPeakPssKB"]) for x in samples]
    psnr=[float(x["pairedPsnr"]) for x in samples]
    ssim=[float(x["pairedSsim"]) for x in samples]
    assert all(v>0 for v in pss)
    assert all(math.isfinite(v) for v in psnr+ssim)
    r[mode]=(statistics.mean(pss),statistics.mean(psnr),statistics.mean(ssim))
for mode in ("off","on"):
    assert abs(r[mode][1]-r["default"][1])<0.15,(mode,r)
    assert abs(r[mode][2]-r["default"][2])<0.015,(mode,r)
with open("qa-backend-matrix/comparison.txt","w") as f:
    f.write("Measured sample PSS, not true physical peak RSS; never infer native ARM64 from x86_64\n")
    for mode in ("default","off","on"):
        f.write("%s avgPeakPssKB=%.0f pairedPsnr=%.6f pairedSsim=%.6f\n" %
                (mode,*r[mode]))
    f.write("This is an experiment; no implicit consumer backend change.\n")
print(open("qa-backend-matrix/comparison.txt").read())
PY
echo "PASS real paired DnCNN default/off/on cold-process benchmark, model invariant" |
  tee qa-backend-matrix/summary.txt
