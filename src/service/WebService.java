package service;

import database.KoneksiDB;
import database.PesananDAO;
import model.*;
import web.SessionStore.Session;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Aturan bisnis web; tidak bergantung pada komponen Swing atau browser. */
public final class WebService {
    private final PesananDAO dao = new PesananDAO();
    private static final List<String> STATUSES = List.of("Terjadwal", "Produksi", "Editing", "Selesai");

    public Map<String, String> login(Map<String, String> form) {
        String username = required(form, "username", 50);
        String password = password(form, false);
        String admin = dao.loginAdmin(username, password);
        if (admin != null) return Map.of("id", username, "name", admin, "role", "admin");
        Klien client = dao.loginKlien(username, password);
        if (client != null) return Map.of("id", client.getIdKlien(), "name", client.getNamaKlien(), "role", "klien");
        return null;
    }

    public synchronized void register(Map<String, String> form) {
        String result = dao.daftarKlienBaru(required(form, "name", 100), required(form, "contact", 50),
                required(form, "username", 50), password(form, true));
        if ("DUPLICATE".equals(result)) throw new IllegalArgumentException("Username sudah dipakai. Pilih username lain.");
        if (result == null) throw new IllegalStateException("Pendaftaran gagal.");
    }

    public List<Map<String, Object>> services() {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = KoneksiDB.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id_layanan, nama_layanan, harga_dasar FROM layanan ORDER BY id_layanan");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(Map.of("id", rs.getString(1), "name", rs.getString(2), "price", rs.getBigDecimal(3)));
            return result;
        } catch (SQLException e) { throw new IllegalStateException("Tidak dapat membaca layanan.", e); }
    }

    public Map<String, Object> data(Session user) {
        List<Pesanan> orders = user.isAdmin() ? dao.getSemuaPesanan() : dao.getPesananByKlien(user.userId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Pesanan order : orders) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", order.getIdPesanan()); row.put("client", order.getNamaKlien());
            row.put("date", order.getJadwal()); row.put("deadline", order.getDeadline());
            row.put("location", order.getLokasi()); row.put("status", order.getStatusPengerjaan());
            row.put("total", order.hitungTotalBiaya()); row.put("paid", order.hitungTotalTerbayar());
            row.put("remaining", order.hitungSisaTagihan()); row.put("result", order.getTautanHasil());
            row.put("details", order.getDaftarDetail().stream().map(d -> Map.of("needs", d.getKebutuhan(), "price", d.getHargaKesepakatan())).collect(java.util.stream.Collectors.toList()));
            row.put("payments", order.getDaftarPembayaran().stream().map(p -> Map.of("date", p.getTanggalBayar(), "amount", p.getNominal(), "method", p.getMetodePembayaran())).collect(java.util.stream.Collectors.toList()));
            rows.add(row);
        }
        List<Map<String, String>> clients = new ArrayList<>();
        if (user.isAdmin()) for (String client : dao.getDaftarKlien()) {
            String[] parts = client.split(" - ", 2);
            clients.add(Map.of("id", parts[0], "name", parts[1]));
        }
        return Map.of("orders", rows, "services", services(), "clients", clients, "statuses", STATUSES);
    }

    public synchronized String createOrder(Session user, Map<String, String> form) {
        String serviceId = required(form, "service", 20);
        Map<String, Object> service = services().stream().filter(s -> s.get("id").equals(serviceId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Layanan tidak tersedia."));
        String clientId = user.isAdmin() ? required(form, "client", 20) : user.userId;
        if (user.isAdmin() && dao.getDaftarKlien().stream().noneMatch(c -> c.startsWith(clientId + " - ")))
            throw new IllegalArgumentException("Klien tidak ditemukan.");
        LocalDate date = date(form, "date"), deadline = date(form, "deadline");
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("Jadwal acara tidak boleh sebelum hari ini.");
        if (deadline.isBefore(date)) throw new IllegalArgumentException("Deadline tidak boleh sebelum jadwal acara.");
        String location = required(form, "location", 255), needs = required(form, "needs", 4000);
        // Harga klien selalu berasal dari katalog server, tidak dari input browser.
        BigDecimal price = user.isAdmin() ? money(form, "price") : (BigDecimal) service.get("price");
        if (price.signum() <= 0) throw new IllegalArgumentException("Harga layanan harus lebih dari nol.");
        String id = id("P-");
        Pesanan order = new Pesanan(id, clientId, "", date, location, deadline, "Terjadwal", null);
        if (!dao.simpanPesananBaru(order, new DetailPesanan(id("D-"), id, serviceId, needs, price.doubleValue())))
            throw new IllegalStateException("Pesanan gagal disimpan.");
        return id;
    }

    public synchronized void createClient(Session user, Map<String, String> form) {
        admin(user);
        if (dao.tambahKlienBaru(required(form, "name", 100), required(form, "contact", 50)) == null)
            throw new IllegalStateException("Klien gagal disimpan.");
    }

    public synchronized void changeStatus(Session user, Map<String, String> form) {
        admin(user); Pesanan order = order(form);
        String status = required(form, "status", 20);
        if (!STATUSES.contains(status)) throw new IllegalArgumentException("Status tidak valid.");
        if (!dao.updateStatus(order.getIdPesanan(), status)) throw new IllegalStateException("Status gagal diubah.");
    }

    public synchronized void pay(Session user, Map<String, String> form) {
        admin(user); Pesanan order = order(form);
        BigDecimal amount = money(form, "amount");
        if (amount.compareTo(BigDecimal.valueOf(order.hitungSisaTagihan()).setScale(2, java.math.RoundingMode.HALF_UP)) > 0)
            throw new IllegalArgumentException("Pembayaran melebihi sisa tagihan.");
        if (!dao.inputPembayaran(new Pembayaran(id("PAY-"), order.getIdPesanan(), LocalDate.now(), amount.doubleValue(), "Transfer Bank")))
            throw new IllegalStateException("Pembayaran gagal disimpan.");
    }

    public synchronized void result(Session user, Map<String, String> form) {
        admin(user); Pesanan order = order(form);
        String link = required(form, "link", 255);
        URI uri;
        try { uri = URI.create(link); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Tautan hasil tidak valid."); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
            throw new IllegalArgumentException("Tautan hasil harus berupa URL HTTPS yang valid.");
        if (!dao.updateTautanHasil(order.getIdPesanan(), link)) throw new IllegalStateException("Tautan gagal disimpan.");
    }

    private Pesanan order(Map<String, String> form) {
        String id = required(form, "id", 20);
        return dao.getSemuaPesanan().stream().filter(p -> p.getIdPesanan().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pesanan tidak ditemukan."));
    }
    public static void admin(Session user) { if (!user.isAdmin()) throw new SecurityException("Fitur ini hanya untuk admin."); }
    private static String id(String prefix) { return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18); }
    private static String required(Map<String, String> form, String key, int max) {
        String value = form.getOrDefault(key, "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Kolom " + key + " wajib diisi.");
        if (value.length() > max) throw new IllegalArgumentException("Kolom " + key + " terlalu panjang (maksimal " + max + ").");
        return value;
    }
    private static String password(Map<String, String> form, boolean newAccount) {
        String value = form.getOrDefault("password", "");
        if (value.length() > 128 || value.trim().isEmpty() || (newAccount && value.length() < 8))
            throw new IllegalArgumentException(newAccount ? "Password harus terdiri dari 8–128 karakter." : "Password wajib diisi (maksimal 128 karakter).");
        return value;
    }
    private static LocalDate date(Map<String, String> form, String key) {
        try { return LocalDate.parse(required(form, key, 10)); }
        catch (java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Tanggal " + key + " tidak valid."); }
    }
    private static BigDecimal money(Map<String, String> form, String key) {
        try {
            BigDecimal n = new BigDecimal(required(form, key, 30));
            if (n.signum() <= 0 || n.compareTo(new BigDecimal("9999999999.99")) > 0 || n.scale() > 2) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Nominal harus positif, maksimal 2 angka desimal."); }
    }
}
