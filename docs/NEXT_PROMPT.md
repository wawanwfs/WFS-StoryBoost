# WFS StoryBoost — Next Execution Prompt v3.0 beta10

Continue from the newest verified main at https://github.com/wawanwfs/WFS-StoryBoost and reconcile the unmerged beta7–beta9 drafts before starting. Read [beta9 QA](BETA9_QA.md), [test plan](../tests/TEST_PLAN.md), and all exact-head CI evidence. Never force push or merge a failed or unverified required gate. Preserve source bytes, app package, Recent, 24 Color Looks, and the pinned DnCNN.

1. Confirm beta9 exact-head run, Android 10's actual encoder outcome, and API35 late cancellation/owner revocation. Close any platform codec or UI coverage gaps without classifying passthrough or a rejected encode as HD/FHD output. Add API29 Quick Enhance and Merge Studio outcomes where encoders are supported.
2. Run the [physical QA procedure](PHYSICAL_ARM64_QA_WINDOWS.md) on owner-operated Android 10 low-RAM and Android 15 ARM64 devices, with sampled PSS, CPU, battery, thermal, cancel and LMK records. Verify real owner-selected Google Photos URI persistence/revocation/reimport separately.
3. Use only rights-cleared consented portraits and low-light material. Prove crop focus and caption safe-zone pixel placement and TalkBack controls at 320×640/160dpi and 1080×2400/440dpi. Preserve original SHA and portrait identity.
4. With owner signing already configured, independently build two releases, verify same cert/package with ascending versionCode, and perform adb install -r with Recent and its authorized source intact. Do not upload secrets or treat debug APKs as production.

Report exact baseline, head, CI, test counts, artifacts, emulator vs physical evidence, blockers, and a unique next prompt. Keep roadmap at 23/32 unless another feature is fully implemented and verified.
