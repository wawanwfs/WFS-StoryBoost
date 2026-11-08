# WFS StoryBoost v2.9 — On-device DnCNN model card and validation

## Selected model
- Qualcomm DnCNN from https://huggingface.co/qualcomm/DnCNN (MIT tag) / https://aihub.qualcomm.com/models/dncnn
- Original KAIR source weights: https://github.com/cszn/KAIR/releases/download/v1.0/dncnn_25.pth; MIT license https://github.com/cszn/KAIR/blob/master/LICENSE.
- Qualcomm AI Hub DnCNN implementation: https://github.com/qualcomm/ai-hub-models/blob/main/src/qai_hub_models/models/dncnn/model.py (code BSD-3-Clause). Float **TFLite export** from pinned v0.65.0 Qualcomm model artifact:
  https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/dncnn/releases/v0.65.0/dncnn-tflite-float.zip
- SHA256 of extracted `.tflite`: **to be recorded from GitHub CI log and pinned in fetch script**.
- Model: 17 conv layers, 555k parameters, float model ~2.12 MB. Input grayscale [1,256,256,1] float32 normalized [0,1], output denoised luminance of same dimensions. Primarily trained for Gaussian sigma 25. No claim of RGB noise removal; preserve RGB chroma by a shared, bounded luminance correction.
- Runtime: org.tensorflow:tensorflow-lite:2.17.0 CPU to cover Android 10+ without GPU/NPU requirement. Hard limit 2.6 MP and 120 neural tiles; larger images fail with an informative message, not hidden upscale or downscale.
- Security: weights are embedded at CI build time (network used only to download public model at build). All user images stay on device. No INTERNET permission or external uploads.

## Evidence checklist (must be filled from run logs)
- [ ] Download artifact SHA verified and pinned
- [ ] TFLite model validated and packaged in ARM64/Universal
- [ ] Real Android emulator neural invocation confirms model tensor dims
- [ ] CI computes PSNR before and after using deterministic Gaussian noise and checks improvement
- [ ] CI computes SSIM before and after and checks improvement
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
