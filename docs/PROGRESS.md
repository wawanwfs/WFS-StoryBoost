# WFS StoryBoost — Progress report v2.9.0

This is a **feature roadmap coverage estimate** for the target v3.0, not a claim of tested production quality. Each checkbox counts as one module. Implemented means source code is present and the project builds; feature correctness still requires end-to-end device tests.

## Project roadmap: 23 / 32 modules ≈ 72%

### A. Editor foundation — 8 / 8
- [x] Import local photo/video
- [x] Project dashboard and project creation
- [x] Recent project list
- [x] Interactive photo/video preview
- [x] 11 ratios + custom dimensions
- [x] Fit / Crop / blurred framing
- [x] Photo & video media paths
- [x] Multi-item Quick Enhance batch

### B. Editing capability — 6 / 8
- [x] 24 Color Looks
- [x] Manual brightness/contrast/saturation
- [x] Text overlay
- [x] Video trim
- [x] Video split export
- [x] Join multiple video clips
- [ ] Multi-track video / audio / image layers
- [ ] Animated keyframes and transitions

### C. AI photography — 5 / 8
- [x] Offline ML face detection
- [x] Offline ML scene labeling and recommended Look
- [x] Face-aware smart crop
- [x] Offline ML portrait mask / background bokeh (v2.9.0 beta)
- [x] Model-backed luminance AI denoising — DnCNN on-device (v2.9), bounded input 2.6 MP
- [ ] Model-backed AI deblurring
- [ ] True super-resolution (with measured quality)
- [ ] Generative background replacement / inpainting

### D. Export & production — 4 / 8
- [x] Social profiles for WA / Instagram / TikTok
- [x] Crop source-detail guard
- [x] Android APK build and signature checks in CI (ABI-specific ARM64 and universal debug artifacts)
- [x] Emulator install + UI smoke tests
- [ ] Physical-device photo/video export QA matrix completed
- [ ] Stable release signing and upgrade test across versions
- [ ] Public release / Play distribution readiness
- [ ] Benchmarks for memory, rendering time and actual output media quality

**Total: 8 + 6 + 5 + 4 = 23 of 32 = 71.875%, rounded to 72%, only if CI inference benchmark passes.**

## v2.9 neural inference evidence and limitations
- Candidate model: Qualcomm DnCNN-25 float TFLite (MIT / KAIR), grayscale Gaussian denoise trained for sigma=25. App projects luminance only and preserves chroma.
- CI verifies SHA256 `59bc2803…1d9fd`, packages actual weights and runs real TensorFlow Lite CPU inference. Initial successful unpinned v2.9 CI: PSNR 20.18→28.49 dB, global SSIM 0.7764→0.9591 on deterministic Gaussian noise; model inference 1.526 seconds for one 256×256 tile on emulator. Final pinned build is checked separately. This is not a clinical measure or real-photo blind IQA.
- Full-resolution images >2.6 MP intentionally return an error; true 4K denoise is **not** supported. Do not claim deblur, super-resolution, detail recovery or identical result on social platforms.
- Physical devices, real portrait QA, stable signing and multi-track are still pending.

## v2.8 delta and evidence boundaries
- End-to-end tests now render actual synthetic JPEGs in Android emulator (color, geometry, EXIF, raw-copy, negative/no-person AI) and collect memory/timing reports. These tests do not constitute completed physical-device QA or full benchmarks across hardware.
- Portrait mask raw-size inference, feather policy, aligned blur and asynchronous preview debouncing implemented. Real human image quality and hair, glasses, multi-person, night and dark-image behavior remain unverified.
- **The v2.8 baseline was 22/32 (69%)**; v2.9 adds only the independently tested DnCNN denoise module.

## Evidence and caution
- v2.6 baseline: GitHub Actions build success and 123 pure-Java logic tests.
- v2.9.0 new: AI Portrait Bokeh + guard regression tests; review **latest successful** GitHub Actions build (do not infer success from a commit alone).
- Emulator smoke checks launching screens, **not** portrait segmentation accuracy, real JPEG visual quality, MP4 playback through social platforms, or installation on real low-memory devices.
- APK debug signing is ephemeral across GitHub runners. Releases require a stable signing key kept privately in GitHub Secrets.
- WhatsApp/Instagram/TikTok compress uploaded videos and images independently. We cannot guarantee uploads will remain lossless.
