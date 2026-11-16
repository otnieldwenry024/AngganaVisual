package model;

public class Klien {
    private String idKlien;
    private String namaKlien;
    private String kontak;
    private String username;

    public Klien(String idKlien, String namaKlien, String kontak, String username) {
        this.idKlien = idKlien;
        this.namaKlien = namaKlien;
        this.kontak = kontak;
        this.username = username;
    }

    public String getIdKlien() { return idKlien; }
    public String getNamaKlien() { return namaKlien; }
    public String getKontak() { return kontak; }
    public String getUsername() { return username; }
}