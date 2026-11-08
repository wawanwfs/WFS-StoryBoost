# WFS StoryBoost — Creative Editing Studio v2.8.0

**Android offline-first.** Antarmuka editor non-destruktif dengan workflow proyek, timeline satu media, filter, trim, split-export, konversi rasio, dan kualitas ekspor sumber-asli.

[![Build APK](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml/badge.svg)](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml)

## Baru v2.8 — AI Bokeh reliability & real JPEG export QA

- **AI Portrait Bokeh**: proses segmentasi Google ML Kit memakai *raw mask* resolusi rendah yang dibesarkan dengan filtering dan feathered alpha. Latar yang diblur kini mengikuti transformasi crop/Fit dan posisi fokus yang sama dengan subjek; piksel foreground tetap diambil dari foto asli.
- **Preview nonblocking**: pekerjaan preview foto lama digabungkan/dibuang dari antrean tersendiri supaya slider filter tidak menumpuk job AI. Preview gagal mengosongkan gambar lama dan menampilkan alasan error, bukan menampilkan hasil edit sebelumnya.
- **Penanganan RAM**: kegagalan alokasi bitmap di preview AI menjadi error yang dapat dibaca dan tidak memaksakan interpolasi ke resolusi tidak realistis.
- **QA nyata di emulator**: pipeline memproses JPEG sungguhan yang dihasilkan offline, memverifikasi 1080×1920 dan custom 720×960, perubahan warna, EXIF rotasi 90°, copy sumber tanpa recompress, dan respons AI pada gambar tanpa manusia. Laporan durasi/memori dan hasil QA disimpan sebagai artifact CI.
- **Research model on-device**: `docs/MODEL_RESEARCH_V28.md` mengevaluasi NAFNet MIT (denoise) dan contoh TensorFlow ESRGAN. **Belum ada model baru denoise, deblur atau super-resolution yang dibundel**, sehingga ketiganya **belum aktif**; tidak mengklaim filter sebagai AI.
- **Kompatibilitas**: 24 Color Looks, 12 rasio, profil WA/IG/TikTok, video trim/split/merge, ARM64 dan Universal tetap dipertahankan.
- **Batas bukti**: QA otomatis memakai JPEG sintetis, **bukan** pengujian wajah manusia, rambut, kacamata, multi-person, atau HP fisik. Pengujian tersebut wajib sebelum rilis production. Platform sosial dapat mengompresi unggahan.

## v2.7.1 — Ukuran APK lebih efisien

- GitHub Actions sekarang menerbitkan **ARM64 APK** dan **Universal APK** terpisah.
- Pilih **ARM64** untuk HP Android modern berarsitektur arm64-v8a. Pilih **Universal** jika tidak mengetahui arsitektur CPU atau memakai perangkat lama/berbeda.
- **Ekstrak file ZIP artifact**, lalu pasang file `.apk` di dalamnya. Jangan mencoba memasang ZIP secara langsung.
- Kedua APK tetap debug-signed dan dapat berbeda sertifikat antara build. Untuk upgrade stabil, konfigurasi release signing privat masih diperlukan.
- Ukuran APK yang dicantumkan di Actions berasal dari file asli, bukan asumsi. Library native pada universal memuat empat ABI sehingga berukuran besar.

## WFS StoryBoost v2.7 — AI Portrait Bokeh (On-device, beta)

### Yang benar-benar berubah
- **AI Portrait Bokeh:** segmentasi orang memakai model **Google ML Kit Selfie Segmentation** bundled (`segmentation-selfie:16.0.0-beta6`), mode `SINGLE_IMAGE_MODE`. Model membuat mask alpha, lalu editor menggambar **piksel wajah/badan dari foto asli** di atas latar yang diblur. Tidak memodifikasi identitas atau fitur wajah.
- **Live AI Portrait preview:** saat sakelar aktif, editor menampilkan hasil segmentasi pada frame edit saat ini. Tombol **Sebelum / Sesudah** dapat membandingkan foto sumber dan efek AI. Photo export JPEG menjalankan komposisi dengan segmentasi lagi pada sumber resolusi lebih tinggi agar hasil tidak hanya bergantung preview.
- **Original file protection:** ketika AI Portrait aktif, mode byte-for-byte JPEG otomatis dinonaktifkan sehingga efek benar-benar ikut ke ekspor. Pilihan rasio/kualitas/custom/social tetap mempertahankan setelan AI.
- **Memori:** mask dibuat pada citra maksimal sisi 768 px; model tetap berjalan offline, dan ekspor foto resolusi besar mungkin memerlukan RAM tambahan.
- **Fallback:** apabila subjek tidak terdeteksi, mask hampir seluruh frame, model gagal, atau memori tidak cukup, proses mengembalikan pesan error—bukan berpura-pura berhasil menghapus background.
- **UI:** panel AI Photo kini dapat digulir sehingga tombol analisis, sakelar Portrait Bokeh, rekomendasi look, dan smart crop dapat diakses di layar kecil.
- **Tidak ada AI super-resolution/deblur/denoise model dalam rilis ini.** Background blur berbasis model segmentasi nyata, tetapi mengembalikan detail foto buram perlu model terpisah. Kompresi WA/IG/TikTok tetap tidak dapat dijamin hilang.

### Penting sebelum rilis publik
API selfie segmentation Google berada di tahap **beta**. Bentuk rambut, aksesori transparan, kondisi minim cahaya, serta beberapa orang dalam bingkai dapat memunculkan artefak mask. Uji foto orang asli dan perangkat fisik sebelum dianggap final. Jangan gunakan data wajah untuk upload/cloud; implementasi ini offline-only.

Lihat [PROGRESS.md](docs/PROGRESS.md) untuk estimasi capaian roadmap dan [NEXT_PROMPT.md](docs/NEXT_PROMPT.md) untuk instruksi pengembangan berikutnya.

## Baru v2.6 — Rasio Custom + AI Photo Studio

- **11 pilihan rasio** (9:16,16:9,1:1,4:5,Original,3:4,4:3,3:2,2:3,21:9,9:21) **+ Custom**. Pengguna bisa mengatur lebar dan tinggi (64–3840 piksel, batas 8,3 megapiksel). Rasio ini berlaku pada foto, video, preview, dan Quick Enhance. **Custom adalah satu pilihan tambahan**, jadi jumlah pilihan total menjadi 12.
- **AI Photo Studio (offline):** menggunakan model **Google ML Kit face-detection 16.1.7 dan image-labeling 17.0.9 yang dibundel di APK**. Gambar di-decode maksimal 1200 px pada sisi panjang untuk inferensi, tidak diunggah.
- **AI Face-Aware Smart Crop:** mendeteksi kotak wajah dan menyarankan fokus crop tanpa mengubah bentuk atau identitas wajah. Pengguna tetap harus menekan tombol penerapan.
- **AI Scene Color:** mengenali label seperti Beach, Forest, Sunset, Selfie dan merekomendasikan Color Look dari 24 preset. Hasil deteksi bisa keliru; fallback memakai Natural/Portrait Soft.
- **AI tidak sama dengan generative retouching:** **belum** tersedia AI super-resolution/deblur sungguhan, generative fill, atau pemulihan detail ajaib. Kualitas akhir tetap bergantung sumber dan kompresi platform WhatsApp/IG/TikTok.
- Model bundled memperbesar APK sekitar 12–15 MB dan membutuhkan lebih banyak RAM saat inferensi. Tidak ada login atau pengunggahan foto untuk pemrosesan AI.
- Peningkatan versi diuji dalam GitHub Actions: source geometry + AI decision policy serta build dan smoke test Android emulator. Pengujian ML Kit pada beragam merek HP tetap diperlukan.

## Baru di v2.5 — 24 Color Looks + Social Export
- **24 preset warna**: Natural, Cinematic, Vibrant, Soft Night, Original, Teal & Orange, Golden Hour, Warm Film, Cool Breeze, Moody Blue, Matte Film, Vintage 90s, Black & White, Silver Noir, Rose Glow, Pastel Dream, Forest Green, Urban Night, Neon City, Beach Light, Sunset Glow, Portrait Soft, Cafe Brown, Mono Fade.
- **Satu katalog untuk foto dan video:** parameter kontras, saturasi, tone RGB dan shadow lift diterjemahkan untuk Canvas ColorMatrix maupun GPU Media3. Karena pipeline Android berbeda, kesesuaian warna foto/video mendekati tetapi tidak dijamin piksel-identik.
- **Ekspor platform:** Manual, WhatsApp Status, Instagram Story, Instagram Reels, TikTok. Memilih platform mengatur 9:16 1080×1920, H.264/AAC untuk video, JPEG untuk foto, dan batas bitrate 8/12/16/18 Mbps secara berurutan. Pengaturan ini mengurangi encoding berlebihan dan menargetkan kompatibilitas; bukan angka bitrate yang diwajibkan platform.
- **Safe-zone pratinjau:** editor menampilkan area UI atas/bawah pada rasio 9:16. Caption sosial diposisikan lebih ke tengah bingkai dan diproses dalam ekspor, bukan hanya overlay dekoratif.
- **Mode manual** tetap mendukung original, 16:9, 1:1, 4:5, Max Detail hingga 4K sumber.
- **Peringatan:** bitrate terukur bergantung hardware encoder; input yang blur atau sudah dikompresi tidak bisa disulap menjadi detail asli. Instagram/TikTok/WhatsApp tetap mengompresi unggahan. WhatsApp HD di chat tidak berlaku otomatis pada Status. FPS dan warna HDR masih tergantung sumber/codec perangkat; aplikasi tidak memaksa normalisasi frame rate.
- **Sumber parameter format:** WhatsApp Help Center (https://faq.whatsapp.com/759301289012856); Instagram Reels Publishing Meta (https://www.postman.com/meta/instagram/folder/830j7my/reels-publishing); TikTok Developers (https://developers.tiktok.com/docs/en/content-posting-api-media-transfer-guide).

## Tambahan v2.4: Multi-Clip Video Merge
- **Gabung Banyak Klip:** proyek khusus untuk memilih hingga 10 video, mengubah urutan dengan tombol ↑/↓, menghapus klip, dan memutar preview klip.
- **Ekspor satu MP4:** `VideoMergeEngine` memakai **Media3 Composition** dan `EditedMediaItemSequence` untuk konkatenasi video sesuai storyboard. Format output H.264 / AAC, kualitas HD, 1080p atau Native/Max Detail.
- **Tanpa crop diam-diam:** output memakai ukuran frame pertama dengan mode Fit agar rasio tidak berganti secara paksa.
- **QA emulator:** workflow memeriksa bahwa layar multi-clip dapat dibuka dari aplikasi nyata dan mengunggah screenshot.
- **Batasan jujur:** belum ada crossfade antar klip, layer timeline berganda, penggabungan musik tersendiri, atau keyframe; perbedaan HDR/codec/audio channel antara media masukan dapat menyebabkan gabungan gagal pada perangkat tertentu. Pengujian ekspor gabungan dua video di ponsel fisik masih wajib.

## Fitur tambahan v2.3

- **Color Adjust sungguhan:** slider Brightness, Contrast, Saturation (–100 sampai +100) terhubung dengan `PhotoEngine` dan efek GPU `VideoEngine`.
- **Text Overlay:** tulisan sampai 80 karakter, warna putih atau emas. Pada foto dirender oleh Canvas; pada video memakai Media3 `TextOverlay` dan `OverlayEffect` untuk seluruh durasi output.
- **Audio Studio:** opsi menghapus audio video ketika ekspor dengan `EditedMediaItem.Builder.setRemoveAudio(true)`; track audio hasil divalidasi. Tidak mengubah audio source.
- **Preview:** teks tampil langsung di editor; filter dan adjust foto tampil sebagai matriks warna, sedangkan efek GPU video diterapkan saat render hasil.
- **Ekspor Original:** mode copy byte-for-byte hanya aktif jika tidak ada crop, trim, color adjustment, teks, mute, atau efek lain. Tidak mengklaim kualitas baru melalui re-encoding.
- **Regression tests:** memeriksa pelestarian adjustment, caption, mute saat penggantian rasio/kualitas dan menolak data tidak valid.

**Batasan:** overlay teks masih statis (bukan keyframe), pemutar preview video belum memperlihatkan efek GPU hingga ekspor, tidak ada perpustakaan musik atau multi-track. Mengedit konten sumber berkualitas rendah tidak dapat menciptakan detail asli baru tanpa model super-resolution yang nyata.

## Fitur baru v2.2
- **Project Home baru:** dashboard, proyek baru, Photo Editor, Video Editor, Quick Enhance (fitur batch lama), preset rasio dan proyek terbaru. Semua kartu mempunyai aksi; **Coba Editor** membuka media demo yang dibuat offline.
- **Editor dengan timeline:** preview foto/video; pemutaran, seekbar, strip thumbnail video, dan rentang trim awal/akhir.
- **Split at Playhead:** membagi ekspor satu video menjadi **dua berkas video** berdasarkan posisi playhead. Belum menyatukan kembali track atau transisi.
- **Filter:** Natural, Cinematic, Vibrant, Soft Night, Original (untuk foto live preview, video dirender saat ekspor).
- **Rasio & framing:** 9:16, 16:9, 1:1, 4:5, Original; Fit/Crop/Blur dan posisi fokus.
- **Ekspor:** HD, Full HD, Max Detail, dua rasio sekaligus, progress dan share hasil. Audio sumber dipertahankan oleh engine lama.
- **Peringatan detail:** Crop ekstrem menawarkan Blur, agar pengguna memahami piksel yang akan hilang.
- **Smoke test UI aktual:** GitHub Actions memasang APK di emulator Android 15, membuka dashboard, lalu membuka editor demo dan menyimpan screenshot nyata sebagai artifact.

## Catatan yang perlu diketahui
UI ini menargetkan pengalaman aplikasi editor profesional, **bukan salinan CapCut**. V2.2 masih editor **satu media per proyek**, bukan timeline multi-layer dengan animasi keyframe, efek premium, green screen, penghilangan latar, musik multi-track, atau AI upscale. Fitur tersebut membutuhkan modul yang belum diterapkan. Mode ekspor tetap tergantung codec ponsel. Kompresi WhatsApp/Instagram setelah unggah tidak dapat dicegah aplikasi.

## Pasang
Buka [Actions: Build and verify WFS StoryBoost APK](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml), pilih run sukses terbaru, unduh `WFS-StoryBoost-v2.8.0-ARM64-APK atau WFS-StoryBoost-v2.8.0-Universal-APK`, **ekstrak ZIP**, lalu instal `app-arm64-v8a-debug.apk atau app-universal-debug.apk`. Jika pemasangan update gagal karena kunci signing debug berbeda, hapus versi debug lama terlebih dahulu atau gunakan release signing stabil dengan GitHub Secrets.

## Pengembangan / QA
Aplikasi memakai Android Java 17 / Media3. Workflow menjalankan tes geometri, `assembleDebug`, `apksigner`, `zipalign`, lalu install & launch emulator. Untuk testing HP fisik, lihat `tests/TEST_PLAN.md`. Screenshot editor dari emulator bukan jaminan semua pemrosesan media lolos pengujian perangkat nyata.

\n---\n\n# ✦ WFS StoryBoost — Creative Media Studio

**Aplikasi Android offline-first untuk mempercantik foto/video dan menyiapkan hasil untuk WhatsApp Story, Instagram, TikTok, maupun konten landscape.**

[![Build APK](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml/badge.svg)](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml)

> **v2.8.0 • Premium Studio UI + Source-Aware Quality Engine.** CI menghasilkan **APK debug** jika workflow berhasil. Peningkatan warna dan ketajaman berbasis filter (bukan AI generatif). WhatsApp tetap dapat mengompresi Status, jadi kualitas media setelah unggah tidak bisa dijamin identik dengan berkas ekspor.

## ✨ Baru di v2.7.1
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

Workflow membangun APK, memvalidasi signature dan launcher, memasang APK di Android emulator API 35, meluncurkan MainActivity, lalu merekam **screenshot aktual home app** sebagai artifact `WFS-StoryBoost-v2.7.1-Emulator-Screenshot`. Screenshot adalah UI aplikasi nyata, bukan poster AI.

## 📥 Download APK (via GitHub Actions)

1. Buka tab [**Actions**](https://github.com/wawanwfs/WFS-StoryBoost/actions/workflows/build-apk.yml).
2. Pilih workflow **Build and verify WFS StoryBoost APK**.
3. Buka run terbaru dengan status **Success** (atau klik **Run workflow** jika diperlukan).
4. Unduh artifact **WFS-StoryBoost-v2.8.0-ARM64-APK atau WFS-StoryBoost-v2.8.0-Universal-APK** dan ekstrak ZIP-nya.
5. Instal `app-arm64-v8a-debug.apk atau app-universal-debug.apk` di Android 10 atau versi lebih baru (aktifkan izin pemasangan aplikasi dari sumber tersebut saat diminta).

> Artifact tersedia **hanya setelah build sukses**. Debug APK ditandatangani kunci debug sementara; untuk update stabil gunakan signed release dengan GitHub Secrets. v1.7 memakai package baru untuk menghindari konflik dengan versi lama. Jangan mengunggah kunci signing atau password ke repository.

## 🧰 Build lokal Windows 10/11

Syarat: **JDK 17**, **Android SDK API 36 + Build Tools 35**, Android Studio atau SDK cmdline tools, dan koneksi untuk Gradle/dependensi.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-windows.ps1
```

Jika sukses, APK debug berada pada `dist/WFS-StoryBoost-v2.7.1-debug.apk`.

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
