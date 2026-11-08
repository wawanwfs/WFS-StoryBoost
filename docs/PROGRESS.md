# WFS StoryBoost — Progress report v2.7

This is a **feature roadmap coverage estimate** for the target v3.0, not a claim of tested production quality. Each checkbox counts as one module. Implemented means source code is present and the project builds; feature correctness still requires end-to-end device tests.

## Project roadmap: 22 / 32 modules ≈ 69%

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

### C. AI photography — 4 / 8
- [x] Offline ML face detection
- [x] Offline ML scene labeling and recommended Look
- [x] Face-aware smart crop
- [x] Offline ML portrait mask / background bokeh (v2.7 beta)
- [ ] Model-backed AI denoising
- [ ] Model-backed AI deblurring
- [ ] True super-resolution (with measured quality)
- [ ] Generative background replacement / inpainting

### D. Export & production — 4 / 8
- [x] Social profiles for WA / Instagram / TikTok
- [x] Crop source-detail guard
- [x] Android APK build and signature checks in CI
- [x] Emulator install + UI smoke tests
- [ ] Physical-device photo/video export QA matrix completed
- [ ] Stable release signing and upgrade test across versions
- [ ] Public release / Play distribution readiness
- [ ] Benchmarks for memory, rendering time and actual output media quality

**Total: 8 + 6 + 4 + 4 = 22 of 32 = 68.75%, rounded to 69%.**

## Evidence and caution
- v2.6 baseline: GitHub Actions build success and 123 pure-Java logic tests.
- v2.7 new: AI Portrait Bokeh + guard regression tests; review **latest successful** GitHub Actions build (do not infer success from a commit alone).
- Emulator smoke checks launching screens, **not** portrait segmentation accuracy, real JPEG visual quality, MP4 playback through social platforms, or installation on real low-memory devices.
- APK debug signing is ephemeral across GitHub runners. Releases require a stable signing key kept privately in GitHub Secrets.
- WhatsApp/Instagram/TikTok compress uploaded videos and images independently. We cannot guarantee uploads will remain lossless.
