package com.proyectoestadistico.model;

import java.util.ArrayList;
import java.util.List;

public class TablaDatos {

    private final List<String> encabezados = new ArrayList<>();
    private final List<List<String>> filas = new ArrayList<>();

    public List<String> getEncabezados() {
        return encabezados;
    }

    public List<List<String>> getFilas() {
        return filas;
    }

    public int getNumeroColumnas() {
        return encabezados.size();
    }

    public int getNumeroFilas() {
        return filas.size();
    }

    public String getValor(int fila, int columna) {
        return filas.get(fila).get(columna);
    }
}

