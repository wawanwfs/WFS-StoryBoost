# WFS StoryBoost — Progress report v3.0.0-beta3

This is a **feature roadmap coverage estimate** for the target v3.0, not a claim of tested production quality. Each checkbox counts as one module. Implemented means source code is present and the project builds; feature correctness still requires end-to-end device tests.

## Beta7 QA candidate — cancellation-safe exports across the complete app

- [x] Verified beta6 PR #7 merged as cd8ad45535078f80c79876bdde8adb4c11dca378 after exact final 4/4 green CI: https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42664085487 . Model SHA and DnCNN low-memory backend, independent SAF provider, Media3 acceptance preserved.
- [ ] Cancellable MediaSaver video, photo and byte-for-byte original MP4 output, including before insert, after first 128KiB chunk and before/after publish; no pending or published cancelled Gallery rows.
- [ ] Queue-scoped AtomicBoolean cancellation tokens now cover Quick Enhance, single Editor and Merge Studio, preventing stale user-visible success/errors and unsolicited writing of new gallery files after pressing Batalkan. No original JPEG/MP4 URI is modified or deleted.
- [ ] Real Android `MediaSaverCancellationQaTest`: photo/video first-stage, mid-copy, original-MP4-copy and hidden MediaStore IS_PENDING cleanup. Validate API29 and API35 full tests at exact latest branch HEAD before acceptance.
- [ ] Windows ARM64 PowerShell physical QA diagnostics-only by default; opt-in synthetic inference mode with `-RunInstrumented` and owner knowledge; docs at docs/PHYSICAL_ARM64_QA_WINDOWS.md, local logs excluded from Git.
- [ ] Real physical ARM64 Android10 low-RAM and Android15 acceptance, Google Photos-owned URI, consented portrait/low-light samples and stable owner signing NOT performed.
- [ ] beta7 PR #8 must remain draft until exact latest commit has all 4 build, API29, API35 and smoke jobs green.
- Distinct roadmap coverage still **23/32 (71.875%)**; QA tests/bug fixes do not add new modules.

## Project roadmap: 23 / 32 modules ≈ 72% (beta)

### A. Editor foundation — 8 / 8
- [x] Import local photo/video
- [x] Project dashboard and project creation
- [x] Recent project list
- [x] Interactive photo/video preview
- [x] 11 ratios + custom dimensions
- [x] Fit / Crop / blurred framing
- [x] Photo & video media paths
- [x] Multi-item Quick Enhance batch

### B. Editing capability — 6 / 8
- [x] 24 Color Looks
- [x] Manual brightness/contrast/saturation
- [x] Text overlay
- [x] Video trim
- [x] Video split export
- [x] Join multiple video clips
- [ ] Multi-track video / audio / image layers
- [ ] Animated keyframes and transitions

### C. AI photography — 5 / 8
- [x] Offline ML face detection
- [x] Offline ML scene labeling and recommended Look
- [x] Face-aware smart crop
- [x] Offline ML portrait mask / background bokeh (v2.9.0 beta)
- [x] Model-backed luminance AI denoising — DnCNN on-device (v2.9), bounded input 2.6 MP
- [ ] Model-backed AI deblurring
- [ ] True super-resolution (with measured quality)
- [ ] Generative background replacement / inpainting

### D. Export & production — 4 / 8
- [x] Social profiles for WA / Instagram / TikTok
- [x] Crop source-detail guard
- [x] Android APK build and signature checks in CI (ABI-specific ARM64 and universal debug artifacts)
- [x] Emulator install + UI smoke tests
- [ ] Physical-device photo/video export QA matrix completed
- [ ] Stable release signing and upgrade test across versions
- [ ] Public release / Play distribution readiness
- [ ] Benchmarks for memory, rendering time and actual output media quality

**Total: 8 + 6 + 5 + 4 = 23 of 32 = 71.875%, rounded to 72%, only if CI inference benchmark passes.**

## Beta4 QA candidate — not accepted until all four CI jobs pass

- [ ] Real foreground Editor edits persisted before onPause; external adb force-stop during blocked test, new PID reopens Recent, decoded 1080x1920 JPEG and source SHA unchanged on API29/35.
- [ ] Native DocumentsUI tests still green with Photos isolated and separately Photos enabled (coexistence only, NOT Google Photos URI).
- [ ] Real DnCNN TFLite CPU test repeated in two independent processes per API; inspect sampled PSS, Java/native/graphics and threads, and investigate prior API29 365,138 KB snapshot.
- [ ] Preserve full Media3 video QA and all 24 Color Looks, no source mutations.
- [ ] Independent revocable DocumentsProvider, actual persistent-grant revocation and provider deletion/recovery, physical Android10 ARM64 low-RAM, real portrait benchmarks and owner-signed update remain unverified.
- [ ] Measure low-end UI response during synchronous small draft commits; two emulator memory repeats are not enough to certify the memory risk.
- **Feature ledger remains 23/32 = 71.875%.**

## v3.0 beta3 — evidence vs remaining blockers

[All four CI jobs passed](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42499169024) on `5757f76c5d4bd473306c9afd6ebd8244fe5e3186`: API29/API35 local MediaStore state persisted over genuine external `adb am force-stop`, independent instrumentation processes and PID check, Recent resume, decoded 1080×1920 JPEG and original SHA-256. Native DocumentsUI was subsequently proven in [run 42506941662](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42506941662), but externally revoked third-party grants remain unverified.

New synthetic API35 media checks: separate H.264/AAC split files, 2-clip merge, silent and rotated MP4, active cancellation and cleanup. Real Activity UI checks: crop focus gesture, Undo/Redo, before-after, repeated import without inheriting a Recent draft. Quality on real portraits, low-RAM Android 10/ARM64 hardware, complete accessibility, 24-Look visual inspection, full source-provider tests and two owner-signed releases remain **unverified**. Earlier exploratory DocumentsUI CI failed and was not merged. The corrected native-picker tests subsequently passed on **API29 + API35** and were merged after all gates passed ([evidence](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42506941662)); the Google Photos provider is isolated during these tests.

**Native SAF picker acceptance, added after beta3 baseline:** The [integrated green run](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42506941662) verifies real `ACTION_OPEN_DOCUMENT` interactions via Android DocumentsUI, a persisted read grant, synthetic fixture SHA-256, force-stop with a new process PID, Recent restoration and decoded Full-HD JPEG output on API29/35. Google Photos is disabled during this isolated emulator test, then restored. No externally revoked provider, Google Photos return path, or physical-phone test has been certified. Testing evidence does not increase unique roadmap feature count.

**Distinct feature recount:** A 8/8 + B 6/8 + C 5/8 + D 4/8 = **23/32 = 71.875%**. No new roadmap module counted; testing confidence improved. WhatsApp/Instagram/TikTok may recompress.

### Additional beta3 on-device AI test

Synthetic real TFLite DnCNN benchmark on Android 15 x86_64 emulator: 480×480 RGB with paired clean/noisy fixture, **four 256px overlapping tiles**, CPU inference **10.783 s**, full pipeline **12.028 s**, PSNR **20.307→28.801 dB**, SSIM **0.293→0.745**, process PSS snapshots **48,144→53,274 KB**. The RGB chroma-difference guard and overlap-boundary checks passed. [Green AI CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42501208561). Not a portrait, low-light camera, physical ARM64, peak-RAM or overall IQA benchmark; no new AI weights or models activated.

The CI suite improves confidence in the existing DnCNN module; it **does not** add a 24th roadmap module. End-to-end physical-device and consented face-photo QA remain incomplete. A previously seen API29 MediaStore URI access failure after process death prompted an explicit IS_PENDING publication and failure-diagnostic regression; do not describe it as fixed until the final run passes.

**Emulator memory limitation:** The [beta3 combined API29/API35 CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42502015377) shows a **memory-usage outlier** on Android 10 x86_64 emulator: DnCNN 480×480 four-tile synthetic image CPU inference **14.249 s**, total **15.573 s**, process PSS snapshots **41,790→365,138 KB**. In the same CI run Android 15 showed inference **14.805 s**, total **15.946 s**, PSS **48,004→41,105 KB**. These are process snapshots, not device peak RAM or a statistically representative benchmark; the cause of API29's high PSS remains unconfirmed. Do not certify low-RAM phones.

## v3.0 beta2 — evidence summary (no extra modules)

[Verified candidate CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42491741331): four jobs passed, 4 Android 10/API 29 and 5 Android 15/API 35 instrumented tests passed. Checks include synthetic local SAF-result photo import (intent result stubbed, actual DocumentsUI picker untested), Activity close and fresh Home → Recent restoration, Color Look/Crop/Caption/AI flags, 1080×1920 decoded JPEG, SHA-256 immutability, corrupted drafts, deleted media and clearing history. Video QA checks synthetic FFmpeg H.264/AAC, silent, rotated and malformed MP4, with real Media3 trimmed **1080×1920 H.264/AAC encoding on API 35**, metadata and A/V duration checks. The [Pixel 6 screenshot smoke job in run 42493751785](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42493751785) separately confirmed PNG dimensions at 320×640 and 1080×2400; that overall run was unsuccessful due to intermittent dialog-focus automation, which has been addressed in a later QA commit. Human interaction correctness has not been certified.

**Unverified:** process death by OS force-stop, third-party persistable URI revocation, repeated fresh imports, split/merge/cancel full media acceptance, real portrait (hair/glasses/multi-person) bokeh, chroma noise/tile seam DnCNN, physical Android 10+ CPU/RAM, and matching certificates/upgrade across two owner-signed builds. No extra AI models or production signing were introduced.

**Unique module recount** A editor 8 + B editing 6 + C AI 5 + D export 4 = **23 / 32 = 71.875%**. Recent editing or codec test repetitions do not add modules. Debug signing is not release signing. WA/IG/TikTok recompress media.

## v3.0 beta changes (quality and continuity, not new modules)
- Implemented secure local draft storage/restore for media reopened through Recent Projects. Persists Color Looks, frame/crop, caption, quality, video trim and AI effect flags; source media remains untouched. Undo/Redo remains an in-memory history within each editor session.
- Autosaves on user edits and during Activity pause; Clear Recent clears editing drafts as well.
- Added source-resolution validation **before** AI DnCNN preview/export to avoid promising an output that the full-size renderer cannot process (>2.6 MP).
- Added real Android emulator tests of SharedPreferences draft roundtrip, malformed/corrupt draft rejection, source isolation, URI privacy, clearing, and >2.6 MP guard. Evidence requires the latest successful CI run, not just compiling.
- Actual emulator 320×640 screenshot identified a wrapped editor header and unnecessary disabled photo seekbar; v3 beta1 fixes both. Check the latest screenshot to confirm layout.
- Roadmap stays **23/32 = 71.875% (~72%)** because saved editing state strengthens an already-counted Editor foundation module. No real-photo ML benchmark, physical-device QA, stable signed release or multi-track editing was completed.
- Note: Original output quality on WhatsApp/Instagram/TikTok cannot be guaranteed after their server-side recompression.

## v2.9 neural inference evidence and limitations
- Candidate model: Qualcomm DnCNN-25 float TFLite (MIT / KAIR), grayscale Gaussian denoise trained for sigma=25. App projects luminance only and preserves chroma.
- CI verifies SHA256 `59bc2803…1d9fd`, packages actual weights and runs real TensorFlow Lite CPU inference. Initial successful unpinned v2.9 CI: PSNR 20.18→28.49 dB, global SSIM 0.7764→0.9591 on deterministic Gaussian noise; model inference 1.526 seconds for one 256×256 tile on emulator. Final pinned build is checked separately. This is not a clinical measure or real-photo blind IQA.
- Full-resolution images >2.6 MP intentionally return an error; true 4K denoise is **not** supported. Do not claim deblur, super-resolution, detail recovery or identical result on social platforms.
- Physical devices, real portrait QA, stable signing and multi-track are still pending.

## v2.8 delta and evidence boundaries
- End-to-end tests now render actual synthetic JPEGs in Android emulator (color, geometry, EXIF, raw-copy, negative/no-person AI) and collect memory/timing reports. These tests do not constitute completed physical-device QA or full benchmarks across hardware.
- Portrait mask raw-size inference, feather policy, aligned blur and asynchronous preview debouncing implemented. Real human image quality and hair, glasses, multi-person, night and dark-image behavior remain unverified.
- **The v2.8 baseline was 22/32 (69%)**; v2.9 adds only the independently tested DnCNN denoise module.

## Evidence and caution
- v2.6 baseline: GitHub Actions build success and 123 pure-Java logic tests.
- v2.9.0 new: AI Portrait Bokeh + guard regression tests; review **latest successful** GitHub Actions build (do not infer success from a commit alone).
- Emulator smoke checks launching screens, **not** portrait segmentation accuracy, real JPEG visual quality, MP4 playback through social platforms, or installation on real low-memory devices.
- APK debug signing is ephemeral across GitHub runners. Releases require a stable signing key kept privately in GitHub Secrets.
- WhatsApp/Instagram/TikTok compress uploaded videos and images independently. We cannot guarantee uploads will remain lossless.
