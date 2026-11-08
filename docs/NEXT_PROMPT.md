# WFS StoryBoost — Next execution prompt after v3.0.0-beta1

Continue from the LATEST main of https://github.com/wawanwfs/WFS-StoryBoost. Read README.md, docs/PROGRESS.md, docs/MODEL_RESEARCH_V29.md, docs/NEXT_PROMPT.md, tests/TEST_PLAN.md, and successful/failing GitHub Actions logs before changing code.

**Phase v3.0 beta2 priorities (in order):**
1. Audit and harden the new StudioDraftStore and editor onPause/autosave/reopen flow. Run real Activity UI tests on API 29 and API 35: import image via SAF, edit colors, ratio, text and AI flags, back, reopen Recent, verify exact preview/export and source immutability. Test revoked/deleted media permissions, corrupt drafts, process death, clear history, and repeated imports. Fix all real bugs; no mock UI claims.
2. Improve UX/performance of DnCNN. Current sigma-25 grayscale model operates on luminance only and sources <=2.6 MP. Benchmark real photos with permission across low-light and chroma noise plus 256px-tile seam edge cases, CPU/RAM/latency. Do not call it AI deblur/upscale. Consider an optional licensed RGB model only after verified CPU fallback and reliable license+SHA hash.
3. Integrate a single genuine on-device AI deblur OR 4x super-resolution model only if model weights are redistributable, Android 10 CPU-compatible, and end-to-end inference demonstrates measurable benefit against paired images. If not, leave disabled and document the blocker.
4. Add end-to-end H.264/AAC MP4 export tests with actual synthetic frames, inspect via ffprobe and/or MediaExtractor, test trim/merge/audio sync and cancellation. Address failure handling before declaring video production ready.
5. Review a real small-screen mobile UI in emulator: Undo/Redo, Touch Crop, caption/safe-zone positioning, vertical tool panel, 24 Color Looks and all 12 ratios. Fix clipped buttons, accidental touches, text and stale AI preview.
6. For release: configure stable Android keystore from owner's private CI secrets (never commit secret/keystore), test signature match and in-place upgrade across TWO builds, produce ARM64 and Universal signed APKs when credentials are available. Debug APK is not a public release.
7. Preserve no-INTERNET-permission, on-device ML Kit + TFLite, 24 Looks, ratios, WA/IG/TikTok presets and original-media privacy.

**Definition of done:** Commit to a non-overwriting branch or fast-forward main, compile Android release/debug as appropriate, pass pure-Java tests, inspect latest GitHub Actions and emulator report, publish only verified APK artifact, report final commit SHA, direct Actions URL, test counts, failed checks, observed benchmarks, and 32-module roadmap progress with honest denominator. Physical-device/real portrait QA remains pending until independently demonstrated.

At the end, provide a follow-up prompt for the next iteration. Do NOT inflate progress merely by improving an already counted module, declare zero compression on social uploads, or claim CapCut parity.
