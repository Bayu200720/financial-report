# PRD: My Financial Report

_Product Requirements Document · versi 1.0 · 24 November 2026 · disusun dari seluruh proses pengembangan bersama Ian_

## 1. Ringkasan

My Financial Report adalah aplikasi keuangan pribadi berbentuk satu file HTML (`financial-ledger.html`). Aplikasi mencatat pemasukan, pengeluaran, aset, liabilitas, dan modal dengan akuntansi akrual, lalu menghasilkan laporan keuangan yang selalu seimbang. Data tersimpan di perangkat pengguna dan tidak dikirim ke server.

Produk ini berkembang menjadi satu keluarga komponen:

- **Aplikasi utama** (HTML): pencatatan, laporan, anggaran, impor dan ekspor CSV, scan struk, dua bahasa.
- **Pencatat uang masuk otomatis** (Android, Kotlin): membaca notifikasi bank dan e-wallet, lalu mencatatnya sebagai Income.
- **Halaman log** (HTML): riwayat uang masuk yang terdeteksi.
- **Penguji parser** (HTML): menguji aturan pembacaan teks notifikasi.
- **Tab uji NFC** (HTML): menguji apa yang bisa dibaca ponsel dari kartu e-money.

## 2. Pengguna dan masalah

**Pengguna utama:** Ian, manajer keuangan di Indonesia, memakai ponsel Android, berbahasa Indonesia dan Inggris, memahami akuntansi.

**Masalah yang diselesaikan:**

1. Pencatat keuangan pribadi umumnya hanya arus kas, tanpa neraca dan laba rugi yang benar.
2. Mencatat uang masuk dari bank dan e-wallet secara manual melelahkan dan sering terlewat.
3. Data keuangan bersifat sensitif, sehingga pengguna ingin data tetap di perangkat.

## 3. Tujuan dan bukan tujuan

**Tujuan**

- Laporan keuangan bergaya akuntansi (P&L, neraca, perubahan ekuitas) yang selalu seimbang.
- Satu file, tanpa server, dapat dibuka di ponsel.
- Uang masuk terdeteksi otomatis di Android tanpa mengirim data keluar perangkat.
- Tampilan ramah ponsel, mode terang dan gelap, bahasa Inggris dan Indonesia.

**Bukan tujuan (versi ini)**

- Sinkronisasi antar perangkat, akun pengguna, atau penyimpanan cloud.
- Membaca saldo kartu e-money lewat NFC (dibatasi enkripsi bank, lihat bagian 9).
- Pendeteksian otomatis di iPhone.
- Pelaporan pajak atau standar akuntansi resmi (SAK).

## 4. Aplikasi utama: kebutuhan fungsional

### 4.1 Pencatatan transaksi

| ID | Kebutuhan |
| --- | --- |
| F-01 | Lima tipe transaksi: Expense, Income, Asset, Liability, Capital. |
| F-02 | Asset, Liability, dan Capital punya arah Increase atau Decrease. Ketiganya tidak memengaruhi laba rugi maupun arus kas. |
| F-03 | Setiap transaksi punya efek neraca otomatis. Boleh dua efek (split): jumlah efek pertama diketik, sisanya dihitung otomatis. |
| F-04 | Setiap entri selalu seimbang (debit sama dengan kredit). Pasangan dibuat otomatis, misalnya "Opening balance equity" atau kas atau bank. |
| F-05 | Jumlah diketik dengan pemisah ribuan otomatis dan pratinjau berformat. |
| F-06 | Kategori berupa dropdown per tipe, dengan tombol "+ New category" untuk kategori kustom. |
| F-07 | Income dan Expense tidak boleh bertarget Capital, karena laba mengalir ke modal lewat laba ditahan. |

### 4.2 Kartu saldo

- Kartu "Total balance" tetap, satu kartu Cash tetap, dan kartu Bank, E-wallet, atau Card yang bisa ditambah, diberi nama, diubah, dan dihapus.
- Saldo kartu sama dengan saldo awal ditambah transaksi baru yang memilih kartu itu. Transaksi lama tanpa kartu tidak menggerakkan kartu.
- Saldo awal kartu muncul di neraca sebagai Asset dengan pasangan "Opening balance equity".

### 4.3 Laporan

| ID | Laporan | Aturan |
| --- | --- | --- |
| R-01 | Ringkasan periode | Total pemasukan, pengeluaran, arus kas bersih. |
| R-02 | Anggaran | Batas bulanan, bar progres, mode tahun memakai anggaran bulanan dikali 12. |
| R-03 | Pergerakan neraca | Kartu Assets, Liabilities, Capital dengan saldo awal, kenaikan, penurunan, saldo akhir. |
| R-04 | Laba rugi per kategori | Persentase tiap kategori terhadap total seksinya sendiri: pemasukan terhadap total pemasukan, pengeluaran terhadap total pengeluaran. Persentase ditampilkan tebal dan berwarna (hijau untuk pemasukan, warna pengeluaran aplikasi untuk pengeluaran). |
| R-05 | Perubahan ekuitas | Ekuitas awal, kontribusi, penarikan, laba bersih dari P&L, ekuitas akhir. Harus sama dengan Total capital di neraca. |
| R-06 | Grafik | Pemasukan dan pengeluaran: 6 bulan di mode bulan, 12 bulan Januari sampai Desember di mode tahun. Judul tanpa keterangan jumlah bulan. |
| R-07 | Neraca | Posisi pada akhir periode. Pesan "Balanced" harus selalu muncul. |
| R-08 | Daftar transaksi | Mengikuti periode yang dipilih. |

### 4.4 Periode dan tahun buku

- Pilihan **Period type**: Bulan atau Tahun. Pilihan diingat.
- Tahun buku mengikuti tahun kalender, Januari sampai Desember.
- **Penutupan otomatis:** laba tahun-tahun yang sudah lewat langsung menyatu ke Owner capital mulai 1 Januari tahun berikutnya. Neraca hanya menampilkan baris Owner capital dan baris Current year profit untuk laba tahun berjalan. Tidak ada baris laba ditahan terpisah.
- Ekuitas awal Januari sama dengan ekuitas akhir Desember tahun sebelumnya. Aturan ini berlaku untuk tahun rugi juga.
- Penutupan bersifat tampilan dan perhitungan. Tidak ada transaksi penutup yang tercatat, sehingga data asli tidak berubah.

### 4.5 Impor dan ekspor

- Ekspor CSV dan template CSV. Kolom: Date, Type, Category, Description, Amount, Direction, Links.
- Impor CSV mendukung kata Indonesia (aset, utang, modal), format tanggal dan angka Eropa maupun AS, dan menolak file .xlsx.
- Scan struk memakai pembaca teks Tesseract (bahasa Inggris dan Indonesia) dan mengisi formulir untuk diperiksa pengguna sebelum disimpan.

### 4.6 Tampilan dan bahasa

- Gaya Duolingo: hijau cerah, tombol 3D tebal, font Nunito, kontras teks sudah diuji.
- Mode terang dan gelap, dengan tombol bulat di pojok kanan atas.
- Dua halaman utama (Home dan Reports) dan tab NFC di bar menu bawah (di atas pada layar 900px ke atas).
- Bahasa: English dan Bahasa Indonesia dengan tombol bendera. Teks dan pesan dinamis diterjemahkan, sedangkan data yang diketik pengguna tidak.
- Mata uang: IDR, USD, EUR, GBP, SGD, JPY, AUD.

## 5. Pencatat uang masuk otomatis (Android)

### 5.1 Alur data

1. Layanan membaca notifikasi hanya dari aplikasi dalam daftar yang diizinkan (BRImo, Livin' by Mandiri, BCA mobile, myBCA, BNI Mobile, DANA, OVO, GoPay). Nama paket harus dipastikan lewat Logcat di perangkat pengguna.
2. Parser mengambil nominal dan pengirim dari teks notifikasi.
3. Layanan menyimpan hanya pengirim, nominal, waktu, dan nama aplikasi. Teks notifikasi lengkap tidak disimpan.
4. Aplikasi membuka `financial-ledger.html` di WebView dan memanggil `importIncome(id, pengirim, nominal, waktu, aplikasi)`.
5. Aplikasi keuangan mencatat transaksi Income berkategori "Incoming (auto-detected)" dengan deskripsi "Auto: nama (aplikasi)".

### 5.2 Aturan parser

- Konservatif: notifikasi yang meragukan dilewati, lebih baik terlewat daripada salah mencatat.
- Diterima bila ada petunjuk uang masuk (menerima, diterima, dana masuk, uang masuk, transfer masuk, saldo masuk, setoran, received, incoming).
- Ditolak bila ada petunjuk lain (mengirim, pembayaran, debit, penarikan, OTP, tagihan, top up, dan sejenisnya).
- Nominal setelah kata saldo, biaya, admin, fee, atau sisa dilewati.
- Format angka: `1.500.000`, `1.500.000,00`, `1,500,000.00`, dan `50.000.` dibaca sebagai rupiah utuh.
- Spasi khusus (non-breaking) setelah "Rp" dibaca dengan benar.
- Pengirim diambil setelah kata "dari" atau "from". Bila tidak ada, dipakai nama aplikasi.

### 5.3 Pencocokan akun

- Uang masuk dihubungkan ke kartu yang namanya cocok dengan aplikasi sumber (misalnya BCA mobile ke kartu "BCA").
- Bila tidak ada yang cocok, kartu bank atau e-wallet yang belum diberi nama dipakai hanya jika jumlahnya tepat satu. Kartu yang sudah diberi nama tidak pernah dipakai sebagai cadangan.
- Bila tidak ada, transaksi dicatat tanpa menggerakkan kartu.

### 5.4 Ketahanan

- Setiap uang masuk punya ID. ID yang sudah pernah diimpor ditolak, juga setelah muat ulang.
- Notifikasi yang sama dalam 2 menit dianggap duplikat.
- Uang masuk baru ditandai terkirim hanya bila halaman menjawab sukses. Bila gagal, dicoba lagi.
- Layanan menyambung ulang sendiri bila diputus sistem.

### 5.5 Privasi dan keamanan

- Aplikasi sengaja tanpa izin internet, sehingga tidak ada data yang keluar dari ponsel.
- WebView hanya boleh membuka file aplikasi sendiri.
- Satu-satunya jalur dari halaman ke aplikasi adalah permintaan menyimpan teks CSV, yang harus disetujui pengguna di dialog sistem (maksimum 5 juta karakter).
- Dialog konfirmasi, pemilih file untuk impor CSV dan scan struk, serta penyimpanan CSV didukung lewat komponen WebView khusus.

## 6. Komponen pendukung

- **log.html:** riwayat uang masuk dengan format Rupiah, tanggal dan jam, ringkasan bulan ini, penyimpanan di perangkat, tampilan ponsel, mode gelap, dan hapus riwayat dengan dua ketukan.
- **income-parser.html:** salinan JavaScript dari `IncomeParser.kt` untuk menguji teks notifikasi. Hasilnya diverifikasi identik dengan aturan Kotlin pada 36 contoh teks.
- **Tab NFC:** uji baca kartu lewat Web NFC. Menampilkan nomor seri, jumlah rekaman, dan isi NDEF bila ada. Hanya membaca, tidak menulis ke kartu dan tidak mengubah catatan keuangan.

## 7. Kebutuhan non-fungsional

| Area | Kebutuhan | Status |
| --- | --- | --- |
| Privasi | Data hanya di perangkat, tanpa server | Terpenuhi |
| Kode | `'use strict'`, const dan let, arrow function, template literal | Terpenuhi (1 `var` tersisa di skrip tema) |
| Lighthouse | Meta description, CSP, manifest, ikon, footer, satuan rem, tombol bertipe, tanpa inline style dan onclick | Terpasang, skor asli belum diukur |
| Aksesibilitas | Label, ARIA, fokus terlihat, gerak berkurang, target sentuh 44px | Terpenuhi |
| Responsif | Tanpa scroll horizontal dari 320px sampai 1200px | Teruji |
| Bahasa | English dan Indonesia | Terpenuhi |

## 8. Pengujian

Pengujian dijalankan di Chrome sungguhan lewat otomasi, dengan ukuran layar ponsel.

| Rangkaian | Hasil |
| --- | --- |
| Alur utama (transaksi, tab, tema, bahasa, simpan, hapus) | 19 dari 19 |
| Mendalam (neraca seimbang, terjemahan penuh) | 8 dari 8 |
| Tahun buku (penutupan ke Owner capital, tahun rugi, tiga tahun) | 27 dari 27 |
| Filter tahun (jumlah tahun sama dengan jumlah bulan, grafik 12 bulan, anggaran tahunan) | 35 dari 35 |
| Persentase laba rugi dan warna | 12 dari 12 |
| Impor uang masuk otomatis | 28 dari 28 |
| Ekspor CSV lewat jembatan Android dan browser biasa | 9 dari 9 |
| Tab NFC (dengan NFC tiruan) | 34 dari 34 |
| Halaman log | 24 dari 24 |
| Halaman penguji parser | 12 dari 12 |
| Parser: Kotlin versus JavaScript pada 36 teks | 36 identik |

**Belum teruji:**

- Kode Kotlin belum dikompilasi dan belum dijalankan di ponsel.
- Skor Lighthouse asli belum diukur.
- Scan struk belum diuji karena butuh internet.
- NFC belum diuji dengan kartu e-money sungguhan.
- Tampilan di layar ponsel sungguhan belum dilihat.

## 9. Risiko dan batasan

| Risiko | Penjelasan | Mitigasi |
| --- | --- | --- |
| Nama paket aplikasi bank | Daftar di kode adalah perkiraan | Cek lewat Logcat, matikan log setelah benar |
| Kode Kotlin belum dikompilasi | Mungkin ada galat saat build pertama | Bangun di Android Studio, kirim galat untuk diperbaiki |
| Notifikasi terlewat | Parser konservatif, misalnya kata "pembayaran" dalam uang masuk | Disengaja, bisa ditambah aturan per aplikasi |
| Saldo masuk GoPay | Format "Saldo masuk ... dari ..." ditolak | Tambah aturan khusus GoPay bila sering terjadi |
| Saldo kartu e-money | Flazz, e-Money Mandiri, dan BRIZZI memakai area terlindungi, jadi saldo tidak terbaca | Hanya nomor seri yang mungkin. Jalur resmi lewat aplikasi bank |
| Web NFC | Butuh Chrome Android dan alamat https. Tidak jalan dari file langsung, di iPhone, dan umumnya di WebView | Hosting https, atau NFC native bila perlu |
| Scan struk | Butuh internet, sedangkan aplikasi Android tanpa izin internet | Tambah izin INTERNET hanya bila fitur ini diinginkan |
| Izin Google Play | Akses notifikasi dibatasi di toko aplikasi | Aman untuk pemasangan pribadi |
| Anggaran tahunan | Anggaran bulanan dikali 12, tidak bisa berbeda per bulan | Jadi pengembangan lanjutan |

## 10. Keputusan penting

1. Satu file HTML tanpa framework, data di penyimpanan peramban.
2. Akuntansi akrual dengan jurnal seimbang otomatis, bukan pencatatan arus kas semata.
3. Penutupan tahun otomatis dan tampilan saja, bukan transaksi penutup.
4. Pendeteksian uang masuk di Android native, karena halaman web tidak bisa membaca notifikasi.
5. Parser konservatif, dengan pencocokan akun yang tidak pernah menebak akun yang sudah diberi nama.
6. Aplikasi Android tanpa izin internet demi privasi.
7. NFC dicoba dulu sebagai halaman uji sebelum diputuskan menjadi fitur.

## 11. Peta jalan

**Berikutnya**

1. Bangun proyek Android di Android Studio, perbaiki galat kompilasi bila ada.
2. Pastikan nama paket aplikasi bank lewat Logcat.
3. Buka halaman uji NFC lewat alamat https, coba dengan kartu asli, lalu putuskan apakah layak dilanjutkan.

**Kandidat pengembangan**

- Kamera langsung untuk tombol "Take photo" di scan struk.
- Laporan tahunan yang bisa diekspor ke PDF atau CSV.
- Perbandingan dua tahun berdampingan.
- Anggaran berbeda per bulan.
- Penguji parser terhubung ke aplikasi utama, untuk menempel teks notifikasi dan mencatatnya secara manual di iPhone atau komputer.
- Streak ala Duolingo.
- Port fitur ke aplikasi React Native (proyek terpisah, kodenya belum pernah dijalankan).

## 12. Lampiran: berkas

| Berkas | Fungsi |
| --- | --- |
| `financial-ledger.html` | Aplikasi utama |
| `financial-ledger-backup1` sampai `backup33.html` | Cadangan bertahap sebelum tiap perubahan besar |
| `IncomeNotificationListener.kt` | Layanan pembaca notifikasi dan daftar aplikasi |
| `IncomeParser.kt` | Aturan pembacaan teks notifikasi |
| `IncomeStore.kt` | Penyimpanan uang masuk di perangkat |
| `MainActivity.kt` | WebView, pemilih file, penyimpanan CSV, izin akses notifikasi |
| `AndroidManifest.xml` | Pendaftaran layanan, tanpa izin internet |
| `IncomeParserTest.kt` | Tes unit parser |
| `log.html` | Halaman riwayat uang masuk |
| `income-parser.html` | Penguji parser di peramban |

Proyek terpisah: dokumen "My Financial Report: React Native app with NFC" berisi kode React Native dan NFC yang belum dijalankan.
