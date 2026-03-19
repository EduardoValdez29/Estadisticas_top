package com.proyectoestadistico.service;

import com.proyectoestadistico.model.TablaDatos;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LectorCSV {

    public TablaDatos leer(Path rutaCsv) throws IOException {
        TablaDatos tabla = new TablaDatos();

        // Usamos ISO_8859_1 que acepta cualquier byte 0-255 (típico CSV de Excel en español)
        // y así evitamos "MalformedInputException: Input length = 1".
        try (Reader reader = Files.newBufferedReader(rutaCsv, StandardCharsets.ISO_8859_1);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            tabla.getEncabezados().addAll(parser.getHeaderNames());

            for (CSVRecord record : parser) {
                var fila = new java.util.ArrayList<String>();
                for (String header : tabla.getEncabezados()) {
                    fila.add(record.get(header));
                }
                tabla.getFilas().add(fila);
            }
        }

        return tabla;
    }
}

