-- Pilih/buat database anggana_visual_db dahulu di phpMyAdmin.
-- Tidak menghapus data atau mengubah tabel yang sudah ada.
CREATE TABLE IF NOT EXISTS admin (
 id_admin INT AUTO_INCREMENT PRIMARY KEY, nama_lengkap VARCHAR(100) NOT NULL,
 username VARCHAR(50) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL,
 terdaftar_sejak DATE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS klien (
 id_klien VARCHAR(20) PRIMARY KEY, nama_klien VARCHAR(100) NOT NULL,
 kontak VARCHAR(50) NOT NULL, username VARCHAR(50) UNIQUE, password VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS layanan (
 id_layanan VARCHAR(20) PRIMARY KEY, nama_layanan VARCHAR(100) NOT NULL,
 harga_dasar DECIMAL(12,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pesanan (
 id_pesanan VARCHAR(20) PRIMARY KEY, id_klien VARCHAR(20) NOT NULL,
 jadwal DATE NOT NULL, lokasi VARCHAR(255) NOT NULL, deadline DATE NOT NULL,
 status_pengerjaan ENUM('Terjadwal','Produksi','Editing','Selesai') DEFAULT 'Terjadwal',
 tautan_hasil VARCHAR(255),
 FOREIGN KEY(id_klien) REFERENCES klien(id_klien) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS detail_pesanan (
 id_detail VARCHAR(20) PRIMARY KEY, id_pesanan VARCHAR(20) NOT NULL,
 id_layanan VARCHAR(20) NOT NULL, kebutuhan TEXT NOT NULL,
 harga_kesepakatan DECIMAL(12,2) NOT NULL,
 FOREIGN KEY(id_pesanan) REFERENCES pesanan(id_pesanan) ON DELETE CASCADE ON UPDATE CASCADE,
 FOREIGN KEY(id_layanan) REFERENCES layanan(id_layanan) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS pembayaran (
 id_pembayaran VARCHAR(30) PRIMARY KEY, id_pesanan VARCHAR(20) NOT NULL,
 tanggal_bayar DATE NOT NULL, nominal DECIMAL(12,2) NOT NULL,
 metode_pembayaran VARCHAR(50) NOT NULL DEFAULT 'Transfer Bank',
 FOREIGN KEY(id_pesanan) REFERENCES pesanan(id_pesanan) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
