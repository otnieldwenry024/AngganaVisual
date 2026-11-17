# Anggana Visual

Aplikasi kelompok PBO: backend Java, antarmuka web HTML/CSS/JavaScript, dan MySQL. Versi Swing tetap tersedia.

## Menjalankan versi web

1. Aktifkan MySQL di XAMPP. Apache tidak diperlukan untuk server Java.
2. Pastikan database `anggana_visual_db` sudah ada beserta enam tabelnya. Untuk database baru, impor `database/schema.sql` dari phpMyAdmin. Skrip ini tidak mengisi akun atau mengubah tabel lama.
3. Tambahkan MySQL Connector/J ke Dependencies IntelliJ (scope **Compile**). Untuk Terminal, letakkan JAR di `lib/`; skrip juga mengenali Connector/J yang sudah diekstrak di `~/Downloads/mysql-connector-j-*/`.
4. **IntelliJ:** buka `src/web/WebServer.java`, jalankan `main`. Working directory harus folder utama proyek.
5. Buka **http://localhost:8080**. Gunakan akun database yang sudah ada, atau buat akun klien melalui tab **Buat akun**.

Alternatif melalui Terminal (JDK 17+):

```sh
./scripts/run-web.sh
```

Hentikan dengan tombol Stop IntelliJ atau Ctrl+C. Jangan menjalankan dua server pada port yang sama.

`LoginFrame` dan `MainFrame` masih membuka aplikasi desktop. Untuk web gunakan **WebServer**.

## Konfigurasi opsional

Environment variables di Run Configuration IntelliJ atau Terminal:

| Nama | Default |
| --- | --- |
| `AV_PORT` | `8080` |
| `AV_DB_URL` | `jdbc:mysql://localhost:3306/anggana_visual_db` |
| `AV_DB_USER` | `root` |
| `AV_DB_PASSWORD` | kosong |
| `AV_WEB_ROOT` | `web`, relatif terhadap working directory |

Server mengikat `127.0.0.1` untuk penggunaan lokal. Sites hosting tidak menjalankan backend JVM ini; deployment publik Java membutuhkan hosting Java serta konfigurasi HTTPS dan database tersendiri.

## Fitur

- Login admin/klien, registrasi klien, sesi 8 jam, logout.
- Dua halaman: `/app` untuk ringkasan statistik dan `/app/orders` untuk daftar pesanan. Sidebar mengikuti halaman aktif, termasuk setelah refresh dan Back/Forward.
- Pencarian/filter status, detail pesanan, riwayat pembayaran, tautan hasil.
- Klien membuat dan melihat pesanan sendiri. Harga klien berasal dari katalog database.
- Admin membuat pesanan/klien, menyesuaikan harga, mengubah status, mencatat pembayaran, dan memasukkan URL HTTPS hasil akhir.
- Validasi tanggal, nominal positif, pembayaran tidak melebihi sisa tagihan; ID acak agar tidak berulang setiap 10 detik.
- Penambahan klien oleh admin hanya membuat data klien tanpa kredensial. Akun login dibuat lewat registrasi. Belum ada proses menghubungkan akun baru ke klien lama.

Alur status tetap sama seperti desktop; admin dapat memilih salah satu status. Aplikasi mencatat pembayaran manual, bukan payment gateway. Katalog layanan dikelola melalui database.

## Struktur OOP

- `src/web/`: server HTTP/controller, serializer JSON, penyimpanan sesi.
- `src/service/WebService.java`: aturan bisnis dan pemeriksaan peran.
- `src/database/`: akses data JDBC; digunakan bersama web dan Swing.
- `src/model/`: objek bisnis dan perhitungan tagihan.
- `src/security/Passwords.java`: hash PBKDF2 untuk akun baru, pemeriksaan password lama untuk kompatibilitas.
- `web/`: halaman, stylesheet, dan JavaScript browser. Tidak ada PHP.

Akun baru menggunakan hash PBKDF2. Akun lama dengan password plaintext masih dikenali, belum dimigrasikan otomatis. Password lama tidak berubah. Sesudah server dimatikan, sesi di memori hilang dan pengguna harus login ulang. Pembatasan percobaan login/registrasi adalah 15 permintaan per menit per alamat IP.

## Pengujian

Jalankan `python3 tests/web_integration.py` saat server uji sudah berjalan. Pengujian menggunakan database uji yang diisi oleh `tests/seed_test_db.sql`, bukan database kelompok. Lihat bagian atas file pengujian untuk detail. Jangan impor seed uji ke database kelompok.

Untuk memeriksa model/security tanpa database:

```sh
javac -encoding UTF-8 -d out src/security/Passwords.java src/web/Json.java src/web/SessionStore.java tests/CoreChecks.java
java -cp out CoreChecks
```
