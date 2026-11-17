package view;

import database.PesananDAO;
import model.DetailPesanan;
import model.Pembayaran;
import model.Pesanan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class MainFrame extends JFrame {
    private final PesananDAO pesananDAO = new PesananDAO();
    private List<Pesanan> daftarPesananAktif;
    private final String namaAdmin;

    private JComboBox<String> cbKlien, cbLayanan;
    private JTextField txtLokasi, txtJadwal, txtDeadline, txtHarga, txtKebutuhan;
    private JButton btnTambahKlien, btnSimpan;

    private JTable tabelPesanan;
    private DefaultTableModel tableModel;
    private JButton btnUbahStatus, btnBayar, btnLinkDrive;

    public MainFrame() {
        this("Admin");
    }

    public MainFrame(String namaAdmin) {
        this.namaAdmin = namaAdmin;
        setTitle("Anggana Visual - Panel Admin (" + namaAdmin + ")");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initUI();
        muatDropdown(null);
        muatDataTabel();
    }

    private void initUI() {
        JPanel panelAtas = new JPanel(new BorderLayout());

        // HEADER ADMIN + LOGOUT
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel lblAdmin = new JLabel("Admin Aktif: " + namaAdmin);
        lblAdmin.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelHeader.add(lblAdmin, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        panelHeader.add(btnLogout, BorderLayout.EAST);
        panelAtas.add(panelHeader, BorderLayout.NORTH);

        // 1. FORM INPUT ATAS
        JPanel panelForm = new JPanel(new GridLayout(4, 4, 8, 8));
        panelForm.setBorder(BorderFactory.createTitledBorder("Tahap 1: Input Pesanan Baru"));

        panelForm.add(new JLabel("Pilih Klien:"));
        JPanel panelKlien = new JPanel(new BorderLayout(4, 0));
        cbKlien = new JComboBox<>();
        btnTambahKlien = new JButton("+ Klien");
        btnTambahKlien.addActionListener(e -> aksiTambahKlien());
        panelKlien.add(cbKlien, BorderLayout.CENTER);
        panelKlien.add(btnTambahKlien, BorderLayout.EAST);
        panelForm.add(panelKlien);

        panelForm.add(new JLabel("Pilih Layanan:"));
        cbLayanan = new JComboBox<>();
        panelForm.add(cbLayanan);

        panelForm.add(new JLabel("Jadwal Acara (YYYY-MM-DD):"));
        txtJadwal = new JTextField("2026-05-01");
        panelForm.add(txtJadwal);

        panelForm.add(new JLabel("Batas Deadline (YYYY-MM-DD):"));
        txtDeadline = new JTextField("2026-05-08");
        panelForm.add(txtDeadline);

        panelForm.add(new JLabel("Lokasi Acara:"));
        txtLokasi = new JTextField();
        panelForm.add(txtLokasi);

        panelForm.add(new JLabel("Harga Kesepakatan (Rp):"));
        txtHarga = new JTextField();
        panelForm.add(txtHarga);

        panelForm.add(new JLabel("Rincian Kebutuhan:"));
        txtKebutuhan = new JTextField();
        panelForm.add(txtKebutuhan);

        btnSimpan = new JButton("+ Simpan Pesanan");
        btnSimpan.setBackground(new Color(40, 167, 69));
        btnSimpan.setForeground(Color.WHITE);
        btnSimpan.addActionListener(e -> aksiSimpanPesanan());
        panelForm.add(btnSimpan);

        panelAtas.add(panelForm, BorderLayout.CENTER);
        add(panelAtas, BorderLayout.NORTH);

        // 2. TABEL DAFTAR PESANAN TENGAH
        String[] kolom = {"ID Pesanan", "Klien", "Jadwal", "Lokasi", "Total Biaya", "Status Tagihan", "Status Pengerjaan", "Link Google Drive"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tabelPesanan = new JTable(tableModel);
        tabelPesanan.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollTable = new JScrollPane(tabelPesanan);
        scrollTable.setBorder(BorderFactory.createTitledBorder("Tahap 2: Daftar Pesanan & Status Tagihan"));
        add(scrollTable, BorderLayout.CENTER);

        // 3. TOMBOL AKSI ADMIN BAWAH
        JPanel panelAksi = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        panelAksi.setBorder(BorderFactory.createTitledBorder("Tahap 3: Aksi Admin"));

        btnUbahStatus = new JButton("Status (Ubah Alur)");
        btnBayar = new JButton("+ Bayar (DP / Pelunasan)");
        btnLinkDrive = new JButton("+ Link Google Drive");

        btnUbahStatus.addActionListener(e -> aksiUbahStatus());
        btnBayar.addActionListener(e -> aksiTambahBayar());
        btnLinkDrive.addActionListener(e -> aksiInputLinkDrive());

        panelAksi.add(btnUbahStatus);
        panelAksi.add(btnBayar);
        panelAksi.add(btnLinkDrive);
        add(panelAksi, BorderLayout.SOUTH);
    }

    private void muatDropdown(String pilihIdKlien) {
        cbKlien.removeAllItems();
        for (String item : pesananDAO.getDaftarKlien()) {
            cbKlien.addItem(item);
            if (pilihIdKlien != null && item.startsWith(pilihIdKlien + " - ")) {
                cbKlien.setSelectedItem(item);
            }
        }

        cbLayanan.removeAllItems();
        for (String item : pesananDAO.getDaftarLayanan()) {
            cbLayanan.addItem(item);
        }
    }

    public void muatDataTabel() {
        tableModel.setRowCount(0);
        daftarPesananAktif = pesananDAO.getSemuaPesanan();
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        for (Pesanan p : daftarPesananAktif) {
            tableModel.addRow(new Object[]{
                    p.getIdPesanan(),
                    p.getNamaKlien(),
                    p.getJadwal(),
                    p.getLokasi(),
                    nf.format(p.hitungTotalBiaya()),
                    p.getStatusTagihan(),
                    p.getStatusPengerjaan(),
                    (p.getTautanHasil() == null || p.getTautanHasil().isEmpty()) ? "Belum Diisi" : p.getTautanHasil()
            });
        }
    }

    private void aksiTambahKlien() {
        JTextField txtNama = new JTextField();
        JTextField txtKontak = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Nama Klien:"));
        panel.add(txtNama);
        panel.add(new JLabel("No. HP / Kontak:"));
        panel.add(txtKontak);

        int hasil = JOptionPane.showConfirmDialog(this, panel, "Tambah Klien Baru", JOptionPane.OK_CANCEL_OPTION);
        if (hasil == JOptionPane.OK_OPTION) {
            String nama = txtNama.getText().trim();
            String kontak = txtKontak.getText().trim();
            if (nama.isEmpty() || kontak.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama dan kontak wajib diisi!");
                return;
            }
            String idBaru = pesananDAO.tambahKlienBaru(nama, kontak);
            if (idBaru != null) {
                JOptionPane.showMessageDialog(this, "Klien baru berhasil didaftarkan dengan ID: " + idBaru);
                muatDropdown(idBaru);
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menambahkan klien baru.");
            }
        }
    }

    private void aksiSimpanPesanan() {
        try {
            if (cbKlien.getSelectedItem() == null || cbLayanan.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Pilih klien dan layanan terlebih dahulu!");
                return;
            }

            String idPesanan = "P-" + System.currentTimeMillis() % 10000;
            String idDetail = "D-" + System.currentTimeMillis() % 10000;
            String idKlien = cbKlien.getSelectedItem().toString().split(" - ")[0];
            String idLayanan = cbLayanan.getSelectedItem().toString().split(" - ")[0];
            LocalDate tglJadwal = LocalDate.parse(txtJadwal.getText().trim());
            LocalDate tglDeadline = LocalDate.parse(txtDeadline.getText().trim());
            String lokasi = txtLokasi.getText().trim();
            double harga = Double.parseDouble(txtHarga.getText().trim());
            String kebutuhan = txtKebutuhan.getText().trim();

            Pesanan p = new Pesanan(idPesanan, idKlien, "", tglJadwal, lokasi, tglDeadline, "Terjadwal", null);
            DetailPesanan d = new DetailPesanan(idDetail, idPesanan, idLayanan, kebutuhan, harga);

            if (pesananDAO.simpanPesananBaru(p, d)) {
                JOptionPane.showMessageDialog(this, "Pesanan " + idPesanan + " berhasil disimpan!");
                muatDataTabel();
                txtLokasi.setText("");
                txtHarga.setText("");
                txtKebutuhan.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menyimpan pesanan.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format input salah: " + ex.getMessage());
        }
    }

    private void aksiUbahStatus() {
        int baris = tabelPesanan.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu baris pesanan di tabel terlebih dahulu!");
            return;
        }

        Pesanan p = daftarPesananAktif.get(baris);
        String[] pilihanStatus = {"Terjadwal", "Produksi", "Editing", "Selesai"};
        String statusBaru = (String) JOptionPane.showInputDialog(
                this, "Pilih status pengerjaan saat ini:",
                "Ubah Status Pengerjaan", JOptionPane.QUESTION_MESSAGE,
                null, pilihanStatus, p.getStatusPengerjaan()
        );

        if (statusBaru != null && !statusBaru.equals(p.getStatusPengerjaan())) {
            if (pesananDAO.updateStatus(p.getIdPesanan(), statusBaru)) {
                JOptionPane.showMessageDialog(this, "Status berhasil diperbarui menjadi: " + statusBaru);
                muatDataTabel();
            }
        }
    }

    private void aksiTambahBayar() {
        int baris = tabelPesanan.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu baris pesanan di tabel terlebih dahulu!");
            return;
        }

        Pesanan p = daftarPesananAktif.get(baris);
        String nominalStr = JOptionPane.showInputDialog(this,
                "Masukkan nominal pembayaran klien untuk " + p.getIdPesanan() + "\n(" + p.getStatusTagihan() + "):");

        if (nominalStr != null && !nominalStr.trim().isEmpty()) {
            try {
                double nominal = Double.parseDouble(nominalStr.trim());
                String idPay = "PAY-" + System.currentTimeMillis() % 10000;
                Pembayaran bayar = new Pembayaran(idPay, p.getIdPesanan(), LocalDate.now(), nominal, "Transfer Bank");

                if (pesananDAO.inputPembayaran(bayar)) {
                    JOptionPane.showMessageDialog(this, "Pembayaran sebesar Rp " + nominal + " berhasil dicatat!");
                    muatDataTabel();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Nominal harus berupa angka!");
            }
        }
    }

    private void aksiInputLinkDrive() {
        int baris = tabelPesanan.getSelectedRow();
        if (baris == -1) {
            JOptionPane.showMessageDialog(this, "Pilih salah satu baris pesanan di tabel terlebih dahulu!");
            return;
        }

        Pesanan p = daftarPesananAktif.get(baris);
        String link = JOptionPane.showInputDialog(this,
                "Masukkan URL Google Drive hasil akhir untuk " + p.getIdPesanan() + ":",
                p.getTautanHasil() == null ? "https://drive.google.com/" : p.getTautanHasil());

        if (link != null && !link.trim().isEmpty()) {
            if (pesananDAO.updateTautanHasil(p.getIdPesanan(), link.trim())) {
                JOptionPane.showMessageDialog(this, "Tautan Google Drive berhasil disimpan!");
                muatDataTabel();
            }
        }
    }

    public static void main(String[] args) {
        // Semua peluncuran aplikasi harus dimulai dari halaman login.
        LoginFrame.main(args);
    }
}
