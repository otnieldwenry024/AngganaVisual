package view;

import database.PesananDAO;
import model.Klien;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final PesananDAO pesananDAO = new PesananDAO();
    private JTextField txtUsername;
    private JPasswordField txtPassword;

    public LoginFrame() {
        setTitle("Anggana Visual - Login");
        setSize(400, 260);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JLabel lblJudul = new JLabel("ANGGANA VISUAL", SwingConstants.CENTER);
        lblJudul.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(lblJudul, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 12));
        formPanel.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Password:"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);
        panel.add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 6, 8));
        JButton btnLogin = new JButton("Login");
        btnLogin.setBackground(new Color(0, 123, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.addActionListener(e -> aksiLogin());

        JButton btnDaftar = new JButton("Daftar Akun Pemesan Baru");
        btnDaftar.addActionListener(e -> aksiDaftar());

        btnPanel.add(btnLogin);
        btnPanel.add(btnDaftar);
        panel.add(btnPanel, BorderLayout.SOUTH);

        add(panel);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void aksiLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan password wajib diisi!");
            return;
        }

        // 1. Cek sebagai Admin
        String namaAdmin = pesananDAO.loginAdmin(user, pass);
        if (namaAdmin != null) {
            new MainFrame(namaAdmin).setVisible(true);
            dispose();
            return;
        }

        // 2. Cek sebagai Pemesan (Klien)
        Klien klien = pesananDAO.loginKlien(user, pass);
        if (klien != null) {
            new PemesanFrame(klien).setVisible(true);
            dispose();
            return;
        }

        JOptionPane.showMessageDialog(this, "Username atau password salah!", "Login Gagal", JOptionPane.ERROR_MESSAGE);
    }

    private void aksiDaftar() {
        JTextField txtNama = new JTextField();
        JTextField txtKontak = new JTextField();
        JTextField txtUserBaru = new JTextField();
        JPasswordField txtPassBaru = new JPasswordField();

        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 6));
        panel.add(new JLabel("Nama Lengkap:"));
        panel.add(txtNama);
        panel.add(new JLabel("No. HP / Kontak:"));
        panel.add(txtKontak);
        panel.add(new JLabel("Username:"));
        panel.add(txtUserBaru);
        panel.add(new JLabel("Password:"));
        panel.add(txtPassBaru);

        int hasil = JOptionPane.showConfirmDialog(this, panel, "Daftar Akun Pemesan Baru", JOptionPane.OK_CANCEL_OPTION);
        if (hasil == JOptionPane.OK_OPTION) {
            String nama = txtNama.getText().trim();
            String kontak = txtKontak.getText().trim();
            String user = txtUserBaru.getText().trim();
            String pass = new String(txtPassBaru.getPassword()).trim();

            if (nama.isEmpty() || kontak.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Semua kolom wajib diisi!");
                return;
            }

            String res = pesananDAO.daftarKlienBaru(nama, kontak, user, pass);
            if ("DUPLICATE".equals(res)) {
                JOptionPane.showMessageDialog(this, "Username sudah dipakai, gunakan username lain!");
            } else if (res != null) {
                JOptionPane.showMessageDialog(this, "Pendaftaran berhasil! ID Klien Anda: " + res + "\nSilakan login.");
                txtUsername.setText(user);
                txtPassword.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "Gagal mendaftarkan akun.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}