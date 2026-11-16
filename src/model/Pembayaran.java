package model;

import java.time.LocalDate;

public class Pembayaran {
    private String idPembayaran;
    private String idPesanan;
    private LocalDate tanggalBayar;
    private double nominal;
    private String metodePembayaran;

    public Pembayaran(String idPembayaran, String idPesanan, LocalDate tanggalBayar, double nominal, String metodePembayaran) {
        this.idPembayaran = idPembayaran;
        this.idPesanan = idPesanan;
        this.tanggalBayar = tanggalBayar;
        this.nominal = nominal;
        this.metodePembayaran = metodePembayaran;
    }

    public String getIdPembayaran() { return idPembayaran; }
    public String getIdPesanan() { return idPesanan; }
    public LocalDate getTanggalBayar() { return tanggalBayar; }
    public double getNominal() { return nominal; }
    public String getMetodePembayaran() { return metodePembayaran; }
}