# WFS StoryBoost beta10 — Gallery and photo viewport QA

Verified baseline main `cd8ad45535078f80c79876bdde8adb4c11dca378`. This candidate extends beta9 `04edca628590ad2d6168336c5db90ac13e8d21a6` and the beta7–beta9 draft stack, without modifying the other drafts or main. [Beta9 exact-head CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/43601678888) was 4/4: 18 connected tests passed on API29 and 29 on API35. The API29 engine emitted genuine 720×1280 and 1080×1920 H.264/AAC with 5 ms final-sample drift; API35 cancelled at 85% and rejected a separately owned source after grant revocation.

## New evidence targets

- API29 Quick Enhance real Full HD MP4 transcode and API29 Merge Studio two-clip Gallery publication. Inspect the finished MediaStore row, encoded canvas, codecs, sample PTS, duration, intermediate cleanup and both source hashes. A native-size merge is not a Full HD result.
- Photo caption layout uses the same proportional renderer for Editor preview and JPEG. The ImageView no longer crops already-framed previews a second time. AI preview reproduces the blurred framing background used in export.
- Use synthetic stripes and a caption (no portrait claims). On both API29 and API35 exercise the actual crop gesture, compare preview/export pixel location and safe-zone boundaries at 320×640/160dpi and 1080×2400/440dpi, and exercise the labeled crop SeekBar through Android accessibility actions. This is not a manual TalkBack session and does not certify video caption parity.
- Physical Android 10 and 15, real Google Photos URI, licensed/consented portrait samples, and two independently owner-signed APKs remain separate. No APK has been installed over a user's production installation.

The roadmap stays 23/32 (71.875%). Check the exact candidate workflow before accepting any test result.
