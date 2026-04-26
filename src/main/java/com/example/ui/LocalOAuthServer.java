package com.example.ui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

// Tiny local HTTP server that catches GitHub's OAuth redirect
public class LocalOAuthServer {

    private final HttpServer server;
    private final BlockingQueue<HttpExchange> callbackQueue = new LinkedBlockingQueue<>();

    public LocalOAuthServer(int port) throws IOException {
        server = HttpServer.create(new java.net.InetSocketAddress(port), 0);
        server.createContext("/callback", exchange -> callbackQueue.offer(exchange));
        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
        System.out.println("Waiting for GitHub callback on port " + port);
    }

    // Blocks until GitHub redirects back, or times out
    public HttpExchange waitForCallback(long timeoutSeconds) throws TimeoutException, InterruptedException {
        HttpExchange exchange = callbackQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
        if (exchange == null) throw new TimeoutException("GitHub didn't respond in time");
        return exchange;
    }

    public void sendResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }

    public void close() {
        if (server != null) server.stop(0);
    }
}
