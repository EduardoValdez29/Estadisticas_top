package com.proyectoestadistico.db;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Credenciales desde variables de entorno (prioridad) o desde
 * {@code %USERPROFILE%/ProyectoEstadistico/sqlserver.properties} (o {@code ~/...}).
 * Ver {@code sqlserver.properties.example} en el repositorio.
 */
public final class ConexionSQLServer {

    private static final Path RUTA_PROPIEDADES_DEFINIDA = Path.of(
            System.getProperty("user.home"),
            "ProyectoEstadistico",
            "sqlserver.properties"
    );

    private static volatile SqlConfig configCache;

    private ConexionSQLServer() {}

    public static Connection getConnection() throws SQLException {
        SqlConfig cfg = configCache;
        if (cfg == null) {
            synchronized (ConexionSQLServer.class) {
                if (configCache == null) {
                    configCache = SqlConfig.cargar();
                }
                cfg = configCache;
            }
        }
        cfg.validar();

        String url = "jdbc:sqlserver://" + cfg.host + ":" + cfg.port
                + ";databaseName=" + cfg.database
                + ";encrypt=" + cfg.encrypt + ";trustServerCertificate=" + cfg.trustServerCertificate + ";";

        return DriverManager.getConnection(url, cfg.user, cfg.password);
    }

    /**
     * Fuerza releer config en la próxima conexión (por si se editó el .properties).
     */
    public static void limpiarCacheConfig() {
        synchronized (ConexionSQLServer.class) {
            configCache = null;
        }
    }

    private static final class SqlConfig {
        final String host;
        final String port;
        final String database;
        final String user;
        final String password;
        final boolean encrypt;
        final boolean trustServerCertificate;

        SqlConfig(String host, String port, String database, String user, String password,
                  boolean encrypt, boolean trustServerCertificate) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.user = user;
            this.password = password;
            this.encrypt = encrypt;
            this.trustServerCertificate = trustServerCertificate;
        }

        static SqlConfig cargar() {
            Properties archivo = leerArchivoSiExiste(RUTA_PROPIEDADES_DEFINIDA);

            String host = primeroNoVacio(env("SQLSERVER_HOST"), archivo.getProperty("sqlserver.host"));
            String port = primeroNoVacio(env("SQLSERVER_PORT"), archivo.getProperty("sqlserver.port", "1433"));
            String database = primeroNoVacio(env("SQLSERVER_DATABASE"), archivo.getProperty("sqlserver.database"));
            String user = primeroNoVacio(env("SQLSERVER_USER"), archivo.getProperty("sqlserver.user"));
            String password = primeroNoVacio(env("SQLSERVER_PASSWORD"), archivo.getProperty("sqlserver.password"));
            boolean encrypt = parseBool(
                    env("SQLSERVER_ENCRYPT"),
                    archivo.getProperty("sqlserver.encrypt"),
                    true
            );
            boolean trustCert = parseBool(
                    env("SQLSERVER_TRUST_SERVER_CERTIFICATE"),
                    archivo.getProperty("sqlserver.trustServerCertificate"),
                    true
            );

            return new SqlConfig(host, port, database, user, password, encrypt, trustCert);
        }

        void validar() {
            if (host == null || host.isBlank()) {
                throw new IllegalStateException(mensajeAyuda("host / SQLSERVER_HOST o sqlserver.host"));
            }
            if (port == null || port.isBlank()) {
                throw new IllegalStateException(mensajeAyuda("puerto / SQLSERVER_PORT o sqlserver.port"));
            }
            if (database == null || database.isBlank()) {
                throw new IllegalStateException(mensajeAyuda("base de datos / SQLSERVER_DATABASE o sqlserver.database"));
            }
            if (user == null || user.isBlank()) {
                throw new IllegalStateException(mensajeAyuda("usuario / SQLSERVER_USER o sqlserver.user"));
            }
            if (password == null || password.isBlank()) {
                throw new IllegalStateException(mensajeAyuda("contraseña / SQLSERVER_PASSWORD o sqlserver.password"));
            }
        }

        private static String mensajeAyuda(String campo) {
            return "SQL Server no configurado (" + campo + "). "
                    + "Defina variables de entorno o cree el archivo: " + RUTA_PROPIEDADES_DEFINIDA
                    + " (copie sqlserver.properties.example del proyecto).";
        }

        private static Properties leerArchivoSiExiste(Path ruta) {
            Properties p = new Properties();
            if (!Files.isRegularFile(ruta)) {
                return p;
            }
            try (InputStream in = Files.newInputStream(ruta)) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (Exception ignored) {
            }
            return p;
        }

        private static String env(String nombre) {
            String v = System.getenv(nombre);
            return v == null ? null : v.trim();
        }

        private static String primeroNoVacio(String a, String b) {
            if (a != null && !a.isBlank()) {
                return a.trim();
            }
            if (b != null && !b.isBlank()) {
                return b.trim();
            }
            return "";
        }

        private static boolean parseBool(String envVal, String propVal, boolean porDefecto) {
            String s = envVal != null && !envVal.isBlank() ? envVal.trim() : propVal;
            if (s == null || s.isBlank()) {
                return porDefecto;
            }
            return Boolean.parseBoolean(s.trim());
        }
    }
}
