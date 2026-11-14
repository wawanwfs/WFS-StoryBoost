# WFS StoryBoost — Next Execution Prompt v3.0 beta11

Start from the newest verified main in https://github.com/wawanwfs/WFS-StoryBoost and reconcile all open beta7–beta10 drafts. Read [beta10 QA](BETA10_QA.md), the exact CI logs and the [test plan](../tests/TEST_PLAN.md). Preserve package, source SHA, Recent, 24 Color Looks and pinned DnCNN. No force push or merge of failed/unverified gates.

1. Verify beta10 API29 Gallery outputs and both viewport pixel logs. Extend the photo proof to owner-consented portraits and low-light inputs. Measure preservation of face details without pretending that synthetic colors are human or ML quality evidence.
2. Bring actual encoded video caption placement to parity with the selected safe zone, including custom/social profiles and both rotations. Prove result with decoded video frames rather than a screenshot of the source preview. Keep existing 1080p, MediaStore cancellation, sample PTS and revoked-grant cases.
3. Run physical QA on owner-operated API29 low-RAM and API35 ARM64 phones. Use the Windows runbook and record memory/CPU/thermal/battery/LMK, plus manual TalkBack. Confirm real Google Photos grants and Recent separately.
4. Only with the owner's existing signing setup and a spare test handset, compare two independent release APK certificates/package and verify rising versionCode, then perform install-over-upgrade without removing app data or losing authorized Recent. Never publish or request signing secrets.

Report exact baseline/head, CI, test totals and artifact links, what was proven only in emulation and remaining blockers. Maintain 23/32 until a genuinely new module is verified.
