# WFS StoryBoost 📱

**Aplikasi Android offline-first untuk mempercantik foto/video dan menyiapkan hasil untuk WhatsApp Story, Instagram, TikTok, maupun konten landscape.**

[![Build APK](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml/badge.svg)](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml)

> **v1.4.0 • source development build.** CI menghasilkan **APK debug** jika workflow berhasil. Peningkatan warna dan ketajaman berbasis filter (bukan AI generatif). WhatsApp tetap dapat mengompresi Status, jadi kualitas media setelah unggah tidak bisa dijamin identik dengan berkas ekspor.

## 🌟 Fitur

| Fitur | Detail |
|---|---|
| 💎 Aesthetic Enhance | Natural, Cinematic, Vibrant, Soft Night untuk foto dan video |
| ↔️ Rasio dua arah | **9:16 → 16:9** dan **16:9 → 9:16**, juga 1:1, 4:5, Original. Tombol satu ketuk menukar rasio |
| 🖼️ Bingkai | **Fit**, **Crop**, **Blur Background** (blur untuk foto; video menggunakan Fit) |
| 🔍 Before / After | **Geser pembatas** pada pratinjau foto asli vs editan setelah ekspor |
| 🎥 Video Optimizer | MP4 H.264/AAC, bitrate adaptif resolusi dan perkiraan FPS, pemeriksaan struktur MP4 hasil ekspor |
| 📦 Batch Processing | Pilih hingga 10 file dan proses secara berurutan |
| 📤 Ekspor | MediaStore: `Pictures/WFS StoryBoost` atau `Movies/WFS StoryBoost` |
| 🔒 Privacy | Tanpa login, izin storage menyeluruh, atau permintaan akses internet |

### Ukuran ekspor

| Rasio | Full HD | HD |
|---|---|---|
| 9:16 | 1080×1920 | 720×1280 |
| 16:9 | 1920×1080 | 1280×720 |
| 1:1 | 1080×1080 | 720×720 |
| 4:5 | 1080×1350 | 720×900 |
| Original | maksimal sisi panjang 1920, tidak upscale | maksimal sisi panjang 1280, tidak upscale |

Konversi rasio **tidak memutar** isi media. **Fit** menjaga seluruh foto/video dengan tepi, sedangkan **Crop** memotong area atas/bawah atau kiri/kanan. Untuk foto, **Blur** mengisi tepi dengan latar kabur. Video Blur dinamis belum tersedia; aplikasi jelas menggunakan Fit, bukan diam-diam mengubah framing.

## 📥 Download APK (via GitHub Actions)

1. Buka tab [**Actions**](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml).
2. Pilih workflow **Build WFS StoryBoost APK**.
3. Buka run terbaru dengan status **Success** (atau klik **Run workflow** jika diperlukan).
4. Unduh artifact **WFS-StoryBoost-v1.4.0-APK** dan ekstrak ZIP-nya.
5. Instal `app-debug.apk` di Android 10 atau versi lebih baru (aktifkan izin pemasangan aplikasi dari sumber tersebut saat diminta).

> Artifact tersedia **hanya setelah build sukses**. Debug APK ditandatangani kunci debug dan belum siap distribusi Google Play. Jangan mengunggah kunci signing atau password ke repository.

## 🧰 Build lokal Windows 10/11

Syarat: **JDK 17**, **Android SDK API 36 + Build Tools 35**, Android Studio atau SDK cmdline tools, dan koneksi untuk Gradle/dependensi.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
```

Jika sukses, APK debug berada pada `dist/WFS-StoryBoost-v1.4.0-debug.apk`.

## ✅ Pengujian

Uji perhitungan tanpa Android SDK:

```bash
bash scripts/test-ratio-geometry.sh
```

Output yang diharapkan: `PASS 27 aspect geometry assertions`.

**Workflow CI:** menjalankan pengujian rasio dan bitrate, kompilasi Android debug APK, lalu mengunggah artifact. Ini belum menggantikan pengujian aktual pada ponsel dan verifikasi visual/AV video yang diekspor. Ikuti [`tests/TEST_PLAN.md`](tests/TEST_PLAN.md) untuk QA manual, khususnya video landscape↔portrait, HEIC, perangkat resolusi kecil, video dengan/tanpa audio, serta pembatalan antrean.

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

## ⚠️ Batasan jujur

- AI super-resolution, true AI deblur, restorasi wajah, analisis subjek, serta video blur dinamis **belum diimplementasikan**.
- Before/after saat ini untuk **foto**; belum ada player preview video di dalam aplikasi.
- Frame-rate metadata tidak selalu tersedia; jika tidak ada, bitrate memakai asumsi 30 fps.
- Pemeriksaan video memastikan file, resolusi dan durasi valid, tetapi belum menjamin audio sinkron; perlu QA perangkat.
- Frame rate dan HDR bergantung decoder/encoder perangkat. Media yang tidak didukung dapat gagal diekspor, dengan pesan error.
- UI saat ini dibuat secara programatik; belum memiliki layar timeline, crop manual, atau ekspor multi-aspect sekaligus.
- Antrean harus dijalankan saat aplikasi tetap aktif (belum foreground service/background worker).
- Untuk video, perangkat mungkin menolak codec, resolusi, atau kombinasi input tertentu; uji pada perangkat nyata.

## 🔧 Teknologi

Android Java 17, minSdk 29 (Android 10), target SDK 35 / compile SDK 36; Android Gradle Plugin 8.11.1, Gradle 8.13, AndroidX Media3 1.11.1, MediaStore scoped storage. Proses tetap lokal di Android.

© WFS StoryBoost. Hak cipta dan lisensi mengikuti pemilik repository; tidak ada lisensi open-source yang diasumsikan atau ditambahkan secara otomatis.