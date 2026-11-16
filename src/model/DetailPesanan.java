package model;

public class DetailPesanan {
    private String idDetail;
    private String idPesanan;
    private String idLayanan;
    private String kebutuhan;
    private double hargaKesepakatan;

    public DetailPesanan(String idDetail, String idPesanan, String idLayanan, String kebutuhan, double hargaKesepakatan) {
        this.idDetail = idDetail;
        this.idPesanan = idPesanan;
        this.idLayanan = idLayanan;
        this.kebutuhan = kebutuhan;
        this.hargaKesepakatan = hargaKesepakatan;
    }

    public String getIdDetail() { return idDetail; }
    public String getIdPesanan() { return idPesanan; }
    public String getIdLayanan() { return idLayanan; }
    public String getKebutuhan() { return kebutuhan; }
    public double getHargaKesepakatan() { return hargaKesepakatan; }
}