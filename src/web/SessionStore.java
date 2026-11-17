package web;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionStore {
    public static final long TTL_SECONDS = 8 * 60 * 60;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    public static final class Session {
        public final String id, userId, name, role, csrf;
        private final Instant expires = Instant.now().plusSeconds(TTL_SECONDS);
        private Session(String userId, String name, String role) {
            this.id = token(); this.csrf = token(); this.userId = userId; this.name = name; this.role = role;
        }
        public boolean isAdmin() { return "admin".equals(role); }
        public Map<String, String> publicData() {
            return Map.of("name", name, "role", role, "csrf", csrf);
        }
    }
    public Session create(String userId, String name, String role) {
        sessions.entrySet().removeIf(e -> e.getValue().expires.isBefore(Instant.now()));
        Session session = new Session(userId, name, role);
        sessions.put(session.id, session);
        return session;
    }
    public Session get(String id) {
        if (id == null) return null;
        Session s = sessions.get(id);
        if (s != null && s.expires.isBefore(Instant.now())) { sessions.remove(id); return null; }
        return s;
    }
    public void remove(String id) { if (id != null) sessions.remove(id); }
    private static String token() {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
