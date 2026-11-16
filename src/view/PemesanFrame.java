package view;

import database.PesananDAO;
import model.DetailPesanan;
import model.Klien;
import model.Pesanan;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class PemesanFrame extends JFrame {
    private final PesananDAO pesananDAO = new PesananDAO();
    private final Klien klienLogin;

    private JComboBox<String> cbLayanan;
    private JTextField txtJadwal, txtDeadline, txtLokasi, txtHarga, txtKebutuhan;
    private DefaultTableModel tableModel;

    public PemesanFrame(Klien klienLogin) {
        this.klienLogin = klienLogin;
        setTitle("Anggana Visual - Halaman Pemesan (" + klienLogin.getNamaKlien() + ")");
        setSize(1050, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initUI();
        muatDropdownLayanan();
        muatDataTabel();
    }

    private void initUI() {
        // HEADER + LOGOUT
        JPanel panelAtas = new JPanel(new BorderLayout());
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));

        JLabel lblSalam = new JLabel("Selamat datang, " + klienLogin.getNamaKlien() + " (" + klienLogin.getIdKlien() + ")");
        lblSalam.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelHeader.add(lblSalam, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        btnLogout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        panelHeader.add(btnLogout, BorderLayout.EAST);
        panelAtas.add(panelHeader, BorderLayout.NORTH);

        // FORM BUAT PESANAN PEMESAN
        JPanel panelForm = new JPanel(new GridLayout(4, 4, 8, 8));
        panelForm.setBorder(BorderFactory.createTitledBorder("Buat Pesanan Baru"));

        panelForm.add(new JLabel("Pilih Layanan:"));
        cbLayanan = new JComboBox<>();
        panelForm.add(cbLayanan);

        panelForm.add(new JLabel("Lokasi Acara:"));
        txtLokasi = new JTextField();
        panelForm.add(txtLokasi);

        panelForm.add(new JLabel("Jadwal Acara (YYYY-MM-DD):"));
        txtJadwal = new JTextField("2026-05-15");
        panelForm.add(txtJadwal);

        panelForm.add(new JLabel("Batas Deadline (YYYY-MM-DD):"));
        txtDeadline = new JTextField("2026-05-22");
        panelForm.add(txtDeadline);

        panelForm.add(new JLabel("Harga Kesepakatan (Rp):"));
        txtHarga = new JTextField();
        panelForm.add(txtHarga);

        panelForm.add(new JLabel("Rincian Kebutuhan:"));
        txtKebutuhan = new JTextField();
        panelForm.add(txtKebutuhan);

        panelForm.add(new JLabel(""));
        JButton btnKirim = new JButton("+ Simpan & Kirim Pesanan ke Admin");
        btnKirim.setBackground(new Color(40, 167, 69));
        btnKirim.setForeground(Color.WHITE);
        btnKirim.addActionListener(e -> aksiKirimPesanan());
        panelForm.add(btnKirim);

        panelAtas.add(panelForm, BorderLayout.CENTER);
        add(panelAtas, BorderLayout.NORTH);

        // TABEL PEMANTAUAN PESANAN MILIK PEMESAN
        String[] kolom = {"ID Pesanan", "Jadwal", "Deadline", "Lokasi", "Total Biaya", "Status Tagihan", "Status Pengerjaan", "Link Google Drive"};
        tableModel = new DefaultTableModel(kolom, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tabel = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(tabel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Pesanan Saya & Status Pengerjaan"));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void muatDropdownLayanan() {
        cbLayanan.removeAllItems();
        for (String item : pesananDAO.getDaftarLayanan()) {
            cbLayanan.addItem(item);
        }
    }

    private void muatDataTabel() {
        tableModel.setRowCount(0);
        List<Pesanan> list = pesananDAO.getPesananByKlien(klienLogin.getIdKlien());
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        for (Pesanan p : list) {
            tableModel.addRow(new Object[]{
                    p.getIdPesanan(),
                    p.getJadwal(),
                    p.getDeadline(),
                    p.getLokasi(),
                    nf.format(p.hitungTotalBiaya()),
                    p.getStatusTagihan(),
                    p.getStatusPengerjaan(),
                    (p.getTautanHasil() == null || p.getTautanHasil().isEmpty()) ? "Belum Tersedia" : p.getTautanHasil()
            });
        }
    }

    private void aksiKirimPesanan() {
        try {
            if (cbLayanan.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Layanan belum tersedia!");
                return;
            }
            String idPesanan = "P-" + System.currentTimeMillis() % 10000;
            String idDetail = "D-" + System.currentTimeMillis() % 10000;
            String idLayanan = cbLayanan.getSelectedItem().toString().split(" - ")[0];
            LocalDate tglJadwal = LocalDate.parse(txtJadwal.getText().trim());
            LocalDate tglDeadline = LocalDate.parse(txtDeadline.getText().trim());
            String lokasi = txtLokasi.getText().trim();
            double harga = Double.parseDouble(txtHarga.getText().trim());
            String kebutuhan = txtKebutuhan.getText().trim();

            Pesanan p = new Pesanan(idPesanan, klienLogin.getIdKlien(), klienLogin.getNamaKlien(),
                    tglJadwal, lokasi, tglDeadline, "Terjadwal", null);
            DetailPesanan d = new DetailPesanan(idDetail, idPesanan, idLayanan, kebutuhan, harga);

            if (pesananDAO.simpanPesananBaru(p, d)) {
                JOptionPane.showMessageDialog(this, "Pesanan " + idPesanan + " berhasil dikirim ke Admin!");
                muatDataTabel();
                txtLokasi.setText("");
                txtHarga.setText("");
                txtKebutuhan.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Gagal mengirim pesanan.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Format input salah: " + ex.getMessage());
        }
    }
}