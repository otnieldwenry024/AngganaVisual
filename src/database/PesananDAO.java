package database;

import model.DetailPesanan;
import model.Klien;
import model.Pembayaran;
import model.Pesanan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PesananDAO {

    // Cek login Admin -> kembalikan nama lengkap jika cocok
    public String loginAdmin(String username, String password) {
        String sql = "SELECT nama_lengkap FROM admin WHERE username = ? AND password = ?";
        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nama_lengkap");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Cek login Pemesan/Klien -> kembalikan objek Klien jika cocok
    public Klien loginKlien(String username, String password) {
        String sql = "SELECT id_klien, nama_klien, kontak, username FROM klien WHERE username = ? AND password = ?";
        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Klien(
                            rs.getString("id_klien"),
                            rs.getString("nama_klien"),
                            rs.getString("kontak"),
                            rs.getString("username")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Daftar akun pemesan baru (auto-increment K-003, K-004, dst.)
    public String daftarKlienBaru(String namaKlien, String kontak, String username, String password) {
        String cekSql = "SELECT COUNT(*) FROM klien WHERE username = ?";
        String insertSql = "INSERT INTO klien (id_klien, nama_klien, kontak, username, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = KoneksiDB.getConnection()) {
            try (PreparedStatement psCek = conn.prepareStatement(cekSql)) {
                psCek.setString(1, username);
                try (ResultSet rs = psCek.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "DUPLICATE";
                    }
                }
            }

            String idBaru = generateIdKlienBaru(conn);
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, idBaru);
                ps.setString(2, namaKlien);
                ps.setString(3, kontak);
                ps.setString(4, username);
                ps.setString(5, password);
                if (ps.executeUpdate() > 0) {
                    return idBaru;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getDaftarKlien() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id_klien, nama_klien FROM klien ORDER BY id_klien ASC";
        try (Connection conn = KoneksiDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("id_klien") + " - " + rs.getString("nama_klien"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> getDaftarLayanan() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id_layanan, nama_layanan FROM layanan ORDER BY id_layanan ASC";
        try (Connection conn = KoneksiDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("id_layanan") + " - " + rs.getString("nama_layanan"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String generateIdKlienBaru(Connection conn) throws SQLException {
        int max = 0;
        String sql = "SELECT id_klien FROM klien";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String id = rs.getString("id_klien");
                try {
                    int angka = Integer.parseInt(id.replaceAll("[^0-9]", ""));
                    if (angka > max) max = angka;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("K-%03d", max + 1);
    }

    public String tambahKlienBaru(String namaKlien, String kontak) {
        String sql = "INSERT INTO klien (id_klien, nama_klien, kontak) VALUES (?, ?, ?)";
        try (Connection conn = KoneksiDB.getConnection()) {
            String idBaru = generateIdKlienBaru(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, idBaru);
                ps.setString(2, namaKlien);
                ps.setString(3, kontak);
                if (ps.executeUpdate() > 0) {
                    return idBaru;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean simpanPesananBaru(Pesanan p, DetailPesanan d) {
        String sqlPesanan = "INSERT INTO pesanan (id_pesanan, id_klien, jadwal, lokasi, deadline, status_pengerjaan) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlDetail = "INSERT INTO detail_pesanan (id_detail, id_pesanan, id_layanan, kebutuhan, harga_kesepakatan) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = KoneksiDB.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psPesanan = conn.prepareStatement(sqlPesanan);
                 PreparedStatement psDetail = conn.prepareStatement(sqlDetail)) {

                psPesanan.setString(1, p.getIdPesanan());
                psPesanan.setString(2, p.getIdKlien());
                psPesanan.setDate(3, Date.valueOf(p.getJadwal()));
                psPesanan.setString(4, p.getLokasi());
                psPesanan.setDate(5, Date.valueOf(p.getDeadline()));
                psPesanan.setString(6, p.getStatusPengerjaan());
                psPesanan.executeUpdate();

                psDetail.setString(1, d.getIdDetail());
                psDetail.setString(2, d.getIdPesanan());
                psDetail.setString(3, d.getIdLayanan());
                psDetail.setString(4, d.getKebutuhan());
                psDetail.setDouble(5, d.getHargaKesepakatan());
                psDetail.executeUpdate();

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Untuk Admin: ambil semua pesanan
    public List<Pesanan> getSemuaPesanan() {
        List<Pesanan> list = new ArrayList<>();
        String sql = "SELECT p.*, k.nama_klien FROM pesanan p JOIN klien k ON p.id_klien = k.id_klien ORDER BY p.jadwal DESC";

        try (Connection conn = KoneksiDB.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Pesanan p = buatObjekPesanan(conn, rs);
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Untuk Pemesan: ambil pesanan milik klien yang sedang login saja
    public List<Pesanan> getPesananByKlien(String idKlien) {
        List<Pesanan> list = new ArrayList<>();
        String sql = "SELECT p.*, k.nama_klien FROM pesanan p JOIN klien k ON p.id_klien = k.id_klien WHERE p.id_klien = ? ORDER BY p.jadwal DESC";

        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idKlien);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Pesanan p = buatObjekPesanan(conn, rs);
                    list.add(p);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Pesanan buatObjekPesanan(Connection conn, ResultSet rs) throws SQLException {
        Pesanan p = new Pesanan(
                rs.getString("id_pesanan"),
                rs.getString("id_klien"),
                rs.getString("nama_klien"),
                rs.getDate("jadwal").toLocalDate(),
                rs.getString("lokasi"),
                rs.getDate("deadline").toLocalDate(),
                rs.getString("status_pengerjaan"),
                rs.getString("tautan_hasil")
        );
        muatDetailPesanan(conn, p);
        muatPembayaranPesanan(conn, p);
        return p;
    }

    private void muatDetailPesanan(Connection conn, Pesanan p) throws SQLException {
        String sql = "SELECT * FROM detail_pesanan WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getIdPesanan());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    p.tambahDetail(new DetailPesanan(
                            rs.getString("id_detail"),
                            rs.getString("id_pesanan"),
                            rs.getString("id_layanan"),
                            rs.getString("kebutuhan"),
                            rs.getDouble("harga_kesepakatan")
                    ));
                }
            }
        }
    }

    private void muatPembayaranPesanan(Connection conn, Pesanan p) throws SQLException {
        String sql = "SELECT * FROM pembayaran WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getIdPesanan());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    p.tambahPembayaran(new Pembayaran(
                            rs.getString("id_pembayaran"),
                            rs.getString("id_pesanan"),
                            rs.getDate("tanggal_bayar").toLocalDate(),
                            rs.getDouble("nominal"),
                            rs.getString("metode_pembayaran")
                    ));
                }
            }
        }
    }

    public boolean updateStatus(String idPesanan, String statusBaru) {
        String sql = "UPDATE pesanan SET status_pengerjaan = ? WHERE id_pesanan = ?";
        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusBaru);
            ps.setString(2, idPesanan);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean inputPembayaran(Pembayaran bayar) {
        String sql = "INSERT INTO pembayaran (id_pembayaran, id_pesanan, tanggal_bayar, nominal, metode_pembayaran) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bayar.getIdPembayaran());
            ps.setString(2, bayar.getIdPesanan());
            ps.setDate(3, Date.valueOf(bayar.getTanggalBayar()));
            ps.setDouble(4, bayar.getNominal());
            ps.setString(5, bayar.getMetodePembayaran());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateTautanHasil(String idPesanan, String link) {
        String sql = "UPDATE pesanan SET tautan_hasil = ? WHERE id_pesanan = ?";
        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, link);
            ps.setString(2, idPesanan);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}