# WFS StoryBoost — Next execution prompt (after v3.0.0-beta2)

Continue from verified latest main in https://github.com/wawanwfs/WFS-StoryBoost. Inspect GitHub Actions for API29 and API35 UI instrumentation and API35 real MP4 exporter, README, PROGRESS, QA reports, and MODEL_RESEARCH_V29.

1. **Full SAF end-to-end instrumentation**: automate the actual Android DocumentsUI file picker and persisted-URI permission grant on API29/35; test URI revoke/restore and real process termination/relaunch across independent test processes. Current beta2 UI test simulates SAF result with a real MediaStore content URI, not OS picker taps.
2. **Production video matrix**: inspect real H.264/AAC encoder artifacts, expand MP4 tests to rotation, video-only sources, VFR/60fps, mixed codec streams, audio alignment, cancellation under memory pressure, and at least one real Android 10 ARM64 phone. Fix engine errors and quantify 1080p runtime and memory. Do not claim unsupported codec handling when untested.
3. **Real portrait and DnCNN review**: obtain user-consented or openly licensed imagery with reproducible provenance, paired clean/low-light samples when PSNR/SSIM are needed, analyze hair/glasses/multi-person bokeh and denoise tile seam artifacts; never commit identifiable private photos. DnCNN remains Gaussian sigma25 luminance denoise, not AI deblur/upscale.
4. **Editor polish**: verify crop focal transformations and hitboxes on both 320×640 and 1080×2400 screenshots with espresso/UIAutomator, focus accessibility, composition and before-after UI, plus test Undo/Redo snapshots across editor state.
5. **Secure releases**: only if owner configures private STORYBOOST_* GitHub secrets, test two successive release signers with apksigner certificate SHA256 equality and an actual adb install -r upgrade. Never add keystore/password to Git.
6. Keep 24 Color Looks, 12 ratios, WA/IG/TikTok presets, ML Kit portrait bokeh, offline DnCNN and original-media privacy.

Deliver verified commit SHA, run URLs and artifact links, failed gates, JVM/instrumented/emulator/physical results, actual model/media benchmarks, progress of the unchanged 32-module roadmap, and next prompt. Work on branch, fast-forward main only after all mandatory checks pass. If a gate is unsupported, keep it pending rather than claiming success.
