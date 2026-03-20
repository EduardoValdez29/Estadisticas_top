package com.proyectoestadistico.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeneradorCSVDesdeINegi {

    private static final String HEADER_PERIODO = "Periodo";

    public Path generarCsvDesdeJson(JsonNode root, ConfigINEGI.GrupoDef grupo, Path carpetaSalida) throws IOException {
        if (root == null) {
            throw new IllegalArgumentException("root JSON no puede ser null");
        }
        if (grupo == null) {
            throw new IllegalArgumentException("grupo no puede ser null");
        }
        if (carpetaSalida == null) {
            throw new IllegalArgumentException("carpetaSalida no puede ser null");
        }

        Files.createDirectories(carpetaSalida);
        Path archivoSalida = Path.of(ConfigINEGI.nombreArchivoCsv(grupo, carpetaSalida));

        JsonNode series = root.get("Series");
        if (series == null || !series.isArray()) {
            throw new IOException("JSON INEGI inválido: falta nodo 'Series' o no es array");
        }

        // indicadorCodigo -> (periodo -> valor)
        Map<String, Map<String, String>> valores = new HashMap<>();
        // Periodos completos (unión)
        Set<String> periodos = new LinkedHashSet<>();

        for (JsonNode serieNode : series) {
            if (serieNode == null || serieNode.isNull()) continue;

            JsonNode codigoNode = serieNode.get("INDICADOR");
            String indicadorCodigo = codigoNode == null || codigoNode.isNull() ? "" : codigoNode.asText();
            if (indicadorCodigo.isBlank()) continue;

            JsonNode obsArray = serieNode.get("OBSERVATIONS");
            if (obsArray == null || !obsArray.isArray()) continue;

            for (JsonNode obsNode : obsArray) {
                if (obsNode == null || obsNode.isNull()) continue;
                JsonNode periodoNode = obsNode.get("TIME_PERIOD");
                JsonNode obsValNode = obsNode.get("OBS_VALUE");

                String periodo = periodoNode == null || periodoNode.isNull() ? "" : periodoNode.asText();
                if (periodo.isBlank()) continue;

                String obsVal = obsValNode == null || obsValNode.isNull() ? "" : obsValNode.asText();

                periodos.add(periodo);
                valores.computeIfAbsent(indicadorCodigo, k -> new HashMap<>()).put(periodo, obsVal);
            }
        }

        if (periodos.isEmpty()) {
            throw new IOException("JSON INEGI inválido: no se encontraron OBSERVATIONS (periodos vacíos)");
        }

        List<String> periodosOrdenados = new ArrayList<>(periodos);
        periodosOrdenados.sort(new Comparator<>() {
            @Override
            public int compare(String a, String b) {
                Integer ia = tryParseInt(a);
                Integer ib = tryParseInt(b);
                if (ia != null && ib != null) return Integer.compare(ia, ib);
                return a.compareTo(b);
            }
        });

        // Encabezados: Periodo + nombres de indicadores en orden
        List<String> encabezados = new ArrayList<>();
        encabezados.add(HEADER_PERIODO);
        for (ConfigINEGI.IndicadorDef ind : grupo.indicadores()) {
            encabezados.add(ind.nombre());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(archivoSalida, StandardCharsets.ISO_8859_1);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(encabezados.toArray(new String[0]))
                             .setRecordSeparator("\n")
                             .build()
             )) {
            for (String periodo : periodosOrdenados) {
                List<String> row = new ArrayList<>(encabezados.size());
                row.add(periodo);
                for (ConfigINEGI.IndicadorDef ind : grupo.indicadores()) {
                    String val = valores.getOrDefault(ind.codigo(), Map.of()).get(periodo);
                    row.add(val == null ? "" : sanitizeCsvValue(val));
                }
                printer.printRecord(row);
            }
        }

        return archivoSalida;
    }

    private static Integer tryParseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String sanitizeCsvValue(String v) {
        if (v == null) return "";
        // Evita saltos de línea dentro del CSV
        return v.replace("\r", " ").replace("\n", " ").trim();
    }
}

