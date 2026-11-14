# WFS StoryBoost beta8 — QA evidence

## Baseline verified

- `main` was `cd8ad45535078f80c79876bdde8adb4c11dca378`. Beta8 carries the commits in the unmerged beta7 [PR #8](https://github.com/wawanwfs/WFS-StoryBoost/pull/8) so it does not drop its cancellation work.
- Beta7 head `d61e4ff3ec03fafd2743f4c5befd5a15ff3f9d4e` had [4/4 exact-head jobs green](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42726507194): build/inspection, Android 35 install/launch, API29 and API35 instrumentation. These are **emulator results**, not ARM64 physical evidence.
- Beta7 [API29 DnCNN CSV](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42726507194/artifacts/13722617799) and [API35 DnCNN CSV](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42726507194/artifacts/13723795877) were inspected. Both identify the pinned model SHA `59bc280342afd08357282232784d135c29a4ec64cd61b5172921e4d3d881d9fd`. Each mode had two distinct processes, a synthetic 480 × 480 paired input, and two CPU threads.

| Android | Backend | Sampled peak PSS (KB) | Inference (ms) |
|---|---|---:|---:|
| API29 | Consumer default | 102,316 / 102,649 | 4,388 / 4,335 |
| API29 | Explicit off | 99,826 / 99,093 | 4,379 / 4,423 |
| API29 | Explicit on | 406,213 / 405,501 | 3,188 / 3,294 |
| API35 | Consumer default | 99,866 / 99,989 | 6,520 / 7,804 |
| API35 | Explicit off | 99,836 / 99,832 | 6,607 / 6,656 |
| API35 | Explicit on | 406,590 / 406,747 | 5,933 / 5,893 |

Default/off paired PSNR was 28.80142013 dB and SSIM 0.74544408. Explicit on was 28.80142057 dB and 0.74544435. These sampled process metrics do not establish physical peak memory, battery, thermal, CPU usage, LMK behavior or results on real portraits.

The [beta7 UI artifact](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42726507194/artifacts/13722752034) contains actual 320 × 640 and 1080 × 2400 Editor screenshots. Both show the non-destructive notice over image pixels; a photo also has a glyph-only playback-style control that plays nothing. Beta8 moves the notice outside the canvas and removes the photo-only no-op, while labeling video playback and navigation. The screenshots are not a crop-pixel/caption or TalkBack acceptance test.

## Candidate gate

- `ActivityGalleryCancellationQaTest` uses actual Activity controls, MediaStore, and Media3. Only picker results and boundary timing are controlled. The debug-only in-process observer is not a release feature, and separate real DocumentsUI regression remains mandatory.
- Verify absence of new Gallery rows including hidden `IS_PENDING`, absence of leaked encoder cache, preservation of original SHA and prior accepted output, and no stale success UI. Intermediate ownership must stay with the writer during cancellation. Tests cover cancellation before insert, during chunks, before publish, after publish and after Activity teardown before UI delivery.
- API29 currently exercises photo, original passthrough and malformed preflight in the new tests. New actual Media3 Activity encoder/merge paths run on API35. Do not infer API29 codec compatibility from passthrough or extractor coverage.
- The repeated DnCNN backend gate requires five fresh default processes on API29 and two on API35. This is a test change, not a measurement until CI reports exist.
- Merge only after review of exact final-head 4/4 CI, applicable repository review requirements, and any release blockers. Do not mark a stable signed upgrade or production release verified based on debug APK artifacts. APK version/package/keys have not been changed by beta8 QA.

## Still open

No connected physical ARM64 API29 low-RAM or API35 handset, authenticated Google Photos-owned source, consented real portrait/low-light sample set, or owner-controlled two-release signing verification. No new proof of late actual encoder progress, loss of source access after stream open, or full crop/caption pixel parity. No lossless social-media upload or CapCut parity claim. Roadmap remains 23/32.
