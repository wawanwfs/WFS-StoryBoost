# WFS StoryBoost v1.7.0 — Instalasi Android

## Download yang benar
1. Buka **GitHub Actions → Build and verify WFS StoryBoost APK**.
2. Pilih run terbaru **Success**.
3. Download **WFS-StoryBoost-v1.7.0-Debug-APK**. Artifact GitHub adalah ZIP.
4. Ekstrak ZIP, lalu instal **app-debug.apk** (bukan file ZIP).
5. Di Android 10–16: izinkan pemasangan aplikasi dari browser/file manager bila diminta.

**Penting:** APK v1.7 memakai package baru `com.wfs.storyboost.studio`, sehingga tidak perlu menghapus v1.4–v1.6 (`com.wfs.storyboost`). App lama tetap terpisah.

## Jika masih muncul “App not installed”
- `INSTALL_FAILED_UPDATE_INCOMPATIBLE`: file yang akan dipasang memiliki sertifikat yang berbeda dari aplikasi dengan package sama. Jangan paksa upgrade debug; uninstall paket **baru** `com.wfs.storyboost.studio` lalu install ulang, atau gunakan stable signed release.
- `INSTALL_FAILED_OLDER_SDK`: Android di bawah versi 10 belum didukung (minSdk 29).
- `INSTALL_PARSE_FAILED_*`: pastikan APK diekstrak dari ZIP dan unduhan tidak terpotong.
- `INSTALL_FAILED_INSUFFICIENT_STORAGE`: kosongkan ruang penyimpanan.
- `INSTALL_FAILED_TEST_ONLY`: gunakan debug artifact yang benar, bukan file pengujian.
- `INSTALL_FAILED_VERIFICATION_FAILURE`: verifikasi lewat installer Android; jangan menonaktifkan Play Protect hanya untuk memasang APK.

Gunakan ADB untuk mengetahui kode error pasti:
```bash
adb install -r app-debug.apk
adb shell pm list packages | grep storyboost
adb shell am start -W -n com.wfs.storyboost.studio/com.wfs.storyboost.MainActivity
```

## Signing stabil (untuk update tanpa uninstall)
GitHub runner membuat debug keystore berbeda untuk tiap build. Dibandingkan APK v1.4, v1.5, v1.6, certificate SHA-256 terbukti berubah. Untuk production:
1. Buat keystore release **secara lokal**, jangan unggah file .jks ke repository publik.
2. Atur GitHub repository **Settings → Secrets and variables → Actions**:
   - `STORYBOOST_KEYSTORE_BASE64`: keystore .jks encoded base64, satu baris
   - `STORYBOOST_KEYSTORE_PASSWORD`: password keystore
   - `STORYBOOST_KEY_ALIAS`: nama alias
   - `STORYBOOST_KEY_PASSWORD`: password key
3. Jalankan ulang workflow. Artifact **WFS-StoryBoost-v1.7.0-Release-Signed** akan dibuat.
4. Simpan backup .jks dan kredensial di lokasi aman. Semua update wajib menggunakan key yang sama dan `versionCode` lebih tinggi.

Contoh membuat keystore pribadi di Windows dengan JDK 17:
```powershell
keytool -genkeypair -v -keystore .\storyboost-release.jks -alias storyboost -keyalg RSA -keysize 3072 -validity 10000
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\storyboost-release.jks))) | Set-Clipboard
```

Debug APK tetap untuk percobaan, dan bukan release yang dapat diperbarui lintas build. Jika Anda melaporkan pesan error persis dari ADB atau tangkapan layar installer, penyebab lainnya bisa didiagnosis.
