package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import service.WebService;
import web.SessionStore.Session;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;

/** Entry point web. Jalankan kelas ini, lalu buka http://localhost:8080. */
public final class WebServer {
    private final WebService service = new WebService();
    private final SessionStore sessions = new SessionStore();
    private final Path root;
    private final int port;
    private final Map<String, long[]> attempts = new HashMap<>();

    private WebServer(Path root, int port) { this.root = root; this.port = port; }
    public static void main(String[] args) throws IOException {
        Path root = Paths.get(System.getenv().getOrDefault("AV_WEB_ROOT", "web")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(root.resolve("index.html")))
            throw new IllegalStateException("Folder web tidak ditemukan. Jalankan dari folder utama AngganaVisual-main.");
        int port = Integer.parseInt(System.getenv().getOrDefault("AV_PORT", "8080"));
        WebServer app = new WebServer(root, port);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", app::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
        server.start();
        System.out.println("Anggana Visual Web: http://localhost:" + port);
        System.out.println("Hentikan dengan tombol Stop atau Ctrl+C.");
    }

    private void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("X-Frame-Options", "DENY");
        ex.getResponseHeaders().set("Referrer-Policy", "same-origin");
        ex.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        try {
            String host = ex.getRequestHeaders().getFirst("Host");
            if (!("localhost:" + port).equals(host) && !("127.0.0.1:" + port).equals(host)) {
                json(ex, 403, Map.of("error", "Host tidak diizinkan.")); return;
            }
            if (ex.getRequestURI().getPath().startsWith("/api/")) api(ex);
            else asset(ex);
        } catch (IllegalArgumentException e) { json(ex, 400, Map.of("error", e.getMessage()));
        } catch (SecurityException e) { json(ex, 403, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            json(ex, 503, Map.of("error", "Database belum dapat digunakan. Pastikan MySQL aktif, driver tersedia, dan tabel Anggana Visual sudah diimpor."));
        } finally { ex.close(); }
    }

    private void api(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath(), method = ex.getRequestMethod();
        Session user = sessions.get(cookie(ex));
        if ("GET".equals(method)) {
            if (user == null) { json(ex, 401, Map.of("error", "Silakan masuk terlebih dahulu.")); return; }
            switch (path) {
                case "/api/me": json(ex, 200, user.publicData()); return;
                case "/api/data": json(ex, 200, service.data(user)); return;
                default: json(ex, 404, Map.of("error", "Halaman tidak ditemukan.")); return;
            }
        }
        if (!"POST".equals(method)) { ex.getResponseHeaders().set("Allow", "GET, POST"); json(ex, 405, Map.of("error", "Metode tidak diizinkan.")); return; }
        String origin = ex.getRequestHeaders().getFirst("Origin");
        if (origin == null || !(origin.equals("http://localhost:" + port) || origin.equals("http://127.0.0.1:" + port)))
            throw new SecurityException("Asal permintaan tidak diizinkan. Buka aplikasi dari localhost.");
        Map<String, String> form = form(ex);
        if (path.equals("/api/login") || path.equals("/api/register")) {
            if (!allowAttempt(ex.getRemoteAddress().getAddress().getHostAddress())) {
                json(ex, 429, Map.of("error", "Terlalu banyak percobaan. Coba lagi dalam satu menit.")); return;
            }
            if (path.equals("/api/register")) {
                service.register(form); json(ex, 201, Map.of("message", "Akun berhasil dibuat. Silakan masuk.")); return;
            }
            Map<String, String> identity = service.login(form);
            if (identity == null) { json(ex, 401, Map.of("error", "Username atau password salah.")); return; }
            sessions.remove(cookie(ex));
            Session session = sessions.create(identity.get("id"), identity.get("name"), identity.get("role"));
            ex.getResponseHeaders().add("Set-Cookie", "AVSESSION=" + session.id + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=" + SessionStore.TTL_SECONDS);
            json(ex, 200, session.publicData()); return;
        }
        if (user == null) { json(ex, 401, Map.of("error", "Sesi berakhir. Silakan masuk kembali.")); return; }
        if (!user.csrf.equals(ex.getRequestHeaders().getFirst("X-CSRF-Token"))) throw new SecurityException("Sesi tidak valid. Muat ulang halaman.");
        switch (path) {
            case "/api/logout":
                sessions.remove(user.id);
                ex.getResponseHeaders().add("Set-Cookie", "AVSESSION=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
                json(ex, 200, Map.of("message", "Berhasil keluar.")); return;
            case "/api/orders":
                String id = service.createOrder(user, form);
                json(ex, 201, Map.of("message", "Pesanan berhasil dibuat.", "id", id)); return;
            case "/api/clients": service.createClient(user, form); break;
            case "/api/status": service.changeStatus(user, form); break;
            case "/api/payments": service.pay(user, form); break;
            case "/api/result": service.result(user, form); break;
            default: json(ex, 404, Map.of("error", "Halaman tidak ditemukan.")); return;
        }
        json(ex, 200, Map.of("message", "Perubahan berhasil disimpan."));
    }

    private synchronized boolean allowAttempt(String ip) {
        long now = System.currentTimeMillis();
        long[] window = attempts.get(ip);
        if (window == null || now - window[0] > 60_000) { window = new long[]{now, 0}; attempts.put(ip, window); }
        return ++window[1] <= 15;
    }
    private Map<String, String> form(HttpExchange ex) throws IOException {
        String type = ex.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.startsWith("application/x-www-form-urlencoded")) throw new IllegalArgumentException("Format permintaan tidak didukung.");
        byte[] bytes = ex.getRequestBody().readNBytes(16_385);
        if (bytes.length > 16_384) throw new IllegalArgumentException("Data terlalu besar.");
        Map<String, String> result = new HashMap<>();
        for (String item : new String(bytes, StandardCharsets.UTF_8).split("&")) {
            String[] pair = item.split("=", 2);
            result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }
    private String cookie(HttpExchange ex) {
        for (String header : ex.getRequestHeaders().getOrDefault("Cookie", List.of()))
            for (String item : header.split(";")) if (item.trim().startsWith("AVSESSION=")) return item.trim().substring(10);
        return null;
    }
    private void asset(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod()) && !"HEAD".equals(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Allow", "GET, HEAD"); json(ex, 405, Map.of("error", "Metode tidak diizinkan.")); return;
        }
        String path = ex.getRequestURI().getPath();
        // Hanya aset publik yang boleh dibaca, bukan file Java, SQL, atau konfigurasi.
        Map<String, String> assets = Map.of("/", "index.html", "/index.html", "index.html", "/app", "app.html", "/app.html", "app.html", "/app/orders", "app.html", "/style.css", "style.css", "/app.js", "app.js", "/login.js", "login.js");
        if (!assets.containsKey(path)) { json(ex, 404, Map.of("error", "Halaman tidak ditemukan.")); return; }
        if ((path.equals("/app") || path.equals("/app.html") || path.equals("/app/orders")) && sessions.get(cookie(ex)) == null) {
            ex.getResponseHeaders().set("Location", "/"); ex.sendResponseHeaders(302, -1); return;
        }
        Path file = root.resolve(assets.get(path));
        String type = path.endsWith(".css") ? "text/css" : path.endsWith(".js") ? "application/javascript" : "text/html";
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        if (ex.getRequestMethod().equals("HEAD")) { ex.sendResponseHeaders(200, -1); return; }
        byte[] bytes = Files.readAllBytes(file);
        ex.sendResponseHeaders(200, bytes.length); ex.getResponseBody().write(bytes);
    }
    private static void json(HttpExchange ex, int status, Object data) throws IOException {
        byte[] bytes = Json.encode(data).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length); ex.getResponseBody().write(bytes);
    }
}
