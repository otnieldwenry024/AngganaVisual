package security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Akun baru memakai PBKDF2; akun lama tetap bisa masuk tanpa mengubah data kelompok. */
public final class Passwords {
    private static final int ITERATIONS = 210_000;
    private Passwords() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        if (!stored.startsWith("pbkdf2$")) {
            return MessageDigest.isEqual(password.getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
        }
        try {
            String[] parts = stored.split("\\$");
            int iterations = Integer.parseInt(parts[1]);
            if (parts.length != 4 || iterations < 1 || iterations > 1_000_000) return false;
            return MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(parts[2]), iterations),
                    Base64.getDecoder().decode(parts[3]));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Hash password tidak tersedia.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
