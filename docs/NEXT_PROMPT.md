# WFS StoryBoost — Next Execution Prompt v3.0 beta6

Continue https://github.com/wawanwfs/WFS-StoryBoost from the newest verified main. Audit beta5 draft PR #6, exact latest CI jobs/logs, the QA DocumentsProvider APK, DnCNN cold-process CPU matrix, Media3 sample PTS assertions, README, PROGRESS, TEST_PLAN and MODEL_RESEARCH. Preserve working offline functionality, original bytes, UI/API compatibility, app ID and debug APK production isolation. No force push; never merge a failed gate.

## P0: Close remaining beta5 blockers

1. Run independent-UID QA DocumentsProvider tests on Android29 and 35, with actual native ACTION_OPEN_DOCUMENT picker, persisted grant, true external adb force-stop and distinct PID Recent restore with SHA/decoded JPEG checks. Prove owner-driven persistent grant revocation, deleted and disabled provider behavior, explicit re-grant after reimport; collect screenshots/hierarchy/logcat for failures. Do not use app-owned MediaStore as a substitute, and do not ship the QA provider in release artifacts. Actually select a Google Photos-owned URI with Google Photos enabled in separate tests; simple picker coexistence is NOT that proof.
2. Reproduce the transient ~400MB TFLite peak PSS observed on both API29 and API35; run five or more independent real DnCNN synthetic paired CPU inferences per 1/2/3 CPU thread configuration on API29 and comparisons on API35. Record Java/native/graphics/time/PSS, model SHA, unchanged paired PSNR/SSIM/chroma/32px tile seam. Analyze tensor/delegate scratch memory before choosing any safe production change. Do not quietly lower resolution or model quality. A physical Android10 low-RAM ARM64 phone is required for approval.
3. Run real Media3 trims, split, merge, audio/silent in BOTH orders, sample PTS drift and rotated pixel dimensions on API29/35 where supported. Add cancellation at multiple points, unsupported codecs/malformed inputs and zero cache or MediaStore leaks; diagnose failing encoders. Never describe an HD fallback as 1080p success.

## P1: Real-world UX, AI and release

4. Inspect 320x640/160dpi and 1080x2400/440dpi screenshots, crop touch-to-export pixel agreement, caption safe zones, comparison, contrast/overflow/accessibility and each of 24 Color Looks. Screenshots alone do not prove UX quality.
5. Obtain rights-cleared and consented paired real portraits and low-light photos for DnCNN luminance/chroma/seams and ML Kit hair/glasses/multi-person bokeh. Measure physical ARM64 CPU/RAM/thermal and paired-only PSNR/SSIM; verify license/weight hash for each model; no new model without legal and inference validation.
6. Require owner GitHub secrets STORYBOOST_KEYSTORE_BASE64, STORYBOOST_KEYSTORE_PASSWORD, STORYBOOST_KEY_ALIAS, STORYBOOST_KEY_PASSWORD, never commit keystores. Verify same certificate across two independent release builds, increasing versionCode and genuine adb install -r with Recent restoration; until then publish clearly labeled debug APK only.

## Completion

Report exact baseline, commit SHA, PR, CI URLs and ALL job outcomes, emulator vs physical counts, raw memory/media results, feature ledger (baseline 23/32=71.875%), blockers and artifacts. Update all docs with a genuinely new next prompt. Do not claim WA/IG lossless, provider compatibility beyond proven sources or complete CapCut parity. Merge only after entire final-head CI is green; otherwise preserve draft QA branch.
