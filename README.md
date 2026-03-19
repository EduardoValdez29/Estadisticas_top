## Proyecto Estadístico - Reporte tabular, gráficas y PDF

Aplicación de escritorio en Java (Maven + Swing) que:

- Carga datos desde un archivo CSV (proveniente de Excel).
- Muestra un **reporte tabular** con los datos.
- Genera **3 tipos de gráficas distintas** (no pastel ni histograma): barras, líneas y dispersión.
- Exporta el **reporte completo (tabla + gráficas) a un archivo PDF** usando iText.

### Cómo ejecutar

1. Asegúrate de tener instalado **JDK 17 o superior**.
2. Entra a la carpeta del proyecto (`ProyectoEstadistico`).
3. Compila el proyecto con Maven (en tu consola, una vez que tengas `mvn` instalado y configurado en el PATH):

   ```bash
   mvn clean package
   ```

4. Ejecuta la aplicación:

   ```bash
   java -cp "target/reporte-graficas-pdf-1.0-SNAPSHOT.jar;target/dependency/*" com.proyectoestadistico.ui.MainApp
   ```

5. Desde la ventana:
   - Pulsa **"Cargar CSV"** para seleccionar un archivo CSV exportado desde Excel.
   - Verifica la **tabla** y las **tres gráficas** (barras, líneas y dispersión).
   - Pulsa **"Generar PDF"** para guardar el reporte con tabla y gráficas en un archivo PDF.

### CSV de ejemplo

Puedes generar un CSV desde Excel con columnas como:

- `X` (números o categorías)
- `Y` (valores numéricos)

Ejemplo sencillo:

```text
Categoria,Valor
A,10
B,15
C,7
```

