# WFS StoryBoost — Next Execution Prompt (v3.0 beta3)

**Repository:** https://github.com/wawanwfs/WFS-StoryBoost — begin from the latest `main` HEAD. Before changing code, audit README.md, docs/PROGRESS.md, docs/MODEL_RESEARCH_V29.md, docs/NEXT_PROMPT.md, tests/TEST_PLAN.md, all relevant Android source and CI jobs. Preserve every working feature and original media. Work through a branch, use an expected-SHA fast-forward only after successful CI; never force-push or rewrite working history.

## P0 — real persisted editing flow and permission recovery

The beta2 CI has real Activity integration with a **stubbed SAF result** (not full DocumentsUI automation), fresh Home → Recent reopen, JSON corruption, deleted source and synthetic JPEG export on API 29/35. Expand coverage with *actual system DocumentsUI picker*, repeated import of the same URI as a new project (must not silently inherit Recent), and exact preview/crop/caption/AI export comparison. Create a two-phase `adb` test where the Android app is force-stopped between persistent edit and restore; validate old and new process IDs, preferences, SHA256 source bytes and output. Cover revoked third-party SAF read permission, deleted document, corrupt JSON, Clear Recent and process-kill during autosave. Add real touch gestures, Undo/Redo and comparison slider.

## P0 — H.264/AAC end-to-end and cancellation

Preserve beta2's synthetic video generator, MediaExtractor and real API35 encoder verification. Add separate accepted real exports for split, sequential merge of multiple clips, silent source, rotation, cancellation of an *active* transform, malformed input, and no leaked temp/MediaStore files. Inspect **actual** H.264/AAC tracks, rotation, dimensions, duration and A/V drift with MediaExtractor or ffprobe. Distinguish API29 codec limitations from functional defects, and never label an HD fallback as 1080p success. Test at least one low-memory physical Android 10 phone and an Android 15 ARM64 phone when available.

## P1 — UX and AI benchmarking

Inspect actual screenshots at 320×640 and 1080×2400 for title overflow, controls below fold, accidental touches, crop focus mapping, caption safe zones and before/after composition. Add positive gesture tests with decoded output comparisons. Use consented, legally reusable portrait/low-light assets for Google ML Kit Portrait Bokeh edge QA (hair, glasses, multiple people) and pinned DnCNN sigma-25 luminance denoising (Gaussian, chroma noise, real low light, 256px tiles and seams). Report CPU/RAM, end-to-end latency and energy where measurable; PSNR/SSIM only against paired clean targets. DnCNN is **not** deblur or super-resolution. Never ship unlicensed/unchecked models; require weight SHA-256, rights, TFLite CPU inference and quality acceptance before enabling anything new.

## P1 — stable release and real upgrades

Request only GitHub Secrets from the owner:
`STORYBOOST_KEYSTORE_BASE64`, `STORYBOOST_KEYSTORE_PASSWORD`, `STORYBOOST_KEY_ALIAS`, `STORYBOOST_KEY_PASSWORD`. Never commit keystores/passwords. When present, sign ARM64 and Universal release variants and verify that two independent CI builds have exactly the same app package, signer certificate fingerprint, increasing versionCode and working `adb install -r` preserving Recent and projects. Until then, publish debug APKs explicitly marked **debug-signed**, not an upgradeable production release.

## Reporting / acceptance

Record exact commit SHA, Actions URL, job outcomes, test counts, fixture specifications, emulator versus physical device evidence, model benchmark outputs and failed tests. Maintain a **single non-duplicated 32-module ledger** (beta2 is 23/32 = 71.875% feature coverage; test count alone adds no module). Clearly state remaining limitations. WhatsApp/Instagram/TikTok may recompress; do not claim lossless uploads or 100% CapCut equivalence. Update this NEXT_PROMPT file at the end of the next verified iteration.
