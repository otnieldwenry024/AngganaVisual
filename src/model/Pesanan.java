package model;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Pesanan {
    private String idPesanan;
    private String idKlien;
    private String namaKlien;
    private LocalDate jadwal;
    private String lokasi;
    private LocalDate deadline;
    private String statusPengerjaan;
    private String tautanHasil;

    private List<DetailPesanan> daftarDetail;
    private List<Pembayaran> daftarPembayaran;

    public Pesanan(String idPesanan, String idKlien, String namaKlien, LocalDate jadwal,
                   String lokasi, LocalDate deadline, String statusPengerjaan, String tautanHasil) {
        this.idPesanan = idPesanan;
        this.idKlien = idKlien;
        this.namaKlien = namaKlien;
        this.jadwal = jadwal;
        this.lokasi = lokasi;
        this.deadline = deadline;
        this.statusPengerjaan = statusPengerjaan;
        this.tautanHasil = tautanHasil;
        this.daftarDetail = new ArrayList<>();
        this.daftarPembayaran = new ArrayList<>();
    }

    public void tambahDetail(DetailPesanan detail) {
        this.daftarDetail.add(detail);
    }

    public void tambahPembayaran(Pembayaran bayar) {
        this.daftarPembayaran.add(bayar);
    }

    public double hitungTotalBiaya() {
        return daftarDetail.stream().mapToDouble(DetailPesanan::getHargaKesepakatan).sum();
    }

    public double hitungTotalTerbayar() {
        return daftarPembayaran.stream().mapToDouble(Pembayaran::getNominal).sum();
    }

    public double hitungSisaTagihan() {
        double sisa = hitungTotalBiaya() - hitungTotalTerbayar();
        return Math.max(0, sisa);
    }

    public String getStatusTagihan() {
        Locale id = new Locale("id", "ID");
        NumberFormat nf = NumberFormat.getCurrencyInstance(id);
        double sisa = hitungSisaTagihan();
        if (sisa <= 0 && hitungTotalBiaya() > 0) {
            return "Lunas";
        }
        return "Sisa: " + nf.format(sisa);
    }

    public String getIdPesanan() { return idPesanan; }
    public String getIdKlien() { return idKlien; }
    public String getNamaKlien() { return namaKlien; }
    public LocalDate getJadwal() { return jadwal; }
    public String getLokasi() { return lokasi; }
    public LocalDate getDeadline() { return deadline; }
    public String getStatusPengerjaan() { return statusPengerjaan; }
    public void setStatusPengerjaan(String statusPengerjaan) { this.statusPengerjaan = statusPengerjaan; }
    public String getTautanHasil() { return tautanHasil; }
    public void setTautanHasil(String tautanHasil) { this.tautanHasil = tautanHasil; }
    public List<DetailPesanan> getDaftarDetail() { return daftarDetail; }
    public List<Pembayaran> getDaftarPembayaran() { return daftarPembayaran; }
}