# WFS StoryBoost — Manual Device QA Matrix v1.3.0

Geometry tests (`bash scripts/test-ratio-geometry.sh`) cover 17 non-Android assertions. **They do not prove Android build or end-to-end behavior.** Before a production release, run this checklist on Android 10/12/14/16 (at least low-memory and flagship codecs).

| # | Input | Selected output | Framing | Expected dimensions / behavior |
|---|---|---|---|---|
| 1 | Landscape photo 1920×1080 | 9:16 FHD | Fit | 1080×1920, entire photo, top/bottom bars |
| 2 | Landscape photo 1920×1080 | 9:16 FHD | Crop | 1080×1920, center cropped left/right |
| 3 | Landscape photo 1920×1080 | 9:16 FHD | Blur | 1080×1920, entire photo + soft background |
| 4 | Portrait photo 1080×1920 | 16:9 FHD | Fit | 1920×1080, entire photo, side bars |
| 5 | Portrait photo 1080×1920 | 16:9 FHD | Crop | 1920×1080, cropped top/bottom |
| 6 | Portrait photo 1080×1920 | 16:9 FHD | Blur | 1920×1080, entire photo + blurred sides |
| 7 | Any photo | 1:1 FHD | Crop | 1080×1080 |
| 8 | Any photo | 4:5 HD | Fit | 720×900 |
| 9 | 640×480 photo | Original FHD | Fit | 640×480; no upscale |
| 10 | EXIF-rotated JPEG / HEIC | 9:16 | Fit | Upright composition, no sideways output |
| 11 | Landscape video with audio | 9:16 FHD | Fit | 1080×1920 MP4, audio present/synced |
| 12 | Portrait video with audio | 16:9 FHD | Crop | 1920×1080 MP4, audio present/synced |
| 13 | Portrait video | 16:9 FHD | Blur | Fit black bars; UI clearly warns fallback |
| 14 | Rotated video | Original HD | Fit | Correct orientation, even dimensions |
| 15 | 10 mixed files | 9:16 HD | Fit | All processed sequentially, saved separately |
| 16 | Corrupt file | 9:16 HD | Fit | Error shown; queue continues |
| 17 | Large video + Cancel | Any | Any | Encoder stops, temp file removed, UI recovers |
| 18 | 4K H.265 / HDR | 9:16 FHD | Crop | Export success or informative device-related error |
| 19 | App idle > 1min | Save/share | Any | Can share saved output, readable file URI |
| 20 | WhatsApp Status upload | 9:16 | Fit | Visually inspect original/export/post-upload; compression likely |

Inspect recorded MP4 video with MediaInfo or `ffprobe`: width, height, duration, video H.264, audio AAC if source has audio. Listen fully to confirm no broken audio/transitions. Confirm Gallery folders, rotate the phone, and retry after process kill. Do not call the APK production-ready without these device tests passing.

## Additional v1.3 QA — before/after and bitrate

- [ ] Import an image, export with Natural, confirm two halves show same composition and dragging separator reveals the difference.
- [ ] Repeat with 16:9 source → 9:16 Fit and Crop; no mismatched scale between before and after.
- [ ] Repeat with 9:16 → 16:9 Fit, Crop and photo Blur.
- [ ] Export 10 photos sequentially: slider updates to latest photo and no crash/memory leak is observed.
- [ ] Export a video after a photo: stale comparison is hidden; audio plays and exported resolution is correct.
- [ ] Video HD/Full HD bitrate and file size look reasonable; compression may differ by encoder.
- [ ] Cancel a long video and inspect that temp file and pending queue are cleaned.
- [ ] Test Android 10 and Android 16 with HEIC photos, unusual EXIF rotations, silent videos, and high-fps recordings.
