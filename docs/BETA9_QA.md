# WFS StoryBoost beta9 — encoder and source-access acceptance

Baseline main `cd8ad45535078f80c79876bdde8adb4c11dca378`; this branch includes beta7 and beta8 without rewriting either draft. [Beta7 exact CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/42726507194) and [beta8 exact CI](https://github.com/wawanwfs/WFS-StoryBoost/actions/runs/43470526311) passed 4/4. Beta8 connected instrumentation passed 17 on API29 and 27 on API35 with 10 skips each. Beta8 source/cancel cases, UI screenshot bounds and synthetic model evidence are documented in [beta8 QA](BETA8_QA.md) and the [beta8 draft](https://github.com/wawanwfs/WFS-StoryBoost/pull/9).

## Candidate behavior

- Full HD retries at lower HD resolution only for video-encoder errors where the planned output is genuinely smaller. Input permission, decoding, I/O and other errors are surfaced without a speculative HD retry. Compare the callback canvas to the requested canvas, never infer 1080p from a selected preset.
- Single and multi-clip encoding check every input again before handing the MP4 to Gallery. Video writers check again immediately around publication and roll back their own output on source denial. This does not assume Android closes already-open descriptors and does not make URI grants and MediaStore writes a single atomic operation.
- New API29 instrumentation distinguishes actual H.264/AAC HD, FHD, an explicit HD fallback and codec failure. Sample timelines are read from compressed MP4 tracks.
- API35 instruments actual 12-second encoding past 85% before cancellation, Quick Enhance encoded video with a pending Gallery row, and provider-UID revocation after nonzero progress. API29/35 both check owner revocation during byte-for-byte MP4 copy, including proof of continued reading via the old descriptor and denial on fresh open. The video test grant is issued by the separate synthetic provider UID using a QA-only shell control. Existing real DocumentsUI tests remain separate and unchanged.

## Still requires independent proof

- ADB access to distinct Android 10 low-RAM and Android 15 physical ARM64 phones, and owner consent for instrumentation. Use the [Windows QA runbook](PHYSICAL_ARM64_QA_WINDOWS.md).
- Owner-authenticated Google Photos URI across force-stop, revocation and reimport. A QA DocumentsProvider does not satisfy this.
- Consented real portrait and low-light sample set, crop/caption pixel validation in both viewports and TalkBack.
- Two independent releases signed by the owner with identical certificate/package and increasing versionCode, plus on-device upgrade with Recent intact. Debug APKs cannot certify this.

No new roadmap module is counted. Coverage remains 23/32 (71.875%). Review the exact-head CI and its artifacts before accepting any new candidate result.
