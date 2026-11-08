# WFS StoryBoost — Next Execution Prompt v3.0 beta5

Repository: https://github.com/wawanwfs/WFS-StoryBoost . Start from latest verified main. Audit QA PR, all CI logs and artifacts, README, PROGRESS, TEST_PLAN, MODEL_RESEARCH_V29 and Android source. Preserve source bytes, offline-first editor, existing passed QA and ABI APKs. Never force push and never merge a failed gate.

## Priority P0 — finish unfinished beta4 proof

1. Verify newly added abrupt on-screen editor force-stop QA: completed Color Look, caption and 1080p persisted before any onPause, external adb kill while activity still RESUMED, next PID Recent reopen, real JPEG decode and original SHA. Diagnose and fix API29 issues. Evaluate low-end latency of synchronous SharedPreferences commits before release.
2. Create a completely separate, installable QA-only DocumentsProvider APK with its own Android UID and synthetic rights-safe media. Prove ACTION_OPEN_DOCUMENT, takePersistableUriPermission, revocation by provider owner, disabled/deleted provider recovery, repeated imports, ungranted access and fresh-process Recent on API29/35. App-owned MediaStore fixtures cannot prove revocable third-party grants. Exercise the *actual* Google Photos-owned URI with Photos enabled; mere enabled Photos picker coexistence is insufficient.
3. Perform 5+ independent real DnCNN TFLite sigma25 luminance multi-tile inference runs on API29, compare API35 and prior 365,138 KB PSS outlier. Retain identical weights, clean/noisy synthetic quality and PSNR/SSIM/chroma/seams, collect per-process PID, CPU threads, Java/native/graphics memory and sampled live high-water PSS; compare controlled 1/2/3 TFLite threads. Diagnose before optimizing; do not lower neural quality or secretly downsample. Test real low-RAM ARM64 Android10 and Android15 phones when available.
4. Expand actual Media3 tests for audio/silent in both clip orders, trim/split/merge, cancellation at multiple progress points with zero leaked cache or MediaStore files, malformed/unsupported codecs, timestamp-accurate audio/video drift, true rotated pixel dimensions and API29 low-memory behavior. An HD fallback is never a successful 1080p encode.

## Priority P1 — UX, AI and production

5. Review actual 320x640/160dpi and 1080x2400/440dpi screenshots and touch-crop versus decoded export. Check all 24 Color Looks, captions/social safe zones, contrast, comparison, overflow, touch target and accessibility.
6. Benchmark rights-cleared and/or explicitly consented paired real-photo data for DnCNN and Google ML Kit portrait bokeh: low light, chroma, tile seams, hair, glasses and multiple people. Record model license, exact SHA256, CPU/RAM/inference and paired-only PSNR/SSIM. No unvalidated new neural model.
7. Use only owner-provided STORYBOOST_KEYSTORE_BASE64, STORYBOOST_KEYSTORE_PASSWORD, STORYBOOST_KEY_ALIAS and STORYBOOST_KEY_PASSWORD. Never commit secrets. Verify same cert/package across two independently signed builds, increasing versionCode, successful adb install -r, persisted Recent and original bytes. Until then only label artifacts as debug.

## Delivery

Report exact baseline and resulting commit SHA, PR and merge only if actually complete, precise all-job CI URL/outcomes, emulators vs physical devices, raw measured audio/video and AI/memory evidence, artifacts, remaining faults and a new unique next execution prompt. Keep the distinct feature ledger at beta3 23/32 = 71.875% until a genuinely missing new feature is implemented and proven. Never claim WhatsApp/Instagram lossless output, all providers supported or full CapCut parity.
