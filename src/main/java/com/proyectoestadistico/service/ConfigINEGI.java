package com.proyectoestadistico.service;

import java.util.List;

public final class ConfigINEGI {

    public record IndicadorDef(String codigo, String nombre) {
        public IndicadorDef {
            if (codigo == null || codigo.isBlank()) {
                throw new IllegalArgumentException("codigo indicador no puede ser vacío");
            }
            if (nombre == null || nombre.isBlank()) {
                throw new IllegalArgumentException("nombre indicador no puede ser vacío");
            }
        }
    }

    public record GrupoDef(String categoria, String url, List<IndicadorDef> indicadores) {
        public GrupoDef {
            if (categoria == null || categoria.isBlank()) {
                throw new IllegalArgumentException("categoria no puede ser vacía");
            }
            if (url == null || url.isBlank()) {
                throw new IllegalArgumentException("url no puede ser vacía");
            }
            if (indicadores == null || indicadores.isEmpty()) {
                throw new IllegalArgumentException("indicadores no puede ser vacío");
            }
        }
    }

    private ConfigINEGI() {}

    public static final GrupoDef GRUPO_EDUCACION = new GrupoDef(
            "Educación",
            "https://www.inegi.org.mx/app/api/indicadores/desarrolladores/jsonxml/INDICATOR/1002000041,6207019031,6200240314,6200240365/es/00/false/BISE/2.0/387dc926-4ef0-7513-5e54-c4e1de660ea3?type=json",
            List.of(
                    new IndicadorDef("1002000041", "Personas alfabetas"),
                    new IndicadorDef("6207019031", "Personas mayores a 15 con escolaridad básica"),
                    new IndicadorDef("6200240314", "Porcentaje mayores a 15 con instrucción media superior"),
                    new IndicadorDef("6200240365", "Porcentaje mayores a 15 años con instrucción superior")
            )
    );

    public static final GrupoDef GRUPO_POBLACION = new GrupoDef(
            "Población",
            "https://www.inegi.org.mx/app/api/indicadores/desarrolladores/jsonxml/INDICATOR/1002000001,1002000002,1002000003,6207020632/es/00/false/BISE/2.0/387dc926-4ef0-7513-5e54-c4e1de660ea3?type=json",
            List.of(
                    new IndicadorDef("1002000001", "Población total"),
                    new IndicadorDef("1002000002", "Población total de hombres"),
                    new IndicadorDef("1002000003", "Población total de mujeres"),
                    new IndicadorDef("6207020632", "Población total en viviendas particulares habitadas")
            )
    );

    public static final GrupoDef GRUPO_SEGURIDAD = new GrupoDef(
            "Seguridad",
            "https://www.inegi.org.mx/app/api/indicadores/desarrolladores/jsonxml/INDICATOR/6200028425,6200028433,6200028430/es/00/false/BISE/2.0/387dc926-4ef0-7513-5e54-c4e1de660ea3?type=json",
            List.of(
                    new IndicadorDef("6200028425", "Porcentaje de atestiguamiento de robos o asaltos"),
                    new IndicadorDef("6200028433", "Porcentaje de atestiguamiento de pandillas o bandas violentas"),
                    new IndicadorDef("6200028430", "Porcentaje de atestiguamiento de secuestros")
            )
    );

    public static List<GrupoDef> grupos() {
        return List.of(GRUPO_EDUCACION, GRUPO_POBLACION, GRUPO_SEGURIDAD);
    }

    public static String nombreArchivoCsv(GrupoDef grupo, java.nio.file.Path carpetaSalida) {
        String safeCategoria = grupo.categoria().replaceAll("[^a-zA-Z0-9\\-_áéíóúÁÉÍÓÚñÑ ]", "").trim().replace(' ', '_');
        return carpetaSalida.resolve("DatosMexico_" + safeCategoria + ".csv").toString();
    }
}

