package com.proyectoestadistico.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionSQLServer {

    private static final String servidor = "D_VALDEZ";
    private static final String puerto = "1433";
    private static final String baseDatos = "DatosMexico";
    private static final String usuario = "diegovaldez";
    private static final String password = "060629";

    private ConexionSQLServer() {}

    public static Connection getConnection() throws SQLException {
        validarCredenciales();

        String url = "jdbc:sqlserver://" + servidor + ":" + puerto
                + ";databaseName=" + baseDatos
                + ";encrypt=true;trustServerCertificate=true;";

        return DriverManager.getConnection(url, usuario, password);
    }

    private static void validarCredenciales() {
        if (servidor.isBlank()) throw new IllegalStateException("servidor SQL no configurado");
        if (puerto.isBlank()) throw new IllegalStateException("puerto SQL no configurado");
        if (baseDatos.isBlank()) throw new IllegalStateException("baseDatos SQL no configurada");
        if (usuario.isBlank()) throw new IllegalStateException("usuario SQL no configurado");
        if (password.isBlank()) throw new IllegalStateException("password SQL no configurado");
    }
}

