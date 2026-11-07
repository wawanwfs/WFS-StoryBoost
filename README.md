# WFS StoryBoost 📱

**Aplikasi Android offline-first untuk mempercantik foto/video dan menyiapkan hasil untuk WhatsApp Story, Instagram, TikTok, maupun konten landscape.**

[![Build APK](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml/badge.svg)](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml)

> **v1.6.0 • source development build.** CI menghasilkan **APK debug** jika workflow berhasil. Peningkatan warna dan ketajaman berbasis filter (bukan AI generatif). WhatsApp tetap dapat mengompresi Status, jadi kualitas media setelah unggah tidak bisa dijamin identik dengan berkas ekspor.

## ✨ v1.7.0: Logo & instalasi

- Launcher icon **WFS StoryBoost** yang baru (kamera + play + orbit biru-ungu), adaptive icon untuk Android 10–16, plus logo SVG dalam `assets/`.
- **Application ID baru:** `com.wfs.storyboost.studio` untuk menghindari konflik update aplikasi debug lama `com.wfs.storyboost`. Versi lama dan baru dapat terpasang bersamaan.
- GitHub CI mengecek signature dengan `apksigner`, file ZIP dengan `unzip -t`, alignment dengan `zipalign`, nama package, label dan aset ikon.
- CI menjalankan **emulator Android 35** untuk smoke test instalasi dan launch.
- Dukungan *stable signed release* apabila empat GitHub Actions secrets signing telah disiapkan. Tanpa secrets, yang tersedia hanya debug APK yang **tidak menjamin update tanpa uninstall**.

[Lihat panduan instalasi dan signing](docs/INSTALL.md).

## 🌟 Fitur

| Fitur | Detail |
|---|---|
| 💎 Aesthetic Enhance | Natural, Cinematic, Vibrant, Soft Night untuk foto dan video |
| 🎯 Fokus crop | Slider kiri/kanan dan atas/bawah untuk foto & video pada mode Crop |
| ↔️ Rasio dua arah | **9:16 → 16:9** dan **16:9 → 9:16**, juga 1:1, 4:5, Original. Tombol satu ketuk menukar rasio |
| 🖼️ Bingkai | **Fit**, **Crop**, **Blur Background** untuk foto dan video dengan GPU |
| 🔍 Before / After | Geser pembatas pada foto; pemutar video asli vs hasil setelah ekspor |
| 🎥 Video Optimizer | MP4 H.264/AAC, bitrate FPS track, validasi audio/durasi, fallback Full HD→HD |
| 📦 Batch Processing | Pilih hingga 10 file, opsi ekspor 9:16 dan 16:9 sekaligus (20 output) |
| 📤 Ekspor | MediaStore: `Pictures/WFS StoryBoost` atau `Movies/WFS StoryBoost` |
| ✂️ Trim video | Potong awal/akhir video dengan waktu detik |
| 🔒 Privacy | Tanpa login, izin storage menyeluruh, atau permintaan akses internet |

### Ukuran ekspor

| Rasio | Full HD | HD |
|---|---|---|
| 9:16 | 1080×1920 | 720×1280 |
| 16:9 | 1920×1080 | 1280×720 |
| 1:1 | 1080×1080 | 720×720 |
| 4:5 | 1080×1350 | 720×900 |
| Original | maksimal sisi panjang 1920, tidak upscale | maksimal sisi panjang 1280, tidak upscale |

Konversi rasio **tidak memutar** isi media. **Fit** menjaga seluruh foto/video dengan tepi, sedangkan **Crop** memotong area atas/bawah atau kiri/kanan. Untuk foto, **Blur** mengisi tepi dengan latar kabur. Video Blur dinamis diimplementasikan menggunakan GaussianBlurWithFrameOverlaid dan perlu pengujian visual di GPU perangkat nyata.

## 📥 Download APK (via GitHub Actions)

1. Buka tab [**Actions**](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml).
2. Pilih workflow **Build WFS StoryBoost APK**.
3. Buka run terbaru dengan status **Success** (atau klik **Run workflow** jika diperlukan).
4. Unduh artifact **WFS-StoryBoost-v1.7.0-Debug-APK** dan ekstrak ZIP-nya.
5. Instal `app-debug.apk` di Android 10 atau versi lebih baru (aktifkan izin pemasangan aplikasi dari sumber tersebut saat diminta).

> Artifact tersedia **hanya setelah build sukses**. Debug APK ditandatangani kunci debug sementara; untuk update stabil gunakan signed release dengan GitHub Secrets. v1.7 memakai package baru untuk menghindari konflik dengan versi lama. Jangan mengunggah kunci signing atau password ke repository.

## 🧰 Build lokal Windows 10/11

Syarat: **JDK 17**, **Android SDK API 36 + Build Tools 35**, Android Studio atau SDK cmdline tools, dan koneksi untuk Gradle/dependensi.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
```

Jika sukses, APK debug berada pada `dist/WFS-StoryBoost-v1.7.0-debug.apk`.

## ✅ Pengujian

Uji perhitungan tanpa Android SDK:

```bash
bash scripts/test-ratio-geometry.sh
```

Output yang diharapkan: `PASS 43 aspect geometry assertions`.

**Workflow CI:** menjalankan pengujian rasio dan bitrate, kompilasi Android debug APK, lalu mengunggah artifact. CI menambahkan smoke test instalasi pada emulator, namun belum menggantikan pengujian aktual pada ponsel dan verifikasi visual/AV video yang diekspor. Ikuti [`tests/TEST_PLAN.md`](tests/TEST_PLAN.md) untuk QA manual, khususnya video landscape↔portrait, HEIC, perangkat resolusi kecil, video dengan/tanpa audio, serta pembatalan antrean.

## 📁 Struktur proyek

```text
app/src/main/java/com/wfs/storyboost/
  MainActivity.java          # UI, picker, queue, progress, share
  PhotoEngine.java           # perbaikan foto, framing, before/after
  BeforeAfterView.java       # interaksi pembatas preview
  VideoEngine.java           # Media3 Transformer
  VideoBitrate.java          # estimator bitrate AVC (tested)
  AspectRatioProfile.java    # rasio output (tested)
  FrameMath.java             # fit/crop (tested)
  ExportOptions.java         # snapshot pengaturan batch
  MediaSaver.java            # ekspor ke galeri via MediaStore
.github/workflows/build-apk.yml
scripts/test-ratio-geometry.sh
scripts/build-windows.ps1
tests/AspectRatioProfileTest.java
tests/TEST_PLAN.md
```

## ⚠️ Audit batasan v1.6.0

| Batasan | Status |
|---|---|
| Blur video bergerak | Sudah ditambahkan; masih perlu QA visual berbagai perangkat |
| Preview video asli/hasil | Sudah ada player di aplikasi setelah ekspor |
| FPS metadata | Dibaca dari track via MediaExtractor; 30 FPS hanya fallback |
| Track audio hilang | Divalidasi keberadaan audio serta perbedaan durasi (belum sample-accurate) |
| Multi-aspect sekaligus | 9:16 dan 16:9 dalam satu batch |
| Trim video | Input waktu mulai/akhir dalam detik |
| Encoder Full HD gagal | Mencoba sekali lagi pada HD, tidak menjamin seluruh decoder vendor |
| AI super-resolution, AI deblur, face restoration | Belum: membutuhkan model ML berlisensi serta evaluasi performa dan identitas wajah |
| Crop manual berbasis fokus | Slider X/Y bekerja untuk foto dan video pada mode Crop |
| Pelacakan subjek otomatis dan timeline multi-track | Belum; membutuhkan modul analisis dan editor lanjutan |
| Proses saat aplikasi ditutup | Belum; butuh foreground service media-processing yang sesuai aturan Android |
| HDR dan format vendor | Masih tergantung dukungan decoder/encoder perangkat |
| Play Store | APK debug saja; belum signing release dan QA produksi |
| Status WhatsApp/IG | Kompresi setelah unggah di luar kontrol aplikasi |

Fitur bertanda *belum* tetap dicantumkan secara transparan dan tidak diklaim selesai.

## 🔧 Teknologi

Android Java 17, minSdk 29 (Android 10), target SDK 35 / compile SDK 36; Android Gradle Plugin 8.11.1, Gradle 8.13, AndroidX Media3 1.11.1, MediaStore scoped storage. Proses tetap lokal di Android.

© WFS StoryBoost. Hak cipta dan lisensi mengikuti pemilik repository; tidak ada lisensi open-source yang diasumsikan atau ditambahkan secara otomatis.