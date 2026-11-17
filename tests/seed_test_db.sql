-- HANYA untuk database uji baru. Bukan data produksi/kelompok.
INSERT INTO admin (nama_lengkap, username, password, terdaftar_sejak)
VALUES ('Admin Uji', 'testadmin', 'AdminTest123!', CURRENT_DATE);
INSERT INTO layanan VALUES ('L-TEST', 'Foto & Video Uji', 1500000.00);
