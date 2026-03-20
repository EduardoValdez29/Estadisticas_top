package com.proyectoestadistico.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class INegiHttpClient {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public INegiHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = new ObjectMapper();
    }

    public JsonNode obtenerJson(String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url no puede ser vacía");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int status = resp.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("INEGI respondió con HTTP " + status);
        }

        String body = resp.body();
        if (body == null || body.isBlank()) {
            throw new IOException("Respuesta INEGI vacía");
        }

        JsonNode root = mapper.readTree(body);
        if (root == null) {
            throw new IOException("No se pudo parsear JSON INEGI");
        }
        return root;
    }
}

