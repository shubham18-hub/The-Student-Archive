package com.example.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;


public class LocalOAuthServer {

    private HttpServer server;

    // BlockingQueue is a thread-safe queue — the server thread puts the request in,
    // and the OAuth thread takes it out
    private BlockingQueue<HttpExchange> callbackQueue;

    // Starts the HTTP server on the given port
    public LocalOAuthServer(int port) throws IOException {
        this.callbackQueue = new LinkedBlockingQueue<>();

        // Create a simple HTTP server
        this.server = HttpServer.create(new java.net.InetSocketAddress(port), 0);

        // When GitHub calls /callback, put the request into the queue
        server.createContext("/callback", exchange -> {
            System.out.println("GitHub callback received!");
            callbackQueue.offer(exchange);
        });

        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
        System.out.println("Waiting for GitHub on http://localhost:" + port + "/callback");
    }

    // Waits until GitHub sends the callback 
   
    public HttpExchange waitForCallback(long timeoutSeconds) throws TimeoutException, InterruptedException {
        HttpExchange exchange = callbackQueue.poll(timeoutSeconds, TimeUnit.SECONDS);
        if (exchange == null) {
            throw new TimeoutException("GitHub did not respond within " + timeoutSeconds + " seconds");
        }
        return exchange;
    }

    // Sends an HTML response back to the browser after the callback
    public void sendResponse(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }

    // Shuts down the server — called after we get the code
    public void close() {
        if (server != null) {
            server.stop(0);
            System.out.println("OAuth server stopped");
        }
    }
}
