# Beta11 — captions in encoded video

Baseline main `cd8ad45535078f80c79876bdde8adb4c11dca378`; beta10 head `a0e8f9638b53b2b455e98ce80651e834c78bd29b` and its ancestor drafts remain intact. [Beta10 exact CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/43608346447) passed 4/4. API29 Quick Enhance and Merge Studio published real H.264/AAC to Gallery, and photo crop and safe-zone tests passed at both viewport sizes on both APIs.

- Video preview and exported video now use the same caption geometry and typography as JPEG. The overlay is rasterized once per frame size, after framing, into a transparent strip. It is not a full-frame bitmap allocated per frame.
- The Media3 overlay uses display coordinates (not source rotation) and works with the selected manual or social profile. Source playback is still a source preview; this change does not preview all video effects.
- Instrumentation uses monochrome 0° and 90° clips. It decodes MP4 frames at two timestamps and compares detected caption bounds with the Editor overlay, checks safe margins, AVC/AAC, real sample timestamp drift and input SHA. It also verifies actual dimensions, not just the requested quality.
- Keep existing gallery publication/cancellation, source grants, 24 Color Looks, app package, draft and DnCNN regression gates.

Pending separately: owner-consented portrait and low-light validation, physical API29 low-RAM/API35 ARM64 with performance/LMK evidence, actual Google Photos URI persistence, manual TalkBack, and owner-signed install-over-upgrade on a spare device. No physical or portrait claim follows from emulator fixtures. Roadmap stays 23/32 (71.875%).
