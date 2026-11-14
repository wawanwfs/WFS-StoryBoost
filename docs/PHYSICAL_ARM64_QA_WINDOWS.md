# WFS StoryBoost — Physical ARM64 QA on Windows 10/11

This is an owner-operated test runbook. **No physical ARM64 phone has been tested in GitHub Actions**; existing Android29/35 results use x86_64 emulators.

## Prerequisites

Install Android SDK Platform-Tools (`adb` on PATH), PowerShell 5.1+, and connect an authorized USB-debugging Android10/API29+ ARM64 handset. For optional synthetic DnCNN instrumentation also install JDK17, Gradle 8.13, Android SDK platform36/build-tools35 and matching app/androidTest debug APKs. Finish and save any editing session before opting in to force-stop testing.

## Diagnostics-only (no install, no force-stop)

From the repository root:

```powershell
adb devices -l
powershell -ExecutionPolicy Bypass -File .\scripts\qa-physical-arm64.ps1
```

For multiple devices use `-Serial YOUR_ADB_SERIAL`. Local results are kept in `qa-physical-results/`, excluded from Git. They may contain device metadata; keep them private. No gallery media, photos, or Google account is accessed by diagnostics.

## Optional synthetic neural inference (deliberate opt-in)

First in Git Bash run `bash scripts/fetch-dncnn-model.sh` to obtain the pinned verified MIT-licensed model. From PowerShell:

```powershell
gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r .\app\build\outputs\apk\debug\app-arm64-v8a-debug.apk
adb install -r .\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
powershell -ExecutionPolicy Bypass -File .\scripts\qa-physical-arm64.ps1 -RunInstrumented -Repetitions 5
```

The app and test APKs **must be signed with the same debug certificate**. If `INSTALL_FAILED_UPDATE_INCOMPATIBLE` occurs, do NOT uninstall an existing app or lose its drafts: use the existing signing key or a spare test handset. The QA script never silently installs/uninstalls anything. With `-RunInstrumented` it intentionally force-stops only WFS StoryBoost between cold inference runs.

The synthetic input is 480×480 clean/noisy RGB made on-device. It checks SHA-pinned DnCNN on the production XNNPACK-off path, distinct PIDs, PSS samples, inference latency, PSNR and SSIM. It does NOT use real portraits or Photos-owned media, and never asks the device to use the high-memory legacy XNNPACK-on path. A real low-RAM device may safely refuse AI due to the memory-pressure preflight; record this as a **safe refusal**, not a passing inference.

Retain `measurements.csv`, `instrument-N.txt`, `memory-N.txt`, `initial-meminfo.txt`, `battery.txt` and `thermal.txt` privately. Sampled process PSS can miss true instantaneous peaks. Before physical certification, repeat on distinct Android10 low-RAM ARM64 and Android15 ARM64 devices and inspect OS memory/thermal. Actual Google Photos-owned URI QA requires the owner's account and explicit permission; consented real portraits and stable owner release-signing secrets are separate incomplete gates.

WhatsApp, Instagram and TikTok may independently recompress exports. No claim of lossless upload or complete CapCut parity is justified by this runbook.
