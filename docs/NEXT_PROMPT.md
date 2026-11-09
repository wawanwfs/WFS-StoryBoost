# WFS StoryBoost — Next Execution Prompt v3.0 beta7

Repository https://github.com/wawanwfs/WFS-StoryBoost . Start at the newest verified main, not an outdated commit. First audit beta6 PR #7, all final exact-head GitHub Actions, QA artifacts, README, PROGRESS, MODEL_RESEARCH_V29, TEST_PLAN and all Android source. Protect the original media bytes and existing 24 Color Looks, SAF draft recovery and Media3 functionality. No force pushes and never merge a failed gate.

## Priority P0 — verify real production low-memory AI and media safety

1. Confirm the default consumer DnCNN backend really uses setUseXNNPACK(false) with identical pinned SHA256, 480x480 synthetic clean/noisy paired PSNR/SSIM/chroma/tile overlap outputs. On API29/35 verify two new PIDs per default/off/on setting: default/off transient PSS substantially lower than explicit on; report exact peak PSS distribution and inference time, no hidden resizing or model downgrade. If any latest beta6 gate fails, diagnose and fix it before anything is merged.
2. Measure native transient PSS, CPU, thermal, crashes and background memory pressure on *physical ARM64 Android10 low-RAM and Android15* phones. Distinguish system.available RAM from app process PSS. Check the conservative 512 MiB preflight for false refusals and whether reduced XNNPACK allocations support a smaller *physically validated* threshold; do not change it without repeatable hardware evidence.
3. Exercise real Media3 H264/AAC encoder cancel at immediate and nonzero-progress points with actual 12-second fixture, no callbacks after cancel, zero leaked MP4 cache/MediaStore outputs, unchanged original SHA; investigate failures across API29/35 without claiming unavailable 1080p hardware encoders are successful HD fallback. Add late-stage cancellation and unsupported-codec / malformed container coverage with actual track PTS drift.
4. Repeat independent-UID native Android DocumentsUI SAF grant/revoke/disable/restore/force-stop and Recent on both APIs. Verify intermittent Android35 cold-start picker retry never substitutes mock Intents or false positive provider. With authenticated owner test account, select *actual Google Photos-owned document* with Photos enabled, check permission revocation/reimport and original immutable JPEG, not just coexisting with Photos.

## Priority P1 — real production release and design

5. Obtain rights-cleared, consented paired low-light and portrait images to benchmark DnCNN luminance/chroma, Google ML Kit bokeh hair/glasses, multiple people and tile seams; record source/license/model SHA and paired-only PSNR/SSIM, physical CPU/RAM/latency. No unauthorized real faces or provider imagery.
6. Review actual 320x640/160dpi and 1080x2400/440dpi screenshot artifacts for crop touch geometry vs exported pixels, safe zones, readability, 24 looks, before/after slider, accessible screen readers and focus. Apply evidence-backed fixes and new pixel-level UI/instrumentation tests.
7. Only owner-provided STORYBOOST_KEYSTORE_BASE64, STORYBOOST_KEYSTORE_PASSWORD, STORYBOOST_KEY_ALIAS and STORYBOOST_KEY_PASSWORD may sign release APK. Verify same cert/package and increasing versionCode in two independent signed builds, true adb install -r and Recent restoration. No keystores in repository, no debug APK mislabeled as release.

## Completion contract

Provide exact main baseline and resulting commit, PR/draft/merge status, final-head 4/4 job CI URL, emulator-versus-physical evidence, raw per-backend PSS/PSNR/SSIM/infer times, actual MP4 sample and cancel metrics, artifact URLs, known blockers, and next unique execution prompt. Maintain distinct module ledger 23/32=71.875% unless a genuinely missing feature is implemented and validated; QA tests do not inflate coverage. Do not claim lossless WhatsApp/IG/TikTok, all providers supported or full CapCut parity. Never promise asynchronous work.
