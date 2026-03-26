package com.memdiag.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzer;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzerFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentServer {

    private final String host;
    private final int port;
    private final Instrumentation instrumentation;
    private final NativeMemoryAnalyzer nativeAnalyzer;
    private final Gson gson;

    private HttpServer server;
    private ExecutorService executor;
    private volatile boolean running = false;

    public AgentServer(String host, int port, Instrumentation instrumentation) {
        this.host = host;
        this.port = port;
        this.instrumentation = instrumentation;
        this.nativeAnalyzer = NativeMemoryAnalyzerFactory.getInstance();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void start() throws Exception {
        if (running) {
            return;
        }

        executor = Executors.newFixedThreadPool(4);
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(executor);

        server.createContext("/api/v1/histogram", new SimpleHandler("histogram"));
        server.createContext("/api/v1/threads", new SimpleHandler("threads"));
        server.createContext("/api/v1/diagnose", new SimpleHandler("diagnose"));
        server.createContext("/api/v1/snapshot", new SimpleHandler("snapshot"));
        server.createContext("/api/v1/detach", new DetachHandler());

        server.createContext("/api/v1/native/status", new NativeStatusHandler());
        server.createContext("/api/v1/native/summary", new NativeSummaryHandler());
        server.createContext("/api/v1/native/regions", new NativeRegionsHandler());
        server.createContext("/api/v1/native/diagnose", new NativeDiagnoseHandler());

        server.start();
        running = true;
        System.out.printf("[MemDiag] Agent server started on %s:%d%n", host, port);
    }

    public void stop() throws Exception {
        if (!running) {
            return;
        }

        running = false;
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
        System.out.println("[MemDiag] Agent server stopped");
    }

    public boolean isRunning() {
        return running;
    }

    private void sendJsonResponse(HttpExchange exchange, Object response, int statusCode) throws IOException {
        String json = gson.toJson(response);
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String error, int statusCode) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("error", error);
        sendJsonResponse(exchange, response, statusCode);
    }

    private class SimpleHandler implements HttpHandler {
        private final String type;

        SimpleHandler(String type) {
            this.type = type;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", type + " endpoint placeholder");
            response.put("timestamp", System.currentTimeMillis());
            sendJsonResponse(exchange, response, 200);
        }
    }

    private class DetachHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Detach request received");
                sendJsonResponse(exchange, response, 200);

                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        stop();
                    } catch (Exception e) {
                    }
                }).start();
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> status = new HashMap<>();
                status.put("available", nativeAnalyzer.isAvailable());
                status.put("platform", nativeAnalyzer.getPlatform());
                status.put("requiresAgent", nativeAnalyzer.requiresAgent());
                status.put("agentAttached", nativeAnalyzer.isAgentAttached());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", status);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", nativeAnalyzer.getSummary());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeRegionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", nativeAnalyzer.getMemoryRegions());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeDiagnoseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", nativeAnalyzer.analyzeNativeLeaks());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }
}
