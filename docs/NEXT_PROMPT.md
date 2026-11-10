# WFS StoryBoost — Next Execution Prompt v3.0 beta8

Repository: https://github.com/wawanwfs/WFS-StoryBoost . Start from newest verified main and audit beta7 PR #8, exact CI/artifacts, README, docs/PROGRESS.md, docs/MODEL_RESEARCH_V29.md, tests/TEST_PLAN.md, media/safety source and UI screenshots. Preserve non-destructive edits, app package, source JPEG/MP4 bytes, 24 Color Looks and all prior green tests. Never force push or merge if any required gate fails.

## Priority P0: finish production cancellation and physical validation

1. Prove real MediaStore cancellation through Quick Enhance, Studio Editor, Merge Studio and original MP4 passthrough at every transition: before insert, during 128KiB chunks, after write/before publish, after publish/before callback, after Activity destroyed and when a second export starts. Test photo, video, and multi-clip merge through actual UI instrumentation, with zero hidden IS_PENDING rows, no orphaned scoped storage, no unwanted UI callback or concurrent file deletion, original media SHA and previously completed gallery exports unchanged.
2. Confirm Media3 real encoder on Android29 and 35: immediate cancel, 12s H264/AAC cancel after >0 progress, late cancel near completion, source authorization revoked mid-stream, unsupported/malformed codec, A/V sample PTS and 1080p-vs-HD resolution disclosure. Distinguish codec unsupported from a falsely successful full-HD export; never silently publish fallback as FHD.
3. Confirm the default DnCNN model SHA256 59bc280342afd08357282232784d135c29a4ec64cd61b5172921e4d3d881d9fd and XNNPACK-off memory profile, bounded psnr/ssim/chroma/seams with 5 fresh runs on API29 and 2+ on API35, no source resize or model substitution. Using a real USB-attached ARM64 Android10 low-RAM phone and a separate Android15 phone, run the explicit physical QA PowerShell script (diagnostics default, instrumentation only with owner permission). Record actual sampled PSS, CPU, thermal, battery, cancellations and LMK; do not declare physical performance without device logs.
4. With the owner's authenticated Android test account, use *actual Google Photos-owned URI* in native picker with Photos enabled; verify persisted read grant across force-stop, owner revocation/delete, safe Recent and explicit reimport. The independent synthetic QA DocumentsProvider cannot establish compatibility with every Google Photos configuration.

## Priority P1: production polish and signed upgrades

5. Inspect rights-cleared consented real portrait/low-light/bokeh examples including glasses/hair/multiple people, true paired ground truth when available; test crop touch/preview/export pixels and accessible labels/caption safe zones on 320x640/160dpi and 1080x2400/440dpi. Fix concrete mismatches rather than claiming CapCut parity.
6. Owner-only release signing secrets STORYBOOST_KEYSTORE_BASE64, STORYBOOST_KEYSTORE_PASSWORD, STORYBOOST_KEY_ALIAS, STORYBOOST_KEY_PASSWORD. Compare two independently built signed releases, same certificate/package and higher versionCode; perform adb install -r while preserving Recent. Never commit secrets or substitute ephemeral debug keystore.

## Completion contract

Implement/test code, document baseline SHA, QA PR, merge criteria, *exact final-head* 4/4 CI link or precise failed gate, emulator vs physical count, synthetic vs user-consented samples, PSS/quality/video/media-cleanup evidence, artifact URLs and blockers. Keep roadmap 23/32 = 71.875% unless new functionality is truly complete; QA regression and bug fixes do not increment feature count. Never claim lossless social-media upload or all-provider coverage. Keep a unique beta9 next prompt for genuine follow-on work.
