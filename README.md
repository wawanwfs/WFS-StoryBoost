# ✦ WFS StoryBoost — Creative Media Studio

**Aplikasi Android offline-first untuk mempercantik foto/video dan menyiapkan hasil untuk WhatsApp Story, Instagram, TikTok, maupun konten landscape.**

[![Build APK](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml/badge.svg)](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml)

> **v2.1.0 • Premium Studio UI + Source-Aware Quality Engine.** CI menghasilkan **APK debug** jika workflow berhasil. Peningkatan warna dan ketajaman berbasis filter (bukan AI generatif). WhatsApp tetap dapat mengompresi Status, jadi kualitas media setelah unggah tidak bisa dijamin identik dengan berkas ekspor.

## ✨ Baru di v2.1.0
- **Premium Visual Hero** memakai panorama matahari terbenam dan ilustrasi kota pada Canvas Android, tidak bergantung koneksi internet atau poster AI.
- **Quick Studio 6 menu**: Enhance Foto, Enhance Video, Ubah Rasio, Batch Export, Filter Premium, Lihat Hasil.
- **Preset chip tanpa radio circle**; navigasi editor, hasil dan pilihan rasio tetap aktif.
- **Quality Advisor** menghitung piksel asli yang tersisa ketika crop; dialog memberi pilihan lanjut crop atau beralih Blur sebelum kehilangan detail.
- **Original / No Edit** untuk menjaga sumber tanpa filter. Pada JPEG asli dan MP4 asli dengan rasio Original, Fit, Max Detail, tanpa trim: berkas disalin persis tanpa *lossy re-encode*.
- **Max Detail** tetap default; batas nyata resolusi dan kualitas sumber dijelaskan. Tidak ada klaim AI upscale palsu.



**Premium Studio UI** kini memiliki header berlogo, hero gradien, kartu cepat Enhance Foto/Video, tombol Ubah Rasio, batch processing, panel editor dengan preset warna, kontrol crop, panel ekspor, dan navigasi Beranda–Editor–Hasil yang interaktif.

**Quality Engine**:
- **HD**: preset hemat data.
- **Full HD**: kanvas 1080×1920 atau orientasi lain, H.264 adaptif sampai 22 Mbps.
- **Max Detail**: mempertahankan resolusi sumber hingga batas 4K untuk sumber yang mencukupi; bitrate AVC dapat mencapai 60 Mbps tergantung resolusi dan FPS.
- Pada mode **Crop + Max Detail**, ukuran output dapat lebih kecil daripada 4K agar pemotongan tidak memperbesar piksel dari sumber.
- Foto diekspor JPEG kualitas 98 dan filter sharpen kasar dihilangkan guna mencegah halo, ringing, serta lonjakan memori.
- Tidak ada AI upscaling sungguhan; resolusi tidak sama dengan detail baru yang diciptakan.
- **Sumber gambar berkualitas rendah tidak akan menjadi tajam sempurna hanya dengan ekspor ulang.**

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
| 💎 Aesthetic Enhance | Natural, Cinematic, Vibrant, Soft Night, dan Asli / No Edit |
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

## 🧪 Bukti pengujian UI & instalasi

Workflow membangun APK, memvalidasi signature dan launcher, memasang APK di Android emulator API 35, meluncurkan MainActivity, lalu merekam **screenshot aktual home app** sebagai artifact `WFS-StoryBoost-v2.1.0-Emulator-Screenshot`. Screenshot adalah UI aplikasi nyata, bukan poster AI.

## 📥 Download APK (via GitHub Actions)

1. Buka tab [**Actions**](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml).
2. Pilih workflow **Build and verify WFS StoryBoost APK**.
3. Buka run terbaru dengan status **Success** (atau klik **Run workflow** jika diperlukan).
4. Unduh artifact **WFS-StoryBoost-v2.1.0-Debug-APK** dan ekstrak ZIP-nya.
5. Instal `app-debug.apk` di Android 10 atau versi lebih baru (aktifkan izin pemasangan aplikasi dari sumber tersebut saat diminta).

> Artifact tersedia **hanya setelah build sukses**. Debug APK ditandatangani kunci debug sementara; untuk update stabil gunakan signed release dengan GitHub Secrets. v1.7 memakai package baru untuk menghindari konflik dengan versi lama. Jangan mengunggah kunci signing atau password ke repository.

## 🧰 Build lokal Windows 10/11

Syarat: **JDK 17**, **Android SDK API 36 + Build Tools 35**, Android Studio atau SDK cmdline tools, dan koneksi untuk Gradle/dependensi.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
```

Jika sukses, APK debug berada pada `dist/WFS-StoryBoost-v2.1.0-debug.apk`.

## ✅ Pengujian

Uji perhitungan tanpa Android SDK:

```bash
bash scripts/test-ratio-geometry.sh
```

Output yang diharapkan: `PASS 53 aspect geometry assertions`.

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

## ⚠️ Audit batasan pengembangan

| Batasan | Status |
|---|---|
| Blur video bergerak | Sudah ditambahkan; masih perlu QA visual berbagai perangkat |
| Preview video asli/hasil | Sudah ada player di aplikasi setelah ekspor |
| FPS metadata | Dibaca dari track via MediaExtractor; 30 FPS hanya fallback |
| Track audio hilang | Divalidasi keberadaan audio serta perbedaan durasi (belum sample-accurate) |
| Multi-aspect sekaligus | 9:16 dan 16:9 dalam satu batch |
| Trim video | Input waktu mulai/akhir dalam detik |
| Encoder Full HD gagal | Mencoba sekali lagi pada HD; Max Detail tidak turun diam-diam ke HD |
| AI super-resolution, AI deblur, face restoration | Belum: membutuhkan model ML berlisensi serta evaluasi performa dan identitas wajah |
| Crop manual berbasis fokus | Slider X/Y bekerja untuk foto dan video pada mode Crop |
| Pelacakan subjek otomatis dan timeline multi-track | Belum; membutuhkan modul analisis dan editor lanjutan |
| Proses saat aplikasi ditutup | Belum; butuh foreground service media-processing yang sesuai aturan Android |
| HDR dan format vendor | Masih tergantung dukungan decoder/encoder perangkat |
| Play Store | APK debug; signed release tersedia hanya jika pemilik repo mengonfigurasi signing secrets; QA perangkat produksi masih perlu |
| Status WhatsApp/IG | Kompresi setelah unggah di luar kontrol aplikasi |

Fitur bertanda *belum* tetap dicantumkan secara transparan dan tidak diklaim selesai.

## 📝 Ekspektasi kualitas

WhatsApp/Instagram tetap melakukan proses ulang unggahan. Untuk membandingkan kualitas secara adil, periksa **sumber asli → file hasil aplikasi → media setelah diunggah** secara terpisah. Kualitas Max Detail menghasilkan file lebih besar dan membutuhkan RAM, CPU, GPU, serta ruang penyimpanan lebih banyak. Kamera sumber yang sudah pecah atau berkompresi berat tidak bisa dipulihkan sepenuhnya dengan filter ini.

## 🔧 Teknologi

Android Java 17, minSdk 29 (Android 10), target SDK 35 / compile SDK 36; Android Gradle Plugin 8.11.1, Gradle 8.13, AndroidX Media3 1.11.1, MediaStore scoped storage. Proses tetap lokal di Android.

© WFS StoryBoost. Hak cipta dan lisensi mengikuti pemilik repository; tidak ada lisensi open-source yang diasumsikan atau ditambahkan secara otomatis.
## 🔬 QA tambahan v2.1

Periksa **foto 1920×1080 → 9:16 Crop**: dialog Quality Advisor harus muncul dan menawarkan Blur (mencegah klaim kualitas Full HD palsu). Uji sebaliknya 9:16 → 16:9 Crop, dan bandingkan hasil Fit/Blur/Crop. Uji *preset Asli* dengan JPG + MP4 dalam rasio Original, Fit, Max Detail: SHA-256 sumber dan hasil harus identik jika sumber MP4/JPEG didukung. Untuk perangkat fisik, lakukan ekspor di ponsel lalu evaluasi *source → exported → WhatsApp Status*. Kompresi WhatsApp tetap di luar kendali aplikasi.
