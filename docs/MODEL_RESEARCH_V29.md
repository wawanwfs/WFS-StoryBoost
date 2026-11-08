# WFS StoryBoost v2.9 — On-device DnCNN model card and validation

## Selected model
- Qualcomm DnCNN from https://huggingface.co/qualcomm/DnCNN (MIT tag) / https://aihub.qualcomm.com/models/dncnn
- Original KAIR source weights: https://github.com/cszn/KAIR/releases/download/v1.0/dncnn_25.pth; MIT license https://github.com/cszn/KAIR/blob/master/LICENSE.
- Qualcomm AI Hub DnCNN implementation: https://github.com/qualcomm/ai-hub-models/blob/main/src/qai_hub_models/models/dncnn/model.py (code BSD-3-Clause). Float **TFLite export** from pinned v0.65.0 Qualcomm model artifact:
  https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/dncnn/releases/v0.65.0/dncnn-tflite-float.zip
- SHA256 of extracted `.tflite`: `59bc280342afd08357282232784d135c29a4ec64cd61b5172921e4d3d881d9fd`. This checksum is pinned in `scripts/fetch-dncnn-model.sh`; a mismatch fails the build.
- Model: 17 conv layers, 555k parameters, float model ~2.12 MB. Input grayscale [1,256,256,1] float32 normalized [0,1], output denoised luminance of same dimensions. Primarily trained for Gaussian sigma 25. No claim of RGB noise removal; preserve RGB chroma by a shared, bounded luminance correction.
- Runtime: org.tensorflow:tensorflow-lite:2.17.0 CPU to cover Android 10+ without GPU/NPU requirement. Hard limit 2.6 MP and 120 neural tiles; larger images fail with an informative message, not hidden upscale or downscale.
- Security: weights are embedded at CI build time (network used only to download public model at build). All user images stay on device. No INTERNET permission or external uploads.

## Evidence checklist (must be filled from run logs)
- [x] Download artifact SHA verified and pinned
- [x] TFLite model validated and packaged in ARM64/Universal (v2.9 CI build); verify on final pinned run
- [x] Real Android emulator neural invocation confirms model tensor dims (256x256 float32)
- [x] CI computes PSNR before and after using deterministic Gaussian noise and checks improvement (20.176 dB → 28.493 dB on v2.9 initial run)
- [x] CI computes global SSIM before and after and checks improvement (0.7764 → 0.9591 on synthetic sample)
- [ ] Physical Android 10 low-RAM / Android 15 ARM64 phone performance
- [ ] Real photo consented portrait / hair / low light, non-Gaussian noise and video checks

## Other models intentionally NOT shipped
- NAFNet LiteRT FP16 ~62.5 MB: GPU/NPU-specific, not proven CPU universal.
- TensorFlow ESRGAN: requires separate weight license verification, 4× tiled implementation and measured seams/memory.
- Deblur: no independently validated Android 10 compatible model selected.

## Third-party rights
MIT KAIR: Copyright (c) 2019 Kai Zhang. License https://github.com/cszn/KAIR/blob/master/LICENSE
Qualcomm exporter implementation SPDX-License-Identifier: BSD-3-Clause, Copyright (c) 2025 Qualcomm Technologies.
The source and output model documentation retain upstream attribution. The build's binary model asset is not committed directly to source control.

## Beta3 measured multi-tile TFLite follow-up (synthetic only)

Synthetic real TFLite DnCNN benchmark on Android 15 x86_64 emulator: 480×480 RGB with paired clean/noisy fixture, **four 256px overlapping tiles**, CPU inference **10.783 s**, full pipeline **12.028 s**, PSNR **20.307→28.801 dB**, SSIM **0.293→0.745**, process PSS snapshots **48,144→53,274 KB**. The RGB chroma-difference guard and overlap-boundary checks passed. [Green AI CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42501208561). Not a portrait, low-light camera, physical ARM64, peak-RAM or overall IQA benchmark; no new AI weights or models activated.

The synthetic fixture is paired clean/noisy RGB, so PSNR/SSIM apply *only* to this test. For real portraits, chroma/compression noise or low-light without a clean ground truth, do not publish PSNR/SSIM as objective quality evidence. Retain DnCNN's 2.6 MP cap and state clearly: this is grayscale-sigma25 luminance denoise, not AI Deblur or Super Resolution.

### Beta3 API29 emulator memory outlier (requires investigation)

The [beta3 combined API29/API35 CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42502015377) shows a **memory-usage outlier** on Android 10 x86_64 emulator: DnCNN 480×480 four-tile synthetic image CPU inference **14.249 s**, total **15.573 s**, process PSS snapshots **41,790→365,138 KB**. In the same CI run Android 15 showed inference **14.805 s**, total **15.946 s**, PSS **48,004→41,105 KB**. These are process snapshots, not device peak RAM or a statistically representative benchmark; the cause of API29's high PSS remains unconfirmed. Do not certify low-RAM phones.

Next: repeat 5+ fresh-process runs with post-GC and separate Java/native/graphics PSS, track peak memory while iterating tiles, compare 1 vs 2/3 TFLite CPU threads, and test on real Android 10 low-RAM ARM64 phones. Never infer a production RAM minimum from emulator-only snapshots.


### Beta4 emulator diagnostic candidate

The qa-dncnn-memory.sh script executes the pinned real DnCNN four-tile RGB fixture twice with different Android process IDs, collecting sampled live PSS, Java/native/graphics categories, native allocated heap, CPU inference duration and thread counts. These are emulator process measurements, not device-level peak RAM. No beta4 numeric memory result is asserted until its exact GitHub CI artifact is inspected. Repeat 5+ independent Android10 processes and compare controlled TFLite thread counts and physical low-RAM ARM64 before treating the prior API29 365,138 KB outlier as explained. Do not silently lower neural output resolution or weights to make memory appear better.
