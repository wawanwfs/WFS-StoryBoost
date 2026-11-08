# WFS StoryBoost — Next execution prompt (after v2.9.0)

Continue development in https://github.com/wawanwfs/WFS-StoryBoost using the latest main branch (v2.9.0 or later). Audit README.md, docs/PROGRESS.md, tests/TEST_PLAN.md, CI build logs, and current Android source before editing. Make changes on an appropriate development branch or use safe fast-forward updates without overwriting concurrent work. Preserve commit attribution to the connected user's GitHub identity.

**Priority 1 — image quality & real AI models**
- Recheck DnCNN TFLite asset SHA, licensing and all emulator PSNR/SSIM/latency reports. On physical Android 10+ ARM64 phones, test CPU speed, memory, JPEG export, and ensure model preserves face texture. Never treat synthetic-only metrics as real-image results.
- Test non-Gaussian noise and chroma noise separately; the current DnCNN is grayscale sigma25 only. Consider a separately licensed small RGB denoiser if real portraits require it.
- Design optional model downloads only with cryptographic hash verification and user consent; do not add online access to private user photos.
- Add Undo/Redo editor state snapshots, persist projects safely, touch crop controls, and tested MP4 1080p media benchmarks.
- Read `docs/MODEL_RESEARCH_V28.md` and the synthetic-media CI QA report. Source a legally redistributable LiteRT model and independently verify its model license, binary SHA256, CPU/GPU support, minimum Android/API, input tensor layout, output limits, memory usage, and quality on real paired images before enabling a user-facing AI denoise/deblur feature.
- Evaluate the MIT-licensed NAFNet-SIDD-width32 LiteRT candidate **only on supported GPU backends** (62.5 MB FP16, unsuitable as universal CPU fallback) and avoid bundling it without a documented fallback and size budget.
- Review TensorFlow's ESRGAN SISR sample; a 50×50 tile 4× example is not proof of full-resolution photo restoration. Confirm separate model-weight licensing, image tiling seams, and performance.

**Priority 1B — real-portrait validation**
- Add licensed (with consent) real image fixtures for single face, multiple people, dark scenes, glasses, fine hair and EXIF rotations; measure foreground preservation and perceptual quality, and never upload user photographs to CI. Record device failure modes without claiming perfect masks.
- Validate v2.7 Portrait Bokeh with representative portraits, difficult hair edges, multi-person images, EXIF rotations, and no-person cases. Fix segmentation mask edge artifacts and device-specific crashes. Keep all face identity and features unchanged.
- Research a properly licensed, actively maintained *on-device* denoise/deblur or super-resolution TFLite/LiteRT model with manageable APK footprint. Integrate only if it can be downloaded legally, run on CPU or GPU on Android 10+, and be tested on synthetic and real images with objective before/after checks. Do NOT relabel a sharpen/unsharp mask filter as generative AI or super-resolution.
- Ensure photo export and preview match, preserve aspect ratio, handle OOM, validate source dimensions, and do not claim recovery of unavailable detail.

**Priority 2 — editor and production**
- Add true undo/redo of editing operations, stable project persistence, more precise touch crop controls, and clear export before/after comparisons.
- Develop multi-track composition and transitions incrementally, only when verified by rendering two real clips.
- Verify arm64 APK on an actual arm64 device, measure installation size and performance, reduce unused resources (R8/resource shrinking when safe). Implement stable signed release signing using repository secrets supplied by the owner; never commit any keystore, token or password.
- Add tests: unit, emulator export integration, real JPEG output validation, 1080p MP4 playback/audio, A/B comparison, and a physical-device QA matrix.
- Publish APK as a GitHub Actions artifact only after all available gates pass. Inspect emulator screenshots and declare anything not tested.

**Required final report every iteration**
1. Commit SHA, branch and GitHub Actions run URL, status and APK download link.
2. Exact completed features, unit-test counts, build status, emulator install and real-media test coverage.
3. Updated 32-module roadmap progress percentage (do not mark merely designed/mock features as done).
4. Remaining blockers, technical risk, no-claim list (not production-ready unless verified).
5. Next prompt with prioritized actions for the following iteration.

Never assert 100% CapCut parity, flawless social upload quality, or actual AI restoration without reproducible tests. Keep offline-first and user privacy.
