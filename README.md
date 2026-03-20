# Proyecto Estadístico — INEGI, tablas, gráficas y PDF

Aplicación de escritorio en **Java 17** (**Maven** + **Swing**) que descarga indicadores del **INEGI**, los muestra en **tablas** y **tres gráficas** (barras, líneas y dispersión), y exporta un **PDF** con tabla, gráficas y un texto introductorio generado con la **API de Gemini**.

## Funcionalidades

- **Actualizar datos (INEGI)**: peticiones HTTP al API de indicadores, parseo JSON y generación de CSV por categoría.
- **Categorías** (definidas en código): **Educación**, **Población** y **Seguridad**. Tras actualizar, se elige la categoría en el combo para refrescar tablas y gráficas.
- **Persistencia opcional en SQL Server**: casilla *“Guardar en BD (SQL Server)”*. Si está activa, los CSV generados se vuelcan a la tabla `dbo.INEGI_Indicadores` (ver paquete `db`).
- **Visualización**: hasta **tres tablas** derivadas del mismo CSV (eje X = primera columna, típicamente `Periodo`; series Y = hasta tres indicadores). **Tres gráficas**: barras, líneas y dispersión (sin pastel ni histograma).
- **Generar PDF**: reporte con la tabla de la **pestaña seleccionada** y las tres gráficas para esa serie; incluye introducción breve vía Gemini (~750 caracteres máx.).

Los CSV intermedios se guardan por defecto en:

`%USERPROFILE%\ProyectoEstadistico\inegi_csv` (Windows)  
equivalente a `~/ProyectoEstadistico/inegi_csv` en sistemas tipo Unix.

## Requisitos

- **JDK 17** o superior.
- **Maven 3.6+** en el PATH.
- **Conexión a Internet** para INEGI (y para Gemini al generar el PDF).
- **SQL Server** accesible solo si usas la opción de guardar en BD (JDBC en el proyecto).

## Cómo compilar y ejecutar

1. Clona o copia el repositorio y entra a la carpeta del proyecto (donde está `pom.xml`).

2. Compila y empaqueta:

   ```bash
   mvn clean package
   ```

3. Copia las dependencias al directorio `target/dependency` (el JAR generado no incluye librerías externas):

   ```bash
   mvn dependency:copy-dependencies -DoutputDirectory=target/dependency
   ```

4. Ejecuta la aplicación (**Windows**, separador `;`):

   ```bash
   java -cp "target/reporte-graficas-pdf-1.0-SNAPSHOT.jar;target/dependency/*" com.proyectoestadistico.ui.MainApp
   ```

   En **Linux o macOS** usa `:` en lugar de `;`:

   ```bash
   java -cp "target/reporte-graficas-pdf-1.0-SNAPSHOT.jar:target/dependency/*" com.proyectoestadistico.ui.MainApp
   ```

5. En la ventana:
   - Opcional: desmarca *“Guardar en BD”* si no quieres escribir en SQL Server.
   - Pulsa **“Actualizar datos (INEGI)”** y espera a que termine la descarga.
   - Elige **Categoría** en el combo.
   - Revisa las pestañas de **tablas** y **gráficas**.
   - **“Generar PDF”** pide ruta de guardado; la introducción usa Gemini si hay API key configurada.

## Configuración opcional

### API Gemini (intro del PDF)

El servicio lee la variable de entorno **`GEMINI_API_KEY`**. Si no existe, el código puede usar un valor por defecto embebido (no recomendable en repositorios públicos).

Ejemplo (PowerShell):

```powershell
$env:GEMINI_API_KEY = "tu-clave"
java -cp "target/reporte-graficas-pdf-1.0-SNAPSHOT.jar;target/dependency/*" com.proyectoestadistico.ui.MainApp
```

### SQL Server

La conexión se resuelve en este orden (**las variables de entorno tienen prioridad** sobre el archivo):

1. Variables de entorno (recomendado en servidores / CI):

   | Variable | Descripción |
   |----------|-------------|
   | `SQLSERVER_HOST` | Servidor (ej. `localhost` o nombre de instancia) |
   | `SQLSERVER_PORT` | Puerto (por defecto `1433` si no se define) |
   | `SQLSERVER_DATABASE` | Nombre de la base de datos |
   | `SQLSERVER_USER` | Usuario |
   | `SQLSERVER_PASSWORD` | Contraseña |
   | `SQLSERVER_ENCRYPT` | `true` / `false` (por defecto `true`) |
   | `SQLSERVER_TRUST_SERVER_CERTIFICATE` | `true` / `false` (por defecto `true`, típico en desarrollo) |

2. Archivo local (misma carpeta base que los CSV):

   `%USERPROFILE%\ProyectoEstadistico\sqlserver.properties` (Windows)  
   `~/ProyectoEstadistico/sqlserver.properties` (Linux / macOS)

   Copia `sqlserver.properties.example` del repositorio a esa ruta, renómbralo a `sqlserver.properties` y edita valores. **No subas** `sqlserver.properties` a Git (está en `.gitignore` si lo pones en la raíz del proyecto; el path por defecto es fuera del repo).

Ejemplo PowerShell solo con entorno:

```powershell
$env:SQLSERVER_HOST = "localhost"
$env:SQLSERVER_DATABASE = "DatosMexico"
$env:SQLSERVER_USER = "miUsuario"
$env:SQLSERVER_PASSWORD = "miContraseña"
java -cp "target/reporte-graficas-pdf-1.0-SNAPSHOT.jar;target/dependency/*" com.proyectoestadistico.ui.MainApp
```

Si no hay host/usuario/contraseña configurados, la aplicación mostrará un error claro al intentar guardar en BD.

## Dependencias principales (Maven)

| Uso        | Dependencia        |
|-----------|--------------------|
| Gráficas  | JFreeChart         |
| PDF       | iText 7 (`itext7-core`, tipo BOM POM) |
| CSV       | Apache Commons CSV |
| INEGI JSON| Jackson Databind   |
| SQL Server| Microsoft JDBC     |

## Estructura del código (resumen)

- `com.proyectoestadistico.ui` — `MainApp`, tema visual (`UITheme`).
- `com.proyectoestadistico.model` — `TablaDatos`.
- `com.proyectoestadistico.service` — INEGI (`ConfigINEGI`, `ActualizadorINEGI`, `INegiHttpClient`, `GeneradorCSVDesdeINegi`), CSV (`LectorCSV`), gráficas (`GeneradorGraficas`), PDF (`GeneradorPDF`), Gemini (`GeminiIntroduccionService`).
- `com.proyectoestadistico.db` — conexión y DAO SQL Server.

## Nota sobre CSV manual

El flujo principal es INEGI → CSV generado automáticamente. Los CSV tienen cabecera y columnas alineadas con los grupos definidos en `ConfigINEGI` (primera columna de periodo más una columna por indicador). Un CSV “genérico” tipo solo `X,Y` no coincide con ese contrato sin adaptar el generador o la configuración.

---

*Documentación alineada con el comportamiento actual de la aplicación (actualización INEGI, BD opcional y PDF con Gemini).*
