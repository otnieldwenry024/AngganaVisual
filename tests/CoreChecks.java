import security.Passwords;
import web.Json;
import web.SessionStore;
import java.util.Map;

public class CoreChecks {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        String hash = Passwords.hash("tes Password123");
        check(!hash.equals(Passwords.hash("tes Password123")));
        check(Passwords.verify("tes Password123", hash));
        check(!Passwords.verify("salah", hash));
        check(Passwords.verify("legacy", "legacy"));
        check(!Passwords.verify("salah", "legacy"));
        check(!Passwords.verify("x", "pbkdf2$invalid"));
        check(!Passwords.verify("x", null));
        check(Json.encode(Map.of("text", "\"\n\\\t\u0001")).equals("{\"text\":\"\\\"\\n\\\\\\t\\u0001\"}"));
        SessionStore store = new SessionStore();
        SessionStore.Session session = store.create("K-1", "Uji", "klien");
        check(store.get(session.id) == session && !session.isAdmin());
        check(session.csrf.length() > 30 && !session.id.equals(session.csrf));
        store.remove(session.id); check(store.get(session.id) == null);
        check(store.get(null) == null);
        System.out.println("Core checks passed: hashing, compatibility, JSON escaping, session lifecycle.");
    }
}
