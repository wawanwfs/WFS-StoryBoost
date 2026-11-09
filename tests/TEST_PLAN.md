## Beta5 QA hard gates (candidate)

- [ ] Independent-UID QA-only DocumentsProvider APK and real Android DocumentsUI picker on API29/35.
- [ ] Native persisted grant, external force-stop, different PID Recent, decoded JPEG and preserved SHA.
- [ ] Owner-controlled revoke, deleted/disabled provider, no accidental gallery source read, explicit reimport.
- [ ] DnCNN pinned model CPU 1/2/3 threads x five fresh API29 processes and two fresh API35 processes each, quality and sampled transient memory intact.
- [ ] Actual sample PTS and reversed mixed-order AAC/silent Media3 merge, plus all existing video and 24 Color Look regressions.
- [ ] Verify final HEAD all four jobs green; keep PR draft and do not merge otherwise.
- [ ] Google Photos-owned URI, real consented portraits, physical low-RAM ARM64 and stable owner-signed upgrade NOT proven.

## Beta4 QA candidate gates

- [ ] Local app-private media process death with qa-process-death-local.sh (not third-party MediaStore permission evidence).
- [ ] Abrupt live Editor death with qa-process-death-pending.sh: completed look/caption/1080p changes, Editor still RESUMED, external adb force-stop before onPause, new PID, Recent, source SHA and decoded JPEG.
- [ ] Genuine native ACTION_OPEN_DOCUMENT persisted grant on both APIs with Photos temporarily isolated, plus WFS_PHOTOS_ENABLED=1 compatibility pass. Enabled Photos is NOT a Photos URI test.
- [ ] Two isolated real DnCNN inference runs with Java/native/graphics PSS/thread records and live sampled PSS on API29/35. Physical-device and five-process research pending.
- [ ] Existing 24 Color Looks, Media3 mixed/split/merge/silent/rotated/cancel and byte-preservation gates stay green.
- [ ] Build a distinct test-only revocable provider and prove external permission revoke, provider disable/deletion and Photos-owned URI. Keep separate from local MediaStore.
- [ ] All four CI jobs green before merge. Retain 23/32 unique module count. No release without owner-signed independent install-r proof.

# WFS StoryBoost — Manual Device QA Matrix v1.4.0

Geometry tests (`bash scripts/test-ratio-geometry.sh`) cover 27 non-Android assertions. **They do not prove Android build or end-to-end behavior.** Before a production release, run this checklist on Android 10/12/14/16 (at least low-memory and flagship codecs).

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

## Additional v1.4 QA — before/after and bitrate

- [ ] Import an image, export with Natural, confirm two halves show same composition and dragging separator reveals the difference.
- [ ] Repeat with 16:9 source → 9:16 Fit and Crop; no mismatched scale between before and after.
- [ ] Repeat with 9:16 → 16:9 Fit, Crop and photo Blur.
- [ ] Export 10 photos sequentially: slider updates to latest photo and no crash/memory leak is observed.
- [ ] Export a video after a photo: stale comparison is hidden; audio plays and exported resolution is correct.
- [ ] Video HD/Full HD bitrate and file size look reasonable; compression may differ by encoder.
- [ ] Cancel a long video and inspect that temp file and pending queue are cleaned.
- [ ] Test Android 10 and Android 16 with HEIC photos, unusual EXIF rotations, silent videos, and high-fps recordings.

## v1.4 tambahan

- [ ] Tekan tombol tukar rasio 9:16 ↔ 16:9, konfirmasi ukuran berubah pada label output.
- [ ] Pilih Soft Night pada foto malam; pastikan shadow lebih terbaca tanpa mengubah wajah.
- [ ] Ekspor video 60 fps dan 30 fps; periksa peningkatan target bitrate bila metadata tersedia.
- [ ] MP4 rusak atau output resolusi salah harus gagal validasi sebelum tersimpan di galeri.

## v1.5 QA tambahan
- [ ] Video Blur 9:16 ↔ 16:9 di beberapa GPU, periksa tidak gepeng.
- [ ] Preview video asli/hasil, playback, audio, seek.
- [ ] Dual-aspect untuk foto/video, cek 2 output masing-masing.
- [ ] Video trim 5–15 detik dari sumber 20 detik, cek durasi dan audio.
- [ ] File dengan dan tanpa audio, sumber HDR, 60fps/120fps.
- [ ] Fallback ketika perangkat tidak mendukung output Full HD.
- [ ] Periksa sinkronisasi audio secara manual di perangkat.

## v1.6 Crop focus
- [ ] Foto landscape ke portrait fokus X 0/50/100.
- [ ] Video portrait ke landscape fokus Y 0/50/100.
- [ ] Fit dan Blur tidak berubah saat fokus digeser.
- [ ] Dual-aspect tidak mendistorsi isi gambar.

## v1.7 Install / launcher icon / upgrade QA
- [ ] Android 10, 13, and 16: install APK with package `com.wfs.storyboost.studio`.
- [ ] App launcher shows WFS camera/play icon and the correct application label.
- [ ] Cold launch works; photo picker opens without ALL_FILES_ACCESS or internet permission.
- [ ] `adb shell am start -n com.wfs.storyboost.studio/com.wfs.storyboost.MainActivity` succeeds.
- [ ] APK contains adaptive launcher icon, passes `apksigner verify`, `zipalign -c` and ZIP integrity check.
- [ ] With an older `com.wfs.storyboost` build still installed, new package installs side-by-side.
- [ ] After GitHub secrets are configured, two successive *signed release* builds have the same signer certificate and install with `adb install -r`.
- [ ] Debug APK update between GitHub runs can fail due to ephemeral debug certificates: DO NOT promise seamless updates.

## v2.1 actual output & UI
- [ ] Real emulator screenshots: home hero contains scenic artwork, six action tiles, and functional bottom navigation.
- [ ] Tap each action: photo picker, video picker, ratio navigation, batch picker, filter editor, result navigation.
- [ ] Landscape 1920x1080 → Story 9:16 Crop: Quality Advisor warns about retained pixels and offers Blur.
- [ ] Portrait 1080x1920 → 16:9 Crop: same warning; selecting Blur preserves complete photo.
- [ ] JPEG with Asli/Original/Fit/Max Detail: output SHA-256 matches input (no recompression).
- [ ] MP4 with Asli/Original/Fit/Max Detail/no trim: output SHA-256 matches input.
- [ ] Enhanced MP4 and JPEG: original must not be copied unchanged; preview and share remain functional.
- [ ] Check screenshot in 320x640 and large resolution: no overflow, no invisible buttons, no clipped labels.

## v2.2 Project workspace and timeline QA
- [ ] Android emulator: launcher goes to `StudioHomeActivity`, demo opens `StudioEditorActivity`.
- [ ] Photo Editor / Video Editor tiles open gallery picker filtered to their type.
- [ ] New Project accepts URI via Storage Access Framework and grants persisted read permission when available.
- [ ] Recent project can reopen with correct URI; Clear recent never deletes original gallery files.
- [ ] Editor photo preview loads; ratio 9:16,16:9,1:1,4:5 and Original change preview aspect.
- [ ] Filter chips update photo live preview, and exported JPEG uses same PhotoEngine filter.
- [ ] Frame Fit/Crop/Blur, focus X/Y controls all work.
- [ ] Video thumbnails from actual MediaMetadataRetriever frames, play, pause, seek and trim input work.
- [ ] Split at playhead creates two independent MP4 exports, preserving audio track if present.
- [ ] Dual-ratio export for photos/videos; progress, Gallery and share ACTION_SEND.
- [ ] Crop severe warning provides safe Blur alternative; test 1920x1080 source into 9:16.
- [ ] Android low memory and 4K HEVC source: gracefully report unsupported codec or memory, never claim detail creation.
- [ ] Screen-size snapshots for home/editor at small 320x640 emulator and 1080x2400 phone.

## v2.3 Color / Text / Audio
- [ ] Foto: Brightness/Contrast/Saturation -100,0,+100; preview and exported pixels change consistently.
- [ ] Photo text overlay with 0, 1, 80 characters; text does not exceed frame width.
- [ ] Video with title overlay: rendered MP4 contains text throughout clip (GPU overlay) and preserves audio when mute disabled.
- [ ] Video mute on/off: MediaProbe output has no audio when selected; includes audio otherwise.
- [ ] Split + dual ratio retain text, color adjustments and mute settings for every output.
- [ ] Copy original JPEG/MP4 is disabled when caption/adjustment/mute is requested.
- [ ] Test UI on 320x640 display: Adjust, Text and Audio tabs can be scrolled horizontally and all controls remain interactive.

## v2.4 Multi-clip merge QA
- [ ] Add 2 or more videos using Android Storage Access Framework (no broad gallery permission).
- [ ] Clips appear in the right order, can preview, reorder, and remove without modifying source files.
- [ ] Merge 2 identical 1080p AVC/AAC clips; resulting MP4 duration equals sum (within encoder tolerance).
- [ ] Mixed landscape and portrait clips: verify Fit letterboxing and correct final aspect.
- [ ] Silent video + clip with audio: check expected audio behavior.
- [ ] 4K/HDR/HEVC mixed inputs: fail gracefully with meaningful codec error; report unsupported device.
- [ ] Cancel merge mid-export; no pending partial video saved to public gallery.
- [ ] Android emulator opens real MultiClip screen without crash; capture UI screenshot.

## v2.5 Social Export / Color Look QA
- [ ] Photo Editor: scroll 24 look thumbnails, confirm visible selection, live preview and JPEG output after applying each.
- [ ] Video Editor: verify all 24 look options can be selected and each changes rendered result; keep source audio.
- [ ] Quick Enhance: 24 looks appear; choosing custom / platform modifies target canvas as shown.
- [ ] WhatsApp Status: 1080x1920 portrait JPEG or H.264 MP4/AAC; bitrate <= 8 Mbps requested; evaluate decoded size, actual average bitrate and visible quality.
- [ ] Instagram Story/Reels/TikTok: 9:16 1080x1920 portrait output, caps 12/16/18 Mbps, stereo AAC when audio present.
- [ ] Social safe guide: shown only in social mode and not burned onto exported frames.
- [ ] Social text title: center zone in exported JPEG and MP4; preview placement corresponds reasonably.
- [ ] Landscape 1920x1080 to 9:16: Fit by default on social selection, never silently crop.
- [ ] Platform upload QA on actual WhatsApp/Instagram/TikTok applications; no claim against server-side recompression.
- [ ] Repeated social profile change does not damage previous project colors/clip bounds.

## v2.6 AI Photo / 12 Ratio QA
- [ ] Select 3:4,4:3,3:2,2:3,21:9,9:21 in both photo and video editors.
- [ ] Custom 1350x1080 portrait, custom 2048x1152 landscape, custom 1080x1920 for story; verify MP4 and JPEG metadata.
- [ ] Reject custom dimensions 0,5000,5000 or > 8.3 megapixels with clear error.
- [ ] Quick Enhance batch and Studio Editor produce matching output ratio.
- [ ] ML Kit bundled: offline airplane mode, detect a real face and propose face-aware crop without editing identity.
- [ ] Label real sunset, beach, forest, and portrait pictures; recommendations may be wrong, always user-confirmed.
- [ ] ML Kit analysis and repeated exit/re-entry: no crashes or bitmap leaks.
- [ ] Test on Android 10 and Android 15 physical devices; both models packaged with APK.
- [ ] No automatic AI upscaling / generative face editing; ensure README accuracy.

## v2.7 Real ML Portrait Bokeh (BETA)
- [ ] With **real human portrait**, toggle AI Portrait: background blurred while facial features, skin and identity are unchanged.
- [ ] With two or more people, check both subjects protected by mask; hair glasses and transparent items may show artifacts.
- [ ] Toggle before/after: both previews show matching ratio and focus; caption should not be duplicated.
- [ ] Export bokeh JPEG and verify blurred background is actually in saved JPEG, not preview-only.
- [ ] Original/Max Detail/No Color with bokeh ON must transcode, not copy original unchanged.
- [ ] No-person landscape photo should display actionable model error; it must not export a misleading success.
- [ ] Airplane-mode model loading works; image is never transmitted to network.
- [ ] Small 3GB RAM handset, low light portrait, high resolution 12MP input: no app process kill; HD fallback and OOM error are clear.
- [ ] EXIF-rotated person image: foreground mask stays correctly aligned with upright subject.
- [ ] AI preview after rapidly changing Look/Ratio doesn't show an outdated result or recycled bitmap.
- [ ] Validate original vs. export on phone and WhatsApp Status; server-side recompression remains outside app control.

## v2.8 real-image automated and human QA
- [ ] GitHub Actions reports all real *synthetic JPEG* tests: 9:16 pixel dimensions, color adjustment, 3:4 custom export, EXIF rotation, MediaStore exact-copy and blank-photo AI rejection.
- [ ] Inspect machine-readable artifact `qa-photo-export.txt` and `qa-photo-memory.txt`; do not mark real device benchmarking complete.
- [ ] Verify preview cancellation on repeated filter taps and ratio changes while Portrait Bokeh runs on an actual photograph.
- [ ] Human single subject, multiple subjects, strands of hair, glasses, dark backgrounds, partial face, and no-person picture: compare export to preview and source; protect original face geometry.
- [ ] Check model segmentation accuracy using consented/rights-cleared images only, never commit private face photographs.
- [ ] Test Android 10 low memory and arm64 physical phone. Verify HDR/HEIC and odd EXIF orientation.
- [ ] Integrate no unlicensed restoration weights. Report measured denoise/deblur/4x-SR only if a working on-device inference model exists.

## v2.9 DnCNN neural denoise and image quality
- [ ] CI pins and verifies SHA256 of Qualcomm DnCNN float TFLite asset (MIT, grayscale sigma=25), confirms asset inside ABI APK.
- [ ] Real TFLite `Interpreter` runs on CPU, validates float32 256×256 tensors; no mocked neural output.
- [ ] Compare PSNR and global SSIM of deterministic noise versus DnCNN output. Report actual times and RAM, no invented values.
- [ ] Test inference on low-end Android 10 ARM64 and x86 emulator with CPU fallback and real photos, portrait details and noisy dusk scenes.
- [ ] Reject images >2.6 MP clearly. Validate that the original source remains unchanged.
- [ ] Check denoise only applies to luminance; no accidental chroma saturation or face shape changes.
- [ ] Undo/Redo 35-snapshot history: preset, caption, framing, AI toggles, custom ratio and trim. Verify toolbar accessibility and persistence within the current editor session.
- [ ] End-to-end neural JPEG export: AI option changes real JPEG bytes, output retains dimensions and can be decoded on emulator.
- [ ] Check 256 tile overlaps on image borders and 1000×1200 images; no visible seams.
- [ ] AI Denoise and Portrait Bokeh combined, before/after preview and actual saved JPEG, original-copy guard.
- [ ] Continue real-portrait Bokeh QA with consented human samples, glasses/hair/night/EXIF.

- [ ] Touch Crop: drag left/right/up/down in Crop mode, verify focus X/Y changes and one final preview on touch release; Undo restores previous focus and Redo reapplies.
- [ ] Drag surface disabled on Fit/Blur and does not interfere with video playback or social safe-zone overlays.

## v3.0 beta1 — Persisted edit drafts and preflight dimensions
- [ ] Open a persisted-URI photo, change Look, ratio, custom dimensions, caption, focus X/Y and AI flags, close editor and reopen **Proyek Terakhir**. Confirm all settings restored, without altering source bytes.
- [ ] Android process killed during background: draft survives; corrupt state and revoked/deleted media never crash the app.
- [ ] Clear Recent clears both media list and drafts, without deleting Gallery original/export files.
- [ ] Fresh new import is not forced into an existing draft; only Recent explicitly resumes a draft.
- [ ] Restore trim/mute, split and dual export from a persisted video source; state must be validated against changed media duration.
- [ ] Neural Denoise photo source >2.6 MP rejects in preview and export BEFORE expensive decode, not silently downsampled.
- [ ] Read the actual emulator report; it must have STATUS=PASS, draftPersistenceTests and over2_6MPGuard, plus original neural PSNR/SSIM metrics.
- [ ] Run real UI test on Android 10/15 physical devices. These items remain incomplete until verified on hardware.

- [ ] On 320x640 emulator, Editor title stays one line, Undo/Redo and Export buttons all visible and clickable.
- [ ] In photo mode no disabled video seekbar consumes vertical space; video mode retains working seekbar.

## Beta2 instrumented coverage — 2026-11-08

The [successful candidate CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42491741331) ran **4 instrumented tests on API 29** and **5 on API 35**, plus separate APK build and API 35 install/smoke jobs. The photo import test simulates the SAF `ACTION_OPEN_DOCUMENT` result; it is not a real picker-interaction test.

- [x] Android Activity photo changes, persisted draft, fresh Home/Recent resume, decoded JPEG 1080×1920 and unchanged SHA256 source.
- [x] Corrupt draft and missing/deleted URI safe rejection; Clear Recent cleanup.
- [x] Generated H.264/AAC, silent, rotated and malformed MP4 inspection on Android.
- [x] API35 Media3 trim encoder: actual 1080×1920 H.264/AAC output, A/V duration and drift assertions.
- [x] Pixel 6 AVD captured PNG screenshots at 320×640 and 1080×2400 with pixel dimension verification ([smoke job passed in run 42493751785](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42493751785); overall run failed a separate flaky dialog test, subsequently hardened).
- [ ] Actual Android DocumentsUI interaction, process death/force-stop/reopen and third-party URI revocation.
- [ ] Repeated import versus explicit resume, physical gesture crop, Undo/Redo and before-after drag through real output.
- [ ] Full split, merge, cancellation and unsupported/low-memory codec QA across physical devices.
- [ ] Rights-cleared real photos for bokeh hair/glasses/multiple people, DnCNN low light/chroma/tile-seam tests and device CPU/RAM.
- [ ] Two independent owner-signed APK builds with same certificate and successful `adb install -r`.

Do not count tests as new roadmap modules. Current ledger remains 23 of 32 unique features. Debug artifacts are not stable signed production builds.

## Beta3 verified and pending acceptance

- [x] Two independent `adb am instrument` processes separated by **external** `adb am force-stop` on API29/35; PID changes, MediaStore Recent/draft persists, 1080×1920 JPEG and original SHA-256 unchanged. [CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42499169024).
- [x] API35 synthetic video: actual H.264/AAC split/merge, silent and rotated transformations, active cancellation and no new cache temp files.
- [x] Android Activity crop gesture focus, Undo/Redo, before-after control, repeated same-URI import starts a fresh project, not Recent.
- [x] Native `ACTION_OPEN_DOCUMENT` through real DocumentsUI selection on API29 and 35, persisted read grant checked, external force-stop/new-PID Recent restore and source SHA-256 intact ([4/4 CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42506941662)). Google Photos was disabled temporarily during this isolated emulator test.
- [ ] Actual Google Photos and third-party DocumentsProvider compatibility, revoked persistable URI grants and missing/deleted provider recovery **remain unverified**.
- [ ] Forced death during **pending autosave**, real-device benchmark/gesture QA, consented real-human bokeh, real low-light chroma/tile-seam DnCNN benchmark.
- [ ] Owner release keystore, two same-certificate signed APKs and actual `adb install -r` preserving project state.

Tests are not additional feature modules. Progress is 23/32.

### Beta3 DnCNN multi-tile synthetic benchmark

- [x] Real Qualcomm/KAIR MIT model loaded by TFLite CPU on API29/API35 and 480×480 paired synthetic RGB image processed across four overlapped 256px tiles; color-channel delta invariance and seam boundary guards. [Green CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42501208561).
- [x] Reported API35 synthetic 4-tile values: inference 10.783 s, total 12.028 s; PSNR 20.307→28.801 dB, SSIM 0.293→0.745, PSS snapshots 48,144→53,274 KB. Not peak memory or physical device data.
- [ ] Consented real portrait and low-light/chroma-seam visual evaluation remains incomplete. No module increase from 23/32.

### Beta3 memory-risk follow-up

- [x] Record the API29 outlier on the synthetic four-tile DnCNN run (PSS 41,790→365,138 KB), alongside the API35 snapshot (48,004→41,105 KB). [Source run](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42502015377).
- [ ] Reproduce in at least five clean Android10 emulator runs, compare native/Java/graphics/peak allocations, tune TFLite thread count only after measuring, and check device stability.
- [ ] Verify one low-RAM Android10 physical ARM64 and a physical Android15 ARM64; consented portrait/chroma/low-light data remain outstanding.
