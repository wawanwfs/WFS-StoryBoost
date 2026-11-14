# WFS StoryBoost — Next Execution Prompt v3.0 beta12

Begin with current main and reconcile the open beta7–beta11 drafts without force-pushing or merging unverified changes. Read [beta11 QA](BETA11_QA.md), exact GitHub Actions artifacts, and the [test plan](../tests/TEST_PLAN.md). Preserve original bytes, app package, Recent, 24 Color Looks and pinned DnCNN.

1. Verify beta11 decoded video caption proof, then add wrap and safe-zone boundary cases for the other social profiles, nonstandard aspect ratios and silent clips. Keep caption-free and passthrough cases, audio/sample PTS, cancellation and provider revocation green.
2. With owner consent, test real portrait and low-light images entirely on-device. Compare crop and face details across source, preview and export, with per-sample bounds and original SHA. Do not add personal images or identifying output to Git or CI.
3. Run the Windows QA procedure on owner-operated low-RAM Android 10 and Android 15 ARM64, record sampled memory/CPU/thermal/battery/LMK, manually check TalkBack and a real Google Photos URI across process death, revocation and reimport.
4. On a spare handset with the owner signing setup, verify two independently signed release APKs with the same package and cert and increasing versionCode. Install over the old release without deleting its data and verify authorized Recent still opens. Never request or publish signing secrets.

Report commit, PR and exact CI results, artifact links, verified module count and blockers. Keep 23/32 unless a genuinely new module is verified.
