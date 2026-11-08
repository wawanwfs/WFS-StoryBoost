# WFS StoryBoost v2.8 — AI model feasibility review

Reviewed November 8, 2026. No neural image-restoration weights are bundled in the v2.8 APK. The existing on-device ML Kit face, scene and segmentation models remain real AI functions.

## Denoise candidate: NAFNet-SIDD-width32-LiteRT
- Model card: https://huggingface.co/litert-community/NAFNet-SIDD-width32-LiteRT
- License shown on the model card: MIT; verify exact weight license and retain notices before redistribution.
- FP16 model size approximately **62.5 MB**. Input [1,3,256,256] RGB 0..1 (NCHW), output matching shape; 256px tile is not whole-photo support.
- Pixel 8a GPU benchmark ~46 ms per 256px image, Galaxy S26 hardware support also documented. Model card reports classic XNNPACK cannot delegate this FP16 network on its tested configuration, so it is **not** a drop-in universal CPU solution for all Android 10+ phones.
- Before integration: validate official LiteRT runtime/API on Android 10/low-end chips, GPL/license dependencies, download provenance+SHA256, tiling edges and memory use, benchmark PSNR/SSIM against paired noisy reference images, fallback that does not secretly switch to fake AI, and APK size.

## Super-resolution candidate: TensorFlow ESRGAN Lite sample
- Official sample: https://github.com/tensorflow/examples/tree/master/lite/examples/super_resolution/android
- TensorFlow blog: https://blog.tensorflow.org/2020/12/how-to-generate-super-resolution-images-using-tensorflow-lite-on-android.html
- Sample source code uses Apache-2.0 headers; separately verify the specific model-weights license before shipping.
- Demo converts an ESRGAN model with **50×50 RGB fixed input to 200×200 output** (4×). That demonstration does not directly process 1080×1920 photos; input tiling, image seams, runtime memory and latency need assessment.
- Do not conflate 4× pixel dimensions with guaranteed recovery of real details or identity-preserving output.

## Deblur
- No verified deburring-specific model meeting offline Android 10, license, reproducible quality and APK budget requirements is selected in v2.8. Search candidates and benchmark actual motion/defocus blur samples separately from noise reduction.

## Decision
**Do not ship or enable a model restoration toggle in v2.8.** Prioritize production-safe ML segmentation fixes and actual media-output tests. Keep status unchanged on the 32-module roadmap (69% total). Future v2.9 may ship one verified model with opt-in installation and CPU/device compatibility checks, after obtaining reproducible benchmark evidence and rights to distribute model weights.

## Primary sources
- https://developers.google.com/ml-kit/vision/selfie-segmentation/android
- https://huggingface.co/litert-community/NAFNet-SIDD-width32-LiteRT
- https://github.com/tensorflow/examples/blob/master/lite/examples/super_resolution/android/README.md
