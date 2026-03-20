package com.proyectoestadistico.db;

import com.proyectoestadistico.model.TablaDatos;
import com.proyectoestadistico.service.ConfigINEGI;
import com.proyectoestadistico.service.LectorCSV;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DAOIndicadoresSQLServer {

    public void crearTablaSiNoExiste(Connection conn) throws SQLException {
        String sql = """
                IF OBJECT_ID('dbo.INEGI_Indicadores', 'U') IS NULL
                BEGIN
                    CREATE TABLE dbo.INEGI_Indicadores(
                        Categoria NVARCHAR(50) NOT NULL,
                        IndicadorCodigo NVARCHAR(20) NOT NULL,
                        IndicadorDescripcion NVARCHAR(200) NULL,
                        Periodo NVARCHAR(20) NOT NULL,
                        Valor DECIMAL(38,20) NULL,
                        CONSTRAINT PK_INEGI_Indicadores PRIMARY KEY (Categoria, IndicadorCodigo, Periodo)
                    );
                END
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        }
    }

    public void actualizarCategoriaDesdeCsv(Connection conn,
                                              Path csv,
                                              ConfigINEGI.GrupoDef grupo,
                                              LectorCSV lectorCSV) throws Exception {
        if (conn == null) throw new IllegalArgumentException("conn no puede ser null");
        if (csv == null) throw new IllegalArgumentException("csv no puede ser null");
        if (grupo == null) throw new IllegalArgumentException("grupo no puede ser null");
        if (lectorCSV == null) throw new IllegalArgumentException("lectorCSV no puede ser null");

        TablaDatos tabla = lectorCSV.leer(csv);
        if (tabla == null || tabla.getNumeroColumnas() == 0) {
            throw new IllegalStateException("CSV inválido o vacío: " + csv);
        }

        int esperadas = 1 + grupo.indicadores().size();
        if (tabla.getNumeroColumnas() != esperadas) {
            throw new IllegalStateException("CSV '" + csv.getFileName() + "' tiene " + tabla.getNumeroColumnas()
                    + " columnas; se esperaban " + esperadas + ".");
        }

        conn.setAutoCommit(false);
        try {
            crearTablaSiNoExiste(conn);

            // Actualización: limpiamos la categoría completa antes de insertar.
            try (PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM dbo.INEGI_Indicadores WHERE Categoria = ?")) {
                del.setString(1, grupo.categoria());
                del.executeUpdate();
            }

            String insertSql = """
                    INSERT INTO dbo.INEGI_Indicadores
                        (Categoria, IndicadorCodigo, IndicadorDescripcion, Periodo, Valor)
                    VALUES
                        (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                int nIndic = grupo.indicadores().size();
                for (var fila : tabla.getFilas()) {
                    if (fila == null || fila.size() < 1) continue;
                    String periodo = fila.get(0);
                    if (periodo == null || periodo.isBlank()) continue;

                    for (int i = 0; i < nIndic; i++) {
                        String valorStr = fila.size() > i + 1 ? fila.get(i + 1) : "";
                        if (valorStr == null) valorStr = "";
                        valorStr = valorStr.trim();
                        if (valorStr.isBlank()) continue; // guardamos sólo valores presentes

                        BigDecimal valor = parseBigDecimal(valorStr);
                        ConfigINEGI.IndicadorDef ind = grupo.indicadores().get(i);

                        ins.setString(1, grupo.categoria());
                        ins.setString(2, ind.codigo());
                        ins.setString(3, ind.nombre());
                        ins.setString(4, periodo.trim());
                        ins.setBigDecimal(5, valor);
                        ins.addBatch();
                    }
                }

                ins.executeBatch();
            }

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    private static BigDecimal parseBigDecimal(String raw) {
        // La API típicamente manda valores con '.' como decimal.
        // Por si acaso, eliminamos separadores de miles.
        String v = raw.replace(",", "").trim();
        if (v.isBlank()) return null;
        return new BigDecimal(v);
    }
}

