package com.proyectoestadistico.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.proyectoestadistico.db.DAOIndicadoresSQLServer;
import com.proyectoestadistico.db.ConexionSQLServer;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActualizadorINEGI {

    private final INegiHttpClient httpClient;
    private final GeneradorCSVDesdeINegi generadorCSV;
    private final LectorCSV lectorCSV;

    public ActualizadorINEGI() {
        this.httpClient = new INegiHttpClient();
        this.generadorCSV = new GeneradorCSVDesdeINegi();
        this.lectorCSV = new LectorCSV();
    }

    public record Resultado(Map<String, Path> csvPorCategoria) {}

    public Resultado actualizar(boolean persistirEnBD, Path carpetaSalida) throws Exception {
        if (carpetaSalida == null) {
            throw new IllegalArgumentException("carpetaSalida no puede ser null");
        }

        Map<String, Path> csvPorCategoria = new HashMap<>();
        List<ConfigINEGI.GrupoDef> grupos = ConfigINEGI.grupos();

        for (ConfigINEGI.GrupoDef grupo : grupos) {
            JsonNode root = obtenerJsonConReintentos(grupo.url(), 3);
            Path csv = generadorCSV.generarCsvDesdeJson(root, grupo, carpetaSalida);
            csvPorCategoria.put(grupo.categoria(), csv);
        }

        if (persistirEnBD) {
            try (Connection conn = ConexionSQLServer.getConnection()) {
                DAOIndicadoresSQLServer dao = new DAOIndicadoresSQLServer();
                for (ConfigINEGI.GrupoDef grupo : grupos) {
                    Path csv = csvPorCategoria.get(grupo.categoria());
                    if (csv == null) continue;
                    dao.actualizarCategoriaDesdeCsv(conn, csv, grupo, lectorCSV);
                }
            }
        }

        return new Resultado(csvPorCategoria);
    }

    private JsonNode obtenerJsonConReintentos(String url, int intentos) throws Exception {
        if (intentos < 1) throw new IllegalArgumentException("intentos inválidos");

        Exception last = null;
        for (int i = 1; i <= intentos; i++) {
            try {
                return httpClient.obtenerJson(url);
            } catch (Exception e) {
                last = e;
                try {
                    long sleepMs = 800L * i;
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        throw last != null ? last : new Exception("No se pudo obtener JSON desde INEGI");
    }
}

