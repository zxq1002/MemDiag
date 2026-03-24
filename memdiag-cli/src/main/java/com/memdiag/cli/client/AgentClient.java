package com.memdiag.cli.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AgentClient {

    private final String baseUrl;
    private final Gson gson;

    public AgentClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
        this.gson = new GsonBuilder().create();
    }

    public boolean isHealthy() throws IOException {
        String response = get("/health");
        return response != null && response.contains("\"status\":\"ok\"");
    }

    public HeapHistogram getHeapHistogram(int limit) throws IOException {
        String response = get("/api/heap/histogram?limit=" + limit);
        return gson.fromJson(response, HeapHistogram.class);
    }

    public ThreadDump getThreadDump() throws IOException {
        String response = get("/api/threads");
        return gson.fromJson(response, ThreadDump.class);
    }

    public DiagnosisResult getDiagnosis() throws IOException {
        String response = get("/api/diagnose");
        return gson.fromJson(response, DiagnosisResult.class);
    }

    private String get(String path) throws IOException {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP error: " + responseCode);
        }

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
