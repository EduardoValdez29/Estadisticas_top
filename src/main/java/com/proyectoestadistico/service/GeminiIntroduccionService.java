package com.proyectoestadistico.service;

import com.proyectoestadistico.i18n.UiMessages;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiIntroduccionService {

    private static final String GEMINI_API_KEY_FALLBACK = "AIzaSyDwUvXnQv2BCzVLTtrXqx_8-4BrklUHbFs";
    private static final String MODEL = "gemini-3.1-flash-lite-preview";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public GeminiIntroduccionService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public String generarIntroduccion(String categoria, String indicador) {
        String cat = categoria == null ? "" : categoria.trim();
        String ind = indicador == null ? "" : indicador.trim();

        String apiKey = obtenerApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return UiMessages.get("gemini.no.token");
        }

        String prompt;
        if (UiMessages.isEnglish()) {
            prompt = ""
                    + "Write a short introductory text (max 750 characters) in English for a report with bar, line, "
                    + "and scatter charts plus a table, focused on the indicator: "
                    + (ind.isBlank() ? "general" : ind)
                    + ". Category: " + (cat.isBlank() ? "general" : cat)
                    + ". No bullet lists or headings. At most 2–3 sentences. "
                    + "It should mention the charts and what the data suggests.";
        } else {
            prompt = ""
                    + "Genera un texto introductorio breve (maximo 750 caracteres) en espanol para acompañar "
                    + "un reporte con graficas de barras, lineas y dispersion, y una tabla, enfocado en el indicador: "
                    + (ind.isBlank() ? "general" : ind)
                    + ". Categoria: " + (cat.isBlank() ? "general" : cat)
                    + ". No uses listas ni titulos. 2-3 frases maximas. "
                    + "Debe hablar sobre las graficas y lo que se observa a partir de los datos.";
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + apiKey;

            ObjectNode root = mapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode content0 = mapper.createObjectNode();
            content0.put("role", "user");
            ArrayNode parts = content0.putArray("parts");
            ObjectNode part0 = mapper.createObjectNode();
            part0.put("text", prompt);
            parts.add(part0);
            contents.add(content0);

            ObjectNode genCfg = root.putObject("generationConfig");
            genCfg.put("temperature", 0.4);
            genCfg.put("maxOutputTokens", 250);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(40))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                    .build();

            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = resp.statusCode();
            if (status < 200 || status >= 300) {
                return UiMessages.get("gemini.no.http", status);
            }

            JsonNode json = mapper.readTree(resp.body());
            String text = extraerTextoGemini(json);
            if (text == null || text.isBlank()) {
                return UiMessages.get("gemini.empty");
            }

            text = text.trim();
            return recortarCaracteres(text, 750);
        } catch (Exception e) {
            String detail = e.getMessage() == null ? UiMessages.get("gemini.detail.none") : e.getMessage();
            return UiMessages.get("gemini.error", detail);
        }
    }

    private static String obtenerApiKey() {
        String env = System.getenv("GEMINI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return GEMINI_API_KEY_FALLBACK;
    }

    private static String extraerTextoGemini(JsonNode json) {
        if (json == null) return null;
        JsonNode candidates = json.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) return null;
        JsonNode cand0 = candidates.get(0);
        JsonNode content = cand0 == null ? null : cand0.get("content");
        if (content == null) return null;
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) return null;
        JsonNode part0 = parts.get(0);
        JsonNode text = part0 == null ? null : part0.get("text");
        return text == null ? null : text.asText();
    }

    private static String recortarCaracteres(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars).trim();
    }
}

