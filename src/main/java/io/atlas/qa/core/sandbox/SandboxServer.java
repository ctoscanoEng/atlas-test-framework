package io.atlas.qa.core.sandbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.atlas.qa.core.config.ConfigLoader;
import io.atlas.qa.core.exception.AtlasException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * The application under test, hosted by the framework itself.
 *
 * <h2>Why this exists</h2>
 * Portfolio automation projects usually target a public demo site. The day that
 * site is slow, rate-limited or redesigned, the whole suite goes red for reasons
 * that have nothing to do with the code. ATLAS ships its own application under
 * test — a static SPA plus a small JSON API — served in-process by the JDK HTTP
 * server (no extra dependency, no container, no network).
 *
 * <p>Consequences:
 * <ul>
 *   <li>the suite runs offline, on a plane or on a locked-down CI agent;</li>
 *   <li>results are deterministic, so a red build always means a real defect;</li>
 *   <li>the port is ephemeral by default, so parallel jobs never collide;</li>
 *   <li>the markup deliberately randomises some attributes at every load,
 *       which is what makes the self-healing locator engine observable.</li>
 * </ul>
 */
public final class SandboxServer {

    private static final Logger LOG = LogManager.getLogger(SandboxServer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CLASSPATH_ROOT = "sut-app";

    private static volatile SandboxServer instance;

    private final HttpServer server;
    private final String baseUrl;

    private SandboxServer(int requestedPort) {
        String bindAddress = ConfigLoader.get("sandbox.bindAddress", "127.0.0.1");
        String advertisedHost = ConfigLoader.get("sandbox.advertisedHost", "127.0.0.1");
        try {
            this.server = HttpServer.create(new InetSocketAddress(bindAddress, requestedPort), 0);
            this.server.createContext("/api/", this::handleApi);
            this.server.createContext("/", this::handleStatic);
            this.server.setExecutor(Executors.newFixedThreadPool(8, runnable -> {
                Thread thread = new Thread(runnable, "atlas-sandbox-http");
                thread.setDaemon(true);
                return thread;
            }));
            this.server.start();
            // Bind address and advertised host are separate on purpose: when the
            // browsers run in other containers, the server must listen on 0.0.0.0
            // while the tests must hand the browsers a hostname those containers
            // can actually resolve.
            this.baseUrl = "http://%s:%d".formatted(advertisedHost, server.getAddress().getPort());
            LOG.info("Sandbox application under test started on {}", baseUrl);
        } catch (IOException e) {
            throw new AtlasException("Unable to start the sandbox application under test", e);
        }
    }

    /** Lazily starts the server on first use; safe to call from every test thread. */
    public static SandboxServer instance() {
        SandboxServer local = instance;
        if (local == null) {
            synchronized (SandboxServer.class) {
                local = instance;
                if (local == null) {
                    int port = ConfigLoader.getInt("sandbox.port", 0);
                    local = new SandboxServer(port);
                    instance = local;
                    Runtime.getRuntime().addShutdownHook(new Thread(local::stop, "atlas-sandbox-shutdown"));
                }
            }
        }
        return local;
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String url(String path) {
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    public void stop() {
        server.stop(0);
        LOG.info("Sandbox application under test stopped");
    }

    // ------------------------------------------------------------------ handlers

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = URI.create(exchange.getRequestURI().getPath()).normalize().getPath();
        if (path.contains("..")) {                       // defensive: no traversal outside the app
            respond(exchange, 400, "text/plain", "Bad request".getBytes(StandardCharsets.UTF_8));
            return;
        }
        if ("/".equals(path)) {
            path = "/index.html";
        }
        byte[] body = readClasspath(CLASSPATH_ROOT + path);
        if (body == null) {
            respond(exchange, 404, "text/html", """
                    <!doctype html><html lang="en"><body>
                    <h1 data-testid="not-found">404 — page not found</h1>
                    </body></html>""".getBytes(StandardCharsets.UTF_8));
            return;
        }
        respond(exchange, 200, contentTypeOf(path), body);
    }

    /**
     * Minimal JSON API so the REST layer of the framework is exercised offline
     * as well. It mirrors the contract of the UI: same users, same catalogue.
     */
    private void handleApi(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && "/api/products".equals(path)) {
                serveJsonResource(exchange, "products.json");
            } else if ("GET".equals(method) && "/api/health".equals(path)) {
                json(exchange, 200, Map.of("status", "UP", "component", "atlas-sandbox"));
            } else if ("POST".equals(method) && "/api/auth/login".equals(path)) {
                handleLogin(exchange);
            } else if ("GET".equals(method) && path.startsWith("/api/products/")) {
                handleProductById(exchange, path.substring("/api/products/".length()));
            } else {
                json(exchange, 404, Map.of("error", "no route for " + method + " " + path));
            }
        } catch (RuntimeException e) {
            LOG.error("Sandbox API failure on {} {}", method, path, e);
            json(exchange, 500, Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonNode body = MAPPER.readTree(exchange.getRequestBody());
        String user = body.path("username").asText("");
        String password = body.path("password").asText("");

        if (SandboxUsers.isLocked(user)) {
            json(exchange, 423, Map.of("error", "Account is locked, contact your administrator"));
        } else if (SandboxUsers.isValid(user, password)) {
            json(exchange, 200, Map.of(
                    "token", SandboxUsers.tokenFor(user),
                    "username", user,
                    "roles", SandboxUsers.rolesOf(user)));
        } else {
            json(exchange, 401, Map.of("error", "Invalid credentials"));
        }
    }

    private void handleProductById(HttpExchange exchange, String rawId) throws IOException {
        JsonNode catalogue = MAPPER.readTree(readClasspath(CLASSPATH_ROOT + "/api/products.json"));
        for (JsonNode product : catalogue.path("products")) {
            if (product.path("id").asText().equals(rawId)) {
                respond(exchange, 200, "application/json", MAPPER.writeValueAsBytes(product));
                return;
            }
        }
        json(exchange, 404, Map.of("error", "product " + rawId + " does not exist"));
    }

    // ------------------------------------------------------------------ plumbing

    private void serveJsonResource(HttpExchange exchange, String fileName) throws IOException {
        byte[] body = readClasspath(CLASSPATH_ROOT + "/api/" + fileName);
        if (body == null) {
            json(exchange, 500, Map.of("error", "fixture " + fileName + " missing from the classpath"));
            return;
        }
        respond(exchange, 200, "application/json", body);
    }

    private void json(HttpExchange exchange, int status, Object payload) throws IOException {
        respond(exchange, status, "application/json", MAPPER.writeValueAsBytes(payload));
    }

    private void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private byte[] readClasspath(String resource) throws IOException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    private String contentTypeOf(String path) {
        int dot = path.lastIndexOf('.');
        String extension = dot < 0 ? "" : path.substring(dot + 1).toLowerCase();
        return switch (extension) {
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "text/javascript";
            case "json" -> "application/json";
            case "svg" -> "image/svg+xml";
            case "png" -> "image/png";
            default -> "application/octet-stream";
        };
    }
}
