# WFS StoryBoost

**Android app — one-tap aesthetic enhancement + WhatsApp Story export for photos and videos.**

The project is implemented in **Java** with native Android UI. Photo enhancement uses native Bitmap processing and video processing uses **AndroidX Media3 Transformer 1.11.1** with the phone's media codecs. There is **no cloud processing**, account login, or INTERNET permission.

> **Build status:** Source is complete as a buildable Android Gradle project, but **the APK has not been compiled or tested on a device in this delivery environment** (Android SDK and online dependencies are unavailable there). Use the GitHub Actions workflow or Android build instructions below to produce an installable debug APK. Do not mistake the project ZIP for a compiled APK.

## Features

| Feature | Photo | Video |
|---|---|---|
| Automatically processes selected file(s) | Yes | Yes |
| Natural / Cinematic / Vibrant looks | Yes | Yes |
| Story aspect ratio 9:16 | Yes | Yes |
| Fit (no crop) or crop to fill | Yes | Yes |
| Full HD 1080×1920 or HD 720×1280 | Yes | Yes |
| Output format | JPEG Q95 | MP4 / H.264 video + AAC audio |
| Image sharpening | Light unsharp | Not implemented (color enhancement only) |
| Video audio preserved | N/A | Yes, AAC transcoded when necessary |
| Gallery output | Pictures/WFS StoryBoost | Movies/WFS StoryBoost |
| Privacy | Local | Local |

- Up to 10 media files per selection, processed sequentially.
- Processing starts automatically when files are selected.
- Shares the most recent output using the Android share chooser.
- Android 10 (API 29) or newer; tested build of this delivery **not available**.
- If the input is too small, upscaling does **not** create genuine new detail. The app does not offer AI generative object editing, face reconstruction or super-resolution.
- When Fit is selected for photos, the whole photo is presented with a soft backdrop; for videos, letter/pillarboxing is used.
- App must remain open during export. No foreground background service in v1.

## Build option A: GitHub Actions (recommended, no Android Studio required)

1. Create a **new empty repository** in GitHub (e.g. `WFS-StoryBoost`).
2. Upload **the files and folders inside this ZIP** to the repository root, preserving `.github/workflows/build-apk.yml`. Do **not** upload the ZIP as a single file.
3. If the default branch is `main` or `master`, the build workflow launches on push. You can also go to **Actions → Build WFS StoryBoost APK → Run workflow**.
4. When the build succeeds, open the workflow run → **Artifacts** → **WFS-StoryBoost-APK**.
5. Download the artifact ZIP and extract `app-debug.apk`, then install it on your Android device. Android will ask permission to install an APK from the selected file manager/browser.
6. This is a **debug-signed APK** for personal installation; distributing via Play Store requires a signed **release** variant and further testing.

Note: GitHub Actions must be enabled for the repository. If a job fails, inspect the build log instead of assuming the APK was created.

## Build option B: Windows 10/11 + Android Studio

1. Install Android Studio and in **SDK Manager**, install **Android SDK Platform 35** and **Build Tools 35**. JDK 17 is required (Android Studio bundles an appropriate JBR in recent releases).
2. Extract the ZIP.
3. In PowerShell, from the project root run:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
   ```

4. The helper downloads Gradle 8.11.1 (internet needed once), then runs `:app:assembleDebug`.
5. The built APK will be at `dist\WFS-StoryBoost-v1.0.0-debug.apk`. It is signed with the standard Android debug certificate.

You can also open the project in Android Studio, configure Gradle 8.11.1/JDK 17, Sync, and run `:app:assembleDebug`.

## Use on your Android phone

1. Install the APK.
2. Open **WFS StoryBoost**.
3. Select **Natural**, **Cinematic** or **Vibrant**.
4. Choose **Fit** (whole frame) or **Crop** (fill Story screen), and **Full HD** or **HD**.
5. Tap **Pilih foto / video**, then select up to 10 items.
6. Keep the app open until the process finishes. Find outputs in your Gallery under **WFS StoryBoost**.
7. Choose **Bagikan hasil terakhir** or use the saved file from the Gallery to post your WhatsApp Status / Instagram Story.

## Limitations and quality facts

- WhatsApp compresses uploaded Status media and changes its settings server-side. **No third-party app can guarantee zero loss or zero compression** after upload.
- The app saves to a Story-compatible 9:16 frame and avoids excessive re-encoding before sharing.
- H.264 hardware encoder and Media3 support vary across Android vendors. Some 1080x1920 video exports may fall back to other supported configurations or fail on particular devices; try HD 720×1280 when necessary.
- HDR content and unusual formats can be device-dependent. Video output is best verified by inspecting the exported file's dimensions/audio and watching the full clip.
- A small input, heavy motion blur, and low bitrates cannot be magically restored to sharp HD by pixel sharpening.
- v1 processes a queue in the foreground, one at a time. It does not divide longer videos into WhatsApp Status segments.
- Photo EXIF GPS and camera metadata are **not intentionally preserved** in the exported JPEG. Source files are never modified.

## Technical notes

- App ID: `com.wfs.storyboost`
- Min SDK 29 / Target SDK 35
- Gradle: 8.11.1 / Android Gradle Plugin: 8.9.2 / Java 17
- Media3: 1.11.1 for Transformer, Effect, Common
- Outputs written via Android MediaStore scoped storage and made visible after completion.
- No storage permission or INTERNET permission; the Android document picker temporarily grants file read access.

## Project layout

```text
WFS-StoryBoost/
├── .github/workflows/build-apk.yml
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/wfs/storyboost/
│       │   ├── MainActivity.java
│       │   ├── PhotoEngine.java
│       │   ├── VideoEngine.java
│       │   └── MediaSaver.java
│       └── res/...
├── scripts/build-windows.ps1
├── build.gradle
├── settings.gradle
└── gradle.properties
```
