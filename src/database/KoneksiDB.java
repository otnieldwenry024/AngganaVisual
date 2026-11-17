package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class KoneksiDB {
    private static final String URL = System.getenv().getOrDefault("AV_DB_URL", "jdbc:mysql://localhost:3306/anggana_visual_db");
    private static final String USER = System.getenv().getOrDefault("AV_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("AV_DB_PASSWORD", ""); // Biarkan kosong jika XAMPP default

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}