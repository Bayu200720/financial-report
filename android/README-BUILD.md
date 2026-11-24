# Build APK (My Financial Report) — panduan singkat

## Prasyarat
- **Android Studio** (terbaru stabil) terpasang.
- Koneksi internet untuk unduh Gradle/SDK/dependensi saat pertama kali sync.

## Langkah build (disarankan)
1. Buka **Android Studio** → **Open** → pilih folder `android` ini (`D:/htdocs/Financial-report/android`).
2. Tunggu **Gradle Sync** selesai. Jika diminta install SDK/Build Tools, ikuti prompt.
3. Build APK debug:
   - **Build > Build Bundle(s) / APK(s) > Build APK(s)**  
   atau di terminal (di dalam folder `android`), setelah sync berhasil:
   - Windows: `gradlew.bat assembleDebug`
4. Hasil APK debug: `app/build/outputs/apk/debug/app-debug.apk`

## Catatan penting
- **Aset HTML** harus ada di `app/src/main/assets/`:
  - `financial-ledger.html` (utama, dibuka WebView)
  - `log.html`, `income-parser.html` (opsional, untuk uji parser di browser)
- **NFC**: proyek ini **belum** mengimplementasikan NFC. APK yang dibuild dari source ini **tidak** bisa membaca kartu e-money/NFC.
- **Akses notifikasi**: setelah install, aktifkan **Notification access** untuk app ini di pengaturan Android (app akan menampilkan dialog pengingat).
- **Izinkan instalasi dari sumber tidak dikenal** saat install APK debug di perangkat (jika diminta).
- **Tanpa INTERNET**: `AndroidManifest.xml` tidak meminta izin INTERNET (sesuai desain PRD). Jika butuh fitur online di masa depan, harus disesuaikan.

## Jika Gradle Sync gagal
- Pastikan `local.properties` atau pengaturan SDK mengarah ke Android SDK Anda (Android Studio biasanya membuatnya otomatis).
- Jika ada pesan versi AGP/Gradle tidak cocok, ikuti rekomendasi Android Studio (upgrade/downgrade yang disarankan), lalu sync ulang.
- Jika `gradlew.bat` belum lengkap wrapper-nya, buka dulu proyek di Android Studio dan biarkan sinkronisasi berjalan; Android Studio akan menyiapkan wrapper.

## Untuk rilis (opsional)
- Butuh **keystore (signing)**. Gunakan **Build > Generate Signed Bundle / APK** dan isi konfigurasi signing Anda.
