package guru.elevatehub;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A tiny HTTP server with no external dependencies (it uses the JDK's
 * built-in com.sun.net.httpserver). In Project 3 the app is a web
 * service so that, once the pipeline deploys it, you can open it in a
 * browser. The Calculator logic is unchanged from Project 2.
 */
public class App {

    private static final Calculator calculator = new Calculator();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", exchange ->
                respond(exchange, 200,
                        "{\"message\":\"my first CI/CD Project 3 deployed from ECR\",\"status\":\"ok\"}"));

        server.createContext("/health", exchange ->
                respond(exchange, 200, "{\"status\":\"healthy\"}"));

        server.createContext("/add", exchange -> {
            Map<String, String> q = queryParams(exchange.getRequestURI());
            try {
                int a = Integer.parseInt(q.getOrDefault("a", "0"));
                int b = Integer.parseInt(q.getOrDefault("b", "0"));
                respond(exchange, 200, "{\"result\":" + calculator.add(a, b) + "}");
            } catch (NumberFormatException e) {
                respond(exchange, 400, "{\"error\":\"a and b must be integers\"}");
            }
        });

        server.setExecutor(null);
        System.out.println("Listening on port " + port);
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getQuery();
        if (query == null) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }
}
